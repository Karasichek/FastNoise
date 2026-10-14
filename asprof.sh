#!/bin/sh

cd ./build
PID=$(procs fabric | fzf | awk '{ print $1}')
echo "Enter duration: "
read DUR
MODE=$(echo -e "cpu-clock\nalloc\ninstructions" | fzf)
/opt/async-profiler/bin/asprof -e $MODE -F comptask,vtable -d $DUR -f profile-%t.html $PID
python -m http.server
