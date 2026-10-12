import SwiftUI
import Shared

/**
 * Maps shared `imageRef` values to asset-catalog image names — iOS-side only;
 * `shared` deliberately knows nothing about asset names. Unknown refs fall
 * back to the event's category poster; so does a nil ref (the fallback path
 * always has artwork).
 *
 * The FALLBACK STEMS come from the shared `categorySpecs` table
 * (`com.evantom.presentation.CategorySpec`) — the single editable source of
 * truth also consumed by `tools/generate_posters.py`. The display labels
 * (moved out of the domain model, T6) live here as the iOS presentation map;
 * the Android map in PosterMap.kt matches byte-for-byte (XCUITests walk the
 * rendered strings).
 */
private func pascalAssetName(_ stem: String) -> String {
    stem.capitalizedFirstLetters()
}

private func categoryFallbackName(_ category: EventCategory, thumb: Bool) -> String {
    let stem = CategorySpecKt.categorySpec(category: category).posterStem
    return pascalAssetName(thumb ? stem + "_thumb" : stem)
}

func posterImageName(imageRef: String?, category: EventCategory) -> String {
    guard let imageRef else { return categoryFallbackName(category, thumb: false) }
    let candidate = imageRef.capitalizedFirstLetters()
    return UIImage(named: candidate) == nil ? categoryFallbackName(category, thumb: false) : candidate
}

/**
 * T5: list rows decode the 480×600 thumb (~1.2MB) instead of the ~29MB hero
 * decode. Detail (hero) keeps `posterImageName`.
 */
func posterThumbImageName(imageRef: String?, category: EventCategory) -> String {
    guard let imageRef else { return categoryFallbackName(category, thumb: true) }
    let candidate = (imageRef + "_thumb").capitalizedFirstLetters()
    return UIImage(named: candidate) == nil ? categoryFallbackName(category, thumb: true) : candidate
}

/// iOS presentation label map (T6) — must match the Android map byte-for-byte.
/// Mapping runs by enum-IDENTITY first (some K/N/iOS runtimes surface `name`
/// as the raw UPPER_SNAKE enum name, which once rendered `CONCERTS` in the
/// chip row); the lowercased wire-code switch is belt-and-braces.
func categoryDisplayName(_ category: EventCategory) -> String {
    let byIdentity: [(EventCategory, String)] = [
        (EventCategory.concerts, "Konzerte"),
        (EventCategory.musicalShow, "Musical & Show"),
        (EventCategory.sports, "Sport"),
        (EventCategory.comedy, "Comedy & Humor"),
        (EventCategory.theater, "Theater & Kultur"),
        (EventCategory.leisure, "Freizeit"),
    ]
    for (candidate, label) in byIdentity where category === candidate {
        return label
    }
    switch category.name.lowercased() {
    case "concerts": return "Konzerte"
    case "musical-show": return "Musical & Show"
    case "sports": return "Sport"
    case "comedy": return "Comedy & Humor"
    case "theater": return "Theater & Kultur"
    case "leisure": return "Freizeit"
    default: return ""
    }
}

extension String {
    /// "poster_musical_show" → "PosterMusicalShow".
    func capitalizedFirstLetters() -> String {
        split(separator: "_").map { $0.prefix(1).capitalized + $0.dropFirst() }.joined()
    }
}