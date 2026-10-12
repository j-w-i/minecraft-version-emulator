package net.historicalexperience;

import net.minecraft.resources.ResourceLocation;
/**
 * Stable identifiers for the Historical Experience vertical slice.
 *
 * <p>The distinction between a <em>gameplay profile</em> and a <em>generation preset</em> is
 * deliberate and load-bearing: a profile describes which era of the game is being emulated
 * (rules, availability, progression), while a generation preset describes only which terrain
 * generator produces the world. A profile may be backed by exactly one generation preset, but
 * the two identifiers are persisted separately so that either can be migrated independently.
 */
public final class HistoricalExperience {
    public static final String MOD_ID = "historical_experience";

    private HistoricalExperience() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * Profile identifiers owned by this mod. These are gameplay profiles; the matching terrain
     * is supplied by {@link #GENERATION_PRESET_BETA_1_7_3}.
     *
     * <p>Only profiles whose gameplay semantics are actually implemented may be listed here. This
     * slice implements terrain binding only, so the single profile below is explicitly described
     * as terrain-only and no era gameplay is claimed.
     */
    public static final class Profiles {
        /**
         * Beta 1.7.3 profile. Terrain, generation content and several simulation rules are
         * implemented and measured; many other subsystems remain unimplemented, so this is still not
         * a complete historical gameplay profile.
         */
        public static final String BETA_1_7_3 = MOD_ID + ":beta_1_7_3";

        /**
         * Beta 1.8.1 profile: the "Adventure Update" boundary, where sprinting, hunger and experience
         * arrive. Its rule set is declared so the shared rules engine can select different behaviour
         * for a second version; its historical details are explicitly provisional until verified from
         * a b1.8.1 client jar.
         */
        public static final String BETA_1_8_1 = MOD_ID + ":beta_1_8_1";

        private Profiles() {
        }
    }

    /**
     * Generation preset identifiers.
     *
     * <p>{@code historical_experience:beta_1_7_3} is owned by this project and is the preset a
     * historical world actually references. Its settings are a scoped override of Moderner Beta's
     * {@code moderner_beta:beta}, declared in
     * {@code data/historical_experience/moderner_beta/settings_preset/beta_1_7_3.json} using the
     * {@code moderner_beta:preset} inheritance component. It exists so that historical intent is
     * an explicit, durable, project owned reference rather than a guess about provider shapes.
     */
    public static final class GenerationPresets {
        /** Project owned settings preset that historical Beta 1.7.3 worlds reference. */
        public static final String HISTORICAL_BETA_1_7_3 =
            MOD_ID + ":beta_1_7_3";

        /** The pinned upstream preset the project preset inherits its terrain from. */
        public static final String MODERNER_BETA_BETA_1_7_3 = "moderner_beta:beta";

        private GenerationPresets() {
        }
    }
}
