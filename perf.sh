#!/bin/sh

BENCH=$(echo -e "biomegen\nterrain" | fzf)
ASYNC=$(echo -e "-Pzuseasync=/opt/async-profiler/lib/libasyncProfiler.so\n" | fzf)
COMMAND="./gradlew :perf:runServer -Pzperfbenchmark="$BENCH" $ASYNC $@"
OPTIONS=(zmod zworldname zbenchmode zwarmuptime zmeasuretime zwarmups zmeasures zforks zthreads zseed zworldradius zendcenter zworldcenter)

echo "$COMMAND \\" >.command
for i in ${OPTIONS[@]}; do echo "# -P$i= \\" >>.command; done

$EDITOR .command
source ./.command
rm .command
