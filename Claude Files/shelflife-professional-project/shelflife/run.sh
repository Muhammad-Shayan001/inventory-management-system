#!/bin/sh
# Starts the desktop application (compiles first if needed).
cd "$(dirname "$0")"
[ -f build/classes/shelflife/ui/ShelfLifeApp.class ] || ./compile.sh || exit 1
exec java -cp build/classes shelflife.ui.ShelfLifeApp
