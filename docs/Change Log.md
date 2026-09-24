MinepacksForked Change Log
=========================

## 0.1.0-SNAPSHOT — unreleased

- Combined Minepacks and the PCGF utility/NMS code into one runtime plugin JAR. Retired the PluginLib runtime dependency, Standalone and Release profiles, Paper bootstrap, and BadRabbit bridge.
- Kept the runtime plugin name `Minepacks` and made its main class an explicit Maven property, independent of the `MinepacksForked` artifact name.
- Pinned Java 25 and Paper API `26.3.build.40-alpha`; verified the official Paper 26.3-40 server implementation.
- Compared the seven latest Paper 26.3 API builds, 34–40. The inventory and plugin interfaces used by Minepacks did not change across them; see the release review.
- Rebuilt the embedded 26.3 serializer to use public `CraftItemStack.asBukkitCopy(ItemInstance)` when available. The earlier `asCraftMirror`-only lookup failed on Paper 26.3-40 for populated backpacks.
- Replaced the all-null-array corruption heuristic with an explicit optional decode. Serialized empty backpacks now restore normally after restart. Missing data and unreadable data have separate load outcomes, preventing a failed restore from caching a new backpack over stored bytes.
- Prevented failed serialization from updating SQL rows or truncating file backpacks. File writes now use a temporary file and replacement move.
- The 26.3 serializer now aborts a save if any item fails to encode, preserving the existing record instead of silently omitting that item.
- Added empty/failure regression tests and a JAR descriptor/class verification gate in CI.
- Centralized versioned file reads and atomic writes across storage, backups, and migrations. SQL migrations now commit copied rows together or roll back on failure.
- Removed reflective access from migration lifecycle, SQL schema copying, command setup, and database recovery; removed the unused nullable connection-provider mode.
- Preserved the embedded library source snapshot, license notices, and rebuild script in this repository.

## Unfinished 0.0.1 work

The v0.0.1 development line was never completed or released as a stable MinepacksForked version. Its Paper 26.3 compatibility work is incorporated into the 0.1.0 candidate above.

## Compatibility notes

The compiled 26.3 serializer has been checked against Paper build 40 and the single JAR initializes under that server. A real populated-backpack restore across restart with representative MySQL and SQLite data remains a release gate. Do not tag v0.1.0 until the release checklist passes.
