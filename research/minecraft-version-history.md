# Minecraft Java Edition historical version reference: Alpha through Release 1.6

> **Research status: in progress; not yet exhaustive.** This document is being assembled from source-backed version histories and technical cross-checks. A target-version heading is not evidence of completeness. The companion [research progress checklist](minecraft-version-history-progress.md) records unfinished verification. Do not use an `Unknown` or `not researched` field as evidence that a feature was absent.

## Part I — Research overview

### Scope and purpose

This reference covers the exact requested Java builds: Alpha v1.0.1, v1.0.4, v1.0.6, v1.0.11, v1.0.14, v1.0.17, v1.1.0, v1.2.0; Beta 1.0, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8; Beta 1.9 Prereleases 1–6 as six separate builds; and Release 1.0 through 1.6 inclusive. The goal is an implementation reference for what the builds actually registered, exposed, generated and did—not an aggregation of later-game behavior or a feature-added list.

Version labels are deliberately explicit. **Beta 1.0 is not Release 1.0.** Release 1.0.0 shipped on 18 November 2011; Beta 1.9 prereleases were development builds for that release, despite their Beta labels. The requested Beta list omits some intervening patches and does not include Beta 1.7.3, Beta 1.8.1, most 1.0.x and later patch builds. Those builds are used only as boundary evidence and are never silently substituted for the named target build. The naming and era chronology follow the [Java Edition version history](https://minecraft.wiki/w/Java_Edition_version_history), which itself notes that it is not an official Mojang changelog.

### Method and evidence grades

Research is version-first and feature-history-second: each entry must be checked against the exact build's history page, feature-specific histories, generation histories, and where feasible primary artifacts or contemporaneous evidence. Existing repository evidence files `research/feature-timeline.json` and `research/version-boundaries.json` contain useful extracts and jar-derived boundaries but are marked `WORKER_GENERATED_PENDING_PARENT_VERIFICATION`; they are leads, not proof on their own.

- **Verified**: supported by a directly relevant source for the named build or inspectable primary artifact; exact inference scope is stated.
- **Corroborated**: multiple credible secondary or contemporary sources agree, but a direct build/source check is unavailable.
- **Inferred**: a conclusion from evidence with a stated reasoning step; not reported as a directly tested fact.
- **Unverified / Unknown**: evidence is insufficient or sources conflict. Keep uncertainty visible.

The [world-generation history](https://minecraft.wiki/w/World_generation/History) is unusually useful for dated generator milestones but currently labels itself a work in progress; it is therefore not treated as definitive algorithm documentation. A repository-local [Beta 1.7.3 generation extraction](research/beta173-generation-evidence.json) is explicitly rejected and is not used to infer ore mappings or generator behavior. Wiki version histories are secondary summaries. Original client/server metadata and jars can substantiate identities, registries, constants and protocol values; their mere existence does not prove ordinary survival obtainability or behavior. No original game has been executed for this document unless an entry explicitly says so. No proprietary game code or assets are reproduced.

### Operational definitions

- **Introduced / present**: a build contains code, assets, registry entry or UI for a feature. This does not imply a usable feature.
- **Internal**: feature data/code exists but ordinary players cannot access it, or it has no user-facing function.
- **Obtainable**: an ordinary survival player can acquire it in that exact build by a documented route. Creative/debug/command/editor acquisition is stated separately.
- **Partial**: visible or partly operating, but core intended effects or progression are missing or broken.
- **Functional**: the intended gameplay operation is available in ordinary play, subject to documented limitations.
- **Changed**: operational but differs materially from its later familiar behavior.
- **Removed**: formerly present and no longer available in the exact target build.
- **No identified change**: research found no change in the category; this is not equivalent to absent or unresearched.
- **Not researched / Unknown**: current evidence cannot establish the state.

### Limitations

Old builds have sparse official documentation, occasional reuploads, and historically inconsistent naming. Wiki pages can mix final release changes with snapshots, previews or later patches; claims are only attributed to exact targets when the source supports that boundary. Some historical class names are obfuscated and cannot safely be assigned without reproducible artifact/hash evidence. Generation descriptions in the available history are often qualitative; parameters and RNG call ordering remain open unless established by code or tests. The individual write-up and audit are still underway; use the progress checklist for known gaps.

## Part II — Master historical feature timeline

This is a milestone index, not a claim that every release between milestones was individually checked. Dates below are the release dates shown on the corresponding version pages; source links target the relevant version or technical history page. Detailed boundaries, especially prereleases and patches, remain under review.

| Feature / mechanic | First appearance | First functional implementation | Later significant changes | Notes |
|---|---|---|---|---|
| Alpha-named public builds | Alpha v1.0.1, “Seecret Friday 3” (2 July 2010) | N/A | Alpha 1.2.0 (Halloween Update) changes generation and adds Nether | Java Edition history labels Alpha development period from 30 June; first build page says v1.0.1 released July 2. The exact client is now lost. |
| Redstone ore generation | Alpha v1.0.1 | Obtainable ore, but redstone circuits predate this target | Later redstone systems add repeaters, pistons, comparators, etc. | Worldgen history reports redstone ore blobs at this boundary; verify detailed behavior by block history. |
| Winter-mode worlds | Alpha v1.0.4 | Functional as alternate world creation mode | Superseded by biome-dependent weather/snow generation | Not equivalent to later snowy biome worldgen. |
| Nether | Alpha v1.2.0 | Accessible via constructed obsidian portal in this era | Beta 1.8 changes generation; Release 1.0 adds fortress progression materials | Alpha v1.2.0 worldgen history marks Nether addition. |
| Biomes and biome-colored terrain | Alpha v1.2.0 | Functional generation/climate-era biome system | Beta 1.8 replaces biome algorithm and couples terrain elevation to biome | In 1.2.0 sea-level/height noise changed; monoliths cease. |
| Birch/spruce trees and lapis ore | Beta 1.2 | Functional worldgen/crafting after exact patch boundary review | Beta 1.2_02 adjusts lapis frequency/height | Do not attribute patch fixes to Beta 1.2 initial without checking. |
| Beds | Beta 1.3 | Functional sleep/time skip; verify exact spawn behavior separately | Later spawn and multiplayer rules evolve | Version-specific recipe and night/hostile-mob constraints need exact entry. |
| Weather | Beta 1.5 | Rain/snow/thunderstorm weather system | Later visual/audio rules change | Distinguish the weather feature milestone from later biome-dependent snow behavior and patch-specific rendering. |
| Maps | Beta 1.6 | Functional craftable map with historical scale/updates | Later map scaling/markers change | Beta 1.6 Test Build 3 also has generation changes/bug. |
| Pistons | Beta 1.7 | Functional with movement limitations/bugs unlike modern pistons | Numerous fixes and later sticky behavior differences | The target includes distinct 1.7 patch changes; per-build claims pending. |
| Adventure Update generation overhaul | Beta 1.8 (14 Sep 2011) | Functional in release build | Beta 1.9 prereleases add/change generation and stronghold portal; Release 1.0 finalizes | Continents, biome-based elevation, fractal biome placement, sea level 63, rivers/oceans/extreme hills; ravines, villages, mineshafts, strongholds introduced. [Generation history](https://minecraft.wiki/w/World_generation/History). |
| Hunger | Beta 1.8 development / PAX Prime build | Beta 1.8 public release (food bar; eating no longer heals directly; regen threshold) | prerelease/Release tuning | The feature history reports introduction in 1.8 Pre-release; do not describe it as Beta 1.9. |
| Sprinting | Beta 1.8 PAX Prime preview | Beta 1.8 public release | Later 1.0 control note; 30-second auto-stop removal occurs in 1.9 snapshots, outside this scope | Sprint history records a five-second cap/special FOV only in PAX and says these are absent later; exact public-build timeout boundary remains unresolved. [Sprinting history](https://minecraft.wiki/w/Sprinting). |
| XP orbs and levels | Beta 1.8 | Partly functional for collection and level display; enchanting consumer does not yet exist | Enchanting in Beta 1.9 development consumes levels; Release 1.0 progression is usable | XP existence is not equivalent to XP spending or modern level curves. |
| End, Ender Dragon, stronghold route | First components in Beta 1.9 prereleases; prerelease-specific accessibility changes | Release 1.0.0 | Later dragon respawn/End changes far outside scope | Beta 1.8 strongholds existed without completed portal/End access. Exact pre1–pre6 boundaries still being audited. |
| Enchanting | UI/system appears by Beta 1.9 Prerelease 3 | Exact functional-enchantments boundary among Prereleases 3–6 must be resolved | Release 1.0 stable enchanting with bookshelves, levels and enchantments | Pre3 has table/UI/glint but its page says no enchantments yet; do not backdate complete enchanting to Beta 1.8. |
| Brewing | Beta 1.9 Prerelease 2 cauldron prototype (no ordinary block ID) | Beta 1.9 Prerelease 3 stand-based brewing (timed, 30 s) | Pre3 narrows recipe space, cauldron becomes water container; Release 1.0 stable recipes | Pre2 cauldron code is internal/unobtainable as an ordinary block; do not claim functional brewing until Pre3. [Pre2](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_2), [Pre3](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_3). |
| Enchantment effects | Beta 1.9 Prerelease 3 GUI/table (no enchantments yet) | Beta 1.9 Prerelease 4 (17 implemented) | Release 1.1 adds bow enchantments | Pre3 adds the UI but no actual enchantments or functional XP spending; Pre4 table requires bookshelves and supports max level 50. [Pre3](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_3), [Pre4](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_4). |
| End dimension | Beta 1.9 Prerelease 4 | Pre4 portal gives access, but no dragon spawn | Pre6 dragon/exit portal/egg/credits; 1.0 stable | Strongholds were Beta 1.8; Pre3 added portal room; Pre4 activates End but boss not yet available. [Pre3](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_3), [Pre4](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_4), [Pre6](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_6). |
| Ender Dragon | Pre4 dragon code/entity exists but inaccessible (unverified exact internals) | Beta 1.9 Prerelease 6 | Release 1.0 stable | Pre5 has kill achievement/test death animation yet dragon still does not spawn; do not treat achievement as boss access. [Pre5](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_5), [Pre6](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_6). |
| Beta 1.9 prerelease publication | Pre1, 22 Sep 2011 (official page title lacks “1”) | N/A | Pre2 Sep 29; Pre3 Oct 6; Pre4 Oct 13; Pre5 Oct 27; Pre6 Nov 11 | Contemporaneous Jeb posts corroborate publication: [Pre1](https://twitter.com/jeb_/status/116880362793222144), [Pre2](https://twitter.com/jeb_/status/119395358135693312), [Pre4](https://twitter.com/jeb_/status/124491774398963712), [Pre5](https://twitter.com/jeb_/status/129568296600219648), [Pre6](https://twitter.com/jeb_/status/135018767620112385). Pre3 has same-day client reupload. |
| Release 1.0.0 | 18 Nov 2011 | Official full-release milestone; stabilized End/brewing/enchanting/Hardcore and related systems | Each 1.0.x artifact must be kept distinct from 1.0.0 | Several systems were already functional in Beta 1.9 prereleases; the Release is a distinct official artifact, not their first implementation. |
| Emeralds and villager trading | Release 1.3 development (12w21a) | Release 1.3.1 stable | Release 1.4.2 adds further trade-related items | Do not backdate the trade economy to Release 1.2; Beta 1.2 cocoa-bean item was editor-only and survival jungle cocoa arrives later. [1.2 guide](https://minecraft.wiki/w/Java_Edition_guides/1.2_releases), [1.3 guide](https://minecraft.wiki/w/Java_Edition_guides/1.3_releases), [emerald history](https://minecraft.wiki/w/Emerald). |
| Hoppers / comparators / redstone update | Release 1.5 development | Release 1.5 | Further redstone mechanics later | Must describe tick/order/interaction details, not just item addition. |
| Horses | Release 1.6 development | Release 1.6.1 | Taming/breeding/armor behavior changed after | Release 1.6 introduces a new launcher/resource pack format and horse entities; patch detail pending. |

### Date and terminology cautions

- The Alpha phase starts 30 June 2010 but first Alpha-labelled build is v1.0.1; the numeral does not imply a previous public Alpha v1.0.
- The public Beta phase begins 20 December 2010. Requested Beta 1.0 means the first Beta 1.0 release, not Release 1.0.0.
- Beta 1.9 Prerelease 3's page identifies an original and reuploaded client on the same day: the original debug-hotbar client and the reupload with that hotbar removed are not identical. Research must identify which is intended whenever feature attribution depends on this split.
- Release 1.0.0 shipped 18 November 2011. It was initially named Beta 1.9 during development, but the official full release is a separate artifact and must have its own section.

## Part III — Detailed version-by-version research

Each section follows the requested 16-field template. Where a specific dimension of behavior has not been verified, the entry will say so instead of filling gaps with modern assumptions. The sections below are populated incrementally; current per-version completeness is tracked in the [progress checklist](minecraft-version-history-progress.md).

### Minecraft Alpha

#### Alpha v1.0.1
1. **Identity/context:** “Seecret Friday 3,” released 2 July 2010. This is commonly treated as the first public Alpha-labelled release; the Wiki's broader period chronology begins 30 June. These are not contradictory dates for the first numbered build vs Alpha development period. The client is lost and no matching server is listed, so behavior claims cannot be tested against the original artifact. [Version page](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.1); [archived Notch announcement](https://web.archive.org/web/0/https://notch.tumblr.com/post/776113692).
2. **New blocks/items:** Introduced early redstone components: redstone ore/dust, redstone torches, levers, buttons, pressure plates, iron doors. Existing signs can now be wall-mounted.
3. **Changed/removed blocks/items:** Monster spawners stopped dropping as items. Mushrooms could generate in more places but popped off in sunlight.
4. **World generation:** Version page reports gold ore generated twice as often and diamond ore twice as often in slightly smaller blobs. The generation-history page separately reports redstone-ore blobs and flags a coal-blob size change as needing verification. No biome system yet (Alpha 1.2.0 boundary). Exact noise and ore-height/attempt parameters are unknown; client artifact is lost.
5. **Mobs/entities:** Wiki history reports improved mob AI, but gives no exact pathing/targeting implementation detail. Mob spawners still exist but are no longer obtainable by ordinary block drops.
6. **Player mechanics:** Detailed movement/health/combat behavior not established by the sources consulted.
7. **Combat/progression:** No later hunger/XP/enchanting assumptions apply. Exact damage/armor rules not yet researched.
8. **Redstone/physics:** First redstone circuitry reported. Dust had only on/off visual states; iron doors required redstone, rather than the later broad interaction options. Exact signal strengths/tick delay/repeater-free wiring rules require further investigation.
9. **Dimensions/structures:** Nether not yet present (Alpha v1.2.0). Dungeons predate this build; Alpha v1.0.14 later doubles generator attempts. Exact dungeon loot/spawner behavior not audited.
10. **Visual/audio:** Not yet audited.
11. **Technical/internal:** The client is lost, preventing binary verification of reported behavior.
12. **Incomplete/experimental/unused:** No source evidence yet for internal unused entries; state beyond the Wiki record is unknown.
13. **Bugs/obscure behavior:** The coal blob-size detail remains specifically marked verification-required by the world-generation history.
14. **Difference from preceding researched version:** First researched target; chronological predecessor (Infdev) is outside requested list.
15. **Recreation notes:** Treat the build as artifact-unavailable; label claims secondary-reported. Keep IDs and upgrade behavior historically specific and do not infer them from later registries.
16. **Sources/unresolved:** [Alpha v1.0.1](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.1); [world-generation history](https://minecraft.wiki/w/World_generation/History); [archived announcement](https://web.archive.org/web/0/https://notch.tumblr.com/post/776113692). Open: exact ore/distribution values, redstone update/timing details, mob AI and missing-client verification.

#### Alpha v1.0.4
1. **Identity/context:** “Seecret Friday 4,” 9 July 2010. A client artifact is listed; details are in [version history](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.4) and the [archived announcement](https://web.archive.org/web/0/https://notch.tumblr.com/post/789757070).
2. **New blocks/items:** Snow and ice; snow layers existed in terrain but were not yet obtainable as an item according to the version history.
3. **Changed/removed:** No further confirmed inventory change in sources consulted.
4. **World generation:** Introduced selectable winter-mode worlds. The history reports a 25% chance at world creation; exposed water froze and exposed surfaces accumulated snow. Gravel-beach water was not fully frozen. Normal world generation appears unaffected. This is a generator/world-type mode, not a later climate-driven snow biome.
5. **Mobs/entities:** Mob physics changed; jumps were shorter. Exact entity-specific effects are not fully documented.
6. **Player mechanics:** Added sitting pose; confirm whether this is an emote/keybinding or other state before reproducing exact controls.
7. **Combat/progression:** No changes identified; not evidence of absence.
8. **Redstone/physics:** Ice is slippery and can melt from fire/lava. No specific redstone change identified.
9. **Dimensions/structures:** No new dimension identified.
10. **Rendering/audio:** Winter snow/ice visual palette; other render/audio changes not verified.
11. **Technical/internal:** Alternate winter-world mode is distinguished from normal generation.
12. **Incomplete/experimental/unused:** Snow-layer item acquisition absent; generated snow should not be treated as normally harvestable.
13. **Bugs/obscure behavior:** Winter mode is alternate world generation; do not map its 25% creation selection to biome probabilities.
14. **Difference from v1.0.1:** Seasonal world type and ice/snow plus shorter mob jumps/sitting pose.
15. **Recreation notes:** Persist world-type flag and apply freeze/snow rules only to the winter world; preserve gravel-beach exception and era-specific ice slipperiness.
16. **Sources/unresolved:** [v1.0.4](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.4), [archived announcement](https://web.archive.org/web/0/https://notch.tumblr.com/post/789757070). Need direct artifact test for freezing edge conditions.

#### Alpha v1.0.6
1. **Identity/context:** “Seecret Friday 5,” 16 July 2010. [Version page/client metadata](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.6); [archived announcement](https://web.archive.org/web/0/https://notch.tumblr.com/post/819109118).
2. **New blocks/items:** Cactus; boats. Snowballs stack to 16 and can be thrown without disappearing.
3. **Changed/removed:** Leaves no longer decay; sapling drop chance reduced 10%→5%; snow blocks became water-resistant. Snow-block recipe uses four snowballs, while breaking yields six, allowing repeatable/infinite snowball supply.
4. **World generation:** Cacti naturally generate on sand. Fancy trees now generate alongside small trees; precise frequencies unverified.
5. **Mobs/entities:** Pigs/sheep spawn only above grass per version record. Exact rates/AI/drops otherwise not audited.
6. **Player mechanics:** Boat was added; early boat durability is low—version history records breakage on land placement/impact—so don't use modern boat physics. It can fit through one-block water openings per historical notes.
7. **Combat/progression:** Not researched beyond above.
8. **Redstone/physics:** Cactus behavior not yet exact; boat collision/breakage and snow/water interaction are documented physical changes.
9. **Dimensions/structures:** No new dimension/structure identified.
10. **Rendering/audio:** No identified change.
11. **Technical/internal:** Client download listed; no corresponding server identified on page.
12. **Incomplete/experimental/unused:** No identified itemized unfinished feature.
13. **Bugs/obscure behavior:** Snowballs are renewable through the snow-block yield/recipe mismatch.
14. **Difference from v1.0.4:** Cacti/boats and mixed tree sizes; leaf decay and sapling odds altered.
15. **Recreation notes:** Reproduce legacy boat breakage, sand cactus placement and the snowball ratio; do not assume later leaf decay.
16. **Sources/unresolved:** [v1.0.6](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.6), [archived announcement](https://web.archive.org/web/0/https://notch.tumblr.com/post/819109118). Verify detailed boat collision/health experimentally.

#### Alpha v1.0.11
1. **Identity/context:** “Seecret Friday 6,” 23 July 2010. [Version page](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.11).
2. **New blocks/items:** Clay, reeds (later named sugar cane), clay balls, paper, books, brick items/blocks, slimes; cows can be milked with buckets.
3. **Changed/removed:** Bookshelf and brick block crafting recipes restored; bookshelves dropped nothing when mined. Cactus now only on sand and cannot be placed adjacent to another block; cactus harms mobs only when contacted.
4. **World generation:** Clay generates near water; reeds appear near beaches and can grow up to three blocks. Tree/flower patch generation changed. Exact placement attempts and heights not documented.
5. **Mobs/entities:** Slimes split into smaller forms on death. Historical notes state splitting only when killed by fist, spawn below Y=32, and only smallest form drops slimeballs; slimeballs had no use then. Exact health/size/roll behavior requires code or build tests.
6. **Player mechanics:** Bucket milking of cows added. No hunger/food bar.
7. **Combat/progression:** Slimeball had no functional use, a clear obtainable-but-useless state.
8. **Redstone/physics:** Cactus's adjacency/placement/contact changes are version-specific.
9. **Dimensions/structures:** No new dimension/structure identified.
10. **Visual/audio:** Not researched.
11. **Technical/internal:** Item/block IDs and save details not yet established.
12. **Incomplete/experimental/unused:** Slimeballs obtainable but without a use.
13. **Bugs/obscure behavior:** Bookshelf mining yielded no drops; slime split rule was fist-dependent per the version page.
14. **Difference from v1.0.6:** Adds early paper/book/brick/clay and slime systems, plus milk and terrain vegetation changes.
15. **Recreation notes:** Make sugar cane/clay worldgen and slime drops follow old rules; do not import later slimeball crafting uses.
16. **Sources/unresolved:** [v1.0.11](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.11). Exact drop probabilities/AI and all patch-specific changes remain open.

#### Alpha v1.0.14
1. **Identity/context:** “Seecret Friday 7,” 30 July 2010. Two distinct clients are documented; original and reupload differ. [Version page with artifact manifests](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.14).
2. **New blocks/items:** Chickens/eggs, jukeboxes/music discs, furnace minecart and chest minecart.
3. **Changed/removed:** Original client jukebox recipe incorrectly yielded bookshelf; same-day reupload fixed it and changed the splash text. Cacti can grow to three blocks. Obsidian blast resistance increased to 1,200.
4. **World generation:** Dungeon generator iterations increased 4→8, increasing dungeon generation and affecting subsequent decorator RNG outcomes such as trees/flowers. This means downstream distribution changes even if their own feature parameters did not change.
5. **Mobs/entities:** Chickens/eggs and minecart variants. Slime spawning was reduced and then disabled due to a reported spawn-limit miscalculation.
6. **Player mechanics:** Music discs obtained in dungeon chests or when a skeleton kills a creeper. Furnace minecart powered by coal and moves itself. Exact interaction/control rules not yet reviewed.
7. **Combat/progression:** Skeleton/creeper disc acquisition is a gameplay-specific loot trigger.
8. **Redstone/physics:** Minecart and jukebox mechanics present; exact rail interaction/timing not researched.
9. **Dimensions/structures:** Dungeons remain; their increased attempts change population order and other generated content.
10. **Visual/audio:** Music is re-enabled and now plays throughout the world rather than only as ambient music. Three tracks are added (`piano1`, `piano2`, `piano3`, now known as Dry Hands, Wet Hands and Mice on Venus). Chicken sound events, jukebox/disc playback, a revised title logo and new splashes also arrive. The original client’s jukebox recipe bug is a distinct artifact boundary; the same-day reupload fixes it. [Alpha 1.0.14 raw](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.14?action=raw), [Music history](https://minecraft.wiki/w/Music?action=raw).
11. **Technical/internal:** Same-day original/reupload is a separate artifact boundary; use manifest/hash if reproducing jukebox recipe.
12. **Incomplete/experimental/unused:** Slime spawns disabled due to a bug; don't infer permanent removal.
13. **Bugs/obscure behavior:** Original jukebox recipe bug; slime population-limit miscalculation. Dungeon attempts have RNG cascade effects.
14. **Difference from v1.0.11:** Music and minecart content/chicken mob; dungeon attempt count doubled.
15. **Recreation notes:** Pin the exact client artifact; avoid treating the reupload recipe correction as present in the first build.
16. **Sources/unresolved:** [v1.0.14](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.14). Exact slime-disable sub-build and generator attempt parameters need artifact comparison.

#### Alpha v1.0.17
1. **Identity/context:** “Seecret Friday 8,” 20 August 2010. Client is lost; the page identifies server 0.1.4 and says the client was replaced after eight minutes. [Version page](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.17); [archived IRC logs](https://archive.org/details/Minecraft-IRCLogs).
2. **New blocks/items:** Oak fences; players reportedly began with 64 in inventory.
3. **Changed/removed:** Milk buckets can be emptied.
4. **World generation:** No identified generator change.
5. **Mobs/entities:** Spider jockey introduced. Chickens no longer take fall damage and glide downward. Pigs, chickens and slimes added to multiplayer, per page.
6. **Player mechanics:** Working multiplayer day/night cycle; detailed player data synchronization not yet established.
7. **Combat/progression:** Not researched.
8. **Redstone/physics:** Fence behavior not fully audited.
9. **Dimensions/structures:** No new dimension/structure identified.
10. **Visual/audio:** Contemporary IRC reports broken/invisible inventory icons during the short-lived client; treat as transient build bug, not stable mechanic.
11. **Technical/internal:** Artifact availability compromised; no client for direct verification.
12. **Incomplete/experimental/unused:** Very brief build/replacement; transient UI defect evidenced by contemporary logs.
13. **Bugs/obscure behavior:** Inventory icons reported broken during the short-lived build; evidence is contemporary report, not test.
14. **Difference from v1.0.14:** Fences and spider jockey, multiplayer animal/slime support and day/night cycle.
15. **Recreation notes:** Avoid asserting behavior beyond preserved server/IRC/wiki evidence; expose uncertainty for lost client.
16. **Sources/unresolved:** [v1.0.17](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.17), [IRC logs](https://archive.org/details/Minecraft-IRCLogs). Client-side exactness unresolvable absent artifact.

#### Alpha v1.1.0
1. **Identity/context:** “Seecret Friday 9,” 10 September 2010; multiple artifacts: original lost, reuploaded 10 September and again 13 September. The launcher entry resolves to the first reupload. [Version page](https://minecraft.wiki/w/Java_Edition_Alpha_v1.1.0); [archived Notch announcement](https://web.archive.org/web/0/https://notch.tumblr.com/post/1098183801).
2. **New blocks/items:** Compass, points toward world spawn.
3. **Changed/removed:** Monster spawners stopped rendering their contained mob (visual/internal change).
4. **World generation:** No identified terrain change.
5. **Mobs/entities:** Experimental switch enables monsters in multiplayer; exact spawn logic not detailed.
6. **Player mechanics:** Shared client/server inventory; player inventory persisted on multiplayer server and player location persisted on disconnect. Chests, signs and furnaces began working in multiplayer.
7. **Combat/progression:** Multiplayer monsters remained behind an experimental setting; not evidence of parity with singleplayer AI.
8. **Redstone/physics:** Multiplayer block entity support (chest/sign/furnace) matters to recreations; redstone details not audited.
9. **Dimensions/structures:** None identified.
10. **Visual/audio:** Camera-through-walls exploit fixed by second reupload.
11. **Technical/internal:** Separate reuploads/launcher mapping; shared inventory/server persistence and multiplayer tile-entity support mark a networking architecture step.
12. **Incomplete/experimental/unused:** Monster enablement experimental.
13. **Bugs/obscure behavior:** Camera wall exploit fixed only in second reupload.
14. **Difference from v1.0.17:** Multiplayer inventory/world interactions and compass; mobs optional/experimental.
15. **Recreation notes:** Match precise reupload and keep monsters disabled unless the relevant experimental setting is active.
16. **Sources/unresolved:** [v1.1.0](https://minecraft.wiki/w/Java_Edition_Alpha_v1.1.0), [archived announcement](https://web.archive.org/web/0/https://notch.tumblr.com/post/1098183801). Exact multiplayer synchronization and protocol packet structure not verified.

#### Alpha v1.2.0
1. **Identity/context:** Halloween Update, 30 October 2010; original client unavailable; a reupload is documented. Important launcher trap: launcher “1.2.0” actually launches v1.2.0_02, not exact v1.2.0. [Version page](https://minecraft.wiki/w/Java_Edition_Alpha_v1.2.0); [archived announcement](https://web.archive.org/web/0/https://notch.tumblr.com/post/1444413329).
2. **New blocks/items:** Nether portals, netherrack, soul sand, glowstone, pumpkins/jack-o’-lanterns, clocks, fishing rod/raw fish; associated Nether materials.
3. **Changed/removed:** Slimes removed from mob spawning code. The selectable winter world mode from Alpha v1.0.4 is removed as biome-based weather replaces it. Alpha v1.2.0_01/_02 adjust soul-sand distribution, so launcher patch should not be mistaken for exact original. The player’s “ough” damage sound is removed.
4. **World generation:** Major generator discontinuity: adds 12 listed biomes; history identifies Ice Desert as unused. The WIP world-generation history reports a depth-noise-like scale changing 100→200 and monoliths ceasing, but labels the datapoint as a development preview and its causal attribution conjectural; treat as low-medium-confidence, not binary-confirmed Alpha 1.2.0. It also reports an all-bedrock bottom layer. Fishing source history gives `1 in 500 chance per tick` while bobber is in water for raw fish (rate interpretation/test semantics unverified). Near 32 million blocks, fake non-solid chunks appear; lighting and listed decoration/dungeon generation stop past boundary. Exact noise field and chunk RNG call order not available from summary. [WIP world-generation history](https://minecraft.wiki/w/World_generation/History?action=raw).
5. **Mobs/entities:** Ghasts and zombie pigmen added; zombie pigmen neutral until attacked, then nearby group retaliation. Ghast fireballs use snowball texture and explode on impact. Slimes no longer naturally spawn.
6. **Player mechanics:** Fishing introduced; documented catch is raw fish only and chance is tick-based. Nether travel maps Overworld distance at 8:1.
7. **Combat/progression:** Portal/dimension progression begins; no later hunger, XP or End systems.
8. **Redstone/physics:** Nether water evaporates; fireball/explosion and portal transport are era-specific. Other redstone not audited.
9. **Dimensions/structures:** Nether is introduced and reachable through player-built portal; Nether terrain and portal rules are distinct from later biome-rich Nether.
10. **Visual/audio:** Adds six music tracks (`hal1`–`hal4`, `nuance1`–`nuance2`), changes the Wet Hands mix, and extends Mice on Venus. Swamp biome tint and Nether visual identity appear; ghast fireball reuses snowball texture as an asset only. Exact music-asset timing across the original and `_01`/`_02` artifacts is uncertain. [Alpha 1.2.0 raw](https://minecraft.wiki/w/Java_Edition_Alpha_v1.2.0?action=raw), [Music history](https://minecraft.wiki/w/Music?action=raw).
11. **Technical/internal:** World-height/bedrock floor changed; fake chunks and generation/lighting cutoffs near coordinate boundary. Launcher metadata does not select the exact original v1.2.0.
12. **Incomplete/experimental/unused:** Ice Desert biome listed unused; no natural spawn of slimes after removal.
13. **Bugs/obscure behavior:** The “1.2.0” launcher version is actually `_02`; do not validate exact `.0` against it. Generation boundaries near 32M produce fake non-solid chunks.
14. **Difference from v1.1.0:** New Nether/dimensions, biome generator, fishing, ghast/zombie-pigman, pumpkin/clock and major terrain shift.
15. **Recreation notes:** Pin the exact archived artifact and distinguish v1.2.0 from `_01`/`_02`; replace the pre-Halloween terrain algorithm rather than toggling biome IDs on the prior map.
16. **Sources/unresolved:** [v1.2.0](https://minecraft.wiki/w/Java_Edition_Alpha_v1.2.0), [world-generation history](https://minecraft.wiki/w/World_generation/History), [archived Notch announcement](https://web.archive.org/web/0/https://notch.tumblr.com/post/1444413329). Exact generator parameters, biome placement, fishing RNG and boundary chunk behavior remain to verify.

### Minecraft Beta

#### Beta 1.0
1. **Identity/context:** First Java Edition Beta release, 20 December 2010; distinct from Release 1.0.0. Protocol 7 and Mojang client metadata linked on [version page](https://minecraft.wiki/w/Java_Edition_Beta_1.0); contemporary [Notch announcement](https://twitter.com/notch/status/2389820531).
2. **New blocks/items:** No major block additions; inventory tooltips and cape support (only Mojang cape existed then), plus deadmau5 ears cosmetic support.
3. **Changed/removed:** Eggs stack to 16 (from 64), can be thrown, with 1/8 chicken spawn chance. Leaf decay reintroduced with changed behavior. Armor-slot outlines removed. Crafting-grid slots no longer hold items; attempted placement drops them.
4. **World generation:** Existing climate-era generator; lava lakes replace neighboring blocks with stone. Beta 1.0 grassland/sand boundary is misaligned until Beta 1.3. Exact generator parameters unknown.
5. **Mobs/entities:** Thrown eggs can create chickens at 12.5%; no other new mob established.
6. **Player mechanics:** Inventory becomes server-side functional; item-name tooltip appears on hover. Chest UI closes if player moves too far or chest explodes. `/kill` fixed. No hunger/sprint/XP/enchanting.
7. **Combat/progression:** Tools on ground no longer repair themselves (durability behavior); no End progression.
8. **Redstone/physics:** No major circuit change identified.
9. **Dimensions/structures:** Nether inherited; no Beta 1.8 strongholds/villages/mineshafts.
10. **Rendering/audio:** Cape support, deadmau5 ears and splash text changes; sound/lighting not researched.
11. **Technical/internal:** Servers save chunks less often; chunks are not resaved if saved during preceding 30 seconds. Server-side inventory fixes multiplayer state issues. Protocol 7, Java 5.
12. **Incomplete/experimental/unused:** Cape pipeline supported although only Mojang cape existed then.
13. **Bugs/obscure behavior:** Crafting-grid placement drops items; leaves decay returns; tools no longer self-repair. Biome-border and lava-lake quirks.
14. **Difference from Alpha v1.2.0:** Beta transition plus server-side inventory/tooltip and item/leaf/chunk-save changes.
15. **Recreation notes:** Preserve egg odds/stack limit, crafting-slot behavior and 30-second save throttle; do not import the integrated server architecture added later.
16. **Sources/unresolved:** [Beta 1.0](https://minecraft.wiki/w/Java_Edition_Beta_1.0), [raw version history](https://minecraft.wiki/w/Java_Edition_Beta_1.0?action=raw), [generation history](https://minecraft.wiki/w/World_generation/History), [Notch announcement](https://twitter.com/notch/status/2389820531). Save-throttle behavior and boundary bug not independently tested.

#### Beta 1.2
1. **Identity/context:** Released 13 January 2011; protocol 8; Java 5. [Version page/raw history](https://minecraft.wiki/w/Java_Edition_Beta_1.2) (page links hash-identified client metadata).
2. **New blocks/items:** Charcoal, note blocks, dyes/colored wool, squid/ink sacs, sugar, dispensers, lapis lazuli and block/ore, bone/bone meal, sandstone, birch/spruce logs/leaves, cake, cocoa beans (Creative/editor-only at this point), and other listed items.
3. **Changed/removed:** Sugar cane renamed from reeds; crafting table renamed from workbench. Tool durability increased: wood 33→59, stone 65→131, iron 129→250, diamond 1025→1561. Gold tools gained much faster mining in some categories while retaining old durability/restrictions. Wool dyeing restored; brown wool still external-tool-only. Stone slab crafting and several dye recipes altered.
4. **World generation:** Birch trees added to forests, spruce trees replace oak in taiga; lapis ore appears. Beta 1.2_02 adjusts lapis frequency/height—do not backdate. Wiki history reports more coal/iron/diamond in caves but does not settle whether frequency or blob size changed.
5. **Mobs/entities:** Squid added, 10 health, drops ink sac, no sounds and could be milked in this build (fixed Beta 1.3). Skeletons now drop bones; spiders can climb walls. Sheep can be dyed and some gray/black colors spawn naturally. Monster spawners show rotating mob model again in singleplayer.
6. **Player mechanics:** Cake restores 3 health per slice; no hunger meter. Bone meal fertilizes saplings/wheat. Fishing remains from Alpha.
7. **Combat/progression:** Increased tool/sword durability and stronger gold mining speed. Lapis is dye, not enchantment currency; enchanting absent.
8. **Redstone/physics:** Note blocks play instrument determined by block underneath and emit on click/use/power; dispenser drops contents. A documented bug: powered redstone fails to update properly when a new source is added. Rails may orient north incorrectly.
9. **Dimensions/structures:** Nether inherited; no strongholds/villages/mineshafts.
10. **Rendering/audio:** Note blocks add musical sounds; texture/render changes include furnace and multiplayer state syncing; performance regression/lag spikes noted.
11. **Technical/internal:** Beta 1.2 multiplayer state synchronization/painting fixes; PowerPC color/audio fixes. Protocol 8, Java 5.
12. **Incomplete/experimental/unused:** Brown wool and cocoa beans are registry/content but only external-editor obtainable; no player survival route. Do not confuse presence of IDs with natural or craftable access.
13. **Bugs/obscure behavior:** Entering Nether portal while mounted on boat/minecart/pig can crash and delete player data (dangerous release). Squid milking and redstone source updates are bugs. `_01`/`_02` differ.
14. **Difference from Beta 1.0:** Adds large crafting/resource suite, squid, note blocks, dispenser, lapis, sandstone and birch/spruce generation; also changes tool durability.
15. **Recreation notes:** Encode exact item availability and drops; preserve squids' old milking behavior only in base version; apply lapis patch separately. If exact failure emulation is in scope, flag data-loss crash.
16. **Sources/unresolved:** [Beta 1.2 page](https://minecraft.wiki/w/Java_Edition_Beta_1.2), [generation history](https://minecraft.wiki/w/World_generation/History). Exact initial-vs-_01/_02 item deltas and numeric ore params remain unresolved.

#### Beta 1.3
1. **Identity/context:** Released 22 February 2011; protocol 9. Original client at 17:13 UTC had debug item spawn; first reupload at 17:33 removed it; second reupload at 17:50 fixed a crash and is launcher-listed as `b1.3b`. Page lists separate server builds. The original artifact has Java 6 dependency; Beta 1.3_01 removes Java 1.6 dependencies, so runtime minimum must be tied to the exact client/reupload. [Version page/raw](https://minecraft.wiki/w/Java_Edition_Beta_1.3).
2. **New blocks/items:** Beds; redstone repeaters; cobblestone/wood/sandstone slabs; smooth stone slab (present but unobtainable); “Skull on Fire” painting content. Repeater extends redstone transmission. Craftable beds use planks and wool; any wool color may be used while bed output stays red.
3. **Changed/removed:** Sandstone generates under natural sand and has faster mining; stone slabs switch to stone recipe; double slabs drop 2 slabs. Stone pressure plate recipe decreases from three material to two. Gunpowder renamed from sulphur. Lapis multiplayer ore drops 4–8.
4. **World generation:** Sandstone under naturally generated sand; Beta 1.0 grassland/sand biome boundary fixed (no longer visibly misaligned at chunk borders). No structure epoch.
5. **Mobs/entities:** Squid can no longer be milked; slime spawn again; squid now swims in 3D instead of remaining at water-bottom. Multiplayer hostile mobs can be attacked properly.
6. **Player mechanics:** Beds allow sleeping/time skip. Do not attribute bed-spawn reset to Beta 1.3; that behavior appears in Beta 1.4 and is bugged there (multiplayer-only and only reset after clicking “Leave bed”).
7. **Combat/progression:** No hunger/XP/enchanting. Multiplayer combat bug fixed.
8. **Redstone/physics:** Repeaters provide delayed/further redstone; powered dust gets darker farther from source and unpowered dust is black; exact tick details require implementation study.
9. **Dimensions/structures:** Nether inherited; no village/stronghold/mine shafts.
10. **Rendering/audio:** Smooth-lighting engine and graphics option added; new splash/loading screen. Redstone dust tint changes.
11. **Technical/internal:** World seed/name selection introduced; multiple world slots and `/time`/whitelist commands added. Mojang’s contemporaneous seed explanation says blank input uses computer clock, same seed/version generates the same map, text strings use Java `String.hashCode()`, and the map seed is used for worldgen only (not spawn location or mob drops). [Jeb/Mojang seed explanation](https://web.archive.org/web/20110225221214/http://mojang.com/2011/02/23/a-short-demystification-of-the-map-seed/).
12. **Incomplete/experimental/unused:** Smooth stone slab present in assets/recipe context but explicitly unobtainable; painting texture placeholder used because painting code predated its texture.
13. **Bugs/obscure behavior:** First client starts players with 64 torches, diamond pickaxe/shovel/axe and 64 beds on creation/respawn due to debug code; removed by 17:33 reupload. Another reupload fixes crash opening world menu without saves directory. This is an artifact-level split.
14. **Difference from Beta 1.2:** Beds/repeaters/slabs and worldgen fix; smooth lighting; player-entered seeds; multiple reuploads.
15. **Recreation notes:** Build identity must distinguish pre-reupload debug spawn, launcher `b1.3b`, and server versions; preserve the version-specific bed spawn behavior.
16. **Sources/unresolved:** [Beta 1.3 page](https://minecraft.wiki/w/Java_Edition_Beta_1.3), [world-generation history](https://minecraft.wiki/w/World_generation/History). Exact region file internals/upgrade error cases need technical source.

#### Beta 1.4
1. **Identity/context:** Released 31 March 2011. Original and reuploaded clients; reupload about 90 minutes later removed debug code. [Version page/raw](https://minecraft.wiki/w/Java_Edition_Beta_1.4).
2. **New blocks/items:** Wolves, cookies and the April Fools locked chest; a changed Minecraft logo and missing-texture fallback. Locked chest can only break via explosions; adjacent ones form a double chest.
3. **Changed/removed:** Bed spawn position reset added but bugged: source notes it only changes when clicking “Leave bed” and works only for multiplayer; “does not work for most users.” Ladders: sneaking while climbing holds position. Wheat grows faster.
4. **World generation:** Locked chest can randomly generate only when system date is April 1 (special joke content, not ordinary structure). Dungeon chests gain cocoa beans. No terrain generator change.
5. **Mobs/entities:** Wolves neutral, common in taiga, 10 health, tame via bones at 1/3 chance per bone; tamed wolves defend owners, can assist combat, become angry if player/owner's wolves hit them; tails indicate health; raw pork can be fed; drop nothing when killed. Spider sounds removed; spiders stop trampling crops.
6. **Player mechanics:** Cookies heal 1 health and stack to 8 (first stackable food); crafted from wheat+cocoa beans. Inventory default key becomes E. Achievement infrastructure includes inventory/wood/workbench triggers, but the complete achievement set/UI arrives incrementally.
7. **Combat/progression:** Wolf owner defense; wolf health/anger behavior distinct from modern passive animal AI. Cookie heals directly; no hunger bar.
8. **Redstone/physics:** No major circuit change identified.
9. **Dimensions/structures:** Nether inherited. Locked chest is an April 1 prank block linked originally to store, not ordinary loot content.
10. **Rendering/audio:** New wolf barks/pants/whines/growls/death/hurt sounds; Minecraft logo change; texture fallback avoids game crash if texture missing (only directly observable by modded asset removal).
11. **Technical/internal:** Debug code in original tracks walking distance in memory, apparently future Beta1.5 statistic, removed in reupload. GUI scale can be changed in options.txt.
12. **Incomplete/experimental/unused:** Locked chest was prank/demo/store placeholder; achievement system infrastructure partly implemented rather than complete final achievements UI.
13. **Bugs/obscure behavior:** Zombies/spiders previously only hurt when one block above player; fixed to hurt in all instances. Multiplayer client slime spawn fixed. Reupload removes debug walking-stat code.
14. **Difference from Beta 1.3:** Wolf mob, cookie healing, prank locked chest, partial achievements and bug fixes.
15. **Recreation notes:** Use 1/3 per bone and legacy wolf stats; don't apply modern wolf breeding/armor/behavior. Preserve broken spawnpoint behavior and date-gated prank block.
16. **Sources/unresolved:** [Beta 1.4 page](https://minecraft.wiki/w/Java_Edition_Beta_1.4), [raw version history](https://minecraft.wiki/w/Java_Edition_Beta_1.4?action=raw), [Notch announcement citation](https://twitter.com/notch/status/4279964053). Patch 1.4_01 detail remains separate.

#### Beta 1.5
1. **Identity/context:** Released 19 April 2011; protocol 11, Java 5; original and reupload are noted, followed by `_01`/`_02`. [Version page/raw](https://minecraft.wiki/w/Java_Edition_Beta_1.5).
2. **New blocks/items:** Birch/spruce saplings, detector rails, powered rails, cobweb. Cobweb exists but cannot be obtained without inventory editing; slows players/mobs and drops string when sword-broken. 16 achievements and statistics introduced.
3. **Changed/removed:** Saplings smelt 0.5 items each; birch/spruce leaves drop respective saplings. Redstone wire can be placed on snow and render states change. Ladder recipe now makes 2 to offset staggered-ladder exploit. Players can shift-click between chest/inventory; dropped items in flowing water move faster.
4. **World generation:** Natural rain, snowfall and thunderstorms; no new base generator. Weather operates with biome snow/rain rules; probabilities not quantified here.
5. **Mobs/entities:** Lightning strikes create fire/deal nearby damage; creepers become charged and explode with greater force; pigs struck by lightning turn into zombie pigmen. Wolves become more common outside taiga/forest and pathfinding through doors improves.
6. **Player mechanics:** Unarmed damage increases 1→2 health points. Beds can be used during daytime thunderstorms. Achievements track actions; exact 16-event definitions are on history page.
7. **Combat/progression:** Achievement/statistics UI and new tracking. Charged creepers change explosion threat; no hunger/XP.
8. **Redstone/physics:** Powered rail requires redstone, transmits signal across up to 8 powered rails and accelerates minecarts. Detector rail detects minecart and emits redstone. Exact ticks/speeds beyond source-listed behavior need code check.
9. **Dimensions/structures:** No new dimension/structure.
10. **Rendering/audio:** Release implements rain, snow, thunderstorms and unique rain sounds; do not import the teaser’s flowing-water rain audio. In multiplayer, weather transitions start/stop instantly. Visual changes include footstep particles (not a footstep sound), GUI scale, Advanced OpenGL/occlusion option, dimmer stars, biome-specific Fancy-grass sides, redstone-wire rendering/color changes, and smooth-lighting fixes for portals/ice. Beta 1.5_01 later fixes rain passing through glass/stairs/slabs. [1.5 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.5?action=raw), [1.5_01 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.5_01?action=raw).
11. **Technical/internal:** Significant performance improvements; statistics menu. Original/reupload client and protocol 11.
12. **Incomplete/experimental/unused:** Cobweb not normally obtainable; only editor/inventory. Achievement implementation has 16 at this point.
13. **Bugs/obscure behavior:** Cactus? no. Patch/test boundaries separate. Reupload exact contents need comparison; weather-specific fixes documented.
14. **Difference from Beta 1.4:** Weather, statistics/achievements, powered/detector rails, cobweb and saplings, charged creeper, lightning transformations.
15. **Recreation notes:** Implement original rail signal distance and charged creeper lightning behavior, no modern weather defaults; preserve the unobtainable cobweb distinction.
16. **Sources/unresolved:** [Beta 1.5 page](https://minecraft.wiki/w/Java_Edition_Beta_1.5), [raw version history](https://minecraft.wiki/w/Java_Edition_Beta_1.5?action=raw). Exact weather RNG and `_01`/`_02` deltas need review.

#### Beta 1.6
1. **Identity/context:** Released 26 May 2011; protocol 13. Client and release server JAR listed; server `.exe` lost. This version has test builds (only Beta to do so per page). [Version page/raw](https://minecraft.wiki/w/Java_Edition_Beta_1.6).
2. **New blocks/items:** Map, wooden trapdoor, dead bush, short grass and fern. Dead bush naturally occurs only in deserts and can be placed only on sand; shrub is an internal/Creative-editor entry that generates nowhere. Grass patches appear on grass in most grassy-soil biomes; seeds found in grass/fern; fern naturally only in swamps/rainforests.
3. **Changed/removed:** Bed in Nether explodes. Fire spread severely slowed and no longer spreads infinitely. Ice regenerates even when not snowing. Mushrooms spread slowly. Leaves on Fast no longer opaque solid blocks for placement/spawning. Powered rail speed rises 0.04→0.06 to offset removal of glitch boosters. Below a layer of source blocks, water no longer behaves as source. Player dismounts from boat/minecart entering Nether portal; earlier portal-ride crash fixed. Combat missed-hit 10 tick cooldown removed. Shift-click crafting/inventory behavior expanded.
4. **World generation:** Public Beta 1.6 fixes an ore-generation quadrant bias: before this version, ores were reported at least 50% more common in southeast (+X,+Z) than northwest (-X,-Z). It also adds grass patches on grass blocks, ferns in swamp/rainforest and dead bushes only in deserts/on sand. Distinct Beta 1.6 Test Build 3 artifacts add randomized vegetation details and contain a clay-coordinate defect (`x=z`) later fixed in 1.7; do not conflate the unreleased test build with public 1.6. [1.6 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.6?action=raw), [Test Build 3](https://minecraft.wiki/w/Java_Edition_Beta_1.6_Test_Build_3?action=raw), [1.7 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.7?action=raw).
5. **Mobs/entities:** No newly introduced mob established here; entities receive many multiplayer sync/fix changes.
6. **Player mechanics:** Maps display overhead section; map item added. Player slides toward nearest opening when stuck in wall. Shift-click crafts max and routes output to inventory; shift-click moves inventory stacks to hotbar.
7. **Combat/progression:** Missed-hit cooldown removed. Hunger/XP not yet present. Bed-in-Nether explosion is a dangerous behavior.
8. **Redstone/physics:** Powered rail speed change; trapdoor can be opened like a door. Floor levers sometimes fail downward signal (fixed later); extensive block-update fixes.
9. **Dimensions/structures:** Nether supported in multiplayer from this release. Nether beds explode; portal riding dismounts. No new dimension.
10. **Rendering/audio:** Map GUI; texture packs gain grass/foliage color-map files (but maps initially were not read; distinguish capability from fix); inventory block icons brighten; Advanced OpenGL temporarily disabled. Beta 1.6 fixes underwater rain/snow, lighting and rain-through-liquids, rain sound on Fast graphics, glass blocking rain sound, slow rain and bogus client-side multiplayer weather transitions. [1.6 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.6?action=raw).
11. **Technical/internal:** Server properties gain `view-distance` and `allow-nether`; network compression strengthened. Many multiplayer client/server sync fixes. Server EXE lost, not server JAR.
12. **Incomplete/experimental/unused:** Shrub block does not generate and is only editor-obtainable. Keep it out of naturally generated block palettes.
13. **Bugs/obscure behavior:** 97 bugs listed as fixed, including previous portal-ride crash, save-while-sneaking falling through ground, unreliable compass, quadrant ore density bug, client-side fake trees/weather and inventory oversize. Surviving issue: shift-clicking a container with full inventory crashes. Test-build clay bug separately.
14. **Difference from Beta 1.5:** Maps, trapdoors, Nether multiplayer and biome vegetation; extensive network/movement/fire/rail/portal fixes.
15. **Recreation notes:** Versioned map and weather UI; implement explosive Nether bed, multiplayer portal and dismount rules; no shrub worldgen; reproduce or fix test-build ore/clay only for exact development build.
16. **Sources/unresolved:** [Beta 1.6](https://minecraft.wiki/w/Java_Edition_Beta_1.6), [raw version page](https://minecraft.wiki/w/Java_Edition_Beta_1.6?action=raw), [generation history](https://minecraft.wiki/w/World_generation/History). Exact Test Build 3 artifact mapping and 1.6.1–1.6.6 differences still need review.

#### Beta 1.7
1. **Identity/context:** Released 30 June 2011; protocol 14. Initial 1.7 differs from `_01` and later 1.7.3. [Version page/raw](https://minecraft.wiki/w/Java_Edition_Beta_1.7).
2. **New blocks/items:** Piston, sticky piston and shears. Shears collect leaves and wool without harming sheep; 238 uses, crafted from two diagonally placed iron ingots. Stone-brick tooltip text exists but the block is not yet in game. Silverfish texture exists but mob entity is not present.
3. **Changed/removed:** Sheared sheep drop 2–4 wool; unsheared sheep killed drop one; punching sheep no longer yields wool. Cobweb now properly broken with shears or sword and drops string. TNT no longer primes by empty-hand/item hit; use flint-and-steel or redstone. Old behavior remains hidden in metadata value 1 (accessible via editors). Bedrock hardness now defined unbreakable rather than -1.
4. **World generation:** Fixes Beta 1.6 clay generation bug; no new terrain generator. No natural stone-brick generation.
5. **Mobs/entities:** Silverfish is texture only—no mob yet. Sheep drop/shearing changes apply.
6. **Player mechanics:** Shears have fixed 238-use durability and allow nonlethal wool harvesting; game now displays F3 direction value.
7. **Combat/progression:** No hunger/XP/sprint. Sheep death/shearing drop mechanics changed.
8. **Redstone/physics:** Piston pushes blocks when powered; sticky piston pushes and pulls. Recipe combines wood/cobblestone/iron/redstone; sticky piston crafted with slimeball+piston. TNT redstone activation added. Some block push/pull combinations crash or soft-lock; powering line and block-36 state are known issues.
9. **Dimensions/structures:** Nether inherited; no new structure.
10. **Rendering/audio:** Texture changes; piston moving blocks; invalid block 36 air crashes when rendered.
11. **Technical/internal:** Protocol 14; artifact hashes/manifests linked on page. Patch 1.7_01 or later repairs affected worlds so they can be played again.
12. **Incomplete/experimental/unused:** Stone bricks and silverfish have text/texture but no corresponding block/mob; cobweb is functional; TNT old punch behavior only via metadata/editor.
13. **Bugs/obscure behavior:** Pushing blocks destroyed by piston can create block 36 (“air”) and crash rendering; pushing powered redstone or same powering line also crashes/soft-locks. Right-clicking sheep can crash. These are severe build-specific bugs, not intentional piston semantics.
14. **Difference from Beta 1.6:** Adds block-moving redstone mechanisms and shears; corrects clay; changes TNT, sheep and cobweb behavior.
15. **Recreation notes:** Preserve actual 1.7 move/destroy rules and invalid state only if crash-compatible; distinguish 1.7_01–1.7.3 fixes. Don't add silverfish/stone-brick gameplay from their assets alone.
16. **Sources/unresolved:** [Beta 1.7](https://minecraft.wiki/w/Java_Edition_Beta_1.7), [raw page](https://minecraft.wiki/w/Java_Edition_Beta_1.7?action=raw), [piston history](https://minecraft.wiki/w/Piston). Exact 12-block movement limit/whitelist/update ordering need code/test evidence.

#### Beta 1.8
1. **Identity/context:** Adventure Update Part I, released 14 September 2011; three prereleases and two previews preceded it. It is the final named public Beta update before separate Release 1.0.0. [Version page](https://minecraft.wiki/w/Java_Edition_Beta_1.8).
2. **New blocks/items:** Melons/seeds, vines, stone bricks and variants, infested stone/cobblestone/stone bricks, iron bars, glass panes, fence gates, huge mushroom blocks/stems, brick and stone-brick stairs/slabs, ender pearls, raw/cooked meat variants and rotten flesh. Exact item registry and survival routes remain to be checked.
3. **Changed/removed:** Food ceases direct healing and feeds hunger. Old biomes and shore beaches are removed; sand/gravel/clay disks replace some old surface behavior. Creative mode returns. When pre-1.8 chunks are opened under the new generator, their biome assignments are regenerated; this does not by itself mean the existing terrain blocks are regenerated. Exact removed inventory/ID mappings not audited.
4. **World generation:** Major generator epoch: fractal biome placement vs earlier climate system; elevation linked to biome; continent-scale land in open ocean; sea level 64→63; ocean/river/extreme-hills biomes added, rainforest/seasonal forest/savanna/shrubland/tundra removed; ravines, strongholds, villages, mineshafts, sand/gravel/clay disks added. Initial strongholds were one per world but initially only central staircase generated; prerelease reupload fixed structure generation and Pre2 raised count to three. Initial villages were broken/partial (wells, paths, lamp posts) and fixed in a reupload; even final Beta 1.8 villages have no villagers. The page notes a modulo correction moving the theoretical Far Lands beyond normal reach (about 5.39×10^16 blocks) and the vanilla world boundary changing from ±32 million to ±30 million. Exact algorithms/noise/structure spacing remain unavailable from WIP history.
5. **Mobs/entities:** Endermen, silverfish and cave spiders. Beta 1.8 villages are empty of villagers; the Villager entity appears in the Beta 1.9 prerelease sequence (Pre1 per the version page). Cave-spider spawners occur in mineshafts. Exact spawn frequencies and AI remain unknown.
6. **Player mechanics:** A 20-point food meter replaces direct food healing; foods can restore it, natural regeneration is gated at above 90% hunger, and zero hunger causes starvation. Sprint begins by double-tapping W, is unavailable at ≤6 hunger, consumes the hunger/exhaustion resource and permits ~4-block horizontal sprint jumps without increasing jump height. The PAX preview’s five-second cap/special FOV does not describe public Beta 1.8; whether public builds auto-stopped after 30 seconds is not resolved here. Exact hunger/exhaustion rates remain unverified.
7. **Combat/progression:** Critical hits and sword blocking introduced. Beta 1.8 critical hit reportedly adds a random 0–100% of weapon damage plus 2 health points, applied after enchantments (no enchantments are available in this build). Sword blocking reduces incoming damage by 50%; it also historically reduced fall damage and let players rise faster underwater until those sub-behaviors were removed in Beta 1.9 prerelease. Bows switch from rapid-fire to hold-to-charge. XP orbs/levels collect/display, but XP has no gameplay spending/use here; no enchanting or End progression.
8. **Redstone/physics:** Fence gates and associated block interactions; exact circuit/tick details not audited. No End portal mechanics.
9. **Dimensions/structures:** Nether inherited. Strongholds generate but no usable End progression yet; villages and mineshafts are generated structures. Having a stronghold does not mean access to the End.
10. **Rendering/audio:** Title-menu panorama; FOV 70–110 and brightness options; new lighting engine with smoother daylight transition and artificial-light flicker; colored sunrise/sunset fog and deeper void fog/darkness/particles. Clouds rise to top of map, stop phasing through blocks and sync to game time; rain/snow render faster. Swamp grass/foliage/water darken; chest-opening animation, chest texture split from terrain atlas; many block textures change, and torch is no longer full-bright. Beta 1.8 has an inventory-block lighting defect—keep regression separate from later fix. [B1.8 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.8?action=raw).
11. **Technical/internal:** Protocol 17; debug screen exposes world seed. Loading pre-1.8 chunks under this generator assigns new biome regions without evidence that the terrain itself is regenerated. Worldgen discontinuity invalidates same-seed terrain assumptions.
12. **Incomplete/experimental/unused:** XP meter exists without spending system; generated villages are empty of villagers; End, enchantment and brewing progression absent.
13. **Bugs/obscure behavior:** Pre-release 2 reduces mineshaft density but raises structure frequency about 2.5×; raises stronghold count from 1 to 3; changes cave-spider poison on Normal/Easy and adds squid XP. Beta 1.8.1 changes mineshaft support beams from planks to fences. These are prerelease/patch-specific deltas. [Development versions](https://minecraft.wiki/w/Java_Edition_Beta_1.8/Development_versions), [1.8.1](https://minecraft.wiki/w/Java_Edition_Beta_1.8.1).
14. **Difference from Beta 1.7:** Full terrain/biome discontinuity, generated structures, hunger/sprint/XP and Adventure mobs/items; not just a content patch.
15. **Recreation notes:** Must use a separate terrain generator; XP state is non-spendable; sprint input/exhaustion and food gate differ from current implementations. Don't add trading to non-interactive villagers.
16. **Sources/unresolved:** [Beta 1.8](https://minecraft.wiki/w/Java_Edition_Beta_1.8), [dev chronology](https://minecraft.wiki/w/Java_Edition_Beta_1.8/Development_versions), [pre-release](https://minecraft.wiki/w/Java_Edition_Beta_1.8_Pre-release), [reupload](https://minecraft.wiki/w/Java_Edition_Beta_1.8_Pre-release_(reupload)), [Pre2](https://minecraft.wiki/w/Java_Edition_Beta_1.8_Pre-release_2_%3B%29), [1.8.1](https://minecraft.wiki/w/Java_Edition_Beta_1.8.1), [Food](https://minecraft.wiki/w/Food), [Experience](https://minecraft.wiki/w/Experience), [Sprinting](https://minecraft.wiki/w/Sprinting), [World generation](https://minecraft.wiki/w/World_generation/History). Exact item list/obtainability, XP curve, regen timing, combat and AI, redstone, visuals and 1.8.1 differences remain open.

### Beta 1.9 development prereleases (six separate builds)

#### Beta 1.9 Prerelease 1 (version page title: “Beta 1.9 Prerelease”)
1. **Identity/context:** First prerelease; page deliberately has no numeral in title. Published 22 September 2011; [Jeb announcement](https://twitter.com/jeb_/status/116880362793222144), [version page](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease).
2. **New blocks/items:** Nether bricks, stairs and fences; mycelium, lily pads; blaze rods, ghast tears, gold nuggets and Nether wart.
3. **Changed/removed:** Lily pads were visually unnamed/grayscale and non-solid; mycelium generated but was not ordinarily obtainable. Rod/tear/wart had no use yet. Snowfall in the Beta 1.9 prerelease temporarily destroys redstone, torches, saplings and rails; Pre5 fixes this regression (do not carry it into Release 1.0). These “present but inert” states are distinct from the eventual brewing/progression system.
4. **World generation:** Nether fortresses and mushroom-island biomes appear; Ice Plains, Ice Mountains, Frozen Ocean and Frozen River are added, but Ice Mountains is inaccessible in this build. Landmass scale is reduced by about half and ocean height variation 0.5→0.4 per build notes. Swamp tint changes. Biome/generation details are from Wiki build history; exact RNG/noise implementation not established. [Pre1 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease?action=raw).
5. **Mobs/entities:** Blaze, magma cube, mooshroom, snow golem and villagers appear. Blazes have fortress spawners. Mushroom islands are hostile-mob-free according to build notes. Exact spawn rates/AI are not resolved.
6. **Player mechanics:** An erroneous jump event grants 1–100 XP; auto-repeat jump was removed. XP dropped on player death is capped in this build, though the cap is not established here. Treat XP-on-jump as a bug, not intended XP earning.
7. **Combat/progression:** Armor effectiveness/durability behavior changed: reported armor no longer reduces fire/explosion/projectile damage, durability increased, and armor no longer loses effectiveness as durability falls. Exact formula needs artifact test.
8. **Redstone/physics:** Lily-pad collision/placement behavior is defective; specifics need testing.
9. **Dimensions/structures:** Nether fortress enters world generation with two blaze spawners; Nether materials are present. No End/enchanting/brewing progression yet.
10. **Rendering/audio:** Lily-pad missing/incorrect name appearance; swamp tint.
11. **Technical/internal:** Performance improvements are reported; exact implementation not researched.
12. **Incomplete/experimental/unused:** Nether wart, blaze rod, ghast tear no functionality; generated mycelium not harvestable; several biome variants inaccessible.
13. **Bugs/obscure behavior:** XP from jumping and disabled auto-repeat; Pre2 removes the XP bug and restores held-jump auto-repeat.
14. **Difference from Beta 1.8:** Adds Nether fortress progression pieces and mobs/biomes but leaves key drops inert; no completed End/enchanting/brewing systems.
15. **Recreation notes:** Preserve inert inventory entries and exact accidental XP. Do not project Pre2/Release crafting onto this build.
16. **Sources/unresolved:** [Pre1 page](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease), [Jeb tweet](https://twitter.com/jeb_/status/116880362793222144), [weather history](https://minecraft.wiki/w/Weather?action=raw). Specific combat formula and generator code need original-build validation.

#### Beta 1.9 Prerelease 2
1. **Identity/context:** Second prerelease, 29 September 2011. [Jeb announcement](https://twitter.com/jeb_/status/119395358135693312), [version page](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_2).
2. **New blocks/items:** Cauldron, glass bottles, spider eyes, blaze powder, fermented spider eye, magma cream; potions/effect system; nine new disc tracks/items: 11, blocks, chirp, far, mall, mellohi, stal, strad, ward. Ward was Creative-only. Added Hardcore mode.
3. **Changed/removed:** Cauldron code existed without assigned block ID, so was not ordinarily placeable/obtainable. Experimental cauldron could hold three bottles, fill from water bucket and mix ingredients; many combinations had negative/no-op effects. Same damaged tools/weapons could be combined in crafting to repair. Milk clears status effects. Spider eye was food/ingredient and could poison when eaten. Fermented eye/magma cream recipes became available.
4. **World generation:** No new terrain/structure milestone identified in build notes. Nether progression exists but does not imply these items were usable there.
5. **Mobs/entities:** Animal breeding using wheat added for cows, mooshrooms, sheep, pigs and chickens. In this build offspring are born fully grown; Pre3 later changes newborns to baby variants. Multiplayer mooshroom issue fixed; exact offspring traits require additional verification.
6. **Player mechanics:** Hunger/effect UI icons added. Hardcore locks difficulty to Hard and uses permadeath. Milk removes potion effects.
7. **Combat/progression:** Speed/effect suite appears; invisibility effect is a no-op. Potion system is present, but distinguish usable effects from inert status-effect entries.
8. **Redstone/physics:** No identified redstone change.
9. **Dimensions/structures:** Nether forts persist from Pre1; Nether wart/rod/tear use remains not the later complete progression.
10. **Rendering/audio:** Tooltip borders, third-person front view; nine discs introduced though Ward Creative-only. Exact music playback behavior not fully researched.
11. **Technical/internal:** Cauldron lacks ID. XP-on-jump debug bug removed; held-jump auto-repeat restored.
12. **Incomplete/experimental/unused:** Cauldron experimental, no block ID; invisibility is a no-op; many brew combinations negative/no-op; Ward disc inaccessible in survival.
13. **Bugs/obscure behavior:** Some potion formulas produce unintended negative/no-op effects; cauldron's internal implementation is not reflected in registry/obtainability.
14. **Difference from Pre1:** Brewing prototype, potion effects, breeding and Hardcore; fixes jump XP and jump-repeat regression.
15. **Recreation notes:** Model code presence separately from assigned ID and survival access. Include Pre2-specific potion recipe anomalies and effect no-ops.
16. **Sources/unresolved:** [Pre2 page](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_2), [Jeb tweet](https://twitter.com/jeb_/status/119395358135693312). Exact potion effect table and bug sub-builds should be verified.

#### Beta 1.9 Prerelease 3
1. **Identity/context:** Released 6 October 2011 as the third Beta 1.9 prerelease for Release 1.0.0; page lists protocol 19 and two client artifacts that day. [Build page](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_3).
2. **New blocks/items:** Brewing stand, enchantment table, End portal frame/block, eye of ender, cauldron, potions and baby-animal variants; enchantment UI/glint (numeric XP levels arrive in Pre4).
3. **Changed/removed:** Cauldrons replaced as potion machinery by brewing stands, becoming water-holding blocks. Brewing is listed at 30 seconds and duplicate potion combinations removed. Enchantment table UI has three Standard Galactic Alphabet choices and documented 15-level cap, but no enchantment effects.
4. **World generation:** Stronghold End portal rooms added; glass locator pillars remain. A multiplayer world-generation bug creates Sky-dimension chunks and causes biome results to differ from singleplayer; fixed in Pre4.
5. **Mobs/entities:** Baby animal variants become present. Other mob AI/stat changes not audited here.
6. **Player mechanics:** XP bar remains visible and brewing is available in its early form; the numeric XP level is not displayed until Pre4. Exact potion recipes and survival acquisition route not fully checked.
7. **Combat/progression:** Enchantment table is a visual/UI feature only; **no enchantments exist yet**. Eye/frame and portal assets do not create End travel in this build.
8. **Redstone/physics:** Not audited beyond the documented worldgen/network issue.
9. **Dimensions/structures:** Stronghold portal room generated; portal frames can be repaired with eyes but page says there is no further effect. End portal block is nonfunctional; End remains inaccessible.
10. **Rendering/audio:** Item glint and “Enchanted!!” presentation added; exact rendering/audio changes not complete.
11. **Technical/internal:** Same-day original and reuploaded client; server was not reuploaded. Protocol 19. Page documents version-string and network/worldgen-related issues.
12. **Incomplete/experimental/unused:** Enchantment table has no effects; End portal block/frame assets are not yet functional; glass stronghold locator pillars remain.
13. **Bugs/obscure behavior:** Original client briefly placed stacks of debug blocks (enchantment tables, bookshelves, End frames and End portal blocks) in hotbar; later same-day reupload removes them. Multiplayer sky-dimension chunk generation differs from singleplayer.
14. **Difference from Pre2:** Brewing stand, enchantment-table UI, portal room/frames and baby animals; still no active enchantments or End access.
15. **Recreation notes:** Keep original/reupload debug hotbar distinction; do not convert the interface, potion ingredients or portal-room generation into functional enchanting or End progression.
16. **Sources/unresolved:** [Pre3 page](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_3), [Enchanting history](https://minecraft.wiki/w/Enchanting), [Brewing history](https://minecraft.wiki/w/Brewing), [End portal history](https://minecraft.wiki/w/End_portal). Exact recipes/IDs, survival routes, all mob changes and full worldgen delta need research.

#### Beta 1.9 Prerelease 4
1. **Identity/context:** Published 13 October 2011. Same-day version-string correction/reuploads documented; contemporaneous announcements: [Jeb tweet](https://twitter.com/jeb_/status/124491774398963712), [correction](https://twitter.com/jeb_/status/124493429781704705), [reupload note](https://twitter.com/jeb_/status/124493891431960576); [version page](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_4).
2. **New blocks/items:** End stone; potion of regeneration and splash potions (gunpowder makes potions throwable); Ender crystals/entity infrastructure. No full item list reproduced here.
3. **Changed/removed:** Enchanting becomes operational, with 17 enchantments; table max level raised to 50 and bookshelf arrangement/power required (up to 30 bookshelves for maximum). Hoes, flint-and-steel and shears removed from table-eligible items. Healing potion recipe changed to glistering melon; fermented spider eye can convert healing to harming. Eye of ender right-click tracks strongholds. Dragon entity code exists but does not spawn / is not yet available; do not call the boss fight implemented.
4. **World generation:** End dimension now generated/accessed; obsidian pillars in End. Removes the stronghold glass locator pillars. Fixes Pre3 multiplayer sky-dimension chunk bug; End replaces/removes the Sky dimension. No claim of a new Overworld generator.
5. **Mobs/entities:** Endermen can spawn in End; spiders/cave spiders immune to poison and categorized as arthropods. Endermen stop burning in daylight and teleport to shade. Dragon code is present but no spawn/fight.
6. **Player mechanics:** Functional enchantment choices consume XP levels; eye-of-ender targeting adds exploration route. Splash potion changes throw/use mechanics.
7. **Combat/progression:** Enchantments actually apply; End accessible and portal functional, but without a spawning dragon it is not yet the final boss progression.
8. **Redstone/physics:** Notable fixes include enchanting being server-side (previously client-only), multiplayer critical-hit particles, glass pillars and furnace minecart bugs; exact remaining circuits not audited.
9. **Dimensions/structures:** End reachable by active portal, with central obsidian platform and pillars; stronghold portal frames/eyes now work. Dragon not normally available.
10. **Rendering/audio:** Moon phases; sun/moon cardinal direction and round textures. Exact audio not reviewed.
11. **Technical/internal:** Server-side enchanting correction; multiple client/server reuploads because version string failed to update.
12. **Incomplete/experimental/unused:** Ender dragon implementation exists but is unfinished/inaccessible; no boss encounter.
13. **Bugs/obscure behavior:** Fixes Pre3 sky chunks; two or more reuploads mean “Pre4” can name different executable artifacts.
14. **Difference from Pre3:** Portal/End functionality and operational enchantments; Pre3's inert portal/table state is not inherited. Dragon remains absent.
15. **Recreation notes:** Separate functional End world and absent dragon; pin exact reupload. Implement 17 enchantments and bookshelf/level requirements, not modern enchantment registry.
16. **Sources/unresolved:** [Pre4 page](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_4), [Jeb release/correction tweets](https://twitter.com/jeb_/status/124491774398963712), [correction](https://twitter.com/jeb_/status/124493429781704705), [reupload note](https://twitter.com/jeb_/status/124493891431960576). Verify exact enchantment calculations and artifact-specific deltas.

#### Beta 1.9 Prerelease 5
1. **Identity/context:** Published 27 October 2011; [Jeb announcement](https://twitter.com/jeb_/status/129568296600219648), [version page](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_5).
2. **New blocks/items:** Ten achievements, including brewing, Nether, locate End and kill Dragon; `/toggledownfall` and `/xp` commands. Note dragon-kill achievement existed before the dragon was available.
3. **Changed/removed:** Hostile mobs become neutral toward players in Creative (attack range reduced); slime spawn height increases from Y=0–16 to 0–40. Silk Touch no longer obtains spawners/lit furnaces and becomes incompatible with Fortune. Water downward flow begins creating source blocks (temporary, reversed in Pre6). Many block textures/names/selection boxes changed; clouds toggle added. Snowfall no longer destroys redstone, torches, saplings and rails, fixing a prerelease weather regression. [Weather history](https://minecraft.wiki/w/Weather?action=raw).
4. **World generation:** No significant new terrain/structure reported. Nether, strongholds, End persist from earlier builds.
5. **Mobs/entities:** Ender Dragon death animation and test health 1 implemented, but it still does not spawn. Slime spawning range expands. Multiplayer breeding now produces babies rather than adults.
6. **Player mechanics:** Command support includes XP command and weather toggle; Creative hostility behavior changed.
7. **Combat/progression:** Achievement system appears; achievement anticipates unavailable dragon kill. Enchanted swords can block (per build notes); exact damage remains unverified.
8. **Redstone/physics:** Downward water source creation is a temporary physics change. Stair lighting and selection boxes fixed/adjusted.
9. **Dimensions/structures:** No major terrain addition; End dragon remains unavailable; achievements can reference it regardless.
10. **Rendering/audio:** Block texture/name updates; clouds can be toggled.
11. **Technical/internal:** New commands; an entity with test health and death animation can exist internally without normal spawn.
12. **Incomplete/experimental/unused:** Dragon achievement and animation exist but no natural boss. Water behavior experiment does not persist to next prerelease.
13. **Bugs/obscure behavior:** Mycelium and End stone become visible in Creative inventory; Silk Touch no longer provides certain special blocks; water source behavior rolled back next build.
14. **Difference from Pre4:** Achievement/command/Creative and mob updates, but no dragon progression.
15. **Recreation notes:** Keep kill-Dragon achievement available but boss absent; implement changed water source propagation only for Pre5.
16. **Sources/unresolved:** [Pre5 page](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_5), [Jeb tweet](https://twitter.com/jeb_/status/129568296600219648). Full achievement list and block texture/selection details need per-page audit.

#### Beta 1.9 Prerelease 6
1. **Identity/context:** Published 11 November 2011, final prerelease before Release 1.0; [Jeb announcement](https://twitter.com/jeb_/status/135018767620112385), [version page](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_6). It remains a prerelease, not Release 1.0.
2. **New blocks/items:** End crystals and the dragon egg become part of a working boss completion path; existing end content is now completable.
3. **Changed/removed:** Ender Dragon spawns; End crystals on obsidian pillars heal it; boss bar added. Killing dragon opens exit portal, dragon egg appears and credits/End poem become reachable. Egg teleports when clicked/mined; torch/non-full-block trick can collect it. Water downward flow is reverted to flowing behavior rather than Pre5 source creation. Tool/block drops and mining strengths change; efficiency works on all blocks; stairs self-drop; End-related and ore smelting/silk touch rules adjusted.
4. **World generation:** No major new terrain boundary; End pillar/crystal boss arena functions. Pre-existing End structures are now tied into boss progression.
5. **Mobs/entities:** First version in this prerelease sequence with a spawning/killable dragon; End crystals heal it. Baby sheep inherit dye. Boss behavior details beyond healing and completion require separate testing.
6. **Player mechanics:** Mining/tool strengths and durability behavior adjusted; Creative mode durability use fixed. Armor durability and item/block interaction fixes.
7. **Combat/progression:** First normally completable End boss and game-ending credits. The achievement's intended target now exists.
8. **Redstone/physics:** Repeater chunk-unload behavior fixed; furnace-cart push behavior fixed; water source experiment removed. Exact timing remain to review.
9. **Dimensions/structures:** End portal exit path and boss arena completed. Lit bedrock fire persists indefinitely in the End per build history.
10. **Rendering/audio:** Sun/moon square again; credits/poem accessible after boss.
11. **Technical/internal:** Several fixes for Creative durability, armor durability, chunk unloading and minecart pushing.
12. **Incomplete/experimental/unused:** Still a prerelease; known issue: wooden doors open only with redstone in this build (per history). No claim of Release 1.0 polish.
13. **Bugs/obscure behavior:** Dragon egg teleports on click/mining, not conventional mining; torch pickup method. Downward water returns to flowing rather than Pre5 source creation.
14. **Difference from Pre5:** Dragon genuinely spawns and game is completable; exit portal/egg/credits sequence is available; reverses temporary water change and fixes numerous drop/tool issues.
15. **Recreation notes:** This is the earliest listed build where the End boss progression works. Keep it distinct from stable 1.0, which follows a week later.
16. **Sources/unresolved:** [Pre6 page](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_6), [Jeb tweet](https://twitter.com/jeb_/status/135018767620112385). Exact dragon AI/damage/targeting and patch carry-forward should be verified against artifact.

### Minecraft Release

#### Release 1.0 (1.0.0)
1. **Identity/context:** First full release; 18 November 2011 (compiled 14 November), MINECON, Adventure Update Part II. The six Beta 1.9 prereleases preceded it but were not themselves Release 1.0. Development was initially called “Beta 1.9,” then official release named 1.0.0. Final client artifact hash is `b679fea27f2284836202e9365e13a82552092e5d`, protocol 22. Release 1.0.1 (24 Nov) is a server-only patch, not a replacement client; it fixes server login “Invalid server key”/NPE issues. [Release page](https://minecraft.wiki/w/Java_Edition_1.0.0), [contemporary release coverage](https://www.eurogamer.net/minecraft-gets-official-release-date), [Engadget launch coverage](https://www.engadget.com/2011-11-18-psa-minecraft-is-out.html).
2. **New blocks/items:** Nether bricks/stairs/fences, Nether wart, mycelium, lily pads, brewing/enchanting apparatus, bottles/potions, End portal frame/portal, End stone, dragon egg, blaze-related items, ghast tear, gold nugget, music discs/tracks and associated craftable items. Much of this set arrived during prereleases; don't claim all are first introduced here.
3. **Changed/removed:** Release page documents the final forms of brewing, enchanting and End progression; compare to Pre6 (which already has the dragon/credits) before assigning “new” labels. This is a stable full-release build, rather than a prerelease state.
4. **World generation:** Inherits Beta 1.8 Adventure generator, Nether fortresses and strongholds; End is generated/accessed with dragon progression. Exact worldgen changes between Pre6 and final jar not yet exhaustively diffed.
5. **Mobs/entities:** Ender Dragon final encounter present; blaze, magma cube, mooshroom, snow golem, villagers and Enderman systems carried forward. Detailed AI/health/spawn/drop values still need individual mob histories.
6. **Player mechanics:** XP, hunger, sprinting, Hardcore, breeding, status effects and sleep systems operational in the 1.0 era, but not yet characterized numerically here. Notably XP/enchanting are more than Beta 1.8's orb collection/display.
7. **Combat/progression:** Functional enchanting/brewing and The End. Stronghold → Eyes of Ender/portal → End → dragon → exit/credits is the progression endpoint; exact eye/frame counts and recipe/effect tables still need feature-level citations.
8. **Redstone/physics:** No major redstone milestone identified beyond inherited prior systems; exact block behavior not audited.
9. **Dimensions/structures:** Nether fortress/Nether and stronghold/End are part of progression; dragon egg and exit path. Portal access in Beta prereleases must not be mistaken for dragon availability until Pre6.
10. **Rendering/audio:** The sound update adds chest open/close, XP-orb pickup, bone-crack damage, eating/drinking, blaze breathing, Enderman-specific sounds (replacing zombie sounds), short portal-like teleport, magma-cube/silverfish sounds, arrow-landing twang and tool/armor-break animation/sounds. It also replaces firing sounds for bows/thrown items, door/trapdoor/fence-gate sounds and explosion variants via the shared asset server—separate those replacements from binary/code changes because older clients could receive assets. Music includes End/credits and nine discs; most disc tracks already appeared in prereleases. [1.0.0 raw](https://minecraft.wiki/w/Java_Edition_1.0.0?action=raw), [Sound history](https://minecraft.wiki/w/Sound?action=raw), [Music history](https://minecraft.wiki/w/Music?action=raw).
11. **Technical/internal:** Piston client metadata link and final client jar listed on [1.0.0 version page](https://minecraft.wiki/w/Java_Edition_1.0.0); protocol 22, Java 5. MINECON showcase build differs from final by one authentication/server-ID parsing line per page; do not substitute it for final.
12. **Incomplete/experimental/unused:** Release page summarizes full systems, but specific 1.0 artifacts can still contain unused entries; not yet exhaustively inspected. Pre3 debug items and inert portal/table are not the release state.
13. **Bugs/obscure behavior:** MINECON showcase jar and final jar differ; historical page reports parsing bug fix. A TCRF exact-version page was inaccessible (403) during research, and no accessible original Mojang launch changelog was located; do not overclaim change-only details from that unavailable page.
14. **Difference from Pre6:** Official stable/final release a week later, with full release status and content polish; much of End/brewing/enchanting/Hardcore/breeding was already in the prerelease sequence. Exact binary diff remains unperformed.
15. **Recreation notes:** Implement 1.0 as its own build, but preserve the fact that much advertised content already worked in Pre6. Avoid taking modern feature behavior as the 2011 final.
16. **Sources/unresolved:** [1.0.0 page](https://minecraft.wiki/w/Java_Edition_1.0.0), [version history](https://minecraft.wiki/w/Java_Edition_version_history), [Eurogamer](https://www.eurogamer.net/minecraft-gets-official-release-date), [Engadget](https://www.engadget.com/2011-11-18-psa-minecraft-is-out.html). Need exact final-build diff, sound update list, feature histories and mob technical parameters.

#### Release 1.1
1. **Identity/context:** Stable release 12 January 2012; protocol 23. [Version page](https://minecraft.wiki/w/Java_Edition_1.1), [1.1 guide](https://minecraft.wiki/w/Java_Edition_guides/1.1). Inherits Release 1.0 systems.
2. **New blocks/items:** Twenty spawn eggs (Creative-only); bow enchantments Power, Punch, Flame and Infinity.
3. **Changed/removed:** Apples now drop from oak leaves (reported 1/200). Skeleton-killed creepers drop music discs, making most prior discs except “11” survival-obtainable. Sheep regrow wool by eating grass; magma cubes can drop magma cream. Ladders lose collision. Fence gates can be activated by redstone. Pumpkin/melon stems can grow onto dirt, not only farmland.
4. **World generation:** Beaches return as a distinct beach biome, with old-style sand/sandstone shore characteristics, rather than pre-Beta 1.8’s per-column beach surface feature. New hills/edge variants include frozen ocean, desert hills, forest hills, taiga hills and extreme-hills edge. Superflat is a separate selectable type (bedrock + three dirt + grass) where villages may generate, not default terrain replacement. [1.1 raw](https://minecraft.wiki/w/Java_Edition_1.1?action=raw).
5. **Mobs/entities:** No new mobs/structures reported by the update guide; sheep and magma-cube behavior/drop changes above.
6. **Player mechanics:** Spawn eggs Creative-only, not survival-available. Four new bow enchantments extend enchanting.
7. **Combat/progression:** Power/Punch/Flame/Infinity are functional bow enchantments; exact level weights not audited.
8. **Redstone/physics:** Fence gates become redstone-operable; ladders no longer collide.
9. **Dimensions/structures:** No new dimension/structure identified; villages can spawn in Superflat.
10. **Rendering/audio:** Disc survival access changes; track-specific audio not audited.
11. **Technical/internal:** Stable protocol 23; packet-level behavior not audited.
12. **Incomplete/experimental/unused:** Spawn eggs present but Creative-restricted.
13. **Bugs/obscure behavior:** “11” disc remains unobtainable through skeleton-killed-creeper route.
14. **Difference from Release 1.0:** Superflat/beaches/bow enchantments, apple/disc survival access, gate/ladder/stem behavior.
15. **Recreation notes:** Make eggs Creative-only; include leaf apple RNG and skeleton-creeper disc condition; Superflat is a separate setting.
16. **Sources/unresolved:** [1.1](https://minecraft.wiki/w/Java_Edition_1.1), [1.1 guide](https://minecraft.wiki/w/Java_Edition_guides/1.1). Exact generator code, disc probabilities and enchantment values remain open.

#### Release 1.2 (stable baseline 1.2.1)
1. **Identity/context:** “1.2” page labels prerelease dated 29 Feb 2012; first stable release is 1.2.1 on 1 Mar. No stable 1.2.0 should be implied. [1.2 prerelease](https://minecraft.wiki/w/Java_Edition_1.2), [1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1), [guide](https://minecraft.wiki/w/Java_Edition_guides/1.2_releases).
2. **New blocks/items:** Jungle wood/log/leaves and associated biome content, redstone lamp, bottle o’ enchanting, fire charge, chiseled stone bricks and other listed additions. Emerald ore, emerald currency/trading, sandstone stairs and jungle stairs/slabs are not 1.2.1 additions; they belong to Release 1.3.1.
3. **Changed/removed:** No emerald-based villager trade economy yet; the feature arrives in Release 1.3. Spawn eggs remain Creative-only; “11” disc remains unavailable by normal skeleton-creeper route.
4. **World generation:** Jungle and Jungle Hills biomes enter in 12w03a, shifting placement of major land biomes; desert wells arrive in 12w04a and mineshaft wooden bridges in 12w05a. Caves/ravines are temporarily disabled in 12w07a and restored in 12w08a, so this does not describe stable 1.2.1. Stable 1.2.1 raises the world-height limit 128→256. This does not imply pre-existing terrain is vertically rescaled. Emerald ore is absent at this version boundary; it arrives with 1.3 development. [World-generation history](https://minecraft.wiki/w/World_generation/History), [1.2.1 raw](https://minecraft.wiki/w/Java_Edition_1.2.1?action=raw).
5. **Mobs/entities:** Ocelots/cats and iron golems; zombie sieges, villager breeding/transformation behavior. In 12w04a zombies and skeletons gain sun-avoidance pathing: if burning, seek water or shelter unless pursuing a player. Exact siege spawn conditions and AI need verification.
6. **Player mechanics:** Bottle o’ enchanting exists but is Creative-only/unavailable in ordinary survival at 1.2.1; villager trading becomes its survival source in 1.3.1. Ocelots/cats can be tamed; exact taming mechanics need feature-history confirmation.
7. **Combat/progression:** XP bottle is Creative-only; enchanting/brewing carried forward from 1.0.
8. **Redstone/physics:** Redstone lamp added; power/tick behavior not reviewed.
9. **Dimensions/structures:** Desert wells and mineshaft wooden bridges are added within the 1.2 development/release boundary; major settlement/underground systems otherwise persist from earlier versions.
10. **Rendering/audio:** Lava gains a faint rumble and large popping particles; third-person view delays head movement enough to reveal the side of the head; Nether fog moves closer; ambience begins working in multiplayer. Debug screen gains block/sky/raw brightness and biome fields. [1.2.1 raw](https://minecraft.wiki/w/Java_Edition_1.2.1?action=raw).
11. **Technical/internal:** Stable 1.2.1 protocol 28, Java 5. Server EXE unavailable although client/server JAR archived.
12. **Incomplete/experimental/unused:** Spawn eggs remain Creative-only; new features have differing routes (ore, trade, crafting, drops).
13. **Bugs/obscure behavior:** Do not conflate Beta 1.2 lapis-drop patches with Release 1.2’s height/jungle changes; several terrain boundaries occurred in snapshots before stable 1.2.1.
14. **Difference from 1.1:** Jungle biome/wood, ocelots, iron golems, zombie sieges, redstone lamp, 256-block world-height limit, desert wells and XP bottle (Creative-only).
15. **Recreation notes:** “Release 1.2” stable baseline means 1.2.1; keep emerald/trading and survival cocoa availability disabled until the 1.3.1 release boundary.
16. **Sources/unresolved:** [1.2](https://minecraft.wiki/w/Java_Edition_1.2), [1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1), [guide](https://minecraft.wiki/w/Java_Edition_guides/1.2_releases). Patch family 1.2.2–1.2.5 and trade/siege details still need audit.

#### Release 1.3 (stable baseline 1.3.1)
1. **Identity/context:** “1.3” prerelease dated 26 July; first stable 1.3.1 on 1 Aug 2012 (protocol 39, Java 5). [1.3](https://minecraft.wiki/w/Java_Edition_1.3), [1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1), [guide](https://minecraft.wiki/w/Java_Edition_guides/1.3_releases).
2. **New blocks/items:** Tripwire hook/wire; Ender chest; book and quill/written books; emeralds/emerald ore/block and villager trading; cocoa pods/beans become naturally available; sandstone stairs and wood species stairs/slabs; enchanted golden apple; desert/jungle temples with loot; optional bonus chest.
3. **Changed/removed:** Villager trading is introduced/expanded; pig drops change. Boats become faster/more responsive and break through lily pads, dropping pad instead of destroying boat.
4. **World generation:** Desert pyramids and jungle temples; Large Biomes world type (~16× biome scale), optional bonus chest and desert-village palette changes. Emerald ore generates only in Extreme Hills, at Y=4–31 inclusive and up to 8 placements per chunk in single-block veins in stable 1.3.1. Earlier 12w21a veins could reach ten blocks; 12w22a reduces to one block before stable. Mobs can spawn atop upside-down slabs/stairs. [1.3.1 raw](https://minecraft.wiki/w/Java_Edition_1.3.1?action=raw).
5. **Mobs/entities:** Villager trade changes; upside-down-slab/stair spawning applies broadly to mobs. No new mob established here.
6. **Player mechanics:** Adventure mode; Open to LAN; integrated singleplayer server; shift-click crafting. Written books craftable/readable; emerald trade loop now exists. Ore mining XP arrives in 12w22a development (mining ores grants XP), before stable 1.3.1.
7. **Combat/progression:** Emerald trading now adds a progression/economy loop; Ender chest shares player inventory across chests. Silk Touch required to harvest the Ender chest as a block; otherwise obsidian drops.
8. **Redstone/physics:** Tripwire is functional redstone input. Boat/lily-pad collision changes as above.
9. **Dimensions/structures:** Temples add loot progression; existing End retained.
10. **Rendering/audio:** Book GUI; audio not reviewed.
11. **Technical/internal:** Integrated server means singleplayer uses client/server architecture; LAN hosting/commands changed. Protocol 39.
12. **Incomplete/experimental/unused:** Bonus chest is optional; Adventure mode is not later polished map-maker feature set.
13. **Bugs/obscure behavior:** Mobs spawn on upside-down slabs/stairs. 1.3.2 is a bug/compatibility follow-up, not the same artifact.
14. **Difference from 1.2.1:** Integrated server/LAN, Adventure mode, Ender chest/tripwire/books/temples and bonus chest.
15. **Recreation notes:** Preserve shared Ender inventory and integrated server model; use historical boat-pad collision and spawn surfaces.
16. **Sources/unresolved:** [1.3](https://minecraft.wiki/w/Java_Edition_1.3), [1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1), [guide](https://minecraft.wiki/w/Java_Edition_guides/1.3_releases). Full 1.3.2 fixes and tripwire tick semantics remain.

#### Release 1.4 (stable baseline 1.4.2)
1. **Identity/context:** 1.4 prerelease on 19 Oct 2012; stable 1.4.2 on 25 Oct; Pretty Scary Update, protocol 47. [1.4](https://minecraft.wiki/w/Java_Edition_1.4), [1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2), [guide](https://minecraft.wiki/w/Java_Edition_guides/Pretty_Scary_Update).
2. **New blocks/items:** Anvil, item frame, flower pot, cobblestone/mossy walls, carrots/potatoes, carrot-on-a-stick, pumpkin pie, Nether wart block, command block, beacon, Nether Star and related items.
3. **Changed/removed:** Anvils repair/combine/enchant items; zombies can pick up items. New night-vision/invisibility potion paths and golden carrot. Command block is not craftable or normally Survival-obtainable.
4. **World generation:** Silverfish-infested monster-egg blocks generate rarely in Extreme Hills and variants from 12w38a; witch huts generate in swamps by 12w40a, with witches associated to the hut. No major terrain generator change; Nether fortresses inherited. Carrot/potato village/loot routes require exact subversion check. [1.4.2 raw](https://minecraft.wiki/w/Java_Edition_1.4.2?action=raw).
5. **Mobs/entities:** Wither, witch, bat, wither skeleton, zombie villager. Witches hostile; bats ambient; wither skeletons are fortress mobs, with 1.4.5 adding spawns in already-generated fortresses. Exact Wither values/attacks not yet checked.
6. **Player mechanics:** Carrot/potato farming, potion effects, anvil repair; difficulty can be changed in-game. Anvil XP formulas not audited.
7. **Combat/progression:** Wither summoned using soul sand and three Wither Skeleton skulls; Nether Star enables beacon, which requires mineral pyramid.
8. **Redstone/physics:** Command block executes commands but requires operator/Creative controls; exact update timing not audited.
9. **Dimensions/structures:** Witch huts are generated in swamps by the 12w40a development boundary; Nether fortresses inherited. Wither/Beacon progression is also new.
10. **Rendering/audio:** Not fully audited.
11. **Technical/internal:** Command system, operator command block and protocol 47; network details not inspected.
12. **Incomplete/experimental/unused:** Command block exists without ordinary Survival acquisition route.
13. **Bugs/obscure behavior:** Later 1.4.x pages warn that mining empty jukebox can yield invisible Air and interaction can crash/delete chunk; associate with exact affected patch, do not assume base 1.4.2.
14. **Difference from 1.3.1:** Wither/Beacon, command block, mobs, anvil, farming and decorative blocks.
15. **Recreation notes:** Separate operator/Creative-only content from Survival. Implement summon and beacon material/pyramid requirements.
16. **Sources/unresolved:** [1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2), [guide](https://minecraft.wiki/w/Java_Edition_guides/Pretty_Scary_Update). Verify exact boss values and jukebox bug patch boundary.

#### Release 1.5
1. **Identity/context:** Redstone Update, released 13 March 2013, protocol 60. [1.5](https://minecraft.wiki/w/Java_Edition_1.5), [guide](https://minecraft.wiki/w/Java_Edition_guides/Redstone_Update).
2. **New blocks/items:** Comparator, hopper, dropper, trapped chest, light/heavy weighted pressure plates, daylight sensor, redstone block, Nether quartz ore/quartz blocks/stairs/slabs, activator rail.
3. **Changed/removed:** These additions are functional components; recipe/stack/strength details need dedicated history checks.
4. **World generation:** Nether quartz ore is generated in Nether; the version history estimates ~70 ore blocks per Nether chunk on average (secondary summary, not a deterministic placement rule). No new biome/structure/mob identified. [1.5 raw](https://minecraft.wiki/w/Java_Edition_1.5?action=raw).
5. **Mobs/entities:** No new mob identified.
6. **Player mechanics:** Redstone automation functional; no player movement change identified.
7. **Combat/progression:** Quartz offers Nether resource; no boss progression change.
8. **Redstone/physics:** Hopper transfers items and is disabled by redstone power; comparator analog output reads container fullness and can compare/subtract side input; dropper ejects; daylight sensor responds to sky light and rain; weighted plates respond to item/entity weight; redstone block is pushable constant power. Exact delays, analog formula, update ordering and edge cases need technical sources; don't assume later behavior.
9. **Dimensions/structures:** Nether ore, no structure addition.
10. **Rendering/audio:** Texture packs now use individual block/item files stitched at runtime, support independent resolutions and animation metadata, special water/lava rules, vanilla fallback for missing textures, and font changes. Lighting is optimized, with more realistic slab/stair lighting; the page notes remaining defects (e.g. active furnace emits light only from front, black spots persist). [1.5 raw](https://minecraft.wiki/w/Java_Edition_1.5?action=raw).
11. **Technical/internal:** Protocol 60; 1.5.1 (21 Mar) focuses performance/bug fixes; 1.5.2 (2 May) fixes/compatibility, protocol 61. Patch-specific fixes not automatically part of 1.5 initial.
12. **Incomplete/experimental/unused:** None established for headline components; edge behavior remains under review.
13. **Bugs/obscure behavior:** Patch versions change performance; do not attribute 1.5.1/1.5.2 changes to 1.5 base.
14. **Difference from 1.4.7:** Redstone automation suite and Nether quartz material.
15. **Recreation notes:** Simulate exact redstone/game tick semantics and item transfer, not modern defaults.
16. **Sources/unresolved:** [1.5](https://minecraft.wiki/w/Java_Edition_1.5), [guide](https://minecraft.wiki/w/Java_Edition_guides/Redstone_Update). Redstone timing semantics need technical evidence.

#### Release 1.6 (stable baseline 1.6.1)
1. **Identity/context:** Horse Update prerelease 1.6 on 25 June; first stable is 1.6.1 (1 July 2013), protocol 73, Java 6 minimum. [1.6](https://minecraft.wiki/w/Java_Edition_1.6), [1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1), [guide](https://minecraft.wiki/w/Java_Edition_guides/Horse_Update).
2. **New blocks/items:** Horses/variants, horse armor, leads, name tags, carpet, hay bales, hardened/stained clay, coal block; donkey/mule chest storage.
3. **Changed/removed:** New launcher/resource-pack system; resource packs replace texture packs (format 1). Horse armor is uncraftable and loot-only; name tags are also uncraftable/loot-only. Skeleton/zombie horse variants are not normal vanilla spawns, despite their association with the horse update.
4. **World generation:** Horses spawn naturally in plains/savanna. Deserts no longer contain water lakes; this begins in 13w17a. Nether fortress chests are introduced by 13w18a; fortresses previously existed without generated chests. These add loot/content to existing structures, not new structures/biomes. [1.6.1 raw](https://minecraft.wiki/w/Java_Edition_1.6.1?action=raw).
5. **Mobs/entities:** Horses, donkeys and mules tameable/rideable; donkey/mule chest storage. Breeding/stat inheritance specialized; exact RNG/health/speed/jump ranges need verification. Skeleton/zombie horse forms are not ordinary vanilla-spawnable entities in this release (require third-party modifications per version guide), despite entity ideas/assets being associated with the horse update.
6. **Player mechanics:** Leads leash entities, name tags name them; saddle/mount controls for riding.
7. **Combat/progression:** Horse armor/mounted transport; no new boss.
8. **Redstone/physics:** No major change identified.
9. **Dimensions/structures:** Existing structures supply uncraftable items; no new dimension.
10. **Rendering/audio:** Resource packs replace texture packs and can include textures, sounds, partial fonts, language files, credits, End poem and splash texts; a missing-texture texture is added. The `playsound` command arrives in the 1.6.1 release boundary, not the prerelease. [1.6.1 raw](https://minecraft.wiki/w/Java_Edition_1.6.1?action=raw).
11. **Technical/internal:** New launcher/resource-pack system and entity/item attribute system; Release 1.6.1 raises the minimum to Java 6. Release 1.5.2 is the last release retaining Java 5 and PowerPC support. 1.6.2 has original/reupload client; 1.6.3 is a prerelease, not a stable target. 1.6.4 adds `player-idle-timeout` to server.properties and changes desert-village wells from sandstone to cobblestone/gravel.
12. **Incomplete/experimental/unused:** Do not mistake 1.6.3 prerelease for stable patch; acquisition varies for armor/saddles/name tags.
13. **Bugs/obscure behavior:** Notable exact patch bug behavior requires further review.
14. **Difference from 1.5.2:** Equine entities, mounted movement, lead/name mechanics, carpet/hay/clay/coal, launcher/resource pack transition.
15. **Recreation notes:** Initial stable is 1.6.1, not “1.6.0”; distinguish 1.6.3 prerelease and 1.6.4 stable; resource-pack behavior is versioned.
16. **Sources/unresolved:** [1.6](https://minecraft.wiki/w/Java_Edition_1.6), [1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1), [1.6.4](https://minecraft.wiki/w/Java_Edition_1.6.4), [Horse Update guide](https://minecraft.wiki/w/Java_Edition_guides/Horse_Update), [horse armor](https://minecraft.wiki/w/Horse_Armor), [name tag](https://minecraft.wiki/w/Name_Tag), [skeleton horse](https://minecraft.wiki/w/Skeleton_Horse). Exact horse parameters and patch bug behavior remain.

### Patch/build boundary register (selected deltas that affect target claims)

These are not additional target headings; they identify sub-build changes that must not be back-projected onto each target's initial artifact. Version-page histories commonly identify separate client/server artifacts; hashes establish identity only, not tested behavior. Raw wikitext links below are secondary sources and should be pinned to immutable history revisions for archival-quality citations.

#### Beta patch/sub-build deltas

| Build | Evidence-backed delta relevant to this reference | Source |
|---|---|---|
| Beta 1.0_01 / 1.0.2 | _01 fixes double-chest inventory selection, rare world-load crash, distant-chunk night lighting, but introduced singleplayer FPS drops. Beta 1.0.2 (not “1.0_02”; no Beta 1.0.1 release exists) fixes Mac OS X startup; its pre-reupload artifact was unplayable and a reupload fixed launch failure. | [1.0_01 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.0_01?action=raw), [1.0.2 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.0.2?action=raw) |
| Beta 1.2_01 | Lowers mob spawn rates, improves performance, fixes block placement/lighting/chunk-visibility issues; adds a placeholder “Skull on Fire” painting entry before its texture arrives. | [raw history](https://minecraft.wiki/w/Java_Edition_Beta_1.2_01?action=raw) |
| Beta 1.2_02 | Lapis lazuli ore drops 4–8 in singleplayer only and can generate at bedrock; item naming changes. A hidden unfinished Shift+F2 panorama screenshot feature was test code. A distinct May developer build reuses the label; do not conflate it with the January public patch. | [raw history](https://minecraft.wiki/w/Java_Edition_Beta_1.2_02?action=raw) |
| Beta 1.3 original / reuploads | Original artifact accidentally debug-spawns 64 torches, diamond tools and beds; reupload removes them. Later reupload fixes crash opening world menu when saves folder is absent. These artifacts are behaviorally distinct. | [raw history](https://minecraft.wiki/w/Java_Edition_Beta_1.3?action=raw) |
| Beta 1.3_01 | Removes Java 1.6 dependencies, fixes macOS singleplayer crash, item duplication and client/server compatibility. | [raw history](https://minecraft.wiki/w/Java_Edition_Beta_1.3_01?action=raw) |
| Beta 1.4_01 | Stops generating April Fool’s locked chests; beds’ spawn reset fix addresses the 1.4 multiplayer/“Leave bed” failure. | [raw history](https://minecraft.wiki/w/Java_Edition_Beta_1.4_01?action=raw) |
| Beta 1.5_01 / _02 | _01 fixes snow/rail/ladder/save issues and reduces wolf frequency; _02 is server-only and fixes multiplayer internal-server errors. Keep known item-pickup/crafting crash separate from stable 1.5 client. | [1.5_01 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.5_01?action=raw), [1.5_02 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.5_02?action=raw) |
| Beta 1.6.1–1.6.6 | Rapid patch train: _1 fixes infinite pickup animation; _2 fixes damaged-equipment duplication; _3 changes safe water-fall depth to ≥3 blocks and fixes lighting/server CPU/furnace issues; _4 attempts CPU fix; _5 alters sneaking/climbing/shift-crafting and performance options; _6 makes glowstone pickaxe-minable/support blocks and craftable back from dust, makes wool from string, and adds bonemeal grass/flower use. | [1.6.1](https://minecraft.wiki/w/Java_Edition_Beta_1.6.1?action=raw), [1.6.2](https://minecraft.wiki/w/Java_Edition_Beta_1.6.2?action=raw), [1.6.3](https://minecraft.wiki/w/Java_Edition_Beta_1.6.3?action=raw), [1.6.4](https://minecraft.wiki/w/Java_Edition_Beta_1.6.4?action=raw), [1.6.5](https://minecraft.wiki/w/Java_Edition_Beta_1.6.5?action=raw), [1.6.6](https://minecraft.wiki/w/Java_Edition_Beta_1.6.6?action=raw) |
| Beta 1.7_01 / 1.7.2 / 1.7.3 | _01 makes worlds broken by invalid piston block 36 loadable and fixes empty-hand sheep crash; 1.7.2 fixes piston disappearance/MP crashes; 1.7.3 addresses piston/redstone-torch duplication and related sign/painting/rail bugs. 1.7.1 was not a release. | [1.7_01](https://minecraft.wiki/w/Java_Edition_Beta_1.7_01?action=raw), [1.7.2](https://minecraft.wiki/w/Java_Edition_Beta_1.7.2?action=raw), [1.7.3](https://minecraft.wiki/w/Java_Edition_Beta_1.7.3?action=raw) |
| Beta 1.8 prerelease / reupload / Pre2 / 1.8.1 | Original leaked Pre1 had debug keys, broken village/stronghold generation, corrupted models and save issues; reupload fixes these. Pre2 lowers mineshaft density while increasing structure frequency ~2.5× and sets three strongholds/world. Stable 1.8.1 changes mineshaft support beams from planks to fences. | [Pre1](https://minecraft.wiki/w/Java_Edition_Beta_1.8_Pre-release?action=raw), [reupload](https://minecraft.wiki/w/Java_Edition_Beta_1.8_Pre-release_(reupload)?action=raw), [Pre2](https://minecraft.wiki/w/Java_Edition_Beta_1.8_Pre-release_2_%3B%29?action=raw), [1.8.1](https://minecraft.wiki/w/Java_Edition_Beta_1.8.1?action=raw) |

#### Release patch/sub-build deltas

| Build | Evidence-backed delta relevant to this reference | Source |
|---|---|---|
| Release 1.0.1 | Server-only patch for server login-key/NPE issues; does not replace client 1.0.0. No 1.0.2+ stable release branch identified in the version history. | [1.0.1 raw](https://minecraft.wiki/w/Java_Edition_1.0.1?action=raw) |
| Release 1.1 | No subsequent minor 1.1.x patch in the version history; do not confuse with rare Alpha v1.1.1. | [1.1 raw](https://minecraft.wiki/w/Java_Edition_1.1?action=raw) |
| Release 1.2.2–1.2.5 | 1.2.2 repairs natural tree growth and map snow rendering; 1.2.3 fixes Y=255 lighting, mob sunburn and sapling growth above Y=127; 1.2.4 adds birch/spruce/jungle planks, sandstone variants, Silk Touch glass and UI fixes; 1.2.5 adds inventory/pick-block changes and fixes village/ocelot/mooshroom/TNT/crash bugs. These deltas are not all part of initial 1.2.1. | [1.2.2](https://minecraft.wiki/w/Java_Edition_1.2.2?action=raw), [1.2.3](https://minecraft.wiki/w/Java_Edition_1.2.3?action=raw), [1.2.4](https://minecraft.wiki/w/Java_Edition_1.2.4?action=raw), [1.2.5](https://minecraft.wiki/w/Java_Edition_1.2.5?action=raw) |
| Release 1.3.2 | Fixes Nether re-entry falling below world, corner-water damage, uncommon crashes and missing fall damage; also changes large-oak generation and F1 server-IP display. | [1.3.2 raw](https://minecraft.wiki/w/Java_Edition_1.3.2?action=raw) |
| Release 1.4.3 prerelease / 1.4.4–1.4.7 | 1.4.3 is a prerelease, not a stable patch; it adds the wait disc and hitboxes, and contains dangerous jukebox/chunk-corruption bug. 1.4.4 adds baby mobs via spawn eggs and other fixes; 1.4.5 enables wither skeletons in existing fortresses and multiplayer map markers; 1.4.6 adds fireworks/enchanted books/Thorns; 1.4.7 is fixes only. Keep unstable prerelease behavior separate from stable versions. | [1.4.3](https://minecraft.wiki/w/Java_Edition_1.4.3?action=raw), [1.4.4](https://minecraft.wiki/w/Java_Edition_1.4.4?action=raw), [1.4.5](https://minecraft.wiki/w/Java_Edition_1.4.5?action=raw), [1.4.6](https://minecraft.wiki/w/Java_Edition_1.4.6?action=raw), [1.4.7](https://minecraft.wiki/w/Java_Edition_1.4.7?action=raw) |
| Release 1.5.1 / 1.5.2 | 1.5.1 restores performance toward 1.4 and enables hoppers to pull empty buckets from furnace fuel slots; 1.5.2 adds performance/compatibility fixes, changes Enderman aggro behavior under environmental damage, and fixes beds/spawn/rendering. Java 5 and PowerPC remain supported through 1.5.2; minimum Java 6 begins at 1.6. | [1.5.1](https://minecraft.wiki/w/Java_Edition_1.5.1?action=raw), [1.5.2](https://minecraft.wiki/w/Java_Edition_1.5.2?action=raw) |
| Release 1.6.2–1.6.4 | 1.6.2 adds natural baby zombie/zombie pigman spawns and fixes horse/lead/rendering bugs. 1.6.3 is a prerelease, not a stable target; 1.6.4 changes desert-village well materials and adds `player-idle-timeout`. No seed or 32-bit chunk-coordinate change is established. | [1.6.2](https://minecraft.wiki/w/Java_Edition_1.6.2?action=raw), [1.6.3](https://minecraft.wiki/w/Java_Edition_1.6.3?action=raw), [1.6.4](https://minecraft.wiki/w/Java_Edition_1.6.4?action=raw) |

## Part IV — Cross-version mechanic evolution

### Terrain generation

Several non-interchangeable generator epochs occur in the requested range. **Alpha v1.0.1–pre-v1.2.0:** pre-biome/Halloween generator; the historical summary places worlds at 128 blocks tall with sea level Y=64, and caves, ore blobs, springs and dungeons predate the target range. **Alpha v1.2.0:** a major terrain and biome discontinuity—biomes are introduced, the history labels terrain change “great,” depth-noise scale changes 100→200, monoliths cease, and the bottom layer becomes all bedrock. Same seed does not preserve terrain across this boundary.

**Beta 1.0–1.7.x:** climate-era generator persists; the history describes Alpha 1.2.0–Beta 1.7.3 as broadly seed-compatible, with population changes and target-specific defects. Beta 1.0 lava-lake and biome-boundary defects; Beta 1.2 vegetation/ores, with 1.2_02 lapis changes; Beta 1.3 sandstone and biome-border fix. Public Beta 1.6 fixes an ore quadrant bias (prior southeast ore abundance reported ≥50% above northwest); the distinct unreleased Test Build 3 has vegetation changes and a clay-coordinate bug (`x=z`), fixed in public Beta 1.7. The Beta 1.2 statement that caves contain more coal/iron/diamond is explicitly ambiguous (could be blob size or frequency).

**Beta 1.8 onward:** a major overhaul: continental land in broad oceans; elevation controlled by biome rather than climate; fractal biome placement replaces the prior climate system; sea level drops 64→63. Ocean/river/extreme-hills biomes and ravines, strongholds, villages and mineshafts arrive; old biomes and beaches are removed, with sand/gravel/clay disks replacing some shore patterns. The publicly documented [Beta 1.8 GenLayer sequence](https://raw.githubusercontent.com/PhoenixVX/golden-age-wiki/main/minecraft/genlayer/sequence/b18.md) describes an island seed → fuzzy zoom → repeated island/zoom → separate river and biome/temperature/downfall branches → river mix → Voronoi zoom architecture. Treat exact salts in that community reconstruction as **medium-confidence secondary implementation evidence**, not Mojang-source proof.

**Height/seed caveats:** Beta 1.8 lowers sea level, but the base world height remains 128 in the Beta 1.8 range. Release 1.2.1 increases the world-height limit 128→256; that change alone does not prove old terrain is vertically rescaled. [Java 1.2.1 page](https://minecraft.wiki/w/Java_Edition_1.2.1). A contemporaneous [Jeb/Mojang explanation (23 Feb 2011)](https://web.archive.org/web/20110225221214/http://mojang.com/2011/02/23/a-short-demystification-of-the-map-seed/) identifies Beta 1.3 as the selectable map-seed boundary: blank input uses the computer clock, text strings use Java `String.hashCode()`, and the map seed is used only for world generation (not spawn or mob drops). It says equal seeds generate identical maps, but this is a within-version determinism claim, not cross-version terrain identity. The `pack.png` seed `3257840388504953787` is separately reported by the secondary [World seed history](https://minecraft.wiki/w/World_seed) to reproduce Alpha v1.2.0–1.2.5 and through Beta 1.7.3 with minor population differences. The exact numeric-string parse width and algorithm/RNG changes per generator epoch still need direct artifact evidence. Beta 1.8 also regenerates biome assignments when opening pre-1.8 chunks because biome data had not been stored; this is not proof that terrain blocks are regenerated.

The [generation-history article](https://minecraft.wiki/w/World_generation/History) explicitly calls itself WIP and says Beta 1.8's exact changes are not exhaustively documented. Exact noise classes/constants, seed mixing, RNG call ordering, cave/ravine parameters, ore tries/heights/vein sizes, structure spacing, and population order remain unresolved; do not import modern numbers.

### Biomes and surface blocks

Before Alpha 1.2.0, no biome system is documented for the targets. The Alpha/Beta-pre-1.8 model derives temperature/rainfall from noise with smooth transitions; terrain height is independent of biome, while rainfall affects height variation. Biome color can depend continuously on temperature/humidity rather than a discrete color, and watercolor texture behavior was disabled (medium-confidence secondary history; exact implementation needs artifact/code validation). Before Beta 1.8, oceans are terrain-defined rather than a distinct ocean biome, and beaches are surface features that may occur in any biome: sand/gravel choice and top-block replacement depend on X/Z and sea-level height; sand can overlie sandstone, gravel beaches may expose caves. Beta 1.8 replaces this with fractal-biome regions and biome-controlled elevation; old biomes and beaches are removed and oceans/rivers become biomes. Release 1.1 reintroduces beaches as distinct biomes, not the pre-1.8 surface feature. Beta-era updates adjust tree species, vegetation patches and sandstone layering; Beta 1.0’s desert boundary defect is fixed in Beta 1.3. Sources: [pre-1.8 biome history](https://minecraft.wiki/w/Biome/Before_Beta_1.8?action=raw) (secondary/outdated), [Beta 1.8 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.8?action=raw), [Release 1.1 raw](https://minecraft.wiki/w/Java_Edition_1.1?action=raw), [world-generation history](https://minecraft.wiki/w/World_generation/History). Exact climate/noise, color lookup, snow placement and biome IDs require further research.

### Structures and caves

Dungeons predate the requested Alpha builds; dungeon frequency doubles in Alpha v1.0.14. Beta 1.8 adds strongholds, villages, mineshafts and ravines; Beta 1.9 prereleases alter the stronghold portal room and associated progression. Structures can include blocks unavailable as player-crafted items, and a registered block may be survival unobtainable. Exact placement criteria and generation order remain to be verified. Caves existed before Alpha and remain through the Beta 1.8 overhaul; detailed cave algorithm and density changes have not yet been established. Ravines are a new Beta 1.8 carver, not a synonym for caves.

### Ores

Alpha v1.0.1 marks redstone ore worldgen and a reported coal blob-size change (source marks the latter for verification). Beta 1.2 adds lapis ore; 1.2_02 apparently changes its abundance and permits bedrock-level generation. Public Beta 1.6 fixes coordinate-dependent ore frequency: prior builds reportedly generated some ores at least 50% more often in southeast than northwest. Keep this distinct from the separate 1.6 Test Build 3 clay/vegetation behavior. No modern uniform ore distributions should be substituted. Numerical vein sizes/tries/height ranges are pending technical source evidence.

### Mob spawning and AI

Historical entities must be modeled as build-specific behaviors, not modern registry entries. The prerelease sequence adds a particularly diverse set in Beta 1.9 Pre1 (blaze, magma cube, mooshroom, snow golem, villagers), with fortress blaze spawners and hostile-free mushroom-island behavior reported there. Enderman health and daylight behavior change across later prereleases (Pre3 doubles health 20→40; Pre4 they stop burning and seek shade), cave spiders/spiders are poison-immune/arthropods in Pre4, and baby animal variants become available during this sequence. Wolves are added in Beta 1.4; Beta 1.8 adds endermen, silverfish and cave spiders; its villages are initially empty of villagers, whose entity appears in the Beta 1.9 prerelease sequence and trade system arrives in Release 1.3. Release 1.2 adds ocelots/cats and iron golems; Release 1.4 adds Wither, witches, bats, wither skeletons and zombie villagers. Exact spawn caps, light thresholds, AI state machines, pathfinding, despawn and many drop tables remain unverified. The source pages are per-build records, not proof that modern AI or spawn conditions apply.

### Combat and movement

There is no modern attack-strength cooldown anywhere in this target range. Beta 1.6 removes the separate 10-tick missed-swing cooldown; do not conflate these mechanics. Beta 1.8 adds falling critical hits, sprint knockback, sword blocking (reported 50% damage reduction), and hold-to-charge bows; critical-hit math and legacy armor equations need build-specific evidence. Pre1 changes armor durability/reduction and excludes some explosion/fire/projectile damage from base armor, while Pre4 introduces Protection-type enchantments. Pre3 reduces sword damage (stone 6→5 HP, iron 8→6, diamond 10→7 per the 1.0.0 comparison table). Release 1.0 fully charged bows deal 9 HP, rarely 10, and have 385 durability; Creative does not require arrows. Release 1.1 adds Power/Punch/Flame/Infinity bow enchantments. Sprint starts with double-tap-forward and is hunger-gated at ≤6; its PAX five-second limit is preview-only, while exact public timeout details are unresolved. Horse riding adds mounted movement in Release 1.6. Exact critical/armor/knockback formulas, tool damage, and several per-build constants remain open. Sources: [Damage](https://minecraft.wiki/w/Damage?action=raw), [Melee attack](https://minecraft.wiki/w/Melee_attack?action=raw), [Blocking](https://minecraft.wiki/w/Blocking?action=raw), [Sprinting](https://minecraft.wiki/w/Sprinting?action=raw), [Bow](https://minecraft.wiki/w/Bow?action=raw), [Release 1.0.0](https://minecraft.wiki/w/Java_Edition_1.0.0?action=raw).

### Hunger, health, and food

Before Beta 1.8, foods restore health directly rather than filling a hunger meter. Target-era milestones include Alpha 1.2 raw/cooked fish; Beta 1.2 cake (3 health per slice, 18 for the whole cake); Beta 1.4 cookies (1 health); and Beta 1.5 cooked pork drops when a pig burns. Beta 1.8 changes food to a 20-point hunger bar, adds new foods and permits 1 health regeneration every four seconds above 90% food; at zero, starvation removes 1 health every four seconds. The difficulty thresholds reported for starvation are Easy 10 HP, Normal 1 HP, Hard can kill. Cake’s Beta 1.8 slice value conflicts across secondary summaries (the version page reports 3 hunger per slice; Food history reports 2), so leave this disputed pending exact-build test. Hunger/saturation/exhaustion behavior should not be treated as today’s complete formula: exact per-action costs and all tick constants remain unresolved. Sprinting is unavailable at ≤6 food. Release 1.1 changes golden apples from the Beta 1.8 10-hunger/30-second regeneration form and gold-block recipe to a 4-hunger/4-second regeneration form with gold nuggets. [Beta 1.8 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.8?action=raw), [Food history](https://minecraft.wiki/w/Food?action=raw), [Jeb on starvation thresholds](https://twitter.com/jeb_/status/111047534318858240).

### Experience

Beta 1.8 adds collectible XP orbs and a bar but no XP consumer; death can create a very large orb pile and performance hazard. Pre1 introduces a temporary 1–100 XP-per-jump bug and caps XP dropped on death. Pre2 removes jump XP and fixes the death-screen score’s literal `&e0` rendering. Pre3 has enchanting UI/glint but no actual enchantments or functional XP spending; it does not yet display the numeric level. Pre4 is the boundary for numeric XP levels and spending levels on enchanting, with rising costs per level. Release 1.0 inherits this system. XP curves and death retention are version-dependent; do not substitute modern values. [Experience history](https://minecraft.wiki/w/Experience?action=raw), [Beta 1.8 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.8?action=raw), [Pre1](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease?action=raw), [Pre2](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_2?action=raw), [Pre4](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_4?action=raw).

### Enchanting and brewing

There is no enchanting in Beta 1.8, despite its XP meter. Beta 1.9 Pre2 contains a cauldron-based potion prototype whose code has no ordinary block ID; Pre3 assigns the cauldron an ID as a water container and switches brewing to a stand. Pre3 has a working 30-second stand and a reduced set of 25 potions/31 combinations, but its enchantment table is only UI/glint with no actual enchantments or functional XP spending. Pre4 implements 17 enchantments, changes the max displayed level to 50 and uses up to 30 bookshelves to attain it; this is the first effective enchanting system in the prerelease run. Pre4 adjusts healing/harming recipes and splash potions. Release 1.0 stabilizes the Adventure Update's progression but much of the system was already operational in Pre6. Later Release 1.1 adds bow enchantments; Release 1.4 adds new potion routes and anvil repair. Exact book power math, potion durations, recipes and every prerelease exception remain under review.

### Redstone

The requested history begins with an early, limited redstone set: Alpha v1.0.1 redstone ore/dust, torches, levers, buttons, pressure plates and iron doors; Beta 1.3 adds repeaters; Beta 1.5 powered/detector rails; Beta 1.7 pistons/sticky pistons; Release 1.3 tripwire; Release 1.5 comparators, hoppers, droppers, trapped chests, weighted plates, daylight sensors, redstone blocks and activator rails. These additions do not imply modern signal/update semantics. Beta 1.7's piston invalid block-36 state can crash rendering. Release 1.5 comparators read container fullness and support comparison/subtraction, hoppers transfer items and respond to redstone power, and daylight sensors encode daylight; exact delays, update ordering, analog outputs, move lists and edge cases require a technical source review. Quasi-connectivity is not to be presumed in these versions; demonstrate its existence per historical build before emulation.

### Dimensions

The Nether arrives in Alpha v1.2.0 and is accessible via built obsidian portal; Nether fortresses arrive during the Beta 1.9 prerelease sequence (sources reviewed here support the sequence-level boundary but not fully verified exact first build). Strongholds generate in Beta 1.8 without an accessible End. Pre3 adds a stronghold portal room, eye/frame/portal assets but the page says the portal block has no function and the frame repair has no further result. Pre4 activates the portal and introduces the End dimension, but the dragon does not spawn. Pre5 has a dragon achievement and test death animation without spawning the boss. Pre6 first makes the dragon encounter/kill/exit portal/egg/credits operable. Release 1.0 follows as the official full release, not the first build with every End mechanic. Presence of a block asset, structure room, code/entity, dimension, survival route and functional boss are distinct implementation stages.

### Rendering, weather, and audio

Alpha v1.0.4 winter mode is a separate world-creation choice with constant snowfall; Alpha v1.2.0 removes it as biome-based weather arrives. Beta 1.5 adds rain/snow/thunder and distinct rain audio; Beta 1.5_01 and Beta 1.6 correct several block-intersection, lighting, sound and multiplayer-weather bugs. Snowfall’s temporary destruction of torches/redstone/saplings/rails belongs to the Beta 1.9 prerelease and is fixed by Pre5, not Release 1.0. Alpha 1.0.14 re-enables three non-ambient music tracks; Alpha 1.2.0 adds six more, though exact old-asset timing remains uncertain. Beta 1.8 introduces a new lighting engine, colored sunrise/sunset and deeper void fog, time-synced clouds, title panorama/FOV/brightness controls, biome colors, chest animation and many texture changes. Release 1.0 enumerates new sound families and shared asset-server replacements; distinguish the latter from client-binary code. Release 1.2 adds lava rumble/particles and closer Nether fog; 1.5 updates texture-pack structure and lighting; 1.6 switches to resource packs and adds missing-texture rendering. These are source-page observations, not direct renderer/audio-engine tests; exact shader/tick/audio implementation remains unverified. Sources: [Alpha 1.0.14](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.14?action=raw), [Alpha 1.2.0](https://minecraft.wiki/w/Java_Edition_Alpha_v1.2.0?action=raw), [Beta 1.5](https://minecraft.wiki/w/Java_Edition_Beta_1.5?action=raw), [Beta 1.6](https://minecraft.wiki/w/Java_Edition_Beta_1.6?action=raw), [Beta 1.8](https://minecraft.wiki/w/Java_Edition_Beta_1.8?action=raw), [weather history](https://minecraft.wiki/w/Weather?action=raw), [sound history](https://minecraft.wiki/w/Sound?action=raw), [music history](https://minecraft.wiki/w/Music?action=raw).

### Protocols and multiplayer architecture

Protocol milestones listed by version pages: a protocol version first appears in Alpha v1.0.5, and the numbering resets to 1 at Alpha v1.0.17; exact Alpha sub-version values are not yet enumerated. Later documented values are Beta 1.0=7, 1.2=8, 1.3=9, 1.4=10, 1.5=11, 1.6=13, 1.7=14, 1.8=17, Beta 1.9 Pre3=19; Release 1.0=22, 1.1=23, 1.2.1=28, 1.3.1=39, 1.4.2=47, 1.5=60 and 1.6.1=73. These are secondary-source version-page values, not protocol-compatibility test results; numeric IDs also should not be compared naively across the later 1.7.2 protocol rewrite, outside this target range. The original Beta 1.3 client has a Java 6 dependency while _01 removes it; Release 1.6.1 raises the minimum Java version to 6. Release 1.3.1 is the major client/server architecture boundary: singleplayer uses an integrated multiplayer server, and LAN hosting arrives, causing documented gameplay changes; this is more than a UI toggle. Exact packet schemas, handshake behavior and execution/thread model remain unverified. [Protocol history](https://minecraft.wiki/w/Protocol_version?action=raw), [Beta 1.3 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.3?action=raw), [Release 1.3.1 raw](https://minecraft.wiki/w/Java_Edition_1.3.1?action=raw), and [Release 1.6.1 raw](https://minecraft.wiki/w/Java_Edition_1.6.1?action=raw) support these boundary claims.

### Survival progression

Pre-Beta 1.8 survival generally centers on crafting/mining/Nether (with early progression items and no hunger/XP loop). Beta 1.8 introduces exploration structures, hunger/sprinting and a collectible/displayed but unspendable XP system; strongholds do not yet permit End travel. Beta 1.9 builds fortresses, portal rooms, eyes, cauldron/stand brewing, and enchanting in stages. Pre3 shows portal/enchanting UI without full function; Pre4 gives functional enchanting/End access but no dragon; Pre5 references unavailable dragon via achievement; Pre6 has the first completable boss/ending. Full Release 1.0 is the official release, not the first build where those systems function. Release 1.2 adds jungle biome, a 256-block height limit and iron golems; Release 1.3 adds emerald trade progression, temples, tripwire/Ender chest; Release 1.4 adds Wither/Beacon. An exact survival recipe/loot path remains to be reconstructed.

## Part V — Historical accuracy and implementation matrix

Status is entered only after evidence establishes the relevant boundary. `?` here means **Unknown / not yet assessed**, not absent. `C` = changed historic implementation, `P` = partial, `F` = functional, `I` = internal/inaccessible, `A` = absent, `R` = removed. The matrix covers selected high-impact milestones, not every block, recipe or mob; omissions are not evidence of absence and the matrix remains subject to audit.

| Feature | A1.0.1 | A1.0.4 | A1.0.6 | A1.0.11 | A1.0.14 | A1.0.17 | A1.1.0 | A1.2.0 | B1.0 | B1.2 | B1.3 | B1.4 | B1.5 | B1.6 | B1.7 | B1.8 | B1.9pre1 | pre2 | pre3 | pre4 | pre5 | pre6 | R1.0 | R1.1 | R1.2 | R1.3 | R1.4 | R1.5 | R1.6 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Biome generation | A | A | A | A | A | A | A | C/F | C | C | C | C | C | C | C | C | C | C | C | C | C | C | C | C | C | C | C | C | C |
| Nether | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Hunger | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Sprint | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | C | C | C | C | C | C | C | C | C | C | C | C | C |
| XP orbs / levels | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | P | P | P | P | F | F | F | F | F | F | F | F | F | F |
| Stronghold generation | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Stronghold End portal room | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | P | F | F | F | F | F | F | F | F | F | F |
| Villages | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Abandoned mineshafts | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Ravines | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Dungeons (monster rooms) | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Desert wells | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F |
| Desert and jungle temples | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F |
| Witch huts | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F |
| Nether fortresses | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Weather (rain/snow/thunder) | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Maps | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Powered and detector rails | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Melee critical hits | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Bow charge | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Animal breeding | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F |
| Baby animals | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F |
| Ender Dragon | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | I | I | I | F | F | F | F | F | F | F | F |
| Villager trading | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F |
| Anvil | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F |
| Wolves | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Endermen/silverfish/cave spiders | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Villager entities in villages | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Ocelots and iron golems | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F |
| Wither, witches, bats, wither skeletons, zombie villagers | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F |
| Enchanting | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | P | F | F | F | F | F | F | F | F | F | F |
| Brewing | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | I | F | F | F | F | F | F | F | F | F | F | F |
| Creative-only spawn eggs | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | I | I | I | I | I | I |
| Pistons | A | A | A | A | A | A | A | A | A | A | A | A | A | A | P | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Hoppers/comparators | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F |
| Horses (normal vanilla entities) | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F |
| 256-block world height | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F |
| Smooth lighting | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Legacy direct food healing | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | R | R | R | R | R | R | R | R | R | R | R | R | R | R |
| Bed sleeping/time skip | A | A | A | A | A | A | A | A | A | A | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Bed respawn-point setting | A | A | A | A | A | A | A | A | A | A | A | P | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Potion status effects | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | I | F | F | F | F | F | F | F | F | F | F | F | F |
| Achievement triggers/UI | A | A | A | A | A | A | A | A | A | A | A | P | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F | F |
| Emeralds and villager trading | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F |
| Cocoa beans: Survival route | A | A | A | A | A | A | A | A | A | I | I | I | I | I | I | I | I | I | I | I | I | I | I | I | I | F | F | F | F |
| XP from mining ores | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | F | F | F | F |
| End portal/End access | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | P | F | F | F | F | F | F | F | F | F | F |

**Matrix caveats:** Each row represents an implementation milestone and may conceal important subfeatures; e.g. Nether access in Beta 1.6 gains multiplayer support but Nether itself exists since Alpha v1.2.0. `I` means a feature is present internally or exposed only in a restricted editor/Creative path, not ordinarily obtainable. `P` includes a partly broken/unstable version (Beta 1.7 piston behavior; Beta 1.9 Pre3 portal/enchanting UI). Cells are provisional unless their precise build boundary is cited in Part III. For Beta 1.9, shortened headers identify each distinct prerelease, not a merged feature set. “Biome generation C/F” means operational but algorithmically changed. The matrix remains an audited draft: complete feature-by-feature patch/artifact testing has not been performed.

### Matrix notes for recreation

1. Do not use a global “legacy” switch for features that arrive across prereleases; gate at the per-build boundary.
2. Treat registered/debug/Creative-only and naturally generated/Survival-obtainable as separate states.
3. Store target identity precisely enough to distinguish Beta 1.9 Pre3 original client from its same-day reupload if reproducing that artifact.
4. Worldgen version changes are seed-discontinuous; block palette filtering alone cannot reconstruct historical terrain.

## Part VI — Sources and research gaps

### Core references consulted

1. [Java Edition version history](https://minecraft.wiki/w/Java_Edition_version_history) — era chronology, release overview. Secondary wiki overview; it says it is not official.
2. [World generation/History](https://minecraft.wiki/w/World_generation/History) — milestone-by-milestone generator changes across Alpha, Beta and later. Explicitly WIP; used only for claims actually listed, with caveats.
3. [Java Edition Beta 1.8](https://minecraft.wiki/w/Java_Edition_Beta_1.8) — Beta 1.8 target release, additions, removals and technical history.
4. [Java Edition Beta 1.9 Prerelease 3](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_3) — detailed build page, date, two same-day client artifacts, partial enchanting/brewing/portal specifics.
5. [Java Edition 1.0.0](https://minecraft.wiki/w/Java_Edition_1.0.0) — full-release identity, date, development-name distinction, feature summary and jar metadata link.
6. [Food history](https://minecraft.wiki/w/Food) — hunger introduction boundary; currently a secondary history source.
7. [Sprinting history](https://minecraft.wiki/w/Sprinting) — development introduction and later behavior changes.
8. [Experience history](https://minecraft.wiki/w/Experience) — XP feature boundary and subsequent changes.
9. [Enchanting history](https://minecraft.wiki/w/Enchanting), [Brewing history](https://minecraft.wiki/w/Brewing), [End portal history](https://minecraft.wiki/w/End_portal) — cross-checks for prerelease/functional boundaries.
10. [Beta 1.2 raw version history](https://minecraft.wiki/w/Java_Edition_Beta_1.2?action=raw), [Beta 1.3 raw history](https://minecraft.wiki/w/Java_Edition_Beta_1.3?action=raw), [Beta 1.4 raw history](https://minecraft.wiki/w/Java_Edition_Beta_1.4?action=raw), [Beta 1.5 raw history](https://minecraft.wiki/w/Java_Edition_Beta_1.5?action=raw), [Beta 1.6 raw history](https://minecraft.wiki/w/Java_Edition_Beta_1.6?action=raw), [Beta 1.7 raw history](https://minecraft.wiki/w/Java_Edition_Beta_1.7?action=raw) — directly reviewed page histories including details hidden from summary blurbs.
11. [Beta 1.8 development versions](https://minecraft.wiki/w/Java_Edition_Beta_1.8/Development_versions), [Beta 1.8.1](https://minecraft.wiki/w/Java_Edition_Beta_1.8.1) — distinct prerelease/reupload and postrelease boundaries.
12. [World seed history](https://minecraft.wiki/w/World_seed), contemporaneous [Jeb/Mojang seed explanation](https://web.archive.org/web/20110225221214/http://mojang.com/2011/02/23/a-short-demystification-of-the-map-seed/), and community [Beta 1.8 GenLayer reconstruction](https://raw.githubusercontent.com/PhoenixVX/golden-age-wiki/main/minecraft/genlayer/sequence/b18.md) — seed/biome architecture; reconstruction is medium-confidence community evidence, not Mojang source.
13. Contemporary Beta 1.9 prerelease publication receipts: Jeb posts for [Pre1](https://twitter.com/jeb_/status/116880362793222144), [Pre2](https://twitter.com/jeb_/status/119395358135693312), [Pre4](https://twitter.com/jeb_/status/124491774398963712), [Pre5](https://twitter.com/jeb_/status/129568296600219648), [Pre6](https://twitter.com/jeb_/status/135018767620112385). These confirm announcements/dates, not exhaustive changelogs.
14. [Java 1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1), [Java 1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1), [Java 1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2), [Java 1.5](https://minecraft.wiki/w/Java_Edition_1.5), [Java 1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1), and update guide pages for feature comparisons.
15. Repository local evidence: `research/feature-timeline.json` and `research/version-boundaries.json`, both requiring parent verification as documented in their metadata.
16. Version-specific audiovisual raw histories: [Alpha 1.0.14](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.14?action=raw), [Alpha 1.2.0](https://minecraft.wiki/w/Java_Edition_Alpha_v1.2.0?action=raw), [Beta 1.5](https://minecraft.wiki/w/Java_Edition_Beta_1.5?action=raw), [Beta 1.5_01](https://minecraft.wiki/w/Java_Edition_Beta_1.5_01?action=raw), [Beta 1.6](https://minecraft.wiki/w/Java_Edition_Beta_1.6?action=raw), [Beta 1.8](https://minecraft.wiki/w/Java_Edition_Beta_1.8?action=raw), [Release 1.0](https://minecraft.wiki/w/Java_Edition_1.0.0?action=raw), [Release 1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1?action=raw), [Release 1.5](https://minecraft.wiki/w/Java_Edition_1.5?action=raw), [Release 1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1?action=raw), plus [Weather](https://minecraft.wiki/w/Weather?action=raw), [Sound](https://minecraft.wiki/w/Sound?action=raw) and [Music](https://minecraft.wiki/w/Music?action=raw) feature histories.
17. [Beta 1.0.2 raw history](https://minecraft.wiki/w/Java_Edition_Beta_1.0.2?action=raw) — patch-specific Mac launch fix and client reupload distinction; [Jeb/Mojang 2011 seed explanation](https://web.archive.org/web/20110225221214/http://mojang.com/2011/02/23/a-short-demystification-of-the-map-seed/) — primary contemporaneous seed semantics.

### Source-to-claim traceability (current sampled claims)

| Claim | Evidence link | Grade / limitation |
|---|---|---|
| Alpha period starts 30 June 2010; Alpha v1.0.1 is first Alpha-labelled build | [Java version history](https://minecraft.wiki/w/Java_Edition_version_history), [Alpha v1.0.1](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.1) | Corroborated by version-history overview; not an official changelog. |
| Alpha v1.2.0 is major biome/generator boundary and adds Nether | [World generation history](https://minecraft.wiki/w/World_generation/History), [Alpha v1.2.0](https://minecraft.wiki/w/Java_Edition_Alpha_v1.2.0) | Secondary; algorithm parameters not fully exposed. |
| Beta 1.8 major overhaul; strongholds/villages/mineshafts/ravines and biome-driven terrain | [Beta 1.8 page](https://minecraft.wiki/w/Java_Edition_Beta_1.8), [world generation history](https://minecraft.wiki/w/World_generation/History) | Corroborated, but generator page WIP and no complete source algorithm yet. |
| Beta 1.8 adds hunger, food no longer heals directly | [Beta 1.8 page](https://minecraft.wiki/w/Java_Edition_Beta_1.8), [Food history](https://minecraft.wiki/w/Food) | Secondary corroboration; detailed hunger values not yet checked. |
| XP collection in Beta 1.8 does not imply enchanting was available | [Beta 1.8 page](https://minecraft.wiki/w/Java_Edition_Beta_1.8), [Enchanting history](https://minecraft.wiki/w/Enchanting) | Verified UI/feature timeline to be technically corroborated; exact internal implementation still pending. |
| Pre3 has enchantment-table UI but no enchantments; same-day debug hotbar reupload distinction | [Beta 1.9 Pre3 page](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_3) | Build-specific secondary history; original/client reupload and source artifact IDs identified by page. |
| Release 1.0.0 is distinct and dated 18 Nov 2011; 1.0.1 is server-only | [Release 1.0](https://minecraft.wiki/w/Java_Edition_1.0.0?action=raw), [1.0.1 raw](https://minecraft.wiki/w/Java_Edition_1.0.1?action=raw) | Secondary histories list artifact type/hash; no original client/server run. |
| Release 1.2.1 is not the emerald/trading boundary; those arrive in 1.3 development | [1.2.1 raw](https://minecraft.wiki/w/Java_Edition_1.2.1?action=raw), [1.3.1 raw](https://minecraft.wiki/w/Java_Edition_1.3.1?action=raw), [emerald history](https://minecraft.wiki/w/Emerald) | Corrected after independent audit; cocoa/emerald survival availability distinguished from item presence. |
| Beta 1.3 beds initially support sleeping/time-skip; respawn update appears in Beta 1.4 and is buggy | [Beta 1.3 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.3?action=raw), [Beta 1.4 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.4?action=raw) | Version-specific secondary histories; no direct multiplayer test. |
| Beta 1.2 includes survival-unavailable cocoa beans and mounted Nether portal data-loss crash | [Beta 1.2 raw page](https://minecraft.wiki/w/Java_Edition_Beta_1.2?action=raw) | Version-specific secondary record; original game not run. |
| Beta 1.3 selectable map seed: blank uses clock, text uses Java String.hashCode, seed drives worldgen only | [Contemporaneous Jeb/Mojang seed note (23 Feb 2011)](https://web.archive.org/web/20110225221214/http://mojang.com/2011/02/23/a-short-demystification-of-the-map-seed/) | Primary author explanation; numeric parsing width and exact generator determinism remain to verify. |
| Beta 1.7 has a stone-brick tooltip/silverfish texture without functional block/entity; piston can create invalid block 36 | [Beta 1.7 raw page](https://minecraft.wiki/w/Java_Edition_Beta_1.7?action=raw) | Build-specific secondary record; exact page documents the severe crash. |
| Beta 1.8 XP is collected/displayed but has no gameplay use | [Beta 1.8 page](https://minecraft.wiki/w/Java_Edition_Beta_1.8), [Experience history](https://minecraft.wiki/w/Experience) | Corroborated secondary history; numeric XP rates not tested. |
| Public Beta 1.6 fixes ore quadrant bias; unreleased Test Build 3 has distinct clay bug | [Beta 1.6 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.6?action=raw), [Test Build 3 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.6_Test_Build_3?action=raw), [Beta 1.7 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.7?action=raw) | Version-specific secondary histories; no controlled ore/chunk testing. |
| Beta 1.8 changes lighting/cloud/fog rendering and biome updates for old chunks | [Beta 1.8 raw](https://minecraft.wiki/w/Java_Edition_Beta_1.8?action=raw) | Direct version-page history; no original client render or chunk upgrade run. |
| Release 1.0 sound changes include asset-server replacements that could affect older clients | [Release 1.0 raw](https://minecraft.wiki/w/Java_Edition_1.0.0?action=raw), [Sound history](https://minecraft.wiki/w/Sound?action=raw) | Page distinguishes binary sounds from shared assets; no audio assets diffed. |
| Alpha 1.0.14 music re-enabled and adds three tracks | [Alpha 1.0.14 raw](https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.14?action=raw), [Music history](https://minecraft.wiki/w/Music?action=raw) | Version page records reupload distinction; exact original audio artifacts not directly tested. |
| Beta 1.9 Pre4 enchanting effects work; End dimension precedes dragon availability until Pre6 | [Pre4](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_4), [Pre5](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_5), [Pre6](https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_6) | Build-specific secondary pages; Jeb posts corroborate dates, not full changelogs. |
| Release 1.2.1 increases the world-height limit 128→256 | [1.2.1 raw](https://minecraft.wiki/w/Java_Edition_1.2.1?action=raw) | Version-page history; the terrain itself is not vertically rescaled by this claim. |

### Critical review of completeness

**Coverage check:** all 29 requested build labels appear as separate headings, and a structural check confirms 16 numbered category fields in every build section. The feature matrix now includes major generation, dimension, progression, redstone, combat, survival-route and selected mob milestones, with 29 aligned version columns. This is organizational completeness—not evidence that every field has been exhaustively researched.

**Strengths:** the most robustly traced areas are the Alpha/Beta/Release generator epochs, Beta 1.8’s terrain discontinuity and known development-version changes, Beta 1.9’s staged brewing/enchanting/End progression, and stable-vs-patch boundaries where specific histories are available. The document explicitly separates Beta 1.0 from Release 1.0.0; introduced assets from functional features; and Creative/editor/debug/Survival acquisition. An independent adversarial review found and prompted correction of material errors: emerald/trading attribution (1.2→1.3), bed sleep vs respawn boundary (Beta 1.3 vs 1.4), empty Beta 1.8 villages, and nonfunctional Pre3 enchanting.

**Material gaps:** large parts of blocks/items, recipes, numeric IDs/metadata, stack sizes, hardness/light/collision, drops, combat/armor math, mob health/AI/spawn conditions, weather/render/audio and protocol internals are not yet resolved across every target. World generation has qualitative boundaries and one community reconstruction, but not authoritative Alpha/Beta terrain code, exact noise/RNG/population ordering, cave/ravine/ore parameters or structure placement algorithms. The selected patch register does not cover every hotfix/artifact, and many claims cite the relevant version page rather than a line-pinned archived revision.

**Evidence limitations:** the main evidence base remains Minecraft Wiki histories (secondary and sometimes WIP), with contemporary Notch/Jeb announcements used mainly for dates. Artifact hashes identify builds; no decompilation, original-build execution, survival-route test, or full binary diff is claimed. No claim in this reference should be labeled directly tested without such retained evidence. Consequently, the document is an evidence-backed research draft and audit map, **not yet an exhaustive technical specification or definition-of-done deliverable**.

### Conflicts and unresolved questions

- Full historical generator source/parameters for Alpha v1.2 and Beta 1.8: algorithm summaries are qualitative; exact octave scales, coordinate transforms, seed derivation, RNG state, and chunk population ordering are not yet demonstrated.
- Exact Beta 1.9 Pre4/5/6 boundaries for first active enchantments, End portal activation, dragon fight, potion recipes, brewing timing, and status effects require focused comparison of each build's history and ideally hash-identified jars.
- Beta 1.8 XP: precise XP source rates, whether any level had a gameplay consumer before the enchantment table, level curve, death retention, orb mechanics and patch differences are not fully verified.
- Early Alpha and Beta per-target drops, mob AI, block physics, rendering/audio and protocol details remain incomplete.
- Release version labels 1.2–1.6 represent version families; public release point versions and snapshot changes must be stated precisely rather than implied as an exact 1.2.0/1.3.0 build.
- The core history page warns itself that it is not official; when Mojang archive material or exact binaries are available, use those to resolve conflicts.

### Suggested validation against original builds

No original game execution is claimed here. A future reproducibility suite should use hash-verified, legally obtained client/server artifacts in isolated directories and record exact hashes, JVM, settings and seed. Tests should include: (1) fixed-seed chunk grids before/after Alpha 1.2 and Beta 1.8, logging biome/height/surface/ore/structure block maps; (2) fresh world vs upgraded world distinction; (3) survival and Creative/UI acquisition checks for potentially internal blocks; (4) controlled hunger/XP/enchanting/brewing/combat tick sequences across Beta 1.8 and each prerelease; (5) mob spawn/health/drop/AI experiments with difficulty, light, biome and dimension controlled; (6) redstone timing circuits with tick-level output; (7) protocol-version and packet-behavior comparisons. Raw test logs and artifact hashes should accompany any claims labeled directly tested.
