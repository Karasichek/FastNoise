default: build

build:
    ./gradlew build

serverBuild:
    ./gradlew -PserverCompile=true build

run:
    ./gradlew runClient

server:
    ./gradlew runServer

dap:
    ./gradlew runDap

hotswapDap:
    JAVA_HOME=/usr/lib/jvm/jre-jetbrains ./gradlew runHotswapDap

sources:
    ./gradlew genSources

wrapper:
    ./gradlew wrapper --gradle-version=latest && ./gradlew wrapper

tidy:
    #!/bin/bash

    # find -iname "*.java" | xargs clang-format -i
    find -iname "*.java" | xargs google-java-format -i #--fix-imports-only -i

    # cd nim
    # find -iname "*.nim" | xargs nimpretty
    
test:
    ./gradlew test

view_test_results:
    cd build/reports/tests/test/ && python -m http.server

bench:
    ./gradlew jmh

stop:
    ./gradlew --stop

clean:
    ./gradlew clean

prod:
    ./gradlew prodClient

modrinth ptype="alpha":
    touch changelog.md
    $EDITOR changelog.md
    ./gradlew modrinth -Pzmtype="{{ptype}}"
    rm changelog.md

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