#!/usr/bin/env bash
# Fresh install of the app from Google Play on the USB phone, then a launch check. Boss's way to test a release:
# delete the app, install from Play, open it.
#   bash scripts/play_reinstall.sh 31     (31 = the versionCode Play must deliver)
# Lessons built in (2026-10-02): taps go by the button's label, never by guessed coordinates; the Play app is opened by
# package, so no "open with" chooser; Play can hand out the old version for a while after release, so it waits and taps
# Update; the notification prompt of a fresh install is answered before checking the app.
set -euo pipefail
export MSYS_NO_PATHCONV=1
WANT="${1:?usage: play_reinstall.sh <versionCode>}"
PKG="com.mcompany.humo"
ADB="${ADB:-D:/CODE/AndroidStudio/Sdk/platform-tools/adb.exe}"
OUT="${TMPDIR:-/tmp}/play-reinstall.png"

focus() { "$ADB" shell dumpsys window | grep mCurrentFocus; }
code() { "$ADB" shell dumpsys package "$PKG" 2>/dev/null | grep -m1 -o 'versionCode=[0-9]*' | cut -d= -f2; }
# Taps the centre of the first on-screen element whose text is exactly $1. Returns 1 when there is none.
tap_text() {
  "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  local b; b=$("$ADB" shell cat /sdcard/ui.xml | grep -o "text=\"$1\"[^>]*bounds=\"[^\"]*\"" | head -1 | grep -o 'bounds="[^"]*"' | grep -o '[0-9]*' | tr '\n' ' ' || true)
  set -- $b
  [ -n "${1:-}" ] || return 1
  "$ADB" shell input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
}
open_play() {
  "$ADB" shell am force-stop com.android.vending
  "$ADB" shell am start -a android.intent.action.VIEW -d "market://details?id=$PKG" -p com.android.vending >/dev/null
  sleep 8
  focus | grep -q com.android.vending || { echo "Play did not open: $(focus)"; exit 1; }
}

"$ADB" get-state >/dev/null 2>&1 || { echo "No phone over USB (debugging on, tethering off)"; exit 1; }
"$ADB" uninstall "$PKG" >/dev/null 2>&1 || true
echo "uninstalled"
for attempt in 1 2 3 4 5 6; do
  open_play
  if [ -z "$(code)" ]; then tap_text Install || { echo "no Install button"; exit 1; }; else tap_text Update || true; fi
  for _ in $(seq 1 36); do sleep 5; [ -n "$(code)" ] && break; done
  now="$(code)"
  echo "attempt $attempt: Play delivered versionCode ${now:-none}"
  [ "$now" = "$WANT" ] && break
  [ "$attempt" = 6 ] && { echo "Play still serves ${now:-nothing}, not $WANT"; exit 1; }
  sleep 30
done

"$ADB" logcat -c
"$ADB" shell input keyevent KEYCODE_HOME
# Home first, then the launcher intent, as a tap on the icon does: started while Play was still in front, the app was
# refused its notification prompt and crashed (fixed in 2.1.2, versionCode 32).
"$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 6
focus | grep -q permissioncontroller && tap_text Allow && echo "notifications allowed" && sleep 8
"$ADB" exec-out screencap -p > "$OUT"
if "$ADB" logcat -d | grep -q "FATAL EXCEPTION"; then echo "CRASH: see adb logcat"; exit 1; fi
# The app is a Trusted Web Activity: once open, Chrome's CustomTabActivity is on top.
focus | grep -q -E "CustomTabActivity|$PKG" || { echo "App not on screen: $(focus)"; exit 1; }
echo "OK: $PKG $(code) from $( "$ADB" shell dumpsys package "$PKG" | grep -m1 -o 'installerPackageName=[a-z.]*' ), on screen: $(focus | grep -o '[a-z.]*/[A-Za-z.]*' | head -1), screenshot $OUT"
