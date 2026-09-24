MinepacksForked Release Checklist
================================

## Source review

- [ ] Review the diff for unrelated changes.
- [ ] Refresh the [Paper API build review](Paper%20API%20Release%20Review.md) against the latest seven official builds at release time.
- [ ] Confirm the embedded serializer was compiled and inspected against the Paper build being released.
- [ ] Confirm the supported-version guard includes the latest supported MC version.
- [ ] Confirm the 26.3 class includes the public `asBukkitCopy(ItemInstance)` conversion path.
- [ ] Confirm database backup and failed-deserialization protections remain enabled.

## Build

```bash
export JAVA_HOME=/server-data/minecraft/java/java25
export PATH="$JAVA_HOME/bin:$PATH"

java -version
mvn -B clean verify
scripts/verify-package.sh Minepacks/target/MinepacksForked-*.jar
```

- [ ] All required Maven dependencies resolve.
- [ ] Unit tests pass.
- [ ] Exactly one self-contained Minepacks plugin artifact is produced.
- [ ] `plugin.yml` names `Minepacks` and the real `at.pcgamingfreaks.Minepacks.Bukkit.Minepacks` class.
- [ ] Generated artifacts are excluded from Git.

## Runtime validation

- [ ] Test on a real Paper 26.3 server.
- [ ] Test one serialized empty backpack across restart; no recovery backup or error should be created.
- [ ] Test a populated backpack with metadata across restart.
- [ ] Restore an existing pre-update backpack.
- [ ] Save and reopen the backpack.
- [ ] Restart the server and repeat the restore test.
- [ ] Test MySQL.
- [ ] Test SQLite.
- [ ] Test the failed-deserialization backup path.
- [ ] Verify an unreadable SQL row or file triggers failure without creating a new cached backpack, even during player preloading.
- [ ] Test SQL migration rollback when a stored backpack cannot be read or copied.
- [ ] Verify command help and translated permission messages after startup and reload.
- [ ] Confirm no valid database record is replaced with an empty backpack.
- [ ] Test upgrading from a two-plugin installation and verify `plugins/Minepacks/` reuse.
- [ ] If using a former shared PluginLib pool, migrate `Database.Type` and credentials before startup.

## Git and release

- [ ] Review `git status` and `git diff --cached`.
- [ ] Confirm no database, server, IDE, target, or Graphify files are tracked.
- [ ] Update `docs/Change Log.md`.
- [ ] Update `docs/Known Issues.md` if runtime verification is incomplete.
- [ ] Set the Maven revision to `0.1.0` and tag v0.1.0 only after all runtime checks pass. Reserve v1.0.0 for a tested compatibility and upgrade contract, including live restore checks; Paper's artifact suffix is not a deciding factor.
