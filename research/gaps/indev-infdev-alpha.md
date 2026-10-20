# Historical gap audit — Indev 20100223, Infdev 20100420/20100611, Alpha v1.0.1 → v1.2.2a

Audit of the 13 declared pre-Beta profiles against authoritative sources. The **actual** side is
`research/version-facts.md` (generated from the code, read-only here); where the generated facts are
ambiguous or look wrong I read the Java source they come from (paths cited) to establish what the runtime
actually does. The **expected** side is minecraft.wiki per-version history pages fetched with `?action=raw`,
plus `research/feature-timeline.json` rows (each of which carries its own wiki quote and URL).

Scope note: this is a *gap* audit. Known gaps that `research/implementation-coverage.md` already records
accurately are **not** re-listed as new rows; they are collected in §6 with a pointer, because repeating
them would hide the new findings.

---

## 1. Evidence base

Every `[Wn]` below resolves to one of these URLs. Quotes are verbatim from the fetched wikitext (template
wrappers removed; `…` marks an ellipsis). "Timeline row" means `research/feature-timeline.json`, whose
`quote` field is the wiki History line and whose `url` field is the page.

| ID | Source (URL) | What it establishes here |
|---|---|---|
| W1 | https://minecraft.wiki/w/Java_Edition_Alpha_v1.2.0 | Halloween Update: Nether, netherrack/soul sand/glowstone, pumpkins, clock, fishing (1/500 per tick, raw fish only, +2/+5 HP), ghasts (10 HP, 0–2 sulphur), zombie pigmen (10 HP, 0–2 cooked porkchops), 12 biomes, temperature/humidity sky, deeper-light monster spawning, slimes removed from spawning, leaves stop decaying |
| W2 | https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.1 | redstone items/blocks; redstone ore drops 4–5 dust and "takes far longer" to mine (4.5 s with iron pick); wire has "two power appearances"; mushrooms "Generate anywhere… eventually pop off" in sunlight; lit furnace drops furnace; monster spawners do not drop; gold/diamond ore generated twice as often; "Improved mob AI" |
| W3 | https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.4 | ice and snow added; snow "Cannot be obtained yet"; winter mode; mobs' jumps shorter; minecart chest model removed |
| W4 | https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.6 | cactus damage on punch/right-click/step; boats break on land and fit in one block of water; leaves no longer decay; sapling drop 10%→5%; snow block recipe 4 snowballs, drops 6; snowballs stack 16; pigs/sheep spawn only above grass |
| W5 | https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.11 | clay/sugar cane/paper/book/brick/slimeball; slime sizes and HP; slimes "Spawn only below Y=32"; split only when killed by fist; bookshelf and bricks recipes; cactus placement change; milk bucket; shovel snow-layer change |
| W6 | https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.14 | chickens (0–2 feathers, egg every 5–10 min, 4 HP), jukebox, music discs (dungeon chests / skeleton-killed creeper), furnace+chest minecarts, three music tracks, arrows flint-tipped, slime spawning disabled, obsidian blast resistance 1200, dungeons 4→8 iterations, music re-enabled |
| W7 | https://minecraft.wiki/w/Java_Edition_Alpha_v1.0.17 | oak fences; "The player starts with 64 fences in the inventory"; spider jockeys added; chickens no fall damage, glide; milk bucket can be emptied; right-click arm swing |
| W8 | https://minecraft.wiki/w/Java_Edition_Alpha_v1.1.0 | compass; monster spawner "No longer renders the model of the mob inside of them"; shared/persisted inventory (multiplayer); experimental monsters switch |
| W9 | https://minecraft.wiki/w/Java_Edition_Alpha_v1.1.2_01 | last build with the old terrain generator, bright green grass, winter mode, monoliths; resource download change |
| W10 | https://minecraft.wiki/w/Java_Edition_Alpha_v1.2.2 | lava flows 8 blocks in the Nether (before: same as Overworld); texture packs; main menu version name top-left; F4 portal debug key in 1.2.2a; fixes for per-tick hurt sound, damage-while-recovering, fire-immune pigmen/ghasts |
| W11 | https://minecraft.wiki/w/Java_Edition_Indev_20100223 | paintings added (recipe planks + light gray cloth); cobblestone smelts to stone; logs drop themselves; planks from log; armour no longer stacks; bread 3 wheat; sheep drop only light gray cloth when punched |
| W12 | https://minecraft.wiki/w/Java_Edition_Infdev_20100420 | saplings grow into big trees; "Mobs now spawn less often"; mob pathfinding/behaviour changed; level-generator update; hostile mobs no longer spawn in bright areas (fix); debug screen "All"/"Counted" changes |
| W13 | https://minecraft.wiki/w/Java_Edition_Infdev_20100611 | terrain islands; trees changed; "Clouds: Are now three-dimensional, have a texture, and are translucent"; "The player now always spawns on sand"; item forms of blocks lit differently; monoliths appear (fixed in Alpha 1.2.0); door drop fix |
| W14 | https://minecraft.wiki/w/Debug_screen | Classic: "The version number is now displayed in the top-left corner. Includes the fps and chunk updates"; Indev 20100202/20100206/20100212 add E/P/LT then C/F/O/B/I/T; Infdev values; Alpha v1.1.1 "The debug screen is no longer listed as an option and now requires F3 to be held down to activate"; v1.2.3 "The coordinates have been added to the debug screen" |
| W15 | https://minecraft.wiki/w/Daylight_cycle | 20100211-2327 "Added the daylight cycle, as well as the sun and the moon" (15 min); 20100212-1210 20 min and stars; Infdev 20100227 removed; 20100313 reimplemented (16 min); 20100316 20 min again |
| W16 | https://minecraft.wiki/w/Slab | "0.26 SURVIVAL TEST Added stone (now smooth stone) slabs"; Indev 20100130 "Slabs have now received a crafting recipe: 3 slabs from 3 cobblestone"; Beta 1.3 "Stone slabs are now crafted from 3 stone"; 1.2.1 "The crafting recipe has been changed, so that 6 slabs would be crafted rather than 3"; 1.14 "Stone slabs had been renamed to smooth stone slabs, and now crafted out of 3 smooth stone" |
| W17 | https://minecraft.wiki/w/Wool | Indev 20100201-0025 "Light gray cloth can now be crafted using nine string"; 20100223 "Sheep now drop only light gray cloth"; Infdev 20100624 cloth colours removed except white, sheep drop white; Alpha v1.0.8 "The crafting recipe of cloth/leather armor has been changed from cloth to leather"; Beta 1.6.6 "The crafting recipe of wool has been changed, reducing the amount of string required from 9 to 4"; Beta 1.7 "shears … Sheep no longer drop wool when hit, albeit they drop one wool when being killed when they are unsheared" |
| W18 | https://minecraft.wiki/w/Leaves | Alpha leaf decay: v1.0.6 "Leaves no longer decay"; v1.0.6_01 decay again; v1.2.0 preview "Leaves no longer decay"; v1.2.3 "Leaf decay has been re-introduced"; v1.2.3_01 disabled; Beta 1.0 re-introduced. Drops: 1.14 "Leaves now have a small chance to drop sticks when decaying"; Release 1.1 (11w48a) "Oak leaves now have a 1/200 chance of dropping an apple" |
| W19 | https://minecraft.wiki/w/Sneaking | "v1.1.1 … Added sneaking." |
| W20 | `research/feature-timeline.json` rows (each has its own URL) | **Food** (https://minecraft.wiki/w/Food): "1.8 Pre-release Hunger was introduced. Food no longer restores health directly…". **Sprinting** (https://minecraft.wiki/w/Sprinting): "1.8 (PAX Prime) Added sprinting." **Experience** (https://minecraft.wiki/w/Experience): "1.8 Pre-release Experience was originally revealed…". **Melee attack** (https://minecraft.wiki/w/Melee_attack): "Pre-release Added the critical hit mechanic". **Sword blocking** (https://minecraft.wiki/w/Sword_blocking): "1.8 Pre-release Added sword blocking, which blocks 50 % of incoming damage." **Zombie**: "1.8 Pre-release Zombies now drop 0-2 rotten flesh instead of 0-2 feathers" and "1.2.1 12w03a The zombies' AI has been improved… allowing them to navigate obstacles". **Skeleton**: "20100219 Skeletons now drop arrows." **Cow**: "1.8 Pre-release … Added raw beef, which drops from cows". **Chicken**: "1.8 Pre-release Chickens now drop raw chicken". **Pig**: "20100219 Pigs now drop 0-2 raw porkchops instead of mushrooms". **Spider**: "20100219 Spiders now drop 0-2 string". **Sheep**: "20100223 Sheep now drop only light gray cloth when hit." **Monster Room** (https://minecraft.wiki/w/Monster_Room): "20100625-1917 Added monster rooms." **Village** (https://minecraft.wiki/w/Village): "1.8 Pre-release Added villages, which only generated in plains and desert biomes." **Mineshaft** and **Stronghold**: "1.8 Pre-release Added…". **Nether Fortress**: "1.0.0 Beta 1.9 Prerelease Added Nether fortresses." **Giant** (https://minecraft.wiki/w/Giant, confidence medium): "never spawns naturally". **Boat**: "12w27a Boats now drop their item instead of 3 oak planks and 2 sticks if broken by a player." **Ladder** (confidence low): "1.1 12w01a The collision boxes for ladders have now been removed." **Minecart booster** (https://minecraft.wiki/w/Tutorial:Minecart_booster): "Minecart boosters no longer work as of Beta 1.6." **Food** (instant eating): "0.24_SURVIVAL_TEST … eaten instantly without sound or animation." |
| W21 | https://minecraft.wiki/w/Java_Edition_Beta_1.5 | "Weather: Added natural occurring rain, snowfalls, and thunderstorms."; "Redstone dust: … Several changes in redstone wire rendering"; "Ladders: Its crafting recipe now produces 2 ladders"; "Unarmed damage was increased from 1 to 2" |
| W22 | Mod source read for the actual side | `Feature.java`, `HistoricalCraftingEra.java`, `HistoricalCreativeCatalog.java`, `HistoricalMobLootHelper.java`, `HistoricalLootRules.java`, `HistoricalSpawnRules.java`, `HistoricalStructureRules.java`, `HistoricalFeatureExclusions.java`, `SheepHistoricalShearMixin.java`, `LootTableHistoricalEarlyLeavesMixin.java`, `HistoricalVersionLabel.java`, `HudHelper.java` (no edit made to any of them) |

---

## 2. How to read the rows

* **Gap type** — `MISSING` (the version had it, the mod does not), `WRONG` (present but not the historical
  behaviour/value), `EXTRA` (the mod has something the version did not), `UNKNOWN` (cannot establish one
  side; §8 says what evidence would settle it).
* **Severity** — judged by player impact in an ordinary survival playthrough of that build: `HIGH` = changes
  what you can get/do/meet, or breaks progression; `MED` = clearly visible behaviour or balance difference;
  `LOW` = cosmetic, quirk, debug-tool or very rare.
* **Confidence** — `verified` = both sides established from an artifact or a primary/secondary wiki line
  *and* the code path read; `inferred` = one side reasoned from adjacent evidence; `unknown` = one side not
  established.
* **Actual** values are quoted from `research/version-facts.md` ("facts") or from the named Java class where
  the generated line is not the whole story (noted as such).

Severity/top-10 ranking is in §7.

---

## 3. Cross-version gap rows

These apply to every listed version unless the "applies to" column narrows them. They are the rows that the
per-version sections below do *not* repeat.

| ID | Domain | Expected (quote + URL) | Actual (facts / code) | Type | Sev | Conf | Applies to |
|---|---|---|---|---|---|---|---|
| **A-01** | mobs / drops | Every Alpha mob had **one** drop item: "Zombies now drop 0-2 rotten flesh **instead of 0-2 feathers**" [W20 Zombie], "Added raw beef, which drops from cows" is itself a *Beta 1.8* change [W20 Cow], "Chickens now drop raw chicken" is Beta 1.8 [W20 Chicken], "Spiders now drop 0-2 string" [W20 Spider], "Skeletons now drop arrows" (bones are Beta 1.2) [W20 Skeleton]. Alpha 1.0.14 already documents the disc condition: "Can be obtained when a creeper is killed by a skeleton" [W6]. | facts: **"Drops: modern drop tables"** for all ten Alpha profiles. Code: `HistoricalMobLootHelper.getTable` only replaces loot when `HistoricalLootRules.hasOldDrops` finds a rule in `version.rules().mobDrops()`; the Alpha profiles declare an empty map, so `getTable` returns the **vanilla 1.21.1 loot table** [W22]. Result: zombies drop rotten flesh/iron/carrot/potato, skeletons drop **bones** as well as arrows, spiders also drop spider eyes, cows drop beef, chickens drop raw chicken, pigs drop 1–3 porkchops (historically 0–2), creepers killed by skeletons can drop any modern disc (historically only 13/cat, and not at all before 1.0.14), and sheep follow the separate wool branch in A-02. | WRONG + EXTRA | HIGH | verified | Alpha v1.0.1 … v1.2.2a (all 10) |
| **A-02** | mobs / drops | Sheep gave wool **only when hit**, not on death: Beta 1.7 is when "Sheep no longer drop wool when hit, albeit they drop one wool when being killed when they are unsheared" [W17], and the Alpha v1.1.2_01 client's sheep class has no death-drop override at all (the project's own `Feature.PUNCH_SHEARS` evidence records the decompiled method) [W22]. | facts: "Drops: modern drop tables" with the sheep special case reaching `HistoricalMobLootHelper` line 189 — `usesLegacySheepDrops` is true for anything before Release 1.8, so an unsheared sheep drops **1 wool on death** in every Alpha world [W22]. Combined with `PUNCH_SHEARS` (also active), the same sheep can be punched for 1–3 and then killed for 1 more. | EXTRA | MED | verified | Alpha (all 10) |
| **A-03** | mobs / AI | "1.2.1 12w03a: The zombies' AI has been improved, giving them a much better sense of direction when pursuing the player, allowing them to navigate obstacles, and even small mazes if constructed, to get to the player" [W20 Zombie, confidence medium]. The goal-based AI behind this is Beta 1.8/1.9 content (`RangedBowAttackGoal` etc.; see `Feature.NO_SKELETON_STRAFING`). Infdev 20100420 already documents simpler AI: "Mob pathfinding and behavior changed" [W12]. | facts list only `NO_SKELETON_STRAFING`, `NO_ANIMAL_PANIC`, `NO_MONSTER_SUN_AVOIDANCE` as AI gates; there is no gate for pathfinding/obstacle navigation. Actual runtime is the 1.21.1 goal AI: zombies and skeletons path around obstacles, open doors, and track the player far better than any build up to Beta 1.7.3. | WRONG | HIGH | verified (fact) / inferred (magnitude) | all 13 |
| **A-04** | mobs → combat / damage values | Pre-Beta melee values are not modern: the wiki's own Beta 1.5 line "Unarmed damage was increased from 1 to 2" [W21] shows attack damage was still being tuned during beta, and the era's mob classes carry their own damage constants. | facts carry no mob-damage rule; runtime damage comes from 1.21.1 attributes (e.g. zombie 3 on Normal, skeleton arrow 2–4 charge-scaled). Nothing keys mob attack damage to the profile. | UNKNOWN | MED | unknown — §8 item 3 | all 13 |
| **A-05** | combat / armour | Armour mitigation before Release 1.9 is a different formula (and pre-Beta-1.8 armour additionally lost effectiveness with durability — `docs`/coverage already flags the *Beta 1.8→Pre1* change as unimplemented, which only makes the pre-1.8 model older still). Expected model is the era client's `damageEntity` armour step. | facts: no armour rule; runtime is the 1.21.1 armour/toughness formula (`LivingEntity` damage pipeline), i.e. armour points 0–20, toughness, and the `min(20, max(armor/5, armor − damage/(2+toughness/4)))` shape. | UNKNOWN | MED | unknown — §8 item 3 | all 13 |
| **A-06** | visuals + UI / debug overlay | The era's debug screen is a version string plus frame/chunk/entity counters, with **no coordinates until v1.2.3**: "v1.1.1 … now requires F3 to be held down to activate" [W14], "v1.2.3 The coordinates have been added to the debug screen" [W14], and pre-1.1.1 it was an options toggle, not F3 [W14]. | facts: only the corner label is version-aware (`VERSION_LABEL`); `HistoricalVersionLabel` deliberately suppresses the label "while F3 is open (… modern F3 already …)" [W22]. No gate was found on the debug overlay itself, so an Alpha world shows the full 1.21 F3 screen — coordinates, facing, biome, light, spawn caps, seed, targeted block — none of which these builds displayed (and no coordinates at all before v1.2.3). | EXTRA | MED | verified | all 13 |
| **A-07** | visuals / clouds | "Infdev 20100611 … Clouds: Are now three-dimensional, have a texture, and are translucent" [W13] — i.e. before that build the clouds were the flat layer between layers 108 and 112 (`HistoricalWorldTweaks` cites "Added clouds, which float westward between layers 108 and 112" [W22]). | facts: cloud height 108 for every one of these profiles, but no flat/2-D cloud model; the 1.21.1 3-D textured translucent cloud renderer is used. Correct for Infdev 20100611 onward; wrong for Indev 20100223 and Infdev 20100420. | EXTRA | LOW | verified | Indev 20100223, Infdev 20100420 |
| **A-08** | UI / game mode | Creative mode did not exist until the Beta 1.8 Adventure Update (`OLD_CREATIVE_MENU` starts at Beta 1.8.1); no pre-Beta build had a creative inventory, item catalogue, or creative flight. | `SnapshotCreativeRequest` toggles `GameType.CREATIVE` for any declared profile, and `HistoricalCreativeCatalog` supplies an era-filtered catalogue for these 13 ids [W22]. facts never mention game mode. This is an EXTRA; whether it is a *defect* depends on whether the owner wants creative as out-of-band tooling — recorded, not asserted as a bug. | EXTRA | LOW | verified | all 13 |
| **A-09** | visuals / audio | Sound and music are version-dated in the sources: 1.0.14 "Music: Re-enabled. Added new music by C418: piano1/piano2/piano3" [W6]; 1.2.0 adds six tracks [W1]; "The 'ough' sound no longer plays when the player takes damage" is a 1.2.0 change, so earlier builds played it [W1]; music discs are 1.0.14 content [W6]. | `version-facts.md` has an entire Visual profile but **no audio line at all**; nothing in the generated facts keys music, ambient sound, the old damage sound or disc playback to the profile. Cannot determine from the facts whether era sounds/music play. | UNKNOWN | MED | unknown — §8 item 1 | all 13 |
| **A-10** | crafting / items | Compass arrives in Alpha v1.1.0: "Compass … Always points towards the world spawn point. Crafted with iron ingots and redstone dust" [W8]. Clock arrives in Alpha v1.2.0: "Clock … New item that tells the time. Crafted with 1 redstone dust and 4 gold ingots" [W1]. | facts crafting refused-lists do not include `compass`/`clock`; code: `HistoricalCraftingEra`'s `always(...)` list contains **"compass", "clock"** [W22], so the modern recipes are allowed in every Alpha profile. The project's own `HistoricalCreativeCatalog` dates them correctly (`from("alpha_1_1_0","compass")`, `from("alpha_1_2_0", "… clock …")`) — the two tables contradict each other. Result: compass craftable in 1.0.1–1.0.17 (should be impossible), clock craftable in 1.0.1–1.1.2_01 (should be impossible). | EXTRA | MED | verified | compass: v1.0.1–v1.0.17; clock: v1.0.1–v1.1.2_01 |
| **A-11** | blocks + crafting / stone slab | Stone slabs have existed since "0.26 SURVIVAL TEST Added stone (now smooth stone) slabs" with the recipe "20100130 … 3 slabs from 3 cobblestone" [W16]; Beta 1.3 changes only the recipe ("Stone slabs are now crafted from 3 stone") and 1.2.1 changes the count [W16]. The era block is the modern **`smooth_stone_slab`** ("1.14 … Stone slabs had been renamed to smooth stone slabs") [W16]. | facts: `smooth_stone_slab` appears in **"Blocks judged anachronistic"** for every one of the 13 profiles (e.g. Alpha 1.0.1 records 75 anachronistic including `smooth_stone_slab`), because `HistoricalBlockAvailability` dates it to Beta 1.3 [W22]. At the same time `HistoricalCraftingEra` allow-lists **`stone_slab`** as "always" — and in 1.21.1 `stone_slab` is the *Release 1.14* block with a 3-stone→6 recipe. Net effect in an Alpha world: the era slab (`smooth_stone_slab`) is refused and the 1.14 slab (`stone_slab`) is craftable with the wrong ingredient and count. The Indev artifact table gets this right (`smooth_stone_slab` ×3 from 3 cobblestone) [W22], which is how the contradiction is visible. | EXTRA + MISSING | MED | verified | all 10 Alpha |
| **A-12** | crafting / arrows | "Arrows: The tip in crafting is now made from flint rather than iron ingots" — Alpha v1.0.14 [W6]. Before it the recipe was iron ingot + stick + feather → 4 arrows (the Indev artifact table records exactly that) [W22]. | facts: no arrow-recipe line; code: the vanilla 1.21.1 flint recipe is allowed in every Alpha profile (`arrow` is "always" on the craft allow-list and no early table applies) [W22]. Flint obtainable from gravel in all these builds, so the difference is material cost: 1 flint instead of 1 iron ingot per 4 arrows. | WRONG | MED | verified | Alpha v1.0.1–v1.0.11 |
| **A-13** | crafting / wool | "Indev 20100201-0025: Light gray cloth can now be crafted using nine string"; the recipe is still 9 string in Alpha, "1.6.6 The crafting recipe of wool has been changed, reducing the amount of string required from 9 to 4 pieces" [W17]. | facts: `white_wool` is craftable in every Alpha profile; code: the vanilla 4-string→1-wool recipe is permitted (`white_wool` on the allow-list, no recipe-id or block override) [W22]. So Alpha worlds get wool at less than half the historical string cost. (Indev/Infdev early tables use the artifact recipe: 9 string → 1 light gray wool [W22].) | WRONG | LOW | verified | all 10 Alpha |
| **A-14** | crafting / armour progression | "Alpha v1.0.8: The crafting recipe of cloth/leather armor has been changed from cloth to leather" [W17]. Before 1.0.8 armour was made from cloth (light gray wool; the Indev artifact recipes show the exact cloth patterns) [W22], and **cows do not exist before Alpha v1.0.8** (facts: Alpha 1.0.1/1.0.4/1.0.6 spawn sets exclude `minecraft:cow`; feature-timeline "mob: cow | in=Alpha v1.0.8" [W20]). | facts: 1.0.1/1.0.4/1.0.6 have `NO_ANIMAL_BREEDING` etc. and a cow-free spawn set, and their crafting is modern-recipe-based; code: leather armour recipes are `always` on the allow-list and the **modern leather patterns** are what craft. Leather has no legit source in those three builds, so armour is **uncraftable**, and cloth armour (the actual route) does not exist. | MISSING | HIGH | verified | Alpha v1.0.1, v1.0.4, v1.0.6 |
| **A-15** | blocks / leaf loot | Sticks from leaves are Release 1.14: "1.14 18w43a Leaves now have a small chance to drop sticks when decaying" [W18]; apples from leaves are Release 1.1: "1.1 11w48a Oak leaves now have a 1/200 chance of dropping an apple" [W18]. | The early leaf-drop replacement (`LootTableHistoricalEarlyLeavesMixin`) only fires for Indev/Infdev, where it cancels the modern roll and emits the artifact's single 10 %-sapling result [W22]. All ten Alpha profiles therefore keep the **modern** oak-leaf table: 5 % sapling + 2 % 1–2 sticks + 0.5 % apple. Sticks and apples are EXTRA in every Alpha build; the sapling chance is correct from 1.0.6 and wrong before it (A-16). | EXTRA | LOW | verified | all 10 Alpha |
| **A-16** | blocks / leaf sapling chance | "Alpha v1.0.6: Leaves … Now have a 1⁄20 (5%) chance of dropping a sapling instead of 1⁄10 (10%)" [W4] — so 1.0.1 and 1.0.4 drop saplings at 10 %. | facts: no sapling-chance line for Alpha; runtime is the modern 5 % oak-leaf sapling roll (A-15). | WRONG | LOW | verified | Alpha v1.0.1, v1.0.4 |
| **A-17** | mobs / spider jockeys | "Alpha v1.0.17 … Spider jockeys: The first jockey to be added into the game" [W7]. Before that build a skeleton could not ride a spider. | 1.21.1 spawns spider jockeys (skeleton riding a spider) from its ordinary spider spawn path for every profile; nothing gates jockeys. | EXTRA | LOW | inferred | Indev 20100223, Infdev 20100420, Infdev 20100611, Alpha v1.0.1–v1.0.14 |
| **A-18** | visuals / sky colour | From the Halloween Update the sky is biome-dependent: "Sky: Color is now dependent on temperature/humidity values" [W1] (grass and leaves likewise). | facts: 1.2.0 and 1.2.2a report `FLAT_SKY_COLOUR` active and "sky FLAT 9158143"; `Feature.FLAT_SKY_COLOUR`'s evidence says this is "the 2026-10-15 owner revert of the temperature sky" [W22]. So the flat sky is deliberate; it is still a deviation from the source, recorded here rather than re-litigated. | WRONG | LOW | verified (deliberate) | Alpha v1.2.0, v1.2.2a |
| **A-19** | mobs / spawning rules | Spawn *conditions* are version-specific and sourced: Alpha 1.0.11 "Slime … Spawn only below Y=32" [W5]; Alpha 1.0.6 "Pigs and sheep: Now only spawn above grass blocks" [W4]; Alpha 1.2.0 "Mobs: General: Higher lighting values are required the deeper they are to keep mobs from spawning" [W1]; Infdev 20100420 "Mobs now spawn less often" [W12]. | `HistoricalSpawnRules` states in its own javadoc that it "establishes which entity types may spawn, not the historical weights or pack sizes; those remain unestablished" [W22]; the only spawn hook is the species filter (`NaturalSpawnerHistoricalSpawnMixin`), and the pre-Beta spawner itself was removed at the owner's request (coverage §6 item 4). Therefore weights, pack sizes, caps and light thresholds are all modern 1.21.1. The *specific* conditions are called out per version below (slime Y for 1.0.11+, light depth for 1.2.0+). | WRONG / UNKNOWN | MED | inferred | all 13 |
| **A-20** | world / monoliths | Infdev 20100611: "Monoliths now generate. This was fixed in Alpha v1.2.0" [W13]; Alpha v1.1.2_01 is "the last version to generate monoliths on the terrain" [W9]. | The Alpha/Infdev terrain comes from `moderner_beta:alpha` / `moderner_beta:infdev_611` with feature exclusions; no monolith generation exists, and the project does not model the 1.0.14-era decorator quirks either (facts: "Dungeon generator iterations … recorded as not implemented"). A HISTORY-OF-BUGS row: benign to omit, but it is a visible terrain signature of those builds. | MISSING (of a documented terrain bug) | LOW | verified | Infdev 20100611; Alpha v1.0.1–v1.1.2_01 (all pre-1.2.0 Alpha) |

---

## 4. Domain ledger — every domain × every version

`✓` = checked against the sources and no gap found (justification in §4.1); a gap ID = a row above or in §5;
`~K` = a gap already recorded accurately in `research/implementation-coverage.md` (§6), not re-listed;
`UNK` = cannot establish one side (§8).

| Domain | Indev 0223 | Infdev 0420 | Infdev 0611 | 1.0.1 | 1.0.4 | 1.0.6 | 1.0.11 | 1.0.14 | 1.0.17 | 1.1.0 | 1.1.2_01 | 1.2.0 | 1.2.2a |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| blocks / items | ✓ | ✓ | ✓ | 101-01, 101-02, A-11, A-15, A-16 | 104-01, A-11, A-15, A-16 | 106-01, 106-03, A-11, A-15 | A-11, A-15 | A-11, A-15 | A-11, A-15 | A-11, A-15 | A-11, A-15 | 120-01, 120-02, A-11, A-15 | A-11, A-15 |
| crafting / smelting | ✓ | ✓ | ✓ | A-10, A-12, A-13, A-14 | A-10, A-12, A-13, A-14 | A-10, A-12, A-13, A-14 | A-10, A-12, A-13 | A-10, A-13 | A-10, A-13 | A-10, A-13 | A-10, A-13 | A-13 | A-13 |
| mobs (spawns, drops, equipment, AI, breeding, kill XP) | A-03, A-04, A-17, A-19 | A-03, A-04, A-17, A-19 | A-03, A-04, A-17, A-19 | A-01, A-02, A-03, A-04, A-17, A-19 | A-01, A-02, A-03, A-04, A-17, A-19 | A-01, A-02, A-03, A-04, A-17, A-19 | 1011-01, 1011-02, A-01, A-02, A-03, A-04, A-17, A-19 | 1014-01, A-01, A-02, A-03, A-04, A-17, A-19 | A-01, A-02, A-03, A-04, A-19 | A-01, A-02, A-03, A-04, A-19 | A-01, A-02, A-03, A-04, A-19 | 120-05, A-01, A-02, A-03, A-04, A-19 | 120-05, A-01, A-02, A-03, A-04, A-19 |
| movement (walk/sprint/sneak/swim/fall) | ✓ | ✓ | ✓ | ✓ | ✓ | 106-02, ✓ | ✓ | ✓ | ✓ | 110-01 | ✓ | ✓ | ✓ |
| combat (crit, cooldown, blocking, durability, damage values) | A-04, A-05 | A-04, A-05 | A-04, A-05 | A-04, A-05 | A-04, A-05 | A-04, A-05 | A-04, A-05 | A-04, A-05 | A-04, A-05 | A-04, A-05 | A-04, A-05 | A-04, A-05 | A-04, A-05 |
| health / hunger / food / regen | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| XP / enchanting / brewing | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| inventory (stack sizes, offhand, containers) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 1017-01 | ✓ | ✓ | ✓ | ✓ |
| world (weather, day/night, nether/end, structures, villages, biomes, ores, height, sea level) | ID-02, UNK | ✓ | IG-01, IG-02, A-20 | 101-03, A-20 | A-20 | A-20, ~K (leaf decay 1.0.6) | A-20 | A-20, ~K (dungeon 4→8) | A-20 | A-20 | A-20 | 120-03, 120-04, 120-06, 120-07, ~K | 120-03, 1222-01, A-18, ~K |
| visuals (textures, lighting, fog/sky/clouds, colours, HUD, audio, particles) | A-06, A-07, A-09 | A-06, A-07, A-09 | A-06, A-09, IG-03 | A-06, A-09 | A-06, A-09 | A-06, A-09 | A-06, A-09 | A-06, A-09 | A-06, A-09 | 110-02, A-06, A-09 | 110-02, A-06, A-09 | 110-02, A-06, A-09, A-18 | 110-02, A-06, A-09, A-18 |
| UI (creative inventory, menus) | A-08, ID-01 | A-08, ID-01 | A-08 | A-08 | A-08 | A-08 | A-08, 1011-03 | A-08 | A-08 | A-08 | A-08 | A-08 | A-08, 1222-02 |

`110-02` is a visuals row, so it appears in the visuals row of the four versions from Alpha v1.1.0 onward.

### 4.1 Why the `✓` cells are justified

* **blocks / items** — the era's dated content is correctly gated: nothing later generates (the project excludes
  modern features per revision) and the era blocks are allowed (clay/sugar cane from 1.0.11, snow/ice from 1.0.4,
  cactus from 1.0.6, pumpkin/netherrack/soul sand/glowstone from 1.2.0; facts "Blocks judged anachronistic" lists
  match these dates). The Indev/Infdev content is *artifact*-derived: 68 recipes, six smelts, a leaf rule and the
  food table were extracted from the hash-verified `in-20100223.jar` [W22], and the values match the wiki rows
  independently (9 string → light gray cloth [W17], 3 cobblestone → 3 stone slab [W16], 3 wheat → bread [W11],
  cloth armour [W17], iron-tipped arrows [W6], painting [W11], no ladders/doors/buckets before Infdev
  20100607 [W20 Door]). The residual `51 of 145` undated blocks is §8 item 6, not a demonstrated gap.
* **crafting / smelting** — apart from A-10…A-16, the gated results date correctly (the allow-list matches the
  page additions: redstone/lever/button/pressure plate/iron door at 1.0.1 [W2], snow block at 1.0.4 [W3], boat at
  1.0.6 [W4], paper/book/clay/bricks at 1.0.11 [W5], jukebox/minecarts at 1.0.14 [W6], fence at 1.0.17 [W7],
  compass at 1.1.0 [W8], fishing rod/glowstone/jack-o'-lantern at 1.2.0 [W1]). Smelting has *no* gate at all, which
  coverage §"Limits" already states; because every smeltable that could leak is either later content (gated) or
  matches history, no additional row is claimed here — but it is a standing risk (§8 item 5).
* **mobs** — the spawn *sets* are correct per build and verified (cow 1.0.8, slime 1.0.11 & disabled 1.0.14,
  chicken 1.0.14, ghast/pigman 1.2.0, slime removed from spawning 1.2.0; facts "Spawns"), and the 1.1.2_01 set is
  read from that client's entity registry [W22]. No breeding/tempting/babies/geared mobs/baby zombies before their
  boundaries (`NO_ANIMAL_*`, `NO_MONSTER_GEAR`), monsters burn in daylight from Indev 20100213 without seeking
  shade (`NO_MONSTER_SUN_AVOIDANCE`), and kill XP does not exist before Beta 1.8 [W20 Experience] — the XP/level
  HUD is also hidden (`HudHelper.hideExperienceBar`). Indev/Infdev species and drops match their artifacts
  (zombie→feather etc., no bones before Beta 1.2 [W20 Skeleton]); the giant "never spawns naturally" [W20 Giant].
  What remains wrong is drops (A-01/A-02), AI (A-03), damage (A-04) and spawn rules (A-19).
* **movement** — walk speed, fall damage and jumping use modern physics, which is the *same* model these builds
  used until the changes we do model (sprinting is Beta 1.8 [W20 Sprinting], sneaking only from v1.1.1 [W19], and
  the 1.13 swim pose is excluded by `NO_SWIMMING`; swimming itself exists from Classic 0.0.12a [W20 Swimming]).
  The mod refuses sprint for every one of these profiles (`LivingEntityHistoricalMovementMixin` reads
  `allowsSprinting`). Sneak is refused before 1.1.0 — correct — but enabled *at* 1.1.0 (110-01).
* **combat** — crits are Beta 1.8 [W20 Melee attack], sword blocking is Beta 1.8 [W20 Sword blocking], the attack
  cooldown/sweep are 1.9 (`NO_ATTACK_COOLDOWN`, `NO_SWEEP_ATTACK`), old sword damage is applied
  (`OLD_SWORD_DAMAGE`), old tool durability applies (`OLD_TOOL_DURABILITY`), and bow use is pre-Beta-1.8
  (`INSTANT_BOW`, `ARROW_SPEED`, `INVINCIBLE_BOW`). The open combat questions are A-04 (mob damage) and A-05
  (armour formula) — both UNKNOWN rather than contradicted.
* **health / hunger / food / regen** — there is no hunger bar and no natural regeneration before Beta 1.8 [W20
  Food]; the mod suppresses the whole `FoodData.tick` hunger/regeneration/starvation path for these profiles
  (`HistoricalPlayerRules.suppressesHungerTick`, `FoodDataHistoricalHungerMixin`) and hides the bar
  (`HudHelper.hideHungerBar`). Food heals directly with researched values (Indev 4 entries, Infdev 6, Alpha 6);
  food is unstackable (`UNSTACKABLE_FOOD`), eating is instant (the same gate), and the 1.2.0 fish values 2/5
  [W1] are what modern nutrition yields, so their absence from the 6-entry Alpha table is a documentation error
  rather than a visible one (§8 item 4).
* **XP / enchanting / brewing** — none of the three exists in these builds: XP is Beta 1.8 [W20 Experience],
  enchanting Beta 1.9 Prerelease 4, brewing Prerelease 3 (`NO_ENCHANTING`, `NO_BREWING`, `NO_REAL_ENCHANTING`),
  orbs are suppressed pre-Beta-1.8 (`ServerLevelHistoricalOrbMixin`, coverage), and the XP bar/level are hidden.
  The `HIDDEN_XP_LEVEL` window and the other gates are consistent with this.
* **inventory** — modern 64-stack sizes are also the era's; tools/armour are unstackable (Indev 20100223:
  "Armor can no longer be stacked" [W11] — modern is unstackable too); food is unstackable before Beta 1.8;
  no offhand (`NO_OFFHAND`); a 2×2 grid plus 3×3 workbench is the era layout; snowballs stack 16 from 1.0.6
  [W4] and modern snowballs are 16; containers (chest/furnace/dispenser-less) behave as modern. The one missing
  quirk is 1017-01.
* **world** — no weather before Beta 1.5 [W21] (`NO_WEATHER`, facts "weather false"); the day/night cycle exists in
  all three eras at 20 minutes [W15]; the End does not exist (end stone/strongholds are later [W20], the End
  portal is inert via `INERT_END_PORTAL`); the Nether is refused before 1.2.0 (`NO_NETHER`) and available from it
  [W1]; there are no structure-set structures but Alpha keeps monster rooms because they are a placed feature and
  arrive Infdev 20100625 [W20 Monster Room] (`HistoricalStructureRules` excludes them only for the two Infdev
  builds); villages are Beta 1.8 [W20 Village]; pre-1.2.0 terrain has no biomes and 1.2.0's twelve biomes are a
  known approximation (§6); the 0..127 envelope and sea level match Alpha/Infdev (facts "finite level
  256x256x128" / "reduced 0..127 envelope"). Height/bedrock details are §8 item 7.
* **visuals** — the version-keyed presentation is present and correct where the facts can show it: old lighting
  engine (`OLD_LIGHTING_ENGINE`), no smooth lighting before Beta 1.3 (`NO_SMOOTH_LIGHTING`), old fog (`OLD_FOG`),
  cloud height 108 (`LOW_CLOUDS`), flat sky with the Alpha star field (`ALPHA_STARS`), no colour maps before
  1.2.0 (`NO_COLOUR_MAPS`) and the first grass/foliage maps from 1.2.0 (`OLD_FOLIAGE_COLOUR_MAP`), old chest
  model, flat dropped items, no item tooltips before Beta 1.0 (`NO_ITEM_TOOLTIPS`), and the corner version label
  through Beta 1.6 (`VERSION_LABEL`, and `HistoricalVersionLabel` matches the Classic 0.0.2a behaviour [W14]).
  Textures come from the era packs (facts "textures vanilla+programmer_art+continuation+golden_days_*"), which is
  the right mechanism even though pixel fidelity is not verifiable from the facts. Open: A-06, A-07, A-09, A-18.
* **UI** — the era-correct no-tooltip, no-recipe-book, no-level-number presentation is gated; the version label
  is drawn and hidden while F3 is open; the hunger/XP bars are hidden; the flat legacy tooltip is used before
  Beta 1.0. Open: A-08 (creative), ID-01/1011-03 (creative-catalogue dates), 1222-02 (main-menu version string),
  A-06 (debug overlay).

---

## 5. Per-version sections

Row identifiers (`ID-01`, `IG-01`, `101-01`, `120-01`, `1222-01`, …) appear in **bold** in each table's
Domain cell so the matrix in §4 and the ranked list in §7 resolve to one row. `~K` rows are known gaps quoted
only to keep the section complete (§6); they are not counted as findings.

Each section: the actual state from `version-facts.md` (abbreviated), the **new** gap rows specific to that
build, the cross-version rows that apply, and anything checked and found sound that is worth recording.

### 5.1 Indev 20100223 (`historical_experience:indev_latest`)

**Actual.** finite 256×256×128 `moderner_beta:finite_2d` island, themes normal, out-of-bounds void; 6 natural
spawns (creeper, pig, sheep, skeleton, spider, zombie); 5 drop rules (zombie→feather, skeleton→arrow,
pig→porkchop, spider→string, creeper→gunpowder); 4 direct heals (bread 5, stew 10, porkchop 3, cooked 8);
68 artifact-extracted recipes + six smelts; no hunger/sprint/XP/structures; no anachronistic-block violation;
old lighting, flat sky, Alpha stars, fog, 108 clouds, no colour maps.
**Cross-version rows that apply:** A-03, A-04, A-05, A-06, A-07, A-08, A-09, A-17, A-19.
This is the strongest profile in the era: its crafting, smelting, food, drops, punch-shear (light gray cloth,
`SheepHistoricalShearMixin` uses `LIGHT_GRAY_WOOL` for the early profiles) and no-decay leaf/yield rules are
artifact-derived and agree with W11/W16/W17/W18.

| Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|
| **ID-01** · UI / creative catalogue | Painting is this build's own addition: "== Additions ==… Painting… New cosmetic entity… Crafted with planks and light gray cloth" [W11]. | The artifact recipe table contains the painting recipe (`8 oak_planks + light_gray_wool → painting`) [W22], but `HistoricalCreativeCatalog` dates painting with `from("infdev_611", … "painting")`, so an Indev creative menu (and Indev/Infdev catalog grants) cannot obtain it [W22]. Survival crafting is unaffected. | WRONG (catalogue) | LOW | verified |
| **ID-02** · world / world-creation options | The Indev phase had selectable world types ("theme normal" in the mod's own evidence implies other themes) and (0.31 era) map sizes; the final Indev build is the one the mod offers as a single 256×256 "normal" world. | facts: "theme normal", one preset. Whether 20100223 exposed other themes/sizes is **not** established by the pages fetched here (the 20100223 page is a delta page) — so this is recorded as UNKNOWN, not as a missing feature. | UNKNOWN | LOW | unknown — §8 item 9 |

**Domains checked, no gap found:** blocks/items (painting recipe, cobblestone→stone smelt, log drops itself,
planks from log, bread 3 wheat, armour unstackable — all W11 and present in the artifact table); crafting/smelting
(68 recipes, 6 smelts, exact patterns/counts); mobs (6 species from the entity registry; the registered giant is
excluded from natural spawning, matching "never spawns naturally" [W20 Giant]; drops and no death-drop sheep;
no breeding/gear/XP [W20]); movement (no sprint, no sneak, swimming exists [W20]); combat (no crit/cooldown/
blocking; old sword damage); health (no hunger/regen [W20 Food]; 4 direct heals; instant eating); XP/enchant/
brew (absent); inventory (armour unstackable W11, food unstackable, no offhand); world (no weather [W21], no
Nether [W1], no structures — monster rooms are Infdev 20100625 [W20], day/night 20 min [W15], finite island);
visuals (textures/lighting/fog/sky/stars; label); UI (tooltips absent before Beta 1.0 [W20 owner-corrected
`NO_ITEM_TOOLTIPS`], no recipe book).

### 5.2 Infdev 20100420 (`historical_experience:infdev_420`)

**Actual.** infinite `moderner_beta:infdev_420` terrain on a 0..127 envelope; 6 spawns; 5 drop rules; 6 direct
heals (bread 5, apple 4, stew 10, golden apple 42, cooked porkchop 8, porkchop 3); 6 smelts; no
hunger/sprint/XP/structures; clouds 108; flat sky; no colour maps; no weather.
**Cross-version rows that apply:** A-03, A-04, A-05, A-06, A-07, A-08, A-09, A-17, A-19.

No version-specific gap rows were found. The build is *before* ladders/doors (Infdev 20100607 [W20 Door]) and
before minecarts (Infdev 20100618 [W20 Minecart booster evidence]), so the early artifact tables' exclusion of
those recipes is correct; its big-tree saplings [W12] are what modern saplings do; and its debug-screen delta
("All"/"Counted", W12) is subsumed by A-06. The only open item is whether the level-generator change of this
build ("much less mountainous… more hills and plains", W12) is what `moderner_beta:infdev_420` actually
reproduces — that is the terrain authority's claim, not a rule this project can state (§8 item 11).

### 5.3 Infdev 20100611 (`historical_experience:infdev_611`)

**Actual.** same rules as 20100420 (6 spawns, 5 drops, 6 heals, no hunger/sprint/XP/structures); infinite
terrain; clouds 108; flat sky; no colour maps.
**Cross-version rows that apply:** A-03, A-04, A-05, A-06, A-08, A-09, A-17, A-19, A-20.

| Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|
| **IG-01** · world / terrain signature | "Monoliths now generate. This was fixed in Alpha v1.2.0" [W13]. | No monolith terrain; the preset is the clean Infdev one (A-20). | MISSING (documented bug) | LOW | verified |
| **IG-02** · world / spawn placement | "The player now always spawns on sand" [W13]. | No spawn-placement rule in facts; runtime uses modern world-spawn selection (biome/heightmap based), so the "always sand" rule is not modelled. | UNKNOWN | LOW | unknown — §8 item 8 |
| **IG-03** · visuals / item-form lighting | "The item forms of most blocks are now lit differently from before. The left and right sides have been noticeable darkened" [W13] (a specific change in *this* build). | Facts carry no item-model shading rule; the 1.21.1 item renderer is used. Whether that matches the post-20100611 shading cannot be told from the facts. | UNKNOWN | LOW | unknown — §8 item 10 |

Also correct here and worth recording: 3-D textured translucent clouds are the *right* state for this build
(A-07's boundary is exactly here) [W13].

### 5.4 Alpha v1.0.1 (`historical_experience:alpha_1_0_1`)

**Actual.** 6 spawns; modern drop tables; 6 heals; 268 allow-listed craft results with 182 refused; no
hunger/sprint/XP/structures/weather; old lighting/fog, 108 clouds, flat sky, Alpha stars, no tooltips, label
shown; left-click use.
**Cross-version rows that apply:** A-01, A-02, A-03, A-04, A-05, A-06, A-08, A-09, A-10, A-11, A-12, A-13,
A-14, A-15, A-16, A-17, A-19, A-20.

| Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|
| **101-01** · blocks / redstone rendering | "Redstone as a placed block has two power appearances; either completely on, or completely off" [W2]. | No redstone gate exists; the 1.21.1 wire renders 16 discrete power levels and its off state is the modern dark-red (which is itself a later Beta 1.5 fix: "Fully off redstone wire is no longer black" [W21]). | EXTRA / WRONG | LOW | verified (1.0.1) |
| **101-02** · blocks / redstone ore mining | "The mining time of redstone ore takes far longer compared to other ores (4.5 seconds with iron pickaxe)" [W2]. | 1.21.1 redstone-ore hardness/tool speeds give ≈0.75 s with an iron pickaxe. No hardness override exists. | UNKNOWN (whether the 4.5 s reflects a general old formula or this block's own hardness) | LOW | unknown — §8 item 5 |
| **101-03** · world / mushrooms | "Mushrooms: Generate anywhere. If generated in sunlight, eventually pop off. Don't pop off at night." [W2]. | Generation/decoration comes from the `moderner_beta:alpha` preset with feature exclusions; no mushroom-pop mechanic and no "generate anywhere" rule exists, so mushrooms behave as modern (dark niches, no sunlight popping). | WRONG | LOW | verified (expected) |

**Also sound:** tooltips absent before Beta 1.0 (`NO_ITEM_TOOLTIPS`); monster spawners do not drop when mined
(modern spawners drop nothing without Silk Touch, and Silk Touch does not exist) [W2]; lit furnaces drop a
furnace (modern) and are not obtainable as lit [W2]; redstone ore drops 4–5 dust, which matches modern
[W2]; compass and clock are wrongly *available* (A-10 — the crafting allow-list's `always` entry lets both
recipes work before the items existed). The doubled gold/diamond ore frequency of this build is already
recorded as not implemented (§6).

### 5.5 Alpha v1.0.4 (`historical_experience:alpha_1_0_4`)

**Actual.** as 1.0.1 (6 spawns, modern drops, 6 heals, 181 refused); snow/ice are correctly dated as present.
**Cross-version rows that apply:** A-01…A-17, A-19, A-20 (A-18 is 1.2.0+ only).

| Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|
| **104-01** · blocks / snow obtainability | "Snow … Spawns on top of most blocks when exposed to snowfall. **Cannot be obtained yet.** It can be melted by fire or lava." [W3]. | The mod applies no snow-layer drop rule: breaking a snow layer with a shovel in 1.21.1 drops snowballs, so snow layers are a farmable item in a 1.0.4 world. The era's later economy (1.0.6: 4 snowballs → block that drops 6 [W4]) only makes sense because snow is obtained from snow *blocks*, not layers. | EXTRA | MED | verified |
| world / winter mode | 1.0.4 is the Winter Update: "Upon world creation there is a 25% chance for it to become a winter world" with `SnowCovered=1` [W3]. | Not declared: coverage §5b records the reason (revisions are append-only improvements, winter is an alternative variant). Listed in §6, not re-litigated. | ~K | — | — |

**Also sound:** ice melts by fire/lava and is slippery (modern ice is slippery and melts) [W3]; the mod dates
snow/ice to this build (facts: 1.0.1 judges `snow` anachronistic, 1.0.4 does not), which is the correct direction.

### 5.6 Alpha v1.0.6 (`historical_experience:alpha_1_0_6`)

**Actual.** as 1.0.1 (6 spawns, modern drops, 6 heals, 180 refused); `SNOWBALL_SURPLUS` active for this build
only; boats craftable; cactus craftable.
**Cross-version rows that apply:** A-01…A-11, A-13, A-14, A-15, A-17, A-19, A-20 (A-12 starts at 1.0.14;
A-16 is 1.0.1/1.0.4 only; A-10's clock half still applies here).

| Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|
| **106-01** · blocks / cactus damage | "Player receives 1 damage when punching them, right clicking on them, or making steps on them" [W4]. | 1.21.1 cactus damages only on contact; punching or right-clicking a cactus does not hurt. (1.0.11 changes this to contact-only [W5], so exactly 1.0.6 needs the old rule.) | MISSING | LOW | verified |
| **106-03** · blocks / cactus placement | 1.0.6: "Generates anywhere with sand" [W4]; the restrictions "can now only be placed on sand" and "can no longer be placed directly next to blocks" are 1.0.11 changes [W5]. | Modern cactus placement rules (sand only, not adjacent to blocks) are applied because block placement is not version-gated. | EXTRA (too strict) | LOW | inferred |
| **106-02** · movement / boats | "They break if they are placed on the land, or if impact with the land is made. Can fit into a single block of water." [W4]; and the era has the old water lift ("boats rising quickly when submerged", in = Alpha v1.0.6, out = Beta 1.6 — feature-timeline row, https://minecraft.wiki/w/Boat), plus old destruction drops "3 oak planks and 2 sticks" [W20 Boat]. | No boat gate beyond the not-yet-ported `OLD_BOAT_DROPS`; 1.21.1 boat physics apply: boats do not break when placed/crashed on land, need more water room, have no old lift, and drop the boat item. The *drop* half is coverage §"Found and NOT fixed"; the *physics* half is new here. | WRONG | MED | verified (expected) / inferred (runtime) |

**Known, not re-listed (§6):** leaf decay removal, 10 %→5 % sapling, snow-block economy. Note the snow-block
surplus itself **is** implemented (`SNOWBALL_SURPLUS`, facts), even though `LegacyCatalog`'s evidence string still
says all three are unimplemented — the code moved on and the string did not. That is a documentation defect, not
a behaviour gap.

### 5.7 Alpha v1.0.11 (`historical_experience:alpha_1_0_11`)

**Actual.** 8 spawns (cow added; chicken still absent); modern drops; 6 heals; 176 refused; clay/sugar cane
allowed; slime allowed.
**Cross-version rows that apply:** A-01, A-02, A-03, A-04, A-05, A-06, A-08, A-09, A-10 (clock), A-11, A-13,
A-15, A-17, A-19, A-20 (A-12 stops at 1.0.11; A-14 stops here because cows exist and the modern leather recipe is
what the post-1.0.8 game used).

| Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|
| **1011-01** · mobs / slime spawn height | "Slime … Spawn only below Y=32" [W5]. | Slime is on the allow-list, but there is no Y rule in `HistoricalSpawnRules`; the modern slime spawner (slime-chunk / swamp rules) decides, so slimes spawn in the wrong places (including surface swamps) and not in the historical underground band. | MISSING / WRONG | MED | verified (expected) / inferred (runtime) |
| **1011-02** · mobs / slime splitting | "Splits only when killed by the fist and not with swords." [W5]. | Modern slimes split from any lethal damage; no gate exists. | WRONG | LOW | verified (expected) |
| **1011-03** · UI / creative catalogue date | Slimeballs exist from this build: "Slimeball: Dropped by the smallest slimes, however, it currently has no use." [W5]. | `HistoricalCreativeCatalog` dates `slime_ball` `from("alpha_1_0_17", …)`, so creative in 1.0.11/1.0.14 cannot grant it even though small slimes drop it (survival drops are the vanilla slime table and do work). | WRONG (catalogue) | LOW | verified |

**Also sound:** clay drops 4 clay balls and the block is craftable from 4 (modern 4:4) [W5]; sugar cane grows
to 3 (modern) [W5]; bookshelf/bricks recipes exist from here (the crafting allow-list dates paper/book/bricks at
1.0.11 [W22]) [W5]; milk buckets work through the modern milk interaction, and cows are only in the spawn set
from this build, so no earlier leak [W5].

### 5.8 Alpha v1.0.14 (`historical_experience:alpha_1_0_14`)

**Actual.** 8 spawns (chicken replaces slime); modern drops; 6 heals; 173 refused; jukebox, both minecart variants
and `music_disc_13/cat` in the creative catalogue from here; `SLIME` absent from the spawn set (correct).
**Cross-version rows that apply:** A-01, A-02, A-03, A-04, A-05, A-06, A-08, A-09, A-10 (clock), A-11, A-13,
A-15, A-19, A-20 (A-12 no longer applies).

| Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|
| **1014-01** · mobs / creeper disc drop | Discs are found "in dungeon chests" or "when a creeper is killed by a skeleton" — and only 13/cat exist in this build [W6]. | Vanilla creeper loot supplies *any* of the 1.21.1 discs on a skeleton kill, because the modern table is unmodified (A-01). For 1.0.1–1.0.11 the same mechanism is a pure EXTRA (discs did not exist). | EXTRA | LOW | inferred |
| world / dungeon frequency | "Dungeons: Now generate twice as much, due to the number of iterations being changed from 4 to 8" [W6]. | Monster rooms generate (they are a placed feature in the Alpha preset), but the iteration count cannot be expressed through the generator — facts' evidence records this. | ~K | — | — |

**Also sound:** chickens fall slowly and lay eggs on the modern 5–10 min cadence [W6]; obsidian blast
resistance 1200 matches modern [W6]; furnace minecarts work and are craftable from here [W6]; arrows are flint
from 1.0.14 (A-12 stops); jukebox craftable (creative/recipe list dates it here) [W6].

### 5.9 Alpha v1.0.17 (`historical_experience:alpha_1_0_17`)

**Actual.** 9 spawns (slime back); modern drops; 6 heals; 173 refused; fence craftable.
**Cross-version rows that apply:** A-01, A-02, A-03, A-04, A-05, A-06, A-08, A-09, A-10 (clock), A-11, A-13,
A-15, A-19, A-20.

| Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|
| **1017-01** · inventory / starting kit | "Hotbar … The player starts with 64 fences in the inventory" [W7] (also "I started with 64 fences"). | No starting-inventory behaviour for any profile was found (no grant in the server lifecycle, and the creative catalogue is unrelated); a fresh 1.0.17 world starts empty. | MISSING | LOW | verified |

**Also sound:** spider jockeys become correct from this build (A-17 stops); chickens' slow fall is modern;
fence recipe is available from this build (crafting allow-list dates `oak_fence` at 1.0.17 — the artifact
tables for earlier builds correctly lack it) [W7]; milk bucket can be emptied (modern bucket behaviour) [W7].

### 5.10 Alpha v1.1.0 (`historical_experience:alpha_1_1_0`)

**Actual.** 9 spawns; modern drops; 6 heals; 173 refused; sneak allowed from this build; compass craftable.
**Cross-version rows that apply:** A-01, A-02, A-03, A-04, A-05, A-06, A-08, A-09, A-10 (clock), A-11, A-13,
A-15, A-19, A-20.

| Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|
| **110-01** · movement / sneaking | "v1.1.1 … Added sneaking." [W19] — i.e. the shift key did nothing in v1.1.0. | `Feature.NO_SNEAKING` ends at `Milestones.ALPHA_1_1_0` ("the project owner's correction is the best evidence available"), so 1.1.0 can sneak. Off by one build; the declared-version list has no 1.1.1, so the practical effect is only on this profile. | EXTRA | LOW | verified |
| **110-02** · visuals / monster spawner render | "Monster spawner: No longer renders the model of the mob inside of them." [W8] — a change *in this build*, so earlier builds rendered it and this one did not. | No spawner-render gate; 1.21.1 renders the spinning mini-mob inside spawners, so every Alpha world from here on shows a mob that the build had removed. | EXTRA | MED | verified |

**Also sound:** compass is correctly craftable from 1.1.0 (A-10's compass half ends here) [W8]; spawner model
rendering was correct before 1.1.0 by accident (modern renders it [W8]).

### 5.11 Alpha v1.1.2_01 (`historical_experience:alpha`)

**Actual.** 9 spawns; modern drops; 6 heals; 173 refused; flat sky; no colour maps; last build of the old terrain
generation; `ALPHA_STARS`.
**Cross-version rows that apply:** A-01, A-02, A-03, A-04, A-05, A-06, A-08, A-09, A-10 (clock), A-11, A-13,
A-15, A-19, A-20, plus 110-02 (spawner rendering) inherited.

No further version-specific rows: this build is "the last version to feature the old terrain generator along
with the bright green grass", the last with winter mode and the last with monoliths [W9] — the terrain model is
the known approximation (§6) and the monoliths are A-20. Resource downloads, sound loading and skins are
technical changes outside the emulation surface.

### 5.12 Alpha v1.2.0 (`historical_experience:alpha_1_2_0`)

**Actual.** 10 spawns (ghast, zombified piglin added; slime removed from spawning); modern drops; 6 heals;
170 refused; Nether allowed; copper-free old lighting retained; `FLAT_SKY_COLOUR`; colour maps active from here;
`NO_NETHER` ends here.
**Cross-version rows that apply:** A-01, A-02, A-03, A-04, A-05, A-06, A-08, A-09, A-11, A-13, A-15, A-18,
A-19, plus 110-02 inherited.

| Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|
| **120-01** · blocks / glowstone drops | "Glowstone … Drops 1 glowstone dust per block." [W1]. | 1.21.1 glowstone loot is 2–4 dust per block, and no block-loot rule covers glowstone (`HistoricalMobLootHelper` and the ore/leaf mixins are the only loot replacements; the block-drop gate only covers ore self-drops and the 1.0.6 snowball quirk). | WRONG | MED | verified |
| **120-02** · items / fishing mechanics | "Fishing … Each tick when a fishing bobber is in water, it has a 1/500 chance of catching a fish. Currently the only item that can be obtained through fishing is raw fish." [W1]. | `OLD_FISHING_LOOT` gates the *loot* to raw fish, but the 1.21.1 fishing *mechanic* remains: open-water requirement, 5–30 s weighted wait, bobber/particle/… The era's 1/500-per-tick roll in any water block is not implemented (facts' 1.2.0 evidence calls the rate "unverified" and does not implement it). | WRONG | MED | verified (expected) / inferred (runtime) |
| **120-03** · world / leaf decay | "v1.2.0 preview … Leaves no longer decay." [W18]. | No leaf-decay gate exists (the known unimplemented item is only 1.0.6); 1.21.1 leaves decay, so 1.2.0's trees shed their leaves and drop saplings/sticks (A-15) where they historically stayed. | WRONG | MED | verified |
| **120-04** · world / lava in the Nether | 1.2.0 is *before* the 1.2.2 change: "Lava: Now flows 8 blocks in the Nether. Before, it flowed the same distance as it does in the Overworld." [W10]. | No lava-spread rule; 1.21.1's hard-coded Nether spread applies. Note that at most one of 1.2.0/1.2.2a can match modern — the pair is recorded together in 1222-01. | UNKNOWN | LOW | unknown — §8 item 12 |
| **120-05** · mobs / spawn light depth | "Mobs: General: Higher lighting values are required the deeper they are to keep mobs from spawning." [W1]. | Spawn conditions are modern (A-19): 1.21.1 uses a fixed light threshold, not a depth-scaled one, so underground density differs from this build. | WRONG | MED | inferred |
| **120-06** · world / pumpkin generation | "Pumpkin … New plant found in patches in the overworld." [W1] and the jack-o'-lantern recipe needs one [W1]. | The 1.2.0 terrain is the 1.1.2-era preset (§6 approximation). Whether `patch_pumpkin` still runs on it is not visible in the facts (no pumpkin exclusion exists in `HistoricalFeatureExclusions`), so pumpkins are plausibly present but unverified. | UNKNOWN | LOW | unknown — §8 item 13 |
| **120-07** · world / 1.2.2 bug fixes | The per-tick hurt sound, damage-while-recovering, and fire-immune pigmen/ghasts are 1.2.2 *fixes* [W10], so 1.2.0 had those bugs. | Modern code has the fixed behaviour; reproducing the 1.2.0 bugs is not attempted. | MISSING (bug not reproduced) | LOW | verified |

**Known, not re-listed (§6):** the twelve-biome terrain and biome-coloured terrain (W1) come from the known
`moderner_beta:alpha` approximation; the modern Nether shape is owner-accepted; the 4→8 dungeon iteration note
does not apply to this build (it starts at 1.0.14 and the count is not modelled).

### 5.13 Alpha v1.2.2a (`historical_experience:alpha_1_2_2a`)

**Actual.** identical rules to 1.2.0 (10 spawns, modern drops, 6 heals, 170 refused); `F4_PORTAL_KEY` is active
for exactly this profile.
**Cross-version rows that apply:** A-01, A-02, A-03, A-04, A-05, A-06, A-08, A-09, A-11, A-13, A-15, A-18,
A-19, 110-02, and 120-03 (leaf decay — the same rule as 1.2.0).

| Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|
| **1222-01** · world / lava flow in the Nether | "Alpha v1.2.2 … Lava: Now flows 8 blocks in the Nether." [W10]. | No lava rule; 1.21.1's Nether spread applies. Paired with 120-04: the 1.2.0/1.2.2a difference means at most one of the two builds can be right by accident. | UNKNOWN | LOW | unknown — §8 item 12 |
| **1222-02** · UI / main-menu version | "Main menu: … Now displays the version name in the top-left corner." [W10]. | The corner label implemented in this project is the *in-game* label (`HistoricalVersionLabel`, `VERSION_LABEL`); nothing in the facts says the **main menu** shows the version string, and the mod has its own main menu/version selector. | UNKNOWN | LOW | unknown — §8 item 14 |

**Also sound:** the F4 debug portal key is exactly right for 1.2.2a and absent from 1.2.0, which the project
hash-checked and gated as an exact build (`F4_PORTAL_KEY`) — a good example of the exact-build pattern working;
texture-pack support is present through the project's own pack system [W10]; the 1.2.2 fixes (fire-immune
pigmen/ghasts, damage-while-recovering, per-tick hurt sound) are what modern code does [W10]; leaf decay remains
wrong (120-03).

---

## 6. Known gaps already recorded accurately in `research/implementation-coverage.md` (not re-listed)

Quoted from that file so it is clear these are deliberately excluded from the row count above:

1. **Nether shape is modern 1.16+** and the owner has accepted it (coverage §12, closed as deliberate).
2. **Alpha 1.2.0 terrain and its twelve biomes** are approximated by `moderner_beta:alpha` (coverage §13); this
   audit treats that as the governing statement for biome/decoration fidelity in 1.2.0/1.2.2a.
3. **Leaf decay removal and the 10%→5% sapling cut (Alpha 1.0.6), the snow-block economy, the 4→8 dungeon
   iterations (1.0.14)** are in the coverage §14 "unimplemented behaviours" list. (This audit adds the *other*
   leaf-decay window — 1.2.0/1.2.2a, 120-03 — which that list does not cover.)
4. **The pre-Beta-1.8 spawning algorithm/caps were implemented and removed at the owner's request**, with the
   research preserved (coverage §6 item 4). A-19 adds only the observation that the *remaining* modern spawner
   brings modern weights/conditions with it; it does not reopen the removal decision.
5. **Smelting, stonecutting, smithing, brewing and trading are not gated** (coverage, "Limits, stated so they
   are not discovered"). Every smelt that could leak in these versions is either later content or matches
   history, so no new row is claimed — but it is tracked in §8 item 5.
6. **Winter mode (1.0.4)** is not declared, with the append-only-revision reason recorded (coverage §5b).
7. **Ore distribution for these builds is not researched**; the default revisions carry no ore rules and the
   `_era_ores` revisions are experimental (facts' evidence says so per version; coverage §3).
8. **The 0..127 envelope / modern Nether / dimension-type height disagreements** are coverage §3 and §12.

---

## 7. Top 10 highest-impact gaps in this era

Ranked by what a player actually experiences in a survival playthrough of these builds, not by row count.

1. **A-01 — mob drops are the modern 1.21.1 tables across every Alpha profile (HIGH, verified).** Zombies
   drop rotten flesh instead of feathers (feathers are the era's arrow-and-bed currency), skeletons drop bones
   (Beta 1.2) and spiders drop eyes and chickens drop meat (Beta 1.8), cows drop beef, and creeper/skeleton
   kills can produce any modern music disc. This is the single largest content error in the era: it changes
   early-game progression and makes "old Minecraft" feel like modern Minecraft with old textures.
2. **A-14 — cloth/leather armour cannot be crafted in Alpha 1.0.1, 1.0.4 and 1.0.6 (HIGH, verified).**
   Armour of that tier was made from cloth until 1.0.8 [W17], and cows (leather) do not exist until 1.0.8; the
   mod allows only the modern leather recipe, so those three builds have **no craftable cloth/leather armour at
   all** (iron/gold/diamond armour still work because their ores exist).
3. **A-03 — modern goal-based pathfinding AI (HIGH, verified).** Zombies and skeletons navigate obstacles and
   chase through terrain in builds that had simple AI; the 1.2.1 AI improvement is the documented boundary.
   This changes combat, mob farms and shelter behaviour in every one of the 13 profiles.
4. **A-02 — sheep drop wool on death (MED, verified).** In these builds wool came from punching/shearing a
   sheep; killing it yielded nothing (Beta 1.7 adds the death wool). Wool is doubled up here, which undercuts
   the era's wool economy and makes beds/books/paintings easier.
5. **A-06 — the modern F3 debug overlay, including coordinates, in all 13 versions (MED, verified).**
   Coordinates were only added in Alpha v1.2.3, and before v1.1.1 the debug screen was an options toggle rather
   than F3. The overlay also exposes biome/light/spawn-cap information these builds never showed.
6. **A-10 — compass and clock are craftable too early (MED, verified).** Compass is a 1.1.0 item and the clock
   a 1.2.0 item; the crafting allow-list's `always(...)` entry contradicts the project's own creative catalogue,
   which dates both correctly.
7. **A-11 — stone slab is the wrong block and the wrong recipe throughout Alpha (MED, verified).** The era's
   stone slab (`smooth_stone_slab`) is refused as "Beta 1.3" while the 1.14 `stone_slab` is craftable from
   3 stone → 6, instead of the 3 cobblestone → 3 of the era. Common building block, wrong availability *and*
   wrong item.
8. **1011-01 — slimes obey modern spawn rules, not "below Y=32" (MED, verified).** Slime spawn location is one
   of the era's most recognisable mechanics and the mod has no Y rule at all.
9. **110-02 — monster spawners render the mob inside them from Alpha v1.1.0 onward (MED, verified).** The
   change that removed the miniature is *in* 1.1.0 [W8], so every later profile shows a mob that build had
   already hidden; spawners are common in Alpha dungeons.
10. **120-01 / 120-03 / 120-02 — the 1.2.0 Halloween-Update trio (MED, verified/inferred): glowstone drops
    2–4 dust instead of 1, leaves decay in a build where they did not until v1.2.3, and fishing uses the modern
    open-water 5–30 s mechanic instead of the 1/500-per-tick roll.** Each is a distinct, testable behaviour of
    the Nether/biome/fishing update that the era is remembered for.

Honourable mentions (all LOW/MED and recorded): boat physics/old water lift (106-02), snow layers obtainable in
1.0.4 (104-01), spider jockeys before 1.0.17 (A-17), flint-tipped arrows before 1.0.14 (A-12), 64 starting
fences in 1.0.17 (1017-01), redstone wire's 16-level rendering in 1.0.1 (101-01).

---

## 8. What I could not verify, and exactly what would settle each

1. **Audio (music, ambient, the old damage sound, disc playback).** No audio line exists in the generated
   facts and I did not open the resource packs. Settle with: the Golden Days/Programmer Art `sounds.json` and
   music file lists, plus a check that `SoundManager` playback is (or is not) keyed by profile — then a
   client-join play test for 1.0.14 music (piano1–3), 1.2.0 tracks (hal1–4, nuance1–2) and the pre-1.2.0 "ough"
   damage sound [W1][W6].
2. **Mob spawn weights, pack sizes, caps and light thresholds.** `HistoricalSpawnRules` says they are
   unestablished; the only hook is the species filter. Settle by reading the spawn constants out of the three
   era client jars (the project already holds `in-20100223.jar`, both Infdev jars and
   `alpha-1.1.2_01-client.jar`, and `pre-beta-1.8-spawning-evidence.json` already has caps for Infdev).
3. **Mob melee damage values and the player armour formula (A-04, A-05).** No source or artifact in the repo
   gives the numbers. Settle with the era clients' `EntityLiving.damageEntity` / mob attack constants (the same
   decompilation route that produced the drop and food tables), or the wiki `Armor` "before 1.9" formula page
   plus a Beta 1.7.3 comparison.
4. **The Alpha food table's missing fish entries.** facts print six direct heals per Alpha profile including
   1.2.0/1.2.2a, yet raw fish (+2) and cooked fish (+5) are 1.2.0 additions [W1];
   `PerVersionFoodHealTest.alphaDoesNotHealFoodThatDidNotExistYet` asserts `researchedHeal("cod")` is *null* for
   the alpha profile, so `directHealAmount` falls through to modern nutrition, which happens to be 2/5. So
   behaviour is right, the declared table and the evidence string ("no edible item is added between them") are
   wrong. Settle by adding cod/cooked cod to `ALPHA_1_2_0`'s table from the era jar (already available) rather
   than relying on the modern coincidence — a documentation/robustness fix, not a behaviour change.
5. **Smelting is ungated.** Every smelt in these versions *should* be one of the six/eight era smelts, but
   nothing enforces it. I did not find a *demonstrated* leak because the leaking results are gated items, but
   a modern-only smelt reaching an early world (e.g. a log→charcoal route, if any such recipe exists for the
   era's items) would not be stopped. Settle by extending the early-smelt table per profile (the Indev/Infdev
   exact tables already exist; Alpha/Beta have no equivalent artifact extraction) and probing the furnace menu.
6. **51 of 145 blocks are undated for these versions** (facts print "51 unknown of 145"). Those blocks are
   allowed by construction. Settle by extracting the item/block registry of each era jar (the project's
   `scripts/EarlyGameplayExtractor.java` route) and dating them against the era id table.
7. **Height/bedrock details.** Sea level and the 0..127 envelope are stated for Infdev/Alpha, but I found no
   line establishing the era's bedrock-layer pattern or exact sea level for the three phases; the mod inherits
   whatever the Moderner Beta presets do. Settle with a chunk-column probe on each preset (bedrock layers,
   water surface) against the era jars.
8. **Player-spawn placement rules** (Infdev 20100611 "always spawns on sand" [W13]; Alpha spawn behaviour).
   Settle by probing world-spawn search in a 20100611 profile and reading the era client's spawn routine.
9. **Indev world-creation options (themes/map sizes).** The 20100223 wiki page is a delta page; the mod offers
   only the "normal" theme, 256×256. Settle with the Indev 0.31 world-creation page or the 20100223 client's
   level options; if other themes existed they are a MISSING world-type feature, not a block bug.
10. **Infdev 20100611 item-form shading.** The build changed the 3-D item lighting ("left and right sides
    noticeably darkened" [W13]); I could not tell from the facts whether the 1.21.1 item renderer matches it.
    Settle with a client screenshot against the wiki's comparison images from the 20100611 page.
11. **The terrain authority's claim for Infdev 20100420** ("much less mountainous… more hills and plains" [W12]).
    That is Moderner Beta's preset claim; verifying it is a generation-probe question, not a rule I can assert.
12. **Nether lava spread distance (120-04 / 1222-01).** I could not establish whether 1.21.1 spreads Nether lava
    4 or 8 blocks from the facts. Settle by reading `LavaFluid`'s spread distance in the pinned 1.21.1 sources,
    which decides whether 1.2.0 or 1.2.2a matches.
13. **Pumpkin (and other 1.2.0 additions) world generation.** Whether the 1.1.2-era preset still runs
    `patch_pumpkin` is not visible without a chunk probe. Settle by sampling generated 1.2.0 chunks for
    `minecraft:pumpkin` and `minecraft:jack_o_lantern` (the same probe style the project already uses).
14. **Whether the 1.2.2 main menu shows the version string** as the wiki says [W10], and how the project's
    replacement menus compare. Settle with a client screenshot of the 1.2.2a main menu.

Also unverified by play (no runtime in this task, and Gradle was not run): every "runtime" claim above is a
code-path reading of the classes named in §1 W22, which is the same style the project's own probe would
confirm. No code, test, docs or other research file was modified; only this file was created.
