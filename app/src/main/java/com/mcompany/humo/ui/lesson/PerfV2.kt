package com.mcompany.humo.ui.lesson

/**
 * The lesson's frame-cost fixes, on by default so the phone run can compare on and off (debug flag: the file files/perf_v2_off turns
 * them off, scripts/phone-perf.sh off). What it switches: the jet shadow is two copies instead of five while moving, the vapour puffs
 * are one cached path each with at most 14 alive, and the board no longer publishes the point's position to the accessibility tree on
 * every pointer move ([boardProbe] is for tests only).
 */
internal object PerfV2 {
    @Volatile var on = true
    @Volatile var boardProbe = false
}
