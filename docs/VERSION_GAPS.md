# Version gaps — status of the per-version fidelity audit

This was the findings document of the 2026-10-21 audit. It is now a **status board**: what the audit found, what has
been implemented since, and what remains open with the reason it is open.

The full evidence for every finding stays in the four era documents:
[Indev/Infdev/Alpha](../research/gaps/indev-infdev-alpha.md), [Beta 1.0–1.7](../research/gaps/beta-1.0-1.7.md),
[Beta 1.8–1.9pre](../research/gaps/beta-1.8-1.9pre.md), [Release 1.0–1.6.4](../research/gaps/release.md). The generated
**actual** behaviour baseline is [version-facts.md](../research/version-facts.md).

## Implemented

| Finding | What was wrong | What was done | Pinned by |
| --- | --- | --- | --- |
| **G1** mob drops | Only 5 of 34 versions declared drop tables; the rest dropped modern items (rotten flesh in Alpha, etc.) | Pre-Beta-1.8 table wired into all 10 Alpha and Beta 1.0–1.6 profiles; Beta 1.7.3's jar table extended; Beta 1.8.1 completed with second-item pools (feather, leather, spider eye, bone, rotten flesh) and version-aware burn smelting | `PreBeta18DropTableTest`, `EraDropItemTest` |
| **G1** reported symptom | Beta 1.8.1 chicken/cow dropped no meat | The helper built one global table per entity type from 1.7.3's items; it now builds from the **version's own rule** | `EraDropItemTest` |
| **A-02** sheep | Died dropping wool in every pre-1.8 build | Death-wool window corrected to Beta 1.7 → Release 1.8 | `PreBeta18DropTableTest` |
| **G3** block dates | Genuine 1.2.1/1.3.1/1.4.2/1.5 content dated to 1.6.4, so Release 1.2.5–1.5 could not craft their own era's blocks | The generator's single 1.6.4 bucket split into the releases that added each block; Indev chronology restored; pre-Classic blocks dated to Indev | `ReleaseBlockDatesTest`, `Pre2Pre3BoundaryTest` |
| **G4/G6** golden apple | Uncraftable before Beta 1.8; modern ingot recipe afterwards; modern effects | Supplied gold-block `[Infdev, 1.1)` and gold-nugget `[1.1, 1.9)` recipes and refused the modern one; effects corrected (Regen I 5 s in 1.0–1.5; Regen II 10 s + Absorption 2:00 in 1.6.4) | `GoldenAppleRecipeTest`, `GoldenAppleEffectsTest` |
| **A-12** arrows | Modern flint recipe in every Alpha build | Iron-tip recipe `[v1.0.1, v1.0.14)` with the modern one refused | `ArrowAndWoolRecipeTest` |
| **A-13** wool | Four-string recipe throughout Alpha and Beta | Nine-string recipe `[v1.0.1, Beta 1.7)` | `ArrowAndWoolRecipeTest` |
| **A-14** cloth armour | v1.0.1/1.0.4/1.0.6 had **no craftable armour** (leather needs cows, which do not exist yet) | Four wool armour recipes `[v1.0.1, v1.0.8)`, leather shapes refused | `ClothArmourTest` |
| **G8** Release height | Every Release world was 128 tall; 1.2.1 raised the limit to 256 | New 0..256 dimension type plus an appended `_height_256` generation revision for 1.2.5/1.3/1.4/1.5/1.6.4, wired through settings mirrors and world presets; 1.0/1.1 stay 128. **all five revisions are ACCEPTED after runtime probes** (`scripts/verify-release-height.ps1`, run per build: y=0/y=1/y=255 buildable and y=256/y=-1 out of world, with `/hex status` naming the height revision in all three generator slots) | `ReleaseHeightTest`, `scripts/verify-release-height.ps1` |
| **G9** geared mobs | Gates proxied by 1.6.4, so 1.4/1.5 had no geared monsters | Windows end at Release 1.4 (the 1.4.2 baseline); baby zombies keep their correct 1.6.2 proxy | `ReleaseGearedMobsAndToolsTest` |
| **G10** End | Dragon existed from Pre4; Pre3 could not place eyes at all | No dragon before Pre6; Pre3 fills frames without opening the portal | `EndBoundariesTest` |
| **G11** Pre2/Pre3 shift | Breeding/tempting gated at Pre3; Pre1/Pre2 content dated Pre3; nether wart/end stone dated Release 1.0 | Breeding and tempting to Pre2; Pre1's blocks and Pre2's potion ingredients to their builds; nether wart Pre1, end stone Pre4 | `Pre2Pre3BoundaryTest` |
| **A-06** debug screen | Coordinates shown in every version | Hidden before Alpha v1.2.3 (client side) | `HistoricalDebugScreenTest` |
| **A-10** compass/clock | In `always()`, craftable from Alpha v1.0.1 | Dated Alpha v1.1.0 / Alpha v1.2.0, matching the creative catalogue | `ReleaseGearedMobsAndToolsTest` |
| cookie stacking | Unstackable in Beta 1.4–1.7.3 | Stack of 8 through one shared stack-limit used by all six hooks | `CookieStackLimitTest` |
| cake | Healed nothing before hunger | 3 health per slice in Beta 1.2–1.7.3 through two redirects inside vanilla's own `CakeBlock.eat` | `CakeSliceHealTest` |
| Beta damage bug | Damage one point low in Beta 1.5–1.7.3 | +1 to player melee attacks unless moving upwards, through one shared hook | `BetaDamageBugTest` |
| redstone lamp | Refused in Release 1.2.5 although it is 1.2.1 content | Dated to Release 1.2.5 (wiki 12w07a) | `ReleaseGearedMobsAndToolsTest` |
| **pressure plates take three blocks before Beta 1.3** | Modern two-block plate shape everywhere | `PRESSURE_PLATE_THREE_BLOCKS` + two supplied three-in-a-row shapes (planks, stone) with the modern shapes refused. minecraft.wiki Pressure Plate history Beta 1.3: \"now require only two wood planks/stone ... previous versions required three\". **The output count is an assumption**: the row gives ingredients only, so the shapes yield one and that is recorded in the code | `PressurePlateTest` |
| **player-broken boat drops the boat item from Release 1.3** | A boat broken by a player dropped planks and sticks in every version | `BOAT_DROP_ITEM_ON_PLAYER_BREAK` (from Release 1.3) + a `VehicleEntity.destroy(DamageSource)` hook: the one place the removal cause is still known. Only the player path changes - fall damage keeps vanilla's three planks and two sticks, per minecraft.wiki Boat 12w27a: \"Boats now drop their item instead of 3 oak planks and 2 sticks if broken by a player. Instead, it needs to fall certain heights...\". Client-verified | `BoatDropTest`, `CLIENTMIXINCHECK` |
| **a cake has six slices, not seven** | Modern seven-slice cake everywhere; the era had six (12 hunger in the hunger era, 18 health before it) | `CAKE_SIX_SLICES` (all declared) + a constant override in `CakeBlock.eat`'s bite comparison, leaving the blockstate range intact so saved cakes stay valid. minecraft.wiki Cake history: Beta 1.8 "Cake now restores 12 hunger and its slice restores 2"; 14w27a "increased from 6 to 7". Also CLOSES the cake-hunger conflict (wiki says 2 per slice, which modern already is, so no hunger override was needed) | `CakeSlicesTest`, `CLIENTMIXINCHECK` |
| **stone slab is the era block** (A-11) | The era'\''s stone slab (today'\''s smooth_stone_slab) was refused as Beta 1.3, while the 1.14 stone_slab was craftable with the modern recipe | Block table dates smooth_stone_slab to the earliest declared build (wiki Slab: "0.26 SURVIVAL TEST Added stone (now smooth stone) slabs"); allow-list carries the era slab, and the 1.14 block is refused everywhere; the documented 20100130 recipe (3 cobblestone -> 3 slabs) is supplied before Release 1.2.1, modern six-output after. Ingredient drift before 1.2.1 recorded as unknown | `StoneSlabTest` |
| **potion strengths before Release 1.6.1** | Instant Health healed 4 and Regeneration ticked at 50 everywhere | `PRE_1_6_1_POTION_STRENGTHS` + `HistoricalPotionStrengths` (pure) with two constant overrides: Instant Health `4 -> 6` and Regeneration `50 -> 25`. minecraft.wiki 1.6.1: "Instant Health nerfed by 33% ... Regeneration nerfed by 50%". Only the health constant is touched, never Instant Damage's 6. Enabled by a new **server-wide profile context** (`HistoricalServerProfile`) because mob effects are stateless singletons with no level; both mixins client-verified | `PotionStrengthTest`, `CLIENTMIXINCHECK` |
| **brewing takes 30 s before Release 1.1** | Every brew took the modern 20 s | `BREW_TIME_THIRTY_SECONDS` `[Pre3, Release 1.1)` and a constant override on `BrewingStandBlockEntity.serverTick` (400 -> 600 ticks). research/version-transitions Pre3: brewing "listed at 30 seconds"; minecraft.wiki 1.1: "Reduced brewing time from 30 seconds to 20 seconds". Mixin application client-verified | `BrewTimeTest`, `CLIENTMIXINCHECK` |
| **block-break delay before Release 1.3.1** | No inter-block delay anywhere | `BLOCK_BREAK_DELAY` + `HistoricalBlockBreakDelay` (pure) and one game-mode hook: a survival break within five ticks of the previous one is refused, creative exempt, modern from Release 1.3. minecraft.wiki 1.3.1: "The 1/4 second (5 game tick) delay between block breaking has been removed for players in Survival". Applied to all earlier declared builds because only the removal is documented - recorded as an inference. Mixin application verified by a client class-load probe | `BlockBreakDelayTest`, `CLIENTMIXINCHECK` |
| **slab and ladder counts before Release 1.2.1** | Modern six-slab/three-ladder recipes everywhere | `LEGACY_SLAB_LADDER_COUNTS` with supplied three-slab shapes for oak/cobblestone/sandstone and the two-ladder shape, modern recipes refused before Release 1.2.5 (the 1.2.1 baseline); minecraft.wiki 1.2.1: "slabs now yields 6 instead of 3", "3 ladders instead of 2" | `SlabLadderCountsTest` |
| **signs and buckets before Release 1.3.1** | Signs crafted three per recipe and buckets stacked to sixteen everywhere | One-sign recipe for Release 1.0.0-1.2.5 (modern three-sign recipe refused) and empty buckets limited to a stack of one through the shared stack-limit hook; minecraft.wiki 1.3.1: "Crafting now gives 3 signs instead of 1" / "Empty buckets: Stack up to 16 now" | `SignAndBucketEraTest` |
| **no achievements before Beta 1.5** | Modern advancements fired in every build, though achievements and their UI arrive in Beta 1.5 (Beta 1.4 has trigger infrastructure only) | `NO_ACHIEVEMENTS` + a `PlayerAdvancements.award` HEAD cancel (nothing is recorded, not just hidden), so a Beta 1.3/1.4 world shows no advancement toast or screen. Mixin application verified by a client class-load probe | `AchievementGateTest`, `CLIENTMIXINCHECK` |
| **hardcore gate** | Hardcore was selectable and effective in every profile, though it arrives in Beta 1.9 Pre2 | `NO_HARDCORE` before Pre2, plus a `PrimaryLevelData.isHardcore` override fed by a gate noted at server start; the stored flag is preserved, only the answer changes. **Runtime-verified**: a hardcore-flagged Beta 1.7.3 world reports `effective=false`, a hardcore Release 1.6.4 world `effective=true` | `HardcoreGateTest`, `/hex status` probe |
| **creeper discs** | A skeleton-killed creeper dropped no disc (and modern discs would have been wrong) | `HistoricalDiscDrops` picks from the version's eligible set (13/cat from v1.0.14; all but \"11\" from Release 1.1; plus \"11\"/\"wait\" from Release 1.5), and a kill hook adds it when the killer is a skeleton. **Runtime-verified**: skeleton-killed creeper dropped Gunpowder **and** Music Disc 13; a non-skeleton kill dropped Gunpowder only | `CreeperDiscDropTest`, RCON probe |
| **no-drop mobs** | A ghast dropped the modern tear and gunpowder in Alpha and Beta even though ghast tears are Beta 1.9 Pre1 content | `HistoricalLootRules.dropsNothing` answers "this mob dropped nothing" separately from "no rule"; ghast drops nothing before Pre1. **Runtime-verified**: killed ghast in a Beta 1.7.3 world left no item entity, while a zombie killed in the same world dropped a feather | `NoDropEntitiesTest`, RCON probe |

## Open, with the reason

### Evidence is missing (the audit said so; do not guess)

| Item | Why open |
| --- | --- |
| Player armour/damage formulas, mob attack values | Need client jars; the era audit marked them UNKNOWN |
| Spawn weights, pack sizes, light thresholds, pre-1.9 AI | Not in any extracted artifact; the spawner keeps modern weights, and the 13 existing mob gates are the modelled AI subset |
| Pre-1.3.1 XP curve | The wiki only says 1.3.1 "adjusted" it; no table found |
| Critical-hit formula at exactly 1.6.x | The wiki's own history asks "1.6 or 1.7?" |
| Pre3 frame: glistering melon / fire charge dates | The audit could not move them from Pre3 |

### A mechanism is missing, not the evidence

| Item | What would be needed |
| --- | --- |
| **Pre3 end-portal-frame item form** (flat filled-frame sprite, empty tooltip) | The texture system routes assets from dependency packs per version; it cannot serve a **mod-authored** per-version item model. Half-doing the tooltip alone would leave the visible half wrong, and the item is creative-only in Pre3, so it is recorded rather than half-implemented |
| Conditional/random drops in general | The disc rule is now a dedicated mechanism (`HistoricalDiscDrops` + one kill hook); a future conditional drop needs the same treatment rather than a data row |
| Potion recipes | Needs a per-potion brewing table per version; nothing in the corpus maps to modern potion components yet |
| Achievements ABOVE Beta 1.5 | The era's own sixteen (and later ten) achievements need a designed mapping to modern advancement ids; the corpus says the full list "needs a per-page audit". The modern advancement set is still shown there |
| Boat PHYSICS and controls; pre-1.4 maps | Boats still need their physics study (v1.0.6_01 break-on-impact-at-speed, lower inertia from 13w16b, control scheme), which is a simulation change rather than a rule; maps need a dynamic item id (the pre-1.4 map is one item, not the empty/filled pair), which the recipe mechanism cannot express yet |

### Verified only by unit test, not at runtime

| Item | What is missing |
| --- | --- |
| Client mixin BEHAVIOUR (not application) | `-PclientMixinCheck` has now proven the debug-overlay mixin APPLIES in a real client (`CLIENTMIXINCHECK loaded DebugScreenOverlay ... its mixins applied`), and the filtering is unit-tested; what is still unproven is the in-game appearance with F3 pressed in a historical world |
| End-to-end BEHAVIOUR of hooks that only run on a specific live action (breaking a block with a real player, opening a screen, placing a potion in a stand) | Application is verified by the boot and client probes, and the logic by unit tests; what is not driven end-to-end is the in-game action itself, which needs a scripted player rather than a class-load probe |

## What is checked and correct (do not re-audit)

Pre-hunger food healing via the documented fallback; the absence of hunger, sprinting, experience, critical hits,
enchanting, brewing and structures before their boundaries; structure allow-lists per version; villages absent before
1.8.1 and villagers only from Pre1; the 0..127 envelope and Beta-era height/bedrock/sea level; the Beta 1.0/1.2
sandstone and lapis exclusions surviving in the newest `_era_ores` presets; enchantment availability per prerelease;
the pre-redraw End frame's four textures and both models routed from the pinned pack; the enchanted golden apple across
1.3–1.6.4.

## How the work is verified

Every implemented row above is pinned by a test, and the claims were checked by four independent routes, each matched to
what it can actually see:

| Route | Proves | Evidence in this tree |
| --- | --- | --- |
| Unit tests | the decision logic: windows, dates, filters, loot sets, constants | 156 suites, **823 tests, 0 failures** |
| Dedicated server boot | server-side mixin APPLICATION and world behaviour | boots to `Done (~2.7s)` with zero mixin errors; two real defects were caught this way that the tests could not see (an invalid injection descriptor on a void method, and a null `overworld()` at `SERVER_STARTING`) |
| RCON probes on real worlds | player-visible outcomes: build limits, drops, hardcore, brewing | `scripts/verify-release-height.ps1` (five builds, 0..255 with y=256/-1 out of world); ghast dropped nothing with a zombie/feather control; a skeleton-killed creeper dropped Gunpowder + Disc 13 while a player kill dropped Gunpowder only; hardcore read `effective=false` in Beta 1.7.3 and `true` in Release 1.6.4 |
| Client class-load probes | client-side mixin application (`CLIENTMIXINCHECK`) | debug overlay, achievements, block-break delay, brewing stand, regeneration and instant-health effect classes all report "its mixins applied" |

The golden snapshot, version matrix and fact sheet are regenerated as part of every change. What each route does NOT
prove is listed in "Verified only by unit test, not at runtime" above - most importantly that no client has yet been
driven to press F3 in a historical world, so the debug overlay's *appearance* is unverified even though its hook and
logic are.


## Where to continue

The work is tracked by this document plus `PROGRESS.md` (newest first). Each implemented row names the test that pins it,
and the four verification routes above are the ones to use. Two lessons cost time and are worth keeping in mind:

1. **A probe that only exercises one code path can be confidently wrong.** The crafting-gate finding was initially
   reported backwards because the probe passed `blockId = null`; the game always passes the result block. Check how the
   real call site calls a rule before measuring it.
2. **A mixin is only verified when its class loads.** Unit tests cannot see an invalid injection descriptor; a
   dedicated-server boot catches server-side hooks, and `-PclientMixinCheck=<class>` catches client ones. Three defects
   in this project's recent history were found that way and nowhere else.

The scripts that do the runtime work are `scripts/verify-release-height.ps1` (build limits, per build) and
`scripts/rcon.py` (drive a running server: `/hex status`, summon/kill/damage probes). `scripts/generate-1211-block-availability.py`
regenerates the block-date table - edit its `TIMELINE` entries rather than the generated Java, and re-run
`scripts/apply-licence-headers.py` afterwards.
