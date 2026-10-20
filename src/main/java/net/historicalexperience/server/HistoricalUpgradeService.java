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

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import net.historicalexperience.world.metadata.HistoricalMetadata;
import net.historicalexperience.world.metadata.HistoricalMetadataStore;
import net.historicalexperience.world.metadata.MetadataException;
import net.historicalexperience.world.upgrade.GenerationUpgrade;
import net.historicalexperience.world.upgrade.UpgradeException;
import net.historicalexperience.world.upgrade.UpgradeJournal;
import net.historicalexperience.world.upgrade.UpgradeRecovery;
import net.historicalexperience.world.upgrade.WorldGenSettingsAdvance;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * The operator-facing half of a generation upgrade, kept apart from the command that calls it.
 *
 * <p>The command layer is deliberately thin: it collects arguments and renders text. Everything that
 * decides anything lives here, so it can be unit-tested against a real world directory without a server,
 * and so the same operations are reachable from the runtime probe and from any future interface without
 * duplicating the rules.
 *
 * <p>These operations are operator actions on a world someone has built in, so each one answers in words
 * rather than in a boolean: what happened, what it means, and what the operator should expect next.
 */
public final class HistoricalUpgradeService {
    private HistoricalUpgradeService() {
    }

    /**
     * A report rendered for a human.
     *
     * @param ok      whether the action succeeded
     * @param summary one line naming the outcome
     * @param details further lines, each already readable
     */
    public record Report(boolean ok, String summary, java.util.List<String> details) {
        public Report {
            details = java.util.List.copyOf(details);
        }

        public static Report failure(String summary) {
            return new Report(false, summary, java.util.List.of());
        }

        public static Report failure(String summary, java.util.List<String> details) {
            return new Report(false, summary, details);
        }
    }

    /**
     * Describes the world's historical state: which version it declares, at which revision, what that
     * version's rules are, and whether an upgrade is waiting to be applied.
     */
    public static Report describe(Path worldDirectory) {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        if (!store.hasEntry()) {
            return Report.failure("This world is not historical, so it has no version or revision.",
                java.util.List.of(
                    "Create a world with one of this mod's world presets to make it historical."));
        }
        HistoricalMetadata metadata;
        try {
            metadata = store.read();
        } catch (MetadataException | IOException e) {
            return Report.failure("This world's metadata cannot be read, so nothing can be reported.",
                java.util.List.of(String.valueOf(e.getMessage())));
        }

        java.util.List<String> details = new java.util.ArrayList<>();
        details.add("profile: " + metadata.profileId());
        if (!metadata.snapshotPatchId().isEmpty()) details.add("snapshot patch: " + metadata.snapshotPatchId());
        details.add("generation preset: " + metadata.generationPreset());
        details.add("document revision: " + metadata.revision()
            + " (a counter of changes to the record, not a generation recipe number)");

        HistoricalVersions.byId(metadata.profileId().value()).ifPresentOrElse(version -> {
            details.add("declared version: " + version.displayName() + " (" + version.era() + ")");
            details.add("its newest generation recipe is revision "
                + version.latestRevision().revision() + ": " + version.latestRevision().alias());
            details.add("rules: sprints=" + version.rules().allowsSprinting()
                + " hunger=" + version.rules().hasHunger()
                + " experience=" + version.rules().awardsExperience()
                + " structures=" + version.rules().hasStructures()
                + " spawns=" + (version.rules().restrictsSpawns()
                    ? version.rules().allowedNaturalSpawns().size() + " allowed" : "unrestricted")
                + " foodHeals=" + version.rules().foodHealPoints().size());
        }, () -> details.add("declared version: NOT DECLARED by this build, so its rules cannot be "
            + "described. Its worlds still load: an unknown version is left alone rather than guessed "
            + "at."));

        try {
            var stored = WorldGenSettingsAdvance.read(worldDirectory);
            stored.ifPresent(state -> details.add("stored generator: " + state.asList()
                + (state.consistent() ? "" : "  (INCONSISTENT - terrain and cave biomes disagree)")));
        } catch (UpgradeException e) {
            details.add("stored generator: could not be read: " + e.getMessage());
        }

        try {
            if (UpgradeJournal.existsIn(worldDirectory)) {
                var journal = UpgradeJournal.readFrom(worldDirectory);
                details.add("PENDING UPGRADE: a staged upgrade is waiting to be applied after the server "
                    + "stops. Its result will be generation recipe " + journal.fromPreset() + " -> "
                    + journal.targetMetadata().generationPreset() + ".");
            }
        } catch (UpgradeException | MetadataException e) {
            details.add("PENDING UPGRADE: a journal exists but cannot be read: " + e.getMessage());
        }

        // What the version-gated behaviour is actually keyed on. Several rules - old damage values, the 1.9
        // cooldown and sweep, sprinting beyond the server - read the active session rather than a level, so a
        // session that was never set makes them silently inert. Reporting it turns "the fix did not work" into
        // an answerable question.
        String session = net.historicalexperience.version.ActiveVersion.current();
        details.add("active session: " + (session == null
            ? "<NONE - session-gated behaviour is inert in this process>"
            : session));
        if (session != null) {
            details.add("combat gates for this session: cooldownSuppressed="
                + Features.active(Feature.NO_ATTACK_COOLDOWN, session)
                + " sweepSuppressed="
                + Features.active(Feature.NO_SWEEP_ATTACK, session)
                + " oldDamageValues="
                + net.historicalexperience.version.ActiveVersion.isHistorical());
        }
        net.historicalexperience.gameplay.HistoricalCombatDiagnostics.report()
            .forEach((name, value) -> details.add("combat hook counter: " + name + "=" + value));
        return new Report(true, "This world is " + metadata.profileId(), details);
    }

    /**
     * Turns a bare path into a full id, so both spellings of a version work in the command.
     *
     * <p>`release_1_2_5` and `historical_experience:release_1_2_5` are the same version, and requiring the
     * namespace is the kind of needless friction that makes an operator mistype a target. Anything that
     * already carries a namespace, including another project's, is passed through untouched so it still
     * fails with a clear message rather than being silently rewritten into one of ours.
     */
    static String qualify(String targetId) {
        if (targetId == null) {
            return null;
        }
        String trimmed = targetId.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed.indexOf(':') >= 0 ? trimmed : "historical_experience:" + trimmed;
    }

    /**
     * Stages an upgrade to a declared version's newest generation recipe.
     *
     * <p>Staging writes the backup, the journal and the new metadata, and does <b>not</b> change the
     * stored generator. That happens when the server stops, because a rewrite made while it is running
     * is undone by its own save. The report says so, because an operator who expects the change
     * immediately would otherwise conclude it failed.
     */
    public static Report upgrade(Path worldDirectory, String targetId, String actor, long timestamp) {
        targetId = qualify(targetId);
        if (net.historicalexperience.version.HistoricalSnapshotPatches.baseProfileForSelection(targetId).isEmpty()) {
            return Report.failure("There is no declared historical version called '" + targetId + "'.",
                java.util.List.of(declaredVersions()));
        }
        try {
            HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
            HistoricalMetadata current = store.read();
            HistoricalMetadata target = GenerationUpgrade.plan(current, targetId, actor, timestamp);
            var result = GenerationUpgrade.stage(worldDirectory, target, false, timestamp);

            return new Report(true, "Staged an upgrade of this world to " + targetId, java.util.List.of(
                "generation recipe: " + result.before().generationPreset() + " -> "
                    + result.after().generationPreset(),
                "document revision: " + result.before().revision() + " -> "
                    + result.after().revision(),
                "backup: " + result.backupDirectory() + " (verified against its SHA-256 manifest)",
                "WHAT HAPPENS NOW: the stored generator is deliberately NOT changed yet. It is rewritten "
                    + "when the server stops, because the server has already read it and would overwrite "
                    + "a change made now. The next start will generate with the new recipe.",
                "Existing chunks are never rewritten. The new recipe applies only to chunks generated "
                    + "after it takes effect, so builds, inventories and entities are untouched."));
        } catch (UpgradeException e) {
            boolean pending = UpgradeJournal.existsIn(worldDirectory);
            return Report.failure("The upgrade did not complete: " + e.getMessage(),
                java.util.List.of(pending
                    ? "A pending journal remains. Do not stage another upgrade; resolve the error and "
                        + "restart to recover under the world lock before its generator is loaded."
                    : "No destination generation recipe was applied. Existing chunks are unchanged; "
                        + "a verified backup may remain if a later transaction step failed."));
        } catch (MetadataException | IOException e) {
            return Report.failure("This world's metadata could not be read, so it cannot be upgraded.",
                java.util.List.of(String.valueOf(e.getMessage())));
        }
    }

    /**
     * What a world looks like before it is opened, for an interface that must show or change it.
     *
     * <p>Structured rather than prose, because a screen has to act on it - disable a control, show a warning -
     * and parsing sentences to decide that would be a second implementation of these rules.
     *
     * @param kind             whether the world is ordinary, historical, or cannot be understood
     * @param profileId        the declared version id, or blank when there is none
     * @param displayName      the version's display name, or blank when there is none
     * @param generationPreset the stored generation recipe alias, or blank when there is none
     * @param documentRevision the metadata document's change counter, or 0 when there is none
     * @param pendingUpgrade   whether a staged upgrade is waiting to be applied on the next load
     * @param problem          why the world cannot be understood, or why it cannot be changed
     */
    public record WorldVersionState(Kind kind, String profileId, String displayName, String generationPreset,
            long documentRevision, boolean pendingUpgrade, String problem, String snapshotPatchId) {
        public WorldVersionState(Kind kind, String profileId, String displayName, String generationPreset,
                                 long documentRevision, boolean pendingUpgrade, String problem) {
            this(kind, profileId, displayName, generationPreset, documentRevision, pendingUpgrade, problem, "");
        }
        public String selectionId() { return snapshotPatchId.isEmpty() ? profileId : snapshotPatchId; }
        /** The three states the lifecycle itself distinguishes. */
        public enum Kind {
            /** No historical document: an ordinary world, which this mod leaves alone. */
            ORDINARY,
            /** A historical world this build can describe and therefore change. */
            HISTORICAL,
            /** A document this build cannot understand. Never rewritten, and never substituted. */
            UNREADABLE
        }

        /** Whether a version change may be offered for this world. */
        public boolean changeable() {
            return this.kind == Kind.HISTORICAL && !this.pendingUpgrade;
        }
    }

    /**
     * Inspects a world directory without opening it.
     *
     * <p>Uses the lifecycle's own classification rather than a second copy of it, so an interface cannot disagree
     * with the server about what a world is. Reads only; writes nothing.
     */
    public static WorldVersionState inspect(Path worldDirectory) {
        net.historicalexperience.world.HistoricalWorldState state =
            net.historicalexperience.world.HistoricalWorldState.loadAt(worldDirectory);

        if (state instanceof net.historicalexperience.world.HistoricalWorldState.Normal) {
            return new WorldVersionState(WorldVersionState.Kind.ORDINARY, "", "", "", 0, false,
                "This is an ordinary world. It has no historical version, and one cannot be given to it: the "
                    + "generator a world runs on is written when it is created, and this mod never rewrites the "
                    + "terrain settings of a world that already exists.");
        }

        if (state instanceof net.historicalexperience.world.HistoricalWorldState.Unreadable unreadable) {
            return new WorldVersionState(WorldVersionState.Kind.UNREADABLE, "", "", "", 0, false,
                unreadable.reason());
        }

        var historical = (net.historicalexperience.world.HistoricalWorldState.Historical) state;
        HistoricalMetadata metadata = historical.metadata();
        String profileId = metadata.profileId().value();
        String displayName = net.historicalexperience.version.HistoricalSnapshotPatches.byId(metadata.snapshotPatchId())
            .map(patch -> patch.displayName() + " (snapshot patch)")
            .orElseGet(() -> HistoricalVersions.byId(profileId).map(HistoricalVersion::displayName)
                .orElse(profileId + " (not declared by this build)"));
        boolean pending = UpgradeJournal.existsIn(worldDirectory);

        return new WorldVersionState(WorldVersionState.Kind.HISTORICAL, profileId, displayName,
            metadata.generationPreset().value(), metadata.revision(), pending,
            pending
                ? "A staged upgrade is waiting to be applied. It will be completed when this world is next "
                    + "loaded, so another change cannot be staged until then."
                : "", metadata.snapshotPatchId());
    }

    /**
     * Changes the version of a world that is not open, which is what a world-selection interface does.
     *
     * <p>Delegates to {@link #upgrade}, so the backup, journal, revision rules and metadata write are the same
     * ones the command performs. What it adds is the two checks a caller outside the running world must make and
     * an operator inside it must not:
     *
     * <ul>
     *   <li>the world must not be open. The command runs inside the world, where this process holds the lock, so
     *       it cannot ask this question; a selection screen can, and must, because a change made underneath a
     *       running game is overwritten by that game's own save.</li>
     *   <li>the world must be describable. An ordinary world cannot be made historical by an upgrade - the
     *       generator settings of an existing world are never rewritten from scratch - and an unreadable document
     *       is never replaced.</li>
     * </ul>
     *
     * <p>Like the command, this stages the change: metadata now, generator settings on the next load. Existing
     * chunks are never regenerated.
     */
    public static Report upgradeUnloadedWorld(Path worldDirectory, String targetId, String actor, long timestamp) {
        if (appearsOpen(worldDirectory)) {
            return Report.failure("This world appears to be open, so its version was not changed.",
                java.util.List.of(
                    "A world that is running holds its own copy of the generator settings and writes them back "
                        + "when it saves, so a change made now would be silently undone.",
                    "Close the world, or the other Minecraft instance that has it open, and try again."));
        }

        WorldVersionState state = inspect(worldDirectory);
        if (!state.changeable()) {
            return Report.failure("This world's version was not changed: " + state.problem(),
                java.util.List.of(state.kind() == WorldVersionState.Kind.ORDINARY
                    ? "Create a new world with a historical world type to get a world whose version can be "
                        + "changed."
                    : "No file in this world was modified."));
        }

        return upgrade(worldDirectory, targetId, actor, timestamp);
    }

    /**
     * Whether a world looks open, by trying to take the same lock the game takes when it opens one.
     *
     * <p>Vanilla locks {@code session.lock} for as long as a world is open, so failing to lock it means something
     * else has the world. Every failure is read as "open": if this cannot tell, it must not act, because the
     * alternative is writing to a world that a running game will overwrite.
     */
    public static boolean appearsOpen(Path worldDirectory) {
        Path lock = worldDirectory.resolve("session.lock");

        if (!java.nio.file.Files.isRegularFile(lock)) {
            return false;
        }

        try (java.nio.channels.FileChannel channel = java.nio.channels.FileChannel.open(lock,
                java.nio.file.StandardOpenOption.WRITE);
                java.nio.channels.FileLock held = channel.tryLock()) {
            return held == null;
        } catch (java.nio.channels.OverlappingFileLockException alreadyHeldByThisProcess) {
            return true;
        } catch (IOException cannotTell) {
            return true;
        }
    }

    /**
     * Runs recovery now, for an operator who would rather not wait for the next restart.
     *
     * <p>Only ever publishes the metadata half. The generator advance still has to wait for the server to
     * stop, and the report says which half happened so nobody reads a partial result as a complete one.
     */
    public static Report recover(Path worldDirectory) {
        try {
            Optional<UpgradeRecovery.Result> recovered = GenerationUpgrade.recover(worldDirectory);
            if (recovered.isEmpty()) {
                return new Report(true, "There was no interrupted upgrade to recover.",
                    java.util.List.of("Nothing was changed."));
            }
            var result = recovered.get();
            java.util.List<String> details = new java.util.ArrayList<>();
            details.add("outcome: " + result.outcome());
            details.add("backup verified: " + result.backupVerified());
            details.add(result.detail());
            details.add("The generator advance, if one is pending, is applied when the server stops.");
            return new Report(true, "Recovered an interrupted upgrade", details);
        } catch (UpgradeException e) {
            return Report.failure("Recovery was refused: " + e.getMessage(),
                java.util.List.of("Nothing was changed and the journal was kept, because it is the only "
                    + "record of what was intended."));
        }
    }

    /** Every version this build declares, for an operator who guessed a name. */
    public static String declaredVersions() {
        StringBuilder out = new StringBuilder("Declared versions: ");
        java.util.List<String> ids = HistoricalVersions.all().stream()
            .map(HistoricalVersion::id)
            .toList();
        out.append(String.join(", ", ids));
        out.append("; snapshot patches: ").append(String.join(", ", net.historicalexperience.version.HistoricalSnapshotPatches.all()
            .stream().map(net.historicalexperience.version.HistoricalSnapshotPatches.Patch::id).toList()));
        return out.toString();
    }

    /** A compact map for the runtime probe and for logs. */
    public static Map<String, String> diagnostics(Path worldDirectory) {
        Map<String, String> report = new LinkedHashMap<>();
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        report.put("hasEntry", Boolean.toString(store.hasEntry()));
        report.put("journalPending", Boolean.toString(UpgradeJournal.existsIn(worldDirectory)));
        if (store.hasEntry()) {
            try {
                HistoricalMetadata metadata = store.read();
                report.put("profileId", metadata.profileId().value());
                report.put("selectionId", metadata.selectionId());
                report.put("snapshotPatchId", metadata.snapshotPatchId());
                report.put("generationPreset", metadata.generationPreset().value());
                report.put("documentRevision", Long.toString(metadata.revision()));
            } catch (MetadataException | IOException e) {
                report.put("metadataError", e.getClass().getSimpleName());
            }
        }
        try {
            var stored = WorldGenSettingsAdvance.read(worldDirectory);
            stored.ifPresent(state ->
                report.put("storedGenerator", String.join(",", state.asList())));
        } catch (UpgradeException e) {
            report.put("storedGeneratorError", e.getClass().getSimpleName());
        }
        return report;
    }
}
