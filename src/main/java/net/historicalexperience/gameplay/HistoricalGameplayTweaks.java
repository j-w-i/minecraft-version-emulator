/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * Contains material copied or adapted from Nostalgic Tweaks
 * (https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks), Copyright (C) Adrenix and
 * Nostalgic Tweaks contributors, licensed LGPL-3.0-only, and MODIFIED for this project.
 * The upstream file or tweak this class came from is named in its documentation comment.
 *
 * This file is part of a work distributed under the GNU Lesser General Public License,
 * version 3. See LICENSE and THIRD_PARTY_NOTICES.md.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only
 */
package net.historicalexperience.gameplay;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Per-version gates for behaviours that came and went across Minecraft's history.
 *
 * <h2>What this is for</h2>
 *
 * <p>Each gate answers one question - "did this version's game behave this way?" - so a mixin can ask it and
 * change nothing else. Because the version is read from the world at runtime, an upgrade from Beta 1.7.3 to
 * Beta 1.8.1 changes every gate at once, with no separate migration.
 *
 * <h2>Where the behaviours come from, and where the boundaries come from</h2>
 *
 * <p>The behaviours are ported from <b>Nostalgic Tweaks</b> (LGPL-3.0; see THIRD_PARTY_NOTICES.md). Each
 * method names the upstream tweak it came from. Upstream is a global config flag; here it is a per-version
 * rule, which is the whole point of this project.
 *
 * <p>The boundaries come from <b>research/feature-timeline.json</b>, which records 143 rows with a source
 * URL and an exact quote for each - mostly the Minecraft Wiki's own History tables, cross-checked against
 * hash-verified client jars where one exists. That file is what turned several of these gates from
 * "UNVERIFIED, applies to everything" into sourced boundaries, and it corrected four assumptions this
 * code previously made.
 *
 * <h2>Expressing a boundary</h2>
 *
 * <p>Boundaries are written as {@code olderThan(version, "<declared version>")}, using the registry's own
 * chronology. A few real boundaries fall between two declared versions - Release 1.8.9 is not declared, for
 * instance - so the nearest declared version on the correct side of the boundary is used as the proxy and
 * the comment says so. That is a deliberate, visible approximation; a hand-written list of version ids
 * would be the invisible kind.
 */
public final class HistoricalGameplayTweaks {
    /**
     * Beta 1.8's chronology, written as a literal because Beta 1.8 is not a declared profile - only Beta 1.8.1 is.
     * A test ties it to the registry so the literal cannot drift.
     */
    private static final int BETA_1_8_CHRONOLOGY = 501_800;

    /**
     * The declared versions that predate the goal-based mob AI and the Adventure Update's systems.
     *
     * <p>DERIVED from the registry, not listed. It used to be a hand-written set of four ids - which this class's
     * own javadoc calls "the invisible kind" of approximation, and it was: a new pre-1.8 version would be left out
     * of it, and the seven gates that read this set (sword blocking, its fall-damage window, the critical-hit
     * formula, exhaustion costs, the food hunger values, the regeneration model and the golden apple) would quietly
     * apply Beta 1.8 behaviour to an earlier build without failing anything.
     */
    private static final Set<String> BEFORE_BETA_1_8 = HistoricalVersions.all().stream()
        .filter(version -> version.chronology() < BETA_1_8_CHRONOLOGY)
        .map(net.historicalexperience.version.HistoricalVersion::id)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());

    /** The pre-Beta-1.8 boundary, exposed so a test can tie the literal to the registry. */
    public static int preBeta18BoundaryChronology() {
        return BETA_1_8_CHRONOLOGY;
    }

    /**
     * Whether a version predates Beta 1.8, which is the question seven gates ask before anything else.
     *
     * <p>Exposed because the set behind it is derived and the derivation is the thing worth testing: a test that
     * went through the gates would be testing each gate's other boundaries at the same time and could not say
     * which one failed.
     */
    public static boolean preBeta18(String versionId) {
        return BEFORE_BETA_1_8.contains(versionId);
    }

    private static final Map<String, String> EVIDENCE = Map.ofEntries(
        Map.entry("skeletonStrafing",
            "RangedBowAttackGoal, which is what strafes while shooting, belongs to the goal-based mob AI "
                + "that arrives with Beta 1.8/1.9, so none of the pre-Beta-1.8 versions had it. Upstream "
                + "tweak: DISABLE_SKELETON_STRAFING."),
        Map.entry("criticalHit",
            "Critical hits were added in the Beta 1.8 Pre-release, so Infdev, Alpha and Beta 1.7.3 had "
                + "none and Beta 1.8.1 onwards did. Upstream tweak: DISABLE_CRITICAL_HIT."),
        Map.entry("monsterPickup",
            "Monsters picking dropped items up is 1.4.2 (12w32a/12w34b). Every declared version before "
                + "Release 1.6.4 predates it; Release 1.6.4 does not. Upstream tweak: "
                + "DISABLE_MONSTER_ITEM_PICKUP."),
        Map.entry("gearedMobs",
            "Mobs spawning with armour and weapons is 1.4.2 as well, having existed before Indev "
                + "20100202 and been removed. Upstream tweak: DISABLE_MONSTER_ITEM_SPAWN."),
        Map.entry("panic",
            "Animals fleeing when attacked is the Beta 1.8 Pre-release (feature-timeline mechanics row "
                + "\"animals fleeing when attacked (panic)\"), so Infdev, Alpha and Beta 1.7.3 had no panic "
                + "response and every later declared version did. Upstream tweak: DISABLE_ANIMAL_PANIC."),
        Map.entry("babyZombie",
            "Baby zombies spawning naturally is Release 1.6.2 (feature-timeline mechanics row \"baby "
                + "zombies spawning naturally\"), proxied by Release 1.6.4 as the only declared version "
                + "after it. Baby zombified piglins share the same date and the same hook. Upstream "
                + "tweaks: DISABLE_BABY_ZOMBIE_SPAWN, DISABLE_BABY_PIGLIN_SPAWN."),
        Map.entry("monsterEnchant",
            "Mobs spawning with enchanted gear is Release 1.4.2 (12w32a), the same boundary as the armour "
                + "and weapons themselves, proxied by Release 1.6.4. Upstream tweak: "
                + "DISABLE_MONSTER_ENCHANT_SPAWN."),
        Map.entry("mobArms",
            "Zombie arm-swinging attack animations were REMOVED in Indev 20100202, and every declared profile "
                + "is later (the oldest is Infdev 20100420), so the swinging arms are modern behaviour in all "
                + "ten. Upstream tweak: OLD_ZOMBIE_ARMS."),
        Map.entry("customDeathLoot",
            "A skeleton dropping its bow is Release 1.2.1 (12w06a, 8.5%): the timeline records arrows from "
                + "Indev 20100219 and bones from Beta 1.2, with the bow as a separate later behaviour. "
                + "Upstream tweak: OLD_SKELETON_DROPS."),
        Map.entry("avoidSun",
            "Monsters BURNING in daylight is Indev 20100213 - in range everywhere - but the goal that makes "
                + "them run for shade is Release 1.2.1 (12w04a). Upstream tweak: DISABLE_MONSTER_AVOID_SUN."),
        Map.entry("toolDurability",
            "Tool durability rose across the board in Beta 1.2: version-transitions.json's changed list reads "
                + "\"tool durability: wood 33 -> 59, stone 65 -> 131, iron 129 -> 250, diamond 1025 -> 1561\", and "
                + "research/minecraft-version-history.md records the same. So every declared version older than "
                + "Beta 1.2 breaks tools sooner. Gold is deliberately not covered: the same sentence says gold "
                + "tools gained faster mining \"while retaining old durability\", so there is no old value to apply "
                + "and inventing one is exactly what this project refuses to do."),
        Map.entry("endPortalFrameExplodes",
            "Beta 1.9 Prerelease 3 ONLY. minecraft.wiki's End Portal Frame history, Java Edition table, says "
                + "of Beta 1.9 Prerelease 3: 'It can drop as an item due to explosions', and of Beta 1.9 "
                + "Prerelease 4: 'The End portal can no longer be broken by explosions'. So for exactly one "
                + "declared build the frame is blast-destroyable and yields its own item, and from Pre4 onward it "
                + "is indestructible as it is today. The same history rows give two further Pre3-only facts not "
                + "yet implemented: the block emits light at level 15 where Pre4 and later emit 1, and the item "
                + "form is a flat texture of the filled frame with an empty tooltip. The owner named this bug "
                + "specifically and called it very important."),
        Map.entry("jumpExperienceBug",
            "Beta 1.9 Prerelease 1 ONLY, and our own corpus states it twice. minecraft-version-history.md item 6 "
                + "for that build: 'An erroneous jump event grants 1-100 XP; auto-repeat jump was removed', item "
                + "13: 'XP from jumping and disabled auto-repeat; Pre2 removes the XP bug and restores held-jump "
                + "auto-repeat', and item 11 of Prerelease 2: 'XP-on-jump debug bug removed; held-jump "
                + "auto-repeat restored'. historical-features.json records the same as 'Experience gained from "
                + "jumping as a test'. The range is 1-100, so a single jump can be worth a hundred experience and "
                + "the amount is random per jump. The disabled auto-repeat half is a client input behaviour and is "
                + "recorded rather than implemented."),
        Map.entry("snowballSurplus",
            "Alpha 1.0.6 ONLY. minecraft.wiki, Java Edition Alpha v1.0.6, Changes > Snow block: 'Now require only "
                + "4 snowballs to be crafted instead of 9. They still always drop 6 snowballs, allowing for an "
                + "infinite source of snowballs.' The recipe half is already the modern one - verified in the jar, "
                + "not assumed: data/minecraft/recipe/snow_block.json is a 2x2 of snowballs and "
                + "data/minecraft/loot_table/blocks/snow_block.json returns a count of four - so the surplus is "
                + "exactly this one build returning six, and only this build needs overriding. The fix boundary was "
                + "not established by the research, which is why the gate is equality on the one build that is "
                + "documented rather than a range: a range invented from a single data point is how one build's bug "
                + "gets handed to three innocent neighbours."),
        Map.entry("endPortalFrameLightFifteen",            "Beta 1.9 Prerelease 3 ONLY, the second half of the same history row that carries the explosion drop. "
                + "minecraft.wiki, End Portal Frame history, Java Edition: of Pre3, 'Gives off a light level of 15'; "
                + "of Pre4, 'Now emits light level of 1'. So for exactly one declared build the frame lit its "
                + "surroundings like a torch and the next build dimmed it. The modern value is deliberately not "
                + "written into the code - the hook overrides Pre3 and leaves every other build reading whatever "
                + "the registry declares, so a future change to the block cannot be frozen out by this feature."),
        Map.entry("silkySpawners",
            "Beta 1.9 Prerelease 4 ONLY, and this is the real in-range version of the owner's 'mineable spawners' "
                + "bug. minecraft.wiki's Monster Spawner history, Java Edition table, says of Beta 1.9 Prerelease 4: "
                + "'Monster spawners now drop when mined using a tool enchanted with the new Silk Touch "
                + "enchantment', and of Beta 1.9 Prerelease 5: 'Monster spawners no longer drop when broken using a "
                + "tool enchanted with Silk Touch.' So exactly one declared build dropped the spawner and the next "
                + "one took it away. The owner's premise named the Infdev builds, which is wrong for this matrix - "
                + "the block was ADDED in Infdev 20100625-1917, after both declared Infdev profiles, so there were "
                + "no spawners to mine. This is the bug that premise was reaching for."),
        Map.entry("punchShear",
            "Before shears existed, PUNCHING a sheep shorn it and dropped its wool; the Alpha v1.1.2_01 client's "
                + "sheep class overrides attackEntityFrom with `if (!this.sheared && attacker instanceof "
                + "EntityLiving) { this.sheared = true; drop 1 + random.nextInt(3) of Item.cloth }` and has NO "
                + "death-drop override at all, so killing it yielded nothing. Shears are Beta 1.7: the string "
                + "\"shears\" is absent from alpha-1.1.2_01-client.jar and present in beta173-client.jar, and the "
                + "owner confirmed the version. So this gate applies to every declared version older than Beta "
                + "1.7.3, and Beta 1.7.3 itself uses shears."),
        Map.entry("breeding",
            "Animal breeding AND tempting arrive together in Beta 1.9 Prerelease 2, so Beta 1.8.1 had "
                + "neither. Baby animals are Beta 1.9 Prerelease 3. Upstream tweaks: DISABLE_ANIMAL_BREEDING, "
                + "DISABLE_ANIMAL_TEMPTING, DISABLE_BABY_ANIMAL_SPAWNING. This is a PORT: an earlier note here "
                + "called it a project rule after a worklist search found nothing, but the worklist is a "
                + "200-row subset of the research and the timeline itself names the tweak under the feature "
                + "'animal breeding'."));

    private HistoricalGameplayTweaks() {
    }

    // -------------------------------------------------------------------------------------
    // Version arithmetic
    // -------------------------------------------------------------------------------------

    private static String versionOf(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return HistoricalProfileLookup.profileIdForLevel(serverLevel);
    }

    private static boolean isDeclaredVersion(String versionId) {
        return versionId != null && HistoricalVersions.byId(versionId).isPresent();
    }

    /**
     * Whether a version is older than another declared version, by the registry's own chronology.
     *
     * <p>Writing a boundary this way means it cannot silently go stale: adding a version between two others
     * moves it to the correct side of every boundary automatically.
     */
    private static boolean olderThan(String versionId, String otherVersionId) {
        var self = HistoricalVersions.byId(versionId).orElse(null);
        var other = HistoricalVersions.byId(otherVersionId).orElse(null);
        return self != null && other != null && self.chronology() < other.chronology();
    }

    // -------------------------------------------------------------------------------------
    // Gates with sourced boundaries
    // -------------------------------------------------------------------------------------

    /**
     * The boundary each gate is computed from, in <b>one</b> place, so a gate and its test cannot disagree.
     *
     * <p>Every gate below is this method plus one call. That shape exists because the gates take a
     * {@link Level}, which an offline unit test cannot build; without a version-keyed entry point the
     * per-version behaviour is untestable, and an untestable boundary is how this project shipped a panic
     * gate that applied to every version and a structure allow-list that suppressed its own village.
     *
     * <p>An unrecognised gate name <b>throws</b> rather than answering false. A gate that silently never
     * applies looks exactly like a gate that correctly does not apply, which is the failure this method
     * exists to make impossible.
     */
    public static boolean appliesTo(String gate, String versionId) {
        return switch (gate) {
            case "skeletonStrafing", "criticalHit", "panic" ->
                versionId != null && BEFORE_BETA_1_8.contains(versionId);
            case "avoidSun" -> isDeclaredVersion(versionId) && olderThan(versionId, RELEASE_1_2_5);
            // Same boundary as avoidSun and the same declared proxy: the skeleton bow drop is 1.2.1
            // (12w06a, 8.5%) per the timeline, so through Release 1.1 there is no bow to drop.
            case "customDeathLoot" ->
                isDeclaredVersion(versionId) && olderThan(versionId, RELEASE_1_2_5);
            case "breeding", "tempting", "babyAnimal" ->
                isDeclaredVersion(versionId) && olderThan(versionId, BETA_1_9_PRE_3);
            case "itemPickup", "monsterGear", "monsterEnchant", "babyZombie" ->
                isDeclaredVersion(versionId) && olderThan(versionId, RELEASE_1_6_4);
            // 1.9 combat, and every declared profile is older than 1.9 - the newest is Release 1.6.4 - so
            // these apply to all ten. Expressed as a boundary above every version rather than as a separate
            // rule, so the tested table stays the single source of truth.
            case "cooldown", "sweep", "swim" -> isDeclaredVersion(versionId);
            // Shears are Beta 1.7, so every declared version BEFORE Beta 1.7.3 shears a sheep by punching it.
            case "punchShear" ->
                isDeclaredVersion(versionId) && olderThan(versionId, BETA_1_7_3);
            // Tool durability rose across the board IN Beta 1.2, so every declared version older than that build
            // has the shorter values. The numbers are in HistoricalToolDurability with their source.
            case "toolDurability" ->
                isDeclaredVersion(versionId) && olderThan(versionId, BETA_1_2);
            // True for every declared version for its OWN reason, not because it shares an era with the
            // line above: zombie arm swinging was removed in Indev 20100202, earlier than every profile.
            case "mobArms" -> isDeclaredVersion(versionId);
            // Two single-build bugs, and the difference matters: every other gate here is a BOUNDARY, but these
            // are exact builds, present in one prerelease and removed by the next. Expressed as equality rather
            // than as olderThan, because "before Beta 1.9 Pre4" would wrongly include Pre1, Pre2 and Pre3.
            //
            // End portal frames could be destroyed by explosions and dropped as an item in Pre3 only; Pre4 fixed
            // it. minecraft.wiki's End Portal Frame history says of Pre3 "It can drop as an item due to
            // explosions" and of Pre4 "The End portal can no longer be broken by explosions".
            case "endPortalFrameExplodes" -> isDeclaredVersion(versionId) && versionId.equals(BETA_1_9_PRE_3);
            // Jumping granted experience in Pre1 only; Pre2 removed it. Our own research states it twice:
            // minecraft-version-history.md item 6 "An erroneous jump event grants 1-100 XP", item 13 "XP from
            // jumping and disabled auto-repeat; Pre2 removes the XP bug", and historical-features.json records
            // "Experience gained from jumping as a test".
            case "jumpExperienceBug" -> isDeclaredVersion(versionId) && versionId.equals(BETA_1_9_PRE_1);
            // Silk Touch made monster spawners drop themselves in Pre4 only; Pre5 removed the drop, and the modern
            // game does not drop them either. So the gate is inverted relative to most here - it says YES for the
            // one build that dropped them, and the hook suppresses the drop everywhere the gate says no.
            // minecraft.wiki, Monster Spawner history: Pre4 "Monster spawners now drop when mined using a tool
            // enchanted with the new Silk Touch enchantment", Pre5 "no longer drop when broken using ... Silk Touch".
            case "silkySpawners" -> isDeclaredVersion(versionId) && versionId.equals(BETA_1_9_PRE_4);
            // The frame emitted light level 15 in Pre3 and 1 from Pre4 on. Same one-build shape as the drop rule
            // above, and the same equality rather than a boundary: "before Pre4" would light up Pre1 and Pre2 too.
            // minecraft.wiki, End Portal Frame history: Pre3 "Gives off a light level of 15", Pre4 "Now emits light
            // level of 1".
            case "endPortalFrameLightFifteen" ->
                isDeclaredVersion(versionId) && versionId.equals(BETA_1_9_PRE_3);
            // Alpha 1.0.6 ONLY: the snow-block recipe dropped from nine snowballs to four while breaking a snow
            // block still gave back six, so every cycle produced two free snowballs. The recipe half is what the
            // modern game already has - data/minecraft/recipe/snow_block.json is a 2x2 of snowballs, and
            // loot_table/blocks/snow_block.json returns four - so the historical surplus is exactly one build
            // returning six, and only that build needs the override. Source: minecraft.wiki, Alpha v1.0.6,
            // Changes > Snow block: "Now require only 4 snowballs to be crafted instead of 9. They still always
            // drop 6 snowballs, allowing for an infinite source of snowballs."
            case "snowballSurplus" -> isDeclaredVersion(versionId) && versionId.equals(ALPHA_1_0_6);
            // Axe stripping and shovel pathing are MODERN features that every declared version predates, so this
            // gate is true for all 32 - expressed as "is a declared historical version" rather than as a boundary
            // above the newest, so the two rules read the same way as every other rule in this class.
            //
            // Boundaries, both from the wiki's own History tables rather than from NT, whose catalogue records them
            // as unknown and whose historicalClaim field says a name alone is not evidence:
            //   Axe:      "1.13 17w47a: Axes can now be used to strip logs into stripped logs."
            //   Shovel:   "1.9 15w31a: Shovels can now be used to create paths."
            case "noToolShaping" -> isDeclaredVersion(versionId);
            // The bow trio, all gated on the same boundary and for the same reason. The wiki's Bow history:
            // "1.8 Pre-release: Arrows can no longer be rapid-fired from bows; they must be charged first." So
            // before that build a bow fired the moment it was used (INSTANT_BOW), the charge mattered differently
            // (ARROW_SPEED), and a bow took no damage from shooting (INVINCIBLE_BOW) because there was no charge
            // cycle to break it. The owner asked for all three together "for the versions that it applies to (ie pre
            // beta 1.8)", which is exactly this boundary.
            case "instantBow", "arrowSpeed", "invincibleBow" ->
                isDeclaredVersion(versionId) && BEFORE_BETA_1_8.contains(versionId);
            // Air refills the instant the player's head breaks the surface, instead of the modern trickle. Grouped
            // with swimming by Nostalgic Tweaks (tweak group MECHANICS_SWIMMING) and gated on the same pre-Beta-1.8
            // boundary as the rest of the swim group.
            case "instantAir" -> isDeclaredVersion(versionId) && BEFORE_BETA_1_8.contains(versionId);
            // One bone meal fully grew a plant until Release 1.5. minecraft.wiki, Bone Meal history: "1.5 13w04a:
            // Bone meal has been significantly nerfed", and 13w05a then settled it at 2-5 uses on average. Bone meal
            // itself arrives in Beta 1.2, but a version that has no bone meal item needs no rule here - the item
            // availability gate already refuses it - so this is a single upper boundary. Release 1.5 is the first
            // DECLARED version at or after 13w04a, so it keeps the modern behaviour.
            case "instantBonemeal" -> isDeclaredVersion(versionId) && olderThan(versionId, RELEASE_1_5);
            // Hitting TNT primed it until Beta 1.7. minecraft.wiki, TNT history, Beta 1.7: "TNT can be primed only by
            // redstone ... as well as by hitting it with flint and steel. If the player hits TNT with an empty hand
            // or while holding any other item, it no longer ignites." The catalogue maps Beta 1.7 to the Beta 1.7.3
            // profile (see BETA_1_7_3), so every declared version BEFORE that profile still punches TNT alight.
            case "punchTnt" -> isDeclaredVersion(versionId) && olderThan(versionId, BETA_1_7_3);
            // A hoe on a grass block could throw wheat seeds until Beta 1.6 Test Build 3. minecraft.wiki, Wheat
            // Seeds history: "1.6 Test Build 3: Seeds can no longer be tilled from a grass block", and the Hoe page
            // gives the reason - tall grass arrived, and "this feature has been removed". Beta 1.6 therefore
            // already lacks it, so the rule covers declared versions older than the Beta 1.6 profile.
            case "grassSeeds" -> isDeclaredVersion(versionId) && olderThan(versionId, BETA_1_6);
            // Fishing yielded only raw fish from its Alpha 1.2.0 introduction until Release 1.7.2 (13w36a), which
            // "massively changed" rewards. Release 1.6.4 is the newest declared version and predates that, so every
            // declared version has the old single-item catch.
            case "oldFishingLoot" -> isDeclaredVersion(versionId);
            // Fishing gave no experience until 1.4.2 (12w37a). The declared Release 1.4 profile may be any 1.4.x, so
            // the boundary is placed AT it: older builds are certainly pre-12w37a, and Release 1.4 itself keeps the
            // XP. A boundary that is too early under-refuses and is safe; one that is too late deletes content.
            case "noFishingExperience" -> isDeclaredVersion(versionId) && olderThan(versionId, RELEASE_1_4);
            default -> throw new IllegalArgumentException("unknown gate '" + gate + "'; a gate that never "
                + "applies is indistinguishable from one that correctly does not, so this refuses to guess");
        };
    }

    /** Every gate name {@link #appliesTo} understands, so a test can walk all of them. */
    public static List<String> gateNames() {
        return List.of("avoidSun", "babyAnimal", "babyZombie", "breeding", "cooldown", "criticalHit",
            "customDeathLoot", "grassSeeds", "instantBonemeal", "itemPickup", "mobArms", "monsterEnchant",
            "monsterGear", "noFishingExperience", "oldFishingLoot", "panic", "punchTnt", "skeletonStrafing",
            "sweep", "swim", "tempting");
    }

    private static final String BETA_1_6 = "historical_experience:beta_1_6";
    private static final String RELEASE_1_4 = "historical_experience:release_1_4";
    private static final String RELEASE_1_5 = "historical_experience:release_1_5";
    private static final String RELEASE_1_1 = "historical_experience:release_1_1";
    private static final String RELEASE_1_2_5 = "historical_experience:release_1_2_5";
    private static final String RELEASE_1_6_4 = "historical_experience:release_1_6_4";
    private static final String BETA_1_9_PRE_3 = "historical_experience:beta_1_9_pre_3";
    private static final String BETA_1_9_PRE_1 = "historical_experience:beta_1_9_pre_1";
    private static final String BETA_1_9_PRE_4 = "historical_experience:beta_1_9_pre_4";
    private static final String ALPHA_1_0_6 = "historical_experience:alpha_1_0_6";

    /**
     * Beta 1.7.3, the earliest declared version that HAD shears, and therefore the boundary for the wool rule.
     *
     * <p>Shears arrive in <b>Beta 1.7</b>, confirmed two ways rather than assumed: the string "shears" is absent
     * from {@code alpha-1.1.2_01-client.jar} and present in {@code beta173-client.jar}, and the owner confirmed
     * the version independently. The declared catalogue maps Beta 1.7 to the Beta 1.7.3 profile, so this profile
     * is the first version that must NOT shear on a punch.
     */
    private static final String BETA_1_7_3 = "historical_experience:beta_1_7_3";

    /**
     * Beta 1.2, the build that raised tool durability across the board.
     *
     * <p>Declared as the profile id itself rather than as a chronology literal, so the boundary moves with the
     * registry: version-transitions.json records the change under Beta 1.2, and every declared version older than
     * that profile keeps the shorter values.
     */
    private static final String BETA_1_2 = "historical_experience:beta_1_2";


    /** Skeletons that shoot also strafe sideways: the goal-based mob AI, so the Beta 1.8 boundary. */
    public static boolean skeletonStrafingDisabled(Level level) {
        return appliesTo("skeletonStrafing", versionOf(level));
    }

    /** Critical hits, added in the Beta 1.8 Pre-release. */
    public static boolean criticalHitDisabled(Level level) {
        return appliesTo("criticalHit", versionOf(level));
    }

    /**
     * Monsters pick dropped items up: 1.4.2.
     *
     * <p>Release 1.4.2 is not a declared version, so Release 1.6.4 - the only declared version after it -
     * proxies the boundary: everything older than 1.6.4 is before 1.4.2, and 1.6.4 is after.
     */
    public static boolean monsterItemPickupDisabled(Level level) {
        return appliesTo("itemPickup", versionOf(level));
    }

    /** Mobs spawn already wearing armour and holding weapons: 1.4.2, same proxy as item pickup. */
    public static boolean monsterItemSpawnDisabled(Level level) {
        return appliesTo("monsterGear", versionOf(level));
    }

    /** Monsters flee the sun: Release 1.2.1, proxied by Release 1.2.5 as the next declared version. */
    public static boolean monsterAvoidSunDisabled(Level level) {
        return appliesTo("avoidSun", versionOf(level));
    }

    /** Animals eat from a player's hand to breed: Beta 1.9 Prerelease 2. */
    public static boolean animalBreedingDisabled(Level level) {
        return appliesTo("breeding", versionOf(level));
    }

    /** Animals follow a player holding food: arrives with breeding, Beta 1.9 Prerelease 2. */
    public static boolean animalTemptingDisabled(Level level) {
        return appliesTo("tempting", versionOf(level));
    }

    /** Baby animals: Beta 1.9 Prerelease 3, so Beta 1.9 Prerelease 3 itself HAS them. */
    public static boolean babyAnimalSpawningDisabled(Level level) {
        return appliesTo("babyAnimal", versionOf(level));
    }

    /**
     * Animals panic and flee when hurt: the <b>Beta 1.8 Pre-release</b>, per the feature timeline.
     *
     * <p>Now sourced. The timeline row is "animals fleeing when attacked (panic)", introduced in Beta 1.8,
     * so it belongs to the same boundary as the rest of the Adventure Update's mob behaviour and NOT to
     * every declared version - the previous version of this gate returned true everywhere, which would have
     * removed a behaviour Release 1.6.4 really had. Upstream tweak: DISABLE_ANIMAL_PANIC.
     */
    public static boolean animalPanicDisabled(Level level) {
        return appliesTo("panic", versionOf(level));
    }

    /**
     * Baby zombies spawn naturally: <b>Release 1.6.2</b> (Pre-release), per the feature timeline.
     *
     * <p>Release 1.6.2 is not a declared version, so Release 1.6.4 - the only declared version after it -
     * proxies the boundary: everything older than 1.6.4 has only full-sized zombies, and 1.6.4 itself has
     * babies. Zombified piglins are Zombie subclasses, so the same hook covers their babies, which the
     * timeline also dates to 1.6.2. Upstream tweak: DISABLE_BABY_ZOMBIE_SPAWN.
     */
    public static boolean babyZombieSpawningDisabled(Level level) {
        return appliesTo("babyZombie", versionOf(level));
    }

    /**
     * Monsters spawn already enchanted: <b>Release 1.4.2</b>, the same boundary as armour and weapons.
     *
     * <p>Separate from {@link #monsterItemSpawnDisabled} only so the upstream tweak
     * ({@code DISABLE_MONSTER_ENCHANT_SPAWN}) has a name of its own to map onto; the boundary is shared, so
     * the two cannot drift apart by accident.
     */
    public static boolean monsterEnchantmentSpawnDisabled(Level level) {
        return appliesTo("monsterEnchant", versionOf(level));
    }

    /**
     * A chest may be opened with something on top of it.
     *
     * <p>UNVERIFIED: the timeline's only related row is chest-on-chest placement in Beta 1.9 Pre6, which is
     * not the same question, so this stays on for every declared version rather than pretending to a
     * boundary. Upstream tweak: ALWAYS_OPEN_CHEST.
     */
    public static boolean alwaysOpenChestEnabled(Level level) {
        return isDeclaredWorld(level);
    }

    private static boolean isDeclaredWorld(Level level) {
        return isDeclaredVersion(versionOf(level));
    }

    // -------------------------------------------------------------------------------------
    // Gates that are one rule read twice
    // -------------------------------------------------------------------------------------

    /**
     * The 1.9 attack cooldown: attacks are always at full strength.
     *
     * <p>Ported from Nostalgic Tweaks {@code combat_player/PlayerMixin.java} (LGPL-3.0), which pins
     * {@code getAttackStrengthScale} to 1.0 and resets the attack strength ticker on every attack. Both
     * halves are implemented, because pinning only the scale leaves the cooldown visible and lets the
     * client refuse the next attack.
     */
    public static boolean attackCooldownDisabled(Level level) {
        return appliesTo("cooldown", versionOf(level));
    }

    /** The 1.9 sweep attack: hitting one target never damages the ones beside it. */
    public static boolean sweepAttackDisabled(Level level) {
        return appliesTo("sweep", versionOf(level));
    }

    /**
     * Sword blocking, from the Beta 1.8 Pre-release until 1.9.
     *
     * <p>Note the direction: blocking did NOT exist before Beta 1.8, so this is false for Infdev, Alpha and
     * Beta 1.7.3, and true from Beta 1.8.1 up. The first draft of this gate had it the other way round,
     * which the timeline caught before it was wired to anything.
     */
    public static boolean swordBlockingEnabled(Level level) {
        return swordBlockingEnabled(versionOf(level));
    }

    /**
     * The same boundary, asked about a version instead of a level.
     *
     * <p>Needed because the answer has to hold on the CLIENT too: a client's player level is a ClientLevel, so a
     * level-based question returns null there and the rule switches itself off on the side that draws the block.
     * {@code HistoricalActiveSession} is set on both sides, which is what client-visible rules must ask.
     */
    public static boolean swordBlockingEnabled(String versionId) {
        return isDeclaredVersion(versionId)
            && !BEFORE_BETA_1_8.contains(versionId);
    }

    /**
     * Whether the old block's FALL DAMAGE reduction applies, which is a narrower window than blocking itself.
     *
     * <p>The timeline records that blocking reduced fall damage and let the player rise faster underwater, and that
     * <b>both were removed in Beta 1.9 Prerelease</b> - earlier than blocking itself, which lasted until 1.9. So a
     * version can have the stance and not the reduction, and Beta 1.9 Prerelease 3 is exactly that version.
     *
     * <p>Of the ten declared profiles only Beta 1.8.1 is inside the window, and Beta 1.9 Prerelease 3 is the
     * nearest declared version past the removal, so it is the proxy - the same technique the registry uses
     * wherever a real boundary falls between two declared versions.
     *
     * <p>This was wrong in the first version of the port: the reduction used the blocking gate and so applied in
     * Beta 1.9 Prerelease 3, which is after the prerelease that removed it.
     */
    /**
     * Whether a version is older than another declared version, exposed so a rule can state its own boundary.
     *
     * <p>The private comparator has been here all along; rules that need a two-sided or sub-behaviour boundary
     * could not reach it, which is how the fall damage reduction ended up wearing the blocking gate's window.
     */
    public static boolean olderThanVersion(String versionId, String boundaryVersionId) {
        return isDeclaredVersion(versionId) && olderThan(versionId, boundaryVersionId);
    }

    /**
     * Whether a sword deals the PRE-Release-1.0.0 value, which is every version before Beta 1.9 Prerelease 3.
     *
     * <p>The offset is three, not one: research/historical-features.json records "Sword damage decreased (diamond
     * sword from 5 hearts to 3.5 hearts)" at Release 1.0.0, and ten damage becoming seven is a difference of three.
     * An earlier version of this applied one, taken from a later and unrelated wiki line about 1.9.
     *
     * <p>Named rather than inlined because the boundary is the point: a reader should be able to ask "which
     * profiles have the higher sword damage?" without reading a comparison inside a damage calculation.
     */
    /**
     * Whether critical hits use the Beta 1.8 formula rather than the flat +50%.
     *
     * <p>The formula changed in the "1.6/1.7 era" per research/historical-features.json, so every declared profile
     * older than Release 1.6.4 is on the earlier one - and critical hits do not exist before Beta 1.8 at all, which
     * the {@code criticalHit} gate already enforces by never letting a pre-1.8 player crit.
     *
     * <p>Release 1.6.4 is the proxy for the change: no profile is declared between the era's start and it.
     */
    /**
     * Whether regeneration uses the Beta 1.8 model: food 18 or more, once per 80 ticks, and no exhaustion cost.
     *
     * <p>Gated on the hunger boundary, since a version with no hunger meter has no regeneration to model. The
     * research does not date when the modern fast branch or the exhaustion charge arrived, and every declared
     * profile with hunger is Release 1.6.4 or earlier, so all six are treated as Beta-era. That assumption is
     * deliberate and would be replaced by a real boundary the moment one is sourced.
     */
    /**
     * Whether cookies and mushroom stew fill their pre-change hunger values.
     *
     * <p>Both changes - cookie 1 to 2 in Release 1.3.1, stew 8 to 6 in Release 1.4.2 - happened before Release
     * 1.6.4, the newest declared profile, so it serves as the proxy for both. Gated on the hunger boundary as well,
     * since a food bar is what these values fill.
     */
    /**
     * Whether ores drop the ore block rather than a raw metal.
     *
     * <p>Raw iron, gold and copper arrive in Release 1.17, which is later than every declared profile, so all ten
     * drop the block. The dating is already in this project's availability data, which marks the raw items and
     * their blocks as 1.17 additions.
     *
     * <p>Nostalgic Tweaks implements this as {@code SELF_BLOCK_DROPS}, a listing of six ore blocks, with the server
     * flag defaulting to off; here the version decides.
     */
    public static boolean oreDropsAsBlocks() {
        return isDeclaredVersion(HistoricalActiveSession.current());
    }

    /**
     * Whether the golden apple is the pre-Release-1.1 item: 10 hunger, no Regeneration.
     *
     * <p>Release 1.1 remade it as a gold-nugget recipe granting Regeneration and 4 hunger, so Beta 1.8.1 through
     * Release 1.0.0 are on the earlier item. Release 1.2.5 is the nearest declared profile past the change and
     * serves as the proxy.
     */
    /**
     * Whether exhaustion costs are the Beta 1.8 ones: attacking 0.3 rather than 0.1, and a non-sprint jump 0.2
     * rather than 0.05.
     *
     * <p>Gated on the hunger boundary, since exhaustion only exists with a hunger meter. The research does not date
     * when the modern attack and jump costs arrived, and every declared profile with hunger is Release 1.6.4 or
     * earlier, so all six are treated as Beta-era - the same deliberate assumption as the regeneration model.
     */
    public static boolean betaExhaustionCosts(String versionId) {
        return isDeclaredVersion(versionId) && !BEFORE_BETA_1_8.contains(versionId);
    }

    /**
     * Whether a sprint jump costs the Beta 1.8 value of 0.8 rather than 0.2.
     *
     * <p>The research dates this one exactly: 0.8 in Beta 1.8, lowered to 0.2 in Release 1.3.1, raised back to 0.8
     * in Release 1.7.2. Release 1.6.4 is the only declared profile inside the 0.2 window, and Release 1.6.4 is
     * also the proxy for both changes since no profile is declared between them.
     */
    public static boolean betaSprintJumpExhaustion(String versionId) {
        return betaExhaustionCosts(versionId) && olderThan(versionId, RELEASE_1_6_4);
    }

    /**
     * Beta 1.9 Prerelease 4's chronology, derived from the registry's own numbering scheme rather than looked up.
     *
     * <p>The scheme is {@code eraOffset + major*1000 + minor*100 + patch} with Beta at 500_000, so Beta 1.9
     * Prerelease 4 is 500_000 + 1000 + 900 + 4. It is written as a number rather than as a registry lookup because
     * the boundary is a build the registry does not declare YET: a gate that referred to it by id would silently
     * evaluate false until the version was registered, and would then change behaviour without anyone editing the
     * gate. A test ties this number to the registry by asserting it is exactly one more than the declared Beta 1.9
     * Prerelease 3.
     */
    private static final int BETA_1_9_PRE_4_CHRONOLOGY = 501_904;

    /** Beta 1.9 Prerelease 3's chronology, on the same scheme. */
    private static final int BETA_1_9_PRE_3_CHRONOLOGY = 501_903;

    /**
     * Whether enchanting actually works in this version: enchantments applied, levels spent.
     *
     * <p>Three distinct historical states, from {@code research/minecraft-version-history.md} Part IV:
     *
     * <ul>
     *   <li><b>Before Beta 1.9 Prerelease 3</b> - there is no enchanting table at all. Beta 1.8 has a collectible,
     *       displayed and entirely unspendable experience bar.</li>
     *   <li><b>Prerelease 3</b> - the table exists, with its interface and glint, but applies no enchantments and
     *       spends nothing. The research is explicit: "no actual enchantments or functional XP spending".</li>
     *   <li><b>Prerelease 4 onward</b> - 17 enchantments, numeric levels, and levels spent on enchanting. This is
     *       the first build where enchanting is real.</li>
     * </ul>
     *
     * <p>So an experience bar being present is NOT evidence that enchanting works, and the two are gated apart.
     */
    public static boolean functionalEnchanting(String versionId) {
        var version = HistoricalVersions.byId(versionId).orElse(null);
        return version != null && version.chronology() >= BETA_1_9_PRE_4_CHRONOLOGY;
    }

    /**
     * Whether the enchanting table refuses to enchant at all, which is every declared build before Pre3.
     *
     * <h2>Why this is not just {@code !functionalEnchanting}</h2>
     *
     * <p>It was, and that was a defect the owner caught from the historical record. "Pre3 applies no real
     * enchantments" is true, and it does <b>not</b> mean Pre3 refuses to enchant: the table enchanted, applied the
     * glint and the blue name, and spent levels, and what did not exist yet was the enchantment DATA. So the two
     * states are three states, and collapsing the middle one into "does not work" removed the working half of
     * Prerelease 3 to imitate the half that was genuinely missing.
     *
     * <ul>
     *   <li>before Pre3 - no table to open, and this refuses a click if one is forced;</li>
     *   <li>Pre3 - enchanting works and levels are spent, with the placeholder tooltip from
     *       {@link #placeholderEnchantmentTooltip};</li>
     *   <li>Pre4 onward - real enchantments, real names.</li>
     * </ul>
     */
    public static boolean enchantingDisabled(String versionId) {
        return !functionalEnchanting(versionId) && !placeholderEnchantmentTooltip(versionId);
    }

    /**
     * Whether an enchanted item shows the literal {@code "Enchanted!!"} instead of its enchantments - Pre3 only.
     *
     * <p>minecraft.wiki, Beta 1.9 Prerelease 3: <i>"Enchanted items display \"Enchanted!!\" on their tooltip, and
     * their name becomes blue."</i> Pre4: <i>"Enchanted items' tooltips now display their enchantments instead of
     * \"Enchanted!!\"."</i> The same boundary as {@code functionalEnchanting}, approached from the other side: the
     * build where enchanting works and the data does not exist yet.
     */
    public static boolean placeholderEnchantmentTooltip(String versionId) {
        var version = HistoricalVersions.byId(versionId).orElse(null);
        return version != null
            && version.chronology() >= BETA_1_9_PRE_3_CHRONOLOGY
            && version.chronology() < BETA_1_9_PRE_4_CHRONOLOGY;
    }

    /**
     * Alpha 1.1.0's chronology: the build sneaking was added in, corrected by the project owner.
     *
     * <p>Written as a literal because Alpha 1.1.0 is not a declared profile yet, and tied to the registry's scheme
     * by a test so it cannot drift. The research is SILENT on this boundary - its only sneak reference is Beta
     * 1.4's "sneaking while climbing holds position", which implies sneaking already existed by then but does not
     * date its arrival - so the owner's correction is the best evidence available and is recorded as such.
     */
    private static final int ALPHA_1_1_0_CHRONOLOGY = 401_100;

    /** The sneak boundary, exposed so a test can tie the literal to the registry's numbering. */
    public static int sneakingBoundaryChronology() {
        return ALPHA_1_1_0_CHRONOLOGY;
    }

    /**
     * Release 1.9's chronology: the build that added the offhand slot.
     *
     * <p>A literal because 1.9 is outside the declared matrix - the newest target is Release 1.6 - and tied to the
     * registry's scheme by a test. Every version this project supports therefore has NO offhand, which is why the
     * predicate below is about the boundary rather than about a version list.
     */
    private static final int RELEASE_1_9_CHRONOLOGY = 601_900;

    /** The offhand boundary, exposed so a test can tie the literal to the registry's numbering. */
    public static int offhandBoundaryChronology() {
        return RELEASE_1_9_CHRONOLOGY;
    }

    /**
     * Alpha 1.2.0's chronology: the Halloween Update, which added the Nether.
     *
     * <p>A literal because Alpha 1.2.0 is a target that is not declared yet, tied to the registry's scheme by a test
     * so it cannot drift into gating the wrong build.
     */
    private static final int ALPHA_1_2_0_CHRONOLOGY = 401_200;

    /** The Nether boundary, exposed so a test can tie the literal to the registry's numbering. */
    public static int netherBoundaryChronology() {
        return ALPHA_1_2_0_CHRONOLOGY;
    }

    /**
     * Alpha v1.2.2a's chronology: the F4 portal-spawn debug key. Narrower than the Nether boundary, so it is stated
     * separately rather than reusing it.
     */
    private static final int ALPHA_1_2_2A_CHRONOLOGY = 401_202;

    /** The F4 boundary, exposed so a test can tie the literal to the registry's numbering. */
    public static int f4PortalBoundaryChronology() {
        return ALPHA_1_2_2A_CHRONOLOGY;
    }

    /**
     * Whether pressing F4 rebuilds a lit Nether portal, which is <b>Alpha v1.2.2a only</b>.
     *
     * <p>An equality rather than a "from here on" comparison, because the key is a debug shortcut that existed in one
     * development build. Both clients were downloaded and SHA-1 checked against Mojang's manifest and decompiled:
     * Alpha 1.2.0 handles F11 (87) and F3 (63) and has no F4 case at all, while Alpha 1.2.2a has
     * {@code if (Keyboard.getEventKey() == 62)} calling {@code new nj().c(world, player)}. Treating it as a floor
     * would hand the key to every later Alpha build and invent behaviour those clients do not contain.
     *
     * <p>It is also restricted to the exact declared version rather than to the chronology, because the chronology is
     * a sortable key and not an identity: a later build placed on the same number must not inherit the key.
     */
    public static boolean f4SpawnsPortal(String versionId) {
        var version = HistoricalVersions.byId(versionId).orElse(null);
        return version != null
            && version.chronology() == ALPHA_1_2_2A_CHRONOLOGY
            && "historical_experience:alpha_1_2_2a".equals(version.id());
    }

    /**
     * Whether the Nether exists in this version. It arrives with the Halloween Update, <b>Alpha 1.2.0</b>.
     *
     * <p>The owner asked for this directly: "I assume you've already blocked nether portals until alpha 1.2". The
     * block veto did not cover it and could not - that veto runs on the GENERATION path, so it stops netherrack,
     * glowstone and portal blocks from being generated, and does nothing about a player placing obsidian and
     * lighting it. In an Alpha 1.0.1 world that produced a working portal into a dimension the version never had.
     *
     * <p>Named for the dimension rather than the portal, because the portal is one symptom: the Nether's blocks,
     * its mobs and its travel all arrive together.
     */
    public static boolean netherAvailable(String versionId) {
        var version = HistoricalVersions.byId(versionId).orElse(null);
        return version != null && version.chronology() >= ALPHA_1_2_0_CHRONOLOGY;
    }

    /**
     * Whether food stacks in this version. It does not before <b>Beta 1.8</b>, which is when food became stackable.
     *
     * <p>Reported by the owner: "food also didnt stack until beta 1.8". Every pre-hunger build therefore has food at
     * stack size one, which is the same boundary as the hunger meter, the regeneration model and the rest of the
     * Adventure Update - so it reuses that boundary rather than inventing a second one.
     *
     * <p>Named for the capability, unlike the offhand predicate, because here the truth is split across the
     * declared set: four of the thirteen declared versions DO stack food.
     */
    public static boolean foodStacks(String versionId) {
        var version = HistoricalVersions.byId(versionId).orElse(null);
        return version != null && version.chronology() >= BETA_1_8_CHRONOLOGY;
    }

    /**
     * Whether this version predates the offhand slot, which arrived in <b>Release 1.9</b>.
     *
     * <p>Reported by the owner: "the offhand is available in all versions currently and should not be available at
     * all, not in the gui or via a hotkey". Correct - dual wielding is a 1.9 feature and the newest version here is
     * Release 1.6.4, so no supported version has an offhand.
     *
     * <p>Named for the absence rather than the presence, because "true" is the answer for every version in the
     * matrix and a reader should not have to work out that `!available` is the common case.
     */
    public static boolean offhandUnavailable(String versionId) {
        var version = HistoricalVersions.byId(versionId).orElse(null);
        return version != null && version.chronology() < RELEASE_1_9_CHRONOLOGY;
    }

    /**
     * Whether the player can sneak in this version. Sneaking arrives in <b>Alpha 1.1.0</b>.
     *
     * <p>Reported by the owner after playing an Alpha 1.0.1 world: "i can also still sneak way before sneaking was
     * added". There was no gate for it anywhere in the project - a search for sneak, crouch and the shift-key
     * methods returned nothing outside vanilla - so this is a newly implemented behaviour rather than a correction.
     *
     * <p>Refusing the crouch outright is the faithful answer: before Alpha 1.1.0 the shift key did nothing, so the
     * player neither crouched, nor moved at sneak speed, nor used the sneak pose.
     */
    public static boolean sneakingAvailable(String versionId) {
        var version = HistoricalVersions.byId(versionId).orElse(null);
        return version != null && version.chronology() >= ALPHA_1_1_0_CHRONOLOGY;
    }

    /**
     * Whether brewing works in this version.
     *
     * <p>From {@code research/version-history-transitions}: Beta 1.9 Prerelease 2 carries a cauldron-based potion
     * prototype whose code has <b>no ordinary block ID</b> - internal, unobtainable, with many negative and no-op
     * combinations. Prerelease 3 is the first FUNCTIONAL brewing: stand-based, thirty seconds, a reduced set of 25
     * potions across 31 combinations, and the cauldron becomes a plain water container.
     *
     * <p>So a Beta 1.8 world must not brew at all, and "brewing exists" is not the same as "brewing works" before
     * Prerelease 3.
     */
    public static boolean functionalBrewing(String versionId) {
        var version = HistoricalVersions.byId(versionId).orElse(null);
        return version != null && version.chronology() >= BETA_1_9_PRE_3_CHRONOLOGY;
    }

    /**
     * Whether the End portal does anything when it is completed.
     *
     * <p>Beta 1.9 Prerelease 3 adds the stronghold portal room, the eye of ender and the frame and portal assets,
     * but the portal block has no function: the frame repairs and nothing follows. Prerelease 4 activates the
     * portal and generates the End. The dragon is a later boundary again - it does not spawn until Prerelease 6 -
     * so this gate covers the portal only, and says nothing about the boss.
     */
    public static boolean endPortalFunctional(String versionId) {
        var version = HistoricalVersions.byId(versionId).orElse(null);
        return version != null && version.chronology() >= BETA_1_9_PRE_4_CHRONOLOGY;
    }

    /**
     * The chronology at which enchanting becomes functional, exposed so a test can tie it to the registry.
     *
     * <p>The constant it returns is a literal because the build it names is not declared yet. A literal can drift
     * away from the registry's numbering without anything failing, so this accessor exists purely to let a test
     * assert that the boundary sits exactly one build after the last declared prerelease.
     */
    public static int functionalEnchantingBoundaryChronology() {
        return BETA_1_9_PRE_4_CHRONOLOGY;
    }

    /**
     * Whether the enchanting table exists as a block in this version.
     *
     * <p>From Beta 1.9 Prerelease 3, where the research records its interface arriving. Before that the block is
     * not part of the game, so a Beta 1.8 save must not be able to craft one.
     */
    public static boolean enchantingTableExists(String versionId) {
        var version = HistoricalVersions.byId(versionId).orElse(null);
        return version != null && version.chronology() >= BETA_1_9_PRE_3_CHRONOLOGY;
    }

    /**
     * Whether the golden apple is the pre-Release-1.1 item: 10 hunger, always edible, and Regeneration for 30
     * seconds at amplifier 0.
     *
     * <p>The boundary is Release 1.1 itself, not a later proxy. The change is dated to Release 1.1, and Release 1.1
     * is a DECLARED profile here, so treating a later profile as the boundary would have left Release 1.1 carrying
     * the old item. The evidence for the earlier form is the Beta 1.8 client artifact's item registry line
     * {@code (66,10,1.2F,false).o().a(Regeneration,30,0,1.0F).a("appleGold")}.
     */
    public static boolean betaGoldenApple(String versionId) {
        return isDeclaredVersion(versionId)
            && !BEFORE_BETA_1_8.contains(versionId)
            && olderThan(versionId, RELEASE_1_1);
    }

    public static boolean betaFoodHungerValues(String versionId) {
        return isDeclaredVersion(versionId)
            && !BEFORE_BETA_1_8.contains(versionId)
            && olderThan(versionId, RELEASE_1_6_4);
    }

    public static boolean betaRegenerationModel(String versionId) {
        return isDeclaredVersion(versionId) && !BEFORE_BETA_1_8.contains(versionId);
    }

    public static boolean betaCriticalHitFormula(String versionId) {
        return isDeclaredVersion(versionId)
            && !BEFORE_BETA_1_8.contains(versionId)
            && olderThan(versionId, RELEASE_1_6_4);
    }

    public static boolean swordDamageBeforeReleaseOnePointZero(String versionId) {
        return isDeclaredVersion(versionId) && olderThan(versionId, BETA_1_9_PRE_3);
    }

    /**
     * The sword-damage boundary itself, exposed so a test can prove no declared build sits on the wrong side of it.
     *
     * <p>Written as a literal because Beta 1.9 Prerelease 3 is declared, so the boundary could equally have been a
     * lookup; the literal is used to keep this method free of registry access, which is what lets the test run
     * offline. A test ties it back to the registry.
     */
    public static int swordDamageBeforeReleaseBoundaryChronology() {
        return BETA_1_9_PRE_3_CHRONOLOGY;
    }

    public static boolean blockFallDamageReductionEnabled(String versionId) {
        return swordBlockingEnabled(versionId) && olderThan(versionId, BETA_1_9_PRE_3);
    }

    /**
     * Experience orbs must not exist, in a version that awarded no experience.
     *
     * <p>Reads the version's own {@code awardsExperience} rule rather than a boundary of its own, so the two
     * cannot disagree.
     */
    public static boolean experienceOrbSpawnDisabled(Level level) {
        String versionId = versionOf(level);
        if (versionId == null) {
            return false;
        }
        return HistoricalVersions.byId(versionId)
            .map(version -> !version.rules().awardsExperience())
            .orElse(false);
    }

    /** Why a gate applies where it does, for tests and reporting. */
    public static String evidenceFor(String gate) {
        return EVIDENCE.get(gate);
    }

    /** Every gate name that carries a recorded reason. */
    /**
     * Whether End portal frames can be blown up, for the session's current version.
     *
     * <p>The gate takes no arguments because {@code getExplosionResistance} has none, and the session holder is the
     * same authority the other no-argument answers use. An unknown version makes {@code appliesTo} false, which
     * leaves the frame indestructible: the safe direction, since the other answer would let players blow up a
     * stronghold portal in every world.
     */
    public static boolean endPortalFrameExplodes() {
        return appliesTo("endPortalFrameExplodes", HistoricalActiveSession.current());
    }

    /**
     * Whether Silk Touch mining a monster spawner yields the spawner block itself.
     *
     * <p>True for exactly Beta 1.9 Prerelease 4. This is the one gate in the class whose {@code false} is the
     * <b>modern</b> answer rather than an older one: the modern game does not drop spawners, Pre5 removed the drop,
     * and so the hook that consumes this suppresses a drop that vanilla would otherwise produce. Reading it as
     * "an old behaviour to add" would invert it.
     */
    public static boolean silkySpawnerDrops() {
        return appliesTo("silkySpawners", HistoricalActiveSession.current());
    }

    /**
     * Whether the End portal frame emits light level 15, which it did in Pre3 only.
     *
     * <p>No argument for the same reason as {@link #endPortalFrameExplodes()}: the value is read from a block state
     * with no level in reach. An unknown session answers false, which leaves the frame at the registry's own value -
     * the safe direction, because the other answer would brighten a block in every ordinary world.
     */
    public static boolean endPortalFrameLightFifteen() {
        return appliesTo("endPortalFrameLightFifteen", HistoricalActiveSession.current());
    }

    /**
     * Whether breaking a snow block returns more snowballs than the recipe cost - Alpha 1.0.6's free snowballs.
     *
     * <p>True for exactly one declared build. The value itself (six) is documented in the wiki quote rather than
     * derived, and it is deliberately not written here: the hook that consumes this asks how many to return, so the
     * number lives with the block rule and the gate stays a yes/no question like every other gate in this class.
     */
    public static boolean snowballSurplus() {
        return appliesTo("snowballSurplus", HistoricalActiveSession.current());
    }

    /**
     * Whether an axe must not strip logs and a shovel must not make paths - true for every declared version.
     *
     * <p>Axe stripping is 1.13 (17w47a) and shovel pathing is 1.9 (15w31a); the newest version this project declares
     * is Release 1.6.4, so both features are anachronistic in all 32 builds and the rule applies throughout. Named
     * "noToolShaping" rather than after either feature because one gate covers both and they share a boundary that
     * is simply "later than everything declared".
     *
     * <p>This is the NOSTALGIC TWEAKS behaviour rather than a new invention: upstream answers
     * {@code GameplayTweak.DISABLE_AXE_STRIPPING} and {@code DISABLE_SHOVEL_PATHING}, and the owner asked for both
     * by name. What changed is only where the yes/no comes from - upstream a config flag, here the world's version.
     */
    public static boolean noToolShaping() {
        return appliesTo("noToolShaping", HistoricalActiveSession.current());
    }

    /**
     * Whether the player's air refills the instant their head breaks the surface.
     *
     * <p>Nostalgic Tweaks groups this with swimming ({@code MECHANICS_SWIMMING}, {@code INSTANT_AIR}) and this
     * project gates it on the same pre-Beta-1.8 boundary as the rest of that group. The affected behaviour is
     * {@code LivingEntity.increaseAirSupply}, which modern versions step up gradually; the era's value is the full
     * supply at once.
     */
    public static boolean instantAir() {
        return appliesTo("instantAir", HistoricalActiveSession.current());
    }

    /** Whether one bone meal fully grows a plant (before the Release 1.5 nerf). */
    public static boolean instantBonemeal() {
        return appliesTo("instantBonemeal", HistoricalActiveSession.current());
    }

    /** Whether hitting TNT primes it (before Beta 1.7). */
    public static boolean punchTnt() {
        return appliesTo("punchTnt", HistoricalActiveSession.current());
    }

    /** Whether a hoe on grass can throw wheat seeds (before Beta 1.6 Test Build 3). */
    public static boolean grassSeeds() {
        return appliesTo("grassSeeds", HistoricalActiveSession.current());
    }

    /** Whether fishing yields only raw fish (every declared version; changed in 1.7.2). */
    public static boolean oldFishingLoot() {
        return appliesTo("oldFishingLoot", HistoricalActiveSession.current());
    }

    /** Whether a successful catch grants no experience (before 1.4.2). */
    public static boolean noFishingExperience() {
        return appliesTo("noFishingExperience", HistoricalActiveSession.current());
    }

    /** Whether a bow fires the moment it is used rather than after being charged - pre-Beta-1.8 only. */
    public static boolean instantBow() {
        return appliesTo("instantBow", HistoricalActiveSession.current());
    }

    /**
     * The share of a full charge the era's bow needed, as a percentage, or 0 when the charge does not apply.
     *
     * <p>Nostalgic Tweaks' {@code ARROW_SPEED} defaults to 70, and upstream turns it into a charge window of
     * {@code 72000 - (value / 100 * 20)} ticks. The owner named the same figure. The value lives here rather than in
     * the mixin so the regression the gate protects - "no charge at all" versus "a shorter charge" - is expressed in
     * one place.
     */
    public static int eraArrowSpeedPercent() {
        return 70;
    }

    /** Whether a bow takes no durability from being fired - pre-Beta-1.8 only. */
    public static boolean invincibleBow() {
        return appliesTo("invincibleBow", HistoricalActiveSession.current());
    }

    /**
     * How much experience a jump grants under the Pre1 bug, or 0 when the bug does not apply.
     *
     * <p>1-100 per the research, drawn per jump, so this returns the amount for one jump rather than a fixed value.
     *
     * <p>The zero is the safe answer twice over: it is what an unknown version and every other build get, and it is
     * the value the hook refuses to act on, so a session that is not Pre1 cannot be handed orb-free experience by a
     * caller that forgot to check the gate first.
     */
    public static int jumpExperienceBug(net.minecraft.util.RandomSource random) {
        return jumpExperienceBug(HistoricalActiveSession.current(), random);
    }

    /**
     * The same answer for an explicitly named version.
     *
     * <p>This overload exists so the amount can be asserted against a version id rather than only against the
     * session, because the session is unset in a unit test - which meant the one-argument form could only ever be
     * tested for its zero. The range is the part worth pinning: 1-100 inclusive, drawn fresh per jump.
     */
    public static int jumpExperienceBug(String versionId, net.minecraft.util.RandomSource random) {
        return appliesTo("jumpExperienceBug", versionId)
            ? 1 + random.nextInt(100)
            : 0;
    }

    public static List<String> documentedGates() {
        return EVIDENCE.keySet().stream().sorted().toList();
    }
}