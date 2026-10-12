#!/usr/bin/env python3
"""
EvenTom poster generator (host-side tooling, not a gradle dependency).

Renders one offline poster per fixture event (poster_event_<id>.png) and one
fallback poster per event category (poster_<category>.png) into
androidApp/src/main/res/drawable-nodpi/. Reads the events fixtures directly
from shared/src/commonMain/resources/api/events.json so artwork and data stay
in sync; output is fully deterministic (fixed geometry, no randomness).

Palette mirrors EventCategory.heroColors() in androidApp/EventDetailScreen.kt.
Design language: Eventim ticket-blue accents on a per-category gradient,

Usage:
    python3 tools/generate_posters.py
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
EVENTS_FIXTURE = ROOT / "shared/src/commonMain/resources/api/events.json"
OUT_DIR = ROOT / "androidApp/src/main/res/drawable-nodpi"
# iOS asset catalog: separate catalog so the app icon (when added later) can
# live in its own Assets.xcassets without touching poster artifacts.
IOS_CATALOG = ROOT / "iosApp/iosApp/Posters.xcassets"

WIDTH, HEIGHT = 1200, 1500          # 4:5, comfortably >= 1000px wide
TICKET_BLUE = (28, 110, 232)        # #1C6EE8 — Eventim brand blue
WHITE = (255, 255, 255)

# Same two-stop gradients the detail screen's hero uses.
CATEGORY_GRADIENTS: dict[str, tuple[tuple, tuple]] = {
    "concerts":     ((0x1C, 0x6E, 0xE8), (0x0F, 0x3F, 0xA0)),
    "musical-show": ((0x7B, 0x3F, 0xE4), (0x4A, 0x1E, 0x9E)),
    "sports":       ((0x0E, 0x9F, 0x6E), (0x06, 0x65, 0x44)),
    "comedy":       ((0xF5, 0x9E, 0x0B), (0xB4, 0x53, 0x09)),
    "theater":      ((0xDC, 0x26, 0x62), (0x9A, 0x1F, 0x1F)),
    "leisure":      ((0x06, 0xB6, 0xD4), (0x03, 0x7A, 0x9C)),
}

# Labels as the app renders them (EventCategory.label in the domain enum).
CATEGORY_LABELS: dict[str, str] = {
    "concerts":     "Konzerte",
    "musical-show": "Musical & Show",
    "sports":       "Sport",
    "comedy":       "Comedy & Humor",
    "theater":      "Theater & Kultur",
    "leisure":      "Freizeit",
}

FONT_CANDIDATES = [
    "/System/Library/Fonts/SFNS.ttf",        # SF Pro (variable) → Bold variation
    "/System/Library/Fonts/SFNSRounded.ttf",
    "/System/Library/Fonts/Helvetica.ttc",
    "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
]


def load_font(size: int) -> ImageFont.FreeTypeFont:
    for candidate in FONT_CANDIDATES:
        try:
            font = ImageFont.truetype(candidate, size)
        except Exception:
            continue
        try:
            font.set_variation_by_name("Bold")
        except Exception:
            pass  # static font — accept as-is (candidates are already bold-ish)
        return font
    raise SystemExit("No usable system font found among: " + ", ".join(FONT_CANDIDATES))


def gradient_image(top: tuple, bottom: tuple) -> Image.Image:
    """Deterministic two-stop vertical gradient."""
    img = Image.new("RGB", (WIDTH, HEIGHT))
    d = ImageDraw.Draw(img)
    for y in range(HEIGHT):
        t = y / (HEIGHT - 1)
        color = tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        d.line([(0, y), (WIDTH, y)], fill=color)
    return img


def decorate(draw: ImageDraw.ImageDraw, seed: int, notch_color: tuple) -> None:
    """Geometric decor: concentric arcs, outline circles, perforated ticket stub."""
    # Concentric arcs, bottom-left, quarter sweeps (rotation varies per seed).
    base_angle = (seed * 17) % 360
    for i, radius in enumerate((560, 700, 840)):
        alpha = 60 - i * 15
        draw.arc(
            [(180 - radius, 1500 - radius + 120), (180 + radius, 1500 + radius + 120)],
            start=base_angle - 90, end=base_angle - 10,
            fill=(255, 255, 255, alpha), width=7,
        )
    # Outline circles, top-right.
    for i, (cx, r) in enumerate(((1010, 150), (1150, 70))):
        draw.circle((cx, 190 + seed * 11 % 60), r, outline=(255, 255, 255, 80 - i * 25), width=6)
    # Small solid ticket-blue dot row (brand accent).
    for i in range(5):
        draw.circle((120 + i * 46, 120 + seed * 7 % 40), 9, fill=(*TICKET_BLUE, 255))
    # Ticket-stub perforation: dashed vertical line + two half-moon notches.
    x = 1056
    notch = 22
    for y in range(notch, HEIGHT):
        if (y // 26) % 2 == 0 and not (y < notch * 2 or y > HEIGHT - notch * 2):
            draw.line([(x, y), (x, y + 14)], fill=(255, 255, 255, 110), width=4)
    for cy in (0 + notch, HEIGHT - notch):
        draw.circle((x, cy), notch, fill=(*notch_color, 255))


def draw_title_and_label(
    draw: ImageDraw.ImageDraw,
    title: str,
    category_label: str,
) -> None:
    # Category label: uppercase with fake letter-spacing.
    label_font = load_font(46)
    spaced = "   ".join(category_label.upper())
    draw.text((96, 880), spaced, font=label_font, fill=(255, 255, 255, 220))
    # Ticket-blue accent bar under the label.
    draw.rectangle([96, 960, 380, 976], fill=(*TICKET_BLUE, 255))

    # Large wrapped title, auto-shrunk to fit the safe area (96..5..1040 width band).
    size = 150
    safe_right = 1040
    area_top, area_bottom = 1000, 1400
    while size >= 60:
        font = load_font(size)
        # Greedy wrap by measured pixel width.
        words, lines, current = title.split(), [], ""
        for word in words:
            candidate = (current + " " + word).strip()
            if draw.textbbox((0, 0), candidate, font=font)[2] <= safe_right - 96 or not current:
                current = candidate
            else:
                lines.append(current)
                current = word
        lines.append(current)
        if len(lines) * int(size * 1.12) <= area_bottom - area_top:
            break
        size -= 10
    y = area_top
    for line in lines:
        draw.text((96, y), line, font=font, fill=(255, 255, 255, 255))
        y += int(font.size * 1.12)


def render_poster(title: str, category: str) -> Image.Image:
    if category not in CATEGORY_GRADIENTS:
        raise SystemExit(f"Unknown category code: {category!r}")
    top, bottom = CATEGORY_GRADIENTS[category]
    img = gradient_image(top, bottom).convert("RGBA")
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    # notch circles get the gradient's bottom color so they read as "cut-outs"
    decorate(draw, seed=len(title) + len(category), notch_color=bottom)
    draw_title_and_label(draw, title, CATEGORY_LABELS[category])
    return Image.alpha_composite(img, overlay).convert("RGB")


def verify(paths: list[Path]) -> None:
    print(f"{'file':34} {'dims':12} {'ratio':6} {'colors':7} check")
    ok = True
    for path in paths:
        img = Image.open(path).convert("RGB")
        w, h = img.size
        ratio = h / w
        distinct = len(img.resize((80, 100)).getcolors(maxcolors=1_000_000))
        passed = w >= 1000 and 1.15 <= ratio <= 1.35 and distinct >= 3
        ok &= passed
        print(
            f"{path.name:34} {w}x{h:<8} {ratio:4.2f}   {distinct:<7} {'PASS' if passed else 'FAIL'}"
        )
    if not ok:
        print("VERIFICATION FAILED")
        sys.exit(1)
    print(f"VERIFICATION OK — {len(paths)} posters")


def ios_catalog_name(ref: str) -> str:
    """Asset-catalog imageset name for a poster ref. Refs map 1:1 by convention:
    "poster_event_1" -> "PosterEvent1"; "poster_musical_show" -> "PosterMusicalShow".
    Asset names/refs stay underscore-separated; imageset dirs use PascalCase
    (Xcode 16+ synced groups pick up new imagesets automatically).
    """
    return "".join(part.capitalize() for part in ref.split("_"))


def write_ios_catalog(imagesets: dict[str, Image.Image]) -> None:
    for ref, img in imagesets.items():
        iset_dir = IOS_CATALOG / f"{ios_catalog_name(ref)}.imageset"
        iset_dir.mkdir(parents=True, exist_ok=True)
        img.save(iset_dir / f"{ios_catalog_name(ref)}.png", "PNG", optimize=True)
        (iset_dir / "Contents.json").write_text(
            json.dumps(
                {
                    "images": [
                        {"filename": f"{ios_catalog_name(ref)}.png", "idiom": "universal", "scale": "1x"},
                        {"idiom": "universal", "scale": "2x"},
                        {"idiom": "universal", "scale": "3x"},
                    ],
                    "info": {"author": "xcode", "version": 1},
                },
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )


def main() -> None:
    events = json.loads(EVENTS_FIXTURE.read_text(encoding="utf-8"))
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    written: list[Path] = []
    # Per-event posters (list payload drives ids).
    for event in events:
        event_id, title, category = event["id"], event["title"], event["category"]
        path = OUT_DIR / f"poster_event_{event_id}.png"
        render_poster(title, category).save(path, "PNG", optimize=True)
        written.append(path)
    # Per-category fallbacks (sample-data path).
    # NOTE: res file names may only contain [a-z0-9_.] — sanitize "-" → "_";
    # imageRef values use the sanitized name ("poster_musical_show").
    for category in CATEGORY_LABELS:
        path = OUT_DIR / f"poster_{category.replace('-','_')}.png"
        render_poster(CATEGORY_LABELS[category], category).save(path, "PNG", optimize=True)
        written.append(path)

    verify(written)

    # iOS: same renders, emitted as an asset catalog (one source of truth).
    IOS_CATALOG.mkdir(parents=True, exist_ok=True)
    # Root manifest is required for actool to treat the folder as a catalog.
    (IOS_CATALOG / "Contents.json").write_text(
        '{\n  "info" : {\n    "author" : "xcode",\n    "version" : 1\n  }\n}\n',
        encoding="utf-8",
    )
    ios_imagesets = {}
    for event in events:
        ios_imagesets[f"poster_event_{event['id']}"] = render_poster(event["title"], event["category"])
    for category in CATEGORY_LABELS:
        ios_imagesets[f"poster_{category.replace('-','_')}"] = render_poster(CATEGORY_LABELS[category], category)
    write_ios_catalog(ios_imagesets)
    ios_written = list(IOS_CATALOG.glob("*.imageset/*.png"))
    print(f"\niOS catalog: {len(ios_imagesets)} imagesets in {IOS_CATALOG.relative_to(ROOT)}")
    verify(ios_written)


if __name__ == "__main__":
    main()