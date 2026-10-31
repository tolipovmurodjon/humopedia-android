package com.mcompany.humo.playupdate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayUpdateModelTest {
    private class FakePlay(var offer: PlayOffer) : PlayUpdateSource {
        var downloads = 0
        var installs = 0
        var report: ((PlayOffer) -> Unit)? = null
        override fun query(done: (PlayOffer) -> Unit) = done(offer)
        override fun startDownload(progress: (PlayOffer) -> Unit) { downloads++; report = progress }
        override fun installNow() { installs++ }
    }

    @Test fun `no update on offer shows no pill`() {
        val model = PlayUpdateModel(FakePlay(PlayOffer.None)).also { it.refresh() }
        assertNull(model.pillText)
    }

    @Test fun `an update on offer shows the pill but starts nothing by itself`() {
        val play = FakePlay(PlayOffer.Available)
        val model = PlayUpdateModel(play).also { it.refresh() }
        assertEquals("Yangilanish tayyor", model.pillText)
        assertEquals(0, play.downloads)
        assertEquals(0, play.installs)
    }

    @Test fun `the first tap downloads and the install waits for a second tap`() {
        val play = FakePlay(PlayOffer.Available)
        val model = PlayUpdateModel(play).also { it.refresh() }
        model.onPillTapped()
        assertEquals(1, play.downloads)
        play.report!!(PlayOffer.Downloading)
        assertNull("no pill while the download runs", model.pillText)
        model.onPillTapped()
        play.report!!(PlayOffer.Downloaded)
        assertEquals("downloaded is not installed", 0, play.installs)
        assertEquals("Oʻrnatish uchun bosing", model.pillText)
        model.onPillTapped()
        assertEquals(1, play.installs)
    }

    @Test fun `a failed download puts the first pill back`() {
        val play = FakePlay(PlayOffer.Available)
        val model = PlayUpdateModel(play).also { it.refresh() }
        model.onPillTapped()
        play.report!!(PlayOffer.Available)
        assertEquals("Yangilanish tayyor", model.pillText)
    }

    @Test fun `an update downloaded earlier is offered for install after a restart, never installed`() {
        val play = FakePlay(PlayOffer.Downloaded)
        val model = PlayUpdateModel(play).also { it.refresh() }
        assertEquals("Oʻrnatish uchun bosing", model.pillText)
        assertEquals(0, play.installs)
    }
}
