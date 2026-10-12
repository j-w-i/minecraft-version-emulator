package net.historicalexperience.integration.modernerbeta;

import java.util.Objects;
import java.util.Optional;

import mod.bluestaggo.modernerbeta.level.biome.ModernBetaBiomeSource;
import mod.bluestaggo.modernerbeta.level.chunk.ModernBetaChunkGenerator;
import mod.bluestaggo.modernerbeta.registry.ModernBetaRegistries;
import mod.bluestaggo.modernerbeta.registry.ModernBetaResourceKeys;
import mod.bluestaggo.modernerbeta.settings.ModernBetaSettings;
import mod.bluestaggo.modernerbeta.settings.ModernBetaSettingsPreset;
import mod.bluestaggo.modernerbeta.settings.SettingsComponentTypes;
import mod.bluestaggo.modernerbeta.api.level.chunk.surface.SurfaceConfig;
import net.historicalexperience.HistoricalMarker.MarkerState;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * The single bounded adapter between Historical Experience and Moderner Beta.
 *
 * <p>Rules for this package:
 * <ul>
 *   <li>Moderner Beta remains the terrain authority. This mod never reimplements, approximates
 *       or substitutes a terrain algorithm; it only selects a Moderner Beta preset and constructs
 *       the real Moderner Beta generator objects from it.</li>
 *   <li>All Moderner Beta types are confined to this package. Nothing outside
 *       {@code net.historicalexperience.integration.modernerbeta} may import
 *       {@code mod.bluestaggo.modernerbeta.*}.</li>
 *   <li>Historical worlds reference a <em>project owned</em> settings preset,
 *       {@code historical_experience:beta_1_7_3}, which inherits Moderner Beta's
 *       {@code moderner_beta:beta} preset through the {@code moderner_beta:preset} component in
 *       each of its three settings blocks. The {@code moderner_beta:default} alias is never used,
 *       because it resolves against the user's global Moderner Beta configuration and would make
 *       terrain depend on client configuration rather than on persisted world data.</li>
 *   <li>That project owned preset identifier is also the durable historical marker: it survives
 *       serialisation because Moderner Beta's chunk generator keeps the unresolved settings (and
 *       therefore the {@code moderner_beta:preset} component) in {@code chunkSettings}, while
 *       preset mapping is applied only to working copies. See
 *       {@link #explicitPresetMarker(ChunkGenerator)}.</li>
 * </ul>
 */
public final class ModernerBetaAdapter {
    /** Settings preset that a historical world references: project owned. */
    public static final ResourceLocation HISTORICAL_BETA_1_7_3_PRESET =
        net.historicalexperience.HistoricalMarker.HISTORICAL_PRESET;

    /** The pinned upstream preset our project preset inherits its terrain from. */
    public static final ResourceLocation UPSTREAM_BETA_1_7_3_PRESET =
        net.historicalexperience.HistoricalMarker.UPSTREAM_PRESET;

    /** Resource key of the project owned preset. */
    public static final ResourceKey<ModernBetaSettingsPreset> HISTORICAL_BETA_1_7_3_KEY =
        ResourceKey.create(ModernBetaResourceKeys.SETTINGS_PRESET, HISTORICAL_BETA_1_7_3_PRESET);

    private ModernerBetaAdapter() {
    }

    /**
     * A preset <em>reference</em> to the project owned historical preset.
     *
     * <p>Carries the preset identifier in every settings block rather than a copy of resolved
     * settings, so resolution happens against the same registry data Minecraft uses when loading
     * a level.
     */
    public static ModernBetaSettingsPreset historicalPresetReference() {
        return ModernBetaSettingsPreset.referenced(HISTORICAL_BETA_1_7_3_PRESET);
    }

    /**
     * Resolves the project owned preset against a live preset registry.
     *
     * @throws ModernerBetaIntegrationException when the preset is absent, which means the
     *         datapack did not load and terrain would silently differ from what the world's
     *         metadata claims
     */
    public static ModernBetaSettingsPreset resolveHistoricalPreset(
        HolderGetter<ModernBetaSettingsPreset> presetRegistry
    ) {
        return resolvePreset(presetRegistry, HISTORICAL_BETA_1_7_3_KEY,
            HISTORICAL_BETA_1_7_3_PRESET);
    }

    /**
     * Resolves the pinned upstream {@code moderner_beta:beta} preset. Used by verification code
     * to compare the project preset's inherited settings against the upstream authority.
     */
    public static ModernBetaSettingsPreset resolveUpstreamBeta173(
        HolderGetter<ModernBetaSettingsPreset> presetRegistry
    ) {
        return resolvePreset(presetRegistry,
            mod.bluestaggo.modernerbeta.settings.ModernBetaSettingsPresets.BETA_1_7_3,
            UPSTREAM_BETA_1_7_3_PRESET);
    }

    /**
     * Resolves the upstream foundation a world's own recipe declares.
     *
     * <p>Generalises {@link #resolveUpstreamBeta173(HolderGetter)}: a version records which upstream
     * preset its terrain inherits from - Beta 1.7.3 inherits {@code moderner_beta:beta} and Beta 1.8.1
     * inherits {@code moderner_beta:beta_1_8_1} - so the audit must compare against THAT preset rather
     * than one pinned value. Comparing everything against Beta 1.7.3's preset was correct while one
     * version existed and would have rejected every other version's world for inheriting the right
     * terrain.
     *
     * @return the declared foundation preset, or empty when the alias is not a declared recipe
     */
    public static java.util.Optional<ResourceLocation> declaredFoundation(String rawAlias) {
        return net.historicalexperience.version.HistoricalVersions.byRevisionAlias(rawAlias)
            .flatMap(version -> version.revisions().stream()
                .filter(revision -> revision.alias().equals(rawAlias))
                .findFirst())
            .map(net.historicalexperience.version.HistoricalVersion.GenerationRevision::foundation)
            .map(ResourceLocation::parse);
    }

    /**
     * Resolves any settings preset by identifier.
     *
     * <p>The generic counterpart of {@link #resolveUpstreamBeta173}: it takes the identifier from the
     * world's own declared recipe instead of a constant.
     */
    public static ModernBetaSettingsPreset resolvePresetById(
        HolderGetter<ModernBetaSettingsPreset> presetRegistry,
        ResourceLocation presetId
    ) {
        Objects.requireNonNull(presetId, "presetId");
        return resolvePreset(presetRegistry,
            ResourceKey.create(ModernBetaResourceKeys.SETTINGS_PRESET, presetId),
            presetId);
    }

    private static ModernBetaSettingsPreset resolvePreset(
        HolderGetter<ModernBetaSettingsPreset> presetRegistry,
        ResourceKey<ModernBetaSettingsPreset> key,
        ResourceLocation id
    ) {
        Objects.requireNonNull(presetRegistry, "presetRegistry");
        try {
            return ModernBetaSettingsPreset.referenced(presetRegistry.getOrThrow(key));
        } catch (RuntimeException e) {
            throw new ModernerBetaIntegrationException(
                "Moderner Beta settings preset '" + id + "' is not registered; terrain for this "
                    + "world cannot be resolved without substituting a different preset", e);
        }
    }

    /**
     * Constructs the real Moderner Beta chunk generator for the project owned historical preset.
     *
     * <p>The generator is built from the resolved preset's three settings blocks: chunk settings
     * drive terrain, biome settings drive the Beta climate based biome provider, and cave biome
     * settings drive cave biome assignment. No terrain values are hard coded here - every value
     * comes from the datapack preset, which itself inherits from Moderner Beta.
     */
    public static ModernBetaChunkGenerator createChunkGenerator(
        HolderGetter<Biome> biomeRegistry,
        HolderGetter<ModernBetaSettingsPreset> presetRegistry,
        HolderGetter<SurfaceConfig> surfaceRegistry,
        ModernBetaSettingsPreset preset
    ) {
        Objects.requireNonNull(biomeRegistry, "biomeRegistry");
        Objects.requireNonNull(presetRegistry, "presetRegistry");
        Objects.requireNonNull(surfaceRegistry, "surfaceRegistry");
        Objects.requireNonNull(preset, "preset");

        ModernBetaBiomeSource biomeSource = new ModernBetaBiomeSource(
            biomeRegistry,
            presetRegistry,
            preset.biomeSettings(),
            preset.caveBiomeSettings());

        return new ModernBetaChunkGenerator(
            biomeSource,
            presetRegistry,
            surfaceRegistry,
            preset.chunkSettings());
    }

    /**
     * Reads the durable historical marker from a loaded generator.
     *
     * <p>Moderner Beta's generator keeps the settings it was constructed from, including the
     * {@code moderner_beta:preset} component, in {@code chunkSettings}; preset mapping is applied
     * to working copies only. So a world whose generator was created from a preset reference
     * still reports that preset identifier here, which makes it a durable, explicit marker rather
     * than a provider-shape guess.
     *
     * @return the referenced preset identifier, or empty when the generator carries no preset
     *         reference (for example a world built from fully expanded settings)
     */
    public static Optional<ResourceLocation> explicitPresetMarker(ChunkGenerator generator) {
        if (!(generator instanceof ModernBetaChunkGenerator modernBeta)) {
            return Optional.empty();
        }
        ResourceLocation preset = modernBeta.getChunkSettings().get(SettingsComponentTypes.PRESET);
        return Optional.ofNullable(preset);
    }

    /** True when the generator carries the project owned historical preset marker. */
    public static boolean hasHistoricalPresetMarker(ChunkGenerator generator) {
        return explicitPresetMarker(generator)
            .filter(preset -> net.historicalexperience.HistoricalMarker.isHistoricalMarker(preset.toString()))
            .isPresent();
    }

    /**
     * Whether any of the given levels carries this project's historical preset marker.
     *
     * <p>Diagnostic only. World classification must use
     * {@link #describeServerSettings(net.minecraft.server.MinecraftServer)} and the overworld stem:
     * a marker on any other level (an own-nether or own-end) must never classify the overworld.
     * Returns {@code true} here for any marked level, which is exactly why callers that decide a
     * world's semantics must not use it.
     */
    public static Optional<ResourceLocation> explicitHistoricalPresetMarker(
        Iterable<? extends ChunkGenerator> generators
    ) {
        if (generators == null) {
            return Optional.empty();
        }
        for (ChunkGenerator generator : generators) {
            if (hasHistoricalPresetMarker(generator)) {
                return Optional.of(HISTORICAL_BETA_1_7_3_PRESET);
            }
        }
        return Optional.empty();
    }

    /**
     * A plain, dependency-free description of a loaded chunk generator.
     *
     * <p>This is the only shape that leaves the adapter. Code outside this package deals in this
     * record and never in Moderner Beta or Minecraft generator types, so the adapter boundary is
     * enforced by the compiler rather than by convention.
     *
     * <p>{@code minY}, {@code genDepth} and {@code seaLevel} are diagnostics only and may be
     * {@link #TERRAIN_UNKNOWN} when the generator's terrain provider has not been initialised yet.
     * Moderner Beta initialises providers in its own {@code SERVER_STARTING} listener; an earlier
     * listener must not assume this has happened. These values never participate in the marker
     * decision; the marker comes from the generator's stored settings.
     */
    public record GeneratorFacts(
        String generatorClass,
        boolean modernerBeta,
        String presetMarker,
        String chunkProvider,
        String biomeProvider,
        String caveBiomeProvider,
        int minY,
        int genDepth,
        int seaLevel
    ) {
        public GeneratorFacts {
            Objects.requireNonNull(generatorClass, "generatorClass");
            Objects.requireNonNull(presetMarker, "presetMarker");
            Objects.requireNonNull(chunkProvider, "chunkProvider");
            Objects.requireNonNull(biomeProvider, "biomeProvider");
            Objects.requireNonNull(caveBiomeProvider, "caveBiomeProvider");
        }

        /** True when every terrain dimension was readable (i.e. the provider was initialised). */
        public boolean terrainKnown() {
            return minY != TERRAIN_UNKNOWN
                && genDepth != TERRAIN_UNKNOWN
                && seaLevel != TERRAIN_UNKNOWN;
        }

        /** Highest block Y this generator can address, or {@link #TERRAIN_UNKNOWN}. */
        public int maxY() {
            return terrainKnown() ? minY + genDepth - 1 : TERRAIN_UNKNOWN;
        }

        /** True when the generator carries this project's historical preset marker. */
        public boolean hasHistoricalMarker() {
            return net.historicalexperience.HistoricalMarker.isHistoricalMarker(presetMarker);
        }

        /** The loaded provider triple, for comparison and logging. */
        public String providerTriple() {
            return chunkProvider + "/" + biomeProvider + "/" + caveBiomeProvider;
        }
    }

    /**
     * Evidence that the settings a generator actually generates from were compared, in full, with
     * the pinned upstream {@code moderner_beta:beta} preset.
     *
     * <p>Every field is measured from the live objects:
     * <ul>
     *   <li>the three {@code reference} strings are the raw {@code moderner_beta:preset}
     *       components read straight off the settings the generator and its biome source were
     *       constructed with - before any inheritance is walked;</li>
     *   <li>the three {@code *Components} counts are the sizes of the <em>applied</em> component
     *       maps after {@code mapPreset} resolved inheritance and applied local overrides, which is
     *       the same form the providers consume;</li>
     *   <li>the three {@code *Matches} flags are whole-map equality of those applied maps against
     *       the upstream preset's applied maps. They are never derived from provider identifiers:
     *       provider ids are shared between presets, so a triple match proves nothing.</li>
     * </ul>
     *
     * <p>{@code providersInitialized} only reports Moderner Beta's own readiness - the chunk, biome
     * and cave biome providers are assigned in {@code ModernBetaLevelInitializer.initStarting} on
     * {@code SERVER_STARTING}. It never participates in the equality result.
     */
    public record AppliedPresetEvidence(
        String expectedPreset,
        String chunkReference,
        String biomeReference,
        String caveReference,
        int chunkComponents,
        int biomeComponents,
        int caveComponents,
        boolean chunkMatches,
        boolean biomeMatches,
        boolean caveMatches,
        boolean providersInitialized
    ) {
        /** True only when all three applied settings maps equal the expected preset's. */
        public boolean allSettingsMatch() {
            return chunkMatches && biomeMatches && caveMatches;
        }
    }

    /**
     * Compares the settings a loaded generator actually applies with the pinned upstream
     * {@code moderner_beta:beta} preset, in full.
     *
     * <p>The expected preset is resolved from the live registry at call time through
     * {@link mod.bluestaggo.modernerbeta.settings.ModernBetaSettingsPresets#BETA_1_7_3}; no preset
     * is constructed, copied or reconstructed here, and no component value is read individually.
     * Both sides are reduced to the applied form through
     * {@link ModernBetaSettingsPreset#mapped(HolderGetter)}, which walks inheritance with the
     * correct accessor for each of the three blocks, and are then compared with
     * {@link ModernBetaSettings#equals(Object)} over the complete component maps.
     *
     * <p>Applying {@code mapPreset} to the generator's own stored blocks rather than to the preset
     * they reference is deliberate: a stored block may carry local overrides on top of the
     * reference, and only mapping the stored block itself resolves those. This mirrors the
     * provider path exactly (chunk: {@code ChunkProvider}; biome and cave biome:
     * {@code ModernBetaBiomeSource.initProvider}).
     *
     * <p>This call does not require the providers to be initialised, because it reads the settings
     * the generator was constructed with. It does require the settings preset registry.
     *
     * @throws ModernerBetaIntegrationException when the generator is not a Moderner Beta
     *         generator, when it exposes no settings preset registry, or when the expected preset
     *         cannot be resolved from that registry. It never substitutes a different preset,
     *         an empty settings object or a best-effort answer.
     */
    public static AppliedPresetEvidence compareToUpstreamBeta173(ChunkGenerator generator) {
        return compareAppliedSettings(generator);
    }

    /** Exact full-map audit of the two approved preview deltas, not a terrain reconstruction. */
    public static AppliedPresetEvidence compareToBeta173ContentPreview(ChunkGenerator generator) {
        return compareAppliedSettings(generator);
    }

    /**
     * The revision's declared local overrides - every component except the inherited preset
     * reference - applied on top of the already-mapped upstream preset.
     *
     * <p>Two places can declare an override, and both must be honoured or the audit would compare
     * against the wrong expectation:
     * <ol>
     *   <li>the generator's own stored settings block, which normally only names the project
     *       settings preset;</li>
     *   <li>the settings preset that block references, which is where this project's researched
     *       content deltas actually live.</li>
     * </ol>
     *
     * <p>Exactly one preset link is followed, because that is the shape this project declares: a
     * project alias referencing the pinned upstream foundation. The upstream foundation's own
     * components are never re-declared here - they are obtained by mapping the upstream preset, which
     * is why this is not a copied settings table.
     */
    private static ModernBetaSettings withDeclaredOverrides(
        ModernBetaSettings mappedUpstream,
        ModernBetaSettings storedBlock,
        HolderGetter<ModernBetaSettingsPreset> presetRegistry,
        java.util.function.Function<ModernBetaSettingsPreset, ModernBetaSettings> accessor
    ) {
        ModernBetaSettings local = ModernBetaSettings.builder(storedBlock)
            .remove(SettingsComponentTypes.PRESET)
            .build();

        ResourceLocation presetId = storedBlock.get(SettingsComponentTypes.PRESET);
        if (presetId != null) {
            ModernBetaSettingsPreset referenced = ModernBetaSettingsPreset
                .getPreset(presetId, presetRegistry)
                .orElseThrow(() -> new ModernerBetaIntegrationException(
                    "The stored settings block references settings preset " + presetId
                        + ", which is not registered; refusing to audit settings against an "
                        + "expectation that cannot be resolved"));
            local = ModernBetaSettings.builder(accessor.apply(referenced))
                .remove(SettingsComponentTypes.PRESET)
                .addAll(local)
                .build();
        }

        return ModernBetaSettings.builder(mappedUpstream).addAll(local).build();
    }

    /**
     * The content deltas actually applied by a historical generator.
     *
     * <p>Read from the <b>resolved</b> settings, not from the generator's stored block: the stored
     * block normally declares nothing but the project preset reference, so reading it would report
     * every researched restriction as absent. Separate from the whole-map comparison, which proves
     * the inheritance composition held; this states <em>which</em> deltas are present, so a revision
     * that quietly dropped one cannot pass by being self-consistently wrong.
     *
     * @param chunkPreset       the raw owned recipe alias stored in the chunk settings
     * @param deepslateDisabled true when deepslate generation is explicitly disabled
     * @param caveProvider      the applied cave biome provider identifier, or {@code null}
     * @param noiseSettingsId   the applied noise generator settings identifier, or {@code null}
     */
    public record AppliedRecipeDeltas(
        String chunkPreset,
        boolean deepslateDisabled,
        String caveProvider,
        String noiseSettingsId
    ) {
    }

    /** Reads the applied content deltas of a historical generator without inventing any. */
    public static AppliedRecipeDeltas describeAppliedRecipeDeltas(ChunkGenerator generator) {
        Objects.requireNonNull(generator, "generator");
        if (!(generator instanceof ModernBetaChunkGenerator modernBeta)) {
            throw new ModernerBetaIntegrationException(
                "Cannot describe content deltas: generator is " + generator.getClass().getName()
                    + ", which is not a Moderner Beta generator");
        }
        HolderGetter<ModernBetaSettingsPreset> presetRegistry = modernBeta.getPresetRegistry();
        if (presetRegistry == null) {
            throw new ModernerBetaIntegrationException(
                "Cannot describe content deltas: the generator exposes no settings preset "
                    + "registry, so the applied settings cannot be resolved");
        }
        if (!(modernBeta.getBiomeSource() instanceof ModernBetaBiomeSource source)) {
            throw new ModernerBetaIntegrationException(
                "Cannot describe content deltas: the generator's biome source is "
                    + String.valueOf(modernBeta.getBiomeSource())
                    + ", not a Moderner Beta biome source");
        }

        ModernBetaSettings storedChunk = modernBeta.getChunkSettings();
        ModernBetaSettings appliedChunk = mapStoredBlock(storedChunk, presetRegistry,
            ModernBetaSettingsPreset::chunkSettings, "chunk");
        ModernBetaSettings appliedCave = mapStoredBlock(source.getCaveBiomeSettings(),
            presetRegistry, ModernBetaSettingsPreset::caveBiomeSettings, "cave biome");

        ResourceLocation provider = appliedCave.get(SettingsComponentTypes.PROVIDER);
        var noise = appliedChunk.get(SettingsComponentTypes.NOISE_GENERATOR_SETTINGS);
        return new AppliedRecipeDeltas(
            rawReference(storedChunk),
            !appliedChunk.getOrDefault(SettingsComponentTypes.DEEPSLATE_GENERATION).enabled(),
            provider == null ? null : provider.toString(),
            noise == null ? null : noise.unwrapKey().map(key -> key.location().toString())
                .orElse("<unregistered>"));
    }

    private static AppliedPresetEvidence compareAppliedSettings(ChunkGenerator generator) {
        Objects.requireNonNull(generator, "generator");
        if (!(generator instanceof ModernBetaChunkGenerator modernBeta)) {
            throw new ModernerBetaIntegrationException(
                "Cannot compare applied settings with " + UPSTREAM_BETA_1_7_3_PRESET
                    + ": generator is " + generator.getClass().getName()
                    + ", which is not a Moderner Beta generator");
        }

        HolderGetter<ModernBetaSettingsPreset> presetRegistry = modernBeta.getPresetRegistry();
        if (presetRegistry == null) {
            throw new ModernerBetaIntegrationException(
                "Cannot compare applied settings with " + UPSTREAM_BETA_1_7_3_PRESET
                    + ": the generator exposes no settings preset registry, so neither the "
                    + "expected preset nor the generator's inherited settings can be resolved");
        }

        ModernBetaSettings rawChunk = modernBeta.getChunkSettings();
        ModernBetaSettings rawBiome = null;
        ModernBetaSettings rawCave = null;
        ModernBetaBiomeSource biomeSource = null;
        if (modernBeta.getBiomeSource() instanceof ModernBetaBiomeSource source) {
            biomeSource = source;
            rawBiome = source.getBiomeSettings();
            rawCave = source.getCaveBiomeSettings();
        }
        if (rawBiome == null || rawCave == null) {
            throw new ModernerBetaIntegrationException(
                "Cannot compare applied settings with " + UPSTREAM_BETA_1_7_3_PRESET
                    + ": the generator's biome source is "
                    + String.valueOf(modernBeta.getBiomeSource())
                    + ", not a Moderner Beta biome source, so its biome and cave biome settings "
                    + "are not readable");
        }

        // The expected terrain comes from the world's OWN declared recipe: the alias resolves to a
        // version whose revision names the upstream foundation it inherits from. A plain upstream world
        // declares no recipe, so its alias IS the upstream preset and is used directly. Comparing
        // everything against one pinned upstream preset is what made this adapter single-version.
        String appliedAlias = rawReference(rawChunk);
        ResourceLocation foundationId = declaredFoundation(appliedAlias)
            .orElseGet(() -> ResourceLocation.parse(appliedAlias));
        ModernBetaSettingsPreset expected = resolvePresetById(presetRegistry, foundationId)
            .mapped(presetRegistry);
        ModernBetaSettings expectedChunk = expected.chunkSettings();
        ModernBetaSettings expectedCave = expected.caveBiomeSettings();
        // The expected deltas are always the recipe's OWN declared local components: those stored on the
        // generator's settings block plus those its referenced settings preset declares. There is no
        // "is this a preview" switch any more, because the switch was really asking "does this recipe
        // declare overrides?" - a question the data already answers. A reference-only recipe declares
        // none, so its expectation reduces to the plain mapped foundation exactly as before, while a
        // recipe with deltas composes them. Hardcoding one revision's delta list here was wrong twice
        // over: it rejected a legitimate third delta and would have accepted a revision that dropped one.
        expectedChunk = withDeclaredOverrides(
            expectedChunk, rawChunk, presetRegistry, ModernBetaSettingsPreset::chunkSettings);
        expectedCave = withDeclaredOverrides(
            expectedCave, rawCave, presetRegistry, ModernBetaSettingsPreset::caveBiomeSettings);
        ModernBetaSettings appliedChunk = mapStoredBlock(rawChunk, presetRegistry,
            ModernBetaSettingsPreset::chunkSettings, "chunk");
        ModernBetaSettings appliedBiome = mapStoredBlock(rawBiome, presetRegistry,
            ModernBetaSettingsPreset::biomeSettings, "biome");
        ModernBetaSettings appliedCave = mapStoredBlock(rawCave, presetRegistry,
            ModernBetaSettingsPreset::caveBiomeSettings, "cave biome");

        boolean providersInitialized = modernBeta.getChunkProvider() != null
            && biomeSource.getBiomeProvider() != null
            && biomeSource.getCaveBiomeProvider() != null;

        return new AppliedPresetEvidence(
            foundationId.toString(),
            appliedAlias,
            rawReference(rawBiome),
            rawReference(rawCave),
            appliedChunk.size(),
            appliedBiome.size(),
            appliedCave.size(),
            appliedChunk.equals(expectedChunk),
            appliedBiome.equals(expected.biomeSettings()),
            appliedCave.equals(expectedCave),
            providersInitialized);
    }

    /**
     * The preset reference marker plus the capability to resolve it, both kept inside the adapter.
     *
     * <p>Callers outside this package must use {@link #describeGenerator(ChunkGenerator)} or
     * {@link #describeServerSettings(net.minecraft.server.MinecraftServer)} and never handle a
     * {@link HolderGetter} themselves; exposing one would leak a Moderner Beta type across the
     * package boundary and invite callers to guess at preset resolution.
     */
    public record GeneratorDescription(
        GeneratorFacts facts,
        MarkerState markerState,
        boolean presetRegistryAvailable,
        String registryFailure
    ) {
        public GeneratorDescription {
            Objects.requireNonNull(facts, "facts");
            Objects.requireNonNull(markerState, "markerState");
            Objects.requireNonNull(registryFailure, "registryFailure");
        }

        /** True only when this generator carries this project's historical preset marker. */
        public boolean hasHistoricalMarker() {
            return markerState.isHistorical();
        }

        /** True when the preset registry was unavailable, so the marker could not be read. */
        public boolean markerUnknown() {
            return markerState == MarkerState.UNKNOWN;
        }

        /** The raw marker identifier, or a placeholder when there is none. */
        public String markerValue() {
            return facts.presetMarker();
        }

        /** The loaded provider triple, for diagnostics only. */
        public String providerTriple() {
            return facts.providerTriple();
        }
    }

    /** A plain description of one additional dimension's generator. */
    public record DimensionFacts(String key, String generatorClass, boolean modernerBeta,
        String presetMarker, boolean markerUnknown) {
        public DimensionFacts {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(generatorClass, "generatorClass");
            Objects.requireNonNull(presetMarker, "presetMarker");
        }

        /** True when this dimension carries the project's historical preset marker. */
        public boolean historicalMarker() {
            return net.historicalexperience.HistoricalMarker.isHistoricalMarker(presetMarker);
        }
    }

    /**
     * A plain description of every generator a running server has loaded.
     *
     * <p>{@code primary} is <em>always</em> the overworld stem, never "whichever level happened to
     * be enumerated first". Marker based world classification must only ever consult the
     * overworld: an own-nethers or own-end marker previously caused an ordinary overworld world to
     * be adopted as historical because "some" level was marked.
     */
    public record ServerSettingsDescription(
        DimensionFacts primary,
        java.util.List<DimensionFacts> otherDimensions,
        boolean registryFailure,
        String failureDetail
    ) {
        public ServerSettingsDescription {
            Objects.requireNonNull(primary, "primary");
            Objects.requireNonNull(otherDimensions, "otherDimensions");
            failureDetail = failureDetail == null ? "" : failureDetail;
        }

        /**
         * Marker state of the overworld stem only.
         *
         * <p>This is the single value lifecycle code may branch on.
         */
        public MarkerState primaryMarkerState() {
            if (primary.markerUnknown()) {
                return MarkerState.UNKNOWN;
            }
            if (!primary.modernerBeta()) {
                return MarkerState.NOT_MODERNER_BETA;
            }
            return primary.historicalMarker() ? MarkerState.HISTORICAL : MarkerState.OTHER;
        }

        /** True when the overworld stem could not be described from the registry at all. */
        public boolean primaryMissing() {
            return registryFailure;
        }

        /**
         * True when the overworld's marker itself could not be resolved, for any reason.
         *
         * <p>Distinct from {@link #primaryMissing()}: a description can succeed while the marker
         * remains unreadable. Both are failure evidence, and neither may ever be read as "this
         * world carries no marker" - that reading would adopt an unknown world into the ordinary
         * branch, which the policy forbids.
         */
        public boolean primaryMarkerUnknown() {
            return primary.markerUnknown();
        }

        /** Human readable explanation of why the marker could not be resolved, or empty. */
        public String failureReason() {
            if (registryFailure) {
                return failureDetail.isEmpty() ? "registry unavailable" : failureDetail;
            }
            if (primaryMarkerUnknown()) {
                return failureDetail.isEmpty()
                    ? "the overworld generator's preset marker could not be read"
                    : failureDetail;
            }
            return "";
        }
    }

    /**
     * Describes a loaded generator: facts plus how it relates to the historical marker.
     *
     * <p>This is the only adapter call lifecycle and probe code needs.
     *
     * <p>Two different "no marker" outcomes are deliberately kept apart:
     * <ul>
     *   <li>A generator that is not a Moderner Beta generator at all is reported as
     *       {@link MarkerState#NOT_MODERNER_BETA} with a resolved (non-unknown) marker state. This
     *       is decided from the generator's own runtime type and needs no registry of any kind, so
     *       an ordinary vanilla world can never be made to fail closed by a Moderner Beta registry
     *       problem.</li>
     *   <li>A Moderner Beta generator whose preset reference cannot be resolved - its own preset
     *       registry is absent, or resolution throws - is reported as {@link MarkerState#UNKNOWN}.
     *       That is an unresolved answer, never a silent "no marker".</li>
     * </ul>
     */
    public static GeneratorDescription describeGenerator(ChunkGenerator generator) {
        Objects.requireNonNull(generator, "generator");
        if (!(generator instanceof ModernBetaChunkGenerator modernBeta)) {
            // Non Moderner Beta: the marker question is answered by the runtime type alone.
            // No registry is consulted, so no registry failure can be fabricated here.
            return new GeneratorDescription(
                describeGeneratorWithoutRegistry(generator),
                MarkerState.NOT_MODERNER_BETA,
                true,
                "");
        }
        return describeGenerator(generator, modernBeta.getPresetRegistry());
    }

    /**
     * Describes a generator against an explicitly supplied preset registry.
     *
     * <p>A non Moderner Beta generator is still classified as {@link MarkerState#NOT_MODERNER_BETA},
     * because its classification does not depend on any registry. Only a Moderner Beta generator
     * with an unreachable registry, or one whose resolution throws, yields
     * {@link MarkerState#UNKNOWN} rather than a false negative.
     *
     * @param presetRegistry the registry used to resolve the preset reference; {@code null} means
     *                       "registry unavailable", which yields {@link MarkerState#UNKNOWN} for a
     *                       Moderner Beta generator rather than a false negative
     */
    public static GeneratorDescription describeGenerator(
        ChunkGenerator generator,
        HolderGetter<ModernBetaSettingsPreset> presetRegistry
    ) {
        Objects.requireNonNull(generator, "generator");
        if (!(generator instanceof ModernBetaChunkGenerator modernBeta)) {
            return new GeneratorDescription(
                describeGeneratorWithoutRegistry(generator),
                MarkerState.NOT_MODERNER_BETA,
                true,
                "");
        }
        // Explicit null means unavailable; never override caller-supplied failure evidence.
        HolderGetter<ModernBetaSettingsPreset> registry = presetRegistry;
        if (registry == null) {
            return new GeneratorDescription(
                describeGeneratorWithoutRegistry(generator),
                MarkerState.UNKNOWN,
                false,
                "Moderner Beta preset registry unavailable");
        }
        try {
            GeneratorFacts facts = resolveFacts(generator, registry);
            MarkerState state = facts.hasHistoricalMarker()
                ? MarkerState.HISTORICAL
                : MarkerState.OTHER;
            return new GeneratorDescription(facts, state, true, "");
        } catch (RuntimeException e) {
            return new GeneratorDescription(
                describeGeneratorWithoutRegistry(generator),
                MarkerState.UNKNOWN,
                false,
                e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Marker state of a generator, without exposing Moderner Beta types to the caller.
     */
    public static MarkerState markerStateOf(ChunkGenerator generator) {
        return describeGenerator(generator).markerState();
    }

    /**
     * Describes every level stem a running server has loaded.
     *
     * <p>Deliberately distinguishes three different outcomes, none of which may be conflated:
     * <ul>
     *   <li>The overworld stem could not be reached at all: {@code registryFailure}, a fail-closed
     *       condition. Never "nothing is marked".</li>
     *   <li>The overworld stem was reached but its preset marker could not be resolved: the primary
     *       {@link DimensionFacts} reports {@code markerUnknown}. Also a fail-closed condition, and
     *       callers must test {@link ServerSettingsDescription#primaryMarkerUnknown()} rather than
     *       only {@code registryFailure}.</li>
     *   <li>The overworld stem was reached and its marker resolved: an unmarked overworld is an
     *       ordinary world and is left untouched.</li>
     * </ul>
     *
     * <p>Only the overworld decides. Marker states of other dimensions are diagnostics.
     */
    public static ServerSettingsDescription describeServerSettings(
        net.minecraft.server.MinecraftServer server
    ) {
        Objects.requireNonNull(server, "server");
        try {
            // Use exactly the access vanilla validates in its constructor and createLevels.
            // A failure remains failure evidence; do not substitute another registry access.
            net.minecraft.core.Registry<net.minecraft.world.level.dimension.LevelStem> stems =
                server.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.LEVEL_STEM);
            net.minecraft.world.level.dimension.LevelStem overworldStem =
                stems.getOptional(net.minecraft.world.level.dimension.LevelStem.OVERWORLD).orElse(null);
            if (overworldStem == null || overworldStem.generator() == null) {
                return new ServerSettingsDescription(
                    new DimensionFacts(
                        net.minecraft.world.level.dimension.LevelStem.OVERWORLD.location()
                            .toString(),
                        "<absent>", false, "<absent>", true),
                    java.util.List.of(),
                    true,
                    "the overworld level stem exists but carries no generator");
            }
            DimensionFacts primary = dimensionFacts(
                net.minecraft.world.level.dimension.LevelStem.OVERWORLD, overworldStem.generator());
            java.util.List<DimensionFacts> others = new java.util.ArrayList<>();
            for (net.minecraft.resources.ResourceKey<
                    net.minecraft.world.level.dimension.LevelStem> key : stems.registryKeySet()) {
                if (key.equals(net.minecraft.world.level.dimension.LevelStem.OVERWORLD)) {
                    continue;
                }
                net.minecraft.world.level.dimension.LevelStem stem = stems.getOptional(key).orElse(null);
                if (stem != null && stem.generator() != null) {
                    others.add(dimensionFacts(key, stem.generator()));
                }
            }
            return new ServerSettingsDescription(primary, java.util.List.copyOf(others), false, "");
        } catch (RuntimeException e) {
            // The cause is reported verbatim. A swallowed exception previously made "the marker
            // could not be read" indistinguishable from "the registry is missing", and also hid a
            // Moderner Beta NullPointerException behind a registry-unavailable message.
            String detail = describeThrowable(e);
            net.historicalexperience.HistoricalExperienceMod.LOGGER.error(
                "The overworld level stem could not be described at this lifecycle point ({}); "
                    + "the overworld's historical marker is therefore unknown and the server will "
                    + "refuse to guess. Registry keys visible through the composite access: {}",
                detail,
                compositeRegistryKeys(server),
                e);
            return new ServerSettingsDescription(
                new DimensionFacts("<unavailable>", "<unavailable>", false, "<unavailable>", true),
                java.util.List.of(),
                true,
                detail);
        }
    }

    /** Exception class and message, never a silent placeholder. */
    private static String describeThrowable(Throwable throwable) {
        if (throwable == null) {
            return "<no exception>";
        }
        return throwable.getClass().getName() + ": " + throwable.getMessage();
    }

    /**
     * The registry keys visible through the server's composite registry access, as a diagnostic.
     *
     * <p>This is what distinguishes "the {@code minecraft:dimension} registry is genuinely absent
     * from this access" from "the lookup itself failed". It reports and never throws.
     */
    private static String compositeRegistryKeys(net.minecraft.server.MinecraftServer server) {
        try {
            java.util.List<String> keys = new java.util.ArrayList<>();
            server.registryAccess().registries().map(entry -> entry.key())
                .forEach(key -> keys.add(String.valueOf(key)));
            return keys.toString();
        } catch (RuntimeException e) {
            return "<unavailable: " + describeThrowable(e) + ">";
        }
    }

    private static DimensionFacts dimensionFacts(
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.dimension.LevelStem> key,
        ChunkGenerator generator
    ) {
        GeneratorDescription description = describeGenerator(generator);
        return new DimensionFacts(
            key.location().toString(),
            description.facts().generatorClass(),
            description.facts().modernerBeta(),
            description.facts().presetMarker(),
            description.markerUnknown());
    }

    /**
     * Sentinel for a terrain value that could not be read yet.
     *
     * <p>Distinct from any real value and clearly non-physical, so a diagnostic consumer cannot
     * mistake "not readable at this lifecycle point" for a measured height.
     */
    static final int TERRAIN_UNKNOWN = Integer.MIN_VALUE;

    /** {@code generator.getMinY()} without letting a generator side failure escape. */
    private static int safeMinY(ChunkGenerator generator) {
        try {
            return generator.getMinY();
        } catch (RuntimeException e) {
            return TERRAIN_UNKNOWN;
        }
    }

    /** {@code generator.getGenDepth()} without letting a generator side failure escape. */
    private static int safeGenDepth(ChunkGenerator generator) {
        try {
            return generator.getGenDepth();
        } catch (RuntimeException e) {
            return TERRAIN_UNKNOWN;
        }
    }

    /**
     * {@code generator.getSeaLevel()} without letting a generator side failure escape.
     *
     * <p>This matters for the fail-closed policy: Moderner Beta's
     * {@code ModernBetaChunkGenerator.getSeaLevel()} dereferences its {@code chunkProvider}, which
     * is assigned by {@code ModernBetaChunkGenerator.initProvider(long)} in its
     * {@code SERVER_STARTING} listener. If our listener runs first, reading sea level can throw
     * {@link NullPointerException}. A terrain diagnostic must degrade to {@link #TERRAIN_UNKNOWN}
     * rather than masquerade as a registry failure. Post-start probes must require known terrain
     * values before accepting generation evidence.
     */
    private static int safeSeaLevel(ChunkGenerator generator) {
        try {
            return generator.getSeaLevel();
        } catch (RuntimeException e) {
            net.historicalexperience.HistoricalExperienceMod.LOGGER.warn(
                "Sea-level diagnostic unavailable for {}; marker classification is independent "
                    + "of terrain-provider readiness",
                generator.getClass().getName(), e);
            return TERRAIN_UNKNOWN;
        }
    }

    /** Generator facts for a specific, caller supplied preset registry. */
    private static GeneratorFacts resolveFacts(
        ChunkGenerator generator,
        HolderGetter<ModernBetaSettingsPreset> presetRegistry
    ) {
        Objects.requireNonNull(generator, "generator");
        String generatorClass = generator.getClass().getName();
        // Terrain dimensions are read defensively: see safeSeaLevel for why this must not throw
        // at SERVER_STARTING.
        int minY = safeMinY(generator);
        int genDepth = safeGenDepth(generator);
        int seaLevel = safeSeaLevel(generator);

        if (!(generator instanceof ModernBetaChunkGenerator modernBeta)) {
            return new GeneratorFacts(generatorClass, false, "<not-a-moderner-beta-generator>",
                "<n/a>", "<n/a>", "<n/a>", minY, genDepth, seaLevel);
        }

        // The generator's stored settings carry the preset REFERENCE; a block's provider
        // component is only present after the reference has been resolved. Each stored block is
        // mapped through ITS OWN accessor, because that is exactly what the providers consume:
        // the chunk block through ModernBetaSettingsPreset::chunkSettings (the provider path in
        // ChunkProvider), and the biome and cave biome blocks through ::biomeSettings and
        // ::caveBiomeSettings (ModernBetaBiomeSource.initProvider). Mapping a single stored block
        // with the wrong accessor resolves all three blocks through that one accessor and makes
        // the biome and cave providers collapse onto it.
        //
        // Mapping the stored blocks - rather than the preset they reference - is what keeps local
        // overrides visible: a stored block may add components on top of the reference, and only
        // mapping the stored block itself applies them.
        // The supplied registry is authoritative; unavailable evidence must not be substituted.
        HolderGetter<ModernBetaSettingsPreset> registry =
            Objects.requireNonNull(presetRegistry, "presetRegistry");
        ModernBetaSettings resolvedChunk = mapStoredBlock(modernBeta.getChunkSettings(), registry,
            ModernBetaSettingsPreset::chunkSettings, "chunk");
        ModernBetaSettings resolvedBiome = null;
        ModernBetaSettings resolvedCave = null;
        ModernBetaBiomeSource biomeSource = null;

        if (modernBeta.getBiomeSource() instanceof ModernBetaBiomeSource source) {
            biomeSource = source;
            resolvedBiome = mapStoredBlock(source.getBiomeSettings(), registry,
                ModernBetaSettingsPreset::biomeSettings, "biome");
            resolvedCave = mapStoredBlock(source.getCaveBiomeSettings(), registry,
                ModernBetaSettingsPreset::caveBiomeSettings, "cave biome");
        }

        String biomeProvider = "<not-a-moderner-beta-biome-source>";
        String caveBiomeProvider = "<not-a-moderner-beta-biome-source>";
        if (biomeSource != null) {
            biomeProvider = providerOf(resolvedBiome, biomeSource.getBiomeProvider(),
                ModernBetaRegistries.BIOME);
            caveBiomeProvider = providerOf(resolvedCave, biomeSource.getCaveBiomeProvider(),
                ModernBetaRegistries.CAVE_BIOME);
        }

        String marker = explicitPresetMarker(generator)
            .map(ResourceLocation::toString)
            .orElse("<none>");

        return new GeneratorFacts(
            generatorClass,
            true,
            marker,
            providerOf(resolvedChunk, modernBeta.getChunkProvider(), ModernBetaRegistries.CHUNK),
            biomeProvider,
            caveBiomeProvider,
            minY,
            genDepth,
            seaLevel);
    }

    /**
     * Best-effort generator facts when no preset registry is available.
     *
     * <p>Reports the unresolved stored settings and the live provider keys only. It never resolves
     * a preset reference, because doing so is exactly what needs the registry.
     */
    private static GeneratorFacts describeGeneratorWithoutRegistry(ChunkGenerator generator) {
        String generatorClass = generator.getClass().getName();
        // Same defensive reads as resolveFacts. This method is reached from describeGenerator's
        // catch block, so it must be strictly less likely to throw than the code that failed -
        // otherwise it re-throws and its caller reports an unrelated "registry unavailable".
        int minY = safeMinY(generator);
        int genDepth = safeGenDepth(generator);
        int seaLevel = safeSeaLevel(generator);
        if (!(generator instanceof ModernBetaChunkGenerator modernBeta)) {
            return new GeneratorFacts(generatorClass, false, "<not-a-moderner-beta-generator>",
                "<n/a>", "<n/a>", "<n/a>", minY, genDepth, seaLevel);
        }
        String biomeProvider = "<not-a-moderner-beta-biome-source>";
        String caveBiomeProvider = "<not-a-moderner-beta-biome-source>";
        if (modernBeta.getBiomeSource() instanceof ModernBetaBiomeSource source) {
            biomeProvider = providerOf(source.getBiomeSettings(), source.getBiomeProvider(),
                ModernBetaRegistries.BIOME);
            caveBiomeProvider = providerOf(source.getCaveBiomeSettings(),
                source.getCaveBiomeProvider(), ModernBetaRegistries.CAVE_BIOME);
        }
        return new GeneratorFacts(
            generatorClass,
            true,
            explicitPresetMarker(generator).map(ResourceLocation::toString).orElse("<none>"),
            providerOf(modernBeta.getChunkSettings(), modernBeta.getChunkProvider(),
                ModernBetaRegistries.CHUNK),
            biomeProvider,
            caveBiomeProvider,
            minY,
            genDepth,
            seaLevel);
    }

    /**
     * The provider identifier for a settings block, preferring resolved settings and falling back
     * to the live provider object's registry key when the component is absent.
     *
     * @param registry the specific provider registry that can key the live provider, so a key is
     *                 never looked up in the wrong registry
     */
    private static String providerOf(
        ModernBetaSettings resolved,
        Object liveProvider,
        net.minecraft.core.Registry<?> registry
    ) {
        if (resolved != null) {
            ResourceLocation provider = resolved.get(SettingsComponentTypes.PROVIDER);
            if (provider != null) {
                return provider.toString();
            }
        }
        if (liveProvider != null && registry != null) {
            @SuppressWarnings("unchecked")
            net.minecraft.core.Registry<Object> typed =
                (net.minecraft.core.Registry<Object>) registry;
            ResourceLocation id = typed.getKey(liveProvider);
            if (id != null) {
                return id.toString();
            }
        }
        return "<unresolved>";
    }

    /**
     * Reduces one settings block the generator was constructed with to the form its provider
     * consumes, by walking its own preset reference with the accessor that belongs to that block.
     *
     * <p>When the block carries no {@code moderner_beta:preset} component the settings are already
     * a fully expanded map and are returned unchanged - {@code mapPreset} is the identity in that
     * case - so a world created from expanded settings is described correctly too.
     *
     * @param accessor the accessor for this block: {@code ModernBetaSettingsPreset::chunkSettings},
     *                 {@code ::biomeSettings} or {@code ::caveBiomeSettings}. The accessor must
     *                 match the block, otherwise the block's own reference is resolved through the
     *                 wrong settings and all three providers collapse onto one.
     * @param blockName block name for failure messages only
     * @throws ModernerBetaIntegrationException when the block references a preset that is not
     *         registered, or when resolution does not terminate; never a silent fallback
     */
    private static ModernBetaSettings mapStoredBlock(
        ModernBetaSettings stored,
        HolderGetter<ModernBetaSettingsPreset> presetRegistry,
        java.util.function.Function<ModernBetaSettingsPreset, ModernBetaSettings> accessor,
        String blockName
    ) {
        Objects.requireNonNull(stored, "stored " + blockName + " settings");
        Objects.requireNonNull(accessor, "accessor for " + blockName);
        try {
            return stored.mapPreset(Objects.requireNonNull(presetRegistry, "presetRegistry"), accessor);
        } catch (RuntimeException e) {
            throw new ModernerBetaIntegrationException(
                "The generator's applied " + blockName + " settings could not be resolved from "
                    + "their preset reference " + rawReference(stored) + "; refusing to describe "
                    + "settings that were never actually applied", e);
        }
    }

    /** The raw {@code moderner_beta:preset} component of a settings block, or an explicit marker. */
    private static String rawReference(ModernBetaSettings settings) {
        if (settings == null) {
            return "<no-settings>";
        }
        ResourceLocation preset = settings.get(SettingsComponentTypes.PRESET);
        return preset == null ? "<expanded>" : preset.toString();
    }

    /**
     * The provider triple produced by resolving the pinned upstream {@code moderner_beta:beta}
     * preset from the live registry.
     *
     * <p>Diagnostic only. Provider identifiers are shared between Moderner Beta presets, so a
     * match is never evidence of preset identity and must never be used to classify a world. Use
     * {@link #compareToUpstreamBeta173(ChunkGenerator)} to compare settings rather than ids.
     */
    public static String upstreamBeta173ProviderTriple(ChunkGenerator generator) {
        Objects.requireNonNull(generator, "generator");
        if (!(generator instanceof ModernBetaChunkGenerator modernBeta)) {
            return "<not-a-moderner-beta-generator>";
        }
        HolderGetter<ModernBetaSettingsPreset> presetRegistry = modernBeta.getPresetRegistry();
        // resolveUpstreamBeta173 returns a preset REFERENCE, whose settings blocks carry the
        // preset component but no concrete provider. Mapping each block with its OWN accessor is
        // what fills the provider in; reading getProvider() off unmapped settings throws.
        ModernBetaSettingsPreset upstream =
            resolveUpstreamBeta173(presetRegistry).mapped(presetRegistry);
        return upstream.chunkSettings().getProvider() + "/"
            + upstream.biomeSettings().getProvider() + "/"
            + upstream.caveBiomeSettings().getProvider();
    }

    /**
     * True when the loaded generator's provider triple equals the pinned upstream preset's.
     * Diagnostic only; see {@link #upstreamBeta173ProviderTriple(ChunkGenerator)}.
     */
    public static boolean providersMatchUpstreamBeta173(ChunkGenerator generator) {
        GeneratorFacts facts = describeGenerator(generator).facts();
        if (!facts.modernerBeta()) {
            return false;
        }
        return facts.providerTriple().equals(upstreamBeta173ProviderTriple(generator));
    }

    /** Moderner Beta registry key for settings presets. */
    public static ResourceKey<Registry<ModernBetaSettingsPreset>> settingsPresetRegistryKey() {
        return ModernBetaResourceKeys.SETTINGS_PRESET;
    }

    /** Moderner Beta registry key for surface configs. */
    public static ResourceKey<Registry<SurfaceConfig>> surfaceConfigRegistryKey() {
        return ModernBetaResourceKeys.SURFACE_CONFIG;
    }
}
