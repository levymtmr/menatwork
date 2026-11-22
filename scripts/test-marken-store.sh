#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JAVA_HOME:?Configure JAVA_HOME}"
: "${JSON_TEST_JAR:?Provide the official org.json test jar (not included in the APK)}"
mkdir -p build/store-tests
"$JAVA_HOME/bin/javac" -encoding UTF-8 -cp "$JSON_TEST_JAR" -d build/store-tests tests/jvm/android/content/*.java tests/jvm/android/content/res/*.java tests/jvm/android/net/*.java tests/jvm/android/os/*.java tests/jvm/android/provider/*.java tests/jvm/com/neonshift/app/InvoiceStore.java tests/jvm/com/neonshift/app/MarkenStoreTest.java app/src/main/java/com/neonshift/app/{Store,ClientRegistry,MarkenEngine,OvertimeEngine,CostEngine,TimeLedger,JournalEngine,LunchEngine,InvoiceEngine,Progression,WorksiteBook}.java
"$JAVA_HOME/bin/java" -cp "build/store-tests:$JSON_TEST_JAR" com.neonshift.app.MarkenStoreTest
