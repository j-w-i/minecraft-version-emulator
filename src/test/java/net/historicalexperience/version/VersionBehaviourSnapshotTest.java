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

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.historicalexperience.client.colour.HistoricalColormaps;
import net.historicalexperience.client.hud.HistoricalVersionLabel;
import net.historicalexperience.client.lighting.HistoricalChestTweaks;
import net.historicalexperience.client.lighting.HistoricalFogTweaks;
import net.historicalexperience.client.lighting.HistoricalLightingTweaks;
import net.historicalexperience.client.lighting.HistoricalOldLighting;
import net.historicalexperience.client.lighting.HistoricalSmoothLighting;
import net.historicalexperience.client.lighting.HistoricalStars;
import net.historicalexperience.client.lighting.HistoricalWorldTweaks;
import net.historicalexperience.client.textures.HistoricalTextureRules;
import net.historicalexperience.client.tooltip.HistoricalTooltips;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalCraftingEra;
import net.historicalexperience.gameplay.HistoricalCreativeCatalog;
import net.historicalexperience.gameplay.HistoricalCreativeRules;
import net.historicalexperience.gameplay.HistoricalDragonFight;
import net.historicalexperience.gameplay.HistoricalEnchanting;
import net.historicalexperience.gameplay.HistoricalFoodRules;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.historicalexperience.gameplay.HistoricalItemAvailability;
import net.historicalexperience.gameplay.HistoricalLeftClickEvents;
import net.historicalexperience.gameplay.HistoricalLootRules;
import net.historicalexperience.gameplay.HistoricalPlayerRules;
import net.historicalexperience.gameplay.HistoricalSpawnRules;
import net.historicalexperience.gameplay.HistoricalToolDurability;
import net.historicalexperience.gameplay.HistoricalWeather;
import net.historicalexperience.generation.HistoricalBlockAvailability;
import net.historicalexperience.generation.HistoricalBlockPalette;
import net.historicalexperience.generation.HistoricalStructureRules;
import net.historicalexperience.world.upgrade.BiomeCarryForward;
import net.historicalexperience.worldgen.HistoricalVillageBlocks;
import net.historicalexperience.worldgen.HistoricalVillageRules;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Golden-master snapshot of EVERY pure, version-keyed answer the code gives, for every declared version plus the
 * "ordinary world" inputs ({@code null}, {@code moderner_beta:beta}, {@code example:not_a_version}).
 *
 * <p>Purpose: a refactor will move these decisions into a declarative system, and this file is how we prove nothing
 * changed unintentionally. It records, it does not judge: a surprising answer is a fact to preserve (or to change on
 * purpose, in a reviewed diff of the golden file), never something this test corrects.
 *
 * <h2>Line format</h2>
 * {@code domain.method(args)|<versionId or null>=<result>}. Results are {@code String.valueOf}; sets, maps are rendered
 * sorted so the output is deterministic. A probe that throws is recorded as {@code THROWS <ExceptionClass>} rather than
 * aborting. A class that cannot be loaded in plain JUnit is recorded once as {@code UNLOADABLE <class>}.
 *
 * <p>Large cross-products (items x versions, blocks x versions, recipes x versions ...) are emitted as ONE line per key:
 * {@code name(key)|*=<one token per input, in the order given by the matching name.inputs line>}. Booleans are T/F,
 * enums their first three letters, null is "-". This keeps the file reviewable; a diff still names the exact key.
 *
 * <h2>Updating</h2>
 * Set the environment variable {@code HEX_GOLDEN_UPDATE=1} to (re)write
 * {@code src/test/resources/golden/version-behaviour.txt}. Without it the test compares line by line and reports the
 * first 20 differing lines. To add a probe, add one {@code probe(...)}/{@code perId(...)}/{@code perVersion(...)} call
 * to the matching domain method below.
 */
class VersionBehaviourSnapshotTest {
    private static final String UPDATE_ENV = "HEX_GOLDEN_UPDATE";
    private static final String GOLDEN_RELATIVE = "src/test/resources/golden/version-behaviour.txt";

    /** Inputs that are not a declared version: no world, a foreign preset, and an undeclared id. */
    private static final List<String> ORDINARY =
        Collections.unmodifiableList(Arrays.asList(null, "moderner_beta:beta", "example:not_a_version"));
    private static final List<HistoricalVersion> VERSIONS = HistoricalVersions.all();
    private static final List<String> IDS = VERSIONS.stream().map(HistoricalVersion::id).toList();
    /** Declared ids first (oldest first), then the ordinary inputs. */
    private static final List<String> INPUTS = concat(IDS, ORDINARY);
    /** Version objects plus null, for the client rules that take a HistoricalVersion. */
    private static final List<HistoricalVersion> VERSION_INPUTS = withNull(VERSIONS);

    private static final String P = "historical_experience:";

    private static final List<String> GATES = List.of(
        "skeletonStrafing", "criticalHit", "panic", "avoidSun", "customDeathLoot", "breeding", "tempting",
        "babyAnimal", "itemPickup", "monsterGear", "monsterEnchant", "babyZombie", "cooldown", "sweep", "swim",
        "punchShear", "toolDurability", "mobArms", "endPortalFrameExplodes", "jumpExperienceBug", "silkySpawners",
        "endPortalFrameLightFifteen", "snowballSurplus", "noToolShaping", "instantBow", "arrowSpeed",
        "invincibleBow", "instantAir", "instantBonemeal", "punchTnt", "grassSeeds", "oldFishingLoot",
        "noFishingExperience");

    // =========================================================================================================
    // The test
    // =========================================================================================================

    @Test
    @DisplayName("every version-keyed answer matches the golden master")
    void versionBehaviourMatchesGoldenMaster() throws IOException {
        List<String> actual = build();
        Path golden = Path.of(System.getProperty("user.dir")).resolve(GOLDEN_RELATIVE);

        if ("1".equals(System.getenv(UPDATE_ENV))) {
            Files.createDirectories(golden.getParent());
            Files.write(golden, (String.join("\n", actual) + "\n").getBytes(StandardCharsets.UTF_8));
            System.out.println("[VersionBehaviourSnapshotTest] wrote " + actual.size() + " lines to " + golden);
            return;
        }

        if (!Files.isRegularFile(golden)) {
            fail("Golden file missing: " + golden + " - run once with " + UPDATE_ENV + "=1 to create it.");
        }
        String text = Files.readString(golden, StandardCharsets.UTF_8);
        List<String> expected = new ArrayList<>(Arrays.asList(text.split("\n", -1)));
        if (!expected.isEmpty() && expected.get(expected.size() - 1).isEmpty()) {
            expected.remove(expected.size() - 1);
        }

        List<String> diffs = new ArrayList<>();
        int totalDiffs = 0;
        int max = Math.max(expected.size(), actual.size());
        for (int i = 0; i < max; i++) {
            String e = i < expected.size() ? expected.get(i) : "<missing>";
            String a = i < actual.size() ? actual.get(i) : "<missing>";
            if (!e.equals(a)) {
                totalDiffs++;
                if (diffs.size() < 20) {
                    diffs.add("line " + (i + 1) + ":\n    golden: " + e + "\n    actual: " + a);
                }
            }
        }
        if (totalDiffs > 0) {
            fail("Version behaviour changed: " + totalDiffs + " differing line(s) (golden " + expected.size()
                + " lines, actual " + actual.size() + " lines). First " + diffs.size() + ":\n"
                + String.join("\n", diffs)
                + "\nIf intentional, re-run with " + UPDATE_ENV + "=1 and review the golden diff.");
        }
    }

    // =========================================================================================================
    // The probe list. One domain method per area; one probe call per line of intent.
    // =========================================================================================================

    private static List<String> build() {
        List<String> L = new ArrayList<>(40_000);
        L.add("meta.versionCount|*=" + IDS.size());
        L.add("meta.versionOrder|*=" + String.join(" ", IDS));
        L.add("meta.ordinaryInputs|*=null moderner_beta:beta example:not_a_version");
        gameplayTweaks(L);
        gameplayTweaksSession(L);
        enchanting(L);
        dragonFight(L);
        weatherLeftClickCreative(L);
        itemAndCreativeAvailability(L);
        blockAvailabilityAndPalette(L);
        craftingEra(L);
        lootPlayerFoodToolSpawn(L);
        structuresAndVillages(L);
        clientRules(L);
        biomeCarryForward(L);
        versionRegistry(L);
        return L;
    }

    // ---- 1. gameplay.HistoricalGameplayTweaks ------------------------------------------------------------

    private static void gameplayTweaks(List<String> L) {
        String c = "net.historicalexperience.gameplay.HistoricalGameplayTweaks";
        if (!loadable(L, c)) {
            return;
        }
        String d = "gameplay.HistoricalGameplayTweaks.";
        for (String gate : GATES) {
            perId(L, d + "appliesTo(" + gate + ")", id -> HistoricalGameplayTweaks.appliesTo(gate, id));
        }
        probe(L, d + "appliesTo(bogusGate)|x", () -> HistoricalGameplayTweaks.appliesTo("bogusGate", "x"));
        probe(L, d + "gateNames()|-", () -> HistoricalGameplayTweaks.gateNames());
        probe(L, d + "documentedGates()|-", () -> HistoricalGameplayTweaks.documentedGates());
        for (String gate : HistoricalGameplayTweaks.documentedGates()) {
            probe(L, d + "evidenceFor(" + gate + ").length|-", () -> HistoricalGameplayTweaks.evidenceFor(gate).length());
        }
        probe(L, d + "evidenceFor(unknown)|-", () -> HistoricalGameplayTweaks.evidenceFor("unknown"));

        perId(L, d + "preBeta18", id -> HistoricalGameplayTweaks.preBeta18(id));
        perId(L, d + "swordBlockingEnabled", id -> HistoricalGameplayTweaks.swordBlockingEnabled(id));
        perId(L, d + "betaExhaustionCosts", id -> HistoricalGameplayTweaks.betaExhaustionCosts(id));
        perId(L, d + "betaSprintJumpExhaustion", id -> HistoricalGameplayTweaks.betaSprintJumpExhaustion(id));
        perId(L, d + "functionalEnchanting", id -> HistoricalGameplayTweaks.functionalEnchanting(id));
        perId(L, d + "enchantingDisabled", id -> HistoricalGameplayTweaks.enchantingDisabled(id));
        perId(L, d + "placeholderEnchantmentTooltip", id -> HistoricalGameplayTweaks.placeholderEnchantmentTooltip(id));
        perId(L, d + "f4SpawnsPortal", id -> HistoricalGameplayTweaks.f4SpawnsPortal(id));
        perId(L, d + "netherAvailable", id -> HistoricalGameplayTweaks.netherAvailable(id));
        perId(L, d + "foodStacks", id -> HistoricalGameplayTweaks.foodStacks(id));
        perId(L, d + "offhandUnavailable", id -> HistoricalGameplayTweaks.offhandUnavailable(id));
        perId(L, d + "experienceLevelShown", id -> HistoricalGameplayTweaks.experienceLevelShown(id));
        perId(L, d + "sneakingAvailable", id -> HistoricalGameplayTweaks.sneakingAvailable(id));
        perId(L, d + "functionalBrewing", id -> HistoricalGameplayTweaks.functionalBrewing(id));
        perId(L, d + "endPortalFunctional", id -> HistoricalGameplayTweaks.endPortalFunctional(id));
        perId(L, d + "enchantingTableExists", id -> HistoricalGameplayTweaks.enchantingTableExists(id));
        perId(L, d + "betaGoldenApple", id -> HistoricalGameplayTweaks.betaGoldenApple(id));
        perId(L, d + "betaFoodHungerValues", id -> HistoricalGameplayTweaks.betaFoodHungerValues(id));
        perId(L, d + "betaRegenerationModel", id -> HistoricalGameplayTweaks.betaRegenerationModel(id));
        perId(L, d + "betaCriticalHitFormula", id -> HistoricalGameplayTweaks.betaCriticalHitFormula(id));
        perId(L, d + "swordDamageBeforeReleaseOnePointZero",
            id -> HistoricalGameplayTweaks.swordDamageBeforeReleaseOnePointZero(id));
        perId(L, d + "blockFallDamageReductionEnabled",
            id -> HistoricalGameplayTweaks.blockFallDamageReductionEnabled(id));
        perId(L, d + "jumpRequiresFreshPress", id -> HistoricalGameplayTweaks.jumpRequiresFreshPress(id));
        perId(L, d + "jumpExperienceBug(seed=42)",
            id -> HistoricalGameplayTweaks.jumpExperienceBug(id, RandomSource.create(42L)));

        for (String boundary : Arrays.asList(P + "beta_1_2", P + "beta_1_7_3", P + "beta_1_8_1", P + "release_1_4",
            P + "release_1_6_4", "example:not_a_version", null)) {
            perId(L, d + "olderThanVersion(boundary=" + boundary + ")",
                id -> HistoricalGameplayTweaks.olderThanVersion(id, boundary));
        }

        probe(L, d + "preBeta18BoundaryChronology()|-", () -> HistoricalGameplayTweaks.preBeta18BoundaryChronology());
        probe(L, d + "sneakingBoundaryChronology()|-", () -> HistoricalGameplayTweaks.sneakingBoundaryChronology());
        probe(L, d + "offhandBoundaryChronology()|-", () -> HistoricalGameplayTweaks.offhandBoundaryChronology());
        probe(L, d + "netherBoundaryChronology()|-", () -> HistoricalGameplayTweaks.netherBoundaryChronology());
        probe(L, d + "f4PortalBoundaryChronology()|-", () -> HistoricalGameplayTweaks.f4PortalBoundaryChronology());
        probe(L, d + "functionalEnchantingBoundaryChronology()|-",
            () -> HistoricalGameplayTweaks.functionalEnchantingBoundaryChronology());
        probe(L, d + "swordDamageBeforeReleaseBoundaryChronology()|-",
            () -> HistoricalGameplayTweaks.swordDamageBeforeReleaseBoundaryChronology());
        probe(L, d + "eraArrowSpeedPercent()|-", () -> HistoricalGameplayTweaks.eraArrowSpeedPercent());
    }

    /** The no-argument predicates that read the process-wide active session. */
    private static void gameplayTweaksSession(List<String> L) {
        if (!loadable(L, "net.historicalexperience.gameplay.HistoricalActiveSession")
            || !loadable(L, "net.historicalexperience.gameplay.HistoricalGameplayTweaks")) {
            return;
        }
        String d = "gameplay.HistoricalGameplayTweaks.";
        Map<String, Supplier<Object>> predicates = new LinkedHashMap<>();
        predicates.put("oreDropsAsBlocks", () -> HistoricalGameplayTweaks.oreDropsAsBlocks());
        predicates.put("endPortalFrameExplodes", () -> HistoricalGameplayTweaks.endPortalFrameExplodes());
        predicates.put("silkySpawnerDrops", () -> HistoricalGameplayTweaks.silkySpawnerDrops());
        predicates.put("endPortalFrameLightFifteen", () -> HistoricalGameplayTweaks.endPortalFrameLightFifteen());
        predicates.put("snowballSurplus", () -> HistoricalGameplayTweaks.snowballSurplus());
        predicates.put("noToolShaping", () -> HistoricalGameplayTweaks.noToolShaping());
        predicates.put("instantAir", () -> HistoricalGameplayTweaks.instantAir());
        predicates.put("instantBonemeal", () -> HistoricalGameplayTweaks.instantBonemeal());
        predicates.put("punchTnt", () -> HistoricalGameplayTweaks.punchTnt());
        predicates.put("grassSeeds", () -> HistoricalGameplayTweaks.grassSeeds());
        predicates.put("oldFishingLoot", () -> HistoricalGameplayTweaks.oldFishingLoot());
        predicates.put("noFishingExperience", () -> HistoricalGameplayTweaks.noFishingExperience());
        predicates.put("instantBow", () -> HistoricalGameplayTweaks.instantBow());
        predicates.put("invincibleBow", () -> HistoricalGameplayTweaks.invincibleBow());
        predicates.put("jumpExperienceBug(seed=42)",
            () -> HistoricalGameplayTweaks.jumpExperienceBug(RandomSource.create(42L)));
        predicates.put("HistoricalActiveSession.current", () -> HistoricalActiveSession.current());
        predicates.put("HistoricalActiveSession.isHistorical", () -> HistoricalActiveSession.isHistorical());

        for (Map.Entry<String, Supplier<Object>> predicate : predicates.entrySet()) {
            Supplier<Object> body = predicate.getValue();
            for (String id : INPUTS) {
                probe(L, d + predicate.getKey() + "()[session]|" + label(id), () -> {
                    try {
                        HistoricalActiveSession.set(id);
                        return body.get();
                    } finally {
                        HistoricalActiveSession.clear();
                    }
                });
            }
            probe(L, d + predicate.getKey() + "()[session]|<no-session>", () -> {
                HistoricalActiveSession.clear();
                return body.get();
            });
        }
        // A blank id is documented to mean "no historical world".
        probe(L, d + "HistoricalActiveSession.current()[set(blank)]|<blank>", () -> {
            try {
                HistoricalActiveSession.set("  ");
                return HistoricalActiveSession.current();
            } finally {
                HistoricalActiveSession.clear();
            }
        });
    }

    // ---- 2. gameplay.HistoricalEnchanting ----------------------------------------------------------------

    private static void enchanting(List<String> L) {
        if (!loadable(L, "net.historicalexperience.gameplay.HistoricalEnchanting")) {
            return;
        }
        String d = "gameplay.HistoricalEnchanting.";
        perId(L, d + "usesOldTableRules", id -> HistoricalEnchanting.usesOldTableRules(id));
        probe(L, d + "constants()|-", () -> "MODERN_ENCHANTING_FROM=" + HistoricalEnchanting.MODERN_ENCHANTING_FROM
            + " THORNS_FROM=" + HistoricalEnchanting.THORNS_FROM);
        probe(L, d + "size()|-", () -> HistoricalEnchanting.size());
        probe(L, d + "placeholderEnchantment()|-",
            () -> HistoricalEnchanting.placeholderEnchantment().location() + " level="
                + HistoricalEnchanting.placeholderEnchantmentLevel());
        probe(L, d + "prereleaseFourEnchantments()|-", () -> HistoricalEnchanting.prereleaseFourEnchantments().stream()
            .map(key -> key.location().toString()).sorted().toList());
        Map<String, Integer> introduced = new TreeMap<>();
        HistoricalEnchanting.all().forEach((key, chronology) -> introduced.put(key.location().toString(), chronology));
        probe(L, d + "all()|-", () -> introduced);
        List<net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>> keys =
            new ArrayList<>(HistoricalEnchanting.all().keySet());
        keys.sort((a, b) -> a.location().toString().compareTo(b.location().toString()));
        for (var key : keys) {
            perId(L, d + "available(" + key.location() + ")", id -> HistoricalEnchanting.available(id, key));
        }
    }

    // ---- 3. gameplay.HistoricalDragonFight ---------------------------------------------------------------

    private static void dragonFight(List<String> L) {
        if (!loadable(L, "net.historicalexperience.gameplay.HistoricalDragonFight")) {
            return;
        }
        String d = "gameplay.HistoricalDragonFight.";
        perId(L, d + "bossBarCaption", id -> HistoricalDragonFight.bossBarCaption(id));
        perId(L, d + "originalFight", id -> HistoricalDragonFight.originalFight(id));
        probe(L, d + "constants()|-", () -> "REVAMP_CHRONOLOGY=" + HistoricalDragonFight.REVAMP_CHRONOLOGY
            + " BOSS_HEALTH_CAPTION_END=" + HistoricalDragonFight.BOSS_HEALTH_CAPTION_END
            + " caption=" + HistoricalDragonFight.BOSS_HEALTH_CAPTION);
        for (double distance : new double[] {0.0, 10.0, 48.0, 100.0, 400.0}) {
            probe(L, d + "aimHeightAbove(" + distance + ")|-", () -> HistoricalDragonFight.aimHeightAbove(distance));
        }
    }

    // ---- 4. weather, left click, creative rules ----------------------------------------------------------

    private static void weatherLeftClickCreative(List<String> L) {
        if (loadable(L, "net.historicalexperience.gameplay.HistoricalWeather")) {
            perVersion(L, "gameplay.HistoricalWeather.existsIn", v -> HistoricalWeather.existsIn(v));
        }
        if (loadable(L, "net.historicalexperience.gameplay.HistoricalLeftClickEvents")) {
            perVersion(L, "gameplay.HistoricalLeftClickEvents.availableFor",
                v -> HistoricalLeftClickEvents.availableFor(v));
        }
        if (!loadable(L, "net.historicalexperience.gameplay.HistoricalCreativeRules")) {
            return;
        }
        String d = "gameplay.HistoricalCreativeRules.";
        List<String> selections = selections();
        for (String selection : selections) {
            probe(L, d + "gatesCatalog|" + label(selection), () -> HistoricalCreativeRules.gatesCatalog(selection));
            probe(L, d + "usesOldMenu|" + label(selection), () -> HistoricalCreativeRules.usesOldMenu(selection));
            probe(L, d + "hasCreativeShortcut|" + label(selection),
                () -> HistoricalCreativeRules.hasCreativeShortcut(selection));
            probe(L, d + "allowsCreativeShortcut(ownsWorld=true)|" + label(selection),
                () -> HistoricalCreativeRules.allowsCreativeShortcut(selection, true));
            probe(L, d + "allowsCreativeShortcut(ownsWorld=false)|" + label(selection),
                () -> HistoricalCreativeRules.allowsCreativeShortcut(selection, false));
            probe(L, d + "rejectsStackedBookTarget(2,book)|" + label(selection),
                () -> HistoricalCreativeRules.rejectsStackedBookTarget(selection, 2, true));
            probe(L, d + "allowsBookOnAnyItem(1,book)|" + label(selection),
                () -> HistoricalCreativeRules.allowsBookOnAnyItem(selection, 1, true));
        }
        if (loadable(L, "net.historicalexperience.version.HistoricalSnapshotPatches")) {
            for (String selection : selections) {
                probe(L, "version.HistoricalSnapshotPatches.baseProfileForSelection|" + label(selection),
                    () -> HistoricalSnapshotPatches.baseProfileForSelection(selection));
            }
        }
    }

    // ---- 5. item / creative / block availability ---------------------------------------------------------

    private static void itemAndCreativeAvailability(List<String> L) {
        if (loadable(L, "net.historicalexperience.gameplay.HistoricalItemAvailability")) {
            String d = "gameplay.HistoricalItemAvailability.";
            Map<String, HistoricalItemAvailability.IntroducedIn> all =
                new TreeMap<>(HistoricalItemAvailability.all());
            probe(L, d + "size()|-", () -> HistoricalItemAvailability.size());
            for (var entry : all.entrySet()) {
                HistoricalItemAvailability.IntroducedIn in = entry.getValue();
                probe(L, d + "introducedIn(" + entry.getKey() + ")|-", () -> "version=" + in.versionId()
                    + " chronology=" + in.chronology() + " evidenceLen=" + in.evidence().length());
            }
            List<String> items = new ArrayList<>(all.keySet());
            items.add("minecraft:stone");
            items.add("minecraft:undated_item_for_snapshot");
            matrix(L, d + "existsIn", items, k -> k, INPUTS, VersionBehaviourSnapshotTest::label,
                (item, id) -> HistoricalItemAvailability.existsIn(id, item));
        }
        if (loadable(L, "net.historicalexperience.gameplay.HistoricalCreativeCatalog")) {
            String d = "gameplay.HistoricalCreativeCatalog.";
            Map<String, HistoricalCreativeCatalog.Window> all = new TreeMap<>(HistoricalCreativeCatalog.all());
            for (var entry : all.entrySet()) {
                probe(L, d + "window(" + entry.getKey() + ")|-",
                    () -> entry.getValue().first() + ".." + entry.getValue().endExclusive());
            }
            List<String> selections = selections();
            List<String> items = new ArrayList<>(all.keySet());
            items.add("minecraft:netherite_ingot");
            matrix(L, d + "available", items, k -> k, selections, VersionBehaviourSnapshotTest::label,
                (item, selection) -> HistoricalCreativeCatalog.available(selection, item));
            List<String> potions = Arrays.asList("minecraft:water", "minecraft:healing", "minecraft:long_healing",
                "minecraft:strong_healing", "minecraft:harming", "minecraft:night_vision", "minecraft:invisibility",
                "minecraft:swiftness", "minecraft:strong_swiftness", "minecraft:slowness", "minecraft:strength",
                "minecraft:weakness", "minecraft:poison", "minecraft:regeneration", "minecraft:fire_resistance",
                "minecraft:luck", "minecraft:turtle_master", "other:custom", null);
            for (boolean splash : new boolean[] {false, true}) {
                matrix(L, d + "potionAvailable(splash=" + splash + ")", potions, k -> String.valueOf(k), selections,
                    VersionBehaviourSnapshotTest::label,
                    (potion, selection) -> HistoricalCreativeCatalog.potionAvailable(selection, potion, splash));
            }
        }
    }

    private static void blockAvailabilityAndPalette(List<String> L) {
        if (loadable(L, "net.historicalexperience.generation.HistoricalBlockAvailability")) {
            String d = "generation.HistoricalBlockAvailability.";
            probe(L, d + "CHRONOLOGY|-", () -> HistoricalBlockAvailability.CHRONOLOGY);
            probe(L, d + "size()|-", () -> HistoricalBlockAvailability.size());
            Map<String, HistoricalBlockAvailability.Boundary> all = new TreeMap<>(HistoricalBlockAvailability.all());
            for (var entry : all.entrySet()) {
                HistoricalBlockAvailability.Boundary b = entry.getValue();
                probe(L, d + "boundaryFor(" + entry.getKey() + ")|-",
                    () -> "version=" + b.versionId() + " timeline=" + b.timeline()
                        + " evidenceLen=" + b.evidence().length());
            }
            List<String> blocks = new ArrayList<>(all.keySet());
            blocks.add("minecraft:undated_block_for_snapshot");
            blocks.add(null);
            matrix(L, d + "judge", blocks, k -> String.valueOf(k), INPUTS, VersionBehaviourSnapshotTest::label,
                (block, id) -> HistoricalBlockAvailability.judge(id, block));
        }
        if (loadable(L, "net.historicalexperience.generation.HistoricalBlockPalette")) {
            String d = "generation.HistoricalBlockPalette.";
            perId(L, d + "paletteFor", id -> HistoricalBlockPalette.paletteFor(id));
            probe(L, d + "mappedBlockCount()|-", () -> HistoricalBlockPalette.mappedBlockCount());
            Map<String, String> mapping = new TreeMap<>(HistoricalBlockPalette.mapping());
            for (var entry : mapping.entrySet()) {
                probe(L, d + "legacyNameFor(" + entry.getKey() + ")|-",
                    () -> HistoricalBlockPalette.legacyNameFor(entry.getKey()));
            }
            Set<String> beta = HistoricalBlockPalette.paletteFor(P + "beta_1_7_3");
            if (beta != null) {
                for (String modern : mapping.keySet()) {
                    probe(L, d + "judge(beta_1_7_3," + modern + ")|" + P + "beta_1_7_3",
                        () -> HistoricalBlockPalette.judge(beta, modern));
                }
            }
        }
    }

    private static void craftingEra(List<String> L) {
        if (!loadable(L, "net.historicalexperience.gameplay.HistoricalCraftingEra")) {
            return;
        }
        String d = "gameplay.HistoricalCraftingEra.";
        probe(L, d + "colouredBedsChronology()|-", () -> HistoricalCraftingEra.colouredBedsChronology());
        probe(L, d + "constants()|-", () -> HistoricalCraftingEra.RED_BED + " " + HistoricalCraftingEra.GENERIC_PLANKS);
        TreeSet<String> items = new TreeSet<>(HistoricalCraftingEra.allowListed());
        for (String item : items) {
            probe(L, d + "firstVersion(" + item + ")|-", () -> HistoricalCraftingEra.firstVersion(item));
        }
        for (String colour : List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")) {
            items.add("minecraft:" + colour + "_bed");
        }
        items.add("minecraft:netherite_ingot");
        items.add("minecraft:stone");
        List<String> itemList = new ArrayList<>(items);
        matrix(L, d + "existedBy164", itemList, k -> k, List.of("-"), x -> x,
            (item, ignored) -> HistoricalCraftingEra.existedBy164(item));
        matrix(L, d + "isRefused(block=null)", itemList, k -> k, INPUTS, VersionBehaviourSnapshotTest::label,
            (item, id) -> HistoricalCraftingEra.isRefused(id, item, null));
        matrix(L, d + "isRefused(block=item)", itemList, k -> k, INPUTS, VersionBehaviourSnapshotTest::label,
            (item, id) -> HistoricalCraftingEra.isRefused(id, item, item));
        matrix(L, d + "substitutedResult", itemList, k -> k, INPUTS, VersionBehaviourSnapshotTest::label,
            (item, id) -> HistoricalCraftingEra.substitutedResult(id, item));
        matrix(L, d + "showsPlainBedName", itemList, k -> k, INPUTS, VersionBehaviourSnapshotTest::label,
            (item, id) -> HistoricalCraftingEra.showsPlainBedName(id, item));
        probe(L, d + "isRefused(item=null)|" + P + "beta_1_7_3",
            () -> HistoricalCraftingEra.isRefused(P + "beta_1_7_3", null, null));

        List<String> recipePaths = new ArrayList<>();
        recipePaths.addAll(resourceFirstColumn(L, "/crafting/crafting-recipes-1.21.1.txt"));
        recipePaths.addAll(resourceFirstColumn(L, "/crafting/special-recipes-1.21.1.txt"));
        recipePaths.add("not_a_real_recipe");
        recipePaths.add(null);
        matrix(L, d + "isRecipeRefused", recipePaths, k -> String.valueOf(k), INPUTS,
            VersionBehaviourSnapshotTest::label, (recipe, id) -> HistoricalCraftingEra.isRecipeRefused(id, recipe));
    }

    // ---- 6. loot / player / food / tool durability / spawn -----------------------------------------------

    private static void lootPlayerFoodToolSpawn(List<String> L) {
        if (loadable(L, "net.historicalexperience.gameplay.HistoricalLootRules")) {
            String d = "gameplay.HistoricalLootRules.";
            perId(L, d + "suppressesExperience", id -> HistoricalLootRules.suppressesExperience(id));
            perId(L, d + "usesLegacySheepDrops", id -> HistoricalLootRules.usesLegacySheepDrops(id));
            probe(L, d + "restrictedProfiles()|-", () -> HistoricalLootRules.restrictedProfiles());
            for (String entity : List.of("minecraft:zombie", "minecraft:chicken", "minecraft:spider", "minecraft:cow",
                "minecraft:creeper", "minecraft:skeleton", "minecraft:pig", "minecraft:sheep")) {
                perId(L, d + "forEntity(" + entity + ")",
                    id -> dropRule(HistoricalLootRules.forEntity(id, entity)));
            }
        }
        if (loadable(L, "net.historicalexperience.gameplay.HistoricalPlayerRules")) {
            String d = "gameplay.HistoricalPlayerRules.";
            perId(L, d + "allowsSprinting", id -> HistoricalPlayerRules.allowsSprinting(id));
            perId(L, d + "hasHunger", id -> HistoricalPlayerRules.hasHunger(id));
            perId(L, d + "hasStructures", id -> HistoricalPlayerRules.hasStructures(id));
            perId(L, d + "suppressesHungerTick", id -> HistoricalPlayerRules.suppressesHungerTick(id));
        }
        if (loadable(L, "net.historicalexperience.gameplay.HistoricalFoodRules")) {
            String d = "gameplay.HistoricalFoodRules.";
            perId(L, d + "healsFoodDirectly", id -> HistoricalFoodRules.healsFoodDirectly(id));
            perId(L, d + "allowsEatingWithoutHunger", id -> HistoricalFoodRules.allowsEatingWithoutHunger(id));
            perId(L, d + "hasHunger", id -> HistoricalFoodRules.hasHunger(id));
            perId(L, d + "healPoints", id -> HistoricalFoodRules.healPoints(id));
            probe(L, d + "healingProfiles()|-", () -> HistoricalFoodRules.healingProfiles());
            perId(L, d + "healPointsFor(minecraft:bread)", id -> HistoricalFoodRules.healPointsFor(id, "minecraft:bread"));
            perId(L, d + "healPointsFor(minecraft:golden_apple)",
                id -> HistoricalFoodRules.healPointsFor(id, "minecraft:golden_apple"));
        }
        if (loadable(L, "net.historicalexperience.gameplay.HistoricalToolDurability")) {
            String d = "gameplay.HistoricalToolDurability.";
            TreeSet<String> named = new TreeSet<>(HistoricalToolDurability.namedItems());
            probe(L, d + "namedItems()|-", () -> named);
            List<String> items = new ArrayList<>(named);
            items.add("minecraft:golden_sword");
            items.add("minecraft:netherite_sword");
            items.add(null);
            matrix(L, d + "durabilityFor", items, k -> String.valueOf(k), INPUTS, VersionBehaviourSnapshotTest::label,
                (item, id) -> HistoricalToolDurability.durabilityFor(id, item));
        }
        if (loadable(L, "net.historicalexperience.gameplay.HistoricalSpawnRules")) {
            String d = "gameplay.HistoricalSpawnRules.";
            perId(L, d + "forProfile", id -> {
                Optional<HistoricalSpawnRules.SpawnRule> rule = HistoricalSpawnRules.forProfile(id);
                return rule.isEmpty() ? "unrestricted"
                    : "profile=" + rule.get().profileId() + " allowed=" + render(rule.get().allowedEntityTypes())
                        + " evidenceLen=" + rule.get().evidence().length();
            });
            perId(L, d + "netherEndermenAllowed", id -> HistoricalSpawnRules.netherEndermenAllowed(id));
            probe(L, d + "restrictedProfiles()|-", () -> HistoricalSpawnRules.restrictedProfiles());
            probe(L, d + "NETHER_ENDERMAN_CHRONOLOGY|-", () -> HistoricalSpawnRules.NETHER_ENDERMAN_CHRONOLOGY);
            probe(L, d + "beta173MobsInOrder()|-", () -> HistoricalSpawnRules.beta173MobsInOrder());
            for (String alias : HistoricalVersions.allRevisionAliases()) {
                probe(L, d + "profileIdForAlias(" + alias + ")|-", () -> HistoricalSpawnRules.profileIdForAlias(alias));
            }
        }
    }

    // ---- 7. structures / villages ------------------------------------------------------------------------

    private static void structuresAndVillages(List<String> L) {
        if (loadable(L, "net.historicalexperience.generation.HistoricalStructureRules")) {
            String d = "generation.HistoricalStructureRules.";
            perId(L, d + "ruleForVersion", id -> {
                Optional<HistoricalStructureRules.StructureRule> rule = HistoricalStructureRules.ruleForVersion(id);
                return rule.isEmpty() ? "none"
                    : "restricts=" + rule.get().restricts() + " allowed=" + render(rule.get().allowedStructureIds())
                        + " evidenceLen=" + rule.get().evidence().length();
            });
            perId(L, d + "allowedForVersion", id -> HistoricalStructureRules.allowedForVersion(id));
            probe(L, d + "declaredVersions()|-", () -> HistoricalStructureRules.declaredVersions());
            probe(L, d + "VANILLA_STRUCTURE_IDS|-", () -> HistoricalStructureRules.VANILLA_STRUCTURE_IDS);
            List<String> aliasInputs = concat(HistoricalVersions.allRevisionAliases(), INPUTS);
            matrix(L, d + "suppresses", new ArrayList<>(new TreeSet<>(HistoricalStructureRules.VANILLA_STRUCTURE_IDS)),
                k -> k, aliasInputs, VersionBehaviourSnapshotTest::label,
                (structure, alias) -> HistoricalStructureRules.suppresses(alias, structure));
        }
        if (loadable(L, "net.historicalexperience.worldgen.HistoricalVillageRules")) {
            String d = "worldgen.HistoricalVillageRules.";
            perId(L, d + "generationEnabledForVersion", id -> HistoricalVillageRules.generationEnabledForVersion(id));
            for (String alias : concat(HistoricalVersions.allRevisionAliases(), INPUTS)) {
                probe(L, d + "generationEnabledForAlias|" + label(alias),
                    () -> HistoricalVillageRules.generationEnabledForAlias(alias));
                probe(L, d + "suppressVanillaVillagesForAlias|" + label(alias),
                    () -> HistoricalVillageRules.suppressVanillaVillagesForAlias(alias));
            }
            probe(L, d + "villageVersions()|-", () -> HistoricalVillageRules.villageVersions());
            probe(L, d + "noVillageVersions()|-", () -> HistoricalVillageRules.noVillageVersions());
            probe(L, d + "permittedBiomes()|-", () -> HistoricalVillageRules.permittedBiomes());
            probe(L, d + "vanillaVillageStructures()|-",
                () -> new TreeMap<>(HistoricalVillageRules.vanillaVillageStructures()).keySet());
            for (String biome : List.of("minecraft:plains", "minecraft:desert", "minecraft:savanna",
                "minecraft:taiga", "minecraft:snowy_plains", "minecraft:desert_lakes", "minecraft:forest")) {
                probe(L, d + "biomeIsPermitted(" + biome + ")|-", () -> HistoricalVillageRules.biomeIsPermitted(biome));
                probe(L, d + "isDesert(" + biome + ")|-", () -> HistoricalVillageRules.isDesert(biome));
            }
        }
        if (loadable(L, "net.historicalexperience.worldgen.HistoricalVillageBlocks")) {
            String d = "worldgen.HistoricalVillageBlocks.";
            perId(L, d + "usesDirtPath", id -> HistoricalVillageBlocks.usesDirtPath(id));
            perId(L, d + "forVersion.blocks", id -> HistoricalVillageBlocks.forVersion(id).blocks());
            perId(L, d + "placedBlockIds", id -> HistoricalVillageBlocks.placedBlockIds(id));
            perId(L, d + "desertSubstitutions",
                id -> HistoricalVillageBlocks.desertSubstitutions(HistoricalVillageBlocks.forVersion(id)));
            probe(L, d + "placedBlockIds()|-", () -> HistoricalVillageBlocks.placedBlockIds());
            probe(L, d + "versionIndependentBlockIds()|-", () -> HistoricalVillageBlocks.versionIndependentBlockIds());
            probe(L, d + "forbiddenBlocks()|-", () -> HistoricalVillageBlocks.forbiddenBlocks());
            probe(L, d + "alwaysPlacedIds()|-", () -> HistoricalVillageBlocks.alwaysPlacedIds());
        }
    }

    // ---- 8. client rules ---------------------------------------------------------------------------------

    private static void clientRules(List<String> L) {
        if (loadable(L, "net.historicalexperience.client.textures.HistoricalTextureRules")) {
            perVersion(L, "client.textures.HistoricalTextureRules.planFor.signature",
                v -> HistoricalTextureRules.planFor(v).signature());
        }
        if (loadable(L, "net.historicalexperience.client.colour.HistoricalColormaps")) {
            perVersion(L, "client.colour.HistoricalColormaps.eraSignatureFor",
                v -> v == null ? "n/a(null version)" : HistoricalColormaps.eraSignatureFor(v.chronology()));
        }
        if (loadable(L, "net.historicalexperience.client.tooltip.HistoricalTooltips")) {
            String d = "client.tooltip.HistoricalTooltips.";
            perVersion(L, d + "eraFor", v -> v == null ? "n/a(null version)" : HistoricalTooltips.eraFor(v.chronology()));
            perVersion(L, d + "showsAttributeTooltip", v -> HistoricalTooltips.showsAttributeTooltip(v));
        }
        if (loadable(L, "net.historicalexperience.client.hud.HistoricalVersionLabel")) {
            String d = "client.hud.HistoricalVersionLabel.";
            perVersion(L, d + "shownIn", v -> HistoricalVersionLabel.shownIn(v));
            perVersion(L, d + "textFor", v -> v == null ? "n/a(null version)" : HistoricalVersionLabel.textFor(v));
        }
        if (loadable(L, "net.historicalexperience.client.lighting.HistoricalStars")) {
            perVersion(L, "client.lighting.HistoricalStars.styleFor", v -> HistoricalStars.styleFor(v));
        }
        if (loadable(L, "net.historicalexperience.client.lighting.HistoricalSmoothLighting")) {
            perVersion(L, "client.lighting.HistoricalSmoothLighting.availableFor",
                v -> HistoricalSmoothLighting.availableFor(v));
        }
        if (loadable(L, "net.historicalexperience.client.lighting.HistoricalOldLighting")) {
            perVersion(L, "client.lighting.HistoricalOldLighting.availableFor",
                v -> HistoricalOldLighting.availableFor(v));
        }
        if (loadable(L, "net.historicalexperience.client.lighting.HistoricalLightingTweaks")) {
            perVersion(L, "client.lighting.HistoricalLightingTweaks.availableFor",
                v -> HistoricalLightingTweaks.availableFor(v));
        }
        if (loadable(L, "net.historicalexperience.client.lighting.HistoricalChestTweaks")) {
            String d = "client.lighting.HistoricalChestTweaks.";
            perVersion(L, d + "availableForChest", v -> HistoricalChestTweaks.availableForChest(v));
            perVersion(L, d + "availableForEnderChest", v -> HistoricalChestTweaks.availableForEnderChest(v));
            // wantsOld* also read a persisted client option (default true); in plain JUnit no config file exists.
            perVersion(L, d + "wantsOldChest", v -> HistoricalChestTweaks.wantsOldChest(v));
            perVersion(L, d + "wantsOldEnderChest", v -> HistoricalChestTweaks.wantsOldEnderChest(v));
        }
        if (loadable(L, "net.historicalexperience.client.lighting.HistoricalFogTweaks")) {
            String d = "client.lighting.HistoricalFogTweaks.";
            perVersion(L, d + "beforeBeta18", v -> HistoricalFogTweaks.beforeBeta18(v));
            for (int distance = 2; distance <= 32; distance++) {
                int rd = distance;
                probe(L, d + "farPlane|rd=" + rd, () -> HistoricalFogTweaks.farPlane(rd));
                probe(L, d + "terrainEnd|rd=" + rd, () -> HistoricalFogTweaks.terrainEnd(rd));
                probe(L, d + "skyEnd|rd=" + rd, () -> HistoricalFogTweaks.skyEnd(rd));
            }
        }
        if (loadable(L, "net.historicalexperience.client.lighting.HistoricalWorldTweaks")) {
            String d = "client.lighting.HistoricalWorldTweaks.";
            perVersion(L, d + "availableForCloudHeight", v -> HistoricalWorldTweaks.availableForCloudHeight(v));
            perVersion(L, d + "cloudHeightForVersion(128f)", v -> HistoricalWorldTweaks.cloudHeightForVersion(128f, v));
            perVersion(L, d + "cloudHeightForVersion(192f)", v -> HistoricalWorldTweaks.cloudHeightForVersion(192f, v));
            perVersion(L, d + "cloudHeightForVersion(NaN)",
                v -> HistoricalWorldTweaks.cloudHeightForVersion(Float.NaN, v));
            perVersion(L, d + "availableFor2dItems", v -> HistoricalWorldTweaks.availableFor2dItems(v));
            perVersion(L, d + "availableForRecipeBook", v -> HistoricalWorldTweaks.availableForRecipeBook(v));
            perVersion(L, d + "shouldHideRecipeBookButton(true)",
                v -> HistoricalWorldTweaks.shouldHideRecipeBookButton(v, true));
            perVersion(L, d + "shouldHideRecipeBookButton(false)",
                v -> HistoricalWorldTweaks.shouldHideRecipeBookButton(v, false));
            perVersion(L, d + "skyEra", v -> HistoricalWorldTweaks.skyEra(v));
            perVersion(L, d + "eraSkyColour", v -> HistoricalWorldTweaks.eraSkyColour(v));
            // skyTweakActive also reads a persisted client option (default true).
            perVersion(L, d + "skyTweakActive", v -> HistoricalWorldTweaks.skyTweakActive(v));
            perVersion(L, d + "availableForSkyColour", v -> HistoricalWorldTweaks.availableForSkyColour(v));
        }
    }

    // ---- 9. world.upgrade.BiomeCarryForward --------------------------------------------------------------

    private static void biomeCarryForward(List<String> L) {
        if (!loadable(L, "net.historicalexperience.world.upgrade.BiomeCarryForward")) {
            return;
        }
        String d = "world.upgrade.BiomeCarryForward.";
        perVersion(L, d + "generationFor", v -> BiomeCarryForward.generationFor(v));
        perVersion(L, d + "recordLabelFor", v -> BiomeCarryForward.recordLabelFor(v));
        probe(L, d + "recordLabel()|-", () -> BiomeCarryForward.recordLabel());
        probe(L, d + "placeholders()|-", () -> BiomeCarryForward.placeholders());
        probe(L, d + "constants()|-", () -> BiomeCarryForward.BETA_PLAINS + " " + BiomeCarryForward.LATE_BETA_PLAINS
            + " " + BiomeCarryForward.ALPHA_BIOME);
        for (String label : Arrays.asList("biome_hierarchy_v4:alpha_only", "biome_hierarchy_v4:beta_plains_only",
            "biome_hierarchy_v4:late_beta", "biome_hierarchy_v4:early_release", "other", null)) {
            probe(L, d + "singleBiomeForLabel(" + label + ")|-", () -> BiomeCarryForward.singleBiomeForLabel(label));
        }
        List<String> stored = Arrays.asList(null, "moderner_beta:alpha", "moderner_beta:beta_plains",
            "moderner_beta:late_beta_plains", "moderner_beta:beta_desert", "moderner_beta:beta_forest",
            "moderner_beta:beta_oak_forest", "moderner_beta:beta_seasonal_forest", "moderner_beta:beta_rainforest",
            "moderner_beta:beta_shrubland", "moderner_beta:beta_savanna", "moderner_beta:beta_swampland",
            "moderner_beta:beta_taiga", "moderner_beta:beta_oak_taiga", "moderner_beta:beta_tundra",
            "moderner_beta:beta_ice_desert", "moderner_beta:beta_extreme_hills", "moderner_beta:beta_ocean",
            "moderner_beta:beta_cold_ocean", "moderner_beta:beta_frozen_ocean", "moderner_beta:infdev_227",
            "moderner_beta:infdev_420", "moderner_beta:infdev_611", "moderner_beta:late_beta_taiga",
            "moderner_beta:late_beta_extreme_hills", "moderner_beta:early_release_swampland",
            "moderner_beta:early_release_ice_plains", "minecraft:plains", "example:not_a_biome");
        matrix(L, d + "destinationFor", stored, k -> String.valueOf(k), VERSION_INPUTS,
            VersionBehaviourSnapshotTest::label, (biome, v) -> BiomeCarryForward.destinationFor(biome, v));
    }

    // ---- 10. version.* registry --------------------------------------------------------------------------

    private static void versionRegistry(List<String> L) {
        String d = "version.";
        probe(L, d + "HistoricalVersions.allRevisionAliases()|-", () -> HistoricalVersions.allRevisionAliases());
        probe(L, d + "HistoricalVersions.eras()|-", () -> HistoricalVersions.eras());
        probe(L, d + "HistoricalVersions.finiteProfiles()|-", () -> HistoricalVersions.finiteProfiles());
        for (HistoricalVersion v : VERSIONS) {
            String id = v.id();
            probe(L, d + "HistoricalVersion.displayName|" + id, () -> v.displayName());
            probe(L, d + "HistoricalVersion.era|" + id, () -> v.era());
            probe(L, d + "HistoricalVersion.chronology|" + id, () -> v.chronology());
            var r = v.rules();
            probe(L, d + "VersionRules.restrictsSpawns|" + id, () -> r.restrictsSpawns());
            probe(L, d + "VersionRules.allowedNaturalSpawns|" + id, () -> r.allowedNaturalSpawns());
            probe(L, d + "VersionRules.mobDrops|" + id, () -> {
                Map<String, String> drops = new TreeMap<>();
                r.mobDrops().forEach((entity, rule) -> drops.put(entity, rule.entityTypeId() + "->" + rule.itemId()
                    + " bound=" + rule.maxExclusiveCount() + " evidenceLen=" + rule.evidence().length()));
                return drops;
            });
            probe(L, d + "VersionRules.replacesLoot|" + id, () -> r.replacesLoot());
            probe(L, d + "VersionRules.foodHealPoints|" + id, () -> r.foodHealPoints());
            probe(L, d + "VersionRules.awardsExperience|" + id, () -> r.awardsExperience());
            probe(L, d + "VersionRules.allowsSprinting|" + id, () -> r.allowsSprinting());
            probe(L, d + "VersionRules.hasHunger|" + id, () -> r.hasHunger());
            probe(L, d + "VersionRules.hasStructures|" + id, () -> r.hasStructures());
            probe(L, d + "VersionRules.evidence.length|" + id, () -> r.evidence().length());
            probe(L, d + "HistoricalVersion.latestRevision.revision|" + id, () -> v.latestRevision().revision());
            probe(L, d + "HistoricalVersion.revisionAliases|" + id, () -> v.revisionAliases());
            for (int i = 0; i < v.revisions().size(); i++) {
                var rev = v.revisions().get(i);
                probe(L, d + "GenerationRevision[" + i + "]|" + id, () -> "revision=" + rev.revision()
                    + " alias=" + rev.alias() + " foundation=" + rev.foundation()
                    + " experimental=" + rev.experimental() + " legacy=" + rev.legacy()
                    + " placesOwnOres=" + rev.placesOwnOres()
                    + " excludedPlacedFeatures.size=" + rev.excludedPlacedFeatures().size()
                    + " excluded=" + rev.excludedPlacedFeatures().stream().map(e -> e.featureId()).sorted().toList()
                    + " orePopulation.size=" + rev.orePopulation().size()
                    + " ores=" + rev.orePopulation().stream()
                        .map(o -> o.name() + ":" + o.blockId() + ":" + o.size() + ":" + o.attempts() + ":"
                            + o.originBoundA() + ":" + o.originBoundB())
                        .toList()
                    + " description.length=" + rev.description().length());
                probe(L, d + "HistoricalVersions.byRevisionAlias(" + rev.alias() + ")|-",
                    () -> HistoricalVersions.byRevisionAlias(rev.alias()).map(HistoricalVersion::id));
                probe(L, d + "HistoricalVersions.isFinitePreset(" + rev.alias() + ")|-",
                    () -> HistoricalVersions.isFinitePreset(rev.alias()));
            }
            probe(L, d + "HistoricalVersions.isFiniteWorld|" + id, () -> HistoricalVersions.isFiniteWorld(id));
            probe(L, d + "HistoricalVersions.eraHasRedstoneOre|" + id, () -> HistoricalVersions.eraHasRedstoneOre(v));
            probe(L, d + "HistoricalVersions.eraHasLapisOre|" + id, () -> HistoricalVersions.eraHasLapisOre(v));
            probe(L, d + "HistoricalVersions.eraHasEmeraldOre|" + id, () -> HistoricalVersions.eraHasEmeraldOre(v));
            probe(L, d + "HistoricalVersions.eraOrePopulation.size|" + id,
                () -> HistoricalVersions.eraOrePopulation(v).size());
            probe(L, d + "HistoricalVersions.eraOrePopulation.names|" + id,
                () -> HistoricalVersions.eraOrePopulation(v).stream().map(o -> o.name()).toList());
        }
        for (String alias : INPUTS) {
            probe(L, d + "HistoricalVersions.byRevisionAlias|" + label(alias),
                () -> HistoricalVersions.byRevisionAlias(alias).map(HistoricalVersion::id));
            probe(L, d + "HistoricalVersions.byId|" + label(alias),
                () -> HistoricalVersions.byId(alias).map(HistoricalVersion::id));
            probe(L, d + "HistoricalVersions.isFiniteWorld|" + label(alias), () -> HistoricalVersions.isFiniteWorld(alias));
        }
        for (HistoricalVersion v : VERSIONS) {
            probe(L, d + "HistoricalVersions.atOrAfter(chronology).size|" + v.id(),
                () -> HistoricalVersions.atOrAfter(v.chronology()).size());
            probe(L, d + "HistoricalVersions.before(chronology).size|" + v.id(),
                () -> HistoricalVersions.before(v.chronology()).size());
        }

        if (loadable(L, "net.historicalexperience.version.HistoricalVersionCatalogue")) {
            String c = "version.HistoricalVersionCatalogue.";
            probe(L, c + "eras()|-", () -> HistoricalVersionCatalogue.eras());
            probe(L, c + "entries().size|-", () -> HistoricalVersionCatalogue.entries().size());
            probe(L, c + "implemented().size|-", () -> HistoricalVersionCatalogue.implemented().size());
            probe(L, c + "plannedTargets().size|-", () -> HistoricalVersionCatalogue.plannedTargets().size());
            probe(L, c + "implementedExtras().size|-", () -> HistoricalVersionCatalogue.implementedExtras().size());
            probe(L, c + "future().size|-", () -> HistoricalVersionCatalogue.future().size());
            int index = 0;
            for (HistoricalVersionCatalogue.Entry e : HistoricalVersionCatalogue.entries()) {
                probe(L, c + "entry[" + index++ + "]|" + e.key(), () -> "targetName=" + e.targetName()
                    + " era=" + e.era() + " order=" + e.order() + " status=" + e.status()
                    + " emulated=" + e.emulated() + " target=" + e.target() + " selectable=" + e.selectable()
                    + " futureNote.length=" + e.futureNote().length());
            }
            for (HistoricalVersionCatalogue.Era era : HistoricalVersionCatalogue.eras()) {
                probe(L, c + "entriesIn(" + era + ")|-", () -> HistoricalVersionCatalogue.entriesIn(era).stream()
                    .map(HistoricalVersionCatalogue.Entry::key).toList());
            }
        }
        if (loadable(L, "net.historicalexperience.version.HistoricalSnapshotPatches")) {
            for (var patch : HistoricalSnapshotPatches.all()) {
                probe(L, d + "HistoricalSnapshotPatches.patch|" + patch.id(), () -> "displayName=" + patch.displayName()
                    + " parentRelease=" + patch.parentRelease() + " base=" + patch.baseProfileId()
                    + " date=" + patch.releaseDate() + " features="
                    + patch.features().stream().map(Enum::name).sorted().toList());
            }
        }
    }

    // =========================================================================================================
    // Infrastructure
    // =========================================================================================================

    /** Records one line; a throwing supplier is part of the snapshot, not a failure. {@code name} includes {@code |key}. */
    private static void probe(List<String> lines, String name, Supplier<?> body) {
        String result;
        try {
            result = render(body.get());
        } catch (Throwable failure) {
            result = "THROWS " + failure.getClass().getName();
        }
        lines.add(name + "=" + result);
    }

    /** One line per input id (declared versions, then the ordinary inputs). */
    private static void perId(List<String> lines, String name, Function<String, Object> body) {
        for (String id : INPUTS) {
            probe(lines, name + "|" + label(id), () -> body.apply(id));
        }
    }

    /** One line per declared version, then null. */
    private static void perVersion(List<String> lines, String name, Function<HistoricalVersion, Object> body) {
        for (HistoricalVersion version : VERSION_INPUTS) {
            probe(lines, name + "|" + (version == null ? "null" : version.id()), () -> body.apply(version));
        }
    }

    /**
     * One line per key, one token per input. The inputs line names the order, so the file stays readable and a
     * changed token is still attributable to a (key, input) pair.
     */
    private static <K, I> void matrix(List<String> lines, String name, List<K> keys, Function<K, String> keyLabel,
        List<I> inputs, Function<I, String> inputLabel, BiFunction<K, I, Object> body) {
        lines.add(name + ".inputs|*=" + inputs.stream().map(inputLabel).collect(Collectors.joining(" ")));
        for (K key : keys) {
            List<String> tokens = new ArrayList<>(inputs.size());
            for (I input : inputs) {
                String token;
                try {
                    token = token(body.apply(key, input));
                } catch (Throwable failure) {
                    token = "THROWS(" + failure.getClass().getSimpleName() + ")";
                }
                tokens.add(token);
            }
            boolean single = tokens.stream().allMatch(t -> t.length() == 1);
            lines.add(name + "(" + keyLabel.apply(key) + ")|*=" + String.join(single ? "" : ",", tokens));
        }
    }

    private static String token(Object value) {
        if (value instanceof Boolean flag) {
            return flag ? "T" : "F";
        }
        if (value instanceof Enum<?> constant) {
            String n = constant.name();
            return n.length() > 3 ? n.substring(0, 3) : n;
        }
        if (value == null) {
            return "-";
        }
        return render(value);
    }

    /** Records an UNLOADABLE line once and returns false if the class cannot be initialised in plain JUnit. */
    private static boolean loadable(List<String> lines, String className) {
        try {
            Class.forName(className, true, VersionBehaviourSnapshotTest.class.getClassLoader());
            return true;
        } catch (Throwable failure) {
            String line = "UNLOADABLE " + className;
            if (!lines.contains(line)) {
                lines.add(line);
            }
            return false;
        }
    }

    private static List<String> resourceFirstColumn(List<String> lines, String resource) {
        List<String> out = new ArrayList<>();
        try (InputStream in = VersionBehaviourSnapshotTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                lines.add("meta.resource(" + resource + ")|*=MISSING");
                return out;
            }
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (!line.isBlank()) {
                    out.add(line.trim().split(" ")[0]);
                }
            }
        } catch (IOException failure) {
            lines.add("meta.resource(" + resource + ")|*=THROWS " + failure.getClass().getName());
        }
        return out;
    }

    private static String dropRule(Optional<DropRule> rule) {
        return rule.isEmpty() ? "none" : rule.get().entityTypeId() + "->" + rule.get().itemId()
            + " bound=" + rule.get().maxExclusiveCount() + " evidenceLen=" + rule.get().evidence().length();
    }

    private static List<String> selections() {
        List<String> selections = new ArrayList<>(INPUTS);
        for (HistoricalSnapshotPatches.Patch patch : HistoricalSnapshotPatches.all()) {
            selections.add(patch.id());
        }
        return selections;
    }

    private static String label(String id) {
        return id == null ? "null" : id;
    }

    private static String label(HistoricalVersion version) {
        return version == null ? "null" : version.id();
    }

    private static <T> List<T> concat(List<T> first, List<T> second) {
        List<T> out = new ArrayList<>(first);
        out.addAll(second);
        return Collections.unmodifiableList(out);
    }

    private static <T> List<T> withNull(List<T> values) {
        List<T> out = new ArrayList<>(values);
        out.add(null);
        return Collections.unmodifiableList(out);
    }

    /** Deterministic rendering: sets and maps sorted, lists in their own (significant) order. */
    private static String render(Object value) {
        String text;
        if (value == null) {
            text = "null";
        } else if (value instanceof Optional<?> optional) {
            text = optional.isEmpty() ? "Optional.empty" : "Optional[" + render(optional.get()) + "]";
        } else if (value instanceof Set<?> set) {
            text = "{" + set.stream().map(VersionBehaviourSnapshotTest::render).sorted()
                .collect(Collectors.joining(",")) + "}";
        } else if (value instanceof Map<?, ?> map) {
            text = "{" + map.entrySet().stream()
                .map(e -> render(e.getKey()) + "=" + render(e.getValue())).sorted()
                .collect(Collectors.joining(",")) + "}";
        } else if (value instanceof Collection<?> collection) {
            text = "[" + collection.stream().map(VersionBehaviourSnapshotTest::render)
                .collect(Collectors.joining(",")) + "]";
        } else {
            text = String.valueOf(value);
        }
        return text.replace("\r", "\\r").replace("\n", "\\n");
    }
}
