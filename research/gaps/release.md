# Release-era gap audit: Release 1.0 through Release 1.6.4

Audited 2026-10-21 by `gaps-release` (task-4). Scope: the seven declared Release profiles —
`release_1_0_0`, `release_1_1`, `release_1_2_5`, `release_1_3`, `release_1_4`, `release_1_5`, `release_1_6_4`.

**ACTUAL side:** `research/version-facts.md`, generated from the code by `VersionFactsTest` (last generated
2026-10-21). It is read-only for this task and was not edited. Where `version-facts.md` does not expose a
behaviour (for example a block-level refusal that is separate from the item-level refusal list), I read the
source named in the row and marked the row **(C)**. No code, tests or other research files were changed and
Gradle was not run.

**EXPECTED side:** `minecraft.wiki` version pages fetched with `?action=raw` (and section-scoped raw fetches),
plus the section-scoped history tables of the wiki's feature pages. Every expected claim below carries a
verbatim quote and the URL it came from. Where no source settles a value it is marked **UNKNOWN** and the
settling evidence is named.

Additional research used: `research/feature-timeline.json` (wiki quotes + URLs per boundary),
`research/version-boundaries.json`, `research/version-transitions.json`,
`research/implementation-coverage.md`, `research/ore-height-ranges.json`, `research/historical-features.json`,
`research/creative-menu-catalog.json`.

---

## 0. Conventions

| Field | Values |
|---|---|
| Gap type | **MISSING** = the version had it, the mod does not; **WRONG** = the mod has it with a different value/behaviour; **EXTRA** = the mod has content/behaviour the version lacked; **UNKNOWN** = cannot decide from evidence |
| Severity | **H** = a player cannot do something the era allowed / gets an era-defining behaviour wrong; **M** = clearly visible divergence, workaround exists; **L** = cosmetic or rarely reached |
| Confidence | **H** = artifact/source-backed and checked against code; **M** = single source or inference; **L** = note only |
| Row evidence | (F) visible in `version-facts.md`; (C) read from the named source; (F+C) both |

Two source-selection facts matter for interpreting the rows:

1. `version-facts.md`'s "Crafting: n allow-listed ids, m refused" line is only the **item-level** gate
   (`HistoricalCraftingEra.isRefused`). A block-level gate runs *after* it, and it can refuse a result whose
   item is allow-listed. `HistoricalRecipeGate.isBlockUnavailable`
   (`src/main/java/net/historicalexperience/gameplay/HistoricalRecipeGate.java:93-100`) refuses only when the
   block's boundary is `timeline()`-dated (or undated), **not** when it is palette-derived. So the
   "Blocks judged anachronistic" line in `version-facts.md` is broader than what actually refuses a craft;
   e.g. `sandstone_stairs` is *judged* anachronistic for Release 1.3 but is **not** refused, while
   `ender_chest` is both judged and refused. Rows that rely on the block gate say so **(C)**.
2. `Feature.java`'s windows are the *code's* boundaries and several are documented proxies. This audit
   reports the historical disagreement and says when the proxy changes the answer (task instruction).

### Proxy map (what each declared version is compared against here)

| Declared | Stands for | Why / source |
|---|---|---|
| Release 1.0 (`release_1_0_0`) | 1.0.0, 18 Nov 2011 | exact build in the id |
| Release 1.1 (`release_1_1`) | 1.1, 12 Jan 2012 | exact build in the id |
| Release 1.2.5 (`release_1_2_5`) | the 1.2 line: 1.2.1 content + 1.2.5 fixes | id follows the line's last patch; additions live on the 1.2.1 page |
| Release 1.3 (`release_1_3`) | **1.3.1**, the only 1.3 release client | `research/version-transitions.json` labels the profile "1.3.1 stable baseline"; `HistoricalBlockAvailability` repeats that label in code |
| Release 1.4 (`release_1_4`) | **1.4.2**, the Pretty Scary Update | code comments in `Feature.java` (`LEFT_CLICK_USE`, `BOSS_HEALTH_CAPTION`) and `HistoricalEnchanting` treat Release 1.4 as 1.4.2. The 1.4 *line* runs to 1.4.7, and 1.4.6 content (fireworks, enchanted books, Thorns, 3D dropped items) is therefore out of scope under this proxy — see R14-04 |
| Release 1.5 (`release_1_5`) | 1.5 (1.5.2 is the patch level) | id; content on the 1.5 page |
| Release 1.6.4 (`release_1_6_4`) | the 1.6 line, i.e. 1.6.1 + 1.6.2 + 1.6.4 | `Feature.NO_BABY_ZOMBIES` etc. already use 1.6.4 as the proxy past 1.6.2; the 1.6.1 page holds the line's additions and the 1.6.4 page only the final fixes |
| all | world **height**: 1.0/1.1 = 128 blocks (0..127); 1.2.5 onwards = 256 (0..255) | 1.2.1 page: "Height limit increased from 128 blocks to 256 blocks" |

---

## 1. Cross-version findings (affect several declared versions)

| # | Domain | Expected (quote + URL) | Actual | Gap | Sev | Conf |
|---|---|---|---|---|---|---|
| X-01 | UI — creative menu for 1.3+ | 1.3.1 "splits the [[Creative inventory]] into separate sections"; "There are categorized tabs for various groups of items and blocks. Added a search tab … Added 'Survival Inventory' tab". [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | Release 1.3–1.6.4 fall back to the **unmodified 1.21.1** `CreativeModeInventoryScreen` (`client/LegacyCreativeScreen.java:35-36`); every 1.21.1 block/item is shown in the tabs. Taking an out-of-era stack is refused afterwards by `HistoricalCreativeGrants.allowed` (catalog window). So the era's *tabbed layout* is right but the *catalog shown* is modern. (C) | EXTRA | M | H |
| X-02 | UI — recipe book | Recipe book "Added the recipe book" in 1.12 (17w13a). [Recipe_book](https://minecraft.wiki/w/Recipe_book) | `NO_RECIPE_BOOK` is active for all seven profiles (facts: "NO_RECIPE_BOOK" in every active list) — hidden correctly. | *no gap* | — | H |
| X-03 | Visuals — void fog | 1.1 "Removed the void fog in creative mode. Removed the void fog in the nether and the end." [Java_Edition_1.1](https://minecraft.wiki/w/Java_Edition_1.1) — i.e. 1.0 had it everywhere and later 1.x had it in the survival Overworld until 1.8 | No void-fog implementation exists (no matching `Feature`, no client mixin; `HistoricalOldLighting`/`HistoricalWorldTweaks` do clouds and light only). The facts' visual profile reports `old lighting false; … fog false` for all releases. (F+C) | MISSING | L | M |
| X-04 | World — The End beyond the island | 1.0 End is one island with the dragon, pillars/crystals and the exit portal ("Spawns only once in [[the End]]", "A 'Boss health' bar appears…"). [Java_Edition_1.0.0](https://minecraft.wiki/w/Java_Edition_1.0.0) | All release presets use `minecraft:the_end` with vanilla 1.21.1 noise generator (`world_preset/release_*` files), so the 1.9 outer islands and chorus generate. End cities are suppressed by the structure allow-list, and End gateways have their own special handling
(`HistoricalFeatureExclusions.END_GATEWAY_RETURN`, "not a per-revision exclusion on purpose"), so the outer islands are
unreachable in survival. (C) | EXTRA | L | M |
| X-05 | World — desert villages | Desert villages are Release 1.3.1: "Added desert-specific villages." [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | `HistoricalVillageStructure` picks the desert style purely from the biome (`HistoricalVillageRules.isDesert(biomeId)`, `worldgen/HistoricalVillageStructure.java:216-221`) with **no version gate**, so Release 1.0/1.1/1.2.5 villages generate in deserts too. (C) | EXTRA | M | H |
| X-06 | XP — level curve | 1.3.1 "Adjusted experience level progression: Levels 1–15 cost 17 XP points each; Levels 16–30 cost 3 more XP points than the previous … Levels 31–38 cost 7 more XP points than the previous". [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | No XP-curve code exists (no `Feature`, no `Experience` mixin; `ServerLevelHistoricalOrbMixin` only suppresses orb pickup before Beta 1.8). All seven profiles use the unmodified 1.21.1 curve (7, 9, 11 …), which is not the 1.3.1 curve. (C) | WRONG | M | M |
| X-07 | XP/enchanting — enchanted books | 1.4.6 "introduced [[fireworks]], [[enchanted book]]s, and the [[Thorns]] enchantment". [Java_Edition_1.4.6](https://minecraft.wiki/w/Java_Edition_1.4.6) | No gate on enchanting a book at the enchanting table (modern behaviour for every profile). So Release 1.0–1.5 worlds produce enchanted books; the 1.4.6 content is allowed in 1.0–1.5. (C) | EXTRA | M | H |
| X-08 | Status effects — pre-1.6.1 strengths | 1.6.1 "[[Instant Health]] nerfed by 33% (heals only 4 health points/2 hearts at level I). [[Regeneration]] nerfed by 50% (takes 50 ticks/2.5 seconds to restore 1 health point/.5 heart at level I)." [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | No effect-strength gating exists (grep of `Feature.java` and `mixin/**`: no potion/effect-strength hook). 1.0–1.5 therefore use the post-1.6.1 (modern) values: Instant Health I heals 4, Regeneration I restores 1 HP/50 ticks. (C) | WRONG | M | H |
| X-09 | Crafting — recipe quantities and stack sizes | 1.2.1 "Crafting recipe for slabs now yields 6 instead of 3. Can now be placed upside-down"; "Ladders: … Crafting recipe now yields 3 ladders instead of 2". 1.3.1 "Signs: Stack up to 16 now. Crafting now gives 3 signs instead of 1."; "Empty buckets: Stack up to 16 now." [1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) / [1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | `HistoricalCraftingEra` only **refuses** results; nothing rewrites recipe outputs, counts or stack sizes. All profiles use modern counts: 6 slabs/stairs, 3 ladders, 3 signs, buckets stack 16. (C) | WRONG | M | H |
| X-10 | Health/hunger — natural regeneration model | 1.6.1 "[[Hunger]]: Natural health regeneration now costs hunger/saturation points." [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) — so 1.0–1.5 had the Beta 1.8 model (food ≥ 18, no saturation cost) and 1.6.1+ has the saturation-costing model. | `BETA_REGENERATION` window is `from(BETA_1_8)` with no upper bound (`version/Feature.java:224-226`), so **Release 1.6.4 uses the Beta model** (threshold 18, 1 HP / 80 ticks, no saturation charge, no fast regen). Facts: `BETA_REGENERATION` in 1.0–1.6.4 active lists. (F+C) | WRONG (1.6.4 only) | M | H |
| X-11 | World — terrain foundation for 1.3/1.4/1.5 | 1.3.1/1.4.2/1.5 are separate generator eras? 1.4.2 "no major terrain generator change" is not on the wiki page (the code comment says the research states it); 1.3.1 *did* change world generation: "Biomes have more varied elevations…", "Increased the scale and depth of some biomes", "Jungle trees: Made cocoa beans appear…", "Lava now replaces air below Y=11". [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | Release 1.3/1.4/1.5 generation recipes are built **from `moderner_beta:release_1_2_5`** (facts, "Generation recipes" line for each), i.e. the 1.2.5-era terrain noisy generator is reused. The code comment in `LegacyCatalog` justifies this with "the generator did not change between 1.2.5 and 1.7"; the 1.3.1 page contradicts the terrain part (biome scale/depth, lava level). Impact confined to terrain shape, not content. (F+C) | UNKNOWN | M | M |
| X-12 | Inventory — offhand | Offhand slot is Release 1.9. | `NO_OFFHAND` active for all seven (facts); no offhand in survival. (F) | *no gap* | — | H |
| X-13 | Combat — attack cooldown / sweep | Added 1.9 (15w34b/15w34c). [Melee_attack](https://minecraft.wiki/w/Melee_attack) | `NO_ATTACK_COOLDOWN` / `NO_SWEEP_ATTACK` active for all seven (facts). | *no gap* | — | H |

---

## 2. Release 1.0 (`historical_experience:release_1_0_0`) — compared with 1.0.0

ACTUAL rules (facts): hunger, sprinting, experience, structures; 20 spawns; "drops modern"; no direct food
healing; 17 enchantments; 268 allow-listed ids / 76 refused; structures restricted to
`historical_village, fortress, mineshaft, stronghold`; world rules weather/villages/left-click-use/sprinting all
true; finite world false. Expected 1.0.0 [Java_Edition_1.0.0](https://minecraft.wiki/w/Java_Edition_1.0.0).

| # | Domain | Expected (quote + URL) | Actual | Gap | Sev | Conf |
|---|---|---|---|---|---|---|
| R10-01 | Blocks/items — chiseled stone bricks | Added in 1.2.1: "Chiseled stone bricks — Only available in Creative mode." [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | Allow-listed from Beta 1.8.1 (`HistoricalCraftingEra.java:143-145`), not refused in 1.0/1.1, not block-dated → craftable in a 1.0 world (needs stone bricks, which 1.0 has). (C+F) | EXTRA | L | H |
| R10-02 | Crafting — slabs/stairs/ladders | 1.0 yields 3 slabs/stairs, cannot place them upside-down, ladder recipe yields 2 (all changed in 1.2.1): "Crafting recipe for slabs now yields 6 instead of 3. Can now be placed upside-down"; "Crafting recipe now yields 3 ladders instead of 2". [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | Modern recipes: 6 per craft, top-slab/top-stair placement allowed, 3 ladders. No recipe-count layer. (C+F) | WRONG | M | H |
| R10-03 | Inventory — signs and buckets | 1.3.1 "Signs: Stack up to 16 now. Crafting now gives 3 signs instead of 1."; "Empty buckets: Stack up to 16 now." [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | Modern: 3 signs per craft, signs stack 16, buckets stack 16. (F+C) | WRONG | L | H |
| R10-04 | Crafting — glistering melon slice | 1.0.0 "[[Glistering Melon Slice|Glistering Melon]] — Currently crafted with only 1 [[gold nugget]] and 1 [[melon slice]]." [Java_Edition_1.0.0](https://minecraft.wiki/w/Java_Edition_1.0.0); changed in 1.6.1: "[[Glistering melon]] — Now requires 8 gold nuggets." [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | Modern recipe (8 gold nuggets) for every profile; no recipe-shape gating. (C) | WRONG | L | H |
| R10-05 | Crafting/smelting — nether brick item is 1.5 | "[[Nether Brick|Nether Brick (item)]] — Obtained by smelting [[netherrack]]. Used to make [[Nether bricks]]" and "The crafting recipe is 4 [[Nether brick]] items" — both stated as **1.5 additions**. [Java_Edition_1.5](https://minecraft.wiki/w/Java_Edition_1.5) | Modern smelting `netherrack → nether_brick` exists and `nether_bricks` is allow-listed from Beta 1.9 Pre3, so 1.0–1.4 can smelt the 1.5 item and craft the block from it. (C) | EXTRA | M | M |
| R10-06 | Mobs — drops ("drops modern") | 1.0 drops: zombie drops 0–2 feathers only (rotten flesh is Beta 1.8; the 1.0.0 jar does not add later drops); skeleton drops arrows/bones, **not** its bow (bow 1.2.1); chicken feathers; sheep 2–4 wool (1.1 "Sheep now drop 1–3 wool per shearing rather than 2-4. [Java_Edition_1.1](https://minecraft.wiki/w/Java_Edition_1.1)"); magic: magma cream 1.1 ("Can now drop magma cream upon death"), skeleton-killed-creeper discs 1.1 ("All of the music discs, except for '11', can now be obtained when a skeleton kills a creeper"), zombie rare iron ingot/helmet/shovel/sword 1.2.1 ("Have a rare chance to drop an iron ingot, iron helmet, iron shovel, or iron sword" [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1)) | Facts: "drops modern" for all seven. So a 1.0 world has 1.1+ drops (magma cream, creeper discs, sheep 1–3 wool, and from modern zombie loot also iron/carrot/potato) — `OLD_SKELETON_DROPS` only suppresses the **bow**, which is correct. (F+C) | EXTRA (+WRONG sheep count) | M | H |
| R10-07 | Mobs — creeper AI | 1.2.1 "[[Creeper]]s — No longer strafe to the player's left but now simply stop in front of the player when they are about to explode." [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | Modern creeper AI has no left-strafe; nothing restores it for 1.0/1.1. (C) | MISSING | L | H |
| R10-08 | Combat — critical hit formula | Beta 1.8: "Added the critical hit mechanic, which currently adds a random amount of damage using the formula (+0% ~ +100% weapon damage + 2hp) and is calculated after enchantments are added." [Melee_attack](https://minecraft.wiki/w/Melee_attack) | `PlayerHistoricalCriticalHitMixin` replaces the flat ×1.5 with a random `[1.0, 2.0)` multiplier — but its own javadoc records that the **+2 HP term and the "after enchantments" ordering are not implemented** (`mixin/common/PlayerHistoricalCriticalHitMixin.java:37-53`). (C) | WRONG (incomplete) | M | H |
| R10-09 | Health — golden apple (Beta 1.8 value) | Beta 1.8: golden apple restores "10 hunger" and "Regeneration for 30 seconds" (research `beta18-golden-apple-regeneration`, artifact-verified; same wiki line quoted there). [Food](https://minecraft.wiki/w/Food) | `BETA_GOLDEN_APPLE` gives 10 hunger, always edible, Regeneration 30 s amp 0 — but the rebuilt `FoodProperties` keeps the **modern saturation** (9.6) instead of the Beta 1.8 saturation modifier 1.2 (≈24 points). (C) | WRONG (minor) | L | M |
| R10-10 | Health/potions — brewing time | 1.1 "[[Brewing]] — Reduced brewing time from 30 seconds to 20 seconds." So 1.0 = 30 s. [Java_Edition_1.1](https://minecraft.wiki/w/Java_Edition_1.1) | `BrewingStandHistoricalBrewingMixin` only cancels brewing before Pre3; the brew takes the modern 20 s in a 1.0 world. (C) | WRONG | L | H |
| R10-11 | Health/potions — effect strengths | See X-08 (pre-1.6.1 Instant Health I = 6 HP, Regeneration I = 1 HP/25 ticks). | Modern values (4 HP / 50 ticks). (C) | WRONG | M | H |
| R10-12 | XP — sources | 1.3.1 "Depending on ore type, experience now drops from ores that drop items. Taking smelted items out of furnaces gives experience now. Destroying mob spawners gives experience now." [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | No gating: the 1.21.1 rules apply, so a 1.0 world awards XP from ores, furnace output and spawners (1.3.1 content), and for breeding (1.4.2: "[[Experience]] — Now awarded for fishing and breeding" [Java_Edition_1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2)). (C) | EXTRA | M | H |
| R10-13 | XP — level curve pre-1.3.1 | No source consulted gives the Beta 1.8–1.2.5 per-level cost. The 1.3.1 page only says the progression was *adjusted*. [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | 1.21.1 curve (7, 9, 11 …). | UNKNOWN | L | M |
| R10-14 | XP/enchanting — enchanted books | See X-07 (1.4.6). | Books can be enchanted from 1.0. (C) | EXTRA | M | H |
| R10-15 | XP/enchanting — table level cap | 1.0.0 "[[Enchanting]] — Enchantment tables require [[bookshelves]] to get maximum enchantments, up to level 50 with 30 bookshelves. Added 17 enchantments." [Java_Edition_1.0.0](https://minecraft.wiki/w/Java_Edition_1.0.0) | Modern table caps at level 30 with 15 bookshelves; no code raises the pre-1.3.1 cap (`HistoricalEnchanting` only fixes cost/no-lapis). Enchantment list 17 is correct. (C+F) | MISSING (30–50 offers) | M | H |
| R10-16 | Movement — sprint control | Sprint was double-tap W; the sprint **key** is 1.7.6 (not in this era). 1.0.0: "[[Sprinting]] — Can no longer be done by double-tapping {{key|Shift}} while moving forward." [Java_Edition_1.0.0](https://minecraft.wiki/w/Java_Edition_1.0.0) | Modern key binding `key.sprint` is available in 1.0–1.6.4 (no gate). (C) | EXTRA | L | M |
| R10-17 | Movement — sprint on stairs | 1.5 "[[Stairs]] — Stairs no longer prevent sprinting." So 1.0–1.4 cannot sprint up stairs. [Java_Edition_1.5](https://minecraft.wiki/w/Java_Edition_1.5) | 1.21.1 permits sprinting up stairs. (C) | MISSING | L | H |
| R10-18 | Movement/vehicles — boats | 1.6.1 "New boat physics with lower inertia. Boat controls have been changed from the {{key|A}}, {{key|S}}, and {{key|D}} keys to mouse control." [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | Modern boat steering for all seven. (C) | MISSING | L | H |
| R10-19 | Combat — armor durability | 1.1 "[[Armor]] — The durability of all types of armor has been increased." (table: leather cap 28, iron helmet 83, diamond helmet 182 before). [Java_Edition_1.1](https://minecraft.wiki/w/Java_Edition_1.1) | Modern armor durability (leather 55, iron 165, diamond 363) — the 1.1 values — in a 1.0 world. (C) | MISSING | L | H |
| R10-20 | Visuals/inventory — item drops and pickup polish | 1.0.0 "[[Item tooltips]] — Labels for items have a blue border"; modern. Version label: "The version number at the top-left corner is displayed only in the debug screen again." [Java_Edition_1.0.0](https://minecraft.wiki/w/Java_Edition_1.0.0) | Facts: tooltips MODERN, label hidden — matches. | *no gap* | — | H |
| R10-21 | World — The End and the dragon | 1.0.0 additions: Nether fortress ("Is a large [[structure]] found in [[the Nether]]"), End portal room in strongholds ("The End portal room was added"), dragon ("Spawns only once in [[the End]]", boss bar, exit portal with dragon egg). [Java_Edition_1.0.0](https://minecraft.wiki/w/Java_Edition_1.0.0) | End accessible, `ORIGINAL_DRAGON_FIGHT` active and the 1.9 fight features are removed by the dragon mixins (`gameplay/HistoricalDragonFight.java` docs); outer End islands remain as X-04. (F+C) | EXTRA (X-04 only) | L | M |
| R10-22 | World — structure set | 1.0 structures: villages, mineshafts, strongholds, nether fortresses, dungeons (dungeons are Infdev, `feature-timeline.json` structures #2). | Facts restrict to village/fortress/mineshaft/stronghold — correct; dungeons are placed features, not in the restriction list; their presence in `moderner_beta:release_1_0_0` is not verified here. | UNKNOWN (dungeons) | L | L |
| R10-23 | World — height | 1.0 = 128 blocks; raised in 1.2.1 ("Height limit increased from 128 blocks to 256 blocks"). [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | Release 1.0 world preset uses `historical_experience:beta_1_7_3_overworld` (height 128, min_y 0) — correct for 1.0. (C) | *no gap* | — | H |
| R10-24 | UI/inventory — creative menu and potions | Pre-1.3.1 single-page creative inventory; 1.3.1 is where "all potions" were added to it. [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | `OLD_CREATIVE_MENU` active in 1.0/1.1/1.2.5 only (facts); catalog has no potions until `release_1_3` — correct. But the catalog omits `mycelium` (a 1.0 block: 1.0.0 world-generation additions list "Mycelium") and `dragon_egg`, so they cannot be obtained from the old creative menu. (C+F) | MISSING (minor) | L | M |
| R10-25 | Items — apples before 1.1 | 1.1 "[[Apple]]s — Now have a 1/200 (0.5%) chance of dropping from [[oak leaves]]." (so before 1.1 apples come only from chest loot) [Java_Edition_1.1](https://minecraft.wiki/w/Java_Edition_1.1) | Modern oak-leaf apple drop is active in 1.0 (no leaf-loot gate; `LootTableHistoricalEarlyLeavesMixin` is pre-Beta-1.8 only). (C) | EXTRA | L | M |
| R10-26 | Blocks — block-breaking cadence | 1.3.1 "The 1/4 second (5 game tick) delay between block breaking has been removed for players in [[Survival]]." [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | Modern has no inter-block delay; no gate for 1.0–1.2.5. (C) | MISSING | L | M |

Domains checked for 1.0 with no gap found: hunger/food bar (introduced Beta 1.8, present — `Food` history, research row 1),
sprinting/hunger/XP existence, sword damage (1.0 values stone 5/iron 6/diamond 7 — modern matches; `OLD_SWORD_DAMAGE`
correctly off), sword blocking 50 % (`Sword_blocking` row), bed sleep behaviour (1.0 removed monster spawns on sleep —
modern matches), weather (Beta 1.5), villages/fortress/stronghold/mineshaft (present), nether destination, creative
menu layout (old single list), recipe book absent, offhand absent, attack cooldown/sweep absent, stars (Beta field),
cloud height 128, boss-health caption, structured nether (fixed wastes), inventory size (36+4).

---

## 3. Release 1.1 (`historical_experience:release_1_1`) — compared with 1.1

ACTUAL (facts): same rules as 1.0; 20 spawns; 21 enchantments (adds Flame/Infinity/Power/Punch); 76 refused;
same structures. Expected 1.1 [Java_Edition_1.1](https://minecraft.wiki/w/Java_Edition_1.1). 1.1 adds **no blocks**
("first of only two major releases that did not introduce any new blocks") and no naturally-spawning mob, and the
spawn set correctly stays Release 1.0's.

| # | Domain | Expected (quote + URL) | Actual | Gap | Sev | Conf |
|---|---|---|---|---|---|---|
| R11-01 | Crafting — golden apple recipe | 1.1 "[[Golden apple]]s — Golden apples now restore 4 hunger and provide [[Regeneration]] I for 5 seconds. The recipe has been reduced to use [[gold nugget]]s instead of [[gold block]]s." [Java_Edition_1.1](https://minecraft.wiki/w/Java_Edition_1.1); gold **ingots** only from 1.6.1 ("Now requires [[gold ingot]]s instead of [[gold nugget]]s to craft" [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1)) | `BETA_GOLDEN_APPLE` ends at Release 1.1, so 1.1 uses the **modern** item: 8 gold ingots (should be 8 nuggets) and Regeneration II 5 s + Absorption I 2:00 (should be Regeneration I 5 s, no Absorption). (F+C) | WRONG | M | H |
| R11-02 | Crafting — signs and buckets | X-09 (still 1 sign / bucket stack 1 in 1.1). [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | Modern counts. (C) | WRONG | L | H |
| R11-03 | Crafting — slabs/ladders | X-09 (still yields 3 / 2 ladders in 1.1). [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | Modern counts. (C) | WRONG | M | H |
| R11-04 | Crafting — glistering melon | R10-04 (1 nugget until 1.6.1). [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | Modern 8-nugget recipe. (C) | WRONG | L | H |
| R11-05 | Crafting/smelting — nether brick item | R10-05 (1.5 content). [Java_Edition_1.5](https://minecraft.wiki/w/Java_Edition_1.5) | EXTRA in 1.1. (C) | EXTRA | M | M |
| R11-06 | Mobs — creeper AI | R10-07. [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | No left-strafe. (C) | MISSING | L | H |
| R11-07 | Health/potions — strengths | X-08. [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | Modern values. (C) | WRONG | M | H |
| R11-08 | XP — sources | R10-12 (ores/furnace/spawners 1.3.1; breeding 1.4.2). [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | Modern XP sources active. (C) | EXTRA | M | H |
| R11-09 | XP/enchanting — books | X-07. [Java_Edition_1.4.6](https://minecraft.wiki/w/Java_Edition_1.4.6) | Books enchantable. (C) | EXTRA | M | H |
| R11-10 | Mob drops — zombie rare drops before 1.2.1 | 1.2.1 "Have a rare chance to drop an iron ingot, iron helmet, iron shovel, or iron sword." [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | Modern drop tables give these in 1.1. (F) | EXTRA | L | H |
| R11-11 | World — the level envelope and terrain | 1.1 is a 128-high world ("Height limit increased from 128 … to 256" in 1.2.1). [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | Release 1.1 preset: 128 — correct. | *no gap* | — | H |
| R11-12 | World — villages | R10-22/X-05; 1.1 adds no desert villages (1.3.1) but the village generator would place one in a desert (X-05). [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | Desert villages generate in Release 1.1 (C). | EXTRA | M | H |
| R11-13 | Visuals — void fog | X-03; 1.1's change is removal from creative/nether/end only. [Java_Edition_1.1](https://minecraft.wiki/w/Java_Edition_1.1) | No void fog anywhere. | MISSING | L | M |
| R11-14 | World — world types | 1.1 adds the Superflat world type: "[[Superflat]] — Completely flat world composed of [[grass block]]s, [[dirt]], and [[bedrock]]; also generates [[villages]]." [Java_Edition_1.1](https://minecraft.wiki/w/Java_Edition_1.1) | The mod's create-world path only exposes the declared profile presets; whether a historical Superflat option exists is not established here. | UNKNOWN | L | L |

Domains checked with no gap "1.0 row": sheep wool 1–3 and grass regrowth (1.1 matches modern), music discs from
skeleton-killed creeper (modern matches), magma cream drop (modern matches), apples from leaves (1.1+ correct),
bow enchantments (facts 21 enchantments include Flame/Infinity/Power/Punch — correct), spawn eggs (1.1 addition;
creative catalog adds 20 eggs exactly, "Creative exclusive"), no new naturally-spawning mob, brewing 20 s now correct,
armor durability now correct, no new blocks (catalog adds none), structures/height unchanged.

---

## 4. Release 1.2.5 (`historical_experience:release_1_2_5`) — compared with the 1.2 line (1.2.1 content, 1.2.5 fixes)

ACTUAL (facts): 22 spawns (adds `ocelot`, `iron_golem`); 21 enchantments; 76 refused; same structure set; rules
unchanged. Expected [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) and
[Java_Edition_1.2.5](https://minecraft.wiki/w/Java_Edition_1.2.5).

| # | Domain | Expected (quote + URL) | Actual | Gap | Sev | Conf |
|---|---|---|---|---|---|---|
| R125-01 | World — height | 1.2.1 "Height limit increased from 128 blocks to 256 blocks." [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | Release 1.2.5 world preset uses `historical_experience:beta_1_7_3_overworld` with `"height": 128, "min_y": 0, "logical_height": 128` (`data/historical_experience/dimension_type/beta_1_7_3_overworld.json`). Build height is therefore 127, not 255. (C) | WRONG | H | H |
| R125-02 | Blocks/items — redstone lamp | 1.2.1 "[[Redstone lamp]] — Crafted with [[glowstone]] and [[redstone]]. Gives off light at a light level of 15 only when it is powered." [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | Facts: `redstone_lamp` is in Release 1.2.5's **76 refused** crafting ids (first-dated `release_1_3` in `HistoricalCraftingEra.java:169`), so it cannot be crafted; but the creative catalog dates it `release_1_2_5`, so creative grants it. Contradictory and short of the era's own content. (F+C) | MISSING | M | H |
| R125-03 | Crafting — chiseled stone bricks | 1.2.1 "Only available in Creative mode" (no survival recipe stated). [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | Allowed by the allow-list and not blocked; modern recipe (2 stone brick slabs) works. When the survival recipe actually arrived is not in the sources fetched. | UNKNOWN | L | M |
| R125-04 | Inventory/crafting — signs and buckets | X-09; 1.3.1 "Signs: … Crafting now gives 3 signs instead of 1"; "Empty buckets: Stack up to 16 now." [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | Modern counts. (C) | WRONG | L | H |
| R125-05 | Crafting — golden apple | R11-01: 8 nuggets + Regeneration I 5 s expected for 1.2.5 (gold ingots only 1.6.1). [Java_Edition_1.1](https://minecraft.wiki/w/Java_Edition_1.1), [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | Modern: 8 ingots, Regeneration II 5 s + Absorption I 2:00. (F+C) | WRONG | M | H |
| R125-06 | Crafting — glistering melon | R10-04. [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | Modern 8-nugget recipe. (C) | WRONG | L | H |
| R125-07 | Crafting/smelting — nether brick item | R10-05 (1.5 content). [Java_Edition_1.5](https://minecraft.wiki/w/Java_Edition_1.5) | EXTRA in 1.2.5. (C) | EXTRA | M | M |
| R125-08 | Health/potions — strengths | X-08. [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | Modern values. (C) | WRONG | M | H |
| R125-09 | XP — sources | R10-12 (1.3.1 ore/furnace/spawner XP). [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | Modern XP sources active. (C) | EXTRA | M | H |
| R125-10 | XP/enchanting — books | X-07 (1.4.6). [Java_Edition_1.4.6](https://minecraft.wiki/w/Java_Edition_1.4.6) | Enchantable books. (C) | EXTRA | M | H |
| R125-11 | Inventory — maps | 1.2.5 maps are the old single-map item (1.4.2 reworked maps: "Are now crafted as an [[empty map]] and become a real map by right-clicking"; "Removed text overlay"). [Java_Edition_1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2) | Modern map item and behaviour (empty map + zoom) in 1.2.5. (C) | WRONG | M | M |
| R125-12 | World — desert wells and jungle | 1.2.1 adds "generated features including [[jungle]] biomes, wooden bridges in [[mineshaft]]s, and [[desert well]]s". [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | The 1.2.5 exclusion list removes the `minecraft:desert_well` ban (`HistoricalFeatureExclusions.RELEASE_1_2_5_WITHOUT_MODERN_VANILLA_FEATURES`, filter at lines 749-753) and the foundation is `moderner_beta:release_1_2_5`, so wells/jungle are intended to generate. Not probed here. | *no gap recorded* (unprobed) | — | M |
| R125-13 | World — desert villages | X-05. [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | Desert villages generate in 1.2.5 (C). | EXTRA | M | H |
| R125-14 | Mobs — ocelot and iron golem | 1.2.1 "[[Ocelot]] — Can be tamed into [[cat]]s using [[raw fish]]"; "[[Iron golem]] — Occasionally spawns in [[village]]s"; adds "zombie sieges". [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | Facts spawn set adds `minecraft:ocelot` and `minecraft:iron_golem`; cats are deliberately not in the set (they are a separate 1.21.1 entity that did not spawn naturally until 1.14) — correct. | *no gap* | — | H |
| R125-15 | XP — level curve | X-06 (1.3.1 curve); pre-1.3.1 unknown. | 1.21.1 curve. | UNKNOWN | L | M |
| R125-16 | Inventory — block-break delay | R10-26 (5-tick delay until 1.3.1). [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | No delay (modern). (C) | MISSING | L | M |

Domains checked with no gap: slabs/stairs now 6 + top placement (1.2.1 matches modern), ladder recipe 3 (1.2.1
matches), ladder collision and vines climbable (modern), skeleton bow drop (1.2.1, `OLD_SKELETON_DROPS` correctly
off at 1.2.5), zombie rare drops (correct), zombie sun-avoidance goal (`NO_MONSTER_SUN_AVOIDANCE` correctly off at
1.2.5), zombie sieges/villager breeding (vanilla 1.21.1 has both), fire charge and XP bottle (craftable/creative),
jungle and desert well intent, iron golem/cat handling, block palette boundary (emerald still absent), anvil/beacon
correctly refused, structures (no pyramids/wells as blocks; huts 1.4.2), weather, nether.

---

## 5. Release 1.3 (`historical_experience:release_1_3`) — compared with 1.3.1

ACTUAL (facts): 22 spawns (inherited); 21 enchantments; 41 refused + "REMOVED-RECIPE recipes supplied:
[minecraft:enchanted_golden_apple]"; structures add `desert_pyramid`/`jungle_pyramid`; world rules unchanged;
visual profile switches to modern stars (`BETA_STARS` off). Expected
[Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1).

**Known-fact check (as instructed):** the enchanted golden apple is *not* refused anywhere in 1.3–1.6.4; the
removed recipe is supplied and the allow-list dates the item `release_1_3`. The 1.3.1 page's own addition line —
"[[Enchanted Golden Apple|Golden apple]] has two crafting recipes now … the new one replaces the gold nuggets with
blocks of gold" — matches. **No bug found.**

| # | Domain | Expected (quote + URL) | Actual | Gap | Sev | Conf |
|---|---|---|---|---|---|---|
| R13-01 | World — height | R125-01. [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | Release 1.3 preset is the same 128-high dimension type (`world_preset/release_1_3.json` → `historical_experience:beta_1_7_3_overworld`). Should be 256. (C) | WRONG | H | H |
| R13-02 | Blocks/items — ender chest refused | 1.3.1 "[[Ender chest]] — Crafted by surrounding an [[eye of ender]] with 8 pieces of [[obsidian]]." [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | Item allow-list dates `ender_chest` to `release_1_3` (OK), but `HistoricalBlockAvailability` dates the *block* to `release_1_6_4` with a timeline boundary (lines 681-702), and `HistoricalRecipeGate.isBlockUnavailable` therefore refuses the craft in 1.3. The creative catalog grants `ender_chest` from 1.3 → creative-only in practice. (C+F) | MISSING | M | H |
| R13-03 | XP — level curve | X-06: "Levels 1–15 cost 17 XP points each …" [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | 1.21.1 curve (7, 9, 11 …). (C) | WRONG | M | M |
| R13-04 | XP/enchanting — books | X-07. [Java_Edition_1.4.6](https://minecraft.wiki/w/Java_Edition_1.4.6) | Enchantable books. (C) | EXTRA | M | H |
| R13-05 | Crafting — golden apple | R11-01 (8 nuggets, Regeneration I 5 s). [Java_Edition_1.1](https://minecraft.wiki/w/Java_Edition_1.1), [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | Modern: 8 ingots, Regeneration II 5 s + Absorption. (F+C) | WRONG | M | H |
| R13-06 | Crafting — glistering melon | R10-04. [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | Modern 8-nugget recipe. (C) | WRONG | L | H |
| R13-07 | Crafting/smelting — nether brick item | R10-05 (1.5 content). [Java_Edition_1.5](https://minecraft.wiki/w/Java_Edition_1.5) | EXTRA in 1.3. (C) | EXTRA | M | M |
| R13-08 | Health — sprint-jump exhaustion | Research `beta18-exhaustion-action-costs`: 1.3.1 (12w18a) "Sprint-jump exhaustion lowered to 0.2", back to 0.8 only in 1.7.2 (13w36a). [Food](https://minecraft.wiki/w/Food) | `BETA_SPRINT_JUMP_EXHAUSTION` is active in 1.3 (window ends at `RELEASE_1_6_4`, `Feature.java:227-229`), so 1.3 charges 0.8 per sprint-jump instead of 0.2 — sprint-jumping drains hunger 4× too fast. (F+C) | WRONG | M | H |
| R13-09 | Health/food — cookie hunger | 1.3.1 "[[Cookie]]s — Give a full hunger point now." [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) i.e. 1 → 2 | `BETA_FOOD_HUNGER_VALUES` keeps the Beta value 1 in 1.3 (`HistoricalFoodHelper.BETA_HUNGER`), because its window also ends at 1.6.4. (F+C) | WRONG | L | H |
| R13-10 | Health/potions — strengths | X-08. [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | Modern values. (C) | WRONG | M | H |
| R13-11 | Inventory — maps | R125-11 (1.4.2 rework). [Java_Edition_1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2) | Modern map behaviour in 1.3. (C) | WRONG | M | M |
| R13-12 | World — desert villages | X-05: correct for 1.3+ ("Added desert-specific villages"). [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | Desert style selectable by biome → matches 1.3.1. | *no gap* | — | H |
| R13-13 | UI — tabbed creative menu | 1.3.1 "There are categorized tabs for various groups of items and blocks. Added a search tab…". [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | `OLD_CREATIVE_MENU` off from 1.3 — the tabbed 1.21.1 screen is used (X-01 for the unfiltered catalog). | *layout correct* / EXTRA items | M | H |
| R13-14 | Blocks — mossy cobblestone wall before 1.4.2 | Cobblestone walls (incl. mossy) are 1.4.2 additions: "Cobblestone Wall, Mossy Cobblestone Wall" in 1.4.2's Additions. [Java_Edition_1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2) | `mossy_cobblestone_wall` is dated `release_1_3` (`HistoricalCraftingEra.java:165`) and is not block-dated, so it is craftable in 1.3 (its plain variant is refused by the block gate, inconsistently). (C) | EXTRA | L | H |
| R13-15 | XP — sources | 1.3.1 adds ore/furnace/spawner XP — correct from 1.3 on. [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | Modern sources match. | *no gap* | — | H |
| R13-16 | World — structures | 1.3.1 adds desert pyramids and jungle temples ("new generated structures ([[Jungle pyramid|jungle]] and [[desert pyramid]]s)"). [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | Facts structure list adds `desert_pyramid`, `jungle_pyramid` — correct. | *no gap* | — | H |
| R13-17 | World — terrain foundation | X-11; 1.3.1 changed biome scale/depth, lava below Y=11, jungle tree cocoa. [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | Foundation preset is `moderner_beta:release_1_2_5`. | UNKNOWN (impact) | M | M |
| R13-18 | Visuals — stars | 1.3.1 "Made the stars smaller and brighter." [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | `BETA_STARS` off at 1.3 (facts) — modern smaller field, correct. | *no gap* | — | H |
| R13-19 | World — Large Biomes | 1.3.1 "[[Large Biomes]] world type — Generated as default worlds, but 16× bigger". [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | The project's create-world path offers the declared profiles, not the era's world-type list; not verified. | UNKNOWN | L | L |

Domains checked with no gap: sign/bucket counts now fixed by 1.3.1 (modern matches), bucket/slab stack sizes,
enchanted golden apple (recipe supplied, effects implemented: Regeneration IV 0:30, Resistance 5:00, Fire
Resistance 5:00 per the 1.3.1 addition lines; 4 hunger), no new mobs (spawn set inherited correctly), ocelot/cat,
enchanting cap 30/15 shelves (1.3.1 matches modern), old enchanting rules (cost = level requirement, no lapis —
`HistoricalEnchanting`), enchanting list 21, tripwire/string and tripwire hook (craftable; the *block* judgement is
palette-only and does not refuse), cocoa planting/usage, villagers trades, adventure mode, boss caption, left-click
interaction still on until 1.4.

---

## 6. Release 1.4 (`historical_experience:release_1_4`) — compared with 1.4.2

ACTUAL (facts): 26 spawns (adds bat, witch, wither skeleton, zombie villager); 21 enchantments; 35 refused;
structures add `swamp_hut`; world rules now `left-click use false`; flat dropped items still on.
Expected [Java_Edition_1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2).

| # | Domain | Expected (quote + URL) | Actual | Gap | Sev | Conf |
|---|---|---|---|---|---|---|
| R14-01 | World — height | R125-01 (256 from 1.2.1). [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | 1.4 preset uses the 128-high dimension type (`world_preset/release_1_4.json`). (C) | WRONG | H | H |
| R14-02 | Blocks/items — 1.4.2 blocks refused by the block gate | 1.4.2 adds "[[Anvil]]s", "[[Beacon]]s", "[[Cobblestone Wall]]", "[[Flower Pot]]s", "[[Command Block]]s" (Additions list). [Java_Edition_1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2) | Item-level: anvil/beacon/flower_pot/cobblestone_wall are allowed from `release_1_4`, **but** their blocks are timeline-dated to `release_1_6_4` (`HistoricalBlockAvailability.java:681-702`), so `HistoricalRecipeGate.isBlockUnavailable` refuses them in 1.4. Ender chest (1.3.1) is likewise still refused. Command blocks have no survival recipe, so the game impact is the other four + ender chest. (C+F) | MISSING | H | H |
| R14-03 | Mobs — gear, item pickup, enchants | 1.4.2 "[[Zombie]]s — Now rarely spawn with armor of all kinds, sometimes enchanted. Now rarely, depending on difficulty, spawn with the ability to pick up armor, headwear, items, blocks, weapons, and tools"; same for skeletons and zombie pigmen; skeletons "Rarely spawn with enchanted bows". [Java_Edition_1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2); timeline rows: "Mob armor … 1.4.2 (12w32a)" and "Zombies can now pick up and equip dropped items … 12w34b" [Feature timeline research](https://minecraft.wiki/w/Armor) / [Zombie](https://minecraft.wiki/w/Zombie) | `NO_MONSTER_GEAR`, `NO_MONSTER_ENCHANTS`, `NO_MONSTER_ITEM_PICKUP` all have window `before(RELEASE_1_6_4)` (`Feature.java:85-98`), so **1.4 and 1.5 mobs never spawn with armour, never get enchants and can never pick items up**. The equipment mixin clears only the armour slots (`MobHistoricalEquipmentMixin.java:59-73`); the held-item/weapon half
is not covered by any hook, so whichever weapons 1.21.1 gives monsters still appear in 1.0–1.5. (F+C) | MISSING | H | H |
| R14-04 | Crafting — 1.4.6 content leaks into 1.4 | 1.4.6 "introduced [[fireworks]], [[enchanted book]]s, and the [[Thorns]] enchantment"; "[[Nether Brick Slab]]s — Crafted using nether bricks". [Java_Edition_1.4.6](https://minecraft.wiki/w/Java_Edition_1.4.6) | Under the 1.4.2 proxy: `firework_rocket`/`firework_star` are allow-listed from `release_1_4` and their special recipes run (`HistoricalCraftingEra.SPECIAL_RECIPES`), `nether_brick_slab` is not refused in 1.4, and the creative catalog lists `enchanted_book`/`firework_rocket`/`firework_star` from `release_1_4`. Thorns is correctly absent (22nd enchantment 1.5 only). (F+C) | EXTRA | M | H |
| R14-05 | Crafting/potions — night vision and invisibility | 1.4.2 "[[Potion of Night Vision]] — Brewed by adding a [[golden carrot]] to an awkward potion"; "[[Potion of Invisibility]] — Brewed by adding a [[fermented spider eye]] to a potion of Night Vision". [Java_Edition_1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2) | Creative gating is correct (`HistoricalCreativeCatalog.potionAvailable` gates both behind the anvil window = 1.4+). In survival the golden carrot is craftable (allow-listed 1.4), and carrots can be grown, so the chain works — *if* R14-02's carrot block judgement has no planting impact (it does not: the gate is crafting/generation only). | *no gap in survival recipe* (see R14-08 for the carrot block judgement) | — | M |
| R14-06 | Crafting — golden apple | R11-01/R13-05: 8 nuggets + Regeneration I 5 s expected in 1.4.2. [Java_Edition_1.1](https://minecraft.wiki/w/Java_Edition_1.1) | Modern: 8 ingots, Regeneration II 5 s + Absorption. (F+C) | WRONG | M | H |
| R14-07 | Health/food — cookie and stew values | 1.4.2 (12w37a) "Mushroom stew lowered to 6 hunger" (research `beta18-food-values-table`; cookie was already 2 from 1.3.1). [Food](https://minecraft.wiki/w/Food) | `BETA_FOOD_HUNGER_VALUES` still active in 1.4 → cookie 1 (wrong) and stew 8 (wrong, should be 6). (F+C) | WRONG | L | H |
| R14-08 | Blocks — carrots/potatoes judged anachronistic | Carrots/potatoes are 1.4.2 additions: "[[Carrot]]s … can be planted on [[hydrated soil]]"; "[[Potato]]es …". [Java_Edition_1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2) | `HistoricalBlockAvailability` timeline-dates `minecraft:carrots`/`minecraft:potatoes` to `release_1_6_4` (`:681-702`), so `version-facts` reports them as "Blocks judged anachronistic" in 1.4/1.5. Crafting of the items is unaffected (they are not craft results and not block items), but the worldgen content veto (`FeaturePlacerHistoricalContentMixin`) will remove any *placed feature* whose block is judged anachronistic, and the facts themselves are then misleading. Game impact not measured. (C) | WRONG (judgement; impact UNKNOWN) | M | M |
| R14-09 | Health/potions — strengths | X-08. [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | Modern values. (C) | WRONG | M | H |
| R14-10 | Health — sprint-jump exhaustion | R13-08 (0.2 from 1.3.1). [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) research [Food](https://minecraft.wiki/w/Food) | Still 0.8 in 1.4 (`BETA_SPRINT_JUMP_EXHAUSTION`). (F+C) | WRONG | M | H |
| R14-11 | XP — level curve | X-06 (1.3.1 curve). [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | 1.21.1 curve. (C) | WRONG | M | M |
| R14-12 | XP/enchanting — books | X-07. [Java_Edition_1.4.6](https://minecraft.wiki/w/Java_Edition_1.4.6) | Enchantable books in 1.4 (creative + table). (C) | EXTRA | M | H |
| R14-13 | Mobs — witch spawn placement | 1.4.2 "[[Witch]] — … Spawn exclusively in [[witch huts]] in [[swamp]]s." [Java_Edition_1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2) | Facts add `minecraft:witch` to the **natural spawn set** with no placement restriction; `HistoricalSpawnRules` filters species, not spawn location, so witches can spawn as ordinary swamp monsters (1.7.2+ behaviour) in 1.4/1.5. (F+C) | EXTRA | L | M |
| R14-14 | World — infested stone in Extreme Hills | 1.4.2 "[[windswept hills|Extreme hills]] biome — [[Silverfish]] blocks now rarely generate here. On average, nearly one silverfish block is generated per chunk." [Java_Edition_1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2) | 1.4 uses the same generation foundation/exclusion list as 1.3 (`release_1_2_5` foundation; `RELEASE_1_3_WITHOUT…` exclusions in `LegacyCatalog`), and 1.2.5 had no silverfish blocks. Whether the conﬁgured `ore_infested` placement survives is not verified. | UNKNOWN | M | L |
| R14-15 | World — structure set | 1.4.2 adds "[[Witch Hut]] — Generates in swamps"; pyramids (1.3.1) inherit. [Java_Edition_1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2) | Facts add `minecraft:swamp_hut` — correct. | *no gap* | — | H |
| R14-16 | Combat/UI — left-click use removed; boss bar; 3D items | 1.4.2 "[[Door]]s, [[trapdoor]]s, [[lever]]s & [[button]]s — No longer triggerable with {{mouse|left}}"; "All boss mobs — Now show their name above health bars." [Java_Edition_1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2); 3D dropped items only 1.4.6 ("Now render in 3D when using fancy graphics" [Java_Edition_1.4.6](https://minecraft.wiki/w/Java_Edition_1.4.6)) | `LEFT_CLICK_USE` off at 1.4, `BOSS_HEALTH_CAPTION` off at 1.4, `FLAT_DROPPED_ITEMS` still on at 1.4 (correct for a 1.4.2 proxy) — all match. | *no gap* | — | H |
| R14-17 | Mobs — baby zombies | 1.4.2 "Added baby zombie, which is only spawnable using mods or third-party tools." (natural spawning only 1.6.2: "Baby Zombies and Baby Zombie Pigmens — Now spawn naturally." [Java_Edition_1.6.2](https://minecraft.wiki/w/Java_Edition_1.6.2)) | `NO_BABY_ZOMBIES` active — 1.4/1.5 have no natural babies, matching 1.4.2/1.5. | *no gap* | — | H |
| R14-18 | World — carrots/potatoes in village farms | 1.4.2 "[[Village|Villages]] — Carrots and potatoes can be found on farms in villages." [Java_Edition_1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2) | The historical village generator is used for all profiles; whether its crop slots follow 1.4.2 for a 1.4 world is not established (the 1.3.1 page has no crop change and the 1.4.2 page does). | UNKNOWN | M | L |

Domains checked with no gap: wither skeleton/wither/bat/zombie villager added to spawn set (wither correctly not a
natural spawn — "Place 4 pieces of [[soul sand]] in a T-shape and place 3 [[wither skeleton skull]]s"); boss health
caption off; left-click off; flat items (1.4.2 < 1.4.6); 21 enchantments (no Thorns — correct for 1.4.2); anvil
*mechanics* (vanilla: 39-level cap, combining, prior-work penalty — matches); beacons (mechanics vanilla); item
frames; flower-pot mechanics; witch hut structure; infested blocks in creative catalog; potion creative gating;
fishing/breeding XP now correct; zombie door-breaking/villager infection/curing; slime swamp spawning; HP/hunger
model still Beta (correct for 1.4).

---

## 7. Release 1.5 (`historical_experience:release_1_5`) — compared with 1.5

ACTUAL (facts): 26 spawns (inherited); 22 enchantments (adds Thorns); 35 refused; structures unchanged;
`left-click use false`; weather/villages on. Expected [Java_Edition_1.5](https://minecraft.wiki/w/Java_Edition_1.5).

| # | Domain | Expected (quote + URL) | Actual | Gap | Sev | Conf |
|---|---|---|---|---|---|---|
| R15-01 | World — height | R125-01. [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | 128-high dimension type (`world_preset/release_1_5.json`). (C) | WRONG | H | H |
| R15-02 | Blocks/items — the Redstone Update is uncraftable | 1.5 additions: "Nether Quartz Ore, Nether Quartz, Block of Redstone, Daylight Detector, Hopper, Redstone Comparator, Trapped Chest, Light/Heavy Weighted Pressure Plates, Block of Quartz, Chiseled Quartz Block, Quartz Pillar, Quartz Slab, Quartz Stairs, Activator Rail, Minecart with TNT, Dropper, Minecart with Hopper". [Java_Edition_1.5](https://minecraft.wiki/w/Java_Edition_1.5) | `HistoricalBlockAvailability` timeline-dates `hopper`, `dropper`, `comparator`, `daylight_detector`, `activator_rail`, `quartz_block` (plus the older `anvil`, `beacon`, `cobblestone_wall`, `ender_chest`, `flower_pot`) to `release_1_6_4` (`:681-702`), and `HistoricalRecipeGate` refuses timeline-dated blocks — so **in Release 1.5 none of hopper/dropper/comparator/daylight detector/activator rail/quartz block can be crafted**, and hopper minecarts/quartz stairs/slabs/pillars are unreachable through their ingredients. Redstone blocks, weighted plates and trapped chests escape the gate (undated) and are craftable — but only because they were never dated. (C+F) | MISSING | H | H |
| R15-03 | World — nether quartz ore | 1.5 "[[Nether Quartz Ore]] — Found in [[the Nether]]. There is an average of roughly 70 [[ore]]s per [[chunk]]." [Java_Edition_1.5](https://minecraft.wiki/w/Java_Edition_1.5) | The 1.5 generation list lifts the `minecraft:ore_quartz_nether` ban (`HistoricalFeatureExclusions.RELEASE_1_5…`, lines 755-766), but the foundation is the `moderner_beta:release_1_2_5` preset; whether that preset's Nether places quartz at all is not verified here. A generation probe counting `nether_quartz_ore` would settle it. | UNKNOWN | H | M |
| R15-04 | Mobs — gear, pickup, enchants | R14-03 (1.4.2 behaviour; still missing in 1.5). [Java_Edition_1.4.2](https://minecraft.wiki/w/Java_Edition_1.4.2) | `NO_MONSTER_GEAR`/`_ENCHANTS`/`_ITEM_PICKUP` still active in 1.5. (F+C) | MISSING | H | H |
| R15-05 | Health/food — cookie and stew values | R14-07 (cookie 2 from 1.3.1, stew 6 from 1.4.2). [Food](https://minecraft.wiki/w/Food) | `BETA_FOOD_HUNGER_VALUES` still active → cookie 1, stew 8. (F+C) | WRONG | L | H |
| R15-06 | Health — sprint-jump exhaustion | R13-08 (0.2). [Food](https://minecraft.wiki/w/Food) | 0.8 in 1.5. (F+C) | WRONG | M | H |
| R15-07 | Combat — the 1.5 +1 damage bug | "1.5: All attacks from players now deal {{hp|1}} more damage than intended unless the player is currently moving upwards in singleplayer." (quote recorded in `research/feature-timeline.json` mechanics row 44, from [Damage](https://minecraft.wiki/w/Damage)) | `OLD_SWORD_DAMAGE` only covers pre-Release-1.0; no 1.5 damage bug is implemented, so a 1.5 world does normal 1.21.1 damage. (C) | MISSING | M | M |
| R15-08 | Crafting — golden apple | R11-01 (8 nuggets, Regeneration I 5 s). [Java_Edition_1.1](https://minecraft.wiki/w/Java_Edition_1.1) | Modern (8 ingots, Regeneration II + Absorption). (F+C) | WRONG | M | H |
| R15-09 | Crafting — glistering melon | R10-04. [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | Modern 8-nugget recipe. (C) | WRONG | L | H |
| R15-10 | Health/potions — strengths | X-08. [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | Modern values. (C) | WRONG | M | H |
| R15-11 | XP — level curve | X-06. [Java_Edition_1.3.1](https://minecraft.wiki/w/Java_Edition_1.3.1) | 1.21.1 curve. (C) | WRONG | M | M |
| R15-12 | XP/enchanting — books | X-07. [Java_Edition_1.4.6](https://minecraft.wiki/w/Java_Edition_1.4.6) | Enchantable books. (C) | EXTRA | M | H |
| R15-13 | Mobs — zombie damage scaling | 1.5 "[[Zombie]] — Now are stronger the more damaged they are, dealing more damage (this was removed in 1.6.1)." [Java_Edition_1.5](https://minecraft.wiki/w/Java_Edition_1.5) | Modern zombies have no low-health damage scaling, so the 1.5-only behaviour is absent. `OLD_ZOMBIE_ARMS` does not cover it. (C) | MISSING | L | H |
| R15-14 | Blocks — bone meal, stairs movement, slime phases | 1.5 "[[Bone Meal]] — Nerfed significantly. Now picks a random stage of growth…" ; "[[Stairs]] — Stairs no longer prevent sprinting." ; "[[Slime]]s — Slime spawn rates in swamps now depend on the current moon phase." [Java_Edition_1.5](https://minecraft.wiki/w/Java_Edition_1.5) | `INSTANT_BONE_MEAL` correctly ends at 1.5; modern stairs allow sprinting (correct); modern slime swamp moon-phase spawning exists (correct). | *no gap* | — | H |
| R15-15 | World — structure set and content | 1.5 adds no structure/mob ("no new biome, structure or mob identified" — code comment quoting the research, corroborated by the page's additions list). [Java_Edition_1.5](https://minecraft.wiki/w/Java_Edition_1.5) | Facts: same structure list, same spawn set as 1.4 — correct. | *no gap* | — | H |
| R15-16 | XP — sources | R10-12 (correct from 1.3.1) | Correct for 1.5. | *no gap* | — | H |
| R15-17 | Inventory — maps | R125-11 (1.4.2) | Correct for 1.5. | *no gap* | — | H |
| R15-18 | UI — creative potion categorisation | 1.5 "[[Creative]] Mode — All potions are categorized." [Java_Edition_1.5](https://minecraft.wiki/w/Java_Edition_1.5) | Modern creative tabs sort potions into the Brewing tab; the catalog gating keeps the effect set era-correct. | *no gap* | — | M |

Domains checked with no gap: Thorns enchantment added (facts 22 — correct for 1.5), bone meal nerf, anvil/beacon
*mechanics* (unreachable items are R15-02), nether brick item + block recipe (correct from 1.5), bone-meal growth,
beacon range (vanilla modern differs but no source edit is needed — recorded as cosmetic), hopper/comparator
*mechanics* if obtained by creative (vanilla 1.21.1 still supports them; the gate blocks only the craft), skeleton
arrow range, slime moon phase, mineshaft chest minecarts (1.5; `HistoricalChestLoot` does not verify minecart
contents), old lighting rewrite (no claim), mob set unchanged.

---

## 8. Release 1.6.4 (`historical_experience:release_1_6_4`) — compared with the 1.6 line

ACTUAL (facts): 28 spawns (adds `horse`, `donkey`; mule correctly excluded); 22 enchantments; **0 refused**
("268 allow-listed ids, 0 refused in this version"; removed recipe supplied for the enchanted golden apple);
structures unchanged; attribute tooltips now on (`NO_ATTRIBUTE_TOOLTIPS` absent); `BETA_FOOD_HUNGER_VALUES`,
`BETA_SPRINT_JUMP_EXHAUSTION`, `BETA_CRITICAL_HIT_FORMULA` all absent; `ENCHANTED_GOLDEN_APPLE_ABSORPTION` present.
Expected [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1),
[Java_Edition_1.6.2](https://minecraft.wiki/w/Java_Edition_1.6.2), [Java_Edition_1.6.4](https://minecraft.wiki/w/Java_Edition_1.6.4).

| # | Domain | Expected (quote + URL) | Actual | Gap | Sev | Conf |
|---|---|---|---|---|---|---|
| R164-01 | World — height | R125-01 (256 from 1.2.1). [Java_Edition_1.2.1](https://minecraft.wiki/w/Java_Edition_1.2.1) | 1.6.4 preset uses the 128-high dimension type (`world_preset/release_1_6_4.json`). (C) | WRONG | H | H |
| R164-02 | Health — natural regeneration | 1.6.1 "[[Hunger]] — Natural health regeneration now costs hunger/saturation points." [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | `BETA_REGENERATION` is still active at 1.6.4 (facts active list) — no saturation cost, no fast-regen tier; food effectively lasts longer than in 1.6.4. (F+C) | WRONG | M | H |
| R164-03 | Health — golden apple regeneration duration | 1.6.1: "[[Golden Apple]] — … Now gives [[Regeneration]] II for 10 seconds and both tiers give [[Absorption]] for 1 minute and 30 seconds." (13w23b) then "13w25a Absorption from golden apples now lasts 2:00" (Golden Apple history). [Golden_Apple](https://minecraft.wiki/w/Golden_Apple) / [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | 1.6.4 uses the modern golden apple: Absorption 2:00 correct, but Regeneration is **II for 0:05** (modern) instead of II for 0:10. `DataComponentHolderHistoricalFoodMixin` only touches the golden apple for `BETA_GOLDEN_APPLE` (1.0) and the second tier; the first-tier 1.6.1 duration is not restored. (F+C) | WRONG | M | H |
| R164-04 | Combat — critical-hit formula | "Critical hits now add a flat 50% extra damage, replacing their previous damage formula, and they're now calculated before enchantments is added.{{check version|Was this a 1.6 or 1.7 change?}}" [Melee_attack](https://minecraft.wiki/w/Melee_attack) — the wiki itself leaves the version open | 1.6.4 gets the modern flat +50 % (`BETA_CRITICAL_HIT_FORMULA` absent), which the code comment asserts is the "1.6/1.7-era formula" (`PlayerHistoricalCriticalHitMixin.java:39-42`). If the change was 1.7, 1.6.4 should still have the Beta random formula. | UNKNOWN | M | M |
| R164-05 | UI — creative menu contents | Tabbed menu (correct), but the era's item set only. 1.6.1 additions are the 1.6 line's content: "Carpets, Hay Bale, Lead, Horse Armors, Horse Spawn Egg, Name Tag, Terracotta, Block of Coal, Stained Clay". [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | 1.6.4 uses the unfiltered 1.21.1 creative screen (X-01); the catalog does gate *taking* out-of-era stacks, so the visible tab list is modern (EXTRA) while grants are era-correct. (C) | EXTRA | M | H |
| R164-06 | World — desert village wells | 1.6.4 "[[Desert village]]s — Wells are now made of [[cobblestone]] and [[gravel]] instead of [[sandstone]]." [Java_Edition_1.6.4](https://minecraft.wiki/w/Java_Edition_1.6.4) | The village piece set builds desert wells from the desert style block set; whether it has a 1.6.4-specific cobble/gravel variant is not established (`HistoricalVillagePieces` style tables). | UNKNOWN | L | L |
| R164-07 | Combat/movement — sprint-jump exhaustion | Research `beta18-exhaustion-action-costs`: 0.2 at 1.3.1, 0.8 again at 1.7.2. [Food](https://minecraft.wiki/w/Food) | Modern value is 0.2 and `BETA_SPRINT_JUMP_EXHAUSTION` is absent at 1.6.4 → 0.2, matching the research for the whole 1.3.1–1.7.1 window. | *no gap* | — | H |
| R164-08 | Mobs — baby zombies and 1.6.2 behaviour | 1.6.2 "made baby zombies spawn naturally"; "Baby Zombies and Baby Zombie Pigmens — Now spawn naturally"; "Horses — Now remembers who tamed them"; "Minecarts — A player in a minecart no longer turns with the minecart." [Java_Edition_1.6.2](https://minecraft.wiki/w/Java_Edition_1.6.2) | `NO_BABY_ZOMBIES` absent at 1.6.4 → babies spawn (correct); taming memory is vanilla; minecart-rider camera is not gated (modern = 1.6.2+ behaviour, correct for 1.6.4 but MISSING for 1.0–1.5). (F+C) | *no gap at 1.6.4* (see R10/X rows for ≤1.5) | — | H |
| R164-09 | Mobs — horses and donkeys | 1.6.1 "[[Horse]]s … Rarely spawn in small groups"; "[[Donkey]]s — Like horses, but smaller and gray"; "[[Mule]]s — Obtained by breeding a horse with a donkey." [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | Facts spawn set adds `horse`, `donkey`; mule excluded as bred, not spawned — correct. Modern 1.21.1 horses naturally spawn in plains/savanna, matching the era. | *no gap* | — | H |
| R164-10 | Health — enchanted golden apple | 1.6.1 (13w24b) "Enchanted golden apples now give the player Absorption I as well, and Regeneration IV has been updated to Regeneration V" (quote recorded in `Feature.ENCHANTED_GOLDEN_APPLE_ABSORPTION`, from [Enchanted_Golden_Apple](https://minecraft.wiki/w/Enchanted_Golden_Apple)); 13w25a Absorption lasts 2:00. | Facts `ENCHANTED_GOLDEN_APPLE_ABSORPTION` active; `DataComponentHolderHistoricalFoodMixin` adds Absorption I and raises Regeneration to V; Resistance/Fire Resistance 5:00 from 1.3.1 retained; Absorption duration uses the modern 2:00 (assumption recorded in code, matching 13w25a). | *no gap* (duration assumption flagged) | — | M |
| R164-11 | XP — level curve | X-06: the 1.3.1 curve persisted to 1.8 (14w02a) per the wiki section title "Values from Java Edition 1.3.1 to 1.8 (14w02a)". [Experience](https://minecraft.wiki/w/Experience) | 1.21.1 curve. (C) | WRONG | M | M |
| R164-12 | Enchanting/potions/attribute tooltips | 1.6.1 additions: "Attribute system"; "Tooltips for swords, pickaxes, shovels, axes, and potions show how much damage … and what effect a potion gives"; enchanting list 22 incl. Thorns (1.4.6). [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | Facts: attribute tooltips on, 22 enchantments, effect-strength modern (matching the 1.6.1 nerf) — all correct for 1.6.4. | *no gap* | — | H |
| R164-13 | World — 1.6.1 generation deltas | 1.6.1 "[[Deserts]] — No longer contain water lakes"; "[[Nether]] — Added chest generation in [[nether fortress]]es." [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | Foundation is `moderner_beta:release_1_6_4`; desert-lake removal and fortress chests are vanilla-generator/NBT content in 1.21.1 (fortress chests are vanilla loot and `HistoricalChestLoot` rebuilds tables). Not probed. | *no gap recorded* (unprobed) | — | L |
| R164-14 | UI — creative XP level | 1.6.1 "[[Creative]] mode — … The experience level is no longer displayed in Creative mode." [Java_Edition_1.6.1](https://minecraft.wiki/w/Java_Edition_1.6.1) | HUD-level gating of the XP bar exists as `xp_bar_never_golden_days` / visual profile entries but the creative HUD suppression is not in the release feature list. | UNKNOWN | L | L |

Domains checked with no gap: mob gear/pickup/enchants (present), baby zombies (present), 1.6.1 food values (cookie 2,
stew 6 — modern), 1.6.1 effect strengths (modern = post-nerf), golden apple recipe (8 gold ingots — modern matches
13w23a), enchanted books and Thorns, leads/name tags/horse armor/coal block/hay bale/carpets/terracotta craftable,
anvil/beacon/hopper/comparator reachable (the block gates open exactly at 1.6.4), `ENCHANTED_GOLDEN_APPLE_ABSORPTION`,
original dragon fight, boats (new physics from 1.6.1 — modern matches), regional difficulty, water/`tnt` flint-and-steel
durability, spawn eggs on water, no recipe book, offhand absent, attack cooldown absent, weather/villages/structures.

---

## 9. Top ten highest-impact gaps (ranked by player impact)

1. **Release 1.5 cannot craft the Redstone Update.** Hopper, dropper, comparator, daylight detector, activator rail and
   block of quartz are refused because `HistoricalBlockAvailability` dates them to `release_1_6_4` with a timeline
   boundary while `Release 1.5` predates it (`HistoricalBlockAvailability.java:681-702`, `HistoricalRecipeGate.java:93-100`).
   The era's defining content is unreachable in survival; nether quartz may also be missing at generation (R15-02, R15-03).
2. **Release 1.4 cannot craft the Pretty Scary Update's blocks.** Anvil, beacon, cobblestone wall, flower pot (and the
   1.3.1 ender chest) are refused by the same block-date gate although the item allow-list admits them (R14-02).
3. **Every declared world from 1.2.5 onward is 128 blocks tall instead of 256.** All release presets share
   `historical_experience:beta_1_7_3_overworld` (`height: 128, min_y: 0`) — build limit 127, no space above Y=127 (R125-01,
   R13-01, R14-01, R15-01, R164-01).
4. **Release 1.4 and 1.5 mobs never wear armour, never hold enchanted gear and never pick up items.** The
   `NO_MONSTER_*` windows end at Release 1.6.4, not at 1.4.2 (R14-03, R15-04).
5. **Release 1.3's ender chest is uncraftable** (block gate), the one 1.3.1 block the modern crafting table still makes
   reachable in that profile (R13-02).
6. **The golden apple is wrong in 1.1–1.6.4**: 8 gold *ingots* instead of 8 nuggets in 1.1–1.5, modern Regeneration II +
   Absorption instead of Regeneration I 5 s in 1.1–1.5, and Regeneration 5 s instead of 10 s in 1.6.4 (R11-01, R13-05,
   R14-06, R15-08, R164-03).
7. **Release 1.6.4 regenerates health with the Beta model**, ignoring 1.6.1's saturation-costing regeneration — food and
   saturation behave like a pre-1.6.1 world (X-10, R164-02).
8. **Release 1.0–1.2.5 award XP for ores, smelting and spawners** (1.3.1 additions), and the level curve is the 1.21.1
   curve in every profile (X-06, R10-12).
9. **1.4.6 content leaks into Release 1.4**: fireworks, firework stars, nether brick slabs and enchanted books are
   craftable/creative while Thorns is correctly absent — the 1.4.2 proxy is applied inconsistently (R14-04, X-07).
10. **The creative menu of Release 1.3–1.6.4 lists modern 1.21.1 items.** The tabbed layout is right (1.3.1) and grants
    are refused afterwards, but the player sees concrete, copper, netherite, banners and modern slabs in a 1.3/1.6.4
    inventory (X-01).

Runners-up that a player will notice quickly: 1.0–1.5 recipe quantities (slabs/ladders/signs, X-09), 1.3–1.5 XP curve
(X-06), 1.3–1.5 cookie/stew values (R13-09/R14-07/R15-05), 1.3–1.5 sprint-jump exhaustion 0.8 vs 0.2
(R13-08/R14-10/R15-06), 1.0/1.1 pre-1.6.1 potion strengths (X-08), and desert villages in 1.0–1.2.5 (X-05).

---

## 10. Could not verify

| Topic | Why not | What would settle it |
|---|---|---|
| Pre-1.3.1 XP level curve (Release 1.0/1.1/1.2.5) | The wiki only says 1.3.1 "adjusted" the progression; no pre-1.3.1 table was found | Decompile the `Player`/`EntityPlayer` XP handler of a 1.0.0/1.2.5 client, or a wiki history row for `Experience` before 12w23a |
| Critical-hit formula at exactly 1.6.x | The wiki's own history row says "Was this a 1.6 or 1.7 change?"; research records it as "unknown (1.6/1.7 era)" | Decompile `Player.attack` in a 1.6.4 jar and check whether the 1.50 multiplier precedes or follows the enchantment bonus and whether a `+2` term exists |
| Release 1.5 nether quartz at generation | The exclusion list lifts the quartz ban, but the foundation is the 1.2.5-based preset; no probe was run in this audit | A generation probe on `historical_experience:release_1_5` counting `nether_quartz_ore` in Nether chunks |
| Release 1.4 infested/silverfish blocks in Extreme Hills | The generation foundation is 1.2.5-era; whether the 1.4 exclusion list re-enables the infested placement is not established | Generation probe counting `infested_stone` in an extreme-hills chunk of a 1.4 world |
| Carrots/potatoes as blocks judged anachronistic in 1.4/1.5 | The judgement exists in `HistoricalBlockAvailability`, but only the worldgen veto and the craft gate consume it; the actual player-visible effect was not measured | A 1.4 world probe for carrot/potato village-farm crops and for planting/growing |
| Release 1.3/1.4/1.5 terrain fidelity | All three use the `moderner_beta:release_1_2_5` foundation; how much 1.3.1's biome scale/depth/lava-level changes shift the terrain is not measured (X-11) | A 1.3.1/1.4.2/1.5 jar-based terrain comparison, or upstream presets for those builds |
| Stronghold count and rings for 1.0–1.6.4 | No source consulted gives per-version stronghold placement; the mod only allow-lists the structure type | Count strongholds in a generated release world and compare with the era's world-gen rule (1.0–1.6.4 used 3) |
| Village style by version (desert villages from 1.3.1, 1.6.4 desert wells, 1.4.2 village crops) | The historical village generator picks a style from biome only; no version gate was found, and the piece tables were not fully read | World-generation probe on each release profile for village styles and well/crop block sets |
| Dungeon (monster room) presence in each release preset | Dungeons are placed features, not a restricted structure; not listed in the exclude sets read | Chunk scan for `spawner` blocks in each release profile |
| Creative-menu completeness for Release 1.0/1.1 | The catalog is a curated id list; checking every 1.0.0 creative-inventory item against it is a separate sweep | Item-by-item comparison of the 1.0.0 creative inventory with `HistoricalCreativeCatalog` (mycelium and dragon egg are already known missing) |
| Potion availability in survival for 1.0–1.5 | Modern brewing recipes are used; a few potions are unreachable only because their ingredients are unreachable (R14-05), but the full reachability graph was not walked | Directed reachability test over items obtainable in each profile |
| Release 1.6.4/1.6.1 wither fight specifics | The 1.21.1 wither is used unmodified; behavioural deltas against 1.4.2's 300-HP three-head boss were not audited | Compare wither AI/attack parameters in 1.4.2/1.6.4 jars with 1.21.1 |
| Enchanted-book obtainability in 1.4/1.5 | R14-04/X-07 show books are enchantable, but whether they can then be *used* depends on the anvil gate (R14-02) | Runtime test: enchant a book in a 1.4 world, then try to use it |

---

## 11. Coverage matrix (domains × versions)

| Domain | 1.0 | 1.1 | 1.2.5 | 1.3 | 1.4 | 1.5 | 1.6.4 |
|---|---|---|---|---|---|---|---|
| Blocks/items availability | EXTRA (chiseled bricks) | — | MISSING (redstone lamp) | MISSING (ender chest) | MISSING (anvil/beacon/wall/pot) | MISSING (redstone update) | as designed (0 refused) |
| Crafting/smelting (quantities, glistering, nether brick) | WRONG | WRONG | WRONG | WRONG(quantities)/EXTRA(nether brick) | FAILS (anvil/beacon) + EXTRA (1.4.6) | FAILS + EXTRA(nether brick) | ✓ |
| Anvil | n/a | n/a | n/a | ✓ refused | refused although 1.4.2 content | refused although 1.4.2 content | ✓ |
| Potions / brewing | WRONG (30 s, strengths) | WRONG (strengths) | WRONG (strengths) | WRONG (strengths) | night vision reachable, strengths WRONG | strengths WRONG | ✓ (1.6.1-nerfed) |
| Fireworks | ✓ refused (1.4.6) | ✓ | ✓ | ✓ | EXTRA | ✓ (1.4.6) | ✓ |
| Beds | ✓ (Beta 1.3; single red bed) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Mobs: spawn set | ✓ | ✓ | ✓ (ocelot/golem) | ✓ | ✓ (witch/bat/wither skeleton/zombie villager) | ✓ | ✓ (horse/donkey; mule excluded) |
| Mobs: gear/pickup/enchants | ✓ absent | ✓ absent | ✓ absent | ✓ absent | MISSING | MISSING | ✓ present |
| Mobs: baby zombies | ✓ absent | ✓ absent | ✓ absent | ✓ absent | ✓ absent (correct) | ✓ absent (correct) | ✓ present |
| Mobs: drops | EXTRA/WRONG | EXTRA | ✓ (modern matches 1.2.1) | ✓ | ✓ (except listed) | ✓ | ✓ |
| Movement | sprint key EXTRA, boats WRONG | same | same + stairs | ✓ boats still WRONG | boats WRONG | boats WRONG | ✓ |
| Combat (crit, blocking, sword damage) | crit incomplete; blocking ✓; damage ✓ | same | same | same | same | +1 dmg bug MISSING | crit UNKNOWN |
| Health/hunger/food/regen/golden apple/absorption | golden apple saturation WRONG | golden apple WRONG | golden apple WRONG | golden apple + cookie WRONG, sprint-jump WRONG | + stew WRONG | same | regen WRONG, golden apple duration WRONG |
| XP/enchanting | XP sources EXTRA, curve UNKNOWN, 50-level MISSING, books EXTRA | same | same | curve WRONG, books EXTRA | same | same | curve WRONG |
| Inventory | signs/buckets WRONG, maps WRONG | same | + block-break delay | signs fixed, maps WRONG | maps now correct | — | ✓ |
| World: weather | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| World: structures/villages | ✓ set, EXTRA desert villages | same | same, wells/jungle intended | + pyramids, desert villages correct | + witch hut, infested UNKNOWN | unchanged ✓ | wells UNKNOWN |
| World: the End/dragon | original fight ✓, outer islands EXTRA | same | same | same | same | same | same |
| World: nether | ✓ fortress/wart, no quartz | ✓ | ✓ | ✓ | ✓ | quartz ore UNKNOWN | ✓ |
| World: height | 128 ✓ | 128 ✓ | 128 (should 256) | 128 (should 256) | 128 (should 256) | 128 (should 256) | 128 (should 256) |
| Visuals | void fog MISSING, stars/clouds/caption ✓ | same | same | stars now modern ✓ | flat items ✓, name bar ✓ | ✓ | ✓ |
| UI: creative menu (1.3 tabbed, no recipe book) | old menu ✓ (+2 catalog omissions) | same | same | tabbed ✓ / modern items EXTRA | same | same | same |

---

## 12. Sources fetched for the expected side

- https://minecraft.wiki/w/Java_Edition_1.0.0
- https://minecraft.wiki/w/Java_Edition_1.1
- https://minecraft.wiki/w/Java_Edition_1.2.1
- https://minecraft.wiki/w/Java_Edition_1.2.5
- https://minecraft.wiki/w/Java_Edition_1.3.1
- https://minecraft.wiki/w/Java_Edition_1.4.2
- https://minecraft.wiki/w/Java_Edition_1.4.6
- https://minecraft.wiki/w/Java_Edition_1.5
- https://minecraft.wiki/w/Java_Edition_1.6.1
- https://minecraft.wiki/w/Java_Edition_1.6.2
- https://minecraft.wiki/w/Java_Edition_1.6.4
- https://minecraft.wiki/w/Recipe_book
- https://minecraft.wiki/w/Golden_Apple (Java Edition history section; current Food values)
- https://minecraft.wiki/w/Melee_attack (Java Edition history section)
- feature-timeline pages already carrying wiki quotes: https://minecraft.wiki/w/Food, /Sprinting, /Experience, /Sword_blocking,
  /Zombie, /Armor, /Anvil, /Sheep, /Skeleton, /ocelot_/_cat, /iron_golem, /bat, /witch, /wither_skeleton, /wither,
  /horse_/_donkey_/_mule, /ender_dragon, /Desert_Well, /Desert_Pyramid, /Jungle_Pyramid, /Swamp_Hut, /Nether_Fortress
- supporting research read: research/feature-timeline.json, research/version-transitions.json,
  research/version-boundaries.json, research/historical-features.json, research/implementation-coverage.md,
  research/ore-height-ranges.json, research/creative-menu-catalog.json

Row count: **138 audited rows** (13 cross-version + 26 + 14 + 16 + 19 + 18 + 18 + 14), of which **113 record a gap or an
UNKNOWN** and **25 are explicit "no gap" coverage records** kept so that every required domain is shown to have been
checked for every version.
