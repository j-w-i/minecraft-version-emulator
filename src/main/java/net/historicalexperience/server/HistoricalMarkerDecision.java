package net.historicalexperience.server;

import java.util.Objects;

import net.historicalexperience.HistoricalMarker;
import net.historicalexperience.HistoricalMarker.MarkerState;
import net.historicalexperience.world.HistoricalWorldState;

/**
 * The pure, testable policy that decides what the server does with a world's historical marker.
 *
 * <p>This class contains no Minecraft types and performs no I/O, so every branch - including the
 * fail-closed ones - is covered by plain unit tests. The server lifecycle only supplies evidence
 * and executes the returned {@link Action}.
 *
 * <p>The rules, in order:
 * <ol>
 *   <li>A registry lookup failure is never evidence of anything. It fails closed.</li>
 *   <li>Metadata that exists but is unreadable always fails closed, marked or not.</li>
 *   <li>Metadata present but no marker on the primary (overworld) level fails closed. Treating
 *       this as "historical anyway" was the previous fail-open behaviour.</li>
 *   <li>Metadata present but the loaded marker differs from the persisted generation preset fails
 *       closed. The comparison is exact identifier equality against the closed set of project owned
 *       markers: a different revision of the same era is a different recipe, not the same world.</li>
 *   <li>Only an explicit marker on the overworld may establish metadata for a world that has none.
 *       Markers on other levels (an own-nether or own-end) never classify the overworld.</li>
 *   <li>A normal overworld with an unrecognised marker is left ordinary: this mod never tags a
 *       world automatically.</li>
 * </ol>
 */
public final class HistoricalMarkerDecision {

    /** What the lifecycle must actually do. */
    public enum Action {
        /** Not a historical world: leave it completely untouched. */
        LEAVE_ORDINARY,
        /** The overworld carries this project's marker and no metadata exists yet: publish it. */
        ESTABLISH_METADATA,
        /** Metadata and the overworld marker agree: treat the world as historical. */
        ENFORCE_HISTORICAL,
        /** Nothing may continue safely: abort startup, changing nothing on disk. */
        FAIL_CLOSED
    }

    /**
     * Evidence about the primary level's marker.
     *
     * @param registryFailed  true when the level stem registry could not be consulted at all
     * @param primaryMarker   the marker state of the overworld level's generator
     * @param primaryMarkerId the raw marker identifier, or {@code null} when there is none
     * @param persistedPreset the generation preset persisted in metadata, or {@code null} when no
     *                        metadata is present; used for the mismatch diagnostic
     */
    public record MarkerEvidence(
        boolean registryFailed,
        MarkerState primaryMarker,
        String primaryMarkerId,
        String persistedPreset
    ) {
        public MarkerEvidence {
            Objects.requireNonNull(primaryMarker, "primaryMarker");
        }

        /** Evidence for a level whose marker could be read. */
        public static MarkerEvidence observed(
            MarkerState primaryMarker,
            String primaryMarkerId,
            String persistedPreset
        ) {
            return new MarkerEvidence(false, primaryMarker, primaryMarkerId, persistedPreset);
        }

        /** Evidence for a level whose marker could not be read. */
        public static MarkerEvidence unknown(String detail) {
            return new MarkerEvidence(true, MarkerState.UNKNOWN, detail, null);
        }

        /** True when the overworld positively carries this project's historical preset marker. */
        public boolean overworldMarkedHistorical() {
            return !registryFailed && primaryMarker == MarkerState.HISTORICAL;
        }

        /** The marker as it should be logged. */
        public String displayMarker() {
            if (registryFailed) {
                return "<unknown: " + (primaryMarkerId == null ? "registry unavailable"
                    : primaryMarkerId) + ">";
            }
            if (primaryMarkerId == null) {
                return "<none>";
            }
            return primaryMarkerId;
        }

        /**
         * Whether the saved marker and the persisted preset agree, by exact identifier equality.
         *
         * <p>Both sides must be the <em>same</em> project owned generation-recipe marker: a marker
         * that this build recognises but that is not the persisted one fails, and a persisted value
         * that is not recognised fails. One project preset is never treated as equivalent to
         * another, and a new revision is never silently read as the frozen legacy one.
         *
         * <p>The two synthetic fallbacks are the pre-existing behaviour for evidence that carries no
         * raw identifier at all (a marked overworld described without a marker string, or no
         * metadata yet). They substitute the expected identifier only where the value is genuinely
         * absent; they are never applied to a present-but-unrecognised marker, which is what keeps
         * the new markers from defaulting to the legacy declaration.
         */
        public boolean markersAgree() {
            String expected = HistoricalMarker.HISTORICAL_PRESET_ID;
            String actual = markerReference(primaryMarkerId, expected);
            String persisted = markerReference(persistedPreset, expected);
            return HistoricalMarker.isHistoricalMarker(actual)
                && HistoricalMarker.isHistoricalMarker(persisted)
                && actual.equals(persisted);
        }

        /** The observed value, or the expected one when the observation carries none. */
        private static String markerReference(String value, String expected) {
            return value == null ? expected : value;
        }
    }

    /** The outcome of the policy: an action plus why it was chosen. */
    public record Decision(Action action, String reason, boolean metadataPresent) {
        public Decision {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(reason, "reason");
        }

        /** True when startup must abort. */
        public boolean failsClosed() {
            return action == Action.FAIL_CLOSED;
        }
    }

    private HistoricalMarkerDecision() {
    }

    /**
     * Applies the policy.
     *
     * <p>The returned action is deliberately independent of which project owned marker was observed:
     * {@link Action#ESTABLISH_METADATA} means "the overworld carries an owned marker", and the
     * lifecycle must resolve that observed marker to its own generation binding before writing
     * anything, failing closed when no binding exists. It must never assume the frozen legacy
     * binding.
     *
     * @param state    the world's historical state, as read from disk
     * @param evidence the overworld marker evidence
     */
    public static Decision decide(HistoricalWorldState state, MarkerEvidence evidence) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(evidence, "evidence");

        boolean metadataPresent = !(state instanceof HistoricalWorldState.Normal);

        if (evidence.registryFailed() || evidence.primaryMarker() == MarkerState.UNKNOWN) {
            return new Decision(Action.FAIL_CLOSED,
                "the overworld marker could not be determined from its level stem/settings registry, "
                    + "so the marker is unknown (" + evidence.displayMarker() + "); refusing to guess "
                    + "whether this world is historical",
                metadataPresent);
        }

        if (state instanceof HistoricalWorldState.Unreadable unreadable) {
            return new Decision(Action.FAIL_CLOSED,
                "historical metadata is unreadable: " + unreadable.reason(), true);
        }

        if (state instanceof HistoricalWorldState.Historical historical) {
            String persisted = historical.metadata().generationPreset().value();
            if (!evidence.overworldMarkedHistorical()) {
                return new Decision(Action.FAIL_CLOSED,
                    "historical metadata is present but the overworld level carries no "
                        + "historical_experience preset marker (marker=" + evidence.displayMarker()
                        + "); the terrain no longer matches the recorded world. Refusing to start.",
                    true);
            }
            if (!evidence.markersAgree()) {
                return new Decision(Action.FAIL_CLOSED,
                    "loaded overworld marker '" + evidence.displayMarker()
                        + "' does not match persisted generation preset '" + persisted + "'",
                    true);
            }
            return new Decision(Action.ENFORCE_HISTORICAL,
                "overworld marker and persisted metadata agree", true);
        }

        // Normal world: no metadata document exists.
        if (evidence.overworldMarkedHistorical()) {
            return new Decision(Action.ESTABLISH_METADATA,
                "overworld carries this project's historical preset marker and no metadata exists "
                    + "yet; establishing it", false);
        }
        return new Decision(Action.LEAVE_ORDINARY,
            "overworld is not marked with this project's historical preset (marker="
                + evidence.displayMarker() + ", state=" + evidence.primaryMarker()
                + "); treating it as an ordinary world and leaving it untouched", false);
    }

    /** Convenience for tests and logging: the action only. */
    public static Action actionFor(HistoricalWorldState state, MarkerEvidence evidence) {
        return decide(state, evidence).action();
    }

    /** Evidence for a level whose marker could not be read at all. */
    public static MarkerEvidence registryFailure(String detail) {
        return MarkerEvidence.unknown(detail == null ? "unavailable" : detail);
    }
}
