#!/usr/bin/env bash
# Emulator smoke + critic walkthrough for the native app (NATIVE-BRIEF N0; PAGEL's emulator-smoke.sh + walkthrough.sh,
# cut to what Humopedia has). Uses the app like a child: launch, every tab, scroll the path, one tap on the current
# stone, back, rotate, big font. Each step leaves a screenshot + UI tree in smoke-out/walk; a broken step fails the run.
set -u
T="timeout 60"
PKG=com.mcompany.humo
OUT=smoke-out
WALK=$OUT/walk
mkdir -p "$WALK"
APK=$(ls apk/*.apk | head -1)
N=0
log() { echo "$*" | tee -a "$WALK/steps.txt"; }
fail() { log "WALKTHROUGH FAILED: $*"; timeout 30 adb logcat -d > "$OUT/logcat-at-failure.txt"; exit 1; }

# The first glass frame builds its shaders: 11 s on CI's software GPU for PAGEL. Wait for this process's
# "glass drawn" line (GlassStage) before touching the screen, so a tap never waits behind that frame.
wait_glass() {
  local pid; pid=$($T adb shell pidof "$PKG" | tr -d '\r')
  for i in $(seq 1 45); do
    $T adb logcat -d -s HumoGlass:I | grep -q " $pid .*glass drawn" && break
    sleep 1
  done
  log "glass: $($T adb logcat -d -s HumoGlass:I | grep " $pid " | sed 's/.*HumoGlass: //' | paste -sd ';' -)"
  sleep 1
}

dump() {
  for i in 1 2 3; do $T adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1 && break; sleep 2; done
  timeout 20 adb exec-out cat /sdcard/ui.xml < /dev/null
}

shot() {
  N=$((N + 1)); local name; name=$(printf "%02d-%s" "$N" "$1")
  sleep "${2:-1.5}"
  $T adb exec-out screencap -p > "$WALK/$name.png"
  dump > "$WALK/$name.xml"
  log "$name"
  grep -q "isn't responding" "$WALK/$name.xml" && fail "ANR dialog on step $name"
  LAST="$WALK/$name.xml"
}

# Tap the centre of the first node whose text or content-desc equals $1.
tap() {
  local b; b=$(dump | tr '>' '\n' | grep -E "(text|content-desc)=\"$1\"" | head -1 | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"')
  [ -n "$b" ] || fail "no \"$1\" on screen"
  set -- $(echo "$b" | grep -oE '[0-9]+' | tr '\n' ' ')
  $T adb shell input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
}

has() { grep -q -E "(text|content-desc)=\"$1\"" "$LAST" || fail "step $(basename "$LAST") has no \"$1\""; }

screen_size() { $T adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1; }

log "== root adb, install $APK"
$T adb root > /dev/null; $T adb wait-for-device
$T adb install -r -g "$APK" || fail "install"

log "== launch, alive after 20 s"
$T adb logcat -c
$T adb shell am start -W -n "$PKG/.MainActivity" | tee "$OUT/am-start.txt"
wait_glass
sleep 10
$T adb logcat -d > "$OUT/logcat-launch.txt"
[ -n "$($T adb shell pidof "$PKG" | tr -d '\r')" ] || fail "the app is not running after launch"
grep -q "Process: $PKG, PID" "$OUT/logcat-launch.txt" && fail "crash at launch (logcat-launch.txt)"
grep -q "ANR in $PKG" "$OUT/logcat-launch.txt" && fail "ANR at launch"
log "ok: running, no crash, no ANR"

$T adb shell dumpsys gfxinfo "$PKG" reset > /dev/null
shot home
has "Path"
has "Level 3, Rotations, current"
for t in "Bosh sahifa" Olamlar Jang Reyting Men; do has "$t"; done

for t in Olamlar Jang Reyting Men; do
  tap "$t"; shot "tab-$t"; has "$t"; has "Tez orada"
done
tap "Bosh sahifa"; shot back-home
has "Level 3, Rotations, current"

SIZE=$(screen_size); W=${SIZE%x*}; H=${SIZE#*x}
log "-- scroll the path up into the clouds, to the course title"
$T adb shell input swipe $((W / 2)) $((H * 30 / 100)) $((W / 2)) $((H * 75 / 100)) 400
shot path-up
for i in 1 2 3 4 5 6; do $T adb shell input swipe $((W / 2)) $((H * 25 / 100)) $((W / 2)) $((H * 80 / 100)) 250; sleep 0.5; done
shot path-top
has "Coordinate Transformations"
has "Level 14, Stretching, locked"

log "-- leave Home and come back: the path opens at the current stone again"
tap Jang; sleep 1; tap "Bosh sahifa"; shot path-at-current
has "Level 3, Rotations, current"

log "-- one tap on the current stone opens its lesson"
tap "Level 3, Rotations, current"; shot lesson
has "Lesson 3.1 · Rotation Around the Origin"
$T adb shell input keyevent 4; shot back-from-lesson
has "Level 3, Rotations, current"
has "Bosh sahifa"

log "-- rotate"
$T adb shell settings put system accelerometer_rotation 0
$T adb shell settings put system user_rotation 1; shot landscape 3
has "Bosh sahifa"
$T adb shell settings put system user_rotation 0; shot portrait-again 3
has "Bosh sahifa"

log "-- big font (1.5x)"
$T adb shell settings put system font_scale 1.5; shot font150 3
has "Bosh sahifa"
tap Men; shot font150-me; has "Tez orada"
$T adb shell settings put system font_scale 1.0
tap "Bosh sahifa"; shot font100-home 2

log "-- Back on Home leaves the app; Back on another tab goes Home first"
tap Jang; sleep 1; $T adb shell input keyevent 4; shot back-goes-home
has "Level 3, Rotations, current"

log "-- frame stats"
$T adb shell dumpsys gfxinfo "$PKG" | grep -E "Total frames|Janky frames|percentile|Number Slow|Number Frozen" | tee -a "$WALK/steps.txt"

log "-- forced test crash is written to files/crashes/"
$T adb shell am force-stop "$PKG"
$T adb shell run-as "$PKG" rm -rf files/crashes
$T adb shell am start -n "$PKG/.MainActivity" --ez humo.test_crash true
FILES=""
for i in $(seq 1 40); do
  FILES=$($T adb shell run-as "$PKG" ls files/crashes 2>/dev/null | tr -d '\r')
  case "$FILES" in *crash-*.json*) break;; esac
  sleep 1
done
case "$FILES" in *crash-*.json*) ;; *) fail "no crash file after the forced crash";; esac
FIRST=$(echo "$FILES" | tr ' ' '\n' | grep '^crash-.*\.json$' | head -1)
timeout 20 adb exec-out run-as "$PKG" cat "files/crashes/$FIRST" > "$OUT/crash-file.json" < /dev/null
grep -q "Humo test crash" "$OUT/crash-file.json" || fail "crash file does not hold the test crash"
log "ok: CrashRecorder wrote the forced crash"

log "== walkthrough passed ($N steps)"
