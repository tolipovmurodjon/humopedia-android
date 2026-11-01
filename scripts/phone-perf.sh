#!/usr/bin/env bash
# One command on a real phone, no Gradle: installs the debug APK, plays a fixed adb input path in lesson 1.1 and prints janky %,
# p50, p90 and p99 frame time per scene, with a perfetto trace (gfx, view, am; 10 s) per scene.
#   scripts/phone-perf.sh path/to/app-debug.apk            perf_v2 ON (the default)
#   scripts/phone-perf.sh path/to/app-debug.apk off        perf_v2 OFF (to compare)
# Scenes: P1 drag (adb swipes), P4 Check flight (two jets). The Mach 3 pass has no debug deep link yet, so it is not measured.
# Needs: adb, one phone on USB, the screen on and unlocked. Output goes to phone-perf-out/<time>-<on|off>/.
set -u
APK=${1:?usage: scripts/phone-perf.sh app-debug.apk [off]}
MODE=${2:-on}
PKG=com.mcompany.humo
OUT=phone-perf-out/$(date +%H%M%S)-$MODE
mkdir -p "$OUT"

adb install -r "$APK" > /dev/null || { echo "install failed (a different signing key? uninstall once, then retry)"; exit 1; }
adb shell am force-stop $PKG
if [ "$MODE" = off ]; then adb shell run-as $PKG touch files/perf_v2_off; else adb shell run-as $PKG rm -f files/perf_v2_off; fi
adb shell am start -W -n $PKG/.MainActivity > "$OUT/am-start.txt"
echo "cold start: $(grep -o 'TotalTime: [0-9]*' "$OUT/am-start.txt") ms (perf_v2 $MODE)"
sleep 8

dump() { for i in 1 2 3; do adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1 && break; sleep 2; done; adb exec-out cat /sdcard/ui.xml; }
tap() {
  local b; b=$(dump | tr '>' '\n' | grep -E "(text|content-desc)=\"$1\"" | head -1 | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"')
  [ -n "$b" ] || { echo "no \"$1\" on screen"; exit 1; }
  set -- $(echo "$b" | grep -oE '[0-9]+' | tr '\n' ' ')
  adb shell input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
}
board() { set -- $(dump | tr '>' '\n' | grep 'resource-id="board"' | head -1 | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | grep -oE '[0-9]+' | tr '\n' ' '); BX1=${1:-0}; BY1=${2:-0}; BX2=${3:-1}; }
npx() { awk -v l="$BX1" -v t="$BY1" -v r="$BX2" -v x="$1" -v y="$2" 'BEGIN{s=(r-l)/350; printf "%d %d", l+(48+x*42.67)*s, t+(308-y*42.67)*s}'; }
drag() { board; adb shell input swipe $(npx "$1" "$2") $(npx "$3" "$4") ${5:-700}; sleep 0.6; }
press() { tap "$1"; sleep "${2:-0.8}"; }
num() { press "Key $1" 0.4; }

# janky %, p50, p90, p99 of FrameCompleted - IntendedVsync, from dumpsys gfxinfo framestats
stats() {
  adb shell dumpsys gfxinfo $PKG framestats | awk -F, '/---PROFILEDATA---/{p=!p; next} p && $1==0 && $2+0>0 && $14+0>0 {print ($14-$2)/1e6}' | sort -n > "$OUT/$1.frames"
  awk -v s="$1" '{a[NR]=$1; if ($1>16.67) j++} END{ if(!NR){print s": no frames"; exit}
    printf "%-14s frames %4d  janky %5.1f%%  p50 %5.1f ms  p90 %5.1f ms  p99 %5.1f ms\n", s, NR, 100*j/NR, a[int(NR*0.5)+1], a[int(NR*0.9)+1], a[int(NR*0.99)+1] }' "$OUT/$1.frames" | tee -a "$OUT/summary.txt"
}
trace() { adb shell "perfetto -o /data/misc/perfetto-traces/$1.pftrace -t 10s gfx view am" > /dev/null 2>&1 & TRACE_PID=$!; sleep 1; }
pull_trace() { wait $TRACE_PID 2>/dev/null; adb pull /data/misc/perfetto-traces/$1.pftrace "$OUT/$1.pftrace" > /dev/null 2>&1; }

tap "Lesson 1.1, Translating Points, current"; sleep 4

echo "-- scene P1 drag"
adb shell dumpsys gfxinfo $PKG reset > /dev/null
trace p1-drag
for i in 1 2 3 4; do drag 1 2 5 5 2200; drag 5 5 1 2 2200; done
pull_trace p1-drag
stats p1-drag

echo "-- scene P4 flight (P1 to P3 first)"
drag 1 2 5 3 700; press Check 1.5; press Continue 1
press "Continue lesson" 0.5 2>/dev/null
drag 6 0 6 5 700; press Check 1.5; press Continue 1
press Continue 1.5
num 1; num 2
adb shell dumpsys gfxinfo $PKG reset > /dev/null
trace p4-flight
press Check 0.5
sleep 9
pull_trace p4-flight
stats p4-flight

echo "files in $OUT (frames per scene, perfetto traces: open them at ui.perfetto.dev)"
