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
package net.historicalexperience.world.upgrade;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import net.historicalexperience.profile.HistoricalProfiles;
import net.historicalexperience.world.metadata.HistoricalIdentifier;
import net.historicalexperience.world.metadata.HistoricalMetadata;
import net.historicalexperience.world.metadata.HistoricalMetadataStore;
import net.historicalexperience.world.metadata.MetadataException;
import net.historicalexperience.world.metadata.MetadataRevision;
import net.historicalexperience.world.metadata.MetadataSchema;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * Moves a historical world's persisted generation revision forward, safely.
 *
 * <h2>What an upgrade is allowed to change</h2>
 *
 * <p>Only two things: the metadata document, and the backup/journal it writes itself. It never touches a
 * region file, so existing chunks, builds, inventories and entities cannot be affected by it - and the
 * unit test asserts that by hashing region files across a real upgrade. The new revision therefore
 * applies to <b>future</b> chunks only, which is the whole point: a world records which recipe generated
 * its existing terrain, and that record must not be rewritten after the fact.
 *
 * <h2>Ordering, and why it is this order</h2>
 *
 * <ol>
 *   <li><b>Refuse anything unsafe</b> before writing a byte: an ordinary world, an unknown target, a
 *       target that is not strictly newer, generation in flight, or an unfinished previous upgrade.</li>
 *   <li><b>Back up and verify</b> the files the upgrade could affect.</li>
 *   <li><b>Write the journal</b> carrying the complete intended metadata, and force it. This is the
 *       commit point: before it, the world is untouched; after it, recovery can always finish the
 *       job.</li>
 *   <li><b>Publish the metadata</b> atomically, through the same store that guards every other write.</li>
 *   <li><b>Remove the journal.</b> That is what marks the upgrade complete.</li>
 * </ol>
 *
 * <p>The phase boundary is injectable so the crash windows can be tested for real rather than argued
 * about: a test can stop after step 2, or after step 4, and then run recovery on the resulting
 * directory. That is why {@link #stopAfter} exists in the public API.
 */
public final class GenerationUpgrade {
    /** Where an upgrade run stopped. */
    public enum Phase {
        /** Nothing was written. */
        NONE,
        /**
         * The backup exists and was verified, and <b>no journal was written</b>.
         *
         * <p>This window needs no recovery: the world's document is untouched, so the worst outcome is an
         * unused backup directory. It is a separate phase precisely so that property can be tested.
         */
        BACKUP_WRITTEN,
        /**
         * The journal is on disk and the metadata is <b>not</b> published.
         *
         * <p>This is the crash window the whole scheme exists for: the run has committed to a change, and
         * recovery must finish it from the journal.
         */
        JOURNAL_WRITTEN,
        /**
         * The stored generator has been advanced and the metadata is <b>not</b> published.
         *
         * <p>The dangerous window: the world will now generate with the new recipe while its metadata
         * still records the old one. Recovery exists to close exactly this gap, and the level-stem
         * rewrite is idempotent so replaying it is safe.
         */
        PRESET_ADVANCED,
        /** The metadata is published; the journal remains. */
        METADATA_APPLIED,
        /** The journal is gone and the upgrade is finished. */
        COMPLETE
    }

    /** What an upgrade did. */
    public record Result(
        Phase reached,
        String backupDirectory,
        HistoricalMetadata before,
        HistoricalMetadata after
    ) {
    }

    /** The files an upgrade backs up before changing anything. */
    private static final List<String> BACKED_UP_FILES = List.of(
        MetadataSchema.FILE_NAME,
        // On 1.21.1 level.dat itself contains the generator. Back it up once, not twice.
        WorldGenSettingsAdvance.FILE_NAME);

    private GenerationUpgrade() {
    }

    /**
     * Plans an upgrade to a declared version's newest revision, without touching anything.
     *
     * <p>Planning is separate from performing so a caller can show the user what would change, and so
     * the refusals can be tested without a world on disk.
     *
     * @param current  the world's metadata
     * @param targetId the declared version to move to
     * @param actor    attribution recorded in the new revision's history entry
     * @param timestamp epoch milliseconds for the history entry
     */
    public static HistoricalMetadata plan(
        HistoricalMetadata current,
        String targetId,
        String actor,
        long timestamp
    ) throws UpgradeException {
        HistoricalVersion version = HistoricalVersions.byId(targetId)
            .orElseThrow(() -> new UpgradeException(
                "no declared historical version has the id '" + targetId
                    + "'; an upgrade target must be declared in the version registry"));
        HistoricalVersion.GenerationRevision revision = version.latestRevision();
        HistoricalIdentifier targetPreset = HistoricalIdentifier.parse(revision.alias());

        // A world already on the target recipe has nothing to do. Reporting that is better than
        // performing a no-op upgrade that appends a meaningless history entry.
        if (current.generationPreset().equals(targetPreset)) {
            throw new UpgradeException(
                "this world already uses " + targetPreset + " at revision " + current.revision());
        }

        // "Downgrade" is judged WITHIN a version, by generation revision number, and only there. Moving
        // between versions is a deliberate operator choice that may legitimately go either way - Beta
        // 1.7.3 to Release 1.2.5 is not a downgrade, it is a different world - so refusing it would be
        // overreach. Two identifiers that look alike must not be confused here:
        //   * HistoricalMetadata.revision is a monotonic COUNTER of changes to the document (1, 2, 3...)
        //   * GenerationRevision.revision is the number of a GENERATION RECIPE within a version (0..5)
        int currentGenerationRevision = generationRevisionNumber(current.generationPreset());
        if (currentGenerationRevision >= 0
                && version.id().equals(current.profileId().value())
                && revision.revision() < currentGenerationRevision) {
            throw new UpgradeException("refusing to move " + current.generationPreset() + " back to "
                + targetPreset + ": generation revision " + revision.revision()
                + " is older than the world's generation revision " + currentGenerationRevision);
        }

        // The target profile is taken from the BINDING for the target preset, not from the version id.
        // That is what makes the resulting document consistent by construction: the profile that a
        // preset is bound to is exactly the profile the loader will resolve for it, so the two halves of
        // the document cannot be written from different sources and then disagree.
        HistoricalIdentifier targetProfile = HistoricalProfiles.byGenerationPreset(targetPreset)
            .map(binding -> binding.profile().id())
            .orElseThrow(() -> new UpgradeException(
                "no generation binding is declared for " + targetPreset
                    + ", so the profile a world would have to become cannot be determined"));

        boolean sameVersion = current.profileId().equals(targetProfile);
        String note = sameVersion
            ? "generation revision upgrade to " + targetPreset + " (" + revision.description() + ")"
            : "version upgrade to " + targetProfile + " on generation recipe " + targetPreset
                + " (" + revision.description() + ")";
        HistoricalMetadata target = sameVersion
            // Within a version the profile stays put and only the recipe moves.
            ? current.advance(targetPreset, actor, note, timestamp)
            // Across versions BOTH move. Advancing the recipe alone produced a document that claimed to
            // be one version while generating another, which the next start refused to load - correctly,
            // since the world would otherwise have kept the old version's gameplay rules on new terrain.
            : current.advanceVersion(targetProfile, targetPreset, actor, note, timestamp);
        // Re-checked here rather than assumed: the new document must still be a valid metadata value,
        // including its append-only history.
        requireConsistent(target, current.revision());
        return target;
    }

    /** The generation revision number a persisted recipe alias declares, or -1 when it is not ours. */
    private static int generationRevisionNumber(HistoricalIdentifier preset) {
        return HistoricalVersions.byRevisionAlias(preset.value())
            .flatMap(version -> version.revisions().stream()
                .filter(revision -> revision.alias().equals(preset.value()))
                .findFirst())
            .map(HistoricalVersion.GenerationRevision::revision)
            .orElse(-1);
    }

    /**
     * Records an upgrade's intent and publishes the new metadata, <b>without</b> advancing the stored
     * generator yet.
     *
     * <p>This split exists because of a measured fact rather than a theory. The server reads its level
     * stem into memory before {@code SERVER_STARTING} fires and re-writes that file when it saves, so a
     * generator advance performed during a running session is silently overwritten afterwards: the first
     * version of the startup recovery did exactly that, logged success, and left the metadata claiming a
     * preset the world was not generating with.
     *
     * <p>So the advance is deferred to {@link #applyPendingAdvance}, which runs after the server has
     * stopped. The metadata may safely be published now, because chunk generation resolves its recipe
     * from the loaded generator and not from this document - the record running ahead of the file is
     * not observable in the world, whereas the file running ahead of the record was a real
     * inconsistency.
     *
     * @return the phase reached, which is {@link Phase#METADATA_APPLIED} while the advance is pending
     */
    public static Result stage(
        Path worldDirectory,
        HistoricalMetadata target,
        boolean generationActive,
        long timestamp
    ) throws UpgradeException {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        HistoricalMetadata before = requireUpgradable(worldDirectory, store, target, generationActive);

        String backupDirectory = WorldBackup.create(
            worldDirectory, BACKED_UP_FILES, before.revision(), target.revision(), timestamp);
        WorldBackup.verify(worldDirectory, backupDirectory);

        UpgradeJournal journal = new UpgradeJournal(
            UpgradeJournal.CURRENT_SCHEMA,
            UpgradeJournal.Phase.BACKUP_WRITTEN,
            timestamp,
            backupDirectory,
            before.generationPreset().value(),
            net.historicalexperience.world.metadata.MetadataJson.encode(target));
        journal.writeTo(worldDirectory);

        try {
            store.write(target);
        } catch (IOException e) {
            throw new UpgradeException(
                "the upgrade journal is in place but the metadata could not be published; recovery will "
                    + "finish it on the next start", e);
        }
        return new Result(Phase.METADATA_APPLIED, backupDirectory, before, target);
    }

    /**
     * Applies the pending generator advance and completes the upgrade.
     *
     * <p>Called after the server has stopped, which is the only point at which the rewrite is not
     * immediately undone by the game's own save. Idempotent, so a crash between the rewrite and the
     * journal's removal is repaired by running it again.
     *
     * @return what it did, or empty when no upgrade was pending
     */
    public static Optional<Result> applyPendingAdvance(Path worldDirectory) throws UpgradeException {
        if (!UpgradeJournal.existsIn(worldDirectory)) {
            return Optional.empty();
        }
        UpgradeJournal journal = UpgradeJournal.readFrom(worldDirectory);
        HistoricalMetadata target;
        try {
            target = journal.targetMetadata();
        } catch (MetadataException e) {
            // The journal cannot be read, so its intent cannot be honoured. Leave it and everything else
            // exactly as they are: it is the only record of what was meant.
            throw new UpgradeException(
                "the pending upgrade's journal names a metadata document this build cannot read; nothing "
                    + "was changed and the journal was kept", e);
        }

        // Verify the recovery point before changing anything, exactly as the startup path does: a
        // roll-forward onto a backup that cannot be shown to be intact is how a bad night becomes an
        // unrecoverable one.
        WorldBackup.verify(worldDirectory, journal.backupDirectory());

        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        HistoricalMetadata current;
        try {
            current = store.read();
        } catch (MetadataException | IOException e) {
            throw new UpgradeException("could not read the world's metadata before advancing", e);
        }
        // The "from" preset comes from the JOURNAL, never from the metadata: recovery may already have
        // published the destination document, so the metadata can name the target while the level stem
        // still holds the source. Reading it from the metadata produced exactly that mistake - the first
        // version of this method refused every deferred advance with "the file names X but the upgrade
        // expected Y", where both X and Y were wrong for the same reason.
        WorldGenSettingsAdvance.advance(worldDirectory, journal.fromPreset(),
            target.generationPreset().value());

        UpgradeJournal.deleteFrom(worldDirectory);
        return Optional.of(new Result(Phase.COMPLETE, journal.backupDirectory(), current, target));
    }

    private static HistoricalMetadata requireUpgradable(
        Path worldDirectory,
        HistoricalMetadataStore store,
        HistoricalMetadata target,
        boolean generationActive
    ) throws UpgradeException {
        if (generationActive) {
            throw new UpgradeException(
                "refusing to upgrade while terrain generation is in flight; the new revision must apply "
                    + "to future chunks from a known boundary, not to a partially generated region");
        }
        if (UpgradeJournal.existsIn(worldDirectory)) {
            throw new UpgradeException(
                "an unfinished upgrade journal exists for this world; recover it before starting another "
                    + "upgrade so the two cannot interleave");
        }
        HistoricalMetadata before;
        try {
            before = store.read();
        } catch (MetadataException | IOException e) {
            throw new UpgradeException("cannot upgrade a world whose metadata cannot be read", e);
        }
        if (target.revision() <= before.revision()) {
            throw new UpgradeException("target revision " + target.revision()
                + " is not newer than the world's revision " + before.revision());
        }
        for (String relative : BACKED_UP_FILES) {
            if (!Files.isRegularFile(worldDirectory.resolve(relative))) {
                throw new UpgradeException(
                    "refusing to upgrade: " + relative + " is missing, so the world could not be backed "
                        + "up before the change");
            }
        }
        var stored = WorldGenSettingsAdvance.read(worldDirectory).orElseThrow(() ->
            new UpgradeException("refusing to stage: the saved world has no recognised Moderner Beta generator"));
        if (!stored.alreadyIs(before.generationPreset().value())) {
            throw new UpgradeException("refusing to stage: metadata names " + before.generationPreset()
                + " but the saved generator names " + stored.asList() + "; nothing was changed");
        }
        return before;
    }

    /**
     * Performs an upgrade in one pass, including the generator advance.
     *
     * <p>Correct only when nothing else has the world open - offline tooling, and the unit tests. A
     * running server must use {@link #stage} and {@link #applyPendingAdvance} instead, or its own save
     * will undo the advance.
     *
     * @param worldDirectory  the world to upgrade
     * @param target          the metadata to publish, normally from {@link #plan}
     * @param generationActive whether terrain generation is in flight for this world
     * @param timestamp       epoch milliseconds, used for the backup label
     * @param stopAfter       how far to go; {@link Phase#COMPLETE} performs the whole upgrade
     */
    public static Result perform(
        Path worldDirectory,
        HistoricalMetadata target,
        boolean generationActive,
        long timestamp,
        Phase stopAfter
    ) throws UpgradeException {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);

        // --- 1. refuse before writing anything ---------------------------------------------------
        if (generationActive) {
            // "Drained generation" is a precondition, not a preference. Publishing a new revision while
            // chunks are being generated would let some chunks of one region be generated by the old
            // recipe and the rest by the new one, with no record of where the boundary fell.
            throw new UpgradeException(
                "refusing to upgrade while terrain generation is in flight; the new revision must apply "
                    + "to future chunks from a known boundary, not to a partially generated region");
        }
        if (UpgradeJournal.existsIn(worldDirectory)) {
            throw new UpgradeException(
                "an unfinished upgrade journal exists for this world; recover it before starting another "
                    + "upgrade so the two cannot interleave");
        }
        HistoricalMetadata before;
        try {
            before = store.read();
        } catch (MetadataException | IOException e) {
            throw new UpgradeException("cannot upgrade a world whose metadata cannot be read", e);
        }
        if (target.revision() <= before.revision()) {
            throw new UpgradeException("target revision " + target.revision()
                + " is not newer than the world's revision " + before.revision());
        }
        for (String relative : BACKED_UP_FILES) {
            if (!Files.isRegularFile(worldDirectory.resolve(relative))) {
                throw new UpgradeException(
                    "refusing to upgrade: " + relative + " is missing, so the world could not be backed "
                        + "up before the change");
            }
        }

        // --- 2. back up and verify ---------------------------------------------------------------
        String backupDirectory = WorldBackup.create(
            worldDirectory, BACKED_UP_FILES, before.revision(), target.revision(), timestamp);
        WorldBackup.verify(worldDirectory, backupDirectory);
        if (stopAfter == Phase.BACKUP_WRITTEN) {
            return new Result(Phase.BACKUP_WRITTEN, backupDirectory, before, target);
        }

        // --- 3. write the journal (the commit point) ---------------------------------------------
        UpgradeJournal journal = new UpgradeJournal(
            UpgradeJournal.CURRENT_SCHEMA,
            UpgradeJournal.Phase.BACKUP_WRITTEN,
            timestamp,
            backupDirectory,
            before.generationPreset().value(),
            net.historicalexperience.world.metadata.MetadataJson.encode(target));
        journal.writeTo(worldDirectory);
        if (stopAfter == Phase.JOURNAL_WRITTEN) {
            return new Result(Phase.JOURNAL_WRITTEN, backupDirectory, before, target);
        }

        // --- 4. advance the stored generator, which is what actually changes generation -----------
        // Without this step an "upgrade" would only move a bookkeeping record while the world kept
        // generating exactly as before - which is worse than no upgrade, because it would look like one.
        // This is also why the change is applied at startup rather than mid-session: the game reads this
        // file while loading the world, so the new preset takes effect from the next session's first
        // chunk, with no partially generated region straddling two recipes.
        WorldGenSettingsAdvance.advance(worldDirectory, before.generationPreset().value(),
            target.generationPreset().value());
        if (stopAfter == Phase.PRESET_ADVANCED) {
            return new Result(Phase.PRESET_ADVANCED, backupDirectory, before, target);
        }

        // --- 5. publish the metadata atomically --------------------------------------------------
        try {
            store.write(target);
        } catch (IOException e) {
            // The journal is deliberately LEFT IN PLACE. Recovery will verify the backup and roll the
            // metadata forward, which is safer than unwinding a change that may have partly landed.
            throw new UpgradeException(
                "the upgrade journal is in place but the metadata could not be published; run recovery "
                    + "to finish or roll forward from the backup", e);
        }
        UpgradeJournal.readFrom(worldDirectory).withPhase(UpgradeJournal.Phase.METADATA_APPLIED)
            .writeTo(worldDirectory);
        if (stopAfter == Phase.METADATA_APPLIED) {
            return new Result(Phase.METADATA_APPLIED, backupDirectory, before, target);
        }

        // --- 6. remove the journal: the upgrade is complete --------------------------------------
        UpgradeJournal.deleteFrom(worldDirectory);
        return new Result(Phase.COMPLETE, backupDirectory, before, target);
    }

    /**
     * Resolves an upgrade that was interrupted, which is what makes the whole scheme restart-safe.
     *
     * @return what recovery did, or empty when there was nothing to recover
     */
    public static Optional<UpgradeRecovery.Result> recover(Path worldDirectory) throws UpgradeException {
        if (!UpgradeJournal.existsIn(worldDirectory)) {
            return Optional.empty();
        }
        return Optional.of(UpgradeRecovery.recover(worldDirectory));
    }

    /** The files an upgrade backs up, exposed so a test can assert the list rather than trust it. */
    public static List<String> backedUpFiles() {
        return BACKED_UP_FILES;
    }

    private static void requireConsistent(HistoricalMetadata metadata, long previousRevision)
            throws UpgradeException {
        if (metadata.revision() != previousRevision + 1) {
            throw new UpgradeException("planned document revision " + metadata.revision()
                + " does not follow the world's revision " + previousRevision
                + "; the change counter must advance by exactly one");
        }
        List<MetadataRevision> history = metadata.history();
        if (history.isEmpty() || history.get(history.size() - 1).revision() != metadata.revision()) {
            throw new UpgradeException(
                "planned metadata's history does not end at its revision, so the append-only record "
                    + "would be inconsistent");
        }
    }
}
