#!/bin/bash

ASMJAR=`find ~/.gradle/caches -iname "asm-[0-9]*[0-9].jar" | fzf`
ASMUTILJAR=`find ~/.gradle/caches -iname "asm-util-[0-9]*[0-9].jar" | fzf`

CLASS=`find . -iname "*.class" | fzf`
CLASSDIR=`dirname $CLASS`
java -classpath "$ASMJAR":"$ASMUTILJAR":"$CLASSDIR" org.objectweb.asm.util.ASMifier $CLASS | vscodium - 