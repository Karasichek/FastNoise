#!/bin/sh

cd ./build
PID=`procs fabric | fzf | awk '{ print $1}'`
echo "Enter duration: "
read DUR
/opt/async-profiler/bin/asprof -d $DUR -f profile.html $PID
python -m http.server