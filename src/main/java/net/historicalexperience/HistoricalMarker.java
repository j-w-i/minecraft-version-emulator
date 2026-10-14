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
package net.historicalexperience;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

/**
 * The durable historical world marker, in a class with no Moderner Beta dependency.
 *
 * <p>Why this class exists: {@code ModernerBetaAdapter}'s static initialiser builds Moderner Beta
 * registry keys, which throws unless the mod's registries have been bootstrapped. Any code that
 * only needs to know <em>which preset identifies a historical world</em> must therefore not touch
 * the adapter, or it becomes untestable without a Minecraft runtime. Marker identity lives here so
 * that the classification policy and its unit tests stay pure.
 *
 * <p><b>Only the identifiers in {@link #HISTORICAL_PRESET_IDS} are project owned and
 * recognised</b>, and every one of them is a <em>generation recipe</em> identifier, not a gameplay
 * profile identifier:
 * <ul>
 *   <li>{@link #HISTORICAL_PRESET_ID} - the frozen v1 terrain declaration. Preserved byte for
 *       byte, including in already established worlds, and still recognised.</li>
 *   <li>{@link #CONTENT_PREVIEW_PRESET_ID} - content-preview revision 1: deepslate generation and
 *       cave-biome injection removed, still on the vanilla level envelope.</li>
 *   <li>{@link #CONTENT_PREVIEW_2_PRESET_ID} - content-preview revision 2: the same content deltas
 *       plus the reduced level envelope, so the level no longer spans the modern negative-Y range.
 *       Revision 1 is <b>not</b> retroactively reinterpreted: it stays recognised and unchanged,
 *       because existing worlds persist it. Revision 2 is likewise kept, and is known to have
 *       generated zero bedrock because its noise envelope was still unreduced.</li>
 *   <li>{@link #CONTENT_PREVIEW_3_PRESET_ID} - content-preview revision 3: the same content deltas
 *       on the reduced level envelope <em>and</em> Moderner Beta's published reduced-height noise
 *       envelope, which is what restores the bedrock column.</li>
 * </ul>
 *
 * <p>Each identifier itself carries its generation revision ({@code ..._preview_N}), which is what
 * makes a persisted recipe identifier an immutable, non-reinterpretable record: a revision is a new
 * identifier, never an edit to an old one.
 *
 * <p>The pinned upstream {@code moderner_beta:beta} preset is the <em>foundation</em> both project
 * presets inherit from; it is deliberately <b>not</b> a marker. A plain upstream Beta world is an
 * ordinary Moderner Beta world and must never be adopted as historical, and neither must any other
 * namespace's preset that merely resembles one.
 */
public final class HistoricalMarker {
    /** The project owned v1 settings preset identifier, as a plain string. */
    public static final String HISTORICAL_PRESET_ID =
        HistoricalExperience.GenerationPresets.HISTORICAL_BETA_1_7_3;

    /** The project owned v1 settings preset identifier a historical world reports. */
    public static final ResourceLocation HISTORICAL_PRESET =
        ResourceLocation.parse(HISTORICAL_PRESET_ID);

    /**
     * The project owned content-preview generation-recipe identifier.
     *
     * <p>Declared here as a literal rather than read from {@code HistoricalExperience} so that this
     * marker, which metadata and classification key off, cannot drift when that identifier table is
     * reorganised. The resource documents and the profile binding are asserted against this value.
     */
    public static final String CONTENT_PREVIEW_PRESET_ID =
        HistoricalExperience.MOD_ID + ":beta_1_7_3_content_preview_1";

    /** The project owned content-preview revision 1 generation-recipe identifier. */
    public static final ResourceLocation CONTENT_PREVIEW_PRESET =
        ResourceLocation.parse(CONTENT_PREVIEW_PRESET_ID);

    /**
     * The project owned content-preview <b>revision 2</b> generation-recipe identifier.
     *
     * <p>A separate literal, not a computed {@code ..._preview_2}: the revision suffix is part of the
     * persisted record, so it must be spelled out where it can be diffed and asserted, exactly like
     * revision 1.
     */
    public static final String CONTENT_PREVIEW_2_PRESET_ID =
        HistoricalExperience.MOD_ID + ":beta_1_7_3_content_preview_2";

    /** The project owned content-preview revision 2 generation-recipe identifier. */
    public static final ResourceLocation CONTENT_PREVIEW_2_PRESET =
        ResourceLocation.parse(CONTENT_PREVIEW_2_PRESET_ID);

    /**
     * The project owned content-preview <b>revision 3</b> generation-recipe identifier.
     *
     * <p>Revision 3 completes the envelope adaptation by also adopting Moderner Beta's published
     * reduced-height <em>noise</em> settings. Revision 2 reduced only the dimension type, which
     * measured zero bedrock at runtime: Moderner Beta places its bedrock column relative to the noise
     * envelope, which was still {@code min_y=-64} and therefore entirely below the level floor. The
     * defect is recorded rather than papered over, and revision 2 stays recognised because a probe
     * world already persisted it.
     */
    public static final String CONTENT_PREVIEW_3_PRESET_ID =
        HistoricalExperience.MOD_ID + ":beta_1_7_3_content_preview_3";

    /** The project owned content-preview revision 3 generation-recipe identifier. */
    public static final ResourceLocation CONTENT_PREVIEW_3_PRESET =
        ResourceLocation.parse(CONTENT_PREVIEW_3_PRESET_ID);

    /**
     * The project owned content-preview <b>revision 4</b> generation-recipe identifier.
     *
     * <p>Revision 4 is the same settings as revision 3 plus the first researched
     * <b>generation-time content restrictions</b>: an append-only per-revision policy suppresses
     * placed features that Beta 1.7.3 demonstrably did not have. Those features are attached to
     * biomes in Moderner Beta's Java biome builders and cannot be expressed in configuration, so the
     * restriction is selected by the revision alias rather than by a settings component. Revisions 1
     * to 3 keep generating exactly what they were created to generate.
     */
    public static final String CONTENT_PREVIEW_4_PRESET_ID =
        HistoricalExperience.MOD_ID + ":beta_1_7_3_content_preview_4";

    /** The project owned content-preview revision 4 generation-recipe identifier. */
    public static final ResourceLocation CONTENT_PREVIEW_4_PRESET =
        ResourceLocation.parse(CONTENT_PREVIEW_4_PRESET_ID);

    /**
     * The project owned content-preview <b>revision 5</b> generation-recipe identifier.
     *
     * <p>Revision 5 adds Beta 1.7.3 <b>ore placement</b>: the verified vein algorithm and the verified
     * per-ore parameters, with the vanilla ore features excluded so the two cannot double up. Ore
     * placement changes what a stored chunk contains, so unlike the spawn rule it needs its own
     * immutable revision, and revisions 1 to 4 keep generating exactly the ore placement their recipes
     * recorded.
     */
    public static final String CONTENT_PREVIEW_5_PRESET_ID =
        HistoricalExperience.MOD_ID + ":beta_1_7_3_content_preview_5";

    /** The project owned content-preview revision 5 generation-recipe identifier. */
    public static final ResourceLocation CONTENT_PREVIEW_5_PRESET =
        ResourceLocation.parse(CONTENT_PREVIEW_5_PRESET_ID);

    /** The project owned original declaration for the Beta 1.8.1 version. */
    public static final String BETA_1_8_1_PRESET_ID =
        HistoricalExperience.MOD_ID + ":beta_1_8_1";

    /** The project owned Beta 1.8.1 generation-recipe identifier. */
    public static final ResourceLocation BETA_1_8_1_PRESET =
        ResourceLocation.parse(BETA_1_8_1_PRESET_ID);

    /**
     * Every project owned marker identifier, derived from the version registry.
     *
     * <p>Derived rather than listed: a version declared in one place must not also have to be added
     * here, or a new version's worlds would be recognised by the rules engine and rejected by marker
     * classification. The registry test asserts that every declared revision alias is a marker, so the
     * two cannot drift apart.
     */
    public static final List<String> HISTORICAL_PRESET_IDS =
        net.historicalexperience.version.HistoricalVersions.allRevisionAliases();

    /** The pinned upstream preset this project's presets inherit their terrain from. */
    public static final String UPSTREAM_PRESET_ID =
        HistoricalExperience.GenerationPresets.MODERNER_BETA_BETA_1_7_3;

    /** The pinned upstream preset identifier. */
    public static final ResourceLocation UPSTREAM_PRESET =
        ResourceLocation.parse(UPSTREAM_PRESET_ID);

    /**
     * How a level's generator relates to this project's historical preset marker.
     *
     * <p>A tri-state rather than a boolean, because "the marker could not be read" must never be
     * collapsed into "no marker here": that collapse is how an ordinary world gets adopted as
     * historical.
     */
    public enum MarkerState {
        /** The generator carries one of this project's historical preset markers. */
        HISTORICAL,
        /** The generator is a Moderner Beta generator carrying a different, or no, marker. */
        OTHER,
        /** The generator is not a Moderner Beta generator at all. */
        NOT_MODERNER_BETA,
        /** The marker could not be read (the preset registry was unavailable). */
        UNKNOWN;

        /** True only when the marker positively identifies this project's historical preset. */
        public boolean isHistorical() {
            return this == HISTORICAL;
        }
    }

    private HistoricalMarker() {
    }

    /**
     * True when the raw marker identifier is exactly one of this project's owned markers.
     *
     * <p>Exact, case sensitive comparison against the closed list above. There is no prefix,
     * namespace or substring rule: {@code moderner_beta:beta} is the foundation, not a marker, and
     * any other namespace is unknown regardless of how similar its path looks.
     */
    public static boolean isHistoricalMarker(String rawMarker) {
        return rawMarker != null && HISTORICAL_PRESET_IDS.contains(rawMarker);
    }
}
