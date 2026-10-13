# Matrix completion and the Beta envelope defect — 2026-10-13

## What was completed

Beta 1.4, 1.5 and 1.6 were the last three targets of the 29-version expansion matrix that were **named but not
implemented**: the catalogue listed them with a `planned_` key and the note "no gameplay profile or generation
recipe yet". They are now declared profiles with spawn sets, structure rules, settings presets, world presets
and a menu entry, so all 29 planned targets resolve to real worlds.
A new assertion, `HistoricalVersionCatalogueTest.everyPlannedTargetIsImplementedRatherThanMerelyNamed`, walks
the plan and fails on any target that is only named — the counts it replaces could not tell the two apart.

Spawn sets, each derived rather than listed so a correction propagates:

| Version | Set | Source |
| --- | --- | --- |
| Beta 1.4 | `BETA_1_7_3_MOBS` | the wolf is that build's addition, and it is the last mob before Beta 1.8 |
| Beta 1.5 | Beta 1.4's | weather, saplings, rails, cobweb, achievements — no entity |
| Beta 1.6 | Beta 1.5's | map, trapdoor, dead bush, grass, fern, Nether multiplayer — no entity |

Two proxy dates became true cases and were moved, because leaving them would have made a Beta 1.6 world
**refuse the vegetation that build introduced**: `short_grass` and `dead_bush` (plus `fern`) → Beta 1.6, and
`cobweb` → Beta 1.5. `scripts/audit-proxy-dates.py` now reports no date resting on a build that exists.

## The envelope defect, which the new versions exposed

Adding three Beta versions did **not** move
`GenerationBackport1211Test.supportedEnvelopeDoesNotReconstructTerrain…`, and that is what led to the real
finding. The count tracks settings presets that *state* the historical envelope, and the Beta ones stated
none: they inherited `moderner_beta:beta` bare. A runtime probe measured the consequence directly.

Alias `beta_1_3_no_modern_content`, nine sampled chunks, before the fix:

```
generatorMinY=-64   generatorDepth=192   deepslateDisabled=false
caveProvider=moderner_beta:voronoi
biomes={dripstone_caves=5, lush_caves=46, moderner_beta:beta_forest=93}
sampledPaletteFailures={minecraft:deepslate=8366, minecraft:emerald_ore=424}
```

So a world a player could create from the menu generated at the modern −64..320 height, with deepslate,
emerald ore and 1.17 cave biomes in it. Beta 1.7.3's own `content_preview_*` presets already carried the
correct envelope, which is what proved the omission was in these presets rather than a Moderner Beta limit.

Fix, in two parts for the project's usual reason:

* **Beta 1.4, 1.5 and 1.6** were declared in-session and no world exists on their recipes, so their eight
  settings presets were corrected directly.
* **Beta 1.0, 1.2 and 1.3** are published aliases; rewriting one would move terrain in chunks an existing
  world has not generated yet. Each therefore gains a NEW revision, `<version>_historical_envelope`, and the
  menu selects it. Existing worlds keep the generator they recorded.

A new assertion, `everyPublicMenuRecipeDeclaresTheReducedEnvelope`, reads each public world preset, follows it
to the settings preset the generator names, and requires `min_y` 0, `height` 128 and deepslate disabled. It is
the question the count was only approximating.

## Verification

`gradlew.bat --console=plain build`: exit 0, **340 tests, 0 failures, 0 errors, 0 skipped**.

Runtime probes, Fabric 1.21.1, Moderner Beta 5.0.0-alpha.4, seed `2508291534120694762`, dedicated server:

| Probe | depth / minY | deepslate | anachronistic blocks | notes |
| --- | --- | --- | --- | --- |
| Beta 1.6, before | 192 / −64 | enabled | deepslate, copper ore, emerald ore | 25 chunks |
| Beta 1.6, after | 128 / 0 | disabled | **none** | 25 chunks, cave biomes gone |
| Beta 1.3 `_historical_envelope` | 128 / 0 | disabled | **none** | 9 chunks |
| Beta 1.7.3 `content_preview_10`, origin | 128 / 0 | disabled | none | 25 chunks, ocean/beach region — no grass |
| Beta 1.7.3 `content_preview_10`, centre 400 | 128 / 0 | disabled | none | **grass_block 1,019, short_grass 46** |

The last two rows matter together: the origin sample contains no grass under the envelope, which looks like a
regression until the same preset is sampled away from the water. It is the seed region, not the envelope, and
Beta 1.7.3 — the project's most-verified profile — behaves identically to the three new builds.

Raw verdicts are retained under `run/probe/<world>/generation-probe.json`.

## Limitations

* Existing worlds on `beta_1_0/1_2/1_3_no_modern_content` keep their modern-height generator. Only **new**
  worlds get the envelope. No terrain is regenerated.
* Spawn weights and group sizes remain modern; only which entity types may spawn is enforced.
* Beta 1.4's locked chest, bed-spawn reset and achievement infrastructure, Beta 1.5's weather and rails, and
  Beta 1.6's map, trapdoor, Nether bed explosion and ore-bias fix are recorded in the registry evidence but
  are **not implemented behaviours**. They are the next content work, not claims.
* Only Beta 1.3 and 1.6 were probed at runtime; Beta 1.4 and 1.5 share their exact preset shape and are
  covered by the unit assertions above rather than by their own runs.
