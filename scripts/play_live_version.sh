#!/usr/bin/env bash
# Which version of the app Google Play is serving right now, read from the public store page (no key needed).
#   bash scripts/play_live_version.sh              → prints the live version, e.g. 2.1.0
#   bash scripts/play_live_version.sh --wait 2.1.1 → checks every 10 min (2 h max) until 2.1.1 is live
# A release committed with ./gradlew publishBundle stays on the old version until Google's review approves it.
set -euo pipefail
PACKAGE="com.mcompany.humo"
URL="https://play.google.com/store/apps/details?id=$PACKAGE&hl=en&gl=US"

live() {
  curl -fsS "$URL" | grep -o '\[\[\["[0-9][0-9.]*"\]\]' | head -1 | tr -d '[]"'
}

if [ "${1:-}" = "--wait" ]; then
  want="${2:?usage: --wait <version>}"
  for _ in $(seq 1 12); do
    now="$(live || true)"
    echo "$(date +%H:%M) live=${now:-unknown} want=$want"
    [ "$now" = "$want" ] && { echo "LIVE: $want is on Google Play"; exit 0; }
    sleep 600
  done
  echo "Still not live after 2 h: check Play Console → Publishing overview"; exit 1
fi
live
