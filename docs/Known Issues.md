MinepacksForked Known Issues
===========================

- The embedded serializer now selects public `asBukkitCopy(ItemInstance)` on Paper 26.3 build 40. A newer server build needs an implementation compatibility check because this method is outside the Paper API. The API artifact's `-alpha` suffix is not a release-readiness signal for this project.
- The runtime plugin name intentionally remains `Minepacks` so upgrades reuse `plugins/Minepacks/`. Do not rename it to `MinepacksForked` unless a deliberate data-directory migration is performed.
- A valid serialized empty backpack loads normally. When the serializer returns failure, MinepacksForked refuses to cache a replacement backpack and attempts to back up the original SQL bytes; the log states whether backup creation succeeded.
- `Database.Type: shared`, `global`, and `external` need migration to Minepacks' own MySQL settings before removing PluginLib; these modes cannot use a deleted PluginLib connection pool.
- Live Paper 26.3 database restore testing is still required with representative MySQL and SQLite backups.
- The project remains a fork of Minepacks; upstream changes should be reviewed before syncing them into this maintained branch.
