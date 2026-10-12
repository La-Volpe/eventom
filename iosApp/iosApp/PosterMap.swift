import SwiftUI
import Shared

/**
 * imageRef → asset-catalog image name ("poster_event_1" → "PosterEvent1"),
 * falling back to the category poster when the ref is unknown or nil.
 * Android twin: androidApp/PosterMap.kt (`posterResFor`).
 */
func posterImageName(imageRef: String?, category: EventCategory) -> String {
    let fallback = posterCategoryName(category)
    guard let imageRef else { return fallback }
    let candidate = imageRef.capitalizedFirstLetters()
    return UIImage(named: candidate) == nil ? fallback : candidate
}

private func posterCategoryName(_ category: EventCategory) -> String {
    category.name // wire code: "concerts", "musical-show", ...
}

extension String {
    /// "poster_musical_show" → "PosterMusicalShow".
    func capitalizedFirstLetters() -> String {
        split(separator: "_").map { $0.prefix(1).capitalized + $0.dropFirst() }.joined()
    }
}