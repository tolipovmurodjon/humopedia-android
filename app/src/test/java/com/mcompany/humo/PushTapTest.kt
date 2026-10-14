package com.mcompany.humo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// A launch counts as "opened from a push" only when all three keys the server sends are there.
class PushTapTest {

    @Test
    fun readsTheThreeKeysTheServerSends() {
        assertEquals(PushTap("abc123def456", "B", "smart"), PushTap.from("abc123def456", "B", "smart"))
    }

    @Test
    fun aPlainLaunchOrAConsolePushIsNotATap() {
        assertNull(PushTap.from(null, null, null))
        assertNull(PushTap.from("abc123def456", null, "smart"))
        assertNull(PushTap.from("", "B", "smart"))
    }
}
