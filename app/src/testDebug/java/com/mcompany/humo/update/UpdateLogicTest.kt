package com.mcompany.humo.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UpdateLogicTest {
    private val good = """{"versionCode":43,"versionName":"3.0.0-alpha09","apk":"humopedia-43.apk","sha256":"ABC123","builtAt":"2026-10-31","notes":"x","extra":1}"""
    private val abcDigest = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

    @Test fun `parses a manifest and ignores unknown fields`() {
        val m = UpdateLogic.parse(good)
        assertNotNull(m)
        assertEquals(43, m!!.versionCode)
        assertEquals("humopedia-43.apk", m.apk)
    }

    @Test fun `garbage is null, never a crash`() {
        assertNull(UpdateLogic.parse("<html>502</html>"))
        assertNull(UpdateLogic.parse(""))
        assertNull(UpdateLogic.parse("""{"versionCode":"x"}"""))
    }

    @Test fun `only a higher version code is newer`() {
        val m = UpdateLogic.parse(good)!!
        assertTrue(UpdateLogic.isNewer(42, m))
        assertFalse(UpdateLogic.isNewer(43, m))
        assertFalse("a stale host must never downgrade", UpdateLogic.isNewer(44, m))
    }

    @Test fun `apk names that could escape the folder are refused`() {
        assertTrue(UpdateLogic.safeApkName("humopedia-43.apk"))
        assertFalse(UpdateLogic.safeApkName("../humopedia.apk"))
        assertFalse(UpdateLogic.safeApkName("a/b.apk"))
        assertFalse(UpdateLogic.safeApkName("a\\b.apk"))
        assertFalse(UpdateLogic.safeApkName("humopedia.sh"))
        assertFalse(UpdateLogic.safeApkName(""))
    }

    @Test fun `sha256 matches the known digest and is case blind`() {
        val f = File.createTempFile("humo", ".bin").apply { writeText("abc"); deleteOnExit() }
        val h = UpdateLogic.sha256Hex(f)
        assertEquals(abcDigest, h)
        assertTrue(UpdateLogic.sameHash(h.uppercase(), h))
        assertFalse(UpdateLogic.sameHash(h, h.replaceFirst('b', 'c')))
    }

    @Test fun `a download counts as ready only when its bytes match the published hash`() {
        val dir = File.createTempFile("humo-ready", "").let { it.delete(); it.mkdirs(); it.deleteOnExit(); it }
        val m = UpdateManifest(43, "3.0.0-alpha09", "humopedia-43.apk", abcDigest)
        assertNull("nothing downloaded yet", UpdateLogic.verifiedApk(dir, m))
        File(dir, "humopedia-43.apk").writeText("abd")
        assertNull("a corrupt or swapped file is never installed", UpdateLogic.verifiedApk(dir, m))
        File(dir, "humopedia-43.apk").writeText("abc")
        assertEquals(File(dir, "humopedia-43.apk"), UpdateLogic.verifiedApk(dir, m))
        assertNull("an unsafe name is refused even when the file exists", UpdateLogic.verifiedApk(dir, m.copy(apk = "../humopedia-43.apk")))
    }

    @Test fun `the ready manifest round-trips through its file text`() {
        val m = UpdateManifest(43, "3.0.0-alpha09", "humopedia-43.apk", "ff", "2026-10-31", "notes")
        assertEquals(m, UpdateLogic.parse(UpdateLogic.encode(m)))
    }

    @Test fun `the host is asked at most every ten minutes, by the injected clock`() {
        var now = 1_000_000L
        val clock = Clock { now }
        assertTrue("never asked before", UpdateLogic.shouldCheck(0L, clock))
        val asked = now
        now += UpdateLogic.MIN_GAP_MILLIS - 1
        assertFalse(UpdateLogic.shouldCheck(asked, clock))
        now += 1
        assertTrue(UpdateLogic.shouldCheck(asked, clock))
    }
}
