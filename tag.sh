#!/bin/bash

PROJECT=$(for i in $(find -iname gradle.properties); do dirname $i; done | fzf)
VERSION=$(cat $PROJECT/gradle.properties | grep -E "^(mod_version)" | awk '{ print substr($0, index($0, "=") + 1) }')
MINECRAFT_VERSION=$(cat gradle.properties | grep -E "^(minecraft_version)" | awk '{ print substr($0, index($0, "=") + 1) }')

PROJECT=${PROJECT##"./"}
TAG=$PROJECT\+$VERSION\+$MINECRAFT_VERSION
echo $TAG
