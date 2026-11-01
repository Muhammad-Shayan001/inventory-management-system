#!/bin/sh
# Compiles the application and the test suite into ./build (works with JDK 8 or newer).
set -e
cd "$(dirname "$0")"

rm -rf build
mkdir -p build/classes build/test/classes

find src -name '*.java' > build/sources.txt
javac -encoding UTF-8 -Xlint:-options -d build/classes @build/sources.txt

find test -name '*.java' > build/test-sources.txt
javac -encoding UTF-8 -Xlint:-options -cp build/classes -d build/test/classes @build/test-sources.txt

echo "[OK] Build finished. Run ./run.sh to start the app or ./test.sh to run the tests."
