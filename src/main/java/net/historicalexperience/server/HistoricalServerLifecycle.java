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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.historicalexperience.HistoricalExperience;
import net.historicalexperience.HistoricalExperienceMod;
import net.historicalexperience.HistoricalMarker;
import net.historicalexperience.integration.modernerbeta.ModernerBetaAdapter;
import net.historicalexperience.profile.HistoricalProfiles;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.historicalexperience.world.HistoricalWorldState;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import net.historicalexperience.world.metadata.HistoricalIdentifier;
import net.historicalexperience.world.metadata.HistoricalMetadata;
import net.historicalexperience.world.metadata.HistoricalMetadataStore;
import net.historicalexperience.world.metadata.MetadataException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Server-side historical metadata enforcement.
 *
 * <p>Behaviour on world load, decided by {@link HistoricalMarkerDecision} from observed evidence
 * and then executed here:
 * <ol>
 *   <li>No metadata document and no marker on the <em>overworld</em>: the world is not historical.
 *       Nothing is read, nothing is written, and no other behaviour changes. Ordinary worlds are
 *       never tagged automatically.</li>
 *   <li>No metadata document but the overworld carries one of this project's historical preset
 *       markers: the metadata document is established <em>from the marker that was observed</em>,
 *       through that marker's own immutable generation binding. This is the only automatic write,
 *       and the marker is the player's explicit opt in. A recognised marker with no declared
 *       binding fails closed before anything is written, and the frozen legacy declaration is never
 *       assumed.</li>
 *   <li>Valid metadata and a matching overworld marker: the world is historical and the document
 *       is authoritative. No write happens during load.</li>
 *   <li>Metadata that is unreadable, metadata without an overworld marker, a marker that does not
 *       match the persisted generation preset, or a level stem registry that cannot be consulted:
 *       fail closed. The document is preserved byte for byte and the server refuses to continue.</li>
 * </ol>
 *
 * <p>Only the overworld level may classify a world. A marker on any other level stem (an
 * own-nether or own-end) must never cause an ordinary overworld to be adopted as historical, and a
 * registry lookup failure must never be read as "nothing is marked".
 *
 * <p>Establishing a <em>new</em> historical world is also available explicitly through
 * {@link #establishHistoricalWorld}.
 */
public final class HistoricalServerLifecycle {
    /**
     * System property used by the automated runtime probe to request that the world in the
     * configured run directory be established as a historical world before the server starts.
     * Unset in every normal run.
     */
    public static final String PROBE_ESTABLISH_PROPERTY = "historical_experience.probe.establish";

    /**
     * System property used by the automated runtime probe to request post-start verification
     * (generation counts, chunk persistence, metadata reload).
     */
    public static final String PROBE_VERIFY_PROPERTY = "historical_experience.probe.verify";

    /**
     * System property used by the automated runtime probe to stop the server once the probe has
     * finished, so an automated run terminates without interactive input.
     */
    public static final String PROBE_AUTOSTOP_PROPERTY = "historical_experience.probe.autoStop";

    /**
     * System property marking the probe's second process. In this mode the probe must find the
     * marker block written by the first process still present in the saved chunk, which is what
     * proves persistence and reload rather than same-process re-reading.
     */
    public static final String PROBE_RELOAD_PROPERTY = "historical_experience.probe.reload";

    private HistoricalServerLifecycle() {
    }

    /** Registers lifecycle callbacks. Safe to call once from the mod initialiser. */
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(HistoricalServerLifecycle::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(HistoricalServerLifecycle::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(HistoricalServerLifecycle::onServerStopping);
        // Runs after the server has finished saving. That timing is the whole point: see
        // applyPendingUpgradeAdvance for the measured reason a running server cannot be the one to
        // rewrite its own level stem.
        ServerLifecycleEvents.SERVER_STOPPED.register(
            HistoricalServerLifecycle::applyPendingUpgradeAdvance);
        // A pre-Beta-1.8 spawning ALGORITHM was registered here for a while: it ran the era's own creature cycle
        // and suppressed the modern per-chunk animal pack beside it. The owner asked for the spawning algorithm
        // work to be removed while the filtering stays, so the tick hook is gone and the filtering mixins are
        // untouched. The algorithm, its caps and the jar evidence behind it are kept in
        // research/pre-beta-1.8-spawning-evidence.json.
        if (Boolean.getBoolean(PROBE_VERIFY_PROPERTY)) {
            // The 26.3 RuntimeProbe lived here. It is deliberately NOT ported: its job - reporting what
            // generation actually produced - is done better and far more cheaply by
            // scripts/region-block-census.py, which reads the saved region files without launching the game
            // and can compare them against a version's own block palette. The property is left recognised
            // so an old launch script does not silently mean something different.
        }
    }

    /** Absolute path of the historical metadata document for the server's default world. */
    public static Path metadataFileFor(MinecraftServer server) {        return server.getWorldPath(LevelResource.ROOT)
            .resolve(net.historicalexperience.world.metadata.MetadataSchema.FILE_NAME)
            .toAbsolutePath();
    }

    private static void onServerStarting(MinecraftServer server) {
        // Recovery now runs under LevelStorageAccess's session lock BEFORE getDataTag decodes the
        // generator. SERVER_STARTING is too late to change the loaded generator without a live swap.
        Path startupWorld = server.getWorldPath(LevelResource.ROOT);
        if (net.historicalexperience.world.upgrade.UpgradeJournal.existsIn(startupWorld)) {
            failClosed(metadataFileFor(server), "an upgrade journal remains after world-data loading; "
                + "refusing a late metadata-only recovery that could disagree with the loaded generator");
        }

        Path metadataFile = metadataFileFor(server);
        HistoricalMetadataStore store = new HistoricalMetadataStore(metadataFile);
        HistoricalWorldState state = HistoricalWorldState.load(store);

        // The world's own explicit marker decides whether this world is historical, and only the
        // OVERWORLD may decide it. An own-nether or own-end marker that resembles the project's
        // preset must never cause an ordinary overworld to be adopted.
        ModernerBetaAdapter.ServerSettingsDescription settings =
            ModernerBetaAdapter.describeServerSettings(server);
        HistoricalMarkerDecision.MarkerEvidence evidence = evidenceFor(settings, state);
        HistoricalMarkerDecision.Decision decision =
            HistoricalMarkerDecision.decide(state, evidence);

        HistoricalExperienceMod.LOGGER.info(
            "Historical marker decision for {}: action={} metadataPresent={} marker={} reason={}",
            server.getWorldPath(LevelResource.ROOT),
            decision.action(),
            decision.metadataPresent(),
            evidence.displayMarker(),
            decision.reason());

        switch (decision.action()) {
            case LEAVE_ORDINARY -> {
                // Nothing historical, nothing written, nothing tagged - and no generation scope, so the
                // Nether and End of this world keep vanilla structure rules.
                net.historicalexperience.generation.HistoricalGenerationScope.clear();
            }
            case ESTABLISH_METADATA -> {
                setGenerationScope(settings);
                warnIfHistoricalWorldGeneratesStructures(server);
                establishFromObservedMarker(store, metadataFile, evidence, settings);
            }
            case ENFORCE_HISTORICAL -> {
                setGenerationScope(settings);
                warnIfHistoricalWorldGeneratesStructures(server);
                logEnforcedWorld(metadataFile, state, evidence, settings);
            }
            case FAIL_CLOSED -> {
                net.historicalexperience.generation.HistoricalGenerationScope.clear();
                failClosed(metadataFile, decision.reason());
            }
            default -> failClosed(metadataFile, "unhandled decision action " + decision.action());
        }
    }

    /**
     * Records the historical recipe for hooks that have no marker-carrying generator to read.
     *
     * <p>Measured reason: the Nether and the End of a historical world use ordinary vanilla level stems, so
     * before this existed a Beta 1.7.3 world could generate a Nether fortress - Release 1.0.0 content - and
     * a decorated write in those dimensions could not be judged at all. The scope is per-server because a
     * server has one overworld whose recipe is the world's recipe.
     */
    private static void setGenerationScope(ModernerBetaAdapter.ServerSettingsDescription settings) {
        String alias = settings.primary().presetMarker();
        net.historicalexperience.generation.HistoricalGenerationScope.set(
            net.historicalexperience.HistoricalMarker.isHistoricalMarker(alias) ? alias : null);
    }

    /** Reports the retained world option and the generation-time structure policy actually enforced. */
    private static void warnIfHistoricalWorldGeneratesStructures(MinecraftServer server) {
        boolean generatesStructures = server.getWorldData().worldGenOptions().generateStructures();
        if (!generatesStructures) {
            return;
        }
        String alias = ModernerBetaAdapter.describeServerSettings(server).primary().presetMarker();
        HistoricalExperienceMod.LOGGER.info(
            "Historical world {} retains generate-structures=true; generation-time structure rules "
                + "for recipe {} select which structures may start. Existing chunks are untouched.",
            server.getWorldPath(LevelResource.ROOT), alias);
    }

    /**
     * Establishes metadata for a world whose overworld carries one of this project's markers.
     *
     * <p>The binding is resolved from the marker that was <em>actually observed</em>, never from a
     * default: a new revision must record itself, not the frozen legacy declaration. An observed
     * marker that this build recognises but declares no binding for is a defect, and it fails closed
     * <b>before</b> anything is written, so no world can acquire metadata naming a recipe whose
     * semantics this build does not know.
     */
    private static void establishFromObservedMarker(
        HistoricalMetadataStore store,
        Path metadataFile,
        HistoricalMarkerDecision.MarkerEvidence evidence,
        ModernerBetaAdapter.ServerSettingsDescription settings
    ) {
        String rawMarker = evidence.primaryMarkerId();
        Optional<HistoricalProfiles.GenerationBinding> resolved =
            HistoricalProfiles.byGenerationPreset(rawMarker);
        if (resolved.isEmpty()) {
            HistoricalExperienceMod.LOGGER.error(
                "The overworld carries the recognised historical marker '{}' but this build "
                    + "declares no generation binding for it (marker state={}, owned markers={}); "
                    + "refusing to establish metadata rather than record a recipe this build cannot "
                    + "reproduce.",
                rawMarker,
                evidence.primaryMarker(),
                HistoricalMarker.HISTORICAL_PRESET_IDS);
            failClosed(metadataFile,
                "the observed overworld marker '" + rawMarker + "' has no generation binding in "
                    + "this build (" + evidence.displayMarker() + ")");
            return;
        }

        HistoricalProfiles.GenerationBinding binding = resolved.get();
        // Two different identifiers are logged deliberately: the owned RAW settings alias (what the
        // world references, reports as its marker, and what will be persisted) and the actual
        // upstream Moderner Beta preset that alias inherits its terrain from. They are never the
        // same value, and neither is a fidelity claim.
        HistoricalExperienceMod.LOGGER.info(
            "Establishing historical world from the observed overworld marker {}: profileId={} "
                + "rawRecipePreset={} generationRevision={} experimental={} foundationPreset={} "
                + "(marker={} generator={} providers={})",
            rawMarker,
            binding.profile().id(),
            binding.rawSettingsAlias(),
            binding.generationRevision(),
            binding.experimental(),
            binding.foundationPreset(),
            evidence.displayMarker(),
            settings.primary().generatorClass(),
            settings.primary().presetMarker());

        establishHistoricalWorld(store, binding);
    }

    /** Builds the policy's evidence from the observed server settings, with no guessing. */
    private static HistoricalMarkerDecision.MarkerEvidence evidenceFor(
        ModernerBetaAdapter.ServerSettingsDescription settings,
        HistoricalWorldState state
    ) {
        if (settings.registryFailure()) {
            // A lookup failure is explicitly NOT "nothing is marked": it fails closed later.
            // The observed primary key is logged because it is the only clue available here
            // ("<unavailable>" means the adapter could not describe the access at all); the
            // underlying exception is reported by the adapter itself and must not be swallowed
            // again on this side.
            HistoricalExperienceMod.LOGGER.error(
                "Level stem registry unavailable at SERVER_STARTING (observed primary side={}, "
                    + "reason={}); the overworld's historical marker is unknown, so startup fails "
                    + "closed without reading or writing any world data.",
                settings.primary().key(),
                settings.failureReason());
            return HistoricalMarkerDecision.registryFailure(
                "level stem registry unavailable for side " + settings.primary().key()
                    + " (" + settings.failureReason() + ")");
        }
        if (settings.primaryMarkerUnknown()) {
            // The description succeeded but the overworld marker itself could not be resolved.
            // Treating this as observed(UNKNOWN) would let the policy treat a normal world as
            // ordinary, i.e. "unknown marker" would silently mean "no marker". It is failure
            // evidence, exactly like an unreachable registry. Only the OVERWORLD can reach here;
            // an unknown marker on any other dimension stays diagnostic and never classifies.
            HistoricalExperienceMod.LOGGER.error(
                "The overworld generator's historical marker could not be read at SERVER_STARTING "
                    + "(side={}, reason={}); refusing to treat an unresolved marker as an "
                    + "unmarked world.",
                settings.primary().key(),
                settings.failureReason());
            return HistoricalMarkerDecision.registryFailure(
                "the overworld marker could not be resolved for side " + settings.primary().key()
                    + " (" + settings.failureReason() + ")");
        }
        String persistedPreset = state.metadataOrEmpty()
            .map(metadata -> metadata.generationPreset().value())
            .orElse(null);
        return HistoricalMarkerDecision.MarkerEvidence.observed(
            settings.primaryMarkerState(),
            settings.primary().presetMarker(),
            persistedPreset);
    }

    /**
     * Logs the accepted historical world together with the diagnostic generator facts.
     *
     * <p>The binding is resolved from the persisted raw recipe identifier, so the reported revision,
     * experimental flag and foundation describe the recipe this world is actually bound to. Both the
     * raw recipe alias (what metadata persists) and the actual upstream terrain foundation are named
     * separately. An experimental revision is labelled as such and its foundation is named as a
     * foundation only; this log makes no fidelity claim about historical accuracy or completeness.
     */
    private static void logEnforcedWorld(
        Path metadataFile,
        HistoricalWorldState state,
        HistoricalMarkerDecision.MarkerEvidence evidence,
        ModernerBetaAdapter.ServerSettingsDescription settings
    ) {
        state.metadataOrEmpty().ifPresent(metadata -> {
            Optional<HistoricalProfiles.GenerationBinding> binding =
                HistoricalProfiles.byGenerationPreset(metadata.generationPreset());

            HistoricalExperienceMod.LOGGER.info(
                "Historical world detected: profileId={} rawRecipePreset={} generationRevision={} "
                    + "experimental={} foundationPreset={} bindingResolved={} historyEntries={} "
                    + "metadataFile={} marker={} generator={} providers={}",
                metadata.profileId(),
                metadata.generationPreset(),
                binding.map(HistoricalProfiles.GenerationBinding::generationRevision).orElse(null),
                binding.map(HistoricalProfiles.GenerationBinding::experimental).orElse(null),
                binding.map(HistoricalProfiles.GenerationBinding::foundationPreset).orElse(null),
                binding.isPresent(),
                metadata.history().size(),
                metadataFile,
                evidence.displayMarker(),
                settings.primary().generatorClass(),
                settings.primary().presetMarker());
        });
        if (!settings.otherDimensions().isEmpty()) {
            HistoricalExperienceMod.LOGGER.info(
                "Additional level stems observed (diagnostic only, never used to classify the "
                    + "world): {}", settings.otherDimensions());
        }
    }

    private static void onServerStarted(MinecraftServer server) {
        HistoricalServerContext.onServerStarted(server);
        // Intentionally empty: the 26.3 probe reported here, and the region-file census replaced it.
        //
        // Alpha v1.2.0's biome conversion used to run here as a queued chunk rewrite, worked through a batch per tick.
        // It was removed because it was unnecessary, not because it was wrong: Moderner Beta's biome source is
        // authoritative - ModernBetaBiomeSource.getNoiseBiome asks its provider for every position and never reads the
        // biome stored in a chunk - so rewriting stored biomes changed nothing the game reported, which is exactly what
        // the F3 readout showed after 841 chunks had verifiably changed on disk. The biome now comes from the
        // `alpha_1_2_0_beta_plains` revision's preset setting, so there is no chunk work to gate or to show progress
        // for, and no world entry to hold back.
        //
        // What is reported here instead is the biome the level actually resolves at spawn - the value F3 shows, and the
        // only way to check the Alpha v1.2.0 biome rule, because Moderner Beta's biome source answers every position
        // from its provider rather than from the chunk.
        try {
            var overworld = server.overworld();
            var spawn = overworld.getSharedSpawnPos();
            HistoricalExperienceMod.LOGGER.info("Biome at spawn {} = {} (profile {})",
                spawn.toShortString(),
                overworld.getBiome(spawn).unwrapKey().map(Object::toString).orElse("unresolved"),
                HistoricalProfileLookup.profileIdForLevel(overworld));
            reportBiomeProvider(overworld, spawn);
            reportBiomeVariety(overworld, spawn);
        } catch (RuntimeException failure) {
            HistoricalExperienceMod.LOGGER.warn("Could not report the biome at spawn", failure);
        }
    }

    /**
     * Samples the biome at a spread of positions and reports how many distinct ones there are.
     *
     * <p>This is the check that matters for generation: a single-biome world reports one name for every sample, and a
     * climate-based generator reports several. It exists because a settings change flattened generation once - the
     * owner's report was "in the world upgraded to 1.8.1, NO OTHER BIOME GENERATES" - and the spawn biome alone could
     * never have shown that, since a flattened world still has a correct-looking biome at spawn.
     */
    private static void reportBiomeVariety(ServerLevel level, net.minecraft.core.BlockPos spawn) {
        java.util.Set<String> found = new java.util.TreeSet<>();
        int samples = 0;
        // A wide, deterministic spread: multiples of 256 blocks so the samples fall in different climate cells.
        for (int x = -1024; x <= 1024; x += 256) {
            for (int z = -1024; z <= 1024; z += 256) {
                var pos = new net.minecraft.core.BlockPos(spawn.getX() + x, spawn.getY(), spawn.getZ() + z);
                found.add(level.getBiome(pos).unwrapKey().map(Object::toString).orElse("unresolved"));
                samples++;
            }
        }
        HistoricalExperienceMod.LOGGER.info("Biome variety across {} samples: {} distinct -> {}",
            samples, found.size(), found);
    }

    /**
     * Reports what Moderner Beta's biome source actually resolved, which is the thing the biome rules depend on.
     *
     * <p>Written because two rules were built on the assumption that a settings preset decides the biome, and one of
     * them (Alpha v1.2.0) worked while the other did not, with no way from outside to tell why. This prints the
     * resolved {@code single_biome} component and the concrete provider class, which distinguishes the three possible
     * causes: the override not reaching the settings, the provider not being the single-biome one, or the provider
     * being single but named something else.
     */
    private static void reportBiomeProvider(ServerLevel level, net.minecraft.core.BlockPos spawn) {
        try {
            Object source = level.getChunkSource().getGenerator().getBiomeSource();
            String providerName = "<not a Moderner Beta source>";
            String singleBiome = "<none>";
            if (source instanceof mod.bluestaggo.modernerbeta.level.biome.ModernBetaBiomeSource modernBeta) {
                java.lang.reflect.Field field =
                    mod.bluestaggo.modernerbeta.level.biome.ModernBetaBiomeSource.class
                        .getDeclaredField("biomeProvider");
                field.setAccessible(true);
                Object provider = field.get(modernBeta);
                providerName = provider == null ? "<null>" : provider.getClass().getSimpleName();
                // Walk the resolved settings for the single-biome component.
                var settingsField = mod.bluestaggo.modernerbeta.level.biome.ModernBetaBiomeSource.class
                    .getDeclaredField("biomeSettings");
                settingsField.setAccessible(true);
                var settings = (mod.bluestaggo.modernerbeta.settings.ModernBetaSettings) settingsField.get(modernBeta);
                for (mod.bluestaggo.modernerbeta.settings.SettingsComponent<?> component : settings) {
                    if (component.type() == mod.bluestaggo.modernerbeta.settings.SettingsComponentTypes.SINGLE_BIOME) {
                        singleBiome = String.valueOf(component.value());
                    }
                }
                HistoricalExperienceMod.LOGGER.info(
                    "Moderner Beta biome source: provider={} single_biome={} biome_at_spawn={}",
                    providerName, singleBiome,
                    modernBeta.getNoiseBiome(spawn.getX() >> 2, spawn.getY() >> 2, spawn.getZ() >> 2,
                        net.minecraft.world.level.biome.Climate.empty()).unwrapKey()
                        .map(Object::toString).orElse("?"));
            } else {
                HistoricalExperienceMod.LOGGER.info("Biome source is {}", providerName);
            }
        } catch (ReflectiveOperationException | RuntimeException failure) {
            HistoricalExperienceMod.LOGGER.warn("Could not report the biome source", failure);
        }
    }

    private static void onServerStopping(MinecraftServer server) {
        HistoricalServerContext.onServerStopped();
        // Intentionally empty, for the same reason.
    }

    /**
     * Applies a staged generator advance, after the server has finished saving and stopped.
     *
     * <p>This runs on {@code SERVER_STOPPED} rather than at startup, and the ordering is not a
     * preference. A measured run showed that the server reads its level stem into memory before
     * {@code SERVER_STARTING} fires and writes that file again when it saves, so an advance performed at
     * startup was silently undone while the code logged success - leaving the metadata claiming a preset
     * the world was not generating with. Here, nothing is left to overwrite the rewrite, and the next
     * start loads the intended recipe.
     *
     * <p>A failure is logged and the journal is kept, so the advance is not lost and the next shutdown
     * tries again. The world itself is untouched either way: only the level stem, the metadata and the
     * backup are ever written.
     */
    private static void applyPendingUpgradeAdvance(MinecraftServer server) {
        // The scope is per-process state for a running server, so it must not outlive one: leaving it set
        // would make the next world loaded in this process obey this world's historical structure rules.
        net.historicalexperience.generation.HistoricalGenerationScope.clear();
        Path worldDirectory = server.getWorldPath(LevelResource.ROOT);
        try {
            Optional<net.historicalexperience.world.upgrade.GenerationUpgrade.Result> applied =
                net.historicalexperience.world.upgrade.GenerationUpgrade
                    .applyPendingAdvance(worldDirectory);
            applied.ifPresent(result -> HistoricalExperienceMod.LOGGER.warn(
                "Applied the pending generation upgrade in {}: the stored generator now names {}. It "
                    + "takes effect from the next start, and existing chunks are untouched.",
                worldDirectory, result.after().generationPreset()));
        } catch (Exception e) {
            HistoricalExperienceMod.LOGGER.error(
                "A pending generation upgrade in {} could not be applied and was left pending; the world "
                    + "will continue with the generator its level stem describes, and the next shutdown "
                    + "will try again.",
                worldDirectory, e);
        }
    }

    /**
     * Establishes a world directory as historical, writing the initial metadata document.
     *
     * <p><b>What is persisted is the owned raw settings alias, not the terrain foundation.</b>
     * {@link HistoricalMetadata#generationPreset()} is the recipe identifier a world resource
     * references and a loaded world reports back as its durable marker, so it must be the binding's
     * raw alias. Writing the actual upstream {@code moderner_beta:beta} preset instead would both
     * break the marker check for this world and make a plain upstream Beta world indistinguishable
     * from a project one. Because an alias is immutable and a new content revision is a new alias,
     * the document records the exact generation revision the world was founded on; nothing is
     * rewritten, reinterpreted or silently advanced. No schema field is added and no existing
     * document is touched: this method only ever creates a document that does not exist yet.
     *
     * <p>Never overwrites an existing document: if one is already present the call logs and
     * returns, leaving the existing historical state authoritative.
     */
    public static void establishHistoricalWorld(
        HistoricalMetadataStore store,
        HistoricalProfiles.GenerationBinding binding
    ) {
        java.util.Objects.requireNonNull(store, "store");
        java.util.Objects.requireNonNull(binding, "binding");
        if (store.hasEntry()) {
            HistoricalExperienceMod.LOGGER.info(
                "Historical metadata already present at {}; not overwriting.", store.file());
            return;
        }

        HistoricalMetadata metadata = HistoricalMetadata.initial(
            binding.profile().id(),
            binding.rawSettingsAlias(),
            HistoricalExperience.MOD_ID,
            System.currentTimeMillis());
        try {
            boolean created = store.writeIfAbsent(metadata);
            HistoricalExperienceMod.LOGGER.info(
                "Established historical world metadata at {} (created={}): profileId={} "
                    + "rawRecipePreset={} generationRevision={} experimental={} foundationPreset={} "
                    + "metadataRevision={}",
                store.file(),
                created,
                metadata.profileId(),
                metadata.generationPreset(),
                binding.generationRevision(),
                binding.experimental(),
                binding.foundationPreset(),
                metadata.revision());
        } catch (IOException e) {
            throw new IllegalStateException(
                "failed to write historical metadata to " + store.file(), e);
        }
    }

    /** Reads the metadata document, failing closed when it cannot be trusted. */
    public static Optional<HistoricalMetadata> readMetadata(Path metadataFile) {
        HistoricalMetadataStore store = new HistoricalMetadataStore(metadataFile);
        // hasEntry(), not exists(): a path occupied by a directory, a symlink or any other
        // non-regular entry is a broken historical world, not an ordinary one. Using exists()
        // here would report "no document" for such a path, which is the fail-open reading this
        // method must not produce.
        if (!store.hasEntry()) {
            return Optional.empty();
        }
        try {
            return Optional.of(store.read());
        } catch (MetadataException | IOException e) {
            failClosed(metadataFile, e.getMessage());
            return Optional.empty();
        }
    }

    private static void failClosed(Path metadataFile, String reason) {
        throw new HistoricalMetadataFailure(
            "Refusing to start: historical world classification could not be trusted ("
                + reason + "). This mod has not changed or removed historical metadata at "
                + metadataFile
                + ". Diagnose the reported condition before retrying; a marker/registry failure "
                + "alone does not establish save corruption or justify deleting files.");
    }

    /** Unchecked failure used to abort server startup on unreadable historical metadata. */
    public static final class HistoricalMetadataFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;

        HistoricalMetadataFailure(String message) {
            super(message);
        }
    }

    /** Convenience for logging identifiers without leaking Minecraft types into metadata code. */
    static String describe(HistoricalIdentifier id) {
        return id.value();
    }
}
