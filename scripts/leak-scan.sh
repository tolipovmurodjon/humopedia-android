#!/usr/bin/env bash
# Public repo: fail on anything that looks like a secret, a phone number, an email or a Brilliant asset. Prints file names only.
# usage: scripts/leak-scan.sh [staged|all]   (staged = the index, all = every tracked file; CI runs all)
set -u
mode=${1:-staged}
if [ "$mode" = staged ]; then files=$(git diff --cached --name-only --diff-filter=ACMR); else files=$(git ls-files); fi
bad=0
for f in $files; do
  case "$f" in
    *.env|.env*|*secret*|*token*|*credential*|*google-services.json|*.jks|*.keystore|keystore.properties|*.p12) echo "LEAK name: $f"; bad=1; continue;; esac
  case "$f" in *brilliant*/*|*Brilliant*) echo "LEAK brilliant asset: $f"; bad=1; continue;; esac
  case "$f" in *.png|*.jpg|*.webp|*.mp3|*.ttf|*.jar|*.apk|*.gif|*.ico) continue;; esac
  [ "$f" = scripts/leak-scan.sh ] && continue
  [ -f "$f" ] || continue
  if grep -vE 'git@github\.com' "$f" | grep -qIE 'private_key|AIza[0-9A-Za-z_-]{20,}|sk-[A-Za-z0-9]{20,}|xi-api-key|ghp_[A-Za-z0-9]{20,}|github_pat_|\+998[ 0-9]{9,}|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}'; then echo "LEAK content: $f"; bad=1; fi
done
[ $bad = 0 ] && echo "leak scan: 0 hits" || { echo "leak scan FAILED"; exit 1; }
