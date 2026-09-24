MinepacksForked Backpack Recovery
=================================

Before upgrading Paper or MinepacksForked, stop the server and create both backups:

1. A database dump of the Minepacks database.
2. A copy of the Minepacks plugin data directory.

Do not open or save affected backpacks until serializer compatibility has been verified.

## Failed restore behavior

If the serializer reports failure, MinepacksForked:

- logs a severe error;
- attempts to write a backup of the original serialized data and reports if that backup fails;
- refuses to cache a replacement backpack;
- prevents the failed result from being saved over the database record.

An all-null decoded inventory is a valid empty backpack and does not trigger this path. A missing backpack can be created normally; an unreadable stored backpack cannot. The recovery backup is protection against further data loss. It is not a replacement for the original database backup.

## Recovery procedure

1. Stop the server.
2. Preserve the database and plugin data directory.
3. Confirm the Paper build is covered by the MinepacksForked compatibility matrix.
4. Install the corrected self-contained MinepacksForked build; no separate PluginLib plugin is required for Minepacks.
5. Start the server and test one affected backpack.
6. Confirm its contents after a server restart.
7. Only then allow normal player access.

Never run destructive SQL updates or deletes while investigating a restore failure.
