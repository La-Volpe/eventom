package com.evantom.domain.model

/**
 * Everything the detail screen (M4) will need, beyond what [Event]
 * already carries. Kept separate so the list payload stays light.
 *
 * Note: `longSummary`, not `description` — NSObject owns that name on the
 * Swift side (repo gotcha #4, enforced even with iOS out of scope).
 */
data class EventDetail(
    val id: Long,
    /** Multi-paragraph marketing text (JSON `longDescription`). */
    val longSummary: String,
    /** Act/performer names in running order. */
    val lineup: List<String>,
    /** Time of day the doors open, as the API renders it (e.g. "17:30"). */
    val doorsTime: String,
    /** Bookable ticket categories with prices, in display order. */
    val ticketCategories: List<TicketCategory>,
    /** Age policy line (e.g. "Fsk ab 6 Jahren"). Empty if unrestricted/unknown. */
    val ageInfo: String,
)

/** One bookable ticket tier on the detail screen ("Kategorie 1 – 89,90 €"). */
data class TicketCategory(
    val name: String,
    val price: Double,
)