#!/bin/sh
# Builds everything and runs the automated test suite. Needs a display (use xvfb-run on servers).
cd "$(dirname "$0")"
./compile.sh || exit 1
exec java -cp build/classes:build/test/classes shelflife.test.SystemAuditTestSuite
