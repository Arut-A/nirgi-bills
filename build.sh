#!/bin/bash
# Household Bills — APK build on the NAS, inside a throttled transient container.
# Usage: ./build.sh            — assembleRelease (signed if keystore exists)
#        ./build.sh keystore   — one-time: generate signing keystore + SHA-1
#        ./build.sh sha1       — print the release keystore SHA-1 (for GCP)
#
# Heavy-build safety (DS920+/J4125): --memory=3g --cpus=2, no daemon, no
# parallel. First build downloads SDK (~600 MB) + deps into named volumes;
# incremental builds reuse them. NEVER run as a compose service.

set -euo pipefail
cd "$(dirname "$0")"

DOCKER="${DOCKER:-/usr/local/bin/docker}"
IMAGE="gradle:8.11.1-jdk17"
SDK_VOL="household-android-sdk"
GRADLE_VOL="household-gradle-cache"
PROJECT_DIR="$(pwd)"

RUN="$DOCKER run --rm \
  --memory=3g --memory-swap=4g --cpuset-cpus=0,1 \
  -v $PROJECT_DIR:/home/gradle/project \
  -v $SDK_VOL:/opt/android-sdk \
  -v $GRADLE_VOL:/home/gradle/.gradle \
  -e ANDROID_HOME=/opt/android-sdk \
  -w /home/gradle/project \
  $IMAGE"

bootstrap_sdk() {
  $RUN bash -c '
    set -e
    if [ ! -x /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager ]; then
      echo "[SDK] Installing Android cmdline-tools + SDK 35 (first run only)..."
      apt-get update -qq && apt-get install -y -qq unzip >/dev/null
      curl -sLo /tmp/clt.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
      mkdir -p /opt/android-sdk/cmdline-tools
      unzip -q /tmp/clt.zip -d /opt/android-sdk/cmdline-tools
      mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest
    fi
    yes | /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null 2>&1 || true
    /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager --install \
      "platform-tools" "platforms;android-35" "build-tools;35.0.0" >/dev/null
    echo "[SDK] ready."
  '
}

case "${1:-build}" in
  keystore)
    [ -f keystore/household-bills.jks ] && { echo "keystore already exists — refusing to overwrite (same key forever, or Android refuses updates)"; exit 1; }
    PASS=$(head -c 32 /dev/urandom | base64 | tr -dc 'a-zA-Z0-9' | head -c 24)
    $RUN bash -c "keytool -genkeypair -v -keystore keystore/household-bills.jks \
      -alias household -keyalg RSA -keysize 2048 -validity 10950 \
      -storepass '$PASS' -keypass '$PASS' \
      -dname 'CN=Household Bills, O=Household Bills, C=EE' >/dev/null 2>&1"
    cat > keystore/keystore.properties <<EOF
storeFile=keystore/household-bills.jks
storePassword=$PASS
keyAlias=household
keyPassword=$PASS
EOF
    chmod 600 keystore/keystore.properties
    echo "[KEYSTORE] generated. SHA-1 for the GCP Android OAuth client:"
    $RUN bash -c "keytool -list -v -keystore keystore/household-bills.jks -storepass '$PASS' -alias household 2>/dev/null | grep 'SHA1:'"
    ;;
  sha1)
    source <(grep storePassword keystore/keystore.properties | sed 's/storePassword=/PASS=/')
    $RUN bash -c "keytool -list -v -keystore keystore/household-bills.jks -storepass '$PASS' -alias household 2>/dev/null | grep 'SHA1:'"
    ;;
  build)
    bootstrap_sdk
    echo "[BUILD] assembleRelease (throttled — first build can take 30-60 min on the J4125)..."
    $RUN gradle --no-daemon -q assembleRelease
    mkdir -p dist
    cp app/build/outputs/apk/release/app-release.apk "dist/household-bills-$(date +%Y%m%d).apk"
    ls -la dist/ | tail -3
    echo "[BUILD] done."
    ;;
  *)
    echo "unknown command: $1"; exit 1 ;;
esac
