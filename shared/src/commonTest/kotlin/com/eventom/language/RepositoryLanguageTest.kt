package com.eventom.language

import com.eventom.data.remote.EventRemoteDataSource
import com.eventom.data.repository.EventRepositoryImpl
import com.eventom.domain.model.Event
import com.eventom.domain.model.EventCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.time.Duration.Companion.hours

/** Scripted remote that fakes the backend's Accept-Language response switch. */
private class ScriptedLanguageRemote : EventRemoteDataSource {
    var served: List<Event> = emptyList()
    override suspend fun fetchEvents(): List<Event> = served
}

/**
 * Language toggle → re-emission wiring (bilingual 1/4): the repository is
 * language-invisible; the remote mirrors the backend's per-language payload
 * families. The flow: set the persisted language, refresh (Accept-Language
 * requests the matching family), next observeEvents() carries the titles in
 * that language. Reuses the seeded saved-flag path — nothing hidden.
 */
class RepositoryLanguageTest {

    private val seed: Instant = Instant.parse("2027-06-12T19:30:00Z")

    private fun event(id: Long, title: String, city: String) = Event(
        id = id,
        title = title,
        summary = "",
        category = EventCategory.CONCERTS,
        startTime = seed + 1.hours,
        city = city,
        venue = "Halle am Ufer",
        priceFrom = 59.90,
    )

    private fun repository(remote: EventRemoteDataSource, testScope: CoroutineScope) =
        EventRepositoryImpl(remote = remote, scope = testScope)

    private fun setting() = LanguageSetting(
        store = MemoryLanguageStore(),
        mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    )

    @Test
    fun togglingLanguageThenRefreshingSwitchesTitleFamily() = runTest {
        val deEvent = event(1, "Aurora Nights — Sommer Tour 2027", "Berlin")
        val enEvent = event(1, "Aurora Nights — Summer Tour 2027", "Berlin")
        val languageSetting = setting()
        val remote = ScriptedLanguageRemote()
        val backendScope = backgroundScope
        remote.served = listOf(deEvent)
        val eventRepository = repository(remote, backendScope)
        languageSetting.choose(AppLanguage.DE)
        eventRepository.refresh()
        assertEquals(deEvent.title, eventRepository.observeEvents().first().first().title)

        // Toggle + re-refresh: the backend supplies the family for the new header.
        languageSetting.choose(AppLanguage.EN)
        remote.served = listOf(enEvent)
        eventRepository.refresh()
        assertEquals(EffectiveLanguage.EN, languageSetting.effective.value)
        assertEquals(enEvent.title, eventRepository.observeEvents().first().first().title)
    }
}