#!/usr/bin/env bash
set -euo pipefail

jar_file="${1:?pass the Minepacks JAR path}"
test -f "$jar_file"

descriptor="$(unzip -p "$jar_file" plugin.yml)"
entries="$(jar tf "$jar_file")"

grep -Fxq 'name: "Minepacks"' <<< "$descriptor"
grep -Fxq 'main: "at.pcgamingfreaks.Minepacks.Bukkit.Minepacks"' <<< "$descriptor"
grep -Fxq 'api-version: "26.3"' <<< "$descriptor"
grep -Fxq 'depend: []' <<< "$descriptor"
if grep -q 'PCGF_PluginLib' <<< "$descriptor"; then
    echo 'plugin.yml still depends on PCGF PluginLib' >&2
    exit 1
fi

for entry in \
    at/pcgamingfreaks/Minepacks/Bukkit/Minepacks.class \
    at/pcgamingfreaks/Minepacks/Bukkit/Database/InventorySerializer.class \
    at/pcgamingfreaks/Bukkit/MCVersion.class \
    at/pcgamingfreaks/Bukkit/ItemStackSerializer/NBTItemStackSerializer_26_3_R1.class \
    at/pcgf/libs/com/tcoded/folialib/FoliaLib.class; do
    grep -Fxq "$entry" <<< "$entries" || { echo "Missing $entry" >&2; exit 1; }
done

if grep -Eq '^at/pcgamingfreaks/PluginLib/|^paper-plugin.yml$' <<< "$entries"; then
    echo 'Legacy PluginLib plugin entry point or Paper bootstrap was bundled' >&2
    exit 1
fi

serializer_class=at/pcgamingfreaks/Bukkit/ItemStackSerializer/NBTItemStackSerializer_26_3_R1.class
if ! unzip -p "$jar_file" "$serializer_class" | strings | grep -Fx 'asBukkitCopy' >/dev/null; then
    echo 'Paper 26.3 serializer lacks the public asBukkitCopy conversion path' >&2
    exit 1
fi

echo "Verified self-contained Minepacks plugin: $jar_file"
