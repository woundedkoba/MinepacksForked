#!/usr/bin/env bash
set -euo pipefail

paper_root="${1:?pass a Paper 26.3 runtime directory with versions/26.3 and libraries}"
project_root="$(cd "$(dirname "$0")/.." && pwd)"
server_jar="$paper_root/versions/26.3/paper-26.3.jar"
library_jar="$project_root/vendor/pcgf-pluginlib/pcgf-pluginlib-0.0.1.jar"
source_file="$project_root/vendor/pcgf-pluginlib/src/at/pcgamingfreaks/Bukkit/ItemStackSerializer/NBTItemStackSerializer_26_3_R1.java"

test -f "$server_jar"
test -f "$library_jar"
test -f "$source_file"

build_dir="$(mktemp -d)"
trap 'rm -rf "$build_dir"' EXIT
paper_libraries="$(find "$paper_root/libraries" -name '*.jar' -print | paste -sd: -)"

"${JAVA_HOME:?set JAVA_HOME to Java 25}/bin/javac" --release 25 \
    -cp "$server_jar:$library_jar:$paper_libraries" \
    -d "$build_dir" "$source_file"

(cd "$build_dir" && zip -q "$library_jar" \
    at/pcgamingfreaks/Bukkit/ItemStackSerializer/NBTItemStackSerializer_26_3_R1.class)
sha256sum "$library_jar"
