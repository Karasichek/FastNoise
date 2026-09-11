#!/bin/sh

JARFILE=`find .gradle/loom-cache/minecraftMaven/ -iname '*.jar' | fzf`
CLASS=`unzip -l $JARFILE | grep '\.class' | awk '{ print $4}' | fzf`

CLASSNAME=${CLASS/%".class"}

INNERCLASSES=`unzip -l $JARFILE | grep $CLASSNAME\$ | awk '{ print $4}'`

OUTDIR=build/decompiled

rm -rf $OUTDIR
mkdir -p $OUTDIR
unzip $JARFILE -d $OUTDIR $CLASS $INNERCLASSES
cd $OUTDIR && vineflower --include-classpath='$JARFILE' $CLASS $INNERCLASSES . && vscodium *.java