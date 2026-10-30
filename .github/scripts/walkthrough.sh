#!/usr/bin/env bash
# Emulator smoke + critic walkthrough for the native app (NATIVE-BRIEF N0; PAGEL's emulator-smoke.sh + walkthrough.sh,
# cut to what Humopedia has). Uses the app like a child: launch, every tab, scroll the path up to the cloud bank, one
# tap on the current island starts L1.1 and the walk plays it to the end, a done island's sheet, the course chip, back, rotate, big font. Each step leaves a screenshot + UI tree in smoke-out/walk; a broken step fails the run.
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

# A screenshot only, no UI tree: while the machines fly the screen redraws every frame, so a tree dump could stall.
snap() {
  N=$((N + 1)); local name; name=$(printf "%02d-%s" "$N" "$1")
  sleep "${2:-0.2}"
  $T adb exec-out screencap -p > "$WALK/$name.png"
  log "$name"
}

# Tap the node $1, then take one or two screenshots inside the SAME device shell (names $2 / $4, delays $3 / $5 s after the
# tap): a flight lasts 2-3.6 s and an adb round trip on CI takes about as long, so only this catches the machines in the air.
tapsnap() {
  local b; b=$(dump | tr '>' '\n' | grep -E "(text|content-desc)=\"$1\"" | head -1 | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"')
  [ -n "$b" ] || fail "no \"$1\" on screen"
  local n; n=($(echo "$b" | grep -oE '[0-9]+'))
  shoot_after "$(( (n[0] + n[2]) / 2 ))" "$(( (n[1] + n[3]) / 2 ))" "$2" "${3:-0.8}" "${4:-}" "${5:-0}"
}
shoot_after() {
  N=$((N + 1)); local f1; f1=$(printf "%02d-%s" "$N" "$3"); local cmd="input tap $1 $2; sleep $4; screencap -p /sdcard/$f1.png"; local f2=""
  if [ -n "$5" ]; then N=$((N + 1)); f2=$(printf "%02d-%s" "$N" "$5"); cmd="$cmd; sleep $6; screencap -p /sdcard/$f2.png"; fi
  $T adb shell "$cmd"
  $T adb pull "/sdcard/$f1.png" "$WALK/$f1.png" > /dev/null; log "$f1"
  if [ -n "$f2" ]; then $T adb pull "/sdcard/$f2.png" "$WALK/$f2.png" > /dev/null; log "$f2"; fi
}
scrolldown() { $T adb shell input swipe $((W / 2)) $((H * 70 / 100)) $((W / 2)) $((H * 45 / 100)) 300; sleep 0.8; }

# Wait (up to ${2:-15} s) until a node with this text or content-desc is on screen.
waitfor() {
  local end=$((SECONDS + ${2:-15}))
  while [ $SECONDS -lt $end ]; do
    dump > "$WALK/_wait.xml"
    grep -q -E "(text|content-desc)=\"$1\"" "$WALK/_wait.xml" && { LAST="$WALK/_wait.xml"; return 0; }
    sleep 1
  done
  fail "timed out waiting for \"$1\""
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
has "Lesson 1.1, Translating Points, current"
for t in "Bosh sahifa" Olamlar Jang Reyting Men; do has "$t"; done

for t in Olamlar Jang Reyting Men; do
  tap "$t"; shot "tab-$t"; has "$t"; has "Tez orada"
done
tap "Bosh sahifa"; shot back-home
has "Lesson 1.1, Translating Points, current"

SIZE=$(screen_size); W=${SIZE%x*}; H=${SIZE#*x}
log "-- scroll the path up into the clouds, to the top"
$T adb shell input swipe $((W / 2)) $((H * 30 / 100)) $((W / 2)) $((H * 75 / 100)) 400
shot path-up
for i in 1 2 3 4 5 6; do $T adb shell input swipe $((W / 2)) $((H * 25 / 100)) $((W / 2)) $((H * 80 / 100)) 250; sleep 0.5; done
shot path-top-clouds
has "Kurs: Coordinate Transformations"
# The story: the current island and the six after it; everything above is under the cloud bank, not on the map.
has "Lesson 2.1, Coordinate-wise Translation, locked"
grep -q 'content-desc="Lesson 2.2, ' "$LAST" && fail "lesson 2.2 is above the cloud bank and must not be on the map"

log "-- the course chip opens the course list; Back closes it"
tap "Kurs: Coordinate Transformations"; shot course-list
has "Boshqa kurslar tez orada."
$T adb shell input keyevent 4; shot course-list-closed
has "Bosh sahifa"

log "-- leave Home and come back: the path opens at the current stone again"
tap Jang; sleep 1; tap "Bosh sahifa"; shot path-at-current
has "Lesson 1.1, Translating Points, current"

log "-- one tap on the current island starts L1.1 (no sheet); Back asks before leaving"
tap "Lesson 1.1, Translating Points, current"; shot lesson
has "Move the SR-71 3 steps to the right and 1 up."
$T adb shell input keyevent 4; shot leave-asks 0.8
has "Leave the lesson\?"
tap "Keep going"; sleep 1

log "-- play L1.1 to the end like a child: one wrong try on every graded page"
# The board's corners from the UI tree; Brilliant's graph geometry (docs/qa/l11-spec.md §3): origin (48, 308), cell
# 42.67 in a 350 px wide box.
board() { set -- $(dump | tr '>' '\n' | grep 'resource-id="board"' | head -1 | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | grep -oE '[0-9]+' | tr '\n' ' '); BX1=${1:-0}; BY1=${2:-0}; BX2=${3:-0}; [ "$BX2" -gt 0 ] || fail "no board on screen"; }
npx() { awk -v l="$BX1" -v t="$BY1" -v r="$BX2" -v x="$1" -v y="$2" 'BEGIN{s=(r-l)/350; printf "%d %d", l+(48+x*42.67)*s, t+(308-y*42.67)*s}'; }
drag() { board; local a b; a=$(npx "$1" "$2"); b=$(npx "$3" "$4"); $T adb shell input swipe $a $b 700; sleep 0.8; }
press() { tap "$1"; sleep "${2:-0.8}"; }

tapnode() { board; local a; a=$(npx "$1" "$2"); $T adb shell input tap $a; }
shot P1-start; has "Move the SR-71 3 steps to the right and 1 up."
drag 2 1 3 4; press Check; shot P1-wrong
has "Hmm, think about that again."
press "Try again"; drag 3 4 5 2; press Check; shot P1-correct
has "Good!"
press "Why\?"; shot P1-why; has "Why is this the answer\?"; press "Yes, that makes sense"
press Continue
shot P2-start
log "-- tap the jet, then a spot: the nose turns, then it flies there; the screen is caught in the air"
tapnode 1 3; snap P2-armed 0.5
board; shoot_after $(npx 6 0) P2-tapfly-a 0.5 P2-tapfly-b 0.7
sleep 5; shot P2-landed 0.5
press Check; shot P2-wrong
has "(Hmm, think about that again\.|Not quite.*)"
press "Try again"; drag 6 0 4 4; press Check; shot P2-correct
has "Continue"; press Continue
shot P3; has "Two points, each moved a steps right and b steps up."; press Continue
for P in "P4 1 2 2 2" "P5 1 3 3 1" "P6 3 2 2 3"; do
  set -- $P
  shot "$1-start" 0.5
  press "Tile $2" 0.4; press "Tile $3"; tapsnap Check "$1-flight" 0.9
  waitfor "Try again" 15; shot "$1-wrong"
  press "Try again"; scrolldown; press "Start over"; sleep 1.5
  press "Tile $4" 0.4; press "Tile $5"; tapsnap Check "$1-flight-2" 0.9
  waitfor "Continue" 15; shot "$1-correct"
  press Continue
done
shot P7; press Play 1.5; press Continue
# The splash shows for 1.7 s; the UI tree is read after it, so the check is on the first item.
shot skill-splash 0.3; sleep 2.5; shot skill-SC1 0.5; has "I don(&apos;|.)t know"
scrolldown; press "Tile 3" 0.4; press "Tile 2"; tapsnap Check skill-SC1-flight 0.9
sleep 6
press "I don(&apos;|.)t know" 1.5
scrolldown; press "Tile 1" 0.4; press "Tile 4"; tapsnap Check skill-SC3-flight 0.9
waitfor "That was tricky!" 20; shot skill-score; has "That was tricky!"
press "Skip review"; shot skill-retry; has "Retry skill check"
tapsnap "Skip retry" mach3-a 1.3 mach3-b 1.2
sleep 3; shot lesson-end; has "Lesson complete"; has "75 sparks earned."
press Continue 2; shot back-from-lesson
has "Lesson 1.1, Translating Points, done"
has "Lesson 1.2, Negative Translations, current"
has "Olmos: 75"
shot jet-roll-done 1.5

log "-- L1.2 Negative Translations, the next island: one wrong try on every graded page, then a perfect skill check"
tap "Lesson 1.2, Negative Translations, current"; shot l12-lesson
has "Complete the translation to send the SR-71s to the targets."
for P in "P1 -5 2" "P2 -5 -4" "P3" "P4 -5 -1" "P5 -5 3" "P6 -5 -3" "P7"; do
  set -- $P
  if [ $# -eq 1 ]; then shot "l12-$1"; press Play 1.5; press Continue; continue; fi
  press "Tile $2"; tapsnap Check "l12-$1-flight" 0.9
  waitfor "Try again" 15; shot "l12-$1-wrong"
  press "Try again"; scrolldown; press "Start over"; sleep 1.5
  press "Tile $3"; press Check 0.1
  waitfor "Continue" 15; shot "l12-$1-correct"
  press Continue
done
sleep 2.5; shot l12-SC1; has "I don(&apos;|.)t know"
scrolldown; press "Tile -2"; press Check 0.1; sleep 6
scrolldown; press "Tile 3"; press Check 0.1; sleep 6
scrolldown; press "Tile -2" 0.4; press "Tile 0"; press Check 0.1
waitfor "Perfect!" 20; shot l12-score; has "Perfect!"
tapsnap Continue l12-mach3 1.6; sleep 2; shot l12-end; has "Lesson complete"; has "75 sparks earned."
press Continue 2; shot back-from-l12
has "Lesson 1.2, Negative Translations, done"
has "Lesson 1.3, Translating Shapes, current"
has "Olmos: 150"

log "-- the next island is not built yet: its sheet says so, with no fake button"
tap "Lesson 1.3, Translating Shapes, current"; shot coming-sheet
has "Bu dars tez orada qoʻshiladi."
$T adb shell input keyevent 4; sleep 1

log "-- a done island opens its sheet to replay; Back closes it"
tap "Lesson 1.2, Negative Translations, done"; shot done-sheet
has "Takrorlash"
has "Tugatilgan"
$T adb shell input keyevent 4; shot done-sheet-closed
has "Lesson 1.3, Translating Shapes, current"

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
has "Lesson 1.3, Translating Shapes, current"

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
