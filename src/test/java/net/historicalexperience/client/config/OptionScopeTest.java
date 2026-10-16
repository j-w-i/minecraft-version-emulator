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
package net.historicalexperience.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.historicalexperience.client.lighting.HistoricalOldLighting;
import net.historicalexperience.client.lighting.HistoricalSmoothLighting;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The config screen's rows: what they say, and the windows the two lighting engines answer for.
 *
 * <h2>Why the language file is asserted here</h2>
 *
 * <p>A missing translation key is not an error in Minecraft - the player is shown the raw key, or, when the key takes
 * an argument, the argument silently disappears. The rows in this screen now compose their text from a name, a state
 * and a sentence about where the setting applies, so the failure would read as "Old lighting: ON - (click to turn
 * off)" with a hole where the reason belongs. That is exactly the shape of report this test exists to prevent.
 *
 * <h2>Why the windows are asserted here</h2>
 *
 * <p>The owner's complaint about the lighting options was that toggling them did nothing. One half of the answer is
 * the store-on-click fix, which no unit test can reach; the other half is <em>when the engine applies at all</em>,
 * and that is a pure question about the version registry, so it is pinned here.
 */
class OptionScopeTest {
    /** Every row on the config screen, by its language-file stem. */
    private static final List<String> OPTIONS = List.of("smooth", "oldlight", "versionlabel");

    private static HistoricalVersion version(String id) {
        return HistoricalVersions.byId("historical_experience:" + id).orElseThrow();
    }

    private static JsonObject language() {
        var stream = OptionScopeTest.class.getResourceAsStream("/assets/historical_experience/lang/en_us.json");
        if (stream == null) {
            throw new AssertionError("the language file is not on the test classpath");
        }
        return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    @DisplayName("a scope says where the setting is used, and an unknown version is not 'applies'")
    void scopeFollowsVersionAndWindow() {
        assertEquals(OptionScope.NO_VERSION, OptionScope.of(null, false));
        assertEquals(OptionScope.NO_VERSION, OptionScope.of(null, true),
            "with no world open there is no version to apply to, whatever a stale answer says");
        assertEquals(OptionScope.APPLIES, OptionScope.of(version("beta_1_7_3"), true));
        assertEquals(OptionScope.NOT_IN_THIS_VERSION, OptionScope.of(version("release_1_0_0"), false));

        // The suffixes are what the screen appends to a language key, so they must be the ones in the file.
        assertEquals("applies", OptionScope.APPLIES.keySuffix());
        assertEquals("not_in_this_version", OptionScope.NOT_IN_THIS_VERSION.keySuffix());
        assertEquals("no_version", OptionScope.NO_VERSION.keySuffix());
    }

    @Test
    @DisplayName("every row can be clicked and says where it applies: no version is left without text")
    void everyRowHasTextForEveryScope() {
        JsonObject lang = language();
        for (String option : OPTIONS) {
            for (String state : List.of("on", "off")) {
                String key = "historical_experience.config." + option + "." + state;
                assertTrue(lang.has(key), "missing " + key);
                // The label is deliberately short - name and ON/OFF only. It used to interpolate the scope sentence,
                // which produced rows wider than their own buttons; the owner asked for short names. The sentence is
                // not lost: the screen shows it as the row's tooltip, and the note at the foot covers both lighting
                // options, so these keys must still exist and still be usable as tooltips.
                String label = lang.get(key).getAsString();
                assertFalse(label.contains("%s"), key + " is a short label now: " + label);
                assertTrue(label.length() < 40, key + " must stay short: " + label);
                // The click hint moved into the tooltip with the scope sentence, so it must exist for both states.
                assertTrue(lang.has("historical_experience.config.click." + state),
                    "missing historical_experience.config.click." + state + " for the tooltip");
            }
            for (OptionScope scope : OptionScope.values()) {
                String key = "historical_experience.config." + option + ".note." + scope.keySuffix();
                assertTrue(lang.has(key), "missing " + key);
                assertFalse(lang.get(key).getAsString().isBlank(), key + " must say something");
            }
        }
        // The no-world sentence must not claim anything about a world, since none is open.
        assertTrue(lang.get("historical_experience.config.oldlight.note.no_version").getAsString()
            .contains("kept"), "the no-world note should say the setting is kept for later");
    }

    @Test
    @DisplayName("the older keys the rows used to use are gone, so no dead text is left behind")
    void obsoleteKeysAreRemoved() {
        JsonObject lang = language();
        for (String option : OPTIONS) {
            for (String dead : List.of("not_applicable", "no_world")) {
                assertFalse(lang.has("historical_experience.config." + option + "." + dead),
                    "the rows no longer grey themselves out, so this key is unused: "
                        + "historical_experience.config." + option + "." + dead);
            }
        }
    }

    @Test
    @DisplayName("old smooth shading is offered for Beta 1.3 up to but not including Beta 1.8.1")
    void smoothLightingWindow() {
        assertTrue(HistoricalSmoothLighting.availableFor(version("beta_1_3")));
        assertTrue(HistoricalSmoothLighting.availableFor(version("beta_1_7_3")));
        assertFalse(HistoricalSmoothLighting.availableFor(version("beta_1_2")),
            "the Beta 1.3 engine did not exist in Beta 1.2");
        assertFalse(HistoricalSmoothLighting.availableFor(version("beta_1_8_1")),
            "Beta 1.8 replaced the engine");
        assertFalse(HistoricalSmoothLighting.availableFor(version("release_1_0_0")));
        assertFalse(HistoricalSmoothLighting.availableFor(null),
            "an unknown version is not 'yes': a client that guessed would render one thing and be told another");
    }

    @Test
    @DisplayName("old lighting is offered for every declared build before Beta 1.8.1")
    void oldLightingWindow() {
        assertTrue(HistoricalOldLighting.availableFor(version("infdev_420")));
        assertTrue(HistoricalOldLighting.availableFor(version("alpha_1_0_1")));
        assertTrue(HistoricalOldLighting.availableFor(version("beta_1_0")));
        assertTrue(HistoricalOldLighting.availableFor(version("beta_1_7_3")));
        assertFalse(HistoricalOldLighting.availableFor(version("beta_1_8_1")));
        assertFalse(HistoricalOldLighting.availableFor(version("release_1_6_4")));
        assertFalse(HistoricalOldLighting.availableFor(null));
    }

    @Test
    @DisplayName("a world in the owner's window offers both engines, which is what a probe has to exercise")
    void theWindowProbeFixtureOffersBoth() {
        // Recorded because the client-run probe for this fix plays a Beta 1.7.3 fixture: if that stops being a world
        // where both rows apply, the probe would silently stop testing what it claims to.
        HistoricalVersion beta173 = version("beta_1_7_3");
        assertTrue(HistoricalSmoothLighting.availableFor(beta173));
        assertTrue(HistoricalOldLighting.availableFor(beta173));
    }
}
