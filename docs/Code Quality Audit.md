# Code quality audit of the single-plugin branch

Reviewed on 2026-09-24 against the branch diff and the surrounding storage, migration, command, and build code. The largest touched Java file is `Database/Config.java` at 557 lines; no changed Java file crosses 1,000 lines. The audit focused on simplifying state boundaries and removing reflection and repeated persistence logic while keeping the existing backpack bytes, SQL tables, plugin identity, and public backpack retrieval signatures.

## Structural findings and changes

| Finding | Impact | Change |
|---|---|---|
| The two-method storage callback treated missing data, SQL errors, and failed deserialization identically. Both normal retrieval and player preloading created a new backpack on `onFail()`. | A failed restore could put an empty backpack in cache and later overwrite a preserved row or file. | Internal storage loads now report `found`, `missing`, or `failed`. Only `missing` permits creation. The public `Callback` API remains unchanged. |
| Deserialization wrapped a nullable array and a redundant success boolean, while SQL had a separate one-line rejection policy solely for that wrapper. | More states and indirection than the serializer can actually produce. | `Optional<ItemStack[]>` represents success or failure; a present all-null array remains a valid empty inventory. The wrapper and rejection helper are gone. |
| File saves, backups, and migrations each handled the version prefix and byte stream separately. Two migration paths used direct streams, and file-to-SQL read only once into a length-sized buffer. | Format logic drifted, partial reads could migrate truncated data, and a failed write could leave a partial file. | `BackpackFileStore` owns the one-byte version prefix, complete reads, and temporary-file replacement. SQL target migrations use transactions and rollback on copy failure. |
| Migration read SQL's protected fields and invoked lifecycle methods through reflection. Command startup also set private static fields through reflection. | Refactors could compile and still fail only during migration or startup; casts and string field names hid the contracts. | SQL exposes a typed migration column snapshot and formatter. Minepacks exposes explicit service lifecycle methods. Commands obtain the active plugin and manager through typed interfaces, without mutable static context. New command interface hooks have defaults for existing implementers. |
| MySQL and SQLite constructors retained a nullable external connection provider used only by the retired PluginLib shared-pool mode. | Every ordinary call site passed `null`, carrying a dead mode through the storage layer. | Constructors now create their own providers directly. |
| Migration could report an error from its worker thread, then attempt reload, with no guaranteed final callback. | Callers could receive results on inconsistent threads or no result after reload failure. | Migration completes with one result after the reload attempt on the scheduler's next tick. |

## Verification

Java 25 `mvn -o -B clean verify` passes six tests, including the empty-inventory decode boundary and file format/replace checks. `scripts/verify-package.sh` verifies the self-contained JAR's descriptor and embedded classes. `git diff --check` passes. The official Paper 26.3 build 40 server recognizes the JAR as one `Minepacks` Bukkit plugin before its EULA gate. Full plugin enablement, MySQL/SQLite migration, and a real backpack restart cycle remain release checks.

## Remaining design debt

- The embedded PCGF classes are still delivered as a checked-in binary snapshot plus source archive and a maintained serializer override. This keeps one runtime plugin and one repository, but rebuilding the entire library from source in the reactor would give a clearer dependency boundary. The exact source snapshot and override rebuild script are retained to make that replacement tractable.
- SQL query templates still use mutable strings and regex placeholder substitution. The typed migration columns remove reflection, but a broader query/schema rewrite needs database fixtures for both MySQL and SQLite before changing those templates.
- SQL-to-files migration writes each destination file atomically, but the whole directory migration is not one transaction. Source SQL remains intact if a later file fails; a retry may encounter already written destination files. A staged-directory approach merits a separate, fixture-driven change.
- File-backed backpack loading still performs file I/O synchronously. Moving it off the game thread requires preserving callback and scheduler behavior across Paper and Folia, so it remains outside this behavior-preserving refactor.
- A login preload and an on-demand lookup can still request the same uncached backpack concurrently. Both loads may complete and replace the cache entry. Single-flight loading needs a separate concurrency test with the actual scheduler before changing this legacy path.
