#!/bin/sh

LOADER="fabric"
MINECRAFT_VERSION=$(cat gradle.properties | grep -E "^(minecraft_version)" | awk '{ print substr($0, index($0, "=") + 1) }')

get_latest_version() {
  curl -g "https://api.modrinth.com/v2/project/$1/version?include_changelog=false&game_versions=[%22$MINECRAFT_VERSION%22]&loaders=[%22$LOADER%22]" | jq '.[].files[].url' | head -1 | tr -d '"'
}

DIR=$(printf "%s\n" */run/mods | fzf)

get_version() {
  aria2c -d $DIR $(get_latest_version $1)
}

for i in $@; do get_version $i; done
