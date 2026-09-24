# Contributing to MinepacksForked

MinepacksForked is a separately maintained fork of Minepacks. Please keep changes focused and document compatibility-impacting changes in `docs/Change Log.md`.

## Before submitting changes

- Use Java 25 for Paper 26.x work.
- Preserve the existing database formats unless a migration is documented.
- Do not commit build output, server data, IDE metadata, Graphify output, or database backups.
- Run `mvn clean verify` and `scripts/verify-package.sh` when changing dependencies or serializers.
- Add a known issue when live-server verification is not available.

## Compatibility changes

Paper and Minecraft internals are version-sensitive. Changes to NMS conversion, the embedded PCGF snapshot, serializers, or version detection require:

- an explicit compatibility-matrix update;
- a changelog entry;
- a release-checklist review;
- a database restore test using existing serialized inventory data.

## Attribution and licensing

Retain upstream copyright and license notices. Clearly identify changes made in MinepacksForked and preserve attribution for Minepacks and its dependencies.
