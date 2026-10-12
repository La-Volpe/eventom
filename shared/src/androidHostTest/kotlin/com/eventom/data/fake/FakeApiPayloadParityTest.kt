package com.eventom.data.fake

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * The resource files under `src/commonMain/resources/api/` are the canonical
 * fixtures; `FakeApiPayloads` is what actually gets served at runtime. This
 * test fails the build the moment they drift apart.
 *
 * Lives in the JVM-only host-test source set because reading files/classpath
 * resources is not possible in common code.
 */
class FakeApiPayloadParityTest {

    @Test
    fun embeddedPayloadsMatchResourceFiles() {
        assertEquals(
            resourceText("api/events.json").normalized(),
            FakeApiPayloads.EVENTS_JSON.normalized(),
        )
        for (id in 1L..6L) {
            assertEquals(
                resourceText("api/event_details/$id.json").normalized(),
                assertNotNull(FakeApiPayloads.eventDetailJson(id), "missing detail payload for id=$id").normalized(),
            )
        }
        assertEquals(
            resourceText("api/users.json").normalized(),
            FakeApiPayloads.USERS_JSON.normalized(),
        )
    }

    private fun String.normalized(): String = replace("\r\n", "\n").trim()

    /**
     * Reads a fixture off the test classpath (common resources land there on
     * JVM-style test runs); falls back to locating the file relative to the
     * working directory so it works from the repo root or the module dir.
     */
    private fun resourceText(path: String): String {
        FakeApiPayloads::class.java.classLoader?.getResourceAsStream(path)?.let { stream ->
            return stream.readBytes().decodeToString()
        }
        File(System.getProperty("user.dir")).let { start ->
            generateSequence(start) { it.parentFile }.forEach { dir ->
                listOf(dir, dir.resolve("shared")).forEach { candidate ->
                    val file = candidate.resolve("src/commonMain/resources").resolve(path)
                    if (file.isFile) return file.readText()
                }
            }
        }
        error("Fixture not found on classpath or filesystem: $path (working dir: ${System.getProperty("user.dir")})")
    }
}