package net.historicalexperience.world.upgrade;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import net.historicalexperience.world.metadata.HistoricalMetadata;
import net.historicalexperience.world.metadata.MetadataException;
import net.historicalexperience.world.metadata.MetadataJson;

/**
 * The write-ahead journal for an in-progress generation upgrade.
 *
 * <p>An upgrade changes a world's persisted generation revision. If the process dies between backing the
 * world up and publishing the new metadata, the world is left in a state that is neither the old
 * revision nor the new one, and the next start has to be able to say which it is and finish the job. The
 * journal is what makes that answerable: it is written and forced <em>before</em> the metadata is
 * touched, and it carries the complete intended metadata document rather than a description of it.
 *
 * <p>Carrying the whole document is the point. A journal that recorded only "move revision 3 to 4"
 * would force recovery to reconstruct the new document from assumptions - the actor, the note, the
 * destination preset - and a reconstruction that differs even slightly from what the interrupted run
 * intended is a silent corruption of the world's history. Recovery instead replays exactly what the
 * interrupted run was going to write.
 *
 * <p>The embedded document is stored as a JSON <em>string</em>, so recovery parses it with
 * {@link MetadataJson#decode}, the same strict parser that guards the real metadata file. A journal that
 * cannot be parsed is therefore rejected by the same code path that rejects a corrupt world.
 *
 * @param schema             journal schema version
 * @param phase              how far the interrupted run got
 * @param timestamp          epoch milliseconds the journal was written
 * @param backupDirectory    world-relative path of the backup taken before this upgrade
 * @param targetMetadataJson the complete metadata document the run intended to publish
 */
public record UpgradeJournal(
    int schema,
    Phase phase,
    long timestamp,
    String backupDirectory,
    String fromPreset,
    String targetMetadataJson
) {
    /** Journal schema written and read by this build. */
    public static final int CURRENT_SCHEMA = 1;

    /** File name of the journal inside a world directory. */
    public static final String FILE_NAME = "historical_experience_upgrade.journal";

    /** Directory, inside a world directory, that holds upgrade backups. */
    public static final String BACKUP_DIRECTORY_NAME = "historical_experience_backups";

    /**
     * How far an upgrade got before it stopped.
     *
     * <p>There is deliberately no {@code COMPLETE} phase: a completed upgrade has no journal. The two
     * phases below are the two crash windows that need different handling.
     */
    public enum Phase {
        /** The backup exists and was verified; the metadata has NOT been touched. */
        BACKUP_WRITTEN,
        /** The metadata was published; only the journal's removal remained. */
        METADATA_APPLIED
    }

    public UpgradeJournal {
        if (schema != CURRENT_SCHEMA) {
            throw new IllegalArgumentException(
                "unsupported upgrade journal schema " + schema + " (this build writes and reads "
                    + CURRENT_SCHEMA + ")");
        }
        if (phase == null) {
            throw new IllegalArgumentException("phase must not be null");
        }
        if (timestamp < 0) {
            throw new IllegalArgumentException("timestamp must be >= 0, got " + timestamp);
        }
        if (backupDirectory == null || backupDirectory.isBlank()) {
            throw new IllegalArgumentException("backupDirectory must not be blank");
        }
        // The journal must remember the preset the world was on BEFORE the upgrade. The metadata cannot
        // be asked later, because recovery may already have published the destination document, and the
        // level stem still needs to be told what it is expected to be holding. Recovering that fact from
        // anywhere else would be inference, and inference is what this record exists to remove.
        if (fromPreset == null || fromPreset.isBlank()) {
            throw new IllegalArgumentException("fromPreset must not be blank");
        }
        if (targetMetadataJson == null || targetMetadataJson.isBlank()) {
            throw new IllegalArgumentException("targetMetadataJson must not be blank");
        }
        // Validated at construction, not only at recovery: a journal that cannot be replayed must never
        // reach the disk, because its whole purpose is to be replayed by a process that cannot ask
        // anyone what was meant.
        try {
            MetadataJson.decode(targetMetadataJson);
        } catch (MetadataException e) {
            throw new IllegalArgumentException(
                "targetMetadataJson is not a decodable metadata document: " + e.getMessage(), e);
        }
    }

    /** The intended metadata document, parsed with the same strict parser the world file uses. */
    public HistoricalMetadata targetMetadata() throws MetadataException {
        return MetadataJson.decode(targetMetadataJson);
    }

    /** Encodes this journal as a JSON document. */
    public String encode() {
        StringWriter out = new StringWriter();
        com.google.gson.stream.JsonWriter writer = new com.google.gson.stream.JsonWriter(out);
        try {
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("schema").value(schema);
            writer.name("phase").value(phase.name());
            writer.name("timestamp").value(timestamp);
            writer.name("backupDirectory").value(backupDirectory);
            writer.name("fromPreset").value(fromPreset);
            writer.name("targetMetadataJson").value(targetMetadataJson);
            writer.endObject();
            writer.close();
        } catch (IOException e) {
            // A StringWriter cannot fail; wrapping keeps the signature honest without pretending this is
            // a real I/O path.
            throw new IllegalStateException("in-memory journal encoding failed", e);
        }
        return out.toString();
    }

    /**
     * Parses a journal document strictly.
     *
     * <p>Unknown fields are rejected rather than ignored. A journal written by a newer build that this
     * build does not understand must be reported, not partially applied: the fields it carries may be
     * exactly the ones that decide how to recover.
     */
    public static UpgradeJournal decode(String json) throws UpgradeException {
        try (com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(
                new java.io.StringReader(json))) {
            reader.setLenient(false);
            if (reader.peek() != com.google.gson.stream.JsonToken.BEGIN_OBJECT) {
                throw new UpgradeException("upgrade journal must be a JSON object");
            }
            Integer schema = null;
            Phase phase = null;
            Long timestamp = null;
            String backupDirectory = null;
            String fromPreset = null;
            String targetMetadataJson = null;

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "schema" -> schema = readInt(reader, name);
                    case "phase" -> {
                        String raw = readString(reader, name);
                        try {
                            phase = Phase.valueOf(raw);
                        } catch (IllegalArgumentException e) {
                            throw new UpgradeException(
                                "upgrade journal phase '" + raw + "' is not a phase this build knows");
                        }
                    }
                    case "timestamp" -> timestamp = readLong(reader, name);
                    case "backupDirectory" -> backupDirectory = readString(reader, name);
                    case "fromPreset" -> fromPreset = readString(reader, name);
                    case "targetMetadataJson" -> targetMetadataJson = readString(reader, name);
                    default -> throw new UpgradeException(
                        "upgrade journal contains unsupported field '" + name + "'");
                }
            }
            reader.endObject();
            if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) {
                throw new UpgradeException("upgrade journal has trailing content after the object");
            }
            if (schema == null || phase == null || timestamp == null || backupDirectory == null
                    || fromPreset == null || targetMetadataJson == null) {
                throw new UpgradeException("upgrade journal is missing one or more required fields");
            }
            return new UpgradeJournal(schema, phase, timestamp, backupDirectory, fromPreset,
                targetMetadataJson);
        } catch (IOException | IllegalArgumentException e) {
            throw new UpgradeException("upgrade journal is not readable: " + e.getMessage(), e);
        }
    }

    /** Journal file path inside a world directory. */
    public static Path fileIn(Path worldDirectory) {
        return worldDirectory.resolve(FILE_NAME);
    }

    public static boolean existsIn(Path worldDirectory) {
        return Files.isRegularFile(fileIn(worldDirectory));
    }

    /**
     * Writes the journal and forces it to disk.
     *
     * <p>Publication is a same-directory atomic move, and the bytes are forced before the move, so a
     * crash either leaves no journal or leaves a complete one. A torn journal would be worse than no
     * journal: it would be a document about a world change that cannot be read.
     */
    public void writeTo(Path worldDirectory) throws UpgradeException {
        Path target = fileIn(worldDirectory);
        Path temporary = target.resolveSibling(FILE_NAME + "." + UUID.randomUUID() + ".tmp");
        try {
            byte[] bytes = encode().getBytes(StandardCharsets.UTF_8);
            try (java.nio.channels.FileChannel channel = java.nio.channels.FileChannel.open(
                    temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                channel.write(java.nio.ByteBuffer.wrap(bytes));
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw new UpgradeException("could not write the upgrade journal " + target, e);
        }
    }

    /** Reads the journal from a world directory. */
    public static UpgradeJournal readFrom(Path worldDirectory) throws UpgradeException {
        Path file = fileIn(worldDirectory);
        String json;
        try {
            json = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UpgradeException("could not read the upgrade journal " + file, e);
        }
        return decode(json);
    }

    /** Deletes the journal, which is what makes the upgrade complete. */
    public static void deleteFrom(Path worldDirectory) throws UpgradeException {
        try {
            Files.deleteIfExists(fileIn(worldDirectory));
        } catch (IOException e) {
            throw new UpgradeException("could not remove the completed upgrade journal", e);
        }
    }

    /** Returns a copy of this journal at a later phase. */
    public UpgradeJournal withPhase(Phase next) {
        return new UpgradeJournal(schema, next, timestamp, backupDirectory, fromPreset,
            targetMetadataJson);
    }

    private static int readInt(com.google.gson.stream.JsonReader reader, String field)
            throws IOException {
        if (reader.peek() != com.google.gson.stream.JsonToken.NUMBER) {
            throw new IOException("field '" + field + "' must be a number");
        }
        return reader.nextInt();
    }

    private static long readLong(com.google.gson.stream.JsonReader reader, String field)
            throws IOException {
        if (reader.peek() != com.google.gson.stream.JsonToken.NUMBER) {
            throw new IOException("field '" + field + "' must be a number");
        }
        return reader.nextLong();
    }

    private static String readString(com.google.gson.stream.JsonReader reader, String field)
            throws IOException {
        if (reader.peek() != com.google.gson.stream.JsonToken.STRING) {
            throw new IOException("field '" + field + "' must be a string");
        }
        return reader.nextString();
    }
}
