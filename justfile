default: build

build:
    ./gradlew :build

run:
    ./gradlew :runClient

sources:
    ./gradlew :genSources

wrapper:
    ./gradlew wrapper --gradle-version=latest && ./gradlew wrapper

tidy:
    #!/bin/bash

    # find -iname "*.java" | xargs clang-format -i
    find -iname "*.java" | xargs google-java-format -i #--fix-imports-only -i

    # cd nim
    # find -iname "*.nim" | xargs nimpretty
    
test:
    ./gradlew :test

view_test_results:
    cd build/reports/tests/test/ && python -m http.server

bench:
    ./gradlew :jmh

stop:
    ./gradlew --stop

clean:
    ./gradlew clean

prod:
    ./gradlew :prodClient

modrinth ptype="alpha":
    touch changelog.md
    $EDITOR changelog.md
    ./gradlew :modrinth -Pzmtype="{{ptype}}"
    rm changelog.md

# Available props zperfbenchmark zuseasync zworldname zbenchmode zwarmuptime zmeasuretime zwarmups zmeasures zforks zthreads zseed zworldradius zendcenter zworldcenter
perfSurface +args="":
    ./gradlew :perf:runServer -Pzperfbenchmark="surface" {{args}}

# Available props zperfbenchmark zuseasync zworldname zbenchmode zwarmuptime zmeasuretime zwarmups zmeasures zforks zthreads zseed zworldradius zendcenter zworldcenter
perfNoise +args="":
    ./gradlew :perf:runServer -Pzperfbenchmark="noisegen" {{args}}

# Available props zperfbenchmark zuseasync zworldname zbenchmode zwarmuptime zmeasuretime zwarmups zmeasures zforks zthreads zseed zworldradius zendcenter zworldcenter
perfBiomes +args="":
    ./gradlew :perf:runServer -Pzperfbenchmark="biomegen" {{args}}

# Available props zperfbenchmark zuseasync zworldname zbenchmode zwarmuptime zmeasuretime zwarmups zforks zthreads zseed zworldradius zendcenter zworldcenter
perfNoiseAsync +args="":
    ./gradlew :perf:runServer -Pzuseasync="/opt/async-profiler/lib/libasyncProfiler.so" -Pzperfbenchmark="noisgen" {{args}}

# Available props zperfbenchmark zuseasync zworldname zbenchmode zwarmuptime zmeasuretime zwarmups zforks zthreads zseed zworldradius zendcenter zworldcenter
perfBiomesAsync +args="":
    ./gradlew :perf:runServer -Pzuseasync="/opt/async-profiler/lib/libasyncProfiler.so" -Pzperfbenchmark="biomegen" {{args}}

# Available props zperfbenchmark zuseasync zworldname zbenchmode zwarmuptime zmeasuretime zwarmups zforks zthreads zseed zworldradius zendcenter zworldcenter
parity +args="":
    ./gradlew :perf:runServer -Pzparity="true" {{args}}

curseforge ptype="alpha":
    touch changelog.md
    $EDITOR changelog.md
    ./gradlew curseforge -Pzmtype="{{ptype}}"
    rm changelog.md

publish ptype="alpha":
    touch changelog.md
    $EDITOR changelog.md
    ./gradlew modrinth -Pzmtype="{{ptype}}"
    ./gradlew curseforge -Pzmtype="{{ptype}}"
    rm changelog.md

task taskName:
    ./gradlew {{taskName}}
bytecode:
    ./bytecode.sh
asprof:
    ./asprof.sh