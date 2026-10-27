package ke.tahadhari

import ke.tahadhari.data.ForecastCache
import ke.tahadhari.data.store.FileKeyValueStore
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileKeyValueStoreTest {
    private val dir: File = Files.createTempDirectory("tahadhari-test").toFile()
    private val store = FileKeyValueStore(dir.path)

    @AfterTest fun cleanUp() { dir.deleteRecursively() }

    @Test fun roundTripOverwriteAndRemove() = runTest {
        assertNull(store.get("forecast-47"))
        assertTrue(store.put("forecast-47", "one"))
        assertTrue(store.put("forecast-47", "two"))
        assertEquals("two", store.get("forecast-47"))
        assertFalse(File(dir, "forecast-47.json.tmp").exists())
        store.remove("forecast-47")
        assertNull(store.get("forecast-47"))
    }

    @Test fun keysAreSanitised() = runTest {
        store.put("../evil key", "x")
        assertTrue(File(dir, "___evil_key.json").exists())
        assertEquals("x", store.get("../evil key"))
    }

    @Test fun corruptFileIsIgnoredByCache() = runTest {
        File(dir, "forecast-47.json").writeText("\u0000\u0001garbage")
        assertNull(ForecastCache(store).read(47))
    }

    @Test fun unwritableDirectoryFailsQuietly() = runTest {
        val blocker = File(dir, "blocker").apply { writeText("a file, not a directory") }
        val broken = FileKeyValueStore(File(blocker, "sub").path)
        assertFalse(broken.put("k", "v"))
        assertNull(broken.get("k"))
    }
}
