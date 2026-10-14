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
package net.historicalexperience.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import net.historicalexperience.HistoricalMarker;
import net.historicalexperience.HistoricalMarker.MarkerState;
import net.historicalexperience.profile.HistoricalProfiles;
import net.historicalexperience.world.HistoricalWorldState;
import net.historicalexperience.world.metadata.HistoricalIdentifier;
import net.historicalexperience.world.metadata.HistoricalMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for the world classification policy.
 *
 * <p>No Minecraft runtime, no fake server, no Moderner Beta classes: the policy takes evidence and
 * returns an action, so every fail-closed branch is directly testable. The cases mirror the defects
 * found in the fail-open version: an unmarked overworld was treated as historical, and a marker on
 * any level (an own-nether or own-end) was enough to adopt an ordinary overworld.
 *
 * <p>Deliberately does not touch {@code ModernerBetaAdapter}: its static initialiser builds
 * Moderner Beta registry keys and throws unless the mod's registries are bootstrapped, which would
 * make this test a runtime test by accident.
 */
class HistoricalMarkerDecisionTest {
    private static final String HISTORICAL = HistoricalMarker.HISTORICAL_PRESET_ID;

    private static final Path METADATA_FILE = Path.of("world", "historical_experience.json");

    private static HistoricalWorldState normal() {
        return new HistoricalWorldState.Normal(METADATA_FILE);
    }

    private static HistoricalWorldState unreadable(String reason) {
        return new HistoricalWorldState.Unreadable(METADATA_FILE, reason);
    }

    private static HistoricalWorldState historical(String generationPreset) {
        return new HistoricalWorldState.Historical(
            METADATA_FILE,
            HistoricalMetadata.initial(
                HistoricalIdentifier.parse("historical_experience:beta_1_7_3"),
                HistoricalIdentifier.parse(generationPreset),
                "historical_experience",
                1_700_000_000_000L),
            HistoricalProfiles.beta173());
    }

    /** Evidence as the lifecycle builds it for a marked overworld. */
    private static HistoricalMarkerDecision.MarkerEvidence marked(String persisted) {
        return HistoricalMarkerDecision.MarkerEvidence.observed(
            MarkerState.HISTORICAL, HISTORICAL, persisted);
    }

    /** Evidence for an overworld that is a Moderner Beta world without the project marker. */
    private static HistoricalMarkerDecision.MarkerEvidence otherModernerBeta(String persisted) {
        return HistoricalMarkerDecision.MarkerEvidence.observed(
            MarkerState.OTHER, "moderner_beta:beta", persisted);
    }

    /** Evidence for an overworld that is not a Moderner Beta world at all. */
    private static HistoricalMarkerDecision.MarkerEvidence notModernerBeta() {
        return HistoricalMarkerDecision.MarkerEvidence.observed(
            MarkerState.NOT_MODERNER_BETA, "<not-a-moderner-beta-generator>", null);
    }

    @Test
    @DisplayName("a normal world with no marker is left ordinary and is never tagged")
    void normalUnmarkedIsLeftOrdinary() {
        HistoricalMarkerDecision.Decision decision = HistoricalMarkerDecision.decide(
            normal(), notModernerBeta());

        assertEquals(HistoricalMarkerDecision.Action.LEAVE_ORDINARY, decision.action());
        assertFalse(decision.failsClosed());
        assertFalse(decision.metadataPresent());
    }

    @Test
    @DisplayName("a normal overworld marked with the project preset establishes metadata")
    void normalMarkedEstablishes() {
        HistoricalMarkerDecision.Decision decision = HistoricalMarkerDecision.decide(
            normal(), marked(null));

        assertEquals(HistoricalMarkerDecision.Action.ESTABLISH_METADATA, decision.action());
        assertFalse(decision.failsClosed());
    }

    @Test
    @DisplayName("a normal overworld marked with some other preset is left ordinary")
    void normalOtherMarkerIsLeftOrdinary() {
        HistoricalMarkerDecision.Decision decision = HistoricalMarkerDecision.decide(
            normal(), otherModernerBeta(null));

        assertEquals(HistoricalMarkerDecision.Action.LEAVE_ORDINARY, decision.action());
        assertFalse(decision.failsClosed());
    }

    @Test
    @DisplayName("a normal overworld whose marker registry failed still fails closed")
    void normalWithRegistryFailureFailsClosed() {
        // Even with no metadata at all: an unreadable registry is never evidence of anything, and
        // guessing "ordinary" here is how an own-nether or own-end marker adopts a normal world.
        HistoricalMarkerDecision.Decision decision = HistoricalMarkerDecision.decide(
            normal(), HistoricalMarkerDecision.registryFailure("level stem registry unavailable"));

        assertEquals(HistoricalMarkerDecision.Action.FAIL_CLOSED, decision.action());
        assertTrue(decision.failsClosed());
    }

    @Test
    @DisplayName("existing valid metadata with a matching overworld marker is enforced")
    void historicalWithMatchingMarkerIsEnforced() {
        HistoricalMarkerDecision.Decision decision = HistoricalMarkerDecision.decide(
            historical(HISTORICAL), marked(HISTORICAL));

        assertEquals(HistoricalMarkerDecision.Action.ENFORCE_HISTORICAL, decision.action());
        assertFalse(decision.failsClosed());
        assertTrue(decision.metadataPresent());
    }

    @Test
    @DisplayName("existing metadata with no overworld marker fails closed instead of warning")
    void historicalWithMissingMarkerFailsClosed() {
        // The previous implementation logged a warning and continued as historical: fail-open.
        HistoricalMarkerDecision.Decision decision = HistoricalMarkerDecision.decide(
            historical(HISTORICAL), otherModernerBeta(HISTORICAL));

        assertEquals(HistoricalMarkerDecision.Action.FAIL_CLOSED, decision.action());
        assertTrue(decision.reason().contains("no"), decision.reason());
    }

    @Test
    @DisplayName("metadata whose persisted preset differs from the loaded marker fails closed")
    void historicalWithMarkerMismatchFailsClosed() {
        HistoricalMarkerDecision.Decision decision = HistoricalMarkerDecision.decide(
            historical(HISTORICAL), marked("historical_experience:skylands"));

        assertEquals(HistoricalMarkerDecision.Action.FAIL_CLOSED, decision.action());
        assertTrue(decision.reason().contains("does not match"), decision.reason());
    }

    @Test
    @DisplayName("existing metadata over a non-Moderer-Beta overworld fails closed")
    void historicalOverForeignGeneratorFailsClosed() {
        HistoricalMarkerDecision.Decision decision = HistoricalMarkerDecision.decide(
            historical(HISTORICAL), notModernerBeta());

        assertEquals(HistoricalMarkerDecision.Action.FAIL_CLOSED, decision.action());
    }

    @Test
    @DisplayName("corrupt metadata fails closed even when the overworld is marked")
    void corruptMetadataFailsClosed() {
        HistoricalMarkerDecision.Decision decision = HistoricalMarkerDecision.decide(
            unreadable("unreadable historical metadata"), marked(HISTORICAL));

        assertEquals(HistoricalMarkerDecision.Action.FAIL_CLOSED, decision.action());
        assertTrue(decision.reason().contains("unreadable"), decision.reason());
        assertTrue(decision.metadataPresent());
    }

    @Test
    @DisplayName("corrupt metadata on an unmarked, unregistered overworld still fails closed")
    void corruptMetadataOnUnknownWorldFailsClosed() {
        HistoricalMarkerDecision.Decision decision = HistoricalMarkerDecision.decide(
            unreadable("newer schema"), HistoricalMarkerDecision.registryFailure("unavailable"));

        assertEquals(HistoricalMarkerDecision.Action.FAIL_CLOSED, decision.action());
    }

    @Test
    @DisplayName("evidence defaults to the project preset when no raw marker was observed")
    void evidenceFallsBackToTheProjectPreset() {
        HistoricalMarkerDecision.MarkerEvidence evidence = marked(null);

        assertEquals(HISTORICAL, evidence.displayMarker());
        assertTrue(evidence.markersAgree());
        assertTrue(evidence.overworldMarkedHistorical());
    }

    @Test
    @DisplayName("unknown evidence is reported as unknown, never as absent")
    void unknownEvidenceDisplaysAsUnknown() {
        HistoricalMarkerDecision.MarkerEvidence evidence =
            HistoricalMarkerDecision.registryFailure("registry unavailable");

        assertTrue(evidence.registryFailed());
        assertFalse(evidence.overworldMarkedHistorical());
        assertTrue(evidence.displayMarker().contains("unknown"), evidence.displayMarker());
    }

    @Test
    @DisplayName("an unknown overworld marker fails closed even with an available stem registry")
    void unknownObservedMarkerIsAlwaysClosed() {
        HistoricalMarkerDecision.MarkerEvidence unknown =
            HistoricalMarkerDecision.MarkerEvidence.observed(MarkerState.UNKNOWN, "<unresolved>", null);

        assertEquals(HistoricalMarkerDecision.Action.FAIL_CLOSED,
            HistoricalMarkerDecision.actionFor(normal(), unknown));
        assertEquals(HistoricalMarkerDecision.Action.FAIL_CLOSED,
            HistoricalMarkerDecision.actionFor(historical(HISTORICAL), unknown));
        assertEquals(HistoricalMarkerDecision.Action.FAIL_CLOSED,
            HistoricalMarkerDecision.actionFor(unreadable("bad"), unknown));
    }

    @Test
    @DisplayName("a registry failure never yields a normal-world action for any metadata state")
    void registryFailureIsAlwaysClosed() {
        HistoricalMarkerDecision.MarkerEvidence failure =
            HistoricalMarkerDecision.registryFailure("unavailable");

        assertEquals(HistoricalMarkerDecision.Action.FAIL_CLOSED,
            HistoricalMarkerDecision.actionFor(normal(), failure));
        assertEquals(HistoricalMarkerDecision.Action.FAIL_CLOSED,
            HistoricalMarkerDecision.actionFor(historical(HISTORICAL), failure));
        assertEquals(HistoricalMarkerDecision.Action.FAIL_CLOSED,
            HistoricalMarkerDecision.actionFor(unreadable("bad"), failure));
    }
}
