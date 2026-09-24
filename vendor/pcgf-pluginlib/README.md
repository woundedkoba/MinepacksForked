# Embedded PCGF library snapshot

`pcgf-pluginlib-0.0.1.jar` is a GPLv3 snapshot of `PCGF_PluginLibForked` embedded into Minepacks' `Minepacks-EmbeddedLib` reactor module. The snapshot came from the sibling checkout at commit `bafbb5c145b028024223f0894cc24127dbfa49e0` with local uncommitted Paper 26.3 changes. The original locally built JAR had SHA-256 `eba286508c21a6980c693fab7684f08e218bc920e43720c98929d7d1a0870e34`. The full source tree as reviewed is preserved in `source-snapshot.tar.gz` (SHA-256 `73687419ccb978818b57dc4ee93a13bf37989ec516172ff30437824e2cd48b86`). It includes the original license and third-party notices; the embedded JAR carries those notices too.

The JAR after applying the maintained 26.3 serializer override has SHA-256 `d7418634466397d6824a58ade92314f867d65e59802c56e849cd1fb6c61504e6`.

The original 26.3 serializer looked only for `CraftItemStack.asCraftMirror`. Paper 26.3 build 40 exposes public `asBukkitCopy(ItemInstance)`. The maintained override source is `src/at/pcgamingfreaks/Bukkit/ItemStackSerializer/NBTItemStackSerializer_26_3_R1.java`; the class compiled from that source replaces the original class in the JAR. The override selects `asBukkitCopy` when available and retains `asCraftMirror` as a fallback. It also fails the whole save when any item cannot be encoded, instead of silently dropping that item. Every other embedded class is copied from the snapshot.

To rebuild the override, obtain and run the official Paper 26.3 JAR once in a temporary directory so Paperclip produces `versions/26.3/paper-26.3.jar` and downloads its `libraries/`. Then run:

```bash
export JAVA_HOME=/server-data/minecraft/java/java25
scripts/rebuild-embedded-serializer.sh /path/to/paper-runtime
mvn -B clean verify
scripts/verify-package.sh Minepacks/target/MinepacksForked-*.jar
```

The library snapshot is bundled so Minepacks has one runtime plugin and one source repository to maintain. `Minepacks-EmbeddedLib` excludes the PluginLib plugin entry point, Bungee classes, and plugin descriptors while retaining the utility classes and language resources Minepacks uses. Do not replace the JAR without reviewing its license, source changes, and serializer compatibility.
