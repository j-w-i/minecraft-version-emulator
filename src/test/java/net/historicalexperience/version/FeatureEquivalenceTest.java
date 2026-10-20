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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. A differential test: the new feature table against the string gates and predicates it replaces.
 *
 * <p>This exists only for the migration. It compares every {@link Feature} with the rule it was transcribed from,
 * across every declared version and the ordinary-world inputs, so a window that was copied wrongly fails here before
 * any caller depends on it. It is deleted together with the old rules in the step that removes them; the golden
 * snapshot keeps guarding the answers afterwards.
 */
class FeatureEquivalenceTest {
    private static final List<String> INPUTS = new ArrayList<>();

    static {
        HistoricalVersions.all().forEach(version -> INPUTS.add(version.id()));
        INPUTS.add(null);
        INPUTS.add("");
        INPUTS.add("moderner_beta:beta");
        INPUTS.add("example:not_a_version");
    }

    /** Feature -> the old rule, as "is the deviation active". */
    private static Map<Feature, Predicate<String>> old() {
        Map<Feature, Predicate<String>> m = new LinkedHashMap<>();
        gate(m, Feature.NO_SKELETON_STRAFING, "skeletonStrafing");
        gate(m, Feature.NO_CRITICAL_HITS, "criticalHit");
        gate(m, Feature.NO_ANIMAL_PANIC, "panic");
        gate(m, Feature.NO_MONSTER_SUN_AVOIDANCE, "avoidSun");
        gate(m, Feature.OLD_SKELETON_DROPS, "customDeathLoot");
        gate(m, Feature.NO_ANIMAL_BREEDING, "breeding");
        gate(m, Feature.NO_ANIMAL_TEMPTING, "tempting");
        gate(m, Feature.NO_BABY_ANIMALS, "babyAnimal");
        gate(m, Feature.NO_MONSTER_ITEM_PICKUP, "itemPickup");
        gate(m, Feature.NO_MONSTER_GEAR, "monsterGear");
        gate(m, Feature.NO_MONSTER_ENCHANTS, "monsterEnchant");
        gate(m, Feature.NO_BABY_ZOMBIES, "babyZombie");
        gate(m, Feature.NO_ATTACK_COOLDOWN, "cooldown");
        gate(m, Feature.NO_SWEEP_ATTACK, "sweep");
        gate(m, Feature.NO_SWIMMING, "swim");
        gate(m, Feature.OLD_ZOMBIE_ARMS, "mobArms");
        gate(m, Feature.NO_TOOL_SHAPING, "noToolShaping");
        gate(m, Feature.OLD_FISHING_LOOT, "oldFishingLoot");
        gate(m, Feature.PUNCH_SHEARS, "punchShear");
        gate(m, Feature.OLD_TOOL_DURABILITY, "toolDurability");
        gate(m, Feature.END_PORTAL_FRAME_EXPLODES, "endPortalFrameExplodes");
        gate(m, Feature.END_PORTAL_FRAME_LIGHT_15, "endPortalFrameLightFifteen");
        gate(m, Feature.JUMP_EXPERIENCE_BUG, "jumpExperienceBug");
        gate(m, Feature.SILKY_SPAWNERS, "silkySpawners");
        gate(m, Feature.SNOWBALL_SURPLUS, "snowballSurplus");
        gate(m, Feature.INSTANT_BOW, "instantBow");
        gate(m, Feature.ARROW_SPEED, "arrowSpeed");
        gate(m, Feature.INVINCIBLE_BOW, "invincibleBow");
        gate(m, Feature.INSTANT_AIR, "instantAir");
        gate(m, Feature.INSTANT_BONE_MEAL, "instantBonemeal");
        gate(m, Feature.PUNCH_TNT, "punchTnt");
        gate(m, Feature.GRASS_SEEDS, "grassSeeds");
        gate(m, Feature.NO_FISHING_EXPERIENCE, "noFishingExperience");

        m.put(Feature.ORE_DROPS_AS_BLOCKS, id -> HistoricalVersions.byId(id).isPresent());
        m.put(Feature.OLD_SWORD_BLOCKING, HistoricalGameplayTweaks::swordBlockingEnabled);
        m.put(Feature.BETA_EXHAUSTION_COSTS, HistoricalGameplayTweaks::betaExhaustionCosts);
        m.put(Feature.BETA_REGENERATION, HistoricalGameplayTweaks::betaRegenerationModel);
        m.put(Feature.BETA_SPRINT_JUMP_EXHAUSTION, HistoricalGameplayTweaks::betaSprintJumpExhaustion);
        m.put(Feature.BETA_FOOD_HUNGER_VALUES, HistoricalGameplayTweaks::betaFoodHungerValues);
        m.put(Feature.BETA_CRITICAL_HIT_FORMULA, HistoricalGameplayTweaks::betaCriticalHitFormula);
        m.put(Feature.BETA_GOLDEN_APPLE, HistoricalGameplayTweaks::betaGoldenApple);
        m.put(Feature.OLD_SWORD_DAMAGE, HistoricalGameplayTweaks::swordDamageBeforeReleaseOnePointZero);
        m.put(Feature.SWORD_BLOCK_FALL_PROTECTION, HistoricalGameplayTweaks::blockFallDamageReductionEnabled);
        m.put(Feature.ENCHANTING_PLACEHOLDER, HistoricalGameplayTweaks::placeholderEnchantmentTooltip);
        m.put(Feature.NO_REAL_ENCHANTING, id -> !HistoricalGameplayTweaks.functionalEnchanting(id));
        m.put(Feature.NO_ENCHANTING, HistoricalGameplayTweaks::enchantingDisabled);
        m.put(Feature.NO_SNEAKING, id -> !HistoricalGameplayTweaks.sneakingAvailable(id));
        m.put(Feature.NO_BREWING, id -> !HistoricalGameplayTweaks.functionalBrewing(id));
        m.put(Feature.INERT_END_PORTAL, id -> !HistoricalGameplayTweaks.endPortalFunctional(id));
        m.put(Feature.HIDDEN_XP_LEVEL, id -> !HistoricalGameplayTweaks.experienceLevelShown(id));
        m.put(Feature.NO_NETHER, id -> !HistoricalGameplayTweaks.netherAvailable(id));
        m.put(Feature.UNSTACKABLE_FOOD, id -> !HistoricalGameplayTweaks.foodStacks(id));
        m.put(Feature.NO_OFFHAND, HistoricalGameplayTweaks::offhandUnavailable);
        m.put(Feature.F4_PORTAL_KEY, HistoricalGameplayTweaks::f4SpawnsPortal);
        return m;
    }

    private static void gate(Map<Feature, Predicate<String>> m, Feature feature, String gate) {
        m.put(feature, id -> HistoricalGameplayTweaks.appliesTo(gate, id));
    }

    @Test
    @DisplayName("every feature answers exactly as the rule it replaced, for every version and ordinary input")
    void featuresMatchTheOldRules() {
        Map<Feature, Predicate<String>> old = old();
        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<Feature, Predicate<String>> entry : old.entrySet()) {
            for (String input : INPUTS) {
                boolean expected = entry.getValue().test(input);
                boolean actual = Features.active(entry.getKey(), input);
                if (expected != actual) {
                    mismatches.add(entry.getKey() + " on " + input + ": old=" + expected + " new=" + actual);
                }
            }
        }
        assertTrue(mismatches.isEmpty(), mismatches.size() + " mismatches: " + mismatches);
    }

    @Test
    @DisplayName("the comparison covers every feature, so a new feature cannot skip it silently")
    void everyFeatureIsCompared() {
        assertEquals(EnumSet.allOf(Feature.class), EnumSet.copyOf(old().keySet()),
            "a Feature with no old rule to compare against must be added deliberately");
    }

    @Test
    @DisplayName("an ordinary world has no feature active")
    void ordinaryWorldsHaveNoFeatures() {
        for (String ordinary : new String[] {null, "", "moderner_beta:beta", "example:not_a_version"}) {
            for (Feature feature : Feature.values()) {
                assertFalse(Features.active(feature, ordinary), feature + " must be inactive for " + ordinary);
            }
        }
    }

    @Test
    @DisplayName("the session path agrees with the named-version path")
    void sessionMatchesNamedVersion() {
        try {
            for (HistoricalVersion version : HistoricalVersions.all()) {
                ActiveVersion.set(version.id());
                for (Feature feature : Feature.values()) {
                    assertEquals(Features.active(feature, version.id()), Features.active(feature),
                        feature + " on " + version.id());
                }
            }
            ActiveVersion.clear();
            for (Feature feature : Feature.values()) {
                assertFalse(Features.active(feature), feature + " with no session");
            }
        } finally {
            ActiveVersion.clear();
        }
    }
}
