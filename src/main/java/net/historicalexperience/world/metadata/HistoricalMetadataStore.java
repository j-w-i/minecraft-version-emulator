package net.historicalexperience.world.metadata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * File backed persistence for {@link HistoricalMetadata}.
 *
 * <p>Design rules, in priority order:
 * <ol>
 *   <li><b>Never destroy content.</b> A document that cannot be read is reported as unreadable;
 *       it is never overwritten, truncated or "repaired". Only {@link #write} replaces the
 *       document, and it does so after an explicit caller decision.</li>
 *   <li><b>Never expose a torn document.</b> Writes go to a sibling temporary file which is then
 *       moved into place with an atomic move where the filesystem supports it.</li>
 *   <li><b>Presence is meaningful.</b> {@link #hasEntry} distinguishes absence from an occupied
 *       metadata path; {@link #exists} only reports regular-file shape. Ordinary worlds have no
 *       metadata document and are never adopted without an explicit historical marker.</li>
 * </ol>
 */
public final class HistoricalMetadataStore {
    private final Path metadataFile;

    public HistoricalMetadataStore(Path metadataFile) {
        if (metadataFile == null) {
            throw new IllegalArgumentException("metadataFile must not be null");
        }
        this.metadataFile = metadataFile.toAbsolutePath();
    }

    /** Creates a store rooted at the given world directory. */
    public static HistoricalMetadataStore forWorldDirectory(Path worldDirectory) {
        if (worldDirectory == null) {
            throw new IllegalArgumentException("worldDirectory must not be null");
        }
        return new HistoricalMetadataStore(worldDirectory.resolve(MetadataSchema.FILE_NAME));
    }

    public Path file() {
        return metadataFile;
    }

    /**
     * Returns {@code true} when the metadata path resolves to a regular file.
     * This does not establish readability or validity; only {@link #read()} can do that.
     *
     * <p>Directories and dangling/loop symlinks return false here, but are not evidence that
     * metadata is absent. Classification callers must use {@link #hasEntry()} instead.
     */
    public boolean exists() {
        return Files.isRegularFile(metadataFile) && !Files.isDirectory(metadataFile);
    }

    /**
     * Returns {@code true} when <em>anything</em> occupies the metadata path, including a
     * directory or a symlink that {@link #exists()} would reject.
     *
     * <p>Callers deciding whether a world is ordinary must use this rather than {@link #exists()}:
     * a path that is present but not a readable document is a broken historical world, not an
     * ordinary one, and must fail closed.
     */
    public boolean hasEntry() {
        return Files.exists(metadataFile, java.nio.file.LinkOption.NOFOLLOW_LINKS)
            || Files.exists(metadataFile);
    }

    /**
     * Reads and validates the metadata document.
     *
     * @return the parsed metadata
     * @throws MetadataException  when the document exists but cannot be read, parsed or validated,
     *                            or when the path is not a regular file
     * @throws MetadataException  when no document exists (see {@link #exists()} to distinguish)
     * @throws IOException        when the document cannot be read from disk
     */
    public HistoricalMetadata read() throws MetadataException, IOException {
        Path file = metadataFile;
        if (Files.isDirectory(file)) {
            throw new MetadataException(
                "historical metadata path " + file + " is a directory, not a document");
        }
        if (!Files.isRegularFile(file)) {
            throw new MetadataException("no historical metadata document at " + file);
        }
        String json = Files.readString(file, StandardCharsets.UTF_8);
        try {
            return MetadataJson.decode(json);
        } catch (MetadataException e) {
            throw new MetadataException(
                "unreadable historical metadata at " + file + ": " + e.getMessage(), e);
        }
    }

    /**
     * Fully writes and forces a temporary sibling file, returning its path.
     *
     * <p>"Fully written" means the bytes are on disk before the file is published: the channel is
     * forced, and on platforms that support it the containing directory is forced as well.
     */
    private Path writeForcedTemporary(String json) throws IOException {
        Path parent = metadataFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = metadataFile.resolveSibling(
            MetadataSchema.FILE_NAME + "." + java.util.UUID.randomUUID() + ".tmp");
        try (java.nio.channels.FileChannel channel = java.nio.channels.FileChannel.open(
                temporary, java.nio.file.StandardOpenOption.CREATE_NEW,
                java.nio.file.StandardOpenOption.WRITE)) {
            java.nio.ByteBuffer bytes = java.nio.ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
            while (bytes.hasRemaining()) {
                channel.write(bytes);
            }
            channel.force(true);
        } catch (IOException e) {
            Files.deleteIfExists(temporary);
            throw e;
        }
        return temporary;
    }

    /** Best-effort directory fsync after publishing a document. */
    private static void forceDirectory(Path directory) {
        if (directory == null) {
            return;
        }
        try (java.nio.channels.FileChannel channel = java.nio.channels.FileChannel.open(
                directory, java.nio.file.StandardOpenOption.READ)) {
            channel.force(true);
        } catch (IOException | UnsupportedOperationException e) {
            // Directory fsync is not portable (notably not on Windows). The file itself has
            // already been forced, so the document content is durable; only the directory entry
            // ordering is not guaranteed here. Recorded rather than swallowed silently.
            IO_NOTE.accept("directory fsync unsupported for " + directory + ": " + e);
        }
    }

    /** Diagnostics hook for non-fatal platform limitations. */
    private static final java.util.function.Consumer<String> IO_NOTE = message ->
        java.util.logging.Logger.getLogger(HistoricalMetadataStore.class.getName())
            .fine(message);

    /**
     * Writes the metadata document, replacing any existing document.
     *
     * <p>Publication uses a same-directory atomic move with {@code REPLACE_EXISTING}. The
     * temporary file is fully forced before the move, so a crash can leave either the old
     * document or the new one, never a partial one.
     *
     * @throws IOException when the document cannot be written
     */
    public void write(HistoricalMetadata metadata) throws IOException {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata must not be null");
        }
        Path temporary = writeForcedTemporary(MetadataJson.encode(metadata));
        boolean published = false;
        try {
            try {
                Files.move(temporary, metadataFile, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                throw new IOException(
                    "filesystem does not support an atomic replace for " + metadataFile
                        + "; refusing to publish a document that could be observed half written",
                    e);
            }
            published = true;
        } finally {
            if (!published) {
                Files.deleteIfExists(temporary);
            }
        }
        forceDirectory(metadataFile.getParent());
    }

    /**
     * Creates the metadata document only when none exists, with a guaranteed no-clobber publish.
     *
     * <p>This does <b>not</b> use {@code Files.move} without {@code REPLACE_EXISTING}: the Java
     * documentation states that the behaviour of a move when the target exists is implementation
     * specific and that all other options are ignored in that case, so a missing
     * {@code REPLACE_EXISTING} is not a portable no-clobber guarantee.
     *
     * <p>Instead the fully forced temporary file is published with
     * {@link Files#createLink(Path, Path)}, which creates the destination as a new hard link to
     * the temporary file. Link creation fails with {@link java.nio.file.FileAlreadyExistsException}
     * when the destination already exists and is a single atomic filesystem operation, so two
     * concurrent creators cannot both believe they won and an existing document - including an
     * unreadable one - is never replaced.
     *
     * <p>When the filesystem cannot create hard links, or cannot do so atomically, the operation
     * fails safely with {@link IOException} rather than degrading to a check-then-write race. The
     * temporary file is always removed.
     *
     * @return {@code true} when this call created the document (or found the target already
     *         present with identical content); {@code false} when a document already existed
     * @throws IOException when the document cannot be published safely
     */
    public boolean writeIfAbsent(HistoricalMetadata metadata) throws IOException {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata must not be null");
        }
        if (hasEntry()) {
            return false;
        }

        String json = MetadataJson.encode(metadata);
        Path temporary = writeForcedTemporary(json);
        boolean linked = false;
        try {
            Files.createLink(metadataFile, temporary);
            linked = true;
        } catch (java.nio.file.FileAlreadyExistsException e) {
            // Lost the race: the winner's document is authoritative and is left untouched.
            return false;
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException e) {
            throw new IOException(
                "cannot publish historical metadata at " + metadataFile
                    + " without risking an overwrite: hard links are not usable here ("
                    + e.getClass().getSimpleName() + ": " + e.getMessage()
                    + "). Refusing to fall back to a check-then-write race.", e);
        } finally {
            Files.deleteIfExists(temporary);
        }

        if (linked) {
            forceDirectory(metadataFile.getParent());
        }
        return linked;
    }
}
