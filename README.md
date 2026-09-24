# MinepacksForked

MinepacksForked is a maintained fork of the Minepacks backpack plugin for **Paper 26.3 and Java 25**. The runtime plugin is still named `Minepacks`, so existing data under `plugins/Minepacks/` remains in place. The Maven artifact and JAR are named `MinepacksForked`.

The project now builds **one self-contained plugin JAR**. The Minepacks code uses embedded PCGF utility, database, command, version, scheduler, and item serializer classes. A separate PCGF_PluginLibForked plugin is no longer needed at runtime. The embedded source snapshot and the maintained Paper 26.3 serializer override live under [`vendor/pcgf-pluginlib`](vendor/pcgf-pluginlib/README.md).

## Features

- Permission-based backpack sizes, filters, and automatic item collection
- Files, SQLite, and MySQL storage using the existing Minepacks formats
- Language files, updater, and public Minepacks API
- NBT item restoration through the bundled version-specific serializer

## Requirements

- Paper 26.3 and Java 25 for the maintained target
- Maven 3 and JDK 25 to build
- No PCGF_PluginLibForked runtime plugin

Paper's `-alpha` suffix is part of the published 26.3 artifact coordinate; it is not used here as a measure of release readiness. Other Bukkit or Paper versions may work through inherited code and bundled serializers but are not part of this fork's verified target. See the [seven-build Paper API review](docs/Paper%20API%20Release%20Review.md).

## Build

```bash
export JAVA_HOME=/server-data/minecraft/java/java25
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -B clean verify
scripts/verify-package.sh Minepacks/target/MinepacksForked-*.jar
```

Install the single `Minepacks/target/MinepacksForked-0.1.0-SNAPSHOT.jar` on the server. The build does not require a sibling PCGF repository or a previously installed Standalone classifier. The API and embedded library are built in the same Maven reactor.

## Upgrade from the two-plugin setup

1. Stop the server and back up the Minepacks database and `plugins/Minepacks/`.
2. Replace the old Minepacks JAR with this single JAR. Remove `PCGF_PluginLibForked` only after checking whether other plugins use it.
3. Keep the `plugins/Minepacks/` directory and existing database tables.
4. If `Database.Type` was `shared`, `global`, or `external`, configure Minepacks' own MySQL connection and set `Database.Type: mysql` before starting. The old PluginLib-managed shared pool is no longer present. Confirm the database and table names match the old configuration.
5. Check a populated backpack and an empty backpack, then restart and check them again before opening access to all players.

Serialized empty backpacks now load normally. A decoder returning failure leaves the SQL record untouched, attempts a recovery backup, and never creates a replacement backpack in cache. File storage and file migrations use the same versioned format reader and atomic replacement writer, so a failed serialization does not truncate an existing backpack.

See the [dependency migration review](docs/Dependency%20Migration.md), [compatibility matrix](docs/Compatibility%20Matrix.md), [backpack recovery guide](docs/Backpack%20Recovery.md), and [release checklist](docs/Release%20Checklist.md).

## Release version

The next release candidate is **v0.1.0**; the Maven revision remains `0.1.0-SNAPSHOT` until the runtime checklist passes. The unfinished v0.0.1 work never established a stable public release. A v1.0.0 compatibility promise would be premature while representative MySQL, SQLite, upgrade, and restart restore tests remain outstanding. This recommendation does not depend on Paper's artifact suffix.

## API

The `Minepacks-API` Maven module provides `MinepacksPlugin`, backpack access, events, and command interfaces. The runtime name remains `Minepacks`, so `Bukkit.getPluginManager().getPlugin("Minepacks")` continues to find it. See [Minepacks-API/README.md](Minepacks-API/README.md).

MinepacksForked is GPLv3 licensed and derives from [GeorgH93/Minepacks](https://github.com/GeorgH93/Minepacks) and [GeorgH93/PCGF_PluginLib](https://github.com/GeorgH93/PCGF_PluginLib).
