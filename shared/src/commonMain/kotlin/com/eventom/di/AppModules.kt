package com.eventom.di

import com.eventom.data.fake.BackendConfig
import com.eventom.data.fake.fakeBackendEngine
import com.eventom.data.http.httpClientEngine
import com.eventom.data.remote.EventRemoteDataSource
import com.eventom.data.remote.KtorEventRemoteDataSource
import com.eventom.domain.repository.EventDetailRemoteDataSource
import com.eventom.data.remote.KtorEventDetailRemoteDataSource
import com.eventom.domain.repository.AuthRemoteDataSource
import com.eventom.data.remote.KtorAuthRemoteDataSource
import com.eventom.data.repository.EventRepositoryImpl
import com.eventom.domain.repository.EventRepository
import com.eventom.domain.usecase.FilterEventsUseCase
import com.eventom.domain.usecase.FetchEventDetailUseCase
import com.eventom.domain.usecase.LoginUseCase
import com.eventom.domain.usecase.ObserveEventsUseCase
import com.eventom.domain.usecase.RefreshEventsUseCase
import com.eventom.domain.usecase.ToggleEventSavedUseCase
import com.eventom.presentation.AuthViewModel
import com.eventom.presentation.EventDetailViewModel
import com.eventom.presentation.EventsViewModel
import com.eventom.session.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.KoinApplication
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin wiring for the whole shared graph. Both apps start this once at
 * launch; nothing platform-specific is needed beyond the HTTP engine.
 *
 * To keep this file a stable "wiring map": new classes get registered
 * under the matching module below, in declaration order.
 */

val dataModule = module {
    // HTTP engine seam: the offline fake backend (M1, data/fake/) or the
    // per-platform engines (OkHttp / Darwin). See BackendConfig for the
    // one-line swap once the real EvenTom API exists.
    single<HttpClientEngine> {
        if (BackendConfig.useFakeBackend) fakeBackendEngine() else httpClientEngine()
    }
    single {
        HttpClient(get()) {
            // Non-2xx responses throw ClientRequestException/ServerResponseException
            // instead of feeding error bodies to the (de)serializers — login (M2)
            // relies on this for wrong credentials, detail (M4) for unknown ids.
            expectSuccess = true
            install(ContentNegotiation) {
                json()
            }
            // Explicit timeout policy — replaces cross-platform engine-default
            // differences (OkHttp vs NSURLSession) with one behavior; see
            // AGENTS.md gotcha #15.
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
            }
            // Logging plugin: request/response metadata only (level INFO —
            // bodies are never logged, so credentials stay out of the log);
            // a no-op while the fake demo backend is active (it must not
            // spam logcat). See AGENTS.md gotcha #15.
            if (!BackendConfig.useFakeBackend) {
                install(Logging) {
                    level = LogLevel.INFO
                }
            }
        }
    }
    single<EventRemoteDataSource> { KtorEventRemoteDataSource(get()) }
    // Consumed by the detail screen (M4) and the login screen (M2); wired
    // and tested here so the fake backend is complete from day one.
    single<EventDetailRemoteDataSource> { KtorEventDetailRemoteDataSource(get()) }
    single<AuthRemoteDataSource> { KtorAuthRemoteDataSource(get()) }
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    single<EventRepository> { EventRepositoryImpl(get(), get()).apply { bootstrap() } }
}

val domainModule = module {
    single { ObserveEventsUseCase(get()) }
    single { RefreshEventsUseCase(get()) }
    single { ToggleEventSavedUseCase(get()) }
    single { FilterEventsUseCase() }
    single<LoginUseCase> { LoginUseCase(get()) }
    single<FetchEventDetailUseCase> { FetchEventDetailUseCase(get()) }
}

val sessionModule = module {
    // In-memory sign-in state; every screen can ask who is logged in.
    single<SessionStore> { SessionStore() }
}

val presentationModule = module {
    single { EventsViewModel(get(), get(), get(), get()) }
    single { AuthViewModel(get(), get()) }
    // Keyed on the expression type; the detail VM loads by id on demand.
    single { EventDetailViewModel(get(), get()) }
}

/** Everything the app needs, in one list — use with [startKoin] or Koin for apps. */
val allSharedModules: List<Module> = listOf(dataModule, domainModule, sessionModule, presentationModule)

/**
 * Convenience for the native applications: start Koin once and access the
 * graph through typed extensions below, so Swift code doesn't have to deal
 * with `get(KClass)` at all.
 */
object SharedKoin {
    private var koinApplication: KoinApplication? = null

    /** Safe to call more than once (idempotent). */
    fun start() {
        if (koinApplication == null) {
            koinApplication = startKoin {
                modules(allSharedModules)
            }
        }
    }

    fun stop() {
        koinApplication?.close()
        koinApplication = null
    }

    /** Typed accessors — the only Swift-facing entry points into the graph. */
    fun eventsViewModel(): EventsViewModel = resolve()

    fun authViewModel(): AuthViewModel = resolve()

    fun eventDetailViewModel(): EventDetailViewModel = resolve()

    fun sessionStore(): SessionStore = resolve()

    private inline fun <reified T : Any> resolve(): T = koinApplication?.koin?.get()
        ?: error("SharedKoin.start() has not been called yet")
}