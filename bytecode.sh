#!/bin/sh

JARFILE=`find .gradle/loom-cache/minecraftMaven/ -iname '*.jar' | fzf`
CLASS=`unzip -l $JARFILE | grep '\.class' | awk '{ print $4}' | fzf`

CLASS=${CLASS/%".class"}

FLAGS=`echo -e "-s\n-s -c" | fzf`
javap -cp $JARFILE $FLAGS -private $CLASS | vscodium -