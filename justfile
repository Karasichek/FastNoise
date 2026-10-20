default: build

task taskName options="":
    ./gradlew {{options}} :{{taskName}}

build:
    # just task build
    ./gradlew build

wrapper:
    ./gradlew wrapper --gradle-version=latest && ./gradlew wrapper

tidy:
    #!/bin/bash

    git status -u --porcelain | awk '/\.java$/{print $2}' | xargs google-java-format -i
    # find -iname "*.java" | xargs clang-format -i
    # find -iname "*.java" | xargs google-java-format -i #--fix-imports-only -i

    # cd nim
    # find -iname "*.nim" | xargs nimpretty

tidyAll:
    #!/bin/bash
    git ls-files | awk '/\.java$/' | xargs google-java-format -i

bench:
    just task jmh

stop:
    ./gradlew --stop

clean:
    ./gradlew clean

perf +args="":
  ./perf.sh {{args}}

# Available props zperfbenchmark zuseasync zworldname zbenchmode zwarmuptime zmeasuretime zwarmups zmeasures zforks zthreads zseed zworldradius zendcenter zworldcenter
perfSurface +args="":
    ./gradlew :perf:runServer -Pzperfbenchmark="surface" {{args}}

# Available props zperfbenchmark zuseasync zworldname zbenchmode zwarmuptime zmeasuretime zwarmups zmeasures zforks zthreads zseed zworldradius zendcenter zworldcenter
perfNoise +args="":
    ./gradlew :perf:runServer -Pzperfbenchmark="noisegen" {{args}}

# Available props zperfbenchmark zuseasync zworldname zbenchmode zwarmuptime zmeasuretime zwarmups zmeasures zforks zthreads zseed zworldradius zendcenter zworldcenter
perfBiomes +args="":
    ./gradlew :perf:runServer -Pzperfbenchmark="biomegen" {{args}}

# Available props zperfbenchmark zuseasync zworldname zbenchmode zwarmuptime zmeasuretime zwarmups zmeasures zforks zthreads zseed zworldradius zendcenter zworldcenter
perfSurfaceAsync +args="":
    ./gradlew :perf:runServer -Pzuseasync="/opt/async-profiler/lib/libasyncProfiler.so" -Pzperfbenchmark="surface" {{args}}

# Available props zperfbenchmark zuseasync zworldname zbenchmode zwarmuptime zmeasuretime zwarmups zforks zthreads zseed zworldradius zendcenter zworldcenter
perfNoiseAsync +args="":
    ./gradlew :perf:runServer -Pzuseasync="/opt/async-profiler/lib/libasyncProfiler.so" -Pzperfbenchmark="noisgen" {{args}}

# Available props zperfbenchmark zuseasync zworldname zbenchmode zwarmuptime zmeasuretime zwarmups zforks zthreads zseed zworldradius zendcenter zworldcenter
perfBiomesAsync +args="":
    ./gradlew :perf:runServer -Pzuseasync="/opt/async-profiler/lib/libasyncProfiler.so" -Pzperfbenchmark="biomegen" {{args}}

# Available props zperfbenchmark zuseasync zworldname zbenchmode zwarmuptime zmeasuretime zwarmups zforks zthreads zseed zworldradius zendcenter zworldcenter zseedcount
parityServer +args="":
    ./gradlew :perf:runServer -Pzparity="true" -Pzparityserver="true" -Pzmod="true" {{args}}

parityClient +args="":
    ./gradlew :perf:runServer -Pzparity="true" -Pzparityserver="false" -Pzmod="false" {{args}}

parity +args="":
  tmux new-session -d 'bash'
  tmux split-window -v 'bash'
  tmux send -t 0:0.0 "just parityServer {{args}}" C-m
  tmux send -t 0:0.1 "sleep 10" C-m
  tmux send -t 0:0.1 "just parityClient {{args}}" C-m
  tmux -2 attach-session -d

bytecode:
    ./bytecode.sh

asprof:
    ./asprof.sh
