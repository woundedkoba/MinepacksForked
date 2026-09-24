MinepacksForked Planned Updates
==============================

## Database and deserialization safety

- Refactor `SQL.loadBackpack` into focused operations for database retrieval, deserialization, failure handling, and `Backpack` construction.
- Extend the explicit deserialization result with per-slot diagnostics if a future serializer can report partial recovery. The current 26.3 serializer returns failure when any item conversion fails; a valid empty inventory is accepted.
- Add fixture-based coverage for populated, partially corrupted, and completely unresolvable 26.3 NBT backpacks.
- Test failure behavior during player disconnect, forced unload, shutdown, and scheduled saves.

## Compatibility and dependency coordination

- Keep the vendored source snapshot, compiled library JAR, and Paper compatibility boundary aligned when moving beyond 26.3 build 40.
- Replace the frozen binary snapshot with a smaller source-built utility module after the 26.3 release is proven in production.
- Add a compatibility test matrix for Paper 26.3 backpack load, save, migration, restart, MySQL, and SQLite behavior.
- Confirm the one shaded artifact uses the corrected 26.3 serializer behavior on a live server.

## Release quality

- Make the full vendored library source build reproducible from this repository, reducing reliance on the preserved binary snapshot.
- Expand build verification to exercise an actual populated 26.3 backpack fixture against each supported Paper build.
- Keep the README, changelog, known issues, compatibility matrix, recovery guide, and release checklist synchronized for every release.
- Document the exact Paper build and embedded library snapshot hash used for each published MinepacksForked version.
