#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JAVA_HOME:?Configure JAVA_HOME}"
: "${JSON_TEST_JAR:?Provide official org.json test jar}"
mkdir -p build/pair-tests
"$JAVA_HOME/bin/javac" -encoding UTF-8 -cp "$JSON_TEST_JAR" -d build/pair-tests tests/jvm/android/content/*.java tests/jvm/android/content/res/*.java tests/jvm/android/net/*.java tests/jvm/android/os/*.java tests/jvm/android/provider/*.java tests/jvm/com/neonshift/app/PairedInvoiceTest.java app/src/main/java/com/neonshift/app/{Store,ClientRegistry,MarkenEngine,MarkenBilling,TimesheetData,OvertimeEngine,CostEngine,TimeLedger,JournalEngine,LunchEngine,InvoiceEngine,InvoiceStore,InvoicePdf,InvoiceFont,InvoiceShare,Progression,WorksiteBook}.java
"$JAVA_HOME/bin/java" -cp "build/pair-tests:$JSON_TEST_JAR" com.neonshift.app.PairedInvoiceTest
