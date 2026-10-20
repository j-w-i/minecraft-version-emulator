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
| **hardcore gate** | Hardcore was selectable and effective in every profile, though it arrives in Beta 1.9 Pre2 | `NO_HARDCORE` before Pre2, plus a `PrimaryLevelData.isHardcore` override fed by a gate noted at server start; the stored flag is preserved, only the answer changes. **Runtime-verified**: a hardcore-flagged Beta 1.7.3 world reports `effective=false`, a hardcore Release 1.6.4 world `effective=true` | `HardcoreGateTest`, `/hex status` probe |
| **creeper discs** | A skeleton-killed creeper dropped no disc (and modern discs would have been wrong) | `HistoricalDiscDrops` picks from the version's eligible set (13/cat from v1.0.14; all but \"11\" from Release 1.1; plus \"11\"/\"wait\" from Release 1.5), and a kill hook adds it when the killer is a skeleton. **Runtime-verified**: skeleton-killed creeper dropped Gunpowder **and** Music Disc 13; a non-skeleton kill dropped Gunpowder only | `CreeperDiscDropTest`, RCON probe |
| **no-drop mobs** | A ghast dropped the modern tear and gunpowder in Alpha and Beta even though ghast tears are Beta 1.9 Pre1 content | `HistoricalLootRules.dropsNothing` answers "this mob dropped nothing" separately from "no rule"; ghast drops nothing before Pre1. **Runtime-verified**: killed ghast in a Beta 1.7.3 world left no item entity, while a zombie killed in the same world dropped a feather | `NoDropEntitiesTest`, RCON probe |

## Open, with the reason

### Evidence is missing (the audit said so; do not guess)

| Item | Why open |
| --- | --- |
| Player armour/damage formulas, mob attack values | Need client jars; the era audit marked them UNKNOWN |
| Spawn weights, pack sizes, light thresholds | Not in any extracted artifact; the spawner keeps modern weights |
| Pre-1.3.1 XP curve | The wiki only says 1.3.1 "adjusted" it; no table found |
| Critical-hit formula at exactly 1.6.x | The wiki's own history asks "1.6 or 1.7?" |
| Cake hunger values for the hunger era | Research files disagree (2 vs 3) |
| Pre3 frame: glistering melon / fire charge dates | The audit could not move them from Pre3 |

### A mechanism is missing, not the evidence

| Item | What would be needed |
| --- | --- |
| **Pre3 end-portal-frame item form** (flat filled-frame sprite, empty tooltip) | The texture system routes assets from dependency packs per version; it cannot serve a **mod-authored** per-version item model. Half-doing the tooltip alone would leave the visible half wrong, and the item is creative-only in Pre3, so it is recorded rather than half-implemented |
| Conditional/random drops in general | The disc rule is now a dedicated mechanism (`HistoricalDiscDrops` + one kill hook); a future conditional drop needs the same treatment rather than a data row |
| Potion strengths and brewing recipes | Needs a per-potion effect table per version; nothing in the corpus maps to modern potion components yet |
| Achievements | Modern advancements cannot be mapped onto the era's achievement list without a designed table |
| Boats, maps, signs, block-break delay | Each needs its own mechanic study; the audit recorded the deltas but not their rules |
| Slab/ladder/pressure-plate output counts; stone-slab substitution | The count changes could not be sourced unambiguously, and the slab item mapping needs the substituted-result path |
| Spawn weights and pre-1.9 AI | Research first; the 13 existing mob gates are the modelled subset |

### Verified only by unit test, not at runtime

| Item | What is missing |
| --- | --- |
| Client mixin BEHAVIOUR (not application) | `-PclientMixinCheck` has now proven the debug-overlay mixin APPLIES in a real client (`CLIENTMIXINCHECK loaded DebugScreenOverlay ... its mixins applied`), and the filtering is unit-tested; what is still unproven is the in-game appearance with F3 pressed in a historical world |
| Mixin-application claims generally | A dedicated-server boot is the check, and it has already caught two real defects the tests could not (an invalid injection on a void method, and a wrong descriptor) |

## What is checked and correct (do not re-audit)

Pre-hunger food healing via the documented fallback; the absence of hunger, sprinting, experience, critical hits,
enchanting, brewing and structures before their boundaries; structure allow-lists per version; villages absent before
1.8.1 and villagers only from Pre1; the 0..127 envelope and Beta-era height/bedrock/sea level; the Beta 1.0/1.2
sandstone and lapis exclusions surviving in the newest `_era_ores` presets; enchantment availability per prerelease;
the pre-redraw End frame's four textures and both models routed from the pinned pack; the enchanted golden apple across
1.3–1.6.4.

## Verification counts

`./gradlew test` — **802 tests, 0 failures**. The dedicated server boots with zero mixin errors. The golden snapshot,
version matrix and fact sheet are regenerated as part of each change.
