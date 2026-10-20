# Version gaps — what each declared version is still missing

A per-version fidelity audit of all 34 declared profiles. It compares **what the code actually does** (generated into
[version-facts.md](../research/version-facts.md) from the live APIs) with **what the version actually had** (per-version
wiki history, the project's own research artefacts, and citations).

Four era audits hold the full evidence — one row per gap, with a quote and URL for every expected claim:

| Era | Versions | Document | Size |
| --- | --- | --- | --- |
| Indev, Infdev, Alpha | 13 | [indev-infdev-alpha.md](../research/gaps/indev-infdev-alpha.md) | 20 cross-version rows + 13 per-version sections |
| Beta 1.0 – 1.7.3 | 7 | [beta-1.0-1.7.md](../research/gaps/beta-1.0-1.7.md) | 135 rows |
| Beta 1.8.1 – 1.9 Pre6 | 7 | [beta-1.8-1.9pre.md](../research/gaps/beta-1.8-1.9pre.md) | 71 gaps + 91 checked-and-matched |
| Release 1.0 – 1.6.4 | 7 | [release.md](../research/gaps/release.md) | 138 rows |

**[verified here]** means the Lead checked the code and a cited source directly. Everything else is the era audits'
finding with its own citation; where a claim could be checked it was, and two were found wrong and corrected (see G3 and
G2 — both were my own errors, caught by re-reading the code and by a researcher's contradicting evidence).

## 1. Systemic findings

### G1 — Mob drops are modern in 29 of 34 versions **[verified here]** (HIGH)

Only five versions declare drop tables: Indev, both Infdev builds, Beta 1.7.3 and Beta 1.8.1. Every other version — all
ten Alpha profiles, Beta 1.0–1.6, the seven 1.9 prereleases and the seven Release profiles — uses the **1.21.1 loot
tables**: rotten flesh (Beta 1.8 content), spider eyes (Beta 1.8), raw chicken and beef (Beta 1.8), bones (Beta 1.2),
ghast tears, pigman gold.

minecraft.wiki, [Zombie](https://minecraft.wiki/w/Zombie): *"1.8 (Pre-release) — Zombies now drop 0-2 rotten flesh
instead of 0-2 feathers"*; *"20100219 — Zombies now drop 0–2 feathers"*. Every declared version before Beta 1.8 must
drop feathers, and rotten flesh did not exist there. The Alpha and Beta audits independently rank this first for their
eras. Also **[verified in era audit]**: an unshorn sheep drops wool on death in Alpha and Beta 1.0–1.6 (the death wool
is Beta 1.7), and shears give 1–3 rather than the documented 2–4.

### G2 — RETRACTED: eating does *not* do nothing in Beta 1.0–1.6 **[verified here]**

I first read this from the facts file, which said `Food: none` for those six profiles. Wrong reading: the file showed the
**declared** table while the hook applies the **effective** value, and `HistoricalFoodHelper.researchedHeal` deliberately
falls back to the pre-1.8 table when a version declares none:

> "a version whose heals were never researched declares an empty map, and for those the pre-1.8 table remains a better
> answer than modern nutrition … That keeps Infdev's behaviour unchanged rather than silently 'fixing' it"

`VersionFactsTest` now prints declared and effective separately. The **real** food defects are narrower: cake heals
nothing from Beta 1.2 (its heal is a block interaction), cookies cannot stack from Beta 1.4 (`UNSTACKABLE_FOOD` caps
*all* food at 1), and the Release-era apple/regeneration values below.

### G3 — Release 1.2.5–1.5 cannot craft their own era's blocks **[verified here]** (HIGH; direction corrected)

My first version of this finding said the opposite — that 1.3/1.4 could craft 1.5's Redstone Update — because I probed
`isRefused(version, item, null)`, which tests only the item allow-list. The in-game call passes the **result block**, and
`HistoricalRecipeGate` then asks the block-availability table. Probing both (as `VersionFactsTest` now does) shows the
real picture:

| Version | Refused by the block-date gate (should be craftable) |
| --- | --- |
| Release 1.2.5 | redstone lamp (1.2.1 content) |
| Release 1.3 | ender chest — **the one block 1.3.1 added**, plus 1.5's set |
| Release 1.4 | ender chest, anvil, beacon, cobblestone wall, flower pot (all 1.3.1/1.4.2 content) |
| Release 1.5 | the above plus the entire Redstone Update: hopper, dropper, comparator, daylight detector, activator rail, quartz block |
| Release 1.6.4 | none |

So the defect is the block-availability table timeline-dating genuine 1.3.1/1.4.2/1.5 content to Release 1.6.4, and the
crafting gate faithfully refusing it there. A 1.3 world cannot craft an ender chest; a 1.5 world cannot craft a hopper.
The Release audit calls this the single largest defect cluster in its era, and the direction of the bug is the reverse of
what a null-block probe suggests — worth remembering for future audits.

### G4 — Recipe *ingredients and counts* are modern wherever no removed recipe is supplied **[verified here]** (HIGH)

The crafting layer answers "may this be crafted", not "with what". Confirmed examples: golden apple (G6), arrows (iron
ingot before Alpha 1.0.14), wool (9 string until Beta 1.6.6, 4 after), slab/ladder/pressure-plate output counts before
Release 1.3, and the stone slab itself (the era's `smooth_stone_slab` is refused as Beta 1.3 while the 1.14
`stone_slab` is craftable with the wrong ingredient and count).

### G5 — Mob AI is modern except for thirteen gates **[verified here]** (HIGH; magnitude inferred)

Of 82 features only **13** concern mobs: strafing, panic, sun avoidance, breeding, tempting, baby animals, item pickup,
gear, enchants, baby zombies, zombie arms, punch-shears, Nether endermen. Everything else is 1.21.1 — obstacle
pathfinding, door breaking, spider jockeys, creeper blasts, enderman teleport/block pickup, slime behaviour, and all
spawn **weights, pack sizes, caps and light thresholds**. The Alpha audit ranks modern pathfinding third for its era.

### G6 — Golden apple: uncraftable before Beta 1.8, wrong recipe and effects after **[verified here]** (HIGH)

`golden_apple` is allow-listed only from `beta_1_8_1`, so Beta 1.0–1.7.3 cannot craft it although it was added in
**Infdev 20100227**. The recipe was eight **gold blocks** until Release 1.1, eight **gold nuggets** from Release 1.1, and
eight **ingots** only from 1.9 — so the modern recipe is wrong for every declared version. The Release audit adds that
the *effects* are wrong too: Release 1.1–1.5 should be Regeneration I for 5 s, and 1.6.4 should be 10 s, not the modern
Regeneration II + Absorption. Fix belongs in `RemovedRecipes`, generalised so each removed recipe carries its own window
(the enchanted golden apple already does, for `[1.3, 1.9)`).

### G7 — Dated-table coverage limits **[verified here]** (MED)

* `HistoricalBlockAvailability` cannot judge **51 of its 145** tracked blocks (no date), answering `UNKNOWN` — allowed but
  counted — for older profiles. That limitation is also what causes G3: a *palette-derived* date got promoted to a
  timeline date.
* `HistoricalItemAvailability` dates **80 items**, all because they appear in a vanilla chest table; it is not an item
  registry.
* The creative catalogue and the crafting allow-list contradict each other on compass and clock (catalogue dates them
  Alpha 1.1.0/1.2.0; the allow-list has both in `always(…)`, so they are craftable from Alpha 1.0.1).

### G8 — Every Release world from 1.2.5 is 128 blocks tall; the limit rose to 256 in 1.2 **[verified here]** (HIGH)

All Release presets use `historical_experience:beta_1_7_3_overworld` (height 128, `min_y` 0); the Release audit records
the wiki's 1.2.1 raise to 256. So Release 1.2.5, 1.3, 1.4, 1.5 and 1.6.4 worlds cannot build above y=127, and the terrain
is clipped where those builds had 256. Release 1.0/1.1 are correctly 128. Because presets are append-only, the fix is a
new revision and dimension type per affected version, not an edit.

### G9 — Geared mobs stop at Release 1.6.4, so 1.4/1.5 never have them **[era audit]** (MED)

The `NO_MONSTER_*` windows end at Release 1.6.4 (the proxy used for the 1.4.2 change), so a Release 1.4/1.5 world has no
monster armour, enchants or item pickup although 1.4.2 added them. A proxy artifact, but observably wrong for two
profiles.

### G10 — The Ender Dragon exists from Pre4, three prereleases early **[era audit]** (MED)

The original-fight mixins are implemented, but there is no spawn suppression, so Pre4 and Pre5 get a live 200-HP dragon
with crystals and the boss bar; Pre6 loses its milestone. Pre3's eye of ender also cannot fill a portal frame at all
(`INERT_END_PORTAL` returns PASS), where the wiki says frame repair works.

### G11 — The Pre2/Pre3 boundary is shifted by one prerelease, in both directions **[era audit]** (HIGH)

Breeding, potions and brewing ingredients, music discs and the dragon start a boundary late; nether bricks, gold nuggets,
glistering melon and splash potions are refused a boundary too long. This is one structural error, not seven bugs, and
`NO_ANIMAL_BREEDING` is the clearest: its own evidence says Prerelease 2 while the window says 3.

### G12 — XP and enchanting are modern **[era audit]** (MED)

Every profile uses the 1.21.1 XP curve (1.3.1 introduced 17 XP/level); 1.0–1.2.5 award ore/smelting/spawner XP that 1.3.1
added; Pre3 should cap at 15 and Pre4 at 50 with 30 bookshelves; the enchant cap and costs are vanilla's.

### G13 — No hardcore gate **[era audit]** (LOW/unverified)

Vanilla 1.21.1 hardcore is selectable for every profile; it is anachronistic at 1.8.1/Pre1 but whether the create-world
flow exposes it for historical profiles was not executed.

## 2. Per-era headline gaps

Full evidence in each era document. The ranked lists there are by player impact, not row count.

**Indev, Infdev, Alpha** — modern drops in all ten Alpha profiles; **no craftable cloth/leather armour in Alpha
1.0.1/1.0.4/1.0.6** (armour was cloth until 1.0.8 and cows do not exist until then, so only the leather recipe is
offered; iron/gold/diamond armour are still craftable); modern pathfinding AI; sheep death wool; the modern F3 overlay with coordinates (added Alpha v1.2.3); compass
and clock too early; the stone-slab substitution; arrow recipe; 9-vs-4-string wool; leaf sapling 10% vs 5%, leaf
stick/apple drops (1.14/1.1 content) and **leaf decay at 1.2.0/1.2.2a, where leaves did not decay until v1.2.3**;
spider jockeys before 1.0.17; no audio gating at all.

**Beta 1.0 – 1.7.3** — modern drops; slime spawning in Beta 1.0 although disabled from Alpha 1.2.0 to Beta 1.3; player
damage one point low in 1.5–1.7.3; cookies not stackable and cake inert; golden apples uncraftable; pre-1.6 fire spread,
minecart boosters, missed-swing cooldown and below-source water unmodelled; ore quadrant bias absent; modern crafting
counts; achievements are modern advancements.

**Beta 1.8.1 – 1.9 Pre6** — the Pre2/Pre3 shift (G11); dragon from Pre4 (G10); modern End generator at Pre4 instead of a
single island with pillars; Pre1/Pre2's own content uncraftable; Pre1 armour model unimplemented; modern mob health at
1.8.1/Pre1; pre-Pre3 sword damage 5/7 instead of 6/8; enchant caps; 128 strongholds instead of 3; misdated `nether_wart`
(should be Pre1) and `end_stone` (should be Pre4).

**Release 1.0 – 1.6.4** — G3 (own-era blocks uncraftable), G8 (height), G6 (golden apple), G9 (geared mobs), modern potion
strengths and brewing, XP curve/sources, 1.4.6 content leaking into Release 1.4, full modern creative tabs, desert
villages in 1.0/1.1, 128-height, and the 1.6.1 saturation-costing regeneration not modelled.

## 3. Checked and correct — do not re-audit

* Pre-hunger food healing via the documented fallback (G2) — do **not** "fix" it.
* No hunger, sprinting, experience, critical hits, enchanting, brewing or structures before their boundaries.
* Structure allow-lists per version; villages absent before 1.8.1; villagers only from Pre1 and profession-less until 1.3.
* Beta 0..127 envelope, bedrock, sea level; Beta 1.0/1.2 sandstone and lapis exclusions survive in the newest `_era_ores`
  presets (append-only held).
* Enchantment availability per prerelease; placeholder tooltips at Pre3; no enchanting table before.
* Textures/lighting/fog/label/tooltip eras; Nether and weather gating; creative-menu era; recipe-book absence.
* The enchanted golden apple: supplied from Release 1.3, refused everywhere earlier, effects 1.3.1/13w24b-correct — the fix
  from the previous entry is confirmed correct across 1.3–1.6.4 by the Release audit.

## 4. Prioritised backlog

| # | Item | Why first | Where |
| --- | --- | --- | --- |
| 1 | **G1** era drop tables for 29 versions | largest player-visible error; shape exists for 1.7.3/1.8.1 | `HistoricalLootRules`, `LegacyCatalog` |
| 2 | **G3** block-date misattribution: genuine 1.2.1/1.3.1/1.4.2/1.5 blocks dated 1.6.4 | own-era content uncraftable in five profiles | `HistoricalBlockAvailability` + its generator |
| 3 | **G6/G4** removed/changed recipes: golden apple (2 windows + effects), arrows, wool, slab/ladder/plate counts, stone slab | one mechanism fixes a whole class | `RemovedRecipes` (generalise), `HistoricalCraftingEra` |
| 4 | **G8** 256-height dimension types for Release 1.2.5+ as new revisions | world is visibly half-height | presets/dimension types |
| 5 | **G11** Pre2/Pre3 boundary shift | one structural error behind many rows | `Feature` windows |
| 6 | **G10** dragon spawn from Pre6; eye-of-ender frame repair at Pre3 | End content is wrong by three prereleases | dragon/ender-eye mixins |
| 7 | **A-14** cloth/leather armour missing in Alpha 1.0.1–1.0.6 (other tiers craftable); **G5** spawn weights and pre-1.9 AI | early-survival and "feel" | early recipes, spawner research |
| 8 | **G7** date the 51 unknown blocks; reconcile creative/crafting tables | keeps the guards honest | generators + tables |

## 5. Outstanding verification

Each era document ends with what could not be verified and what would settle it. Recurring: mob damage/armour formulas,
spawn weights, the exact critical-hit formula at 1.6.x, cake hunger (research files disagree), the pre-1.3.1 XP curve, End
content beyond the island, stronghold counts, per-version village styles, and every audio/music question. These are
UNKNOWN, not "fine": nothing should be changed on a guess.
