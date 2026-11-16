#!/usr/bin/env bash
# Dependency-free Android build, using official SDK tools. No Gradle download needed.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?Configure ANDROID_HOME para o Android SDK}"
: "${JAVA_HOME:?Configure JAVA_HOME para um JDK 17 ou 21}"
export PATH="$JAVA_HOME/bin:$PATH"
VERSION_CODE="$(sed -n 's/^versionCode=//p' version.properties)"
VERSION_NAME="$(sed -n 's/^versionName=//p' version.properties)"
[[ "$VERSION_CODE" =~ ^[1-9][0-9]*$ && "$VERSION_NAME" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo 'version.properties inválido'; exit 1; }
APK_PATH="dist/neon-shift-$VERSION_NAME.apk"
TOOLS="$ANDROID_HOME/build-tools/35.0.0"
PLATFORM="$ANDROID_HOME/platforms/android-35/android.jar"
OUT="$PWD/build/manual"
rm -rf "$OUT/gen" "$OUT/classes" "$OUT/dex"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex" dist
rm -f "$OUT/resources.zip" "$OUT/base.apk" "$OUT/unaligned.apk" "$OUT/aligned.apk"
"$TOOLS/aapt2" compile --dir app/src/main/res -o "$OUT/resources.zip"
sed 's/<manifest /<manifest package="com.neonshift.app" /' app/src/main/AndroidManifest.xml > "$OUT/AndroidManifest.xml"
"$TOOLS/aapt2" link -o "$OUT/base.apk" -I "$PLATFORM" --manifest "$OUT/AndroidManifest.xml" \
    --java "$OUT/gen" --min-sdk-version 26 --target-sdk-version 35 \
    --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" -A app/src/main/assets "$OUT/resources.zip"
find app/src/main/java "$OUT/gen" -name '*.java' > "$OUT/sources.txt"
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -classpath "$PLATFORM" -d "$OUT/classes" @"$OUT/sources.txt"
jar cf "$OUT/classes.jar" -C "$OUT/classes" .
"$TOOLS/d8" --lib "$PLATFORM" --min-api 26 --output "$OUT/dex" "$OUT/classes.jar"
cp "$OUT/base.apk" "$OUT/unaligned.apk"
(cd "$OUT/dex" && zip -q -u "$OUT/unaligned.apk" classes*.dex)
"$TOOLS/zipalign" -f -p 4 "$OUT/unaligned.apk" "$OUT/aligned.apk"
# Local signing identity is created once and preserved for future updates.
SIGNING_KEY="${NEON_SIGNING_KEY:-$PWD/.tools/neonshift-local.jks}"
if [ ! -f "$SIGNING_KEY" ]; then
    mkdir -p "$(dirname "$SIGNING_KEY")"
    keytool -genkeypair -keystore "$SIGNING_KEY" -storepass neonshift-local \
        -keypass neonshift-local -alias neonshift -keyalg RSA -keysize 3072 \
        -validity 10000 -dname 'CN=Neon Shift Personal, O=Neon Shift, C=AU'
    chmod 600 "$SIGNING_KEY"
fi
rm -f "$APK_PATH.idsig"
"$TOOLS/apksigner" sign --v4-signing-enabled false --ks "$SIGNING_KEY" --ks-pass pass:neonshift-local \
    --ks-key-alias neonshift --out "$APK_PATH" "$OUT/aligned.apk"
"$TOOLS/apksigner" verify --verbose "$APK_PATH"
"$TOOLS/zipalign" -c -p 4 "$APK_PATH"
sha256sum "$APK_PATH" > "$APK_PATH.sha256"
echo "APK gerado: $APK_PATH"
