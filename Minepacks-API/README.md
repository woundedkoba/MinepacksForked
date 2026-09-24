# MinepacksForked API

This module exposes `MinepacksPlugin`, backpacks, callbacks, events, and command interfaces. The runtime plugin remains named `Minepacks`, and the command manager is available in the single self-contained JAR.

Build it with its embedded utility dependency from the same checkout:

```bash
export JAVA_HOME=/server-data/minecraft/java/java25
export PATH="$JAVA_HOME/bin:$PATH"
mvn -B -pl Minepacks-API -am package
```

If publishing the API to a Maven repository, publish `Minepacks-EmbeddedLib` with the same version. The current development version is `0.1.0-SNAPSHOT` and is not a stable release.

Find the plugin by its stable runtime name:

```java
Plugin plugin = Bukkit.getPluginManager().getPlugin("Minepacks");
if (plugin instanceof MinepacksPlugin minepacks) {
    Backpack backpack = minepacks.getBackpackCachedOnly(player);
    if (backpack != null) {
        Inventory inventory = backpack.getInventory();
        // Use the inventory while the backpack is loaded.
    }
}
```

Use the API rather than accessing Minepacks' database or implementation classes directly. The [main README](../README.md) describes the supported Paper version and release status.

`getBackpack(owner, callback, createNewIfNotExists)` creates a backpack only when no stored record or file exists. If stored data cannot be read or a database query fails, the callback's `onFail()` method runs and no replacement backpack is cached. Command extensions should handle `onFail()` when they request a backpack.
