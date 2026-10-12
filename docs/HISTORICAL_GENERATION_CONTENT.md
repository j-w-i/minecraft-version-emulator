# Historical generation-content compatibility — bounded design

Current target is **Minecraft 1.21.1** by the owner's 2026-10-13 instruction. The current repair,
source parity and runtime evidence are recorded in the [generation backport report](<BACKPORT-1211.md>).
The revision history below describes the preserved **26.3 reference** (including its later revisions),
not the current 1.21.1 port status. Backport parity is not a claim of complete historical gameplay or
Nether/End fidelity.

## Implemented revisions and their measured results

Each revision is a new immutable alias; no alias is ever edited, and every already generated world keeps loading with the recipe it recorded.

| Revision | Delta | Measured in actual chunks | Verdict |
| --- | --- | --- | --- |
| 1 | deepslate generation disabled, cave-biome provider `moderner_beta:none` | deepslate stone **0**, modern cave-biome quarts **0**, but **91** `deepslate_*_ore` blocks | partial — correctly failed the content gate |
| 2 | + reduced level envelope (dimension type `min_y 0`) | deepslate ore variants **1**, but **bedrock 0** | deficient — the noise envelope was still unreduced |
| 3 | + Moderner Beta's reduced-height noise envelope | **`bedrockY=3..0`**, `blockAtMinY=minecraft:bedrock`, bedrock **6947**, deepslate stone **0**, deepslate ore variants **0**, cave-biome quarts **0**, `waterTopY=63` (sea level 64), `minY=0`, `maxY=319`, 91 checks PASS twice | envelope complete; content still anachronistic |
| 4 | + the first researched **content restrictions** (generation-time hook) | **272 placements suppressed**, restricted families **absent**, `unresolvedExclusions` empty; Beta's own ores retained (coal 629, iron 493, gold 46, redstone 251, diamond 104, lapis 136, gravel 5675, dirt 3007); bedrock 6947; establish and reload PASS | first revision that suppresses modern content |

Evidence: [revision 1 establish](<../logs/continuation-19-preview-establish.log>), [revision 3 establish](<../logs/continuation-50-preview3-establish.log>), [revision 3 reload](<../logs/continuation-52-preview3-reload.log>), [revision 4 establish](<../logs/continuation-72-rev4-establish.log>), [revision 4 reload](<../logs/continuation-74-rev4-reload.log>), [revision 2 preservation](<../logs/continuation-54-preview2-preserve.log>).

Every content-preview verdict carries `preview.fullHistoricalContentAcceptance=false`, and the verdict gate refuses `scope=content-preview` as historical acceptance (negative control `probeVerdictPreviewScopeNegativeCheck`). A green preview therefore cannot be mistaken for Beta fidelity.

### Envelope provenance

Both envelope files are Moderner Beta's **own published** reduced-height data, extracted byte-for-byte from the pinned jar's `resourcepacks/reduced_height` and re-homed under our namespace so the behaviour is per-world instead of global:

| Our resource | Upstream source | SHA256 of upstream bytes |
| --- | --- | --- |
| `dimension_type/beta_1_7_3_overworld.json` | `resourcepacks/reduced_height/data/minecraft/dimension_type/overworld.json` | `a0c92a6ed66d831579de34740a58c1e755b9b7fda2f25ecc4fbad6fa461c028c` |
| `worldgen/noise_settings/beta_1_7_3_overworld_128.json` | `resourcepacks/reduced_height/data/moderner_beta/worldgen/noise_settings/overworld_128.json` | `1633facf867837d9db45bd1ff419cac57849a6980c6ad8022bd57a7dae242c82` |

No noise, density, router, material or surface value was reconstructed. The reduction is expressed through Moderner Beta's supported `noise_settings` registry entry and dimension type, referenced by the settings preset's `moderner_beta:noise_generator_settings` component and the world preset's overworld `type`. Editing the level envelope alone was **measured** to remove bedrock, which is why both halves are required together; `BetaEnvelopeDataTest` pins the upstream values and the probe cross-checks the two envelopes against each other.

Moderner Beta's reduced height keeps a 320-tall build range (`height`/`logical_height` 320). Beta 1.7.3's own build limit of 128 is a **separate, still-open** fidelity question and is deliberately not guessed at by shrinking that value.

### Where the remaining anachronisms come from

The remaining modern blocks are not settings-controlled; they are attached to biomes in Moderner Beta's Java biome builders (`level/biome/ModernBetaBiomeFeatures.java`):

- `addDefaultFeatures` → `BiomeDefaultFeatures.addDefaultOres` (coal/copper/iron/gold/redstone/diamond/lapis, plus their deepslate targets) and `BiomeDefaultFeatures.addDefaultCrystalFormations` (**amethyst geodes**: amethyst, budding amethyst, buds, calcite, smooth basalt).
- `addMineables(..., addAlternateStones, addNewMineables)` with the compile-time constants `BETA_ADD_ALTERNATE_STONES = true` (granite, diorite, andesite blobs) and `BETA_ADD_NEW_MINEABLES = true` (tuff, glow lichen).
- `addLakes`, `addSprings`, `addCarvers` and the individual biome vegetation methods (which also add later-version vegetation such as `leaf_litter`).

Measured leftovers in revision 3: amethyst family, `copper_ore` 588, `andesite` 6881, `diorite` 7480, `granite` 8824, `tuff` 408, `glow_lichen` 21, `magma_block` 11. Because these are compile-time Java flags and per-biome feature lists, **configuration cannot express the restriction per world**, so the smallest profile-scoped generation-time hook is justified.

### The content hook, and its measured effect

`HistoricalFeatureExclusions` holds an **append-only** table from a persisted recipe alias to the restrictions that revision selects, and `FeaturePlacerHistoricalContentMixin` cancels `FeaturePlacer.placeWithBiomeCheck` at HEAD for those features. Design properties, each one deliberate:

- **Per-world by construction.** The decision comes from the generator's own stored marker, so no ambient state is read, nothing leaks between concurrently generating worlds, and no cleanup is needed on an exception path.
- **Cancellation returns "nothing placed"**, which is also the value the method already returns when placement finds no position. Nothing is generated and then removed, and because `applyBiomeDecoration` reseeds the shared random per feature, suppressing one does not perturb another's output.
- **Append-only binding.** A restriction is selected by the alias a world persists. Revisions 1–3 select none, so an already generated world cannot silently change recipe; revision 4 is the first that selects them, and rows are never edited or re-pointed.
- **Fail-loud configuration.** The mixin config is `required` with `defaultRequire: 1`: if the hook ever fails to apply, the game fails instead of quietly generating modern content. An exclusion that cannot be resolved against the live placed feature registry is reported through `unresolvedExclusions`, not swallowed.
- **Names are used only to resolve, never to compare.** The skip test is identity against the registry's own `PlacedFeature` instances, so a same-named entry from another addon cannot be filtered by accident.

The three-way controlled comparison at an **identical seed** is the acceptance evidence:

| World | Revision selects restrictions | Hook skips | Restricted families present |
| --- | --- | --- | --- |
| Revision 4 | yes | **272** | **none** (`{}`) |
| Revision 3, freshly generated | no | **0** | amethyst 734, calcite 769, smooth basalt 867, copper 588, tuff 398, granite 8822, diorite 7373, andesite 7000, glow lichen 21, magma 11 |
| Ordinary Moderner Beta (no marker) | n/a | **0** | all of the above **plus** deepslate 109627, sculk 1506 (a deep dark biome and ancient-city structure) |

Because the seed is identical, the only differences between the first two rows are the suppressed families — and the freed volume reappears as ordinary stone (79925 → 107365), which is what a correct suppression looks like. The third row is the non-interference proof: a plain Moderner Beta world and every ordinary world keep their full modern content, including deepslate. Method: [revision 4](<../logs/continuation-72-rev4-establish.log>), [revision 3 fresh](<../logs/continuation-67-rev3fresh-reload.log>), [ordinary upstream](<../logs/continuation-69-upstreamfresh-reload.log>).

## Three responsibilities

1. **Terrain:** the actual corresponding Moderner Beta preset, loaded through its supported registry/codecs, owns landscape/noise/climate and supported historical algorithms.
2. **Generated content:** a researched, version-aware generation recipe owns permitted ores/distributions, vegetation, decoration, caves/biomes, structures, liquids, spawns, bounds/bedrock and dimension rules.
3. **Gameplay:** the same historical version registry selects actual simulation rules; Nostalgic Tweaks is a selectively reviewed implementation resource, not a dependency or historical authority.

No post-generation removal, registry deletion, silent nearest-version substitution, global mutation of ordinary-world biomes, or live generator swapping.

## Verified pipeline boundaries

Pinned Moderner Beta alpha4 maps each complete stored chunk/biome/cave settings block through `ModernBetaSettings.mapPreset` before provider consumption. The new [adapter evidence](<../src/main/java/net/historicalexperience/integration/modernerbeta/ModernerBetaAdapter.java>) compares whole component maps, rather than provider identifiers or selected numeric fields.

- Chunk provider: terrain, surface and supported cave/noise behavior.
- Biome/cave providers: climate selection and cave-biome injection.
- Per-biome carver/feature arrays: mix Moderner Beta historical implementations with modern vanilla entries.
- Vanilla `ChunkGenerator.applyBiomeDecoration`: invokes configured placement and modern structure starts; the preset alone supplies no era filter.
- Mob generation/natural spawns: modern biome environment-attribute spawn tables also contain later entities.
- Minecraft dimension types: storage/build envelope, distinct from generator noise bounds.

The pinned Beta data itself enables deepslate and modern Voronoi cave biomes. The pinned Beta forest biome lists copper, geodes, glow lichen, modern ore placements, bee-tree variants, and modern spawn entries. These require content-level investigation, not a terrain-algorithm rewrite.

A real direct upstream Beta world generated and separately reloaded with all **12/6/2** component maps equal to the live upstream preset, three direct `moderner_beta:beta` references, and initialized providers. See [establish](<../logs/continuation-09-upstream-establish.log>) and [reload](<../logs/continuation-10-upstream-reload.log>). It remained ordinary/unadopted. This is **backend-only** acceptance, not Beta content fidelity; the [verdict](<../logs/continuation-10-upstream-reload-verdict.txt>) explicitly records `scope=upstream-preset`.

## Bounded Beta 1.7.3 implementation contract

- Resolve one immutable historical version into an authoritative upstream terrain preset plus an independently versioned generation recipe and gameplay rules.
- Preserve legacy v1 resources and persisted bindings. New compatibility settings require a new generation-recipe binding; old worlds must not silently acquire changed configuration through an overwritten alias.
- Prefer sparse, necessary supported overrides: investigate the existing deepslate disable and no-cave-biome provider first. Keep historical climate/noise/scales/cave algorithms unchanged unless primary evidence proves a specific content behavior inappropriate.
- Investigate supported generation geometry and Minecraft dimension bounds for the required historical height/bedrock rules. Do not duplicate density routers/material/noise parameter databases. A changed bound must be justified and tested independently from terrain identity.
- For per-world placed features/carvers/structures/spawns, investigate datapack configuration first. Where a global registry override would affect ordinary worlds or an API cannot express the restriction, use the smallest profile-scoped generation-time hook. Threaded generation must not leak recipe context between worlds/tasks or on exceptions.
- Ore rules require primary-verified attempts, sizes, ranges/distributions and geometry, not a block denylist or merely renaming modern ore placements. Unverified data fails acceptance.
- Dimension support must be explicit. Unmodified modern Nether/End is not historically validated Beta generation. Do not label unavailable behavior complete or silently substitute a different era.

## Acceptance matrix

Status against the matrix after revision 3:

1. Complete upstream foundation comparison and exact audit of every necessary compatibility delta; no unrelated terrain keys changed. — **Done per revision.** The comparison is now derived from the deltas each revision declares, and the probe additionally asserts the declared deltas, so a revision cannot pass by being self-consistently wrong.
2. Actual FULL chunks across representative biomes/seeds/underground levels, with block and biome census, ore/decoration placement evidence, structure-start checks, natural/chunk spawn rules and bedrock/build bounds. — **Partially done, and deliberately not claimed complete.** Block and biome censuses, sea level and the bedrock/build envelope are measured across establish and a fresh-JVM reload. Structures are now measured too, but at *eligibility* level plus a reachability radius, because the start-level sample measured zero everywhere. **One seed, nine chunks and one biome (`moderner_beta:beta_forest`)** is not representative coverage, and natural/chunk spawn rules, the Nether and the End are unmeasured.
3. Positive evidence for required historical features/distributions as well as negative evidence for modern content; sampling alone cannot prove all placements absent. — **Substantially improved, still not met.** Beta's ore placement is now implemented and measured inside the ceilings the verified algorithm allows, and the restricted families, structures and spawned entity types are constrained. But the sample is still one seed, nine chunks and one biome, ore weights within a band are not compared against a historical distribution, decoration and spawn **weights** are untouched, and sampling cannot prove a feature is globally absent.
4. Save/reload in a fresh JVM with version and generation-recipe revision retained; marker/player-built content persists. — **Done for revisions 1–3.** Each world retained its own recorded revision, the marker block survived a process restart, and the census was identical across processes.
5. Ordinary-world and legacy-world non-interference; unknown/corrupt metadata and unsupported generation bindings fail closed. — **Done and regression-tested.** Ordinary worlds are never adopted; the direct upstream Beta world stayed ordinary; unknown profiles, unbound recipes and cross pairs fail closed; three real-server lifecycle controls still pass.
6. Restart-safe future-chunk upgrade with backup/journal/crash recovery; existing chunks/builds/inventory/entities are not regenerated or deleted. — **Not implemented.** Revisions exist and are addressed by immutable alias, which is the necessary foundation, but there is no upgrade transaction, backup, journal or crash recovery yet.

## Research quarantine

1. Complete upstream foundation comparison and exact audit of every necessary compatibility delta; no unrelated terrain keys changed.
2. Actual FULL chunks across representative biomes/seeds/underground levels, with block and biome census, ore/decoration placement evidence, structure-start checks, natural/chunk spawn rules and bedrock/build bounds.
3. Positive evidence for required historical features/distributions as well as negative evidence for modern content; sampling alone cannot prove all placements absent.
4. Save/reload in a fresh JVM with version and generation-recipe revision retained; marker/player-built content persists.
5. Ordinary-world and legacy-world non-interference; unknown/corrupt metadata and unsupported generation bindings fail closed.
6. Restart-safe future-chunk upgrade with backup/journal/crash recovery; existing chunks/builds/inventory/entities are not regenerated or deleted.

## Research quarantine

The new [Beta generation candidate](<../research/beta173-generation-evidence.json>) is **REJECTED_DO_NOT_CONSUME** by parent review. Correct artifact hashes did not establish correct block/site/control-flow interpretation; the supplied ore/dungeon table is not accepted. The older [availability candidate](<../research/beta173-availability.json>) remains rejected too. Neither may drive a recipe or whitelist. Primary facts must be independently re-derived from the verified artifact with exact field identity and per-loop data/control flow. Only the [parent-reviewed parameter slice](<../research/beta173-ore-parameters-parent-reviewed.json>) is accepted, and only within the narrow scope it declares.

## Structures are a world option, and Beta 1.7.3 had none

Strongholds, mineshafts and villages all arrive in **Beta 1.8.1**, and dungeons are a placed feature rather than a structure, so a Beta 1.7.3 world must generate **no structures at all**. This is not a settings component: structure generation is gated by `WorldOptions.generateStructures()` (`generate-structures` in `server.properties`, persisted in `level.dat`), and `ChunkStatusTasks` skips start creation, `ChunkGenerator.applyBiomeDecoration` skips placement, and `/locate` returns nothing when it is false.

Measured from the live generator state, a historical world carried **17 structure sets**, with **16 reachable within 64 chunks** and **29 individual structures** configured — villages, mineshafts, strongholds, ocean monuments, woodland mansions, pillager outposts, ruined portals, trial chambers, trail ruins, buried treasure, desert/jungle temples, igloos, shipwrecks and swamp huts. Every one of those postdates Beta 1.7.3. Moderner Beta's own `moderner_beta:ocean_shrine` is the only non-vanilla entry, and its historical status is unassessed. Moderner Beta *does* curate some structure content — an ordinary Moderner Beta world additionally carries `minecraft:ancient_cities`, which the cave-biome removal takes out of the historical world — but it leaves every Release-era structure in place.

The remedy is the supported world option, not a hook:

| Configuration | World option | Manager | Starts | Reachable structures | Verdict |
| --- | --- | --- | --- | --- | --- |
| Correctly created historical world | **false** | false | 0 | 29 still configured | **PASS** |
| Historical world created before the fix | true | true | 0 | 29 still configured | **FAIL** with an actionable message |
| Ordinary Moderner Beta control | true | true | 0 | 30, plus ancient cities | unaffected |

Because the reachable-structure count is identical in the first two rows, the option is demonstrably what prevents them rather than their absence. Evidence: [corrected world](<../logs/continuation-88-rev4b-reload.log>), [pre-fix world](<../logs/continuation-90-prefix-reload.log>), [ordinary control](<../logs/continuation-82-upstream-struct-reload.log>).

**Existing worlds are warned, not refused, and that is a deliberate decision.** The option lives in `level.dat` and cannot be changed for a world that already exists; refusing would lock operators out of worlds they already generated, including the preserved v1 baseline, and would not repair anything. Such a world logs a loud warning naming the option and the consequence, keeps every existing chunk, and keeps loading ([legacy v1](<../logs/continuation-92-legacy-structure-reload.log>)). Fail-closed stays reserved for a world whose recorded identity disagrees with what was loaded. The create-world screen carries the requirement too: the world-preset description, the world-type label and the dropdown info text all say Generate Structures must be OFF, pinned by a test.

**Limits.** `startsInProbeChunks` measured **0 in every world so far**, including the ordinary control whose census *does* contain ancient-city and mineshaft blocks, because a structure's start piece sits at its own origin and usually falls outside a 5x5 sample. Start-level absence is therefore reported but never relied on, and structure blocks are not used as a criterion. The Nether and the End are untested.

### Implemented: Beta ore placement (revision 5)

| Revision | Delta | Measured in actual chunks | Verdict |
| --- | --- | --- | --- |
| 5 | + Beta 1.7.3's own ore population, with every vanilla ore feature and Moderner Beta's emerald excluded | diamond **1..15**, redstone **1..14**, lapis **4..18**, gold **5..30**, coal and iron **1..57**; 2788 veins attempted and 50709 blocks placed in 34 decorated chunks; every maximum inside the derived ceiling | ore distributions now historical |

Evidence: [establish](<../logs/continuation-124-ore5-establish.log>), [reload](<../logs/continuation-126-ore5-reload.log>), [revision 4 still vanilla](<../logs/continuation-128-ore4-establish.log>), [ordinary control](<../logs/continuation-130-ordinary.log>).

The implementation is three pieces: `BetaOreVein` (the transcribed shape), `BetaOrePlacement` (the call-site parameters and chunk population), and `ChunkGeneratorHistoricalOreMixin` (runs it after vanilla decoration for the chunk being decorated). Both halves of the exclusion are required — placing Beta's ores without removing vanilla's would roughly double every count, and removing vanilla's without placing Beta's would leave a world with no ores.

**Individual vein positions are not Beta's.** The chunk-local random is seeded the way vanilla seeds decoration rather than by Beta's own population sequence, so a historical world does not match a real Beta 1.7.3 world vein for vein. The attempted counts, sizes, height ranges and vein shape — which is what a distribution is made of — are the verified ones. That limitation is recorded with the algorithm, not glossed.

## Ore distributions: the algorithm is verified, the gap is measured

Moderner Beta's Beta biomes get their ores from vanilla `BiomeDefaultFeatures.addDefaultOres`, whose height bands are modern. Beta 1.7.3's own placement is different in both the number of attempts and where veins sit, so the ores in a historical world today are the right ones in the wrong places.

Both halves of the fix now rest on primary evidence:

| Half | Evidence |
| --- | --- |
| Call-site parameters (attempts, size, origin Y) for dirt, gravel, coal, iron, gold, redstone, diamond and lapis | [parent-reviewed parameter slice](<../research/beta173-ore-parameters-parent-reviewed.json>) |
| The vein algorithm itself | [vein geometry artifact](<../research/beta173-ore-vein-geometry.json>) |

The geometry was read instruction by instruction: `sin`/`cos` axis endpoints, the `y + nextInt(3) - 2` centre offset, **`i <= size` (size+1 iterations — an inclusive bound, not the more usual exclusive one)**, the `(sin(pi*i/size) + 1) * nextDouble()*size/16 + 1` radius, the integer bound box, the strict `< 1` ellipsoid test at all three levels, and the replacement predicate. Only `uu.u` is replaced, and `uu.u` is **stone** — the string `"stone"` loads immediately before the field assignment — so Beta veins replace stone alone, with no air-exposure discard and no deepslate, andesite, diorite, granite or tuff target.

**Measured contradiction.** The probe now reports per-ore Y ranges and 16-tall band counts, so a modern second placement band cannot hide inside a total count. In a fresh historical world: redstone **1..31** against Beta's reachable maximum of about 19, diamond **3..53** against about 19, lapis **2..55** against about 34 ([log](<../logs/continuation-111-ore-histogram.log>)). Coal 14..58, iron 1..56 and gold 5..18 sit inside their expected ranges, and there are still no deepslate ore variants, no copper and no tuff.

The expected ranges are recorded with the algorithm as origin range widened by the ±2 centre offset and the vein radius, so the implementing slice has a stated pass condition. Reproducing the vein shape necessarily means writing a feature that Beta's algorithm defines — that is **content** restoration, which the architecture permits, and it is not a terrain, noise or climate reconstruction. A renamed modern ore placement remains unacceptable, as does a block whitelist.

**Limits.** The measurement is one 5x5-chunk sample on one seed, taken at generation revision 4; it is evidence of a real placement difference, not a full distribution comparison. The resulting distribution after terrain overlap, cave carving and structure overlap is not derived, and neither is what a vein does at the world's build bounds.

## Next implementation slice

1. ~~A per-world, exception-safe generation-time hook~~ — **implemented and measured**; see the revision-4 section above.
2. ~~Structures~~ — **measured, gated and corrected** by the supported world option; see the structures section above. Their Nether and End behaviour is still untested.
3. ~~Beta-accurate ore features~~ — **implemented and measured**; see the revision-5 section above. What remains is decoration fidelity and the non-ore population steps, not ore placement.
4. Decoration fidelity (later-version vegetation such as `leaf_litter`) and historical spawn **weights/pack sizes**, since only the entity set is currently correct.
5. Building-height rules (Beta 1.7.3's 128 limit versus the 320 the reduced envelope keeps) and explicit Nether/End rules, including their structure and spawn content.
6. Multi-seed and multi-biome census coverage so acceptance 2 and 3 are actually met.
7. The open question about `moderner_beta:ore_emerald_y95`: decide with evidence whether Moderner Beta's `beta` preset models 1.7.3 or a later Beta-era game, rather than deciding it silently.

Next acceptance is the bounded historical-content world, then decisive gameplay implementation. Neither the upstream-world success nor a green content-preview verdict may be reported as that acceptance.
