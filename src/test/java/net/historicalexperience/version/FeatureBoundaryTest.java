/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. The boundary of every {@link Feature}, written against the research rather than against the table.
 *
 * <p>This is the research table that the old string-gate test held ("this gate applies before Beta 1.8", "this one
 * only to Prerelease 3"), kept as an independent statement of what each window should be. It is written as
 * <em>expected boundary builds</em> so that a wrong window fails here with the build named, and so that a new feature
 * cannot be added without saying where it applies.
 */
class FeatureBoundaryTest {
    private static final String P = "historical_experience:";

    /** Features whose window is {@code [.., boundary)}: active in every declared version older than the build. */
    private static final Map<Feature, String> BEFORE = new EnumMap<>(Feature.class);
    /** Features active from a build with no upper end. */
    private static final Map<Feature, String> FROM = new EnumMap<>(Feature.class);
    /** Features that apply to exactly one declared build. */
    private static final Map<Feature, String> EXACTLY = new EnumMap<>(Feature.class);
    /** Features active in {@code [from, until)}. */
    private static final Map<Feature, String[]> BETWEEN = new EnumMap<>(Feature.class);
    /** Features that apply to every declared version (the real boundary lies beyond the newest). */
    private static final EnumSet<Feature> ALL_DECLARED = EnumSet.noneOf(Feature.class);
    /** Features with several windows, each checked by its own test. */
    private static final EnumSet<Feature> MULTI = EnumSet.noneOf(Feature.class);

    static {
        // The goal-based mob AI and the Adventure Update's mob behaviour: before Beta 1.8, which is not declared;
        // Beta 1.7.3 is the newest build before it and Beta 1.8.1 the first after.
        for (Feature f : List.of(Feature.NO_SKELETON_STRAFING, Feature.NO_CRITICAL_HITS, Feature.NO_ANIMAL_PANIC,
            Feature.INSTANT_BOW, Feature.ARROW_SPEED, Feature.INVINCIBLE_BOW, Feature.INSTANT_AIR,
            Feature.UNSTACKABLE_FOOD)) {
            BEFORE.put(f, P + "beta_1_8_1");
        }
        // Release 1.2.1 falls between Release 1.1 and Release 1.2.5; the project uses 1.2.5 as the proxy.
        BEFORE.put(Feature.NO_MONSTER_SUN_AVOIDANCE, P + "release_1_2_5");
        BEFORE.put(Feature.OLD_SKELETON_DROPS, P + "release_1_2_5");
        // Breeding, tempting and baby animals share Prerelease 3 in the code even though the evidence dates the
        // first two to Prerelease 2. Preserved, and recorded in the feature's evidence.
        BEFORE.put(Feature.NO_ANIMAL_BREEDING, P + "beta_1_9_pre_3");
        BEFORE.put(Feature.NO_ANIMAL_TEMPTING, P + "beta_1_9_pre_3");
        BEFORE.put(Feature.NO_BABY_ANIMALS, P + "beta_1_9_pre_3");
        // Release 1.4.2 and 1.6.2 are not declared; Release 1.6.4 is the proxy for all four.
        for (Feature f : List.of(Feature.NO_MONSTER_ITEM_PICKUP, Feature.NO_MONSTER_GEAR, Feature.NO_MONSTER_ENCHANTS,
            Feature.NO_BABY_ZOMBIES)) {
            BEFORE.put(f, P + "release_1_6_4");
        }
        BEFORE.put(Feature.PUNCH_SHEARS, P + "beta_1_7_3");
        BEFORE.put(Feature.PUNCH_TNT, P + "beta_1_7_3");
        BEFORE.put(Feature.OLD_TOOL_DURABILITY, P + "beta_1_2");
        BEFORE.put(Feature.GRASS_SEEDS, P + "beta_1_6");
        BEFORE.put(Feature.INSTANT_BONE_MEAL, P + "release_1_5");
        BEFORE.put(Feature.NO_FISHING_EXPERIENCE, P + "release_1_4");
        BEFORE.put(Feature.OLD_SWORD_DAMAGE, P + "beta_1_9_pre_3");
        BEFORE.put(Feature.NO_ENCHANTING, P + "beta_1_9_pre_3");
        BEFORE.put(Feature.NO_BREWING, P + "beta_1_9_pre_3");
        BEFORE.put(Feature.HIDDEN_XP_LEVEL, P + "beta_1_9_pre_3");
        BEFORE.put(Feature.NO_REAL_ENCHANTING, P + "beta_1_9_pre_4");
        BEFORE.put(Feature.INERT_END_PORTAL, P + "beta_1_9_pre_4");
        BEFORE.put(Feature.NO_SNEAKING, P + "alpha_1_1_0");
        BEFORE.put(Feature.NO_NETHER, P + "alpha_1_2_0");
        // No declared version has an offhand: the boundary is Release 1.9, above the newest declared build.
        BEFORE.put(Feature.OLD_LIGHTING_ENGINE, P + "beta_1_8_1");
        BEFORE.put(Feature.OLD_FOG, P + "beta_1_8_1");
        BEFORE.put(Feature.LOW_CLOUDS, P + "beta_1_8_1");
        BEFORE.put(Feature.OLD_CHEST_MODEL, P + "beta_1_8_1");
        BEFORE.put(Feature.FLAT_DROPPED_ITEMS, P + "release_1_5");
        BEFORE.put(Feature.ALPHA_STARS, P + "beta_1_5");
        BEFORE.put(Feature.NO_SMOOTH_LIGHTING, P + "beta_1_3");
        BEFORE.put(Feature.NO_WEATHER, P + "beta_1_5");
        BEFORE.put(Feature.LEFT_CLICK_USE, P + "release_1_4");
        BEFORE.put(Feature.NO_ITEM_TOOLTIPS, P + "beta_1_0");
        BEFORE.put(Feature.NO_COLOUR_MAPS, P + "alpha_1_2_0");
        BEFORE.put(Feature.BOSS_HEALTH_CAPTION, P + "release_1_4");
        // Release 1.6.1 is the attribute system, not declared; Release 1.6.4 is the first declared build at or past it.
        BEFORE.put(Feature.NO_ATTRIBUTE_TOOLTIPS, P + "release_1_6_4");

        FROM.put(Feature.OLD_SWORD_BLOCKING, P + "beta_1_8_1");
        FROM.put(Feature.BETA_EXHAUSTION_COSTS, P + "beta_1_8_1");
        FROM.put(Feature.BETA_REGENERATION, P + "beta_1_8_1");
        FROM.put(Feature.OLD_ENDER_CHEST_MODEL, P + "release_1_3");
        // The upper end of both windows is Release 1.9 (15w44a made the apple uncraftable), which is newer than every
        // declared build - so among the declared set these are "from" windows. Release 1.6.1 (13w24b) added the
        // Absorption and raises Regeneration to V; Release 1.6.4 is the first declared build at or after it.
        FROM.put(Feature.LEGACY_ENCHANTED_GOLDEN_APPLE, P + "release_1_3");
        FROM.put(Feature.ENCHANTED_GOLDEN_APPLE_ABSORPTION, P + "release_1_6_4");

        EXACTLY.put(Feature.END_PORTAL_FRAME_EXPLODES, P + "beta_1_9_pre_3");
        EXACTLY.put(Feature.END_PORTAL_FRAME_LIGHT_15, P + "beta_1_9_pre_3");
        EXACTLY.put(Feature.JUMP_EXPERIENCE_BUG, P + "beta_1_9_pre_1");
        EXACTLY.put(Feature.SILKY_SPAWNERS, P + "beta_1_9_pre_4");
        EXACTLY.put(Feature.SNOWBALL_SURPLUS, P + "alpha_1_0_6");
        EXACTLY.put(Feature.F4_PORTAL_KEY, P + "alpha_1_2_2a");
        EXACTLY.put(Feature.ENCHANTING_PLACEHOLDER, P + "beta_1_9_pre_3");

        // Beta 1.8 up to, not including, the stated build.
        BETWEEN.put(Feature.BETA_SPRINT_JUMP_EXHAUSTION, new String[] {P + "beta_1_8_1", P + "release_1_6_4"});
        BETWEEN.put(Feature.BETA_FOOD_HUNGER_VALUES, new String[] {P + "beta_1_8_1", P + "release_1_6_4"});
        BETWEEN.put(Feature.BETA_CRITICAL_HIT_FORMULA, new String[] {P + "beta_1_8_1", P + "release_1_6_4"});
        BETWEEN.put(Feature.OLD_SMOOTH_LIGHTING, new String[] {P + "beta_1_3", P + "beta_1_8_1"});
        BETWEEN.put(Feature.FLAT_SKY_COLOUR, new String[] {P + "infdev_420", P + "beta_1_8_1"});
        BETWEEN.put(Feature.BETA_STARS, new String[] {P + "beta_1_5", P + "release_1_3"});
        BETWEEN.put(Feature.OLD_CREATIVE_MENU, new String[] {P + "beta_1_8_1", P + "release_1_3"});
        BETWEEN.put(Feature.LEGACY_TOOLTIP_BOX, new String[] {P + "beta_1_0", P + "beta_1_9_pre_2"});
        BETWEEN.put(Feature.OLD_FOLIAGE_COLOUR_MAP, new String[] {P + "alpha_1_2_0", P + "beta_1_2"});
        BETWEEN.put(Feature.BETA_GOLDEN_APPLE, new String[] {P + "beta_1_8_1", P + "release_1_1"});
        // The golden apple's two historical recipes: gold blocks until Release 1.1, gold nuggets until 1.9.
        BETWEEN.put(Feature.GOLDEN_APPLE_GOLD_BLOCK_RECIPE, new String[] {P + "infdev_420", P + "release_1_1"});
        // No declared version is at or after Release 1.9, so the nugget window is "from Release 1.1" for
        // every declared version; the enchanted apple is described the same way for the same reason.
        FROM.put(Feature.GOLDEN_APPLE_GOLD_NUGGET_RECIPE, P + "release_1_1");
        BETWEEN.put(Feature.ARROW_IRON_TIP_RECIPE, new String[] {P + "alpha_1_0_1", P + "alpha_1_0_14"});
        BETWEEN.put(Feature.CLOTH_ARMOUR_RECIPE, new String[] {P + "alpha_1_0_1", P + "alpha_1_0_11"});
        BETWEEN.put(Feature.WOOL_NINE_STRING_RECIPE, new String[] {P + "alpha_1_0_1", P + "beta_1_7_3"});
        BETWEEN.put(Feature.GOLDEN_APPLE_REGEN_ONE, new String[] {P + "release_1_0_0", P + "release_1_6_4"});
        // Release 1.6.1 is not a declared version, so for declared builds this is "from the 1.6.4 proxy".
        FROM.put(Feature.GOLDEN_APPLE_SIX_ONE_EFFECTS, P + "release_1_6_4");
        // The stance outlives the reduction: Prerelease 1 and 2 still have the reduction, Prerelease 3 does not.
        BETWEEN.put(Feature.SWORD_BLOCK_FALL_PROTECTION, new String[] {P + "beta_1_8_1", P + "beta_1_9_pre_3"});

        // The version label appears, disappears and reappears, so no single boundary describes it.
        MULTI.add(Feature.VERSION_LABEL);

        ALL_DECLARED.addAll(List.of(Feature.NO_ATTACK_COOLDOWN, Feature.NO_SWEEP_ATTACK, Feature.NO_SWIMMING,
            Feature.OLD_ZOMBIE_ARMS, Feature.NO_TOOL_SHAPING, Feature.OLD_FISHING_LOOT, Feature.ORE_DROPS_AS_BLOCKS,
            Feature.NO_OFFHAND, Feature.NO_RECIPE_BOOK, Feature.NO_NETHER_ENDERMEN, Feature.ORIGINAL_DRAGON_FIGHT));
    }

    private static int key(String id) {
        return HistoricalVersions.byId(id).orElseThrow(() -> new AssertionError("not declared: " + id)).chronology();
    }

    @Test
    @DisplayName("every feature has exactly one stated expectation, so none can be added without a boundary")
    void everyFeatureIsDescribed() {
        EnumSet<Feature> described = EnumSet.noneOf(Feature.class);
        List<Feature> duplicates = new ArrayList<>();
        for (EnumSet<Feature> group : List.of(EnumSet.copyOf(BEFORE.keySet()), EnumSet.copyOf(FROM.keySet()),
            EnumSet.copyOf(EXACTLY.keySet()), EnumSet.copyOf(BETWEEN.keySet()), EnumSet.copyOf(ALL_DECLARED), EnumSet.copyOf(MULTI))) {
            for (Feature feature : group) {
                if (!described.add(feature)) {
                    duplicates.add(feature);
                }
            }
        }
        assertTrue(duplicates.isEmpty(), "described twice: " + duplicates);
        assertEquals(EnumSet.allOf(Feature.class), described, "a feature has no stated expectation");
    }

    @Test
    @DisplayName("each feature is active exactly where the research says, in every declared version")
    void everyFeatureMatchesItsBoundary() {
        List<String> wrong = new ArrayList<>();
        for (HistoricalVersion version : HistoricalVersions.all()) {
            int k = version.chronology();
            BEFORE.forEach((feature, boundary) -> check(wrong, feature, version, k < key(boundary)));
            FROM.forEach((feature, boundary) -> check(wrong, feature, version, k >= key(boundary)));
            EXACTLY.forEach((feature, build) -> check(wrong, feature, version, version.id().equals(build)));
            BETWEEN.forEach((feature, range) ->
                check(wrong, feature, version, k >= key(range[0]) && k < key(range[1])));
            ALL_DECLARED.forEach(feature -> check(wrong, feature, version, true));
        }
        assertTrue(wrong.isEmpty(), wrong.size() + " wrong answers: " + wrong);
    }

    private static void check(List<String> wrong, Feature feature, HistoricalVersion version, boolean expected) {
        if (Features.active(feature, version) != expected) {
            wrong.add(feature + " on " + version.id() + " expected " + expected);
        }
    }

    @Test
    @DisplayName("the version label appears, disappears and reappears at the builds the clients record")
    void versionLabelFollowsTheClients() {
        // Always drawn through Beta 1.6; gone in 1.6.5 (so Beta 1.7.3 has none); back in Beta 1.8; gone again in
        // Prerelease 4; back in Prerelease 5; gone in the release.
        for (String shown : List.of("infdev_420", "alpha", "alpha_1_2_2a", "beta_1_0", "beta_1_6", "beta_1_8_1",
            "beta_1_9_pre_1", "beta_1_9_pre_3", "beta_1_9_pre_5", "beta_1_9_pre_6")) {
            assertTrue(Features.active(Feature.VERSION_LABEL, P + shown), shown + " draws the label");
        }
        for (String hidden : List.of("beta_1_7_3", "beta_1_9_pre_4", "release_1_0_0", "release_1_6_4")) {
            assertFalse(Features.active(Feature.VERSION_LABEL, P + hidden), hidden + " does not draw the label");
        }
    }

    @Test
    @DisplayName("the proxy builds keep the behaviour their boundary introduces")
    void theProxyVersionItselfKeepsTheBehaviour() {
        // Release 1.4.2 introduced geared mobs and Release 1.6.4 stands in for it, so 1.6.4 must KEEP geared mobs:
        // a gate that applied to it would delete content that build had.
        for (Feature f : List.of(Feature.NO_MONSTER_ITEM_PICKUP, Feature.NO_MONSTER_GEAR, Feature.NO_MONSTER_ENCHANTS,
            Feature.NO_BABY_ZOMBIES)) {
            assertFalse(Features.active(f, P + "release_1_6_4"), f + " must not apply to its own proxy");
        }
        assertFalse(Features.active(Feature.NO_MONSTER_SUN_AVOIDANCE, P + "release_1_2_5"));
        assertFalse(Features.active(Feature.NO_ANIMAL_PANIC, P + "beta_1_8_1"), "Beta 1.8.1 introduced panic");
        assertFalse(Features.active(Feature.NO_ANIMAL_BREEDING, P + "beta_1_9_pre_3"));
    }

    @Test
    @DisplayName("a feature that is not a deviation in an ordinary world cannot exist: all are inactive")
    void ordinaryWorldsAreUntouched() {
        for (String ordinary : new String[] {null, "", "moderner_beta:beta", "example:not_a_version"}) {
            for (Feature feature : Feature.values()) {
                assertFalse(Features.active(feature, ordinary), feature + " for " + ordinary);
            }
        }
    }

    @Test
    @DisplayName("the research table is complete: every feature carries evidence and a domain")
    void everyFeatureCarriesEvidence() {
        for (Feature feature : Feature.values()) {
            assertFalse(feature.evidence().isBlank(), feature + " states no evidence");
            assertFalse(feature.windows().isEmpty(), feature + " has no window");
            assertTrue(feature.domain() != null, feature + " has no domain");
        }
    }

    @Test
    @DisplayName("windows reject nonsense instead of silently matching nothing")
    void windowsValidate() {
        assertThrows(IllegalArgumentException.class,
            () -> FeatureWindow.between(Milestones.BETA_1_8, Milestones.ALPHA_1_2_0));
        assertThrows(IllegalArgumentException.class, () -> FeatureWindow.exactly(" "));
        assertThrows(IllegalArgumentException.class, () -> new Milestone(1, " "));
    }
}