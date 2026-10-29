package com.eventom.language

import android.content.Context
import com.eventom.data.http.httpClientEngine
import com.eventom.domain.model.AuthUser
import com.eventom.session.SessionStore
import io.ktor.client.engine.okhttp.OkHttpEngine
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * The ANDROID side of the shared expect/actual seams, which no commonTest can
 * reach: [`defaultLanguageStore`] needs a real `Context`, and the documented
 * storage contract ("eventom_prefs" / "app_language", AGENTS.md #24) is only
 * observable through `SharedPreferences`.
 *
 * Robolectric rather than an instrumented test on purpose: connected tests do
 * not feed Kover, so a Context-backed assertion here is the only way these
 * lines are measured at all.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidLanguageActualsTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun attachHost() {
        // Robolectric reuses one sandbox per config, so the object's state can
        // outlive a single test method — always re-attach.
        LanguageContextHost.attach(context)
    }

    @After
    fun detachHost() {
        setHostContext(null)
    }

    /** `context` is private in the production object; the unattached path can
     *  only be exercised by clearing it. */
    private fun setHostContext(value: Context?) {
        val field = LanguageContextHost::class.java.getDeclaredField("context")
        field.isAccessible = true
        field.set(LanguageContextHost, value)
    }

    @Test
    fun attachMakesTheApplicationContextAvailable() {
        assertEquals(context, LanguageContextHost.require())
    }

    /** Failing loudly beats a silent fallback: a missing `attach()` in
     *  `EvenTomApplication.onCreate` must not degrade into English-only content. */
    @Test
    fun requireBeforeAttachFailsWithAnActionableMessage() {
        setHostContext(null)

        val error = assertFailsWith<IllegalStateException> { LanguageContextHost.require() }

        assertEquals(
            "LanguageContextHost not attached — EvenTomApplication must call attach() in onCreate",
            error.message,
        )
    }

    /** Pins the storage contract both platforms document: Android writes
     *  SharedPreferences("eventom_prefs")/key "app_language", iOS NSUserDefaults. */
    @Test
    fun defaultStoreRoundTripsThroughTheDocumentedPreferencesFile() {
        val store = defaultLanguageStore()

        store.writeStoredLanguage("de")

        assertEquals("de", store.readStoredLanguage())
        val prefs = context.getSharedPreferences("eventom_prefs", Context.MODE_PRIVATE)
        assertEquals("de", prefs.getString("app_language", null), "the key/file names are part of the contract")

        store.writeStoredLanguage("")

        assertEquals("", store.readStoredLanguage(), "clearing the choice must round-trip as empty, not as null")
    }

    /** No stored choice yet: the read must degrade to "" (SYSTEM), never crash. */
    @Test
    fun freshInstallReadsEmptyString() {
        val fresh = context.getSharedPreferences("eventom_prefs", Context.MODE_PRIVATE)
        fresh.edit().clear().commit()

        assertEquals("", defaultLanguageStore().readStoredLanguage())
    }

    /** The shared HTTP client must get a real engine on Android (OkHttp), not a
     *  null/unsupported one — the expect/actual factory is the seam. */
    @Test
    fun httpEngineFactoryReturnsTheOkHttpEngine() {
        // OkHttp is the factory object; create() yields the engine instance.
        assertIs<OkHttpEngine>(httpClientEngine())
    }

    /** `LanguageSetting()` with NO arguments resolves the platform store and the
     *  Main dispatcher — the production wiring the injected-scope tests skip. */
    @Test
    fun languageSettingDefaultsResolveThePlatformStore() {
        val setting = LanguageSetting() // no store, no scope, no override

        assertEquals(AppLanguage.SYSTEM, setting.persistedChoice.value)
        setting.choose(AppLanguage.EN)
        assertEquals("en", context.getSharedPreferences("eventom_prefs", Context.MODE_PRIVATE).getString("app_language", null))
        assertEquals(EffectiveLanguage.EN, setting.effective.value)
    }

    /** Same for `SessionStore`: production leaves the scope unset. */
    @Test
    fun sessionStoreDefaultsBuildTheirOwnScope() {
        val session = SessionStore()

        session.signIn(AuthUser(name = "Demo Fan", email = "demo@eventom.dev"))

        assertEquals(true, session.isSignedIn)
    }
}
