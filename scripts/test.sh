#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JAVA_HOME:?Configure JAVA_HOME}"
mkdir -p build/tests
"$JAVA_HOME/bin/javac" -encoding UTF-8 -d build/tests app/src/main/java/com/neonshift/app/CostEngine.java app/src/main/java/com/neonshift/app/TimeLedger.java tests/CostEngineTest.java
"$JAVA_HOME/bin/java" -cp build/tests CostEngineTest
"$JAVA_HOME/bin/javac" -encoding UTF-8 -d build/tests app/src/main/java/com/neonshift/app/InvoiceEngine.java app/src/main/java/com/neonshift/app/InvoiceFont.java app/src/main/java/com/neonshift/app/InvoicePdf.java app/src/main/java/com/neonshift/app/MarkenBilling.java tests/InvoiceTest.java
python3 - <<'PY'
from pathlib import Path
import json
p=json.loads(Path('app/src/main/assets/invoice-defaults.json').read_text())['profile']
p.update(bank='Example Bank',account_name='Example Worker',bsb='000-000',account='00000000',email='worker@example.com',client_email='client@example.com')
Path('build/tests/invoice-profile.properties').write_text('\n'.join(k+'='+str(v).replace('\\','\\\\').replace('\n','\\n') for k,v in p.items()))
PY
"$JAVA_HOME/bin/java" -cp build/tests InvoiceTest build/tests/invoice-profile.properties
"$JAVA_HOME/bin/javac" -encoding UTF-8 -d build/tests app/src/main/java/com/neonshift/app/UpdatePolicy.java tests/UpdatePolicyTest.java
"$JAVA_HOME/bin/java" -cp build/tests UpdatePolicyTest
"$JAVA_HOME/bin/javac" -encoding UTF-8 -cp build/tests -d build/tests app/src/main/java/com/neonshift/app/JournalEngine.java app/src/main/java/com/neonshift/app/Progression.java tests/JournalTest.java
"$JAVA_HOME/bin/java" -cp build/tests JournalTest
"$JAVA_HOME/bin/javac" -encoding UTF-8 -cp build/tests -d build/tests app/src/main/java/com/neonshift/app/LunchEngine.java tests/LunchTest.java
"$JAVA_HOME/bin/java" -cp build/tests LunchTest
"${NODE_BIN:-node}" tests/listing.cjs
"${NODE_BIN:-node}" tests/shift-time.cjs
"$JAVA_HOME/bin/javac" -encoding UTF-8 -cp build/tests -d build/tests app/src/main/java/com/neonshift/app/WorksiteBook.java tests/WorksiteBookTest.java
"$JAVA_HOME/bin/java" -cp build/tests WorksiteBookTest
"$JAVA_HOME/bin/javac" -encoding UTF-8 -d build/tests app/src/main/java/com/neonshift/app/OvertimeEngine.java tests/OvertimeTest.java
"$JAVA_HOME/bin/java" -cp build/tests OvertimeTest
"${NODE_BIN:-node}" tests/weekly.cjs
"$JAVA_HOME/bin/javac" -encoding UTF-8 -d build/tests app/src/main/java/com/neonshift/app/MarkenEngine.java tests/MarkenTest.java
"$JAVA_HOME/bin/java" -cp build/tests MarkenTest
"$JAVA_HOME/bin/javac" -encoding UTF-8 -cp build/tests -d build/tests app/src/main/java/com/neonshift/app/MarkenBilling.java tests/MarkenBillingTest.java
"$JAVA_HOME/bin/java" -cp build/tests MarkenBillingTest
