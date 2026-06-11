#!/bin/bash
# Send the newest built APK to the Home Alerts Telegram channel.
# Reads bot token + chat id from the bills .env (same bot, same channel).
set -euo pipefail
cd "$(dirname "$0")/.."

source /volume1/Projects/bills/.env
APK=$(ls -t dist/household-bills-*.apk 2>/dev/null | head -1)
[ -n "$APK" ] || { echo "no APK in dist/"; exit 1; }

SIZE=$(du -h "$APK" | cut -f1)
SHA=$(sha256sum "$APK" | cut -c1-12)

curl -sf -F chat_id="${TELEGRAM_CHAT_ID}" \
     -F document=@"$APK" \
     -F caption="📱 Household Bills $(basename "$APK") · ${SIZE} · sha256 ${SHA}… — install: open on phone → allow unknown apps. Sign-in works after the GCP OAuth step (see bills-android/README.md)." \
     "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendDocument" >/dev/null \
  && echo "sent: $APK (${SIZE})"
