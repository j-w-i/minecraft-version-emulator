package net.historicalexperience.world.upgrade;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A verified copy of the files that an upgrade could affect, taken before the upgrade runs.
 *
 * <p>An upgrade only rewrites the metadata document, so that is what the backup has to cover - plus
 * {@code level.dat}, which records the world's generator and is the one file a failed upgrade could
 * leave inconsistent. Region files are deliberately <b>not</b> copied here: the upgrade never writes
 * them, and copying gigabytes of chunks to protect against a write that does not happen would make the
 * operation too expensive to run. The upgrade's own test asserts the region files are byte-identical
 * across the operation, which is the guarantee that matters and is cheaper to keep than a copy.
 *
 * <p>The manifest is what makes the copy a backup rather than a hopeful duplicate. It records the size
 * and SHA-256 of every copied file, and recovery verifies them <em>before</em> it applies anything: a
 * backup that cannot be shown to be intact is not a safe place to roll forward from.
 */
public final class WorldBackup {
    /** Manifest file name inside a backup directory. */
    public static final String MANIFEST_FILE_NAME = "backup-manifest.json";

    /** Manifest schema written and read by this build. */
    public static final int MANIFEST_SCHEMA = 1;

    private WorldBackup() {
    }

    /**
     * One backed-up file.
     *
     * @param relativePath path relative to the world directory, so the manifest is portable
     * @param size         byte size at backup time
     * @param sha256       lowercase hex SHA-256 of the bytes at backup time
     */
    public record Entry(String relativePath, long size, String sha256) {
        public Entry {
            if (relativePath == null || relativePath.isBlank()) {
                throw new IllegalArgumentException("relativePath must not be blank");
            }
            if (size < 0) {
                throw new IllegalArgumentException("size must be >= 0, got " + size);
            }
            if (sha256 == null || sha256.length() != 64) {
                throw new IllegalArgumentException(
                    "sha256 must be 64 lowercase hex characters, got " + sha256);
            }
        }
    }

    /**
     * The record of what was copied, and for which upgrade.
     *
     * @param schema             manifest schema version
     * @param timestamp          epoch milliseconds the backup was taken
     * @param fromRevision       the world's revision before the upgrade
     * @param toRevision         the revision the upgrade intended to publish
     * @param entries            one entry per backed-up file
     */
    public record Manifest(
        int schema,
        long timestamp,
        long fromRevision,
        long toRevision,
        List<Entry> entries
    ) {
        public Manifest {
            if (schema != MANIFEST_SCHEMA) {
                throw new IllegalArgumentException(
                    "unsupported backup manifest schema " + schema + " (this build writes "
                        + MANIFEST_SCHEMA + ")");
            }
            if (fromRevision < 1) {
                throw new IllegalArgumentException("fromRevision must be >= 1, got " + fromRevision);
            }
            if (toRevision <= fromRevision) {
                throw new IllegalArgumentException(
                    "toRevision " + toRevision + " must be greater than fromRevision " + fromRevision);
            }
            entries = List.copyOf(entries);
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("a backup manifest must list at least one file");
            }
        }
    }

    /**
     * Copies the given world-relative files into a new, timestamped backup directory.
     *
     * @param worldDirectory the world being upgraded
     * @param relativePaths  world-relative files that must survive the upgrade
     * @param fromRevision   revision before the upgrade, used for the label and the manifest
     * @param toRevision     revision the upgrade intends to publish
     * @param timestamp      epoch milliseconds
     * @return the world-relative path of the backup directory, which is what the journal records
     */
    public static String create(
        Path worldDirectory,
        List<String> relativePaths,
        long fromRevision,
        long toRevision,
        long timestamp
    ) throws UpgradeException {
        String label = timestamp + "-r" + fromRevision + "-to-r" + toRevision;
        Path backupDirectory = worldDirectory
            .resolve(UpgradeJournal.BACKUP_DIRECTORY_NAME)
            .resolve(label);
        try {
            Files.createDirectories(backupDirectory);
            List<Entry> entries = new ArrayList<>();
            for (String relative : relativePaths) {
                Path source = worldDirectory.resolve(relative);
                if (!Files.isRegularFile(source)) {
                    throw new UpgradeException(
                        "cannot back up " + relative + ": it is not a regular file in " + worldDirectory);
                }
                Path destination = backupDirectory.resolve(relative.replace('/', '_'));
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                byte[] bytes = Files.readAllBytes(source);
                entries.add(new Entry(relative, bytes.length, sha256(bytes)));
            }
            Manifest manifest = new Manifest(
                MANIFEST_SCHEMA, timestamp, fromRevision, toRevision, entries);
            Files.writeString(backupDirectory.resolve(MANIFEST_FILE_NAME), ManifestJson.encode(manifest),
                StandardCharsets.UTF_8);
            return UpgradeJournal.BACKUP_DIRECTORY_NAME + "/" + label;
        } catch (IOException e) {
            throw new UpgradeException("could not create a backup in " + backupDirectory, e);
        }
    }

    /**
     * Re-hashes every backed-up file and compares it with the manifest.
     *
     * @return the number of files verified
     */
    public static int verify(Path worldDirectory, String worldRelativeBackupDirectory)
            throws UpgradeException {
        Path backupDirectory = worldDirectory.resolve(worldRelativeBackupDirectory);
        Manifest manifest = readManifest(backupDirectory);
        for (Entry entry : manifest.entries()) {
            Path file = backupDirectory.resolve(entry.relativePath().replace('/', '_'));
            if (!Files.isRegularFile(file)) {
                throw new UpgradeException(
                    "backup is incomplete: " + entry.relativePath() + " is missing from "
                        + backupDirectory);
            }
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(file);
            } catch (IOException e) {
                throw new UpgradeException("could not read backed-up " + entry.relativePath(), e);
            }
            if (bytes.length != entry.size()) {
                throw new UpgradeException("backup of " + entry.relativePath() + " has size "
                    + bytes.length + " but the manifest recorded " + entry.size());
            }
            String actual = sha256(bytes);
            if (!actual.equals(entry.sha256())) {
                throw new UpgradeException("backup of " + entry.relativePath() + " does not match its "
                    + "recorded SHA-256; refusing to treat it as a safe recovery point");
            }
        }
        return manifest.entries().size();
    }

    /** Reads a backup manifest. */
    public static Manifest readManifest(Path backupDirectory) throws UpgradeException {
        Path file = backupDirectory.resolve(MANIFEST_FILE_NAME);
        String json;
        try {
            json = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UpgradeException("could not read the backup manifest " + file, e);
        }
        return ManifestJson.decode(json);
    }

    /** Lowercase hex SHA-256. */
    public static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                out.append(String.format(Locale.ROOT, "%02x", b));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required of every Java platform; failing loudly is correct if it is absent.
            throw new IllegalStateException("SHA-256 is unavailable in this runtime", e);
        }
    }

    /** Manifest encoders and strict decoder. */
    public static final class ManifestJson {
        private ManifestJson() {
        }

        public static String encode(Manifest manifest) {
            StringWriter out = new StringWriter();
            com.google.gson.stream.JsonWriter writer = new com.google.gson.stream.JsonWriter(out);
            try {
                writer.setIndent("  ");
                writer.beginObject();
                writer.name("schema").value(manifest.schema());
                writer.name("timestamp").value(manifest.timestamp());
                writer.name("fromRevision").value(manifest.fromRevision());
                writer.name("toRevision").value(manifest.toRevision());
                writer.name("entries").beginArray();
                for (Entry entry : manifest.entries()) {
                    writer.beginObject();
                    writer.name("relativePath").value(entry.relativePath());
                    writer.name("size").value(entry.size());
                    writer.name("sha256").value(entry.sha256());
                    writer.endObject();
                }
                writer.endArray();
                writer.endObject();
                writer.close();
            } catch (IOException e) {
                throw new IllegalStateException("in-memory manifest encoding failed", e);
            }
            return out.toString();
        }

        public static Manifest decode(String json) throws UpgradeException {
            try (com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(
                    new java.io.StringReader(json))) {
                reader.setLenient(false);
                reader.beginObject();
                Integer schema = null;
                Long timestamp = null;
                Long fromRevision = null;
                Long toRevision = null;
                List<Entry> entries = null;
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    switch (name) {
                        case "schema" -> schema = reader.nextInt();
                        case "timestamp" -> timestamp = reader.nextLong();
                        case "fromRevision" -> fromRevision = reader.nextLong();
                        case "toRevision" -> toRevision = reader.nextLong();
                        case "entries" -> {
                            entries = new ArrayList<>();
                            reader.beginArray();
                            while (reader.hasNext()) {
                                reader.beginObject();
                                String relativePath = null;
                                Long size = null;
                                String sha = null;
                                while (reader.hasNext()) {
                                    switch (reader.nextName()) {
                                        case "relativePath" -> relativePath = reader.nextString();
                                        case "size" -> size = reader.nextLong();
                                        case "sha256" -> sha = reader.nextString();
                                        default -> throw new UpgradeException(
                                            "backup manifest entry has an unsupported field");
                                    }
                                }
                                reader.endObject();
                                if (relativePath == null || size == null || sha == null) {
                                    throw new UpgradeException(
                                        "backup manifest entry is missing a required field");
                                }
                                entries.add(new Entry(relativePath, size, sha));
                            }
                            reader.endArray();
                        }
                        default -> throw new UpgradeException(
                            "backup manifest contains unsupported field '" + name + "'");
                    }
                }
                reader.endObject();
                if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) {
                    throw new UpgradeException("backup manifest has trailing content");
                }
                if (schema == null || timestamp == null || fromRevision == null || toRevision == null
                        || entries == null) {
                    throw new UpgradeException("backup manifest is missing a required field");
                }
                return new Manifest(schema, timestamp, fromRevision, toRevision, entries);
            } catch (IOException | IllegalArgumentException e) {
                throw new UpgradeException("backup manifest is not readable: " + e.getMessage(), e);
            }
        }
    }
}
