# Nether enforcement repair — 2026-10-13

## Cause and repair

Both the placed-feature filter and the natural-spawn filter resolved only a Moderner Beta generator marker.
The Nether's vanilla `NoiseBasedChunkGenerator` has no such marker, so correct exclusion tables were never
consulted. Both now use the active historical server scope as a fallback. The feature hook guards both
`placeWithBiomeCheck` and `place`, before any placement (including direct ore section writes).

The spawn hook filters biome and structure candidate lists before entities are instantiated. Endermen also
need a dimension boundary: their Nether spawning arrived in [16w20a / 1.10](https://minewiki.pl/16w20a#1).
They remain allowed in the Overworld and End when the selected version had them.

## Executed evidence

`gradlew.bat --console=plain build --stacktrace`: exit 0; **338 tests, 0 failures/errors/skips**.
A preceding build had two incorrect new test expectations (early Alpha Nether mobs and Beta magma cubes),
corrected against the registry, and another attempt had a transient Loom mapping-read error; retry passed.

Dedicated Fabric 1.21.1 server, Moderner Beta 5.0.0-alpha.4, seed `2508291534120694762`, structures enabled.
Each run scanned **49 full Nether chunks**, all sections; invoked the injected `NaturalSpawner.mobsAt`
path across all categories at Y=32/64/96; and exercised both excluded placed-feature entry points.

| Run | Quartz blocks | Magma / Nether gold / ancient debris / blackstone | Live sampled spawn candidates |
| --- | ---: | --- | --- |
| Release 1.0 final | 0 | all 0 | ghast, magma cube, zombified piglin |
| Release 1.5 | 6,063 | all 0 | ghast, magma cube, zombified piglin |
| Ordinary control | 4,821 | 1,145 / 1,861 / 59 / 6,268 | enderman, ghast, skeleton, strider |

All three runs exited 0 and wrote `success=true`; both historical runs reported no anachronistic blocks or
spawn candidates and no unresolved feature IDs. Every excluded feature returned false through the live hook.
The ordinary control recorded **zero feature suppressions and zero spawn-filter invocations**.

Retained raw verdicts:
- [Release 1.0](<../run/probe/sol-nether-enforcement-release100-final/generation-probe.json>)
- [Release 1.5](<../run/probe/sol-nether-enforcement-release15/generation-probe.json>)
- [Ordinary control](<../run/probe/sol-nether-enforcement-ordinary/generation-probe.json>)

Reproduction: use the already operator-authorized project-local probe server; select the appropriate world
preset and a fresh level name in its server properties, then run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.4.1'
.\gradlew.bat --console=plain runServer -PgenerationRunDir=run/probe -PgenerationProbe=true -PnetherProbe=true -PprobeToken=<unique-token> -PprobeRadius=0
```

Historical presets tested: `historical_experience:release_1_0_0_no_modern_content` and
`historical_experience:release_1_5_no_modern_content`; control: `minecraft:normal`.

## Deployment and limits

Deployed to the owner's existing Prism 1.21.1 instance; built/deployed SHA-256 prefix **7E27C27BF871AA71**,
verified identical by the deployment/state scripts. This is deployment verification, **not** a new Prism
client playthrough: the runtime tests above were dedicated development-server tests.

Existing chunks, blocks and entities are preserved. Restart Minecraft fully and use a new world to test fresh
Nether generation. Historical terrain shape, spawn weights and pack sizes are not established by this repair.
The live census directly covers Release 1.0/1.5 plus an ordinary control; all-version scope and mob-boundary
coverage is unit-level, not 29 independent runtime runs. Hoglins were not in the sampled Nether-wastes lists;
their exclusion is tested with the shipped crimson-forest list at unit level.
