#!/usr/bin/env bash
# Disposable, signed APK fixtures. Never publish these as app releases.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?Configure ANDROID_HOME}"
: "${JAVA_HOME:?Configure JAVA_HOME}"
export PATH="$JAVA_HOME/bin:$PATH"
TOOLS="$ANDROID_HOME/build-tools/35.0.0"
CURRENT_CODE="$(sed -n 's/^versionCode=//p' version.properties)"
CURRENT_NAME="$(sed -n 's/^versionName=//p' version.properties)"
FUTURE_CODE="$((CURRENT_CODE + 1))"
FUTURE_NAME="$(python3 -c 'import sys; a,b,c=map(int,sys.argv[1].split(".")); print(f"{a}.{b}.{c+1}")' "$CURRENT_NAME")"
OUT="$PWD/build/update-fixtures"
mkdir -p "$OUT/assets"
"$TOOLS/aapt2" link -o "$OUT/future-base.apk" -I "$ANDROID_HOME/platforms/android-35/android.jar" \
  --manifest build/manual/AndroidManifest.xml --min-sdk-version 26 --target-sdk-version 35 \
  --version-code "$FUTURE_CODE" --version-name "$FUTURE_NAME" -A app/src/main/assets build/manual/resources.zip
cp "$OUT/future-base.apk" "$OUT/future-dex.apk"
(cd build/manual/dex && zip -q -u "$OUT/future-dex.apk" classes*.dex)
"$TOOLS/zipalign" -f 4 "$OUT/future-dex.apk" "$OUT/future-aligned.apk"
"$TOOLS/apksigner" sign --v4-signing-enabled false --ks .tools/neonshift-local.jks --ks-pass pass:neonshift-local \
  --out "$OUT/assets/future.apk" "$OUT/future-aligned.apk"
if [ ! -f "$OUT/wrong-key.jks" ]; then
  keytool -genkeypair -keystore "$OUT/wrong-key.jks" -storepass fixture-test -keypass fixture-test \
    -alias fixture -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=Disposable Test Only'
fi
"$TOOLS/apksigner" sign --v4-signing-enabled false --ks "$OUT/wrong-key.jks" --ks-pass pass:fixture-test \
  --out "$OUT/assets/wrong-key.apk" "$OUT/future-aligned.apk"
cp "dist/neon-shift-$CURRENT_NAME.apk" "$OUT/assets/current.apk"
python3 - "$OUT/assets/fixture-config.json" "$CURRENT_CODE" "$CURRENT_NAME" "$FUTURE_CODE" "$FUTURE_NAME" <<'PYFIXTURE'
import json,sys
from pathlib import Path
Path(sys.argv[1]).write_text(json.dumps(dict(currentCode=int(sys.argv[2]),currentName=sys.argv[3],futureCode=int(sys.argv[4]),futureName=sys.argv[5])))
PYFIXTURE
