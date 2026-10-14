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
import java.nio.file.Path;

import net.historicalexperience.world.metadata.HistoricalMetadata;
import net.historicalexperience.world.metadata.HistoricalMetadataStore;
import net.historicalexperience.world.metadata.MetadataException;
import net.historicalexperience.world.metadata.MetadataJson;

/**
 * Finishes or discards an upgrade that was interrupted, so an upgrade is restart-safe.
 *
 * <h2>Why roll forward rather than roll back</h2>
 *
 * <p>The journal carries the complete intended metadata document, so recovery can always finish exactly
 * what the interrupted run was going to do. Rolling back would need the same information plus a
 * judgement about whether a half-applied change is better undone - and the backup exists either way, so
 * the operator keeps the option. Rolling forward is the choice that leaves the world in a state the
 * journal can prove was intended.
 *
 * <p>Recovery verifies the backup's SHA-256 manifest <b>before</b> it applies anything. If the backup is
 * not intact, recovery refuses and says so: a roll-forward onto an unverifiable recovery point is how a
 * bad night becomes an unrecoverable one.
 *
 * <p>Every outcome is idempotent. Running recovery twice, or on a world with no journal, does nothing
 * the second time - the test asserts that, because a recovery path that is only safe once is not safe.
 */
public final class UpgradeRecovery {
    /** What recovery found and did. */
    public enum Outcome {
        /**
         * The metadata step had already landed; only the generator advance remains.
         *
         * <p>The journal is deliberately KEPT in this case. Removing it would forget that a generator
         * advance is still pending, and the world would keep generating with the old recipe forever
         * while its metadata claimed otherwise.
         */
        AWAITING_GENERATOR_ADVANCE,
        /** The metadata was published from the journal; the generator advance is still pending. */
        ROLLED_FORWARD,
        /** The journal named a document this build cannot read; nothing was changed. */
        REFUSED_UNREADABLE_TARGET
    }

    /**
     * The result of a recovery.
     *
     * @param outcome        what happened
     * @param metadata       the world's metadata afterwards, or null when nothing was applied
     * @param backupVerified true when the backup manifest was verified before applying
     * @param detail         a sentence for the log or the operator
     */
    public record Result(
        Outcome outcome,
        HistoricalMetadata metadata,
        boolean backupVerified,
        String detail
    ) {
    }

    private UpgradeRecovery() {
    }

    /** Reads the journal, decides, and acts. Never leaves the journal behind on success. */
    public static Result recover(Path worldDirectory) throws UpgradeException {
        UpgradeJournal journal = UpgradeJournal.readFrom(worldDirectory);

        HistoricalMetadata target;
        try {
            target = journal.targetMetadata();
        } catch (MetadataException e) {
            // The journal is present but unreadable. Leave it exactly as it is: it is the only record of
            // what was intended, and deleting it would destroy the evidence an operator needs.
            return new Result(Outcome.REFUSED_UNREADABLE_TARGET, null, false,
                "the upgrade journal names a metadata document this build cannot read (" + e.getMessage()
                    + "); nothing was changed and the journal was kept");
        }

        // Verify the recovery point BEFORE applying anything.
        int verified = WorldBackup.verify(worldDirectory, journal.backupDirectory());

        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        HistoricalMetadata current;
        try {
            current = store.read();
        } catch (MetadataException | IOException e) {
            throw new UpgradeException(
                "recovery could not read the world's current metadata; nothing was changed", e);
        }

        if (current.equals(target)) {
            // The metadata step landed before the crash. The generator advance is still outstanding, and
            // the journal must stay so it is not forgotten; GenerationUpgrade.applyPendingAdvance applies
            // it once the server has stopped.
            return new Result(Outcome.AWAITING_GENERATOR_ADVANCE, current, true,
                "the metadata step had already been applied; the generator advance is still pending and "
                    + "the journal was kept so it is not forgotten. The backup was verified");
        }

        if (current.revision() > target.revision()) {
            // Someone got further than the journal knows about. Refuse rather than overwrite a newer
            // revision with an older document.
            throw new UpgradeException("recovery found revision " + current.revision()
                + " in the world but the journal only describes revision " + target.revision()
                + "; refusing to overwrite newer metadata");
        }

        // Advance the stored generator BEFORE publishing the metadata, mirroring the order an
        // uninterrupted run uses.
        //
        // NO IT DOES NOT - and the reason is worth keeping. This step used to live here, and a measured
        // run showed it was wrong: the server has already read its level stem into memory by the time
        // this runs, and it re-writes that file when it saves, so the advance was silently undone while
        // this method logged success and the metadata went on claiming a preset the world was not
        // generating with. The advance now happens in GenerationUpgrade.applyPendingAdvance, after the
        // server has stopped, and the journal is deliberately KEPT until then so the pending advance is
        // not forgotten.
        try {
            store.write(target);
        } catch (IOException e) {
            throw new UpgradeException("recovery could not publish the journal's metadata document", e);
        }
        return new Result(Outcome.ROLLED_FORWARD, target, true,
            "published revision " + target.revision() + " from the journal after verifying " + verified
                + " backed-up file(s). The generator advance is still pending and will be applied after "
                + "the server stops, because a rewrite made while it is running is undone by its own "
                + "save; the backup was kept");
    }
}
