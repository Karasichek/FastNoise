#!/bin/sh

JARFILE=$(find .gradle/loom-cache/minecraftMaven/ -iname '*.jar' | fzf)
ALLCLASSES=$(unzip -l $JARFILE | grep '\.class' | awk '{print $4}')
CLASS=$(printf "$ALLCLASSES" | fzf)

CLASSNAME=${CLASS/%".class"/}

INNERCLASSES=$(printf "$ALLCLASSES" | grep -F $CLASSNAME$)

OUTDIR=build/decompiled

rm -rf $OUTDIR
mkdir -p $OUTDIR
unzip $JARFILE -d $OUTDIR $CLASS $INNERCLASSES
cd $OUTDIR && vineflower --include-classpath='$JARFILE' $CLASS $INNERCLASSES . && vscodium *.java
