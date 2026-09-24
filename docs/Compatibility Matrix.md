MinepacksForked Compatibility Matrix
====================================

| Component | Supported target | Notes |
|---|---|---|
| Paper | 26.3 build 40 | Primary server implementation review target; full restart test pending |
| Paper API | 26.3.build.40-alpha | Compile-time coordinate; builds 34–40 compared |
| Java | 25 | Required by Paper 26.3 and the generated plugin classes |
| PCGF utilities and NMS | Embedded 0.0.1 source snapshot with local 26.3 conversion fix | Included in the Minepacks JAR; no second plugin required |
| Storage | Files, SQLite, MySQL | Existing Minepacks formats remain supported; shared PluginLib pool requires configuration migration |
| Folia | Bundled scheduler support | Must be verified on a live server |

## Compatibility boundary

The plugin uses a version-specific embedded NMS serializer. A Paper release newer than 26.3 build 40 should be checked for its item conversion method and database restore path before declaring compatibility. The `-alpha` coordinate suffix does not determine this risk: the serializer depends on server implementation details outside the Paper API. See the [builds 34–40 review](Paper%20API%20Release%20Review.md).

## Required smoke test

For each supported Paper build, test a backpack containing ordinary items, item counts, enchantments, renamed items, and custom item data across server restart and database reload.
