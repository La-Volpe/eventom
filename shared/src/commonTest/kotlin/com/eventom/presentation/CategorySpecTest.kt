package com.eventom.presentation

import com.eventom.domain.model.EventCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * [categorySpecs] is the one editable source of truth for poster fallbacks:
 * `tools/generate_posters.py` PARSES this file to emit the Android
 * `drawable-nodpi` PNGs and the iOS asset catalog (AGENTS.md #12/#17). So the
 * invariants below are asset-pipeline contracts, not cosmetics — a dashes-free
 * stem or a missing category silently breaks one platform's artwork.
 */
class CategorySpecTest {
    @Test
    fun everyCategoryHasASpec() {
        EventCategory.entries.forEach { category ->
            assertTrue(category in categorySpecs, "no CategorySpec for $category")
        }
        assertEquals(
            EventCategory.entries.size,
            categorySpecs.size,
            "categorySpecs has entries that are not EventCategory values",
        )
    }

    /** Android resource names cannot contain '-' (gotcha #12); the iOS asset
     *  name is derived by PascalCasing this stem, so anything beyond
     *  lowercase/underscore/digit would not map back. */
    @Test
    fun posterStemsAreResourceSafeAndDistinct() {
        // Local helper keeps the assertion readable without a multiline
        // initializer (ktlint: multiline-expression-wrapping).
        fun stemOf(
            category: EventCategory,
            spec: CategorySpec,
        ): String {
            assertTrue(
                Regex("^[a-z0-9]+(_[a-z0-9]+)*$").matches(spec.posterStem),
                "${category.name} stem '${spec.posterStem}' is not lowercase snake_case",
            )
            return spec.posterStem
        }
        val stems = categorySpecs.map { (category, spec) -> stemOf(category, spec) }
        assertEquals(stems.size, stems.toSet().size, "two categories share a poster stem: $stems")
    }

    /** The generator reads these as 32-bit ARGB gradient stops (a Kotlin `Long`
     *  literal is only the storage type — alpha lives in bits 24..31, not
     *  56..63). A missing alpha byte renders the poster fully transparent on
     *  both platforms. */
    @Test
    fun gradientStopsAreOpaqueAndActuallyGradient() {
        val thirtyTwoBits = 1L shl 32
        categorySpecs.forEach { (category, spec) ->
            listOf("colorTop" to spec.colorTop, "colorBottom" to spec.colorBottom).forEach { (field, color) ->
                assertTrue(color in 0 until thirtyTwoBits, "${category.name} $field does not fit ARGB32")
                assertEquals(0xFF, ((color ushr 24) and 0xFF).toInt(), "${category.name} $field alpha")
            }
            assertNotEquals(spec.colorTop, spec.colorBottom, "${category.name} gradient is flat")
        }
    }

    /** Swift reaches the data through the typed accessor, not the raw map
     *  (Swift bridges functions, not top-level vals). */
    @Test
    fun accessorReturnsTheMapEntry() {
        EventCategory.entries.forEach { category ->
            val expected = categorySpecs.getValue(category)
            val actual = categorySpec(category)
            assertEquals(expected, actual, "categorySpec(${category.name}) disagrees with categorySpecs")
        }
    }
}
