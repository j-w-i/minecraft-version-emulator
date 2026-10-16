# Indev → Infdev repair handoff

**Later label-only fix:** the rebuilt mod now displays **Minecraft Indev** in Indev's top-left corner; Infdev stays **Minecraft Infdev**. Build/tests pass. The core runtime acceptance and SHA-256 below identify the earlier acceptance build, not this later text-only rebuild; those Minecraft sweeps were not repeated.

## Delivered scope

The default 256×256 Indev island can advance to Infdev without retaining the unchanged synthetic bedrock/dirt apron. The island itself is not regenerated. Exact, provenance-verified apron chunks are released during a locked stopped-world transaction; edited or uncertain chunks remain intact. Moderner Beta remains the terrain authority, with a 64-block coast collar on future terrain.

Early gameplay corrections cover **Indev 20100223, Infdev April20 and Infdev June11**: artifact-backed craft/smelt/food tables, server-side recipe/menu result enforcement, instant direct-heal food, historical oak-sapling quantity, early mob drops and no newly awarded skulls/discs/XP. Giants remain excluded. The ordinary-world controls retain modern behavior, including sheep mutton. See [the artifact audit](<../research/early-gameplay-audit.md>) for precise evidence and limitations; this is not a certification of every historical mechanic.

## Using the repair

**Back up the entire world first.** Keep the mod-generated backups too. No personal world was modified during development.

- For a new historical progression: `/hex upgrade infdev_420` or `/hex upgrade infdev_611`, then stop normally and reopen. These commands take **profile IDs**, not generation-preset IDs.
- For an already-upgraded world with the old border: run `/hex repair-finite-edge`, then stop normally and reopen. The command stages intent; it does **not** rewrite live terrain. In singleplayer, save and quit before reopening.
- Inspect `/hex status` and the server log. If evidence is insufficient or a backup/hash check fails, do not delete chunks or edit the journal to force it through. Refusal/retention protects existing content.

Custom finite dimensions/inline settings and unauthenticated external preset replacements are not assumed equivalent to the supported default island. Conservative repair may leave part of an old border in modified or unprovable chunks. That is intentional, not permission to erase builds.

## Current acceptance — 2026-10-16

[Full-shoreline acceptance](<../logs/20261016-core-repair-acceptance.json>) used the current [remapped JAR](<../build/libs/historical-experience-1211-0.1.0.jar>) and the real operator repair command on a new owned replica. Source fingerprints were unchanged.

- All **256 island chunks**, two protected exterior block/container/biome snapshots, a seven-diamond chest and one synthetic saved-player file remained unchanged.
- **183** proven apron chunks released; repair completed and survived restart.
- **1,008 unmodified boundary columns across all four sides** checked: no high bedrock, at most **one block** of surface difference. The 16 boundary columns belonging to the deliberately protected build were retained and reported separately.
- All **242 generated exterior terrain chunks** outside those two protected fixtures were independently scanned: no bedrock at Y8 or above.
- Build/licence artifacts pass; the unchanged **653-test** result remains valid. [Eight gameplay runtime controls](<../logs/early-mob-paths-acceptance-v1.json>) match the identical current artifact. The resumed build reused unchanged Gradle test inputs; it did not pretend to rerun the cached suite.

Artifact SHA-256: `b986fbd5980d8d393c559031dc4b3c68f30ef6d58ede7b71bc1ded6b91114006`.

## Boundaries

This is bounded repair acceptance, not production certification or an exhaustive later-era audit. Real client/login/pickup, process-level crash injection, genuine GUI-converted source coverage, arbitrary custom islands/datapacks and exhaustive physics/fuel/crop/tool behavior remain unverified. Recovery/corruption paths have automated tests, not a claimed process-crash campaign. Saved-player preservation used a synthetic NBT sentinel, not a logged-in account. Edited/uncertain exterior chunks are never silently deleted.

Transaction design and earlier independent seeds/destinations: [ADR0011](<ADR-0011-finite-island-transition.md>).
