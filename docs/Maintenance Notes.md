MinepacksForked Maintenance Notes
=================================

MinepacksForked is maintained separately from the original Minepacks project. Upstream changes should be reviewed and selectively ported rather than merged blindly, especially changes involving database formats, serializers, Paper bootstrap behavior, or version detection.

The project embeds a frozen PCGF utility and NMS snapshot in `vendor/pcgf-pluginlib`. A Minepacks release that changes the supported Paper version should identify the exact Paper API and server builds used to compile and review the embedded serializer. The maintained override source and rebuild script are in this repository.

The Maven artifact and generated JAR are named `MinepacksForked`, but the runtime plugin name remains `Minepacks` intentionally. Bukkit and Paper derive the plugin data directory from the runtime name; retaining `Minepacks` preserves access to existing configuration, database settings, and stored data under `plugins/Minepacks/`.

When a Paper update changes CraftBukkit or NMS internals:

1. Reproduce the failure with a representative serialized backpack.
2. Fix and test the embedded serializer override first.
3. Update Minepacks' supported-version boundary.
4. Verify the single shaded JAR and its plugin descriptor with `scripts/verify-package.sh`.
5. Run database restore and restart tests before publishing.
