#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?Configure ANDROID_HOME}"
: "${JAVA_HOME:?Configure JAVA_HOME}"
export PATH="$JAVA_HOME/bin:$PATH"
OUT="$PWD/build/device-tests"
TOOLS="$ANDROID_HOME/build-tools/35.0.0"
PLATFORM="$ANDROID_HOME/platforms/android-35/android.jar"
mkdir -p "$OUT/classes" "$OUT/dex"
"$TOOLS/aapt2" link -o "$OUT/test-base.apk" -I "$PLATFORM" --manifest tests/android/AndroidManifest.xml
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -classpath "$PLATFORM:$PWD/build/manual/classes" -d "$OUT/classes" tests/android/InvoiceSmoke.java
jar cf "$OUT/tests.jar" -C "$OUT/classes" .
"$TOOLS/d8" --lib "$PLATFORM" --classpath "$PWD/build/manual/classes.jar" --min-api 26 --output "$OUT/dex" "$OUT/tests.jar"
cp "$OUT/test-base.apk" "$OUT/test-unaligned.apk"
(cd "$OUT/dex" && zip -q -u "$OUT/test-unaligned.apk" classes.dex)
"$TOOLS/zipalign" -f 4 "$OUT/test-unaligned.apk" "$OUT/test-aligned.apk"
"$TOOLS/apksigner" sign --v4-signing-enabled false --ks .tools/neonshift-local.jks --ks-pass pass:neonshift-local \
    --out "$OUT/neonshift-tests.apk" "$OUT/test-aligned.apk"
echo "Somente em emulador descartável: adb -s emulator-5554 install -r $OUT/neonshift-tests.apk"
echo 'adb -s emulator-5554 shell am instrument -w com.neonshift.tests/com.neonshift.app.InvoiceSmoke'
