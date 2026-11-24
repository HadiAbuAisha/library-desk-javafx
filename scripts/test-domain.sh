#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p /tmp/library-desk-test-classes
javac -d /tmp/library-desk-test-classes src/main/java/com/portfolio/library/{Book,Loan,Library,CsvStore}.java src/test/java/com/portfolio/library/LibrarySelfTest.java
java -ea -cp /tmp/library-desk-test-classes com.portfolio.library.LibrarySelfTest
