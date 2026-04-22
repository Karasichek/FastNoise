#!/bin/sh

JARFILE=`find .gradle/loom-cache/minecraftMaven/ -iname '*.jar' | fzf`
CLASS=`unzip -l $JARFILE | grep '\.class' | awk '{ print $4}' | fzf`

CLASS=${CLASS/%".class"}
javap -cp $JARFILE -s -private $CLASS | vscodium -

# javap -cp $JARFILE -s -c -private $CLASS | helix