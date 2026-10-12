package net.historicalexperience.world.metadata;

/**
 * Persisted metadata schema registry.
 *
 * <p>The schema version is stored explicitly in the metadata document. Only
 * {@link #CURRENT} is accepted by this build; anything else (in particular a
 * higher version written by a newer build) must be treated as unreadable rather
 * than repaired in place, so that unsupported stored content is preserved.
 */
public final class MetadataSchema {
    /** The schema version written and read by this build. */
    public static final int CURRENT = 1;

    /** Relative file name of the metadata document inside a world directory. */
    public static final String FILE_NAME = "historical_experience_metadata.json";

    private MetadataSchema() {
    }
}
