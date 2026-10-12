package net.historicalexperience.world.metadata;

/**
 * A single entry in the world's historical revision history.
 *
 * @param revision  monotonically increasing revision number
 * @param timestamp epoch milliseconds at which the revision was recorded
 * @param actor     free-form attribution, normally the mod id that performed the change
 * @param note      short human readable description of what changed
 */
public record MetadataRevision(long revision, long timestamp, String actor, String note) {
    public MetadataRevision {
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be >= 1, got " + revision);
        }
        if (timestamp < 0) {
            throw new IllegalArgumentException("timestamp must be >= 0, got " + timestamp);
        }
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("note must not be blank");
        }
    }
}
