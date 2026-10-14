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

import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Weather exists from Beta 1.5 and in no earlier build.
 *
 * <h2>Why the boundary is asserted in both directions across the whole registry</h2>
 *
 * <p>The two possible mistakes are opposite and both silent. Letting weather run in Beta 1.4 or Alpha means a
 * player watches it rain in an era that had clear skies forever, and the registry entry would still read as
 * implemented. Removing it from Beta 1.5 or later deletes weather from every version that really had it —
 * including the thunderstorms, charged creepers and lightning fires that Beta 1.5 is defined by. Neither shows up
 * in a compile or a sweep of anachronistic blocks, so the boundary is walked rather than spot-checked.
 *
 * <p>Level: unit. The behaviour itself is a server-side injection, which the dedicated-server probe loads and
 * therefore verifies, because that mixin is deliberately common rather than client-scoped.
 */
class HistoricalWeatherTest {
    @Test
    @DisplayName("every build before Beta 1.5 has no weather, and every build from it does")
    void theBoundaryIsBetaOneFive() {
        int dry = 0;
        int wet = 0;
        for (var version : HistoricalVersions.all()) {
            boolean hasWeather = HistoricalWeather.existsIn(version);
            if (version.id().equals("historical_experience:beta_1_5")) {
                assertTrue(hasWeather, "Beta 1.5 is the build that introduced weather");
            }
            if (version.chronology() < HistoricalVersions.byId("historical_experience:beta_1_5")
                    .orElseThrow().chronology()) {
                assertFalse(hasWeather, version.id()
                    + " predates Beta 1.5, so it must have no rain, snow or thunder at all - the research lists "
                    + "weather as a DIFFERENCE FROM BETA 1.4 and its recreation notes say 'no modern weather "
                    + "defaults'");
                dry++;
            } else {
                assertTrue(hasWeather, version.id() + " is Beta 1.5 or later and had weather");
                wet++;
            }
        }
        // Beta 1.0 through 1.4 plus the six Alpha builds and both Infdev builds: the dry group is the large one.
        assertTrue(dry >= 12, "expected the pre-Beta-1.5 group to be substantial, saw " + dry);
        assertTrue(wet >= 15, "expected the Beta-1.5-and-later group to be substantial, saw " + wet);
    }

    @Test
    @DisplayName("the named versions either side of the boundary, so a rename cannot hide a regression")
    void theVersionsEitherSideAreNamed() {
        for (String id : new String[] {"historical_experience:alpha", "historical_experience:infdev_420",
                "historical_experience:beta_1_0", "historical_experience:beta_1_4"}) {
            assertFalse(HistoricalWeather.existsIn(HistoricalVersions.byId(id).orElseThrow()),
                id + " must have no weather");
        }
        for (String id : new String[] {"historical_experience:beta_1_5", "historical_experience:beta_1_6",
                "historical_experience:beta_1_7_3", "historical_experience:beta_1_8_1",
                "historical_experience:release_1_0_0", "historical_experience:release_1_6_4"}) {
            assertTrue(HistoricalWeather.existsIn(HistoricalVersions.byId(id).orElseThrow()),
                id + " had weather");
        }
    }

    @Test
    @DisplayName("an unknown version is left alone rather than being made dry")
    void unknownMeansNoChange() {
        // A null version is "cannot establish", and the safe answer is to change nothing: forcing clear skies on a
        // world whose metadata failed to resolve would alter ordinary worlds this project does not own.
        assertTrue(HistoricalWeather.existsIn(null),
            "an unresolvable version must not be treated as pre-Beta-1.5, or a lookup failure would silently "
                + "remove weather from worlds this project does not manage");
    }

    @Test
    @DisplayName("both weather entry points still exist with the signatures the injections assume")
    void theInjectionTargetsAreReal() throws Exception {
        // The same guard used for the lighting mixin: the injections name these methods by string, so a mapping
        // change would otherwise break them silently. require = 1 makes the failure loud at runtime; this makes it
        // loud in the unit suite, and states the parameter list the handler is written against.
        var cycle = net.minecraft.server.level.ServerLevel.class.getDeclaredMethod("advanceWeatherCycle");
        assertEquals(void.class, cycle.getReturnType());

        var parameters = net.minecraft.server.level.ServerLevel.class.getMethod("setWeatherParameters",
            int.class, int.class, boolean.class, boolean.class);
        assertEquals(void.class, parameters.getReturnType());

        // And each handler must name one of those two methods, so the annotation and the target cannot drift.
        // Counting methods would be the wrong check: the mixin also declares the two @Shadow setters, so a count
        // measures the class shape rather than the thing that can break.
        var mixin = Class.forName("net.historicalexperience.mixin.ServerLevelHistoricalWeatherMixin");
        java.util.List<String> named = new java.util.ArrayList<>();
        for (var method : mixin.getDeclaredMethods()) {
            var inject = method.getAnnotation(org.spongepowered.asm.mixin.injection.Inject.class);
            if (inject != null) {
                named.addAll(java.util.List.of(inject.method()));
            }
        }
        java.util.Collections.sort(named);
        assertEquals(java.util.List.of("advanceWeatherCycle", "setWeatherParameters"), named,
            "the mixin must inject into exactly these two entry points: the natural cycle (which sets the timers "
                + "without going through the setter) and the setter (which every deliberate change, including "
                + "/weather and level initialisation, goes through). Blocking only one leaves a way to rain");
    }
}
