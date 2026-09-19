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

    # find -iname "*.java" | xargs clang-format -i
    find -iname "*.java" | xargs google-java-format -i #--fix-imports-only -i

    # cd nim
    # find -iname "*.nim" | xargs nimpretty

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

bytecode:
    ./bytecode.sh

asprof:
    ./asprof.sh