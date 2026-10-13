# Mechanical historical bugs and obscure behavior

**Scope/status.** Alpha 1.0.1 through Release 1.6 (the repository's 29-build matrix). This is a conservative, build-gated catalogue, not a claim that all historical defects have been exhaustively audited. The local seed document itself labels its research in progress. Records below include only sourced behaviors with a mechanical effect or a directly relevant boundary; transient client/UI issues, uncertain artifacts, and other seeded items are called out under the matching version or unresolved list rather than promoted to proven mechanics. Evidence quotes are verbatim; repository evidence paths are identified as such. `unknown` boundaries must not be treated as a refusal rule. The declared ids follow `research/version-transitions.json` and the repository's version registry.

## Alpha

### Alpha 1.0.1 (`alpha_1_0_1`)
The version history says monster spawners stopped being collectible here, not that they were mineable in the Infdev profiles this project declares. See **Corrections to owner premises**. A coal-ore blob-size change is an explicitly unresolved research lead, not a confirmed gameplay bug. No mechanical bug in this build was verified beyond the spawner availability transition.

### Alpha 1.0.4 (`alpha_1_0_4`)
Winter mode is an alternate world-creation mode, not a biome probability. Its unusual freezing/snow coverage and gravel-beach exception are reported version behavior, but the local corpus says direct artifact testing of edge cases remains outstanding. No source-backed mechanical defect was established here.

### Alpha 1.0.6 (`alpha_1_0_6`)
**Snowball surplus loop.** Four snowballs craft a snow block, while breaking it yields six, so players can repeatedly gain two snowballs. This is an exploitable mechanic, not a claim of a crash or a proven later fix. Exact persistence/fix boundary not established; record boundary is `unknown` and the implementer must not infer that adjacent builds share it. [Source: Alpha v1.0.6 raw, Changes > Snow block](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.6?action=raw).

### Alpha 1.0.11 (`alpha_1_0_11`)
The seeded bookshelf no-drop behavior and fist-dependent slime splitting are reported by the local version-history seed but were not independently rechecked against a direct source in this pass. Treat their exact drop/split semantics as unresolved, not implemented requirements yet.

### Alpha 1.0.14 (`alpha_1_0_14`)
**Natural slime spawning disabled.** The version page states the revised limit miscalculation left slimes spawning only in strange locations and natural spawning was disabled. Slime spawning is restored by Alpha 1.0.17 in the repository's transition notes. Exact affected build is therefore the target Alpha 1.0.14 profile, not all Alpha versions. Existing `HistoricalSpawnRules` is reported in implementation coverage as already expressing this absence.

The original jukebox recipe returned a bookshelf, but the same-day reupload fixed it. This is an **artifact-level** boundary: the repository's `alpha_1_0_14` id does not distinguish original from reupload, so do not apply it to every build carrying that id. [Source: Alpha v1.0.14 raw, introductory artifact note](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.14?action=raw).

### Alpha 1.0.17 (`alpha_1_0_17`)
The local corpus reports broken/invisible inventory icons in the short-lived client; lost client and contemporaneous report prevent testing. It is not catalogued as stable gameplay content. Slime spawning returns after the Alpha 1.0.14 miscalculation.

### Alpha 1.1.0 (`alpha_1_1_0`)
The camera-through-walls exploit is reported fixed in the second reupload, not necessarily the initial client. Artifact split is unresolved at the repo id granularity; no safe all-build gate.

### Alpha 1.2.0 (`alpha_1_2_0`)
The launcher label `1.2.0` can resolve to `_02`, so it is unsafe as evidence for the exact original build. The local history mentions fake/non-solid chunks near the 32-million coordinate limit, but source and exact boundary testing were not completed here.

## Beta

### Beta 1.0 (`beta_1_0`)
The source seed flags crafting-grid placement dropping items and tools no longer self-repairing, plus a biome-border/lava-lake quirk. These candidate behaviors were not independently verified from a directly fetched build page in this pass; their exact fixing builds remain open.

### Beta 1.2 (`beta_1_2`)
**Mounted portal data-loss crash.** Entering a Nether portal while riding a boat, minecart, or pig could crash and delete player data. The Beta 1.6 changelog says a crash when riding a vehicle or animal into the Nether was fixed; the exact intervening patch is not narrowed, so this record is `unknown` boundary for refusal purposes. A modern mod must never delete or corrupt player saves to emulate it.

**Other seeded behavior not independently established:** squids could be milked; powered redstone sometimes failed to update when a new source was added. Treat these as research candidates pending exact fixes and artifact tests.

### Beta 1.3 (`beta_1_3`)
**Debug starting/respawn kit (original artifact only).** The 17:13 UTC client gave 64 torches, a diamond pickaxe, shovel, axe, and 64 beds on world creation/respawn. The 17:33 reupload removed the code; a later upload fixed a separate world-menu crash. The repo's single `beta_1_3` id cannot represent this split, so this remains `unknown`/not a global profile behavior.

### Beta 1.4 (`beta_1_4`)
**Zombie/spider hit-height bug fixed.** Before this version, the version page reports zombies and spiders only hurt the player if they were one block above the player's location; Beta 1.4 fixed them to hurt in all instances. The exact introducing build is not supplied, so this is not a refusal range. Bed respawn position also had a multiplayer-only defect (only changed after clicking “Leave bed”), reported as not working for most users; exact patch endpoint was not confirmed here.

### Beta 1.5 (`beta_1_5`)
The seed has weather-specific regressions and patch fixes (notably rain through glass/stairs/slabs in early 1.5), but the exact applicable declared build is not independently confirmed in this research pass. Do not broaden this to all Beta 1.5 profiles without a patch-by-patch source check.

### Beta 1.6 (`beta_1_6`)
The changelog reports fixing the mounted-vehicle/animal Nether crash above and an ore-density quadrant bias: before the fix, ores were at least 50% more common in southeast than northwest. The latter is a world-generation distribution defect, not a survival interaction, and precise earliest/fix subpatch remains under-researched. A separate shift-click/full-inventory crash is listed as surviving Beta 1.6; no later boundary established here.

### Beta 1.7 (`beta_1_7`)
**Piston invalid block/crash.** Pushing blocks destroyed by piston movement could create block 36 (“air”) and crash rendering; related redstone/piston cases could crash or soft-lock the world. The version page says upgrading to Beta 1.7_01 or later permits affected worlds to load again. Treat it as Beta 1.7-specific, not intentional piston semantics. Right-clicking sheep also reportedly crashed; that separate bug's exact fix was not established.

### Beta 1.8 (`beta_1_8`)
The source seed identifies prerelease-only structure-rate changes and an inventory-block lighting defect. These do not automatically apply to the stable Beta 1.8 profile. Beta 1.8.1 is a distinct target. Exact affected artifacts and correction rows were not fetched during this pass.

### Beta 1.9 Prerelease 1 (`beta_1_9_pre_1`)
**Jump XP bug (exact single-build gate).** Each jump grants a random 1–100 XP; held-jump auto-repeat is disabled. Pre2 removes the debug XP code and restores continuous jumping. This must be gated to Pre1 only, not “all builds before Pre2”: earlier profiles do not necessarily have XP at all.

**Snowfall destroys blocks (Pre1–Pre4 range).** The repository's transition record reports snowfall temporarily destroys redstone, torches, saplings, and rails; Pre5 fixes this regression. The record lists the four declared prerelease ids explicitly. Do not extend it to Release 1.0.

### Beta 1.9 Prerelease 2 (`beta_1_9_pre_2`)
The jump XP and held-jump defects are fixed here. Cauldron brewing code existed without an assigned ordinary block ID and should not be represented as a normally obtainable cauldron mechanic. This is an availability quirk rather than a confirmed survival gameplay bug.

### Beta 1.9 Prerelease 3 (`beta_1_9_pre_3`)
**End portal frame explosion drop (critical one-build gate).** Frames can drop as items due to explosions in Pre3; Pre4 prevents explosion destruction. Do not apply to Pre2 or Pre4+. **Frame luminance also differs only here:** light level 15, reduced to 1 in Pre4. The original Pre3 client briefly gave players full stacks of debug blocks including portal frames; the same-day reupload removed them. Both facts are in the cited version history; the item/explosion/light claims are also recorded in the trusted preceding research lead. This pass did not refetch the End Portal Frame page, per instruction.

**Placeholder enchanting.** In Pre3, using the enchantment table could put an enchantment glint/label on eligible items, but the tooltip was the literal `Enchanted!!` and the prerelease had no real enchantments. Pre4 replaced that placeholder with real enchantment names/effects, among them Feather Falling. Feather Falling was one actual enchantment option, not evidence that every item received it. This is an exact Pre3-to-Pre4 boundary.

### Beta 1.9 Prerelease 4 (`beta_1_9_pre_4`)
**Silk Touch spawner drop.** The monster-spawner History table says Silk Touch makes spawners drop in Pre4; Pre5 removes this. This is one build only in the matrix. It is the substantiated “spawner as a mineable item” case relevant to this target range.

### Beta 1.9 Prerelease 5 (`beta_1_9_pre_5`)
Pre5 fixes the Pre1 snowfall regression and removes Silk Touch spawner drops. It also increases slime spawn height and changes downward water flow to create source blocks; the latter is reverted in Pre6. These are intentional version deltas, not bugs to leak into neighboring profiles.

### Beta 1.9 Prerelease 6 (`beta_1_9_pre_6`)
Downward-flowing water returns to flowing water rather than generating source blocks. Wooden doors reportedly open only with redstone in this build; exact fix build is not established. Dragon egg teleport-on-interaction is documented as intentional behavior, not conventional mining.

## Release

### Release 1.0 (`release_1_0_0`)
No separately verified mechanical bug record added. This pass did not establish the exact-build parsing issue noted in the seed corpus; the unavailable TCRF page is not treated as evidence.

### Release 1.1 (`release_1_1`)
The seed says the “11” disc remains unobtainable by the skeleton-kills-creeper route. Its exact boundary and source row were not independently checked here.

### Release 1.2 (`release_1_2_5`)
The corpus warns against conflating Beta 1.2 lapis changes with Release 1.2 terrain/height/jungle boundaries. No separately sourced gameplay bug confirmed in this pass.

### Release 1.3 (`release_1_3`)
Mobs reportedly spawn on upside-down slabs/stairs; exact introducing and fixing builds not established here. The 1.3.2 follow-up is distinct from the target's base-era evidence.

### Release 1.4 (`release_1_4`)
The seed notes later 1.4.x empty-jukebox drops/invisible Air and interaction crashes/chunk deletion. Exact affected patch was not determined, so do not attribute it to Release 1.4 generally.

### Release 1.5 (`release_1_5`)
Patch-specific performance issues noted in the corpus were not expanded to mechanical bugs here; do not backdate 1.5.1/1.5.2 to the target Release 1.5 profile.

### Release 1.6 (`release_1_6_4`)
The local seed says there is no fully sourced notable exact-patch bug in this target; it remains an explicit research gap rather than an assertion that no bugs existed.

## Corrections to owner premises

1. **Monster spawners were not mineable in the project's declared Infdev profiles.** The History table says the block was added in Infdev 20100625-1917 (“Monster spawners can be picked up and when placed, spawn pigs.”), and that in Alpha v1.0.1 they “can't be picked up anymore.” Both declared Infdev profiles (`infdev_420` = 20100420 and `infdev_611` = 20100611) predate the block's existence. Therefore “spawners mineable before the first Alpha version” is false for this project's declared matrix. The useful later exception is Beta 1.9 Pre4 Silk Touch, removed in Pre5.
2. **End portal-frame explosions are a high-value exact-build behavior:** Pre3 only, fixed in Pre4; do not implement as an open-ended old-version rule.
3. **Jump XP is a Pre1-only behavior:** Pre2 removes it. It is not applicable across earlier Alpha/Beta versions.

## Unresolved / could not establish

- The catalog is deliberately conservative, not an exhaustive sourced treatment of every one of the 29 seed sections. In particular, Alpha bookshelves/slime fist splitting, Beta 1.0 crafting-grid/tool-repair behavior, Beta 1.2 squid-milking/redstone updates, Beta 1.5 weather defects, Beta 1.8 prerelease lighting/structure defects, Pre6 redstone-only doors' fix, Release 1.1 disc, Release 1.3 slab spawning, Release 1.4 empty jukebox, and Release 1.6 exact-patch bugs need feature history or original artifact confirmation.
- Beta 1.3's original-vs-reupload debug kit and Alpha 1.0.14's jukebox recipe are artifact split claims; repo build ids do not encode every reupload.
- Alpha 1.0.6 snowball recipe/yield is confirmed, but its later fix/persistence boundary was not established. Its record is `unknown`, not a license to suppress neighboring versions.
- The version corpus and feature timeline mark themselves pending verification/in progress; they are leads, not primary evidence. Direct jar/artifact execution was not done for these claims.
- **TCRF was not fetched or read.** Repository research notes HTTP 403 for TCRF; no TCRF claim or quotation is used here. I did not try to bypass that failure.
- No Nostalgic Tweaks equivalent was established for these mechanical bugs. The NT catalogs themselves say historical boundaries are unknown; names alone are not evidence.

## Source access log

All URLs fetched directly in this session returned HTTP 200:

| URL | Result |
|---|---:|
| https://minecraft.wiki/w/Experience?action=raw | 200 |
| https://minecraft.wiki/w/Monster_Spawner?action=raw | 200 |
| https://minecraft.wiki/w/Monster_Spawner?action=raw&section=History | 200 |
| https://minecraft.wiki/w/Snowball?action=raw | 200 |
| https://minecraft.wiki/w/Damage?action=raw | 200 |
| https://minecraft.wiki/w/Weather?action=raw | 200 |
| https://minecraft.wiki/w/TNT?action=raw | 200 |
| https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease?action=raw | 200 |
| https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_2?action=raw | 200 |
| https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_3?action=raw | 200 |
| https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_4?action=raw | 200 |
| https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_5?action=raw | 200 |
| https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_6?action=raw | 200 |
| https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.6?action=raw | 200 |
| https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.14?action=raw | 200 |
| https://minecraft.wiki/w/Java_Edition_Beta_1.2?action=raw | 200 |
| https://minecraft.wiki/w/Java_Edition_Beta_1.3?action=raw | 200 |
| https://minecraft.wiki/w/Java_Edition_Beta_1.4?action=raw | 200 |
| https://minecraft.wiki/w/Java_Edition_Beta_1.6?action=raw | 200 |
| https://minecraft.wiki/w/Java_Edition_Beta_1.7?action=raw | 200 |

`https://minecraft.wiki/w/End_Portal_Frame` was not re-fetched in this session, per the user's instruction; the prior confirmed quote is retained as a supplied strong lead. Repository files consulted locally (not HTTP fetches): `research/minecraft-version-history.md`, `research/version-transitions.json`, `research/feature-timeline.json`, `research/version-boundaries.json`, `research/historical-features.json`, `research/nostalgic-tweaks-candidates.json`, `research/nt-port-worklist.json`, `research/implementation-coverage.md`, and `src/main/java/net/historicalexperience/version/HistoricalVersions.java`.
