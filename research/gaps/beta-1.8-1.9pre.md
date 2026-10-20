# Beta 1.8.1 → Beta 1.9 Prerelease 6: historical gap audit

Audit of the seven declared profiles in the Adventure Update window:

| # | Profile id | Declared name | Wiki page used as the expected side |
|---|---|---|---|
| 1 | `historical_experience:beta_1_8_1` | Beta 1.8.1 | `Java Edition Beta 1.8.1` (delta page) + `Java Edition Beta 1.8` (the update it releases) |
| 2 | `historical_experience:beta_1_9_pre_1` | Beta 1.9 Prerelease 1 | `Java Edition Beta 1.9 Prerelease` |
| 3 | `historical_experience:beta_1_9_pre_2` | Beta 1.9 Prerelease 2 | `Java Edition Beta 1.9 Prerelease 2` |
| 4 | `historical_experience:beta_1_9_pre_3` | Beta 1.9 Prerelease 3 | `Java Edition Beta 1.9 Prerelease 3` |
| 5 | `historical_experience:beta_1_9_pre_4` | Beta 1.9 Prerelease 4 | `Java Edition Beta 1.9 Prerelease 4` |
| 6 | `historical_experience:beta_1_9_pre_5` | Beta 1.9 Prerelease 5 | `Java Edition Beta 1.9 Prerelease 5` |
| 7 | `historical_experience:beta_1_9_pre_6` | Beta 1.9 Prerelease 6 | `Java Edition Beta 1.9 Prerelease 6` |

- **Audit date:** 2026-10-21.
- **EXPECTED side:** the raw wikitext of the page above, fetched with `?action=raw` on 2026-10-21. Every
  quote below is a verbatim substring of that raw text unless an ellipsis is shown. **Page-title trap, verified:**
  Prerelease 1 is *not* at `Java_Edition_Beta_1.9_Prerelease_1` (HTTP 404); its real title is
  `Java_Edition_Beta_1.9_Prerelease`. `Java_Edition_Beta_1.9` is a redirect to `Java Edition 1.0.0`.
- **ACTUAL side:** `research/version-facts.md`, generated from the code. Line-references such as
  [version-facts.md L574](../version-facts.md#L574) point at the exact generated row. Where the facts file is
  silent about a mechanism, the code is cited directly (`src/main/java/...:line`); such rows are marked.
- **Beta 1.8.1's expected side is inherited.** The 1.8.1 page itself lists only one change and two fixes
  ("the final version of Java Edition Beta … which replaces support beams in mineshafts with wooden fences and
  fixes bugs"). Everything else that 1.8.1 must do is the Beta 1.8 Adventure Update it ships, quoted from
  [W18]. Rows are labelled accordingly.
- **No value here was invented.** Anything the wiki and the code do not settle is `UNKNOWN` and appears again in
  *Could not verify*, with what would settle it. Modern-vanilla baseline values (mob health, stack sizes,
  stronghold count, enchantment caps) are used only to describe what the mod's *unmodified* modern code does;
  each such row says so.

**Gap types:** `MISSING` = the era had it and the mod does not; `WRONG` = the mod does something, but not the
era's thing; `EXTRA` = the mod does something the era did not have; `UNKNOWN` = evidence does not decide.
**Severity:** High = changes the core loop or major content; Medium = a noticeable mechanic/content difference;
Low = cosmetic/edge. **Confidence:** High = code-verified or quote-verified both sides; Medium = one side is
inference from absence of a gate; Low = ambiguous source or disputed research.

## Sources

Reference-style links, also spelled out per version below.

[W18]: https://minecraft.wiki/w/Java_Edition_Beta_1.8
[W181]: https://minecraft.wiki/w/Java_Edition_Beta_1.8.1
[P1]: https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease
[P2]: https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_2
[P3]: https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_3
[P4]: https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_4
[P5]: https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_5
[P6]: https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_6
[EP]: https://minecraft.wiki/w/Ender_Pearl

## Summary

| Profile | Gap rows | MISSING | WRONG | EXTRA | UNKNOWN | Matched rows | Highest-severity gap |
|---|---|---|---|---|---|---|---|
| Beta 1.8.1 | 21 | 1 | 8 | 7 | 5 | 15 | Combat/mob-stats: pre-Pre3 stone/iron sword damage, enderman/cave-spider/silverfish health |
| Beta 1.9 Pre1 | 13 | 3 | 6 | 3 | 1 | 10 | Pre1's own armor model change never implemented; nether-brick/gold-nugget crafting refused |
| Beta 1.9 Pre2 | 6 | 4 | 0 | 0 | 2 | 11 | No breeding; no potions/brewing ingredients; first batch of music discs missing |
| Beta 1.9 Pre3 | 11 | 2 | 4 | 4 | 1 | 13 | End portal cannot be repaired with eyes; potion set/brew time wrong; glass pillars absent |
| Beta 1.9 Pre4 | 9 | 0 | 6 | 3 | 0 | 15 | Modern 1.9+ End generation; dragon spawns a build early; enchant cap 30 not 50 |
| Beta 1.9 Pre5 | 7 | 4 | 2 | 0 | 1 | 11 | Achievements and `/toggledownfall` missing; Pre4-side slime range/water still wrong |
| Beta 1.9 Pre6 | 4 | 3 | 0 | 1 | 0 | 16 | Wooden-door redstone bug not emulated; silk-touch ice/infested stone still obtainable |
| **Total** | **71** | **17** | **26** | **18** | **10** | **91** | |

71 gap rows, plus 91 explicitly checked-and-matched rows. The single most important structural finding is
that several implementations stop one boundary early (breeding, brewing items, potions, music discs, the
dragon) or start one boundary late (nether bricks, gold nuggets, glistering melon, splash potions), i.e. the
**Pre2 and Pre3 boundaries are shifted by one prerelease** in both directions.

---

## 1. Beta 1.8.1 (`historical_experience:beta_1_8_1`)

**Expected side.** [W181] https://minecraft.wiki/w/Java_Edition_Beta_1.8.1 lists only:
"Support beams in mineshafts now generate with fences instead of planks." and two shift-click crash fixes.
The rest of what this build must do is the Beta 1.8 Adventure Update ([W18]
https://minecraft.wiki/w/Java_Edition_Beta_1.8), quoted per row below.

**Actual side.** [version-facts.md L568–594](../version-facts.md#L568): rules `hunger, sprinting, experience,
structures, 16 spawns allowed, 4 drop rules, no direct food healing`; 16 spawns; 4 drop rules; 44 active
features; structures restricted to `[historical_village, mineshaft, stronghold]`.

### 1.1 Gap table

| # | Version | Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|---|---|
| B1 | 1.8.1 | Health/food | "Now a slice of cake restore {{hunger\|3}}, instead of {{hp\|3}}." — [W18] §Items | Only cookie=1 and stew=8 are overridden; cake still uses modern nutrition 2 (`HistoricalFoodHelper.BETA_HUNGER` excludes cake, [src/.../HistoricalFoodHelper.java:73](../../src/main/java/net/historicalexperience/gameplay/HistoricalFoodHelper.java#L73)); facts: "hunger values only" [L582](../version-facts.md#L582) | WRONG | Low | Medium (research file itself records 3 vs 2 as disputed) |
| B2 | 1.8.1 | Health/food | "Can give the player the hunger effect." (raw chicken) / "Can give the player the hunger effect." (rotten flesh) — [W18] §Items | No historical Hunger-effect probability/duration override exists; the modern item components apply. Facts silent; `LivingEntityHistoricalHungerEffectMixin` only *suppresses* the effect pre-Beta-1.8 | UNKNOWN | Low | Low |
| B3 | 1.8.1 | Health/hunger | "Decreases when the player does certain actions, like when being hurt, jumping and mining." — [W18] §Hunger | Implemented costs are attacking 0.3, jump 0.2, sprint-jump 0.8 (`BETA_EXHAUSTION_COSTS`, `BETA_SPRINT_JUMP_EXHAUSTION` [L574](../version-facts.md#L574)); "hurt" and "mining" costs are the modern ones and are not in the research at all | UNKNOWN | Low | Low |
| B4 | 1.8.1 | Combat/crits | "Do more damage depending how close the player was to the ground while falling." — [W18] §Gameplay | Random 1.0–2.0 multiplier (`PlayerHistoricalCriticalHitMixin`); the recorded "+2 HP" term and after-enchantment ordering are explicitly NOT implemented (class javadoc) | UNKNOWN | Medium | Medium |
| B5 | 1.8.1 | Combat/sword damage | Pre3 page's "Before" column: Stone `{{hp\|6}}`, Iron `{{hp\|8}}`, Diamond `{{hp\|10}}` (the reduction happens at Pre3) — [P3] §Items | Pre-nerf branch returns modifiers stone 4.0 / iron 6.0, i.e. totals 5 and 7 with the player's base 1; wood 4 and diamond 10 are right (`HistoricalDamageValues.eraAttackDamage` [src/...:187](../../src/main/java/net/historicalexperience/gameplay/HistoricalDamageValues.java#L187)). The comments and test assert "total 8" for iron while computing modifier 6 | WRONG | Medium | High (arithmetic) |
| B6 | 1.8.1 | Combat/blocking | "Added blocking. Negates 50% of the incoming damage." — [W18] §Items (no fall-damage statement on the 1.8 page) | `OLD_SWORD_BLOCKING` active and `SWORD_BLOCK_FALL_PROTECTION` active at 1.8.1 ([L574](../version-facts.md#L574)); the fall-damage half is dated "a Beta 1.9 prerelease" with the exact build unknown | UNKNOWN | Low | Low |
| B7 | 1.8.1 | Inventory | "Ender pearls have no use, but can stack up to 64." — [EP] §History (Beta 1.8 row) | Modern ender pearl stack 16; no stack-size gate exists for it; facts have no ender-pearl rule ([L580](../version-facts.md#L580)) | WRONG | Medium | High (modern default, no override) |
| B8 | 1.8.1 | Gameplay/items | "Ender pearls have no use" — [EP] §History | Modern thrown ender pearl teleports; nothing gates it; `ender_pearl` only appears in creative/loot tables in code | EXTRA | Medium | High (no gate; modern behaviour) |
| B9 | 1.8.1 | Mobs/drops | "Ghast tear" is a **Prerelease 1 addition** ("Drops from ghasts. Has no function.") — [P1] §Items | Ghast has no 1.8.1 drop rule, so the modern loot table (ghast tear + gunpowder) applies; facts' 4 rules cover only zombie/chicken/spider/cow [L580](../version-facts.md#L580), [L572](../version-facts.md#L572) | EXTRA | Low | High |
| B10 | 1.8.1 | Mobs/drops | "raw beef … Dropped by cows"; "raw chicken … Dropped by chickens"; chicken also dropped feathers in this era (inferred) — [W18] §Items | Model "carries one item per mob so probabilistic secondary drops are not expressed" ([L572](../version-facts.md#L572)); cow→beef 0..3 and chicken→chicken 0..1 only, so leather and feathers are missing [L580](../version-facts.md#L580) | MISSING | Low | High (self-recorded) |
| B11 | 1.8.1 | Mobs/drops | "rotten flesh … Dropped by zombies and zombie pigmen." — [W18] §Items | No zombie-pigman rule at 1.8.1, so modern zombified-piglin loot applies, which includes a gold nugget — but gold nuggets are a Pre1 addition ([P1] §Items) | EXTRA | Low | High |
| B12 | 1.8.1 | Mobs/stats | Enderman "Has {{hp\|20}}"; cave spider "Has {{hp\|20}}"; silverfish "Has {{hp\|20}}" — [W18] §Mobs | No mob-attribute gating exists; modern values apply: enderman 40, cave spider 12, silverfish 8 (grep of `Attributes.MAX_HEALTH` finds only the HUD) | WRONG | Medium | High |
| B13 | 1.8.1 | Mobs/stats | Zombies get "additional armor points" only in Pre2: "Now have {{hp\|20}} and {{armor\|2}}." — [P2] §Mobs | Modern zombie already has 20 HP **and** 2 armour at 1.8.1; no gate | WRONG | Low | High |
| B14 | 1.8.1 | World/mobs | Snow golem is a Pre1 addition: "Is a buildable passive mob; created by placing two snow blocks and a pumpkin on top." — [P1] §Mobs | Snow golem is buildable in a 1.8.1 world too (modern mechanic; no gate), and since 1.13 it additionally requires a *carved* pumpkin, a 1.13 mechanic | EXTRA | Medium | Medium |
| B15 | 1.8.1 | Blocks/items | "Pumpkin seeds: Cannot be obtained." — [W18] §Items | `pumpkin_seeds` is craftable: it is allowed from `beta_1_8_1` in `HistoricalCraftingEra` ([src/...:145](../../src/main/java/net/historicalexperience/gameplay/HistoricalCraftingEra.java#L145)) and is absent from the refused list [L586](../version-facts.md#L586) | EXTRA | Low | High |
| B16 | 1.8.1 | World/structures | Stronghold: "Only 3 generate per world." — [W18] §World generation | Structure rule only restricts *types* (`strongholds, mineshafts, villages`); vanilla structure placement (128 strongholds by default) applies; no count control in `HistoricalStructureRules` | WRONG | Medium | Medium |
| B17 | 1.8.1 | World/structures | The End portal room is a **Pre3** addition: "The End portal room was added." — [P3] §World generation | `minecraft:stronghold` is allowed from 1.8.1, and the modern structure always contains a portal room; no piece/room filtering exists | EXTRA | Medium | Medium (structure writes could be partially vetoed; room itself is not) |
| B18 | 1.8.1 | UI | Hardcore is a **Pre2** addition: "Locks the difficulty to Hard and forces users to delete the world's save data upon death." — [P2] §Gameplay | No hardcore gate anywhere in `src/main` (grep); the vanilla create-world screen and hardcore mode remain available. Whether the mod's create-world flow exposes it for 1.8.1 was not executed | UNKNOWN | Low | Low |
| B19 | 1.8.1–Pre3 | Mobs/endermen | Endermen only stop burning in daylight at Pre4: "Endermen used to burn in sunlight, but as of this update, they instead randomly teleport around until they find a dark spot" — [P4] §Mobs (so the burning state holds at 1.8.1, Pre1, Pre2 and Pre3) | Modern endermen already teleport/do not burn; no enderman-daylight gate exists (`NO_MONSTER_SUN_AVOIDANCE` only disables the generic `FleeSunGoal`, not enderman daylight behaviour) | WRONG | Medium | Medium |
| B20 | 1.8.1–Pre3 | Visuals | Moon phases are a **Pre4** addition: "The moon now has multiple phases, somewhat akin to the real Moon." — [P4] §Gameplay | The modern renderer always has moon phases and no gate exists, so 1.8.1–Pre3 show the Pre4+ moon | EXTRA | Low | Medium |
| B21 | 1.8.1 | Controls | "Holding left click with a weapon now only swings once." — [W18] §Controls | No gate exists; modern hold-to-attack auto-repeats. Inferred from absence of a gate | WRONG | Low | Medium |

### 1.2 Checked, no gap (Beta 1.8.1)

| Domain | Expected (quote + URL) | Actual |
|---|---|---|
| Hunger model | "Food no longer heals directly, but indirectly: if the food meter is above 90%, heals 1 every 4 seconds. If the food meter is at 0%, the player starves and loses 1 hearts every 4 seconds." — [W18] | `BETA_REGENERATION` active (threshold 18, 80 ticks, heal 1) [L574](../version-facts.md#L574), starve thresholds are modern and match |
| XP existence | "Experience orb … Dropped by monsters and animals when they are killed"; Experience "Currently has no use." — [W18] | orbs collected from Beta 1.8; `HIDDEN_XP_LEVEL` hides the number; enchanting/brewing gated [L574](../version-facts.md#L574) |
| Sprinting | "Activated by double-tapping {{key\|W}}." — [W18] | `sprinting true` [L594](../version-facts.md#L594); 1.3× speed confirmed by project research (modern also offers Ctrl, an extra binding) |
| Bows | "Behavior has been overhauled; they now need to be charged in order to fire." — [W18] | `INSTANT_BOW` not active at 1.8.1 [L574](../version-facts.md#L574) → charged bows |
| Passive mobs | "No longer despawn." / "Randomly flee if attacked by the player or other mobs." — [W18] | `NO_ANIMAL_PANIC` not active; modern animals persist and panic |
| Skeletons | "Skeletons now fire arrows slower, but further." / "no longer bounce around the player" — [W18] | `NO_SKELETON_STRAFING` not active at 1.8.1; modern ranged goal used |
| Food stacking | "Can now be stacked up to 64." — [W18] | `UNSTACKABLE_FOOD` ends at Beta 1.8 [L574](../version-facts.md#L574) |
| Creative | "Infinite access to almost every item"; "Flying by double tapping Space." — [W18] | `OLD_CREATIVE_MENU` active + era creative catalog; modern creative flight |
| Structures exist | Mineshafts, strongholds and (empty) villages — [W18] §World generation | restricted to `historical_village, mineshaft, stronghold` [L588](../version-facts.md#L588); villagers gated to Pre1 (`HistoricalVillagePieces:528`) so 1.8.1 villages are empty |
| Mineshaft delta | "Support beams in mineshafts now generate with fences instead of planks." — [W181] | vanilla modern mineshafts use fences: matched by using the modern structure |
| Spawn set | Enderman, cave spider, silverfish added in Beta 1.8; mooshroom/blaze/villager not — [W18] §Mobs | 16 spawns: the Beta 1.8 additions and none of the Pre1 mobs [L578](../version-facts.md#L578) |
| Visuals | "Implemented a new lighting engine"; chest open/close animation; version tag shown — [W18] | old-lighting/old-chest/old-fog gates all off at 1.8.1, label shown [L592](../version-facts.md#L592) |
| Weather | "Rain and snow now render faster." — [W18] | `weather true` [L594](../version-facts.md#L594) |
| Unarmed damage | "Unarmed damage was decreased from 2 to 1." — [W18] | modern fist damage is 1 |
| Achievements | "Added Sniper Duel" — [W18] | no historical achievement gate found; the era-wide absence is scored once, as P5-1 |

*The Beta 1.8 "Sniper Duel" achievement and Pre5's ten achievements share one absent system; the gap is
scored as P5-1 rather than repeated here.*

---

## 2. Beta 1.9 Prerelease 1 (`historical_experience:beta_1_9_pre_1`)

**Expected side.** [P1] https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease — "adds Nether fortresses,
6 new blocks including Nether bricks and some variants, 5 new mobs including villagers and blazes, 4 new items
including ghast tears, the mushroom island biome".

**Actual side.** [version-facts.md L596–622](../version-facts.md#L596): 20 spawns; modern drops; structures
`[historical_village, fortress, mineshaft, stronghold]`; 90 crafting ids refused; 28 blocks judged
anachronistic; `NO_BREWING`/`HIDDEN_XP_LEVEL` still active; `JUMP_EXPERIENCE_BUG` active.

### 2.1 Gap table

| # | Version | Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|---|---|
| P1-1 | Pre1 | Crafting/blocks | "Nether brick … Generates in Nether fortresses."; "Nether brick stairs … Crafted with Nether brick."; "Nether brick fence … Crafted with Nether brick." — [P1] §Blocks | All three are refused by the crafting gate: `HistoricalCraftingEra` dates `nether_bricks/nether_brick_stairs/nether_brick_fence` from `beta_1_9_pre_3` ([src/.../HistoricalCraftingEra.java:148](../../src/main/java/net/historicalexperience/gameplay/HistoricalCraftingEra.java#L148)), and the Pre1 refused list contains them [L614](../version-facts.md#L614) | MISSING | Medium | High |
| P1-2 | Pre1 | Crafting/items | "Gold nugget … Crafted from a gold ingot." — [P1] §Items | `gold_nugget` is dated Pre3 in the same table ([src/...:153](../../src/main/java/net/historicalexperience/gameplay/HistoricalCraftingEra.java#L153)) and is refused at Pre1 [L614](../version-facts.md#L614) | MISSING | Medium | High |
| P1-3 | Pre1 | Combat/armour | "Armor level no longer decreases as the durability goes lower."; "Armor durability increased."; "Armor no longer reduces some types of damage: Explosion, Fire, Projectiles" — [P1] §Items | Recorded as absent: implementation coverage lists the armour model change as `A` (not attempted); grep finds no armour-attribute/durability/effectiveness gate, only an old-damage *tint* renderer | MISSING | Medium | High |
| P1-4 | Pre1 | Movement | "Continuous jumping by holding down the jump key is no longer possible." — [P1] §General | No gate for held-jump auto-repeat exists; modern hold-to-jump applies | WRONG | Low | Medium (inferred from absence of a gate) |
| P1-5 | Pre1 | Mobs/items | Spider eye is added in **Pre2**: "Spider eye — Drops from spiders." — [P2] §Items | `spider_eye` is in the Pre1 creative catalog ([src/.../HistoricalCreativeCatalog.java:49](../../src/main/java/net/historicalexperience/gameplay/HistoricalCreativeCatalog.java#L49)), and Pre1 drops are "modern drop tables" [L608](../version-facts.md#L608), so spiders already drop it | EXTRA | Low | High |
| P1-6 | Pre1 | Mobs/villagers | "Spawn in villages. Have 5 main professions (numbered 0, 1, 2, 3, 4) … Have the word 'TESTIFICATE' displayed over their heads. Share the same AI as pigs; however, they do not flee when attacked." — [P1] §Mobs | Village residents are correctly gated to Pre1 (`HistoricalVillagePieces:528`), but every pre-Release-1.3 villager is created with `VillagerProfession.NONE` (plain skin, no TESTIFICATE label) and keeps the modern villager AI/trade screen; there is no historical villager-AI gate | WRONG | Medium | Medium |
| P1-7 | Pre1 | Items | "Ender pearls have no use" (Beta 1.8 row); teleporting is added in **Pre2**: "Added teleporting feature to ender pearls." — [EP] | Modern ender pearl teleports; no gate | EXTRA | Medium | High |
| P1-8 | Pre1 | Inventory | Ender pearls "can stack up to 64" until Pre2 ("Ender pearls now stack up to 16") — [EP] | modern 16; no stack-size gate | WRONG | Medium | High |
| P1-9 | Pre1 | Mobs/stats | No stat change listed for Pre1; Beta 1.8 figures still apply — enderman "Has {{hp\|20}}", cave spider "Has {{hp\|20}}", silverfish "Has {{hp\|20}}" — [W18] §Mobs | modern 40 / 12 / 8 [L606](../version-facts.md#L606) spawn set is right, attributes are modern | WRONG | Medium | High |
| P1-10 | Pre1 | Blocks/data | "Nether wart … Is found in Nether fortresses. Can only be placed on soul sand." — [P1] §Items | The block-availability table dates `minecraft:nether_wart` to **Release 1.0** ([src/.../HistoricalBlockAvailability.java:666](../../src/main/java/net/historicalexperience/generation/HistoricalBlockAvailability.java#L666)), so it is listed as anachronistic at Pre1 [L618](../version-facts.md#L618); the generation veto may therefore strip fortress wart blocks, and the data contradicts the build | WRONG | Medium | High (data), Medium (runtime effect — probe needed) |
| P1-11 | Pre1 | World/terrain | "Landmasses now generate at half scale, resulting in smaller continents and less distance between them."; Ocean "Slightly reduced height variation from 0.5 to 0.4." — [P1] §World generation | Terrain comes from `moderner_beta:beta_1_9_pre_3`; the evidence string states Pre1's landmass/ocean change "are not reproduced" [L600](../version-facts.md#L600), but Pre3 postdates Pre1 and no later page reverts them, so whether the preset carries them is unresolved | UNKNOWN | Low | Low |
| P1-12 | Pre1 | Mobs | Snow golem: "created by placing two snow blocks and a pumpkin on top." — [P1] §Mobs | Buildable via modern code, which requires a **carved** pumpkin (1.13 mechanic) instead; the extra carving step is anachronistic | WRONG | Low | Medium |
| P1-13 | Pre1 | Mobs/enderman | "arrows, snowballs, eggs, and other ranged attacks do not hit them because they teleport away before impact"; "Endermen no longer have any sounds." — [P1] §Mobs | Modern endermen take projectile damage/teleport on hit and have modern sounds; no gate for either | EXTRA | Low | Medium |

### 2.2 Checked, no gap (Pre1)

| Domain | Expected (quote + URL) | Actual |
|---|---|---|
| Nether fortress | "Two blaze monster spawners generate in every Nether fortress" — [P1] §World generation | `minecraft:fortress` allowed from Pre1 [L616](../version-facts.md#L616); spawner *count* per fortress not verified (see Could not verify) |
| New mobs | Blaze "May spawn naturally in Nether fortresses in groups of 2 or 3"; magma cube in the Nether; mooshroom "Spawns in mushroom islands"; villagers in villages — [P1] §Mobs | 20-spawn set includes blaze/magma_cube/mooshroom/villager, no snow golem [L606](../version-facts.md#L606) |
| Biome set | Frozen ocean, frozen river, ice plains, ice mountains, mushroom island, mushroom shore — [P1] §World generation | produced by the Pre3 preset; mushroom fields also excludes hostile spawns (modern biome rule) — terrain from the preset, not re-measured here |
| Lily pads | "Generate in swamplands"; non-solid; "Can only be placed on water." — [P1] §Blocks | block availability dated Pre1 after the stated-date correction (`HistoricalBlockAvailability:647`); absent from the anachronistic list [L618](../version-facts.md#L618) |
| Mycelium/mooshroom | "Generates in mushroom islands"; mooshroom shear → 5 red mushrooms + cow — [P1] | mooshroom in spawn set; modern shear behaviour matches |
| Items with no function yet | "Blaze rod … Has no function"; "Ghast tear … Has no function"; "Nether wart … Has no function" — [P1] §Items | `NO_BREWING` active at Pre1 [L602](../version-facts.md#L602) (brewing is Pre3), so they have no use |
| Jump XP bug | "When the player jumps, their experience increases by a number between 1 and 100 experience." — [P1] §General | `JUMP_EXPERIENCE_BUG` active at Pre1 only [L602](../version-facts.md#L602); runtime-covered by `PlayerHistoricalJumpExperienceMixin` |
| Gold nugget drop | "Drops from zombie pigmen." — [P1] | modern zombified-piglin loot includes a gold nugget |
| Fortress generation | "Generates in Nether fortresses" (nether brick) — [P1] | fortress allowed from Pre1; nether bricks judged available [L618](../version-facts.md#L618) |
| Lava | "When flowing lava hits a water source directly from above, it creates stone." — [P1] §Blocks | vanilla behaviour remains (not separately verified in this audit) |

---

## 3. Beta 1.9 Prerelease 2 (`historical_experience:beta_1_9_pre_2`)

**Expected side.** [P2] https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_2 — "adds cauldrons, glass
bottles, potions, effects, animal breeding mechanics, 9 more music discs, Hardcore mode, and the ability to clear
effects by drinking milk."

**Actual side.** [version-facts.md L624–650](../version-facts.md#L624): `NO_ANIMAL_BREEDING` still active;
`NO_BREWING` still active; potion/brewing items refused by crafting; structures same as Pre1; 20 spawns.

### 3.1 Gap table

| # | Version | Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|---|---|
| P2-1 | Pre2 | Mobs/breeding | "Feeding animals makes them enter 'love mode' … Breeding will produce new animals."; chickens/cows/pigs/sheep "Can now be bred." — [P2] §Gameplay/§Mobs | `NO_ANIMAL_BREEDING` window is `before(Milestones.BETA_1_9_PRE_3)` — active at Pre2, inactive at Pre3 ([Feature.java:72](../../src/main/java/net/historicalexperience/version/Feature.java#L72), [L630](../version-facts.md#L630)); the mixin's own javadoc says "breeding arrives in the Beta 1.9 Prerelease 2" — the code keeps the older boundary | MISSING | High | High |
| P2-2 | Pre2 | Items/brewing | "Glass bottle … Crafted from glass."; "Blaze powder … Crafted from blaze rod."; "Fermented spider eye … Crafted from brown mushroom, sugar and spider eye."; "Magma cream … Crafted from blaze powder and slimeballs." — [P2] §Items | All four are dated Pre3 in the crafting gate ([src/.../HistoricalCraftingEra.java:152](../../src/main/java/net/historicalexperience/gameplay/HistoricalCraftingEra.java#L152)) and are refused at Pre2 [L642](../version-facts.md#L642) | MISSING | Medium | High |
| P2-3 | Pre2 | Items/potions & effects | "Potion … Drinkable, giving one or multiple effects"; "Has one of 32 generic names"; Effects gains Speed/Slowness/Haste/Instant Health/… — [P2] §Items/§Gameplay | No potions can be obtained in survival (`NO_BREWING` only ends at Pre3 [L630](../version-facts.md#L630)) and the creative catalog dates the potion item to **Release 1.3** ([src/.../HistoricalCreativeCatalog.java:56](../../src/main/java/net/historicalexperience/gameplay/HistoricalCreativeCatalog.java#L56)), so Pre2 has no potion item at all; cauldron prototype is deliberately not implemented [L628](../version-facts.md#L628) | MISSING | High | High |
| P2-4 | Pre2 | Items/music discs | Nine discs (11, blocks, chirp, far, mall, mellohi, stal, strad, ward) "Currently only obtainable in Creative mode." — [P2] §Items | Creative catalog dates all nine to `release_1_0_0` ([src/.../HistoricalCreativeCatalog.java:53](../../src/main/java/net/historicalexperience/gameplay/HistoricalCreativeCatalog.java#L53)); none is in Pre2's catalog | MISSING | Medium | High |
| P2-5 | Pre2 | World/mobs | "Cave spiders … health points have been reduced from {{hp\|20}} to {{hp\|12}}"; spiders "Now have {{hp\|16}}"; zombies "Now have {{hp\|20}} and {{armor\|2}}" — [P2] §Mobs | modern cave spider 12 / spider 16 / zombie 20+2 match these, but they are *unconditional* modern values: Pre1's 20/20 and Pre1 zombie's lack of armour are wrong (rows P1-9/B13) | UNKNOWN | Low | Medium |
| P2-6 | Pre2 | Mob/drops | "Spiders now drop 0-1 spider eye when killed by a player or tamed wolf."; cave spiders likewise — [P2] §Mobs | modern spider/cave-spider loot already does this, at every version; correct from Pre2 but one build early (P1-5) | UNKNOWN | Low | High |

### 3.2 Checked, no gap (Pre2)

| Domain | Expected (quote + URL) | Actual |
|---|---|---|
| Item repair | "Two of the same tools/weapons that are damaged in crafting will create one repaired tool/weapon." — [P2] §Gameplay | `repair_item` is a known modern special recipe (`logs/recipe-results.json`) and the crafting gate opens it exactly at Pre2 (`HistoricalCraftingEra.SPECIAL_RECIPES` [src/...:197](../../src/main/java/net/historicalexperience/gameplay/HistoricalCraftingEra.java#L197)) |
| Cauldron | "Present in the code, but not assigned to a block ID, making it unobtainable without mods." — [P2] §Blocks | cauldron is refused by crafting at Pre2 [L642](../version-facts.md#L642) — faithful to "unobtainable" |
| Hardcore | "Locks the difficulty to Hard and forces users to delete the world's save data upon death. The health bar changes appearance…" — [P2] | no version gate, so vanilla 1.21.1 hardcore supplies exactly this behaviour; not gated to Pre2 (see B18 for the earlier versions) |
| Milk | "Drinking milk resets all potion effects" — [P2] | modern milk does this |
| Glass pane | "No longer drops as an item when destroyed." — [P2] §Blocks | modern glass pane drops nothing |
| Lava burn | "set on fire for 15 seconds (300 ticks) instead of 30 seconds (600 ticks)" — [P2] §Blocks | modern burn duration is 15 s |
| Zombie/Spider/Cave spider stats | see P2-5 | modern matches Pre2 onwards |
| Tooltips | "Item tooltips … Now have a purple border." — [P2] §General | visual profile switches to MODERN tooltips at Pre2 [L648](../version-facts.md#L648) |
| Jump XP/held jump | "Removed debug code causing jumping to increase experience. Continuous jumping by holding down the jump key is now possible again." — [P2] §General | `JUMP_EXPERIENCE_BUG` inactive from Pre2; no held-jump gate (P1-4's removal never existed, so nothing to restore) |
| Third person | "Can now render the front of the player." — [P2] | modern third-person camera |
| Ender pearl | teleport added + "stack up to 16" — [EP] | modern teleport and 16 stack — correct from Pre2 |

---

## 4. Beta 1.9 Prerelease 3 (`historical_experience:beta_1_9_pre_3`)

**Expected side.** [P3] https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_3 — "adds five new blocks
including the brewing stand and enchantment table, as well as the eye of ender, five baby animal variants,
experience levels, the enchantment glint effect, and completed potion mechanics."

**Actual side.** [version-facts.md L652–678](../version-facts.md#L652): `ENCHANTING_PLACEHOLDER`,
`NO_REAL_ENCHANTING`, `INERT_END_PORTAL`, `END_PORTAL_FRAME_EXPLODES`, `END_PORTAL_FRAME_LIGHT_15` active;
brewing enabled (no `NO_BREWING`); XP level shown (`HIDDEN_XP_LEVEL` inactive); 0 enchantments; 76 refused
crafting ids; structures unchanged from Pre1.

### 4.1 Gap table

| # | Version | Domain | Expected (quote + URL) | Actual (facts) | Code / facts | Type | Sev | Conf |
|---|---|---|---|---|---|---|---|---|
| P3-1 | Pre3 | Enchanting | "The maximum enchantment level is 15." — [P3] §Enchanting | No cap exists; the modern menu's own 1–30 cost scale (15 bookshelves) is used. The click is reimplemented but the cost ladder is vanilla's | `EnchantmentMenuHistoricalEnchantingMixin` has no cap; facts "Enchantments: none" [L668](../version-facts.md#L668) | WRONG | Medium | Medium (modern 30-cost cap is vanilla default, not measured) |
| P3-2 | Pre3 | End portal | "Broken End portal frame blocks can be repaired with eyes of ender, though nothing further occurs." — [P3] §Blocks | Eye-of-ender insertion is refused entirely: `INERT_END_PORTAL` returns `InteractionResult.PASS` from `EnderEyeItem.useOn`, so the eye is not consumed and the frame stays empty | `EnderEyeHistoricalPortalMixin:57`; facts `INERT_END_PORTAL` [L658](../version-facts.md#L658) | WRONG | Medium | High |
| P3-3 | Pre3 | World/structures | "Glass pillars … Extend from the stone brick of the stronghold to the world's height limit. One pillar marks the location of the main stronghold room, and another marks the location of the portal room." — [P3] §World generation | Nothing generates them; the only glass-pillar mention in code is the removal at Pre4 (`HistoricalStructureRules`) | no feature | MISSING | Low | High |
| P3-4 | Pre3 | Items/brewing | Glistering melon is a **Pre4** addition: "Glistering melon … Used to brew potions of Healing. Crafted with gold nugget and a melon." — [P4] §Items | Allowed from Pre3: dated Pre3 in the crafting gate ([src/...:152](../../src/main/java/net/historicalexperience/gameplay/HistoricalCraftingEra.java#L152)), absent from the refused list [L670](../version-facts.md#L670), and added to the Pre3 creative catalog ([src/.../HistoricalCreativeCatalog.java:50](../../src/main/java/net/historicalexperience/gameplay/HistoricalCreativeCatalog.java#L50)) | EXTRA | Low | High |
| P3-5 | Pre3 | Items/potions | "Potion of Regeneration — Brewed by adding a ghast tear into an awkward potion." is Pre4 — [P4] §Items | Modern brewing is active from Pre3, so ghast tear → Regeneration already works | `BrewingStandHistoricalBrewingMixin` gate ends at Pre3; facts `NO_BREWING` absent [L658](../version-facts.md#L658) | EXTRA | Low | High |
| P3-6 | Pre3 | Items/potions | Splash potions are Pre4 additions: "Created by brewing potion with gunpowder. Has an area of effect when thrown." — [P4] §Items | Modern brewing allows gunpowder from Pre3 | same | EXTRA | Medium | Medium |
| P3-7 | Pre3 | Items/potions | "Most duplicate potions were removed (the total possible potions went down from 150 combinations to only 25 different potions in 31 combinations)." — [P3] §Brewing | Modern full potion set (long/strong variants and later effects) is brewable; no potion-set gate | same | EXTRA | Medium | Medium |
| P3-8 | Pre3 | Items/brewing | "Brewing potions with brewing stands currently takes 30 seconds." — [P3] §Brewing | Modern brew timer is 20 s (400 ticks); no timer override | no gate | WRONG | Low | Medium |
| P3-9 | Pre3 | Enchanting/eligibility | "Armor, axes, flint and steel, hoes, pickaxes, shears, shovels, and swords can be enchanted using an enchantment table." — [P3] §Enchanting | No eligibility gate: modern 1.21.1 item tags decide, and flint-and-steel/shears are not enchantable in modern (hoes may be). Pre4's change removing them is therefore true by default, but Pre3's inclusion of them is not implemented | no gate | MISSING | Low | Medium |
| P3-10 | Pre3 | Enchanting/placeholder | "Enchanted items display 'Enchanted!!' on their tooltip, and their name becomes blue." — [P3] §Enchanting | Implemented, but the placeholder also writes **Feather Falling I** onto every enchanted item (`HistoricalEnchanting.placeholderEnchantment`), so from Pre4 a sword can reveal a boots-only enchantment; the owner asked for the fixed reveal deliberately | `EnchantmentMenuHistoricalPoolMixin:63`, `ItemStackHistoricalEnchantedTooltipMixin` | WRONG | Low | High (code) |
| P3-11 | Pre3 | World/loot | "Golden apples can now be found in the new stronghold altar chests."; "Iron armor … Iron pickaxes … Iron swords" likewise — [P3] §Items; "Ender pearls can now be found in the new stronghold altar chests." — [EP] | Chest loot is filtered to remove *too-modern* items, but the modern stronghold tables are not rebuilt into the era's altar loot; whether altar chests exist at all is not verified | `HistoricalChestLoot`; no altar table | UNKNOWN | Low | Low |

### 4.2 Checked, no gap (Pre3)

| Domain | Expected (quote + URL) | Actual |
|---|---|---|
| XP level display | "Experience levels — Now shown above experience bar." — [P3] §Gameplay | `HIDDEN_XP_LEVEL` ends at Pre3 [L658](../version-facts.md#L658); level shown |
| Enchanting table | "Used to enchant tools, weapons, and armor with experience points"; "three randomized options … written in Standard Galactic Alphabet"; "Enchanting requires experience levels" — [P3] | table craftable from Pre3, click spends the offer's own cost, no lapis, glint + placeholder tooltip implemented |
| Brewing stand | "Brewing stands are now used for brewing potions instead of cauldrons." — [P3] | `NO_BREWING` ends at Pre3; modern stand works |
| Cauldron | "Now assigned with block and item IDs"; crafted with iron ingots — [P3] | `cauldron` allowed from Pre3 [L670](../version-facts.md#L670) |
| Baby animals | five baby variants "can be bred by feeding two parents with wheat" — [P3] | `NO_BABY_ANIMALS` ends at Pre3; modern babies |
| Sword damage | Stone 6→5, Iron 8→6, Diamond 10→7 — [P3] §Items | `OLD_SWORD_DAMAGE` ends at Pre3 [L658](../version-facts.md#L658); post-nerf totals 5/6/7 |
| Enderman health | "Endermen now have {{hp\|40}}" — [P3] | modern 40 |
| End portal frame quirks | "Can be obtained by explosions"; "Gives off a light level of 15" — [P3] | `END_PORTAL_FRAME_EXPLODES` + `END_PORTAL_FRAME_LIGHT_15` exactly Pre3, runtime-probed per implementation coverage |
| Portal block | "End portal block — Has no function. Gives off a light level of 15." — [P3] | `INERT_END_PORTAL` active (though see P3-2 for the frame half) |
| Stronghold portal room | "The End portal room was added." — [P3] | `minecraft:stronghold` allowed (but already present at 1.8.1 — B17) |
| Bookshelves | "When destroyed, drop 3 books." — [P3] §Blocks | modern bookshelf drops 3 books |
| Sheep | "Now have {{hp\|8}}" — [P3] | modern sheep 8 |
| Hotbar debug loadout | the original Pre3 upload spawned debug blocks; the *reuploaded* client removed them — [P3] intro | profile represents the reupload: no debug loadout |

---

## 5. Beta 1.9 Prerelease 4 (`historical_experience:beta_1_9_pre_4`)

**Expected side.** [P4] https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_4 — "It adds the End
dimension, the ender dragon, and moon phases. It also completes the enchanting mechanics…"

**Actual side.** [version-facts.md L680–706](../version-facts.md#L680): 17 enchantments; `SILKY_SPAWNERS`
(exactly Pre4); `INERT_END_PORTAL` inactive; End reachable; End's own generation and the dragon not implemented
per the evidence string; status/silk-touch details.

### 5.1 Gap table

| # | Version | Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|---|---|
| P4-1 | Pre4 | World/the End | "New dimension, accessible through the End portal. Endermen spawn here. The ender dragon does not spawn yet. An obsidian platform spawns in the center of the island whenever a player enters the end."; "Obsidian pillars — Generate in the End." — [P4] §World generation | The End is the vanilla modern generator; the evidence states "NOT implemented: the End's own generation beyond the reachability gate" [L684](../version-facts.md#L684). Modern End outer islands (1.9+ content), its plants and any content not expressly suppressed therefore generate; end cities and End gateways are expressly suppressed in code (`HistoricalFeatureExclusions` END_GATEWAY_RETURN, structure suppression) | EXTRA | High | High (generation modern), Medium (exact leftover content) |
| P4-2 | Pre4 | World/dragon | "The ender dragon does not spawn yet." — [P4] §World generation; Pre5: "even though it still does not spawn. Its health has been changed to {{hp\|1}} to test this." — [P5] §Mobs | No dragon-spawn suppression exists (grep of `createDragon/ENDER_DRAGON` finds no gate; the existing dragon mixins only change *behaviour* of an existing fight). The vanilla End's `EndDragonFight` spawns a dragon on first entry, so a Pre4/Pre5 End has a live, killable dragon at 200 HP | WRONG | High | Medium (inferred from absence of a gate; settle by a probe) |
| P4-3 | Pre4 | World/entities | "Ender crystal … They appear on all obsidian pillars in the End." is a **Pre6** addition — [P6] §Non-mob entities | The vanilla End's fight places/uses End crystals on the pillars at every version, so they exist at Pre4/Pre5 too (round root cause: P4-2) | EXTRA | Medium | Medium |
| P4-4 | Pre4 | UI | "A 'Boss health' bar appears … in the same purple hue as the ender dragon." is a **Pre6** addition — [P6] §Mobs | `BOSS_HEALTH_CAPTION` is active before Release 1.4 [L704](../version-facts.md#L704), and the vanilla dragon's boss bar shows whenever the (early) dragon exists | EXTRA | Medium | Medium |
| P4-5 | Pre4 | Enchanting | "The maximum enchantment level of the enchantment table is now 50."; "Enchantment tables now require bookshelves to get maximum enchantments, up to level 50 with 30 bookshelves." — [P4] §Gameplay | The historical click is reimplemented but the option/cost ladder is vanilla's 1–30 (15 bookshelves); no cap or bookshelf-count override exists | WRONG | Medium | Medium |
| P4-6 | Pre4 | Blocks/data | "End stone — The white cobblestone texture introduced in the previous pre-release was changed, and this overhauled version is used by end stone." — [P4] §Blocks | `minecraft:end_stone` is dated **Release 1.0** in `HistoricalBlockAvailability:661`, so it is judged anachronistic at Pre4 [L702](../version-facts.md#L702) even though this page adds it; it is simultaneously in the Pre4 creative catalog (`HistoricalCreativeCatalog:51`) — self-contradictory data | WRONG | Low | High (data), Unknown runtime effect |
| P4-7 | Pre4 | Enchanting | Silk Touch becomes incompatible with Fortune only at **Pre5**: "Silk Touch is now incompatible with Fortune." — [P5] §Enchanting | Modern enchantment data marks them incompatible at every version, so the Pre4 (buggy, compatible) state is not reproduced | WRONG | Low | Medium |
| P4-8 | Pre4 | World/spawning | Slime height is raised from 0–16 to 0–40 only at **Pre5**: "Now spawn on levels 0-40 rather than 0-16" — [P5] §Mobs | No slime-height gate; modern slime-chunk range (below Y≈40) applies, so Pre4 slimes already spawn up to 40 (recorded as unimplemented in implementation coverage) | WRONG | Low | Medium |
| P4-9 | Pre4 | Visuals | "Sun and moon — Now rise in the east and set in the west … and are round." — [P4] §Gameplay; square again at Pre6 — [P6] | Modern sun/moon textures are square (no renderer gate), so Pre4/Pre5 show the Pre6 shape | WRONG | Low | Medium |

### 5.2 Checked, no gap (Pre4)

| Domain | Expected (quote + URL) | Actual |
|---|---|---|
| Enchantments | "Added 17 enchantments" (the exact 17 are listed on the page); "Enchanted items' tooltips now display their enchantments instead of 'Enchanted!!'" — [P4] | facts list exactly the page's 17 [L696](../version-facts.md#L696); `NO_REAL_ENCHANTING`/`ENCHANTING_PLACEHOLDER` end at Pre4; pool filtered to the era's set |
| Enchanting mechanics | "Enchanting is now server-side"; old table rules (no lapis, cost = requirement) — [P4] fixes; `HistoricalEnchanting` | server-side click implemented; lapis not charged; cost = requirement |
| End portal active | "When an entity makes contact with it, they teleport into the End or back to the Overworld respectively now." — [P4] §Blocks | `INERT_END_PORTAL` inactive from Pre4; portal travel gated by `EnderEyeHistoricalPortalMixin`/`HistoricalPortalSpawn` |
| Eye of ender locator | "Added ability to use with right click to locate a stronghold." — [P4] §Items | modern eye-of-ender locator |
| Splash potions | list + "Created by brewing potion with gunpowder." — [P4] §Items | modern splash brewing works (but one build early — P3-6) |
| Healing potion | "now it is brewed from awkward potion and glistering melon" — [P4] | modern recipe matches |
| Endermen daylight | "Endermen used to burn in sunlight, but as of this update, they instead randomly teleport around" — [P4] | modern enderman behaviour matches, but it was applied before Pre4 too — see B19 |
| Spiders | "Spiders are now immune to the Poison status effect"; "now considered arthropods" — [P4] | modern spider behaviour matches |
| Version label | "Version number — Is no longer displayed, unless F3 is pressed." — [P4] | label hidden only at Pre4 in the visual profile [L704](../version-facts.md#L704) |
| Moon phases | "The moon now has multiple phases" — [P4] | modern moon phases (present earlier too — see B20) |
| Obsidian platform | see P4-1 | vanilla End platform on entry matches |
| Crit sync | "Critical hits now appear in multiplayer." — [P4] fixes | modern crits are server-authoritative |
| Enchanting table | "Now named 'Enchantment Table'"; "Hoes, flint and steel, and shears can no longer be enchanted" — [P4] | modern name/tags; eligibility removal true by default |
| Silk Touch spawner (bug) | Pre4 is the build where Silk Touch *could* take spawners, fixed Pre5 — [P5] §Enchanting | `SILKY_SPAWNERS` exactly Pre4, runtime-probed (coverage item 33) |
| Sword damage | post-nerf from Pre3 onward — [P3] | post-nerf table active from Pre3 |

---

## 6. Beta 1.9 Prerelease 5 (`historical_experience:beta_1_9_pre_5`)

**Expected side.** [P5] https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_5 — "adds several new
achievements and controls, changes some mobs and the texture of some blocks, and fixes some bugs."

**Actual side.** [version-facts.md L708–734](../version-facts.md#L708): 17 enchantments; same spawn set;
dragon not spawning not implemented; no command/achievement features; `VERSION_LABEL` shown again.

### 6.1 Gap table

| # | Version | Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|---|---|
| P5-1 | Pre5 | UI/achievements | "Added 10 achievements" (Overkill, Enchanter, Librarian, DIAMONDS!, We Need to Go Deeper, The End?, The End., Local Brewery, Into Fire, Return to Sender) — [P5] §Gameplay | No historical achievement system: the game runs 1.21.1 advancements and no gate/feature was found for achievements | MISSING | Medium | Low (no feature ≠ proof; but no code found) |
| P5-2 | Pre5 | Commands | "{{cmd\|toggledownfall}} — Toggles the weather." — [P5] §Commands | No such command is registered; the only occurrences in `src/main` are evidence strings in `HistoricalStructureRules`/`LegacyCatalog`, and the game offers modern `/weather` only | MISSING | Low | High |
| P5-3 | Pre5 | Blocks/creative | "Mycelium and end stone are now available in the creative inventory list."; "Lily pads … have the correct coloration in the inventory and have a name tag." — [P5] §Fixes | `mycelium` is absent from the historical creative catalog entirely (grep: no mycelium entry), so it is hidden in every historical profile; end stone is in the catalog from Pre4 (one build early) | MISSING | Low | High (catalog), Medium (gate effect) |
| P5-4 | Pre5 | World/water | "Water — Now flowing downwards creates source blocks, instead of flowing water." — [P5] §Blocks | Not implemented; modern downward flow (no new source) applies. Pre6 reverts it, so only Pre5 diverges | MISSING | Low | Medium |
| P5-5 | Pre5 | World/dragon | "Now has a new dying animation even though it still does not spawn. Its health has been changed to {{hp\|1}} to test this." — [P5] §Mobs | The dragon is alive at full 200 HP (P4-2); no Pre5 death-animation/health test state | WRONG | Medium | Medium |
| P5-6 | Pre5 | World/spawning | "Now spawn on levels 0-40 rather than 0-16, making them much more common." — [P5] §Mobs | Pre4 should be 0–16 (P4-8); at Pre5 the modern slime-chunk range is about 0–39 inside this project's 0..127 envelope, so the Pre5 number is approximately matched by accident, not by a gate; swamp slime spawning might add more | WRONG | Low | Low |
| P5-7 | Pre5 | Items/creative | "In Creative mode, using an empty bucket no longer fills it up when removing fluids with it." — [P5] §Items | No gate found; whether modern creative buckets still fill is unverified | UNKNOWN | Low | Low |

### 6.2 Checked, no gap (Pre5)

| Domain | Expected (quote + URL) | Actual |
|---|---|---|
| Silk Touch / spawners | "Silk Touch can no longer pick up monster spawners and lit furnaces." — [P5] §Enchanting | `SILKY_SPAWNERS` only at Pre4 → Pre5 drops nothing from a spawner (runtime-probed per coverage item 33); modern silk-touch furnace returns a plain furnace (lit state not obtainable) |
| Silk Touch / Fortune | "Silk Touch is now incompatible with Fortune." — [P5] | modern data is incompatible from every version (Pre4 wrong — P4-7) |
| Version label | "The version number once again displays on the top-left of the screen." — [P5] §HUD | `VERSION_LABEL` active at Pre5 [L732](../version-facts.md#L732) |
| Version commands | "{{cmd\|xp}} — The command take a player name and then a number of xp orbs to award." — [P5] | modern `/xp` exists and works |
| Clouds option | "Video settings — Added an option to turn off clouds." — [P5] | modern video settings has Clouds; the option therefore exists (earlier than Pre5 as well) |
| Mob neutrality | "Hostile mobs act neutral in Creative mode."; "Entity punch range in Creative mode reduced" — [P5] | modern creative mob neutrality |
| Enchanted blocking | "Data error when attempting to block with an enchanted sword fixed, blocking with enchanted sword now possible." — [P5] §Fixes | modern blocking works with enchanted swords |
| Melon drops | "Melons once again drop the correct number of slices." — [P5] §Fixes | modern melon drops |
| Multiplayer breeding | "In multiplayer, breeding two animals now gives birth to a baby animal instead of a fully-grown animal." — [P5] | vacuous here: breeding itself is missing at Pre2 and only starts at Pre3 (P2-1); babies are modern |
| Mycelium/End Stone names | "Now named 'Mycelium'"; "End stone now has a name tag" — [P5] | modern item names |
| Slime (Pre5 value) | 0–40 — [P5] | see P5-6: approximately matched, not gated |

---

## 7. Beta 1.9 Prerelease 6 (`historical_experience:beta_1_9_pre_6`)

**Expected side.** [P6] https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_6 — "adds dragon eggs,
ender crystals, credits, new video settings, changes the mining time of some blocks, changes the ender dragon
boss fight, and fixes some bugs."

**Actual side.** [version-facts.md L736–762](../version-facts.md#L736): dragon code present, original-fight
behaviour implemented (no gateway, no re-summon, "Boss health" caption), but the dragon already exists at Pre4/5
so this build has no unique spawn moment; 17 enchantments; same spawn set/structures.

### 7.1 Gap table

| # | Version | Domain | Expected (quote + URL) | Actual (facts) | Type | Sev | Conf |
|---|---|---|---|---|---|---|---|
| P6-1 | Pre6 | Blocks | Known build bug: "Wooden doors can be opened only using redstone." — [P6] §Bugs | Not emulated; modern doors open by hand. The evidence string records the bug as "recorded rather than fixed" [L740](../version-facts.md#L740) | MISSING | Low | High |
| P6-2 | Pre6 | Enchanting/Silk Touch | "Silk Touch can no longer pick up ice, Infested Block, and lit Redstone Ore." — [P6] §Enchanting | No gate; modern silk touch yields ice and infested blocks, so those are obtainable at Pre6 | EXTRA | Low | Medium |
| P6-3 | Pre6 | Blocks/creative | "Ferns and grass are now available in the creative inventory." — [P6] §Fixes | catalog adds `short_grass fern` exactly at Pre6 (`HistoricalCreativeCatalog:52`) — matched; but carried gap P5-3 (mycelium still absent) applies | MISSING | Low | High |
| P6-4 | Pre6 | Items/creative | "Dragon egg — Can be obtained only once per world by defeating the ender dragon." — [P6] §Blocks | The egg is produced by the dragon death but is absent from the historical creative catalog (grep: no `dragon_egg` entry), so it cannot be obtained in creative mode | MISSING | Low | Medium (gate effect on creative only) |

### 7.2 Checked, no gap (Pre6)

| Domain | Expected (quote + URL) | Actual |
|---|---|---|
| Dragon spawn | "Now spawns in the end." — [P6] §Mobs | vanilla End spawns a dragon; original-fight behaviour is implemented (`HistoricalDragonFight`, `EndDragonFightHistoricalMixin`, phase/vertical-drive mixins) — but earlier than Pre6 (P4-2) |
| Boss bar | "A 'Boss health' bar appears on the middle top part of the screen" — [P6] | caption "Boss health" implemented via `HistoricalDragonFight.bossBarCaption` |
| Exit portal/egg | "When killed, now leaves behind an exit portal with the dragon egg sitting on top to return the player to their spawn point." — [P6] | vanilla death handler; the Pre6-missing part, no End gateway, is gated off |
| Credits/End Poem | "Credit sequence"; "End Poem" — [P6] §General | vanilla credits/poem exist |
| End crystals | "They appear on all obsidian pillars in the End." — [P6] | modern crystals (present earlier too — P4-3) |
| Baby sheep dye | "Baby sheep from dyed parents are now born dyed instead of always being white." — [P6] §Mobs | modern sheep inheritance |
| Water revert | "Downward flowing water now creates flowing water once more." — [P6] §Blocks | modern flowing water |
| Sun/moon | "Are now square again." — [P6] | modern square textures (Pre4/5 wrong — P4-9) |
| Enchantment table mining | "Enchantment tables now take time to mine, instead of one punch." — [P6] | modern mining time |
| Creative durability | "Using items and hitting mobs no longer uses up items/durability in Creative." — [P6] §Fixes | modern creative behaviour |
| Block drops | stairs drop themselves, coal/lapis/redstone ore smeltable, redstone Fortune, etc. — [P6] §Changes | modern block behaviour matches |
| Grass/fern | "Ferns and grass are now available in the creative inventory." — [P6] | catalog at Pre6 (P6-3) |
| End fire | "Fire above bedrock in the End now burns forever." — [P6] §Blocks | vanilla End fire behaviour |
| Particle setting | "Video settings — Added 'Particles' setting" — [P6] §General | modern video options have it |
| Tool hint | "Tools not suited to digging a certain block can be faster than digging by hand" — [P6] §Items | modern wrong-tool speed is not separately verified (see Could not verify) |
| Armour durability | "Armor durability is back to being much higher, higher than it ever was in Beta 1.8." — [P6] §Fixes | modern armour durability; the Pre1 durability increase was never implemented, so the intermediate values are not historical (folded into P1-3) |

---

## 8. Top 10 highest-impact gaps

Ranked by effect on a player's experience of the era, not by ease of fix.

1. **Pre2 has no animal breeding (P2-1).** Breeding is the headline Pre2 feature and is gated to Pre3;
   the mixin's own comment says Pre2. Food/wheat does nothing. High severity, High confidence.
2. **Pre2 has no potions, effects-as-items, or brewing ingredients (P2-2/P2-3).** The brewing chain is
   gated to Pre3 and the creative catalog dates potion/splash items to Release 1.3, so Pre2 has no potion
   item at all in any game mode. High severity, High confidence.
3. **The Ender Dragon exists from Pre4, two prereleases early (P4-2).** No spawn suppression exists; the
   End contains a live boss, crystals and boss bar at Pre4/Pre5, and Pre6 therefore has no unique milestone.
   High severity, Medium confidence (settle with a Pre4 End probe).
4. **The End uses the modern 1.9+ generator (P4-1).** The Pre4 End should be a single island with obsidian
   pillars; the mod generates the modern End (outer islands and whatever is not expressly suppressed),
   so End exploration is out of era. High severity, High confidence.
5. **The Pre1/Pre2 updates' own crafting is refused (P1-1/P1-2, P2-2).** Nether bricks/stairs/fence and
   gold nuggets are dated Pre3; glass bottle/blaze powder/fermented spider eye/magma cream likewise. A
   player in Pre1/Pre2 cannot craft the items the wiki says were added in that very build. Medium severity,
   High confidence.
6. **Pre1's armour model change is not implemented (P1-3).** Armour still reduces fire/explosion/projectile
   damage, has modern durability and no durability-based effectiveness decay; recorded as `A` in
   implementation coverage. Medium severity, High confidence.
7. **Mob health/armour are modern at 1.8.1/Pre1 (B12/B13/P1-9).** Enderman 40 instead of 20, cave spider 12
   instead of 20, silverfish 8 instead of 20, zombie already wearing its Pre2 armour. Medium severity, High
   confidence.
8. **Pre-Pre3 sword damage is wrong for stone and iron (B5).** 5 and 7 HP instead of the wiki's 6 and 8; the
   pre-nerf table's own comments/test repeat a bad addition (modifier 6 + base 1 ≠ total 8). Medium severity,
   High confidence.
9. **Enchanting caps are modern (P3-1/P4-5).** Pre3 should cap at 15 and Pre4 at 50 with 30 bookshelves;
   both use vanilla's 1–30 ladder. Medium severity, Medium confidence.
10. **Stronghold generation is modern (B16/B17): 128 strongholds with a Pre3 portal room instead of 3
    strongholds without one.** Medium severity, Medium confidence. *(Tied 10th: Pre1's nether-wart block is
    dated Release 1.0 in the block table — P1-10 — which can remove the wart the Pre1 page says is found in
    fortresses.)*

Also close to the top: the Pre3 eye of ender cannot fill a portal frame at all (P3-2), which breaks the
Pre3 End-content story the wiki describes; and the Pre2 music discs are missing from creative (P2-4).

---

## 9. Could not verify

Each item names the concrete evidence that would settle it.

1. **Pre1 snowfall-destroys-redstone regression.** Not on the fetched Pre1 page (the page's only related rows
   are the jump-experience bug and the held-jump removal). The project records it as unsourced and deliberately
   unimplemented (implementation coverage item 31). Settle by disassembling the Pre1–Pre4 client jars named in
   the page infobox.
2. **Critical-hit exact formula at 1.8.1–Pre2 (B4).** The wiki is qualitative ("depending how close the player
   was to the ground while falling"); the mod's 1.0–2.0 random multiplier is from `historical-features.json`
   (medium confidence) and the "+2 HP" term/after-enchantment ordering are knowingly missing. Settle by
   disassembling `Player.attack` in the Beta 1.8 client.
3. **Exhaustion costs for being hurt and for mining (B3).** No numeric source; the implemented 0.3/0.2/0.8
   cover attack/jump/sprint-jump only. Settle from the FoodStats/player classes in the artefacts.
4. **Cake hunger (B1).** Research files disagree: the Beta 1.8 page says 3, the project's food history says 2;
   implementation coverage records the conflict as unresolved. Settle from the Beta 1.8 client's cake
   `FoodStats` call or the item component data.
5. **Rotten-flesh/raw-chicken Hunger-effect probability and duration (B2).** No numbers on the page; modern
   components are used unmodified. Settle from the client jar.
6. **Held left-click swings once (B21, the Beta 1.8 control change).** No gate exists; modern hold-to-attack
   auto-repeats. This is inferred from absence of a gate and needs a play test.
7. **Pre1 villagers' five professions / TESTIFICATE / pig AI (P1-6).** The wiki page states them; whether the
   `VillagerProfession.NONE` choice is the only divergence was not play-tested, and the modern trade UI may
   still be reachable. Settle with a Pre1 village play test and the villager/trade page history.
8. **Block-table data errors' runtime effect (P1-10, P4-6).** `nether_wart` dated Release 1.0 and `end_stone`
   dated Release 1.0 contradict the Pre1 and Pre4 pages; whether the generation veto actually strips fortress
   wart rooms or End stone was not probed (End stone is placed through `SurfaceSystem`/`BlockColumn`, which the
   veto does not cover, per implementation coverage item 11). Settle with a Pre1 nether-fortress chunk scan and
   a Pre4 End chunk scan.
9. **End content beyond the central island (P4-1).** The Pre4 page describes the island and pillars but not
   what did not exist; end cities/gateways are suppressed in code, chorus/other generated content may not be.
   Settle with a Pre4 End generation probe listing block/entity palettes.
10. **Dragon spawn at Pre4/Pre5 (P4-2).** Inferred from absence of any spawn gate; needs an actual server
    probe entering the End in a `beta_1_9_pre_4` world and reporting the dragon entity.
11. **Stronghold count (B16) and portal-room presence (B17).** "128 strongholds" is the vanilla default and
    was not measured in this audit; portal-room piece suppression was not measured either. Settle with a
    structure probe on a 1.8.1 world.
12. **Pre1 terrain landmass/ocean height (P1-11).** Whether `moderner_beta:beta_1_9_pre_3` carries the Pre1
    change was not measured. Settle by comparing the Pre1 and Pre3 client terrain constants or by sampling
    continent scale.
13. **Enchantment caps (P3-1/P4-5), brew time (P3-8), potion-set extras (P3-6/P3-7).** Modern defaults are
    the assumed behaviour; no probe measured the menu costs or the brew timer. Settle with an in-game/unit
    probe of `EnchantmentMenu.costs` and the brewing block entity timer.
14. **Item enchantability at Pre3 (P3-9).** Which items 1.21.1 tags make enchantable (especially hoes) was not
    checked at runtime. Settle by querying the enchantable item tags.
15. **Hardcore availability at 1.8.1/Pre1 (B18).** No hardcore gate exists, but whether the create-world flow
    exposes it for a historical profile was not executed. Settle by opening the create-world screen for those
    profiles.
16. **Moon-phase presence before Pre4 (B20) and round sun/moon at Pre4/Pre5 (P4-9).** Modern renderer
    defaults are the assumed actual; no client screenshot was taken. Settle by client visual check.
17. **Wrong-tool mining speed at Pre6 (P6 table).** The page describes a speed relation; modern behaviour was
    not measured. Settle by timing a pickaxe on dirt in 1.21.1 vs the expected relation.
18. **Slime spawn range (P5-6) and creative-bucket behaviour (P5-7).** Unmeasured modern behaviour; settle by
    play test.
19. **Beta 1.8.1's own two fixes and the "hit mobs from far away" bug.** The shift-click crash fixes are
    matched trivially by modern code; the known 1.8.1 reach bug is not emulated (it is a bug, not a feature,
    and the project only emulates catalogued bugs with sources). Noted rather than scored as a gap.
20. **Achievements (Beta 1.8 Sniper Duel, Pre5's ten).** No historical achievement implementation was found,
    but "no gate found" is weaker than a probe; settle by inspecting the advancement UI/registry for a
    profile.

---

## 10. Method notes and scope limits

- All wiki pages were fetched with `?action=raw` on 2026-10-21; the exact raw text is the quote source. Page
  titles were verified (Pre1 is `Java_Edition_Beta_1.9_Prerelease`, not `…_Prerelease_1`).
- The ACTUAL side is the generated facts file plus targeted code reading. No Gradle task, test or server was
  run for this audit (prohibited by the task); therefore every "not implemented" claim that rests on the
  absence of a gate is labelled Medium/Low confidence and listed in *Could not verify*.
- Out of scope and unchanged: `research/version-facts.md`, other research files, code, tests and PROGRESS.md.
  This file, `research/gaps/beta-1.8-1.9pre.md`, is the only artefact written.
