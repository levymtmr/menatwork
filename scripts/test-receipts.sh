#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JAVA_HOME:?Configure JAVA_HOME}"
: "${JSON_TEST_JAR:?Provide official org.json test jar}"
mkdir -p build/receipt-tests
"$JAVA_HOME/bin/javac" -encoding UTF-8 -cp "$JSON_TEST_JAR" -d build/receipt-tests tests/jvm/android/content/Context.java tests/jvm/android/content/SharedPreferences.java tests/jvm/android/content/res/AssetManager.java app/src/main/java/com/neonshift/app/ReceiptStore.java tests/jvm/com/neonshift/app/ReceiptStoreTest.java
"$JAVA_HOME/bin/java" -cp "build/receipt-tests:$JSON_TEST_JAR" com.neonshift.app.ReceiptStoreTest
"${NODE_BIN:-node}" tests/receipt-parser.cjs
