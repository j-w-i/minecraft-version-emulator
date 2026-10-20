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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. Pins the timeline scale: the exact chronology key of every declared version, and that each named
 * boundary is on that scale.
 *
 * <p>The keys are compared by rules all over the project and the version-keyed answers depend on their exact values,
 * including the irregular ones, so the full table is written out here as an oracle independent of how a key is
 * computed. A refactor that changes how keys are built must still produce these numbers.
 */
class MilestonesTest {
    /** Every declared version and its key, taken from the source declarations. Oldest era first. */
    private static final Map<String, Integer> ORACLE = new LinkedHashMap<>();

    static {
        ORACLE.put("indev_latest", 200_223);
        ORACLE.put("infdev_420", 300_420);
        ORACLE.put("infdev_611", 300_611);
        ORACLE.put("alpha_1_0_1", 401_001);
        ORACLE.put("alpha_1_0_4", 401_004);
        ORACLE.put("alpha_1_0_6", 401_006);
        ORACLE.put("alpha_1_0_11", 401_011);
        ORACLE.put("alpha_1_0_14", 401_014);
        ORACLE.put("alpha_1_0_17", 401_017);
        ORACLE.put("alpha_1_1_0", 401_100);
        ORACLE.put("alpha", 401_102);
        ORACLE.put("alpha_1_2_0", 401_200);
        ORACLE.put("alpha_1_2_2a", 401_202);
        ORACLE.put("beta_1_0", 501_000);
        ORACLE.put("beta_1_2", 501_200);
        ORACLE.put("beta_1_3", 501_300);
        ORACLE.put("beta_1_4", 501_400);
        ORACLE.put("beta_1_5", 501_500);
        ORACLE.put("beta_1_6", 501_600);
        ORACLE.put("beta_1_7_3", 501_703);
        ORACLE.put("beta_1_8_1", 501_801);
        ORACLE.put("beta_1_9_pre_1", 501_901);
        ORACLE.put("beta_1_9_pre_2", 501_902);
        ORACLE.put("beta_1_9_pre_3", 501_903);
        ORACLE.put("beta_1_9_pre_4", 501_904);
        ORACLE.put("beta_1_9_pre_5", 501_905);
        ORACLE.put("beta_1_9_pre_6", 501_906);
        ORACLE.put("release_1_0_0", 601_000);
        ORACLE.put("release_1_1", 601_100);
        ORACLE.put("release_1_2_5", 601_205);
        ORACLE.put("release_1_3", 601_300);
        ORACLE.put("release_1_4", 601_400);
        ORACLE.put("release_1_5", 601_500);
        ORACLE.put("release_1_6_4", 601_604);
    }

    @Test
    @DisplayName("every declared version has exactly the chronology key it has always had")
    void everyDeclaredKeyIsPinned() {
        assertEquals(ORACLE.size(), HistoricalVersions.all().size(),
            "the oracle must list every declared version, and only those");
        for (Map.Entry<String, Integer> entry : ORACLE.entrySet()) {
            HistoricalVersion version = HistoricalVersions.byId("historical_experience:" + entry.getKey())
                .orElseThrow(() -> new AssertionError("not declared: " + entry.getKey()));
            assertEquals(entry.getValue().intValue(), version.chronology(), entry.getKey());
        }
    }

    @Test
    @DisplayName("the era bases are the scale the registry has always used")
    void eraBasesAreStable() {
        assertEquals(200_000, Era.INDEV.base());
        assertEquals(300_000, Era.INFDEV.base());
        assertEquals(400_000, Era.ALPHA.base());
        assertEquals(500_000, Era.BETA.base());
        assertEquals(600_000, Era.RELEASE.base());
        assertEquals(HistoricalVersions.ERA_ALPHA, Era.ALPHA.base());
        assertEquals(HistoricalVersions.ERA_BETA, Era.BETA.base());
        assertEquals(HistoricalVersions.ERA_RELEASE, Era.RELEASE.base());
        assertEquals(501_703, Era.BETA.key(1, 7, 3));
    }

    @Test
    @DisplayName("each milestone that names a declared build sits on that build's key")
    void declaredMilestonesMatchTheRegistry() throws IllegalAccessException {
        Map<String, String> declared = Map.of(
            "BETA_1_7_3", "beta_1_7_3",
            "BETA_1_9_PRE_3", "beta_1_9_pre_3",
            "BETA_1_9_PRE_4", "beta_1_9_pre_4",
            "BETA_1_6", "beta_1_6",
            "BETA_1_2", "beta_1_2",
            "RELEASE_1_1", "release_1_1",
            "RELEASE_1_2_5", "release_1_2_5",
            "RELEASE_1_4", "release_1_4",
            "RELEASE_1_5", "release_1_5",
            "RELEASE_1_6_4", "release_1_6_4");
        int checked = 0;
        for (Field field : Milestones.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != Milestone.class) {
                continue;
            }
            String build = declared.get(field.getName());
            if (build != null) {
                Milestone milestone = (Milestone) field.get(null);
                assertEquals(HistoricalVersions.byId("historical_experience:" + build).orElseThrow().chronology(),
                    milestone.key(), field.getName() + " must equal the registry key of " + build);
                checked++;
            }
        }
        assertEquals(declared.size(), checked, "every declared-build milestone must exist");
    }

    @Test
    @DisplayName("boundaries that are not declared builds sit where history puts them relative to declared ones")
    void undeclaredBoundariesAreBetweenTheRightBuilds() {
        int beta181 = HistoricalVersions.byId("historical_experience:beta_1_8_1").orElseThrow().chronology();
        int beta173 = HistoricalVersions.byId("historical_experience:beta_1_7_3").orElseThrow().chronology();
        assertTrue(Milestones.BETA_1_8.key() > beta173 && Milestones.BETA_1_8.key() < beta181,
            "Beta 1.8 falls between Beta 1.7.3 and Beta 1.8.1");
        assertEquals(501_800, Milestones.BETA_1_8.key());
        assertEquals(401_100, Milestones.ALPHA_1_1_0.key());
        assertEquals(401_200, Milestones.ALPHA_1_2_0.key());
        assertEquals(601_900, Milestones.RELEASE_1_9.key());
        int newest = HistoricalVersions.all().stream().mapToInt(HistoricalVersion::chronology).max().orElseThrow();
        assertTrue(Milestones.RELEASE_1_9.key() > newest, "Release 1.9 is newer than every declared build");
    }

    @Test
    @DisplayName("a synthetic after-boundary is one key past its build, which is how the texture cut-offs are written")
    void afterIsOneKeyPast() {
        assertEquals(501_802, new Milestone(501_801, "Beta 1.8.1").after().key());
        assertEquals(601_206, Milestones.RELEASE_1_2_5.after().key());
    }
}