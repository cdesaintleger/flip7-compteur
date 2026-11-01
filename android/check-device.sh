#!/usr/bin/env bash
# Preserve visual evidence even if the emulator disconnects before Gradle cleanup.
set -u
mkdir -p ../outputs
adb logcat -c
adb logcat -v threadtime > ../outputs/device-logcat.txt &
log_pid=$!
./gradlew --no-daemon connectedDebugAndroidTest &
test_pid=$!
while kill -0 "$test_pid" 2>/dev/null; do
  adb pull /data/local/tmp/flip7-screenshots ../outputs/android-screenshots 2>/dev/null || true
  sleep 2
done
wait "$test_pid"
native_test_result=$?
adb pull /data/local/tmp/flip7-screenshots ../outputs/android-screenshots 2>/dev/null || true
kill "$log_pid" 2>/dev/null || true
sudo dmesg -T | tail -60 > ../outputs/host-kernel.log
exit "$native_test_result"
