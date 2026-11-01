package com.mcompany.humo.ui.lesson

/**
 * The lesson's frame-cost fixes, on by default so the phone run can compare on and off (debug flag: the file files/perf_v2_off turns
 * them off, scripts/phone-perf.sh off). What it switches: the jet shadow is two copies instead of five while moving, the vapour puffs
 * are one cached path each with at most 14 alive, and the board no longer publishes the point's position to the accessibility tree on
 * every pointer move ([boardProbe] is for tests only).
 */
/** Debug-only counters: how many times the board, Humo and the bubble were drawn or composed, logged every 2 s as `HumoPerf counts`. */
internal object PerfCounters {
    @JvmField var board = 0
    @JvmField var humoDraw = 0
    @JvmField var header = 0
    private var started = false
    fun startLogging() {
        if (started) return
        started = true
        Thread({
            var b = 0; var h = 0; var t = 0
            while (true) {
                Thread.sleep(2000)
                if (board != b || humoDraw != h || header != t) {
                    android.util.Log.i("HumoPerf", "counts per 2s: board draws=${board - b} humo draws=${humoDraw - h} bubble compositions=${header - t}")
                    b = board; h = humoDraw; t = header
                }
            }
        }, "perf-counts").apply { isDaemon = true }.start()
    }
}

internal object PerfV2 {
    @Volatile var on = true
    @Volatile var boardProbe = false
}
