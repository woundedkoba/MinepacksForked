# Dependency migration review

## Projects reviewed

The former Minepacks build had three interacting modes. Normal mode compiled against `PCGF_PluginLibForked` and declared `PCGF_PluginLib` as a required runtime plugin. Standalone mode preprocessed `STANDALONE` source branches and shaded an older `PluginLib` coordinate. Release mode depended on the Standalone classifier of the same Minepacks artifact, added BadRabbit and a Paper bootstrap, and switched modes at runtime according to the external PluginLib version. This made the release sensitive to an artifact from an earlier build. The generated Bukkit `main` class also used the renamed Maven `artifactId` as a Java package and did not exist.

The PCGF fork contains a PluginLib `JavaPlugin` entry point, common configuration/message/database code, Bukkit utilities, version detection, platform resolution, version-specific NMS serializers, and Bungee modules. Minepacks needs the utility and serializer classes. It no longer calls the PluginLib plugin singleton. The old Paper bootstrap, BadRabbit bridge, version-detection dependency, and split-mode build have been removed from the reactor.

| Minepacks component | PCGF functionality it uses | Integrated path |
|---|---|---|
| Commands, help, and language | `Message`, placeholders, `SubCommand`, `HelpData`, command registration | Embedded library classes in the one plugin JAR |
| Configuration and persistence | YAML helpers, `DBTools`, MySQL/SQLite `ConnectionProvider`, UUID conversion | Embedded library classes; Minepacks keeps its own database configuration |
| Backpack and item filters | `InventoryUtils`, `MinecraftMaterial`, item-name resolution | Embedded library classes and item translations |
| Scheduling and updates | FoliaLib scheduler, `ManagedUpdater`, version/server detection | Embedded library classes; no PluginLib singleton call |
| Backpack serialization | `ItemStackSerializer`, platform resolver, version-specific NMS serializers | Embedded classes plus maintained Paper 26.3 override source |
| Public API | PCGF message and command types used in API signatures | `Minepacks-API` and `Minepacks-EmbeddedLib` reactor artifacts |

The embedded JAR is a frozen 0.0.1 snapshot and its full source snapshot and license notices are retained under `vendor/pcgf-pluginlib`. `Minepacks-EmbeddedLib` unpacks it in the Maven reactor and excludes the PluginLib entry point, Bungee classes, and plugin descriptors. A clean reactor build shades `Minepacks-API`, `Minepacks-MagicValues`, and `Minepacks-EmbeddedLib` into one deployable Minepacks JAR. The package check confirms that the descriptor names the real `at.pcgamingfreaks.Minepacks.Bukkit.Minepacks` class, the 26.3 serializer is present, and the external PluginLib dependency is absent.

## Persistence and restart failure

SQL and file storage record the serializer version alongside the item bytes. The 26.3 NBT serializer writes the inventory size and an item list. A valid empty backpack has an empty item list and restores to an array of null slots. The previous SQL gate treated any nonempty serialized byte array whose decoded slots were all null as corruption, causing the repeated `Refusing to load an empty backpack` error after every restart. Minepacks now uses `Optional<ItemStack[]>`: a present all-null array is a successful empty decode; an empty result is failure. Storage callbacks distinguish a missing record from an unreadable one. Only a missing record can create a new backpack; failed SQL or file reads leave it out of cache. SQL retains the original row and attempts a recovery backup. Serialization failure stops a SQL save before issuing an update. File saves, backups, and file migrations share the same versioned reader and atomic replacement writer.

Inspection of the Paper 26.3 build 40 server JAR exposed a separate problem for populated backpacks: it has public `CraftItemStack.asBukkitCopy(ItemInstance)` and no `asCraftMirror` method. The previous 26.3 PCGF serializer searched only for `asCraftMirror` and returned failure on item conversion. The embedded override selects the public `asBukkitCopy` method for the NMS item copy; it retains `asCraftMirror` as a fallback for a compatible variant. The patched class is compiled against the official Paper 26.3-40 server JAR and included in the embedded library snapshot. The seven-build API review does not establish when this server implementation mismatch first appeared.

## Upgrade boundary

`Database.Type: shared`, `global`, and `external` previously used the PCGF plugin's shared pool. Those modes now fail clearly. Configure Minepacks' own MySQL host, credentials, database, and table settings, then set `Database.Type: mysql` before removing PluginLib. Existing Minepacks file/SQLite/MySQL storage formats and the `Minepacks` plugin data directory are retained. Other installed plugins may still require PCGF_PluginLibForked; inspect them before removing its JAR.

## Verification and remaining release gates

Java 25 `mvn clean verify` passes, including empty/failure decode regression tests. The single shaded JAR passes descriptor and class-entry checks. Paper 26.3-40 initializes it as one Bukkit plugin before the EULA gate. A full enable, real backpack save/restore across restart, MySQL/SQLite checks, and Folia behavior have not yet been exercised against a server with an accepted EULA and representative data. Keep the version at `0.1.0-SNAPSHOT` until those checks pass.
