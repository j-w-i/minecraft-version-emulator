# Nostalgic Tweaks port audit

Generated from `research/nt-port-worklist.json`, whose `carrierFiles` field names the upstream file that
implements each tweak. This table was missing for most of the port, which is why so much was reimplemented
from the game bytecode instead of copied: the mapping existed and was not used.

| My file | Tweaks it claims | Upstream carrier | Origin |
| --- | --- | --- | --- |
| `mixin/LivingEntityHistoricalSwordBlockingMixin.java` | SWORD_BLOCKING, SHIELD_DISABLE | (not in worklist)<br>(not in worklist) | **INVENTED** |
| `mixin/PlayerHistoricalCombatMixin.java` | DISABLE_COOLDOWN, DISABLE_SWEEP, DISABLE_CRITICAL_HIT | (not in worklist)<br>(not in worklist)<br>(not in worklist) | **PORTED (round 33)** |
| `mixin/ServerLevelHistoricalOrbMixin.java` | DISABLE_ORB_SPAWN | (not in worklist) | **PORTED (round 35)** |
| `mixin/GuiHistoricalHudMixin.java` | HIDE_HUNGER_BAR, HIDE_EXPERIENCE_BAR | helper/candy/hud/HudHelper.java<br>fabric/src/main/java/mod/adrenix/nostalgic/fabric/mixin/tweak/candy/old_hud/GuiMixin.java | **PORTED (round 34)** |
| `client/hud/HudHelper.java` | HIDE_HUNGER_BAR | helper/candy/hud/HudHelper.java | **PORTED (round 34)** |
| `mixin/PlayerHistoricalSprintMixin.java` | DISABLE_SPRINT | mixin/tweak/gameplay/mechanics_player/LivingEntityMixin.java | **INVENTED** |
| `mixin/FoodDataHistoricalHungerMixin.java` | DISABLE_HUNGER | mixin/tweak/gameplay/food_health/FoodDataMixin.java | **INVENTED** |
| `mixin/PlayerHistoricalFoodMixin.java` | CUSTOM_FOOD_HEALTH | mixin/tweak/gameplay/food_health/FoodPropertiesMixin.java | **INVENTED** |
| `mixin/LivingEntityHistoricalLootMixin.java` | OLD_MOB_DROPS, DISABLE_XP_DROPS | (not in worklist)<br>(not in worklist) | **INVENTED** |
| `mixin/FleeSunGoalHistoricalMixin.java` | DISABLE_MONSTER_AVOID_SUN | (not in worklist) | **INVENTED** |
| `mixin/PanicGoalHistoricalMixin.java` | DISABLE_ANIMAL_PANIC | (not in worklist) | **INVENTED** |
| `mixin/TemptGoalHistoricalMixin.java` | DISABLE_ANIMAL_TEMPTING | (not in worklist) | **INVENTED** |
| `mixin/AnimalHistoricalBreedingMixin.java` | DISABLE_ANIMAL_BREEDING | (not in worklist) | **INVENTED** |
| `mixin/AgeableMobHistoricalSpawnMixin.java` | DISABLE_BABY_ANIMALS | (not in worklist) | **INVENTED** |
| `mixin/ZombieHistoricalSpawnMixin.java` | DISABLE_BABY_ZOMBIES | (not in worklist) | **INVENTED** |
| `mixin/MobHistoricalItemPickupMixin.java` | DISABLE_MOB_ITEM_PICKUP | (not in worklist) | **INVENTED** |
| `mixin/MobHistoricalEquipmentMixin.java` | DISABLE_MOB_EQUIPMENT, DISABLE_MOB_ENCHANTMENTS | (not in worklist)<br>(not in worklist) | **INVENTED** |
| `mixin/RangedBowAttackGoalHistoricalMixin.java` | DISABLE_SKELETON_STRAFING | (not in worklist) | **INVENTED** |
| `mixin/ItemStackHistoricalDamageMixin.java` | OLD_DAMAGE_VALUES | (not in worklist) | **INVENTED** |
| `client/hud/*` | HIDE_HUNGER_BAR | helper/candy/hud/HudHelper.java | **PORTED (round 34)** |

## Remediation order

Highest first: the features the owner has actually observed in play, then the rest.

1. `DISABLE_SPRINT` - carrier `mixin/tweak/gameplay/mechanics_player/LivingEntityMixin.java`.
2. `CUSTOM_FOOD_HEALTH` - carrier `mixin/tweak/gameplay/food_health/FoodPropertiesMixin.java`.
3. `DISABLE_HUNGER` - carrier `mixin/tweak/gameplay/food_health/FoodDataMixin.java`.
4. Sword blocking - carriers `helper/gameplay/...` and `combat_player/MinecraftMixin.java`.
5. The mob AI and spawn gates, each from its own named carrier.
6. `OLD_DAMAGE_VALUES` - carrier `combat_item/DataComponentHolderMixin.java`.

Each one: read the carrier, copy its targets and injection points, swap the global config flag for this
project's per-version gate, keep the upstream path in the file header, then boot-verify.


## Research artifact health (round 41)

`nt-port-worklist.json` was generated from a **1.21.4** branch (`upstreamBranch`), while this project targets
**1.21.1**. The checkout is at the same revision the worklist names, so the discrepancy is in the carrier
paths, not the source tree.

Measured over all 200 entries and 252 listed carrier paths:

| | |
| --- | --- |
| carrier paths listed | 252 |
| individual paths that DO NOT EXIST | 15 |
| entries whose PRIMARY (first-listed) carrier is missing | 8 |
| entries with at least one carrier missing | 12 |

Those 8 are the entries that send you to a phantom file if `carrierFiles[0]` is trusted blindly. Two of them
(CUSTOM_FOOD_HEALTH, INSTANT_EAT) each cost a round before this was measured:

- ANIMAL_SPAWN_CAP - MobCategoryMixin.java
- CUSTOM_FOOD_HEALTH - FoodPropertiesMixin.java  (real code: food_health/PlayerMixin.java)
- INSTANT_EAT - ConsumableMixin.java  (real code: food_health/LivingEntityMixin.java)
- OLD_BLUE_VOID_OVERRIDE - SkyRendererMixin.java
- OLD_BOAT_DROPS - AbstractBoatMixin.java
- OLD_BOAT_MOVEMENT_PARTICLES - AbstractBoatMixin.java
- OLD_BOAT_WATER_LIFT - AbstractBoatMixin.java
- OLD_DARK_VOID_HEIGHT - SkyRendererMixin.java

The pattern is consistent: each missing name is a class that only exists from 1.21.2 onward, which a 1.21.4
branch has and a 1.21.1 target does not. The remaining 192 entries carry a real primary carrier.

**Rule for the rest of the port: check that the carrier path exists before reading it, and when it does not,
find where the tweak flag is actually referenced instead of trusting the listed path.**


## Mob drops: exact port specification (rounds 47-48, written before coding)

Carriers, all verified to exist, both full paths because two files share the name MobMixin:

- common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/mob_drops/LivingEntityMixin.java (28 lines)
- common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/mob_drops/MobMixin.java (32 lines)
- common/src/main/java/mod/adrenix/nostalgic/helper/gameplay/MobLootHelper.java (199 lines)

Mechanism: table SUBSTITUTION, not cancellation. NT modifies the getLootTable call inside dropFromLootTable and
returns a generated historical table, so vanilla still rolls it and looting, count ranges and on-fire smelting
all keep working. Verified in the 1.21.1 bytecode: ReloadableServerRegistries$Holder.getLootTable(ResourceKey)
is called inside dropFromLootTable.

CONSEQUENCE OF THE CURRENT INVENTED VERSION: it cancels dropFromLootTable and spawns items with
spawnAtLocation itself, so a looting sword changes nothing and a burning mob does not smelt its drop. The
visible outcome looked right (feather x1 in Beta 1.7.3) while two vanilla mechanics were being skipped.

The 14 mappings in upstream init():
  zombie, zombie villager, drowned, husk -> feather 0-2
  spider, cave spider                    -> string 0-2
  cow, mooshroom                         -> leather 0-2
  chicken                                -> feather 0-2
  pig                                    -> porkchop 0-2, smelts when on fire
  skeleton, stray, bogged                -> arrow 0-2 AND bone 0-2 (two pools)
  rabbit                                 -> rabbit hide 0-1
  zombified piglin                       -> cooked porkchop 0-2
  sheep                                  -> wool of its dye colour, or LootTable.EMPTY if sheared
Six of the fourteen (drowned, husk, stray, bogged, mooshroom, rabbit) do not exist in any profile this project
declares. Ported anyway for fidelity, and to be said so in the header.

Verified dependency: Sheep.ITEM_BY_DYE is private static final Map<DyeColor, ItemLike> in 1.21.1, so the sheep
case needs a Mixin accessor, exactly as upstream has one.

Adaptation to state in the file header: upstream has one flag PER ENTITY (OLD_ZOMBIE_DROPS, OLD_SPIDER_DROPS,
and so on - 14 user toggles). This project gates by version, so the per-entity flags collapse to the boundary
already covered by HistoricalLootRules.


## OLD_MOB_MOVEMENT_SPEED: examined and deliberately NOT ported (round 51)

I said last round this was next. Reading it changed the answer, so the correction is recorded here rather than
quietly dropped.

Carrier: common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/mob_ai/MobMixin.java (31 lines), which
injects at the head of Mob.serverAiStep and sets MOVEMENT_SPEED base value from a per-mob map.

Three independent reasons not to implement it:

1. UPSTREAM DISABLES IT BY DEFAULT. The listing ends with mobMap.setDisabled(true). Porting it as an always-on
   rule for our versions would apply a behaviour Nostalgic Tweaks itself does not apply unless a user asks. That
   is not a port; it is my preference wearing a port as a disguise.
2. THE RESEARCH CALLS THE BOUNDARY UNKNOWN. The worklist records boundary=unknown and no historical claim, so
   there is no evidence for which of our ten profiles should have it - and applying it to all ten would be a
   guess with a number attached.
3. THE VALUES ARE NOT CLEARLY OLDER. The map defaults to 0.25 and names cow, wolf, sheep, ghast, ocelot and
   enderman at 0.25 - which is already the modern base value for several of them, so "old" here may mean
   "different for a few mobs", not "the values before a known change".

Recorded as an explicit exclusion with its evidence, which is the same standard applied to alwaysOpenChest.


## CORRECTION to the OLD_MOB_MOVEMENT_SPEED entry (round 52)

I gave three reasons last round. The first one was wrong, and it is written here rather than deleted because the
same mistake would otherwise be made for every remaining tweak.

I claimed "upstream disables it by default, so porting it would apply a behaviour Nostalgic Tweaks does not
apply". That reasoning is bad, and AGENTS.md says why in the owner instructions: behaviour here is
"gated per version rather than by a global config flag". Nostalgic Tweaks defaults its flags off because it is a
configurable nostalgia mod for modern Minecraft; this project is a version emulator, where the VERSION decides.
A default-off upstream flag therefore says nothing about whether a behaviour belongs to a version we emulate.

MEASURED THIS ROUND: every flag in the mob-drop family defaults to false upstream -
OLD_ZOMBIE_DROPS, OLD_SPIDER_DROPS, OLD_SKELETON_DROPS, OLD_ZOMBIE_PIGMEN_DROPS, OLD_STYLE_STRAY_DROPS are all
TweakFlag.server(false, ...). If default-off disqualified a port, the whole mob-drop family I ported in round 50
would be disqualified too, including the zombie feather drop that is well documented for these versions.

So the movement-speed decision rests on the OTHER two reasons, which are the real ones:

1. the research records boundary=unknown with no historical claim, so there is no evidence for which of the ten
   profiles should have it; and
2. the values are not clearly older - the map defaults to 0.25 for cow, wolf, sheep, ghast, ocelot and enderman,
   which is already the modern base for several of them.

Both are about evidence for the BEHAVIOUR, which is the right test. The default flag is not.


## The worklist is a SUBSET, and that has been misleading me (round 59)

`research/nt-port-worklist.json` holds 200 entries. Its own `counts` field says 489 matching and 471 to port. So
roughly 60 percent of Nostalgic Tweaks is not in the file I have been treating as the to-port authority.

Evidence of the harm: `OLD_SWEEP_PARTICLES` appears NOWHERE in the worklist, yet it is a real tweak, default
**true**, and it was the missing piece behind the owner report "the sweep gui is there even tho the effect is
not". I searched the worklist for it, found nothing, and told the owner its absence meant it was out of scope -
the same mistake as INSTANT_EAT, from the same cause.

**Rule from here: whether a behaviour is a Nostalgic Tweaks feature is decided by its own config classes
(`GameplayTweak`, `CandyTweak`, `SoundTweak`), not by the worklist.** The worklist remains a useful index of
boundaries and historical claims; it is not the feature list.

Porting is still governed by evidence about the VERSION, not by upstream defaults. The crit particle is the
worked example: `OLD_NO_CRIT_PARTICLES` defaults to true upstream, but critical hits and their particle arrive
together in the Beta 1.8 Pre-release and six declared profiles had them, so that flag is a user preference - and
porting it as a version rule would delete part of the era rather than emulate it.

### Combat visuals, as of round 59

| Upstream flag | Default | Ported | Why |
| --- | --- | --- | --- |
| `OLD_SWEEP_PARTICLES` | true | yes | 1.9 particle, absent from every declared version |
| `OLD_NO_DAMAGE_PARTICLES` | true | yes | damage indicator is 1.9 |
| `OLD_NO_MAGIC_HIT_PARTICLES` | true | yes | enchanted-hit sparkle is 1.9 |
| `OLD_NO_CRIT_PARTICLES` | true | **no** | crit and its particle arrive in Beta 1.8; six profiles had them |
| `OLD_ATTACK` (sounds) | true | not yet | the 1.9 attack sound set mapped to the old one - audible, next |


## OLD_SKELETON_ARMS: ported in round 64, REVERTED in round 65

I ported it, then checked the evidence I had claimed for it, and took it back out. Recording both the mistake and
the reason, because the mistake was a familiar one.

What I claimed: that the Indev 20100202 removal of "arm swinging attack animations" covered skeletons as well as
zombies, so both halves of the mob_arms module rested on the same research row.

What the research actually says: a search of feature-timeline.json for the skeleton bow pose returns NOTHING. The
only skeleton animation row is strafing while shooting. The Indev quote is about zombies, and I carried it across
to skeletons because the two share an upstream directory.

Why the behaviour is probably BACKWARDS, not merely unsupported: upstream's skeleton branch replaces the bow
drawing pose with MobArmHelper.applyStaticArms plus an idle bob - the arms-out idle stance. Beta skeletons raised
their arms to draw a bow when shooting, which is the classic firing animation, so applying this removes an
animation these versions HAD. The zombie branch is the opposite case: it removes swinging that the versions had
already lost.

Reinstating it needs one thing only: a research row dating when the skeleton bow pose changed. Until that exists,
it is an unsupported visual change and it stays out.


## The flag-to-research join, and two of my own errors it caught (round 66)

Nostalgic Tweaks declares **504** flags. The research timeline names **95** tweak names, and every one of those 95
is a declared flag. That join is the map the remaining work needed: 95 behaviours have research support and can be
ported with a boundary; the other 409 have no row and stay out until one exists.

It also exposed two claims of mine that were wrong, both from treating the 200-row worklist as the research:

| I said | Reality |
| --- | --- |
| "Nostalgic Tweaks has no breeding toggle - a worklist search returns nothing", and relabelled the breeding gate as a project rule | `DISABLE_ANIMAL_BREEDING` is a declared flag AND the timeline names it under the feature "animal breeding". It is a port. |
| "`alwaysOpenChestEnabled` has no source, so it stays unwired" | `ALWAYS_OPEN_CHEST` is named in the timeline under "chests cannot be opened when a block is above". It has a source. |

Both are the same mistake in opposite directions: the first made me distrust a port, the second made me refuse a
behaviour that has evidence. Corrected in the code and here.

Consequence for the plan: the animation group I was about to work through has NO research rows (sneak, swing,
death-topple and arm pose all return nothing), which is why OLD_SKELETON_ARMS was reverted. The 95 supported flags
are where the remaining porting should happen, and `ALWAYS_OPEN_CHEST` is now a candidate rather than an exclusion.


## Calibrating the join: a name is a lead, not evidence (round 67)

Round 66 used the flag-to-research join to conclude that `ALWAYS_OPEN_CHEST` "has a source". That was too fast.
Reading the row itself:

- `confidence: low`
- the notes say the sourced quote runs the **opposite** direction: the quote is "Chests do not prevent other
  chests underneath from opening" (Beta 1.9 Prerelease 6), which is about a chest above a *chest*, not about a
  solid block above a chest.

So the behaviour is **named and unsupported**, and it stays excluded. The join answers "is there a row?" - it does
not answer "is the claim good?".

**Measured across the corpus:** 62 rows name a Nostalgic Tweaks tweak, distributed as **high 44, medium 6, low
12**. Any port drawn from a named row has to read that row's `confidence` and `notes` before it counts as
evidence. Three of the twelve low rows are worth knowing about because they touch work already done:

| Flag | Confidence | Note |
| --- | --- | --- |
| `ALWAYS_OPEN_CHEST` | low | source runs the other way; not ported |
| `DISABLE_ORB_SPAWN` | low | the orb port rests on stronger primary evidence (the b1.8.1 client registry has orb entities, b1.7.3's does not), not on this row |
| `OLD_STYLE_ZOMBIE_VILLAGER_DROPS` | low | one of the fourteen loot mappings; unreachable in these versions anyway |

The breeding correction from round 66 **survives** its own check: that row is `confidence: high` and its notes name
the development version explicitly ("Beta 1.9 Prerelease 2 ... Beta 1.8 and Beta 1.8.1 therefore have NO breeding").


## A server boot does NOT exercise player join (rounds 93-94)

The most expensive lesson of this port, recorded because it invalidated a whole category of evidence.

`FoodDataHistoricalRegenerationMixin` modified the second `Player.addExhaustion` call in `FoodData.tick` with
`ordinal = 1`. Only one such call exists in that method, so the injection never matched - and a failed `@ModifyArg`
is FATAL when the class is transformed. `FoodData` is transformed when a PLAYER IS PLACED IN THE WORLD, so every
join threw:

    Couldn't place player in world
    Mixin transformation of net.minecraft.world.food.FoodData failed
    InjectionError: Critical injection failure: Argument modifier method
        historicalExperience$noExhaustionFromRegeneration
    Client disconnected with reason: Invalid player data

The owner reported "invalid player data" twice and I spent three rounds on it, including moving their player data
aside - which was treating a symptom that did not exist. The files were never the problem.

WHY EVERY CHECK MISSED IT: this project's generation and gameplay probes run a dedicated server and never place a
player, so `FoodData` was never transformed. Every "boots clean" reported since that mixin went in was blind to
this entire class of failure - and it is the class that breaks the game for a player.

RULES THAT FOLLOW:

1. **A boot check validates only the classes the boot touches.** An injection on a class that is first transformed
   at player join, world load or dimension change is unverified by it. Say which classes a run exercised.
2. **`ordinal =` is a guess until the count is verified against the bytecode.** Two other failures this session
   came from the same habit, and the audit of all ten ordinal sites is now in this file.
3. **A client run that joins a world is the only check that exercises player join.** It was restored in round 94
   and a player joined with no injection failure, which is what verifies the six client-side ordinals - armour
   tint, HUD elements, sword-block pose, instant-eat animation - that no server run ever touched.
4. **Client harness worlds must be client-created.** Worlds copied from a server probe do not open in the client,
   which is what made the harness look broken for eight rounds.
5. **Stop the client gracefully.** The probe now asks the process to close and waits up to 30 seconds before
   forcing, because a force-kill during a save is what half-wrote player data twice.
