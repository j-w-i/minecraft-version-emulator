# Moderner Beta 26.3 — integration findings (concise)

**Status:** upstream inspection complete; **doc-only, no runtime acceptance**. No Minecraft process was launched,
no world created. Facts are source-read, jar-data-read, or network-fetched; each is labelled.

**Machine evidence:** [`../research/moderner-beta-evidence.json`](../research/moderner-beta-evidence.json) holds the
full extracted preset JSON (Beta 1.7.3, Beta 1.8.1, world preset), per-file SHA-256 for all 71 presets, component
tables, pack inventories and reproduction commands. This document does not duplicate it.

**Exclusions:** no architecture decision (parent owns). No terrain algorithm recreation. Nothing in `.upstream/`
modified. Flashback (`j-w-i` fork) compatibility, recording, playback and seeking are **out of scope and
unassessed** — only Fabric/MC target viability is evidence here.

---

## 1. Exact artifact

| Fact | Value |
| --- | --- |
| Published version | `5.0.0-alpha.4+26.3-fabric` (Modrinth `version_number`, id `Ucgo1y8G`) |
| Published file | `moderner-beta-fabric-5.0.0-alpha.4+26.3.jar` — hyphens, **not** `moderner_beta-…` |
| Size / SHA-256 | 2,442,164 bytes / `2da0b0ded6053dbc18b630d73c3252e30f961ad920589edf75a6dbe243220727` |
| Tag → commit | `5.0.0-alpha.4` → `95f3da6c3f23e7e128b319e5c721201ed4c17243` (2026-09-15T23:22:16+03:00) |
| Branch tips | `work/26.3` = `e654082`; `multiversion` = `87c015b` — **neither is the release commit** |

`git merge-base --is-ancestor 5.0.0-alpha.4 work/26.3` → **false**; four commits exist only in the tag
(`c60fa25`, `aad8d49`, `567cc6c`, `95f3da6`). **Reproduce from the tag, never a branch head.** The existing
`.upstream/moderner-beta` clone is shallow with no tags, so it cannot serve as tag evidence.

Canonical remote is Codeberg (`fabric.mod.json` `sources`); GitHub is the mirror; both resolve the tag identically.

**License:** MIT (`LICENSE`, `fabric.mod.json`). `README.md` L51-L55 records LGPLv3 *up to and including* commit
`55519d1`. Provenance flag for ADR: `README.md` L61 credits a Nintendo Switch Edition decompilation as an LCE
reference — it does not touch the Beta presets, but should be recorded.

*Historical note (2026-10-14):* older Moderner Beta was LGPLv3 **up to and including** commit `55519d1`, so do not
read the pinned `5.0.0-alpha.4+1.21.1-fabric` artefact's MIT licence as covering those earlier revisions. This
project copies exactly one Moderner Beta data file (`dimension_type/beta_1_7_3_overworld.json`) from the pinned
artefact, whose licence is MIT; the copied file and the MIT text are recorded in
[THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) and [PROVENANCE.md](PROVENANCE.md).

**Target:** Fabric/Quilt, MC `26.3.x`, Java 25, loader `>=0.16.0` (mod pins `0.19.3`), `fabric-api: "*"` (mod pins
`0.160.4+26.3`). Verified present: java `25.0.4.1+1-LTS` on this host; 26.3 is Mojang's current release; Fabric meta
lists loaders to `0.19.5` for 26.3; both `fabric-api-0.160.4+26.3.jar` (2,587,950 B) and `0.161.0+26.3.jar`
(2,620,967 B) return HTTP 200. Declaration-level only — no coexistence was tested.

---

## 2. Presets and era coverage

Era selection is a **settings preset id** in registry `moderner_beta:settings_preset`, not a world type. The mod
registers **one** world preset, `moderner_beta:moderner_beta`.

- **71** preset JSON files ship in the jar under `data/moderner_beta/moderner_beta/settings_preset/`
  (67 registered in `ModernBetaSettingsPresetData.bootstrap` L59-L129, plus 4 large-biomes/amplified overlay files).
- Requested eras present: **Beta 1.7.3** = `moderner_beta:beta`; **Beta 1.8.1** = `moderner_beta:beta_1_8_1`
  (+`_large_biomes`, +`_amplified`); `beta_1_1_02`; `beta_1_9_pre_3` (+variants); `alpha` + `alpha_winter`;
  `infdev_227/325/415/420/611`; `indev` (+`_paradise`,`_woods`,`_hell`); `classic_0_30`,
  `classic_0_0_14a_08`; `pe`; `release_1_0_0/1_1/1_2_5/1_6_4/1_12_2/1_17_1` (+variants); `bedrock_1_2`,
  `bedrock_1_17`, `legacy_console_*`, `beta_xbox_legacy`; plus novelty presets.

**Coverage gap — recorded, not substituted (parent decision):**

- **No Pre-Classic preset.** Earliest is `classic_0_0_14a_08`.
- **No Release 1.7.10 preset.** Ships `release_1_6_4` and `release_1_12_2`; 1.7.x is skipped.

Neither is to be dropped or replaced by a nearest version. Exact-era mapping is deferred research; the concrete
input is that the two release presets differ in **seven** components despite sharing a key set — cave generation
(`seedMethod` `early_release`→`modern`, Beta force flags on→off), noise generator settings (`overworld_128`
Y −64..127 → `overworld_256` Y −64..255), surface-noise mode (`arraySurfaceNoise`→`simplexSurfaceNoise`),
`forced_biome_height` override sets (6 `*hills` entries → ~80 full modern biome ids), structure modifiers (ocean
shrine registered → none), surface properties (`gravelOceanBed`/`legacyBadlandsBands` false→true), and biome
settings (1.12.2 adds `use_32bit_layer_seed`). Both build their layer pipelines in
`ModernBetaSettingsPresetData.java` (`preset164` L1396, `preset1122` L1968); those builders plus
`level/biome/provider/fractal/**` are the starting point for deciding whether 1.7.10 is representable.

---

## 3. Serialization (schema only; full JSON in evidence)

`ModernBetaSettingsPreset.CODEC` (`ModernBetaSettingsPreset.java` L40-L48): optional `name`/`description` Components
plus **required** `chunkSettings`, `biomeSettings`, `caveBiomeSettings`, each `ModernBetaSettings.WORLD_CODEC` keyed
by registered `SettingsComponentType` ids. Absent keys take the codec default from `SettingsComponentTypes.init()`
L96-L488. A bare reference form (`PRESET_REFERENCE_CODEC`, L50-L55) is an object with a `preset` key;
`SETTINGS_TEXT_CODEC = either(...)` (L57) is what dedicated-server `generator-settings` text is parsed with.

`moderner_beta:default` is a **sentinel, not a file** (`ModernBetaSettings.java` L51) that resolves through
`ModernerBeta.getDefaultPresetId()` (`ModernerBeta.java` L198-L202), defaulting to `moderner_beta:beta`.

---

## 4. Dimensions and height (arithmetic corrected)

`maxY inclusive = min_y + height - 1`. The mod's own pins (`ModernBetaNoiseSettings.java` L14-L19), consumed at
`ChunkProviderNoise.java` L88-L90:

| id | min_y | height | maxY | blocks | used by |
| --- | --- | --- | --- | --- | --- |
| `moderner_beta:overworld_128` | −64 | 192 | **127** | 192 | beta, beta_1_8_1, beta_1_9_pre_3, release_1_0_0…1_6_4 |
| `moderner_beta:overworld_256` | −64 | 320 | 255 | 320 | release_1_12_2, release_1_17_1 |
| `moderner_beta:sky_128` | 0 | 128 | 127 | 128 | skylands-style presets |
| `moderner_beta:finite_2d` | 0 | 256 | 255 | 256 | indev / classic / pe |
| `moderner_beta:infdev_415` | −64 | 192 | 127 | 192 | infdev_415 |

- `_128` does **not** mean height 128; `overworld_128` is Y **−64..127**.
- The historical Beta 1.7.3 band **0..127 is a strict subset** of that envelope — the only excess is the 64 blocks
  at Y −64..−1. The open question is therefore narrow: **does anything generate below Y 0?**
- Vanilla `minecraft:dimension_type/overworld` in the 26.3 jar (read from the local read-only install,
  `%APPDATA%/.minecraft/versions/26.3/26.3.jar`, `data/minecraft/dimension_type/overworld.json`) is
  min_y −64 / height 384 / logical_height 384 → **Y −64..319**. The Beta world is thus 192 blocks shorter than
  vanilla at the top; Y 128..319 lies outside the generator's declared range. Whether that reads as void is
  **unverified**.
- Bedrock is placed from `bedrockFloor = worldMinY = -64` (`ChunkProviderNoise.java` L92), so a Y −64 floor is the
  code's intent; whether the Beta density leaves it empty is **not** derivable from source.
- Density axis: `ChunkProviderNoise3D` indexes `noiseY = y + noiseMinY` over the whole envelope (L615-L621) and takes
  its vertical term from `(blockY - depth) * stretchY / scale` (L744-L755) — anchored on block Y and the
  noise-driven `depth`, not on a fixed historical anchor.
- `moderner_beta:use_carvers` appears in every preset's `cave_generation` but **no 26.3 code path was found reading
  it**; treat its effect as unknown.

**Runtime probe required** (cheap, settles it): sample one column and record bedrock floor Y, lowest solid stone Y,
waterline Y (expect 64), and highest terrain Y. Historical Beta 1.7.3: bedrock Y 0..4, sea level 64, ceiling 127.

---

## 5. Structures, decoration and anachronism (namespace is not evidence)

**Structures:** `StructureModifiers.getDefault` (`StructureModifiers.java` L39-L47) adds only
`moderner_beta:ocean_shrine` (spread 64/16, salt 357, triangular) and removes **nothing** (beta JSON
`"removed": []`). `createState` (`ModernBetaChunkGenerator.java` L299-L326) filters only by
`hasBiomesForStructureSet`, so vanilla structure sets stay candidates. Whether any actually places is **unverified**.

**Decoration:** all 56 `moderner_beta` biomes carry `features` arrays mixing vanilla and mod features
(e.g. `data/moderner_beta/worldgen/biome/beta_plains.json` L153-L215). `applyBiomeDecoration`
(`ModernBetaChunkGenerator.java` L893-L900) calls `super` with no era filter. Mob spawns come from 26.3's biome
`attributes` block and are modern (`beta_plains.json` L2-L143).

A `minecraft:*` id is **not** proof of anachronism — monster rooms, springs, pumpkins, sugar cane, flowers, lakes and
dirt/gravel/coal/iron/gold/redstone/diamond/lapis ore all existed in Beta. Classification by produced content:

| Class | Examples | Action |
| --- | --- | --- |
| **Post-Beta content** (did not exist in 1.7.3) | `amethyst_geode` (1.17), `ore_copper` (1.17), `ore_tuff` (1.17), `underwater_magma`, `glow_lichen` (1.17), `patch_firefly_bush_near_water` (1.21.5+), `monster_room_deep`, and the 1.18 ore variants `ore_diamond_medium/large/buried`, `ore_granite/diorite/andesite_lower`, `ore_iron_middle/small`, `ore_coal_lower`, `ore_gold_lower`, `ore_redstone_lower`, `ore_lapis_buried` | candidate removals; 10 entries listed in evidence |
| **Historical but algorithm-unvalidated** | `monster_room`, `lake_lava_*`, `spring_water/lava`, the non-split ores, `patch_pumpkin`, `patch_sugar_cane`, `flower_plains`, `disk_*`, `freeze_top_layer`, tree features | presence expected; **distribution/Y-band/count must be validated against historical evidence** before any fidelity claim |
| **Mod-authored** | `moderner_beta:ore_clay`, `ore_emerald_y95`, `patch_*`, `trees_beta_*`, `freeze_top_layer` | intended historical layer; still needs validation — authorship ≠ accuracy |

Counts (1753 `minecraft:*` refs / 75 distinct vs 373 `moderner_beta:*` / 41 distinct; 21 modern refs in all 56
biomes) measure **reuse, not anachronism**. **Never strip all `minecraft:*` features as "modern"** — that would
delete Beta-appropriate ores, monster rooms, springs and vegetation.

**Upstream optional mitigation (not a fix, not activated):** `ModernerBeta.BUILT_IN_PACKS = ["reduced_height",
"deepslate_blobs"]` (`ModernerBeta.java` L64-L67), shipped in the jar, registered **selectable not forced**
(`PackActivationType.NORMAL`, `ModernerBetaFabric.java` L35-L50). `reduced_height` (47 entries) re-scopes vanilla ore
placement, vanilla overworld density functions, vanilla noise settings and `minecraft:dimension_type/overworld`;
`deepslate_blobs` (3 entries) touches only `ore_coal_upper` + `ore_tuff`. Neither removes the post-Beta entries above
(they live in biome `features` arrays). Because `reduced_height` rewrites vanilla dimension/noise data, it would
affect unrelated normal Moderner Beta worlds — **parent blocked blind activation until measured.**

---

## 6. Project preset bootstrap contract (verified)

The parent's design — preset `historical_experience:beta_1_7_3` inheriting `moderner_beta:beta`, referenced from the
world's `provider_settings` as a durable marker — is **supported by the shipped jar and source**.

**Inheritance:** `ModernBetaSettings.mapPreset` L139-L155 reads the map's `moderner_beta:preset` key, resolves it via
`ModernBetaSettingsPreset.getPreset` L266-L283, then
`settingsProvider.apply(base).extend().addAll(this.extend().remove(PRESET)).build()` — **local keys win**, and it
loops (multi-level chains work; a cycle loops forever). Component id `moderner_beta:preset`, value `Identifier`
(`ModernBetaBuiltInTypes` L8; `SettingsComponentTypes.java` L99-L103).

**The marker persists:** the generator stores the map verbatim (`ModernBetaChunkGenerator.java` L115
`resolveDefaultPreset()`, which expands only the `moderner_beta:default` sentinel) and its codec writes that field
back as `provider_settings` (L76-L83). `ModernBetaBiomeSource` behaves identically for
`provider_settings`/`cave_provider_settings` (L53-L60, L81-L87). Lookup order: registry, then saved-preset pack
(L274-L277).

**Minimal pack** — `world/datapacks/<pack>/pack.mcmeta` (mod's own packs use `min_format`/`max_format` 121) plus
`data/historical_experience/moderner_beta/settings_preset/beta_1_7_3.json`:

```json
{
  "name": { "color": "yellow", "translate": "historical_experience.preset.name.beta_1_7_3" },
  "description": { "translate": "historical_experience.preset.desc.beta_1_7_3" },
  "chunkSettings":     { "moderner_beta:preset": "moderner_beta:beta" },
  "biomeSettings":     { "moderner_beta:preset": "moderner_beta:beta" },
  "caveBiomeSettings": { "moderner_beta:preset": "moderner_beta:beta" }
}
```

Overrides are sibling keys, e.g. to drop the anachronistic deepslate band while inheriting the rest:

```json
"moderner_beta:deepslate_generation": { "block": "minecraft:deepslate", "enabled": false, "maxY": 8, "minY": 0 }
```

All three maps are required (`fieldOf`, not `optionalFieldOf`). Any registered component can be overridden
(defaults table: `SettingsComponentTypes.init()` L96-L488).

**Wiring:** world `provider_settings` = `{"moderner_beta:preset":"historical_experience:beta_1_7_3"}`; dedicated
server `generator-settings={"preset":"historical_experience:beta_1_7_3"}` (`WorldDimensionDataMixin.java` L46-L48);
world preset must remain `moderner_beta:moderner_beta` (L44). **Do not use config `defaultSettingsPreset`** — it
only feeds the `default` sentinel and would change every sentinel world.

**Cave avoidance candidate (parent brief):** `caveBiomeSettings."moderner_beta:provider" = "moderner_beta:none"`
is the shipped first-party switch for cave **biomes** (`ModernBetaSettings.noCaveBiomes()` L108-L112), exactly what
`alpha.json` does. Beta instead uses `moderner_beta:voronoi` with modern points (`lush_caves`, `dripstone_caves`,
`deep_dark`, `sulfur_caves`) over `depthMinY −64`/`depthMaxY 64`. Correction to an earlier wording: this is cave
**biomes** — cave *terrain* carving is enabled in **both** alpha and beta (only infdev/indev/classic use
`CaveGeneration.DISABLED`). A biome-injection override is the second candidate; neither was validated.

**Failure modes (fail-safe, not silent):** a missing/disabled preset leaves the map unresolved with a WARN
(`ModernBetaSettingsPreset.java` L279) and then throws
`Component of type "moderner_beta:provider" supplied a null value!` (`ModernBetaSettings.java` L190-L197) during
`SERVER_STARTING` — the world does not start, so the pack must exist before opening it. A reference cycle loops
forever. Per-biome decoration cannot be fixed by any preset (`ModernBetaSettings` has no feature-list component).

---

## 7. Generator selection APIs

| # | Surface | Evidence |
| --- | --- | --- |
| A | World preset id `moderner_beta:moderner_beta` | `ModernBetaWorldPresets.java` L33 |
| B | `server.properties` `generator-settings` | `WorldDimensionDataMixin.java` L37-L68; reference **or** full preset JSON; fires only when the world preset is already Moderner Beta |
| C | `config/moderner_beta.json` → `…config_miscellaneous.defaultSettingsPreset` | `ModernerBeta.java` L126-L156, L198-L202; `MiscConfig.java` L16 — global sentinel only |
| D | Stored `level.dat` generator/biome source | shape in evidence `serializationSchema.worldLevelDatShape` |
| E | Java: `ModernBetaSettingsPreset.getPreset(...)` L266-L283; `WorldDimensions.replaceOverworldGenerator(...)` used at `WorldDimensionDataMixin.java` L53-L64; `ModernBetaSettings.mapPreset` L139-L155 | programmatic |
| F | Saved user presets (`config/moderner_beta/datapacks/moderner_beta_saved_presets`, hash-addressed ids) | `ModernBetaSavedPresetPack.java` — **client-only UI** |

Server-side automation is limited to B, C, D. World-creation/settings/JSON/export screens are client-only;
`DebugProviderSettingsCommand` registers **only when `ModernerBeta.DEV_ENV`** (`ModernerBetaFabric.java` L63-L65).

---

## 8. Unsafe generator switching points

Providers are built **exactly once per server start** (`ModernBetaLevelInitializer.initStarting` L17-L39 on
`SERVER_STARTING`, `ModernerBetaFabric.java` L67), so restart-with-a-changed-preset is the only supported switch —
and it is inherently partial (only never-generated chunks change).

| Unsafe point | Why | Evidence |
| --- | --- | --- |
| Editing config `defaultSettingsPreset` | every `default`-sentinel world re-resolves on next start; generated chunks keep old blocks → permanent seam | `ModernBetaSettingsPreset.java` L266-L283; `ModernerBeta.java` L198-L202 |
| Editing the config while running | read once at mod init; the mod also writes it back | `ModernerBetaFabric.java` L56; `ModernerBeta.java` L181-L196 |
| Live provider/settings swap | no setter; `ChunkProviderHolder.set` private; noise router, density functions, aquifer surface level, carvers and biome-injection caches are already captured; `RandomState`/structure state seeded | `ModernBetaChunkGenerator.java` L85-L186, L279-L296, L1008-L1019 |
| Editing stored `level.dat` settings maps | merged with the referenced preset and re-diffed; an unknown provider id throws at init | `ModernBetaSettings.java` L139-L155, L205-L229; `ModernBetaChunkGenerator.java` L282-L289 |
| `/reload` | clears biome-injection caches but does **not** re-run `initStarting` → caches and providers can disagree | `ModernerBetaFabric.java` L69-L73; `ModernBetaLevelInitializer.java` L41-L63 |
| Changing the world-preset type | generator/biome codecs are type-specific; the server-path injection fires only for Moderner Beta | `WorldDimensionDataMixin.java` L44-L67 |
| Reading `getGenDepth()`/`getMinY()` before `initProvider` | falls back to 192 / −64, then changes after init | `ModernBetaChunkGenerator.java` L949-L966 |

**Required upgrade procedure:** stop cleanly and drain generation → back up world **and** config → change exactly one
input → start and watch for the mod's `Unable to read settings JSON/NBT!` revert lines (L150, L193, L254) → generate
only in new territory → verify by content inspection (the dev-only debug command is unavailable in production).

---

## 9. Acceptance recipe and integration (not executed)

**External facts verified:** MC 26.3 current release; java 25 installed; 253 Fabric loader entries for 26.3;
`fabric-api-0.160.4+26.3.jar` HTTP 200; installer `1.1.2`.

**A. Deterministic, no game launch:** fetch tag `5.0.0-alpha.4` (`95f3da6`), run `./gradlew runDatagen` with JDK 25,
and diff `src/main/generated/data/moderner_beta/moderner_beta/settings_preset/*.json` against the jar's copies;
expected byte-identical for all 71 files (hashes in evidence).

**B. Dedicated server (needs explicit human EULA authorization):** work only under `run/`; install Fabric server
26.3 / loader 0.19.3; `level-type=moderner_beta:moderner_beta`;
`generator-settings={"preset":"historical_experience:beta_1_7_3"}`; config as in §7C. Assert: no settings-parse
error; `level.dat` stores generator `moderner_beta:moderner_beta`; fresh-region biomes are `moderner_beta:beta_*`.

**C. Probes that source reading cannot settle:**
1. column probe from §4 (bedrock floor, lowest stone, waterline, terrain ceiling);
2. decoration classification against §5 — post-Beta blocks absent/present, historical features present with
   distribution recorded;
3. structure list over a wide sample;
4. negative path: `{"preset":"moderner_beta:does_not_exist"}` must warn/refuse, not silently fall back
   (`ModernBetaSettingsPreset.java` L279; `WorldDimensionDataMixin.java` L48);
5. restart-upgrade: stop, back up, change to `moderner_beta:beta_1_8_1`, restart, generate in a **new** area,
   confirm old chunks byte-identical.

**Minimal external dependency:** `moderner-beta-fabric-5.0.0-alpha.4+26.3.jar` pinned by **SHA-256** (version
strings are ambiguous across MC targets), + Fabric API 26.3, MC 26.3, Java 25. Keep upstream external — no fork, no
vendor, no algorithm recreation. Persist the preset id, jar hash, MC/loader versions and enabled packs in project
metadata: the mod stores only settings maps plus the `default` sentinel, so a world directory alone does not record
its era. First-slice status: **terrain-integration prototype with modern leakage an explicitly unmet criterion.**

---

## 10. Verified vs assumed

**Verified:** tag/branch identity and divergence; MIT license + README history; all 71 preset JSON files with
hashes; codec field names for preset/settings/generator/biome source; the `default`-sentinel chain and config keys;
structure-modifier defaults; per-biome feature composition and the post-Beta content list; built-in pack contents;
provider-init timing; vanilla 26.3 overworld bounds (read from the local install, read-only).

**Assumed / unverified — do not treat as fact:** no Minecraft process was ever started, so nothing here is runtime
evidence; whether anything generates below Y 0 in a Beta 1.7.3 world; whether modern structures or modern cave
biomes actually place; whether the jar's JSON matches a fresh `runDatagen` (hashes provided for checking); whether
`use_carvers` still has an effect; how a Beta world's Y 128..319 behaves; whether data-pack-only presets appear in
the client preset list; Flashback fork compatibility of any kind.
