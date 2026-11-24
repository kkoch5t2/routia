#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."

emulator=(adb -s "emulator-${EMULATOR_PORT:-5554}")
"${emulator[@]}" install app/build/outputs/apk/debug/app-debug.apk
"${emulator[@]}" install -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

run_suite() {
  local suite=$1 assertions=$2 result
  result=$("${emulator[@]}" shell am instrument -w -r -e suite "$suite" \
    app.routia.reportqa.test/app.routia.MvpInstrumentation | tr -d '\r')
  printf '%s\n' "$result"

  # adb can exit normally even when Android reports a failed instrumentation run.
  if ! grep -Eq "^INSTRUMENTATION_RESULT: stream=PASS [1-9][0-9]* $assertions assertions$" <<< "$result" \
    || ! grep -Fxq 'INSTRUMENTATION_CODE: -1' <<< "$result"; then
    echo "The $suite regression suite did not pass." >&2
    exit 1
  fi
}

run_suite focus-store 'isolated ticket/midnight-timer'
run_suite reminders 'reminder count/time/alarm/UI'
