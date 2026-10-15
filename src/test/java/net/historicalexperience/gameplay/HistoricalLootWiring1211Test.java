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
package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The drop tables are actually wired up, and every mixin that exists is registered.
 *
 * <h2>Why a wiring test and not just a data test</h2>
 *
 * <p>The reviewed per-version drop tables sat in {@code HistoricalLootRules} as <b>dead code</b>: a
 * {@code forEntity} method nothing called, so a Beta 1.7.3 zombie dropped rotten flesh and no test noticed,
 * because every test asked the table a question rather than asking whether anything read it.
 *
 * <p>So this test checks the two structural facts that failure needed: that the mixin which applies the
 * tables is registered, and - more generally - that the set of mixin classes on disk is <b>exactly</b> the
 * set the mixin configuration names. A behaviour file with no registration is the shape of the bug, and it
 * now fails here.
 */
class HistoricalLootWiring1211Test {
    private static final Path REPO = Path.of("").toAbsolutePath();
    private static final Path MIXIN_DIR = REPO.resolve("src/main/java/net/historicalexperience/mixin");
    private static final Path CONFIG = REPO.resolve("src/main/resources/historical_experience.mixins.json");

    private static List<String> profiles() {
        return HistoricalVersions.all().stream()
            .sorted(java.util.Comparator.comparingInt(HistoricalVersion::chronology))
            .map(HistoricalVersion::id)
            .toList();
    }

    @Test
    @DisplayName("every mixin class on disk is registered, and every registered mixin exists")
    void mixinRegistrationMatchesTheSources() throws Exception {
        JsonObject config = JsonParser.parseString(Files.readString(CONFIG)).getAsJsonObject();
        Set<String> registered = new LinkedHashSet<>();
        // Both arrays. Client-only mixins are registered under "client" so a dedicated server never loads
        // them; checking only the shared list made this test report a correctly-registered mixin as missing.
        config.getAsJsonArray("mixins").forEach(element -> registered.add(element.getAsString()));
        if (config.has("client")) {
            config.getAsJsonArray("client").forEach(element -> registered.add(element.getAsString()));
        }
        if (config.has("server")) {
            config.getAsJsonArray("server").forEach(element -> registered.add(element.getAsString()));
        }

        Set<String> onDisk;
        try (var files = Files.list(MIXIN_DIR)) {
            onDisk = files.map(path -> path.getFileName().toString())
                .filter(name -> name.endsWith(".java"))
                .map(name -> name.substring(0, name.length() - ".java".length()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        Set<String> unregistered = new LinkedHashSet<>(onDisk);
        unregistered.removeAll(registered);
        assertTrue(unregistered.isEmpty(), "these mixin classes exist but are never registered, so their "
            + "behaviour does not happen: " + unregistered);

        Set<String> missing = new LinkedHashSet<>(registered);
        missing.removeAll(onDisk);
        assertTrue(missing.isEmpty(), "the mixin configuration names classes that do not exist, which makes "
            + "the game refuse to start: " + missing);

        // The loot mixin specifically: it is the one this test was written for.
        assertTrue(registered.contains("LivingEntityHistoricalLootMixin"),
            "the mob-drop tables are dead code again unless this mixin is registered");
    }

    @Test
    @DisplayName("the drop tables apply to the versions that had their own drops, and to no others")
    void dropTablesApplyToTheExpectedProfiles() {
        Set<String> replaces = HistoricalLootRules.restrictedProfiles();
        assertTrue(replaces.contains("historical_experience:beta_1_7_3"),
            "Beta 1.7.3's drops are verified from its own client and must be applied");
        assertTrue(replaces.contains("historical_experience:beta_1_8_1"),
            "Beta 1.8.1's drops are the Adventure Update's replacements");
        assertFalse(replaces.contains("historical_experience:release_1_6_4"),
            "Release 1.6.4's drops were never researched, so it must keep the modern ones rather than "
                + "borrow a neighbouring version's");

        // The measured defect in one line: a Beta 1.7.3 zombie drops a feather, not rotten flesh, and the
        // count bound is 3 because Beta used nextInt(3).
        var betaZombie = HistoricalLootRules
            .forEntity("historical_experience:beta_1_7_3", "minecraft:zombie").orElseThrow();
        assertEquals("minecraft:feather", betaZombie.itemId());
        assertEquals(3, betaZombie.maxExclusiveCount());
        assertFalse(betaZombie.evidence().isBlank(), "a rule must say where its value came from");

        // Every mob the tables name is dated, and every rule carries evidence.
        for (String profile : replaces) {
            for (HistoricalLootRules.DropRule rule : HistoricalLootRules.BETA_1_7_3) {
                var found = HistoricalLootRules.forEntity(profile, rule.entityTypeId());
                assertTrue(found.isPresent(), profile + " must have a rule for " + rule.entityTypeId());
                assertFalse(found.get().evidence().isBlank(), "no evidence for " + rule.entityTypeId());
            }
        }

        // A version that does not replace loot answers empty, so the vanilla path runs untouched.
        for (String profile : profiles()) {
            if (replaces.contains(profile)) {
                continue;
            }
            assertTrue(HistoricalLootRules.forEntity(profile, "minecraft:zombie").isEmpty(),
                profile + " must not replace drops it never had researched");
        }
    }

    @Test
    @DisplayName("experience is withheld exactly where the version awarded none")
    void experienceIsSuppressedOnlyWhereTheVersionAwardedNone() {
        // Beta 1.7.3 predates experience, which arrives in Beta 1.8.1; both are verified from their clients.
        assertTrue(HistoricalLootRules.suppressesExperience("historical_experience:beta_1_7_3"),
            "Beta 1.7.3 had no experience orbs at all");
        assertTrue(HistoricalLootRules.suppressesExperience("historical_experience:alpha"));
        assertFalse(HistoricalLootRules.suppressesExperience("historical_experience:beta_1_8_1"),
            "Beta 1.8.1 introduced experience");
        assertFalse(HistoricalLootRules.suppressesExperience("historical_experience:release_1_6_4"));
        assertFalse(HistoricalLootRules.suppressesExperience("moderner_beta:beta"),
            "an ordinary world must keep its experience");
        assertFalse(HistoricalLootRules.suppressesExperience(null));
    }

    @Test
    @DisplayName("the experience level number is withheld between the bar and the number")
    void experienceLevelNumberHasItsOwnBoundary() {
        // Owner: "The experience point number was only added in a later version ... you couldnt see how much
        // experience you had in beta 1.8 or pre 1". minecraft.wiki, Java Edition Beta 1.9-pre3, section Experience
        // levels: "Now shown above experience bar." The bar is Beta 1.8, so the number is its own boundary: a Beta
        // 1.8.1 world has the bar WITHOUT the number, which is what this test exists to protect.
        //
        // The boundary is Prerelease 3, corrected by the owner after this rule was first written against the
        // Experience article's History table, which files the same sentence under Prerelease 4. See the long note on
        // HistoricalGameplayTweaks.experienceLevelShown.
        assertFalse(HistoricalGameplayTweaks.experienceLevelShown("historical_experience:beta_1_8_1"),
            "Beta 1.8.1 drew the bar and no number");
        assertFalse(HistoricalGameplayTweaks.experienceLevelShown("historical_experience:beta_1_9_pre_1"),
            "Prerelease 1 is the build the owner was playing when they reported this");
        assertFalse(HistoricalGameplayTweaks.experienceLevelShown("historical_experience:beta_1_9_pre_2"));
        assertTrue(HistoricalGameplayTweaks.experienceLevelShown("historical_experience:beta_1_9_pre_3"),
            "Prerelease 3 is where the version page and the owner both put the number");
        assertTrue(HistoricalGameplayTweaks.experienceLevelShown("historical_experience:beta_1_9_pre_4"));
        assertTrue(HistoricalGameplayTweaks.experienceLevelShown("historical_experience:beta_1_9_pre_5"));
        assertTrue(HistoricalGameplayTweaks.experienceLevelShown("historical_experience:release_1_0_0"));
        assertTrue(HistoricalGameplayTweaks.experienceLevelShown("historical_experience:release_1_6_4"));

        // Positive predicate, so the modern answer for a world with no historical rules - see the convention on
        // HistoricalGameplayTweaks.functionalEnchanting. Getting this backwards is what switched four features off
        // in ordinary worlds.
        assertTrue(HistoricalGameplayTweaks.experienceLevelShown(null),
            "an ordinary world must keep the number");
        assertTrue(HistoricalGameplayTweaks.experienceLevelShown("moderner_beta:beta"));
        assertTrue(HistoricalGameplayTweaks.experienceLevelShown("historical_experience:no_such_version"));

        // And the two boundaries are independent: the versions that had no experience at all have neither.
        for (String version : new String[] {"historical_experience:beta_1_7_3", "historical_experience:alpha"}) {
            assertTrue(HistoricalLootRules.suppressesExperience(version));
            assertFalse(HistoricalGameplayTweaks.experienceLevelShown(version),
                version + " had no bar and no number");
        }
    }
}
