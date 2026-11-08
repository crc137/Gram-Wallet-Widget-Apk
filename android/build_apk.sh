#!/usr/bin/env bash

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST="$PROJECT_DIR/../dist"
GRADLE_FILE="$PROJECT_DIR/app/build.gradle"

mkdir -p "$DIST"

VERSION_CODE=$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]+([0-9]+).*$/\1/p' "$GRADLE_FILE")
VERSION_NAME=$(sed -nE 's/^[[:space:]]*versionName[[:space:]]+"([^"]+)".*$/\1/p' "$GRADLE_FILE")

if [[ -z "$VERSION_CODE" || -z "$VERSION_NAME" ]]; then
  echo "Could not read version from $GRADLE_FILE" >&2
  exit 1
fi

NEW_VERSION_CODE=$((VERSION_CODE + 1))

if [[ ! "$VERSION_NAME" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  echo "Unsupported version format: $VERSION_NAME" >&2
  exit 1
fi

NEW_VERSION_NAME="${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.$((BASH_REMATCH[3] + 1))"

sed -i \
  -e "s/versionCode[[:space:]]\+$VERSION_CODE/versionCode $NEW_VERSION_CODE/" \
  -e "s/versionName[[:space:]]*\"$VERSION_NAME\"/versionName \"$NEW_VERSION_NAME\"/" \
  "$GRADLE_FILE"

echo "Building version $NEW_VERSION_NAME ($NEW_VERSION_CODE)"

if [[ -z "${JAVA_HOME:-}" ]] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "1[7-9]\|version "2[0-1]'; then
  if [[ -x "$HOME/.local/jdk-17/bin/java" ]]; then
    export JAVA_HOME="$HOME/.local/jdk-17"
  fi
fi

export PATH="${JAVA_HOME:+$JAVA_HOME/bin:}$PATH"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
export ANDROID_HOME="$SDK"

if [[ ! -f "$PROJECT_DIR/local.properties" ]]; then
  echo "sdk.dir=$SDK" > "$PROJECT_DIR/local.properties"
fi

cd "$PROJECT_DIR"
grep -rLZ --exclude-dir={node_modules,.next,.git,build,target,.gradle,.idea} --include='*.java' --include='*.xml' -P '^\s+▐\s*$' . | xargs -r0 grep -lZ 'CoonDev' | xargs -r0 perl -Mutf8 -CSD -0pi -e 's/(█▐[ \t]*\n)/$1 . (" " x 43) . "▐\n"/e' || true
./gradlew --no-daemon assembleDebug

APK_SRC="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

if [[ ! -f "$APK_SRC" ]]; then
  echo "Gradle did not produce $APK_SRC" >&2
  exit 1
fi

cp -f "$APK_SRC" "$DIST/GramWallet.apk"

BUILD_TOOLS="${BUILD_TOOLS:-$SDK/build-tools/34.0.0}"

if [[ ! -x "$BUILD_TOOLS/aapt" ]]; then
  BUILD_TOOLS="$(ls -d "$SDK"/build-tools/* | sort -V | tail -n1)"
fi

echo "APK: $DIST/GramWallet.apk"
"$BUILD_TOOLS/aapt" dump badging "$DIST/GramWallet.apk" | head -n 3
