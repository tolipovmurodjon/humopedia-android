package com.mcompany.humo.ui.lesson

import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

/**
 * The eight playtest events (Boss, 2026-11-01), one JSON line each in files/playtest.jsonl on the phone, with a random session id and
 * nothing that names the child: page_time, retry, preview_count, hesitation (over 5 s idle), quit_point, help_tap, one_more, replay.
 * Nothing leaves the phone.
 */
internal object PlaytestLog {
    val EVENTS = listOf("page_time", "retry", "preview_count", "hesitation", "quit_point", "help_tap", "one_more", "replay")
    val session: String = UUID.randomUUID().toString().take(8)
    @Volatile private var file: File? = null
    /** Tests write on the calling thread so they can read the file at once. */
    @Volatile internal var sync = false
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "playtest-log").apply { isDaemon = true } }

    fun attach(dir: File) { file = File(dir, "playtest.jsonl") }

    internal fun line(event: String, page: String, value: Long, at: Long = System.currentTimeMillis()): String =
        "{\"s\":\"$session\",\"e\":\"$event\",\"p\":\"$page\",\"v\":$value,\"t\":$at}"

    fun log(event: String, page: String, value: Long = 0L) {
        require(event in EVENTS) { "unknown playtest event $event" }
        val f = file ?: return
        val l = line(event, page, value) + "\n"
        if (sync) f.appendText(l) else io.execute { runCatching { f.appendText(l) } }
    }
}
