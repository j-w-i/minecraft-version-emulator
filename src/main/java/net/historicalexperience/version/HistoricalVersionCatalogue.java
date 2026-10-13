package net.historicalexperience.version;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every historical version this project intends to support, whether or not it is implemented yet.
 *
 * <p>The selector needs to show a hierarchy that is larger than the implementation, and it must do so without
 * inventing playable versions or quietly pointing an unsupported choice at a supported one. This catalogue is that
 * hierarchy: it MERGES the real registry with the versions still to come, and marks which is which.
 *
 * <h2>Why this is not the registry</h2>
 *
 * <p>{@link HistoricalVersions} declares only versions whose gameplay semantics actually exist, and
 * {@link HistoricalVersion} refuses to be constructed without rules and a generation revision. That is the right
 * constraint for behaviour and the wrong shape for a menu, so the menu needs a second, thinner description: a
 * stable key, a display name, an era, an order, and an implementation status.
 *
 * <p>Nothing here duplicates implementation data. An implemented entry's display name and world-generation
 * summary are READ from the registry at lookup time, so a version that gains a revision cannot go stale in the
 * menu, and a version that is added to the registry appears here without an edit.
 *
 * <h2>Targets versus profiles</h2>
 *
 * <p>The future matrix is expressed in the coarse names the project plans by - "Release 1.6", "Beta 1.7" - while
 * an implemented profile is a specific build, "Release 1.6.4". Where a target is represented by a specific
 * profile that is stated EXPLICITLY in {@link #representedBy}, never inferred, so the screen can say
 * "Beta 1.7 - emulated as Beta 1.7.3" rather than implying 1.7 exactly. Two implemented profiles are not targets
 * at all - both Infdev builds and the Alpha v1.1.2_01 profile - and they are listed as implemented extras under
 * their own eras rather than hidden or renamed.
 */
public final class HistoricalVersionCatalogue {
    /** The eras the selector groups by. Infdev is included because the project implements two of them. */
    public enum Era {
        INFDEV("Infdev", 300_000),
        ALPHA("Alpha", 400_000),
        BETA("Beta", 500_000),
        RELEASE("Release", 600_000);

        private final String displayName;
        private final int base;

        Era(String displayName, int base) {
            this.displayName = displayName;
            this.base = base;
        }

        /** The name a screen shows for this era. */
        public String displayName() {
            return this.displayName;
        }

        /** The base of this era's chronology, matching the registry's own scale. */
        public int chronologyBase() {
            return this.base;
        }
    }

    /** Whether a version can actually be played today. */
    public enum Status {
        IMPLEMENTED,
        FUTURE
    }

    /**
     * One row of the hierarchy.
     *
     * @param key        a stable identifier for the TARGET, not for the profile that may later represent it. An
     *                   implemented entry carries the registry id, because that is what a world persists. A future
     *                   entry carries a {@code planned_} key: it is deliberately not the profile id the version
     *                   will take, so that a target can never be mistaken for a playable version by anything
     *                   holding one - and it therefore CHANGES when the version is implemented, which is what the
     *                   test asserting the {@code planned_} prefix records. Nothing persists this key: a world
     *                   stores the profile id, and a menu selection is re-derived from the registry.
     * @param targetName the coarse name the project plans by, e.g. {@code Beta 1.7}
     * @param era        the group this belongs to
     * @param order      chronological key. For an implemented entry this IS the registry's chronology, taken from
     *                   it rather than computed; for a planned one it follows the registry's numbering scheme. A
     *                   test asserts the implemented case, because the registry hand-writes at least one
     *                   chronology that no formula covering the display name would reproduce.
     * @param status     implemented or still to come
     * @param emulated   for an implemented entry, the specific build it emulates; blank when it is the target
     *                   itself. This is what lets the screen say "Beta 1.7 - emulated as Beta 1.7.3"
     * @param futureNote what is missing, for a future entry; blank for an implemented one
     * @param target     whether this is one of the planned matrix's versions. False for an implemented profile
     *                   that the plan does not name - both Infdev builds and Alpha v1.1.2_01 - so a screen can
     *                   show those without counting them against the plan, and so a test can assert the matrix
     *                   size without inferring it from a name
     */
    public record Entry(
        String key,
        String targetName,
        Era era,
        int order,
        Status status,
        String emulated,
        String futureNote,
        boolean target
    ) {
        public Entry {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("a catalogue entry needs a key");
            }
            if (targetName == null || targetName.isBlank()) {
                throw new IllegalArgumentException("a catalogue entry needs a display name: " + key);
            }
            if (emulated == null) {
                emulated = "";
            }
            if (futureNote == null) {
                futureNote = "";
            }
        }

        /** Whether a screen may select this entry. */
        public boolean selectable() {
            return this.status == Status.IMPLEMENTED;
        }

        /** The registry entry behind this row, or null when the version is not implemented. */
        public HistoricalVersion version() {
            return this.status == Status.IMPLEMENTED ? HistoricalVersions.byId(this.key).orElse(null) : null;
        }

        /** The display name to show, read from the registry when implemented. */
        public String displayName() {
            HistoricalVersion declared = version();
            return declared != null ? declared.displayName() : this.targetName;
        }

        /**
         * A one-line description of what this version generates with, or why it cannot be chosen yet.
         *
         * <p>Read from the newest generation revision rather than stored here, so it cannot describe a recipe that
         * no longer exists.
         */
        public String summary() {
            HistoricalVersion declared = version();

            if (declared == null) {
                return this.futureNote;
            }

            HistoricalVersion.GenerationRevision revision = declared.latestRevision();
            return "generates with " + revision.alias() + " (from " + revision.foundation()
                + "), revision " + revision.revision();
        }
    }

    private static final List<Entry> ENTRIES = build();

    private HistoricalVersionCatalogue() {
    }

    /** Every version, implemented and planned, in chronological order. */
    public static List<Entry> entries() {
        return ENTRIES;
    }

    /** The eras, in chronological order, for a screen to group by. */
    public static List<Era> eras() {
        return List.of(Era.values());
    }

    /** The versions in one era, chronological. */
    public static List<Entry> entriesIn(Era era) {
        return ENTRIES.stream().filter(entry -> entry.era() == era).toList();
    }

    /** Lookup by stable key. */
    public static Optional<Entry> byKey(String key) {
        return ENTRIES.stream().filter(entry -> entry.key().equals(key)).findFirst();
    }

    /** Only the versions that can be played today. */
    public static List<Entry> implemented() {
        return ENTRIES.stream().filter(Entry::selectable).toList();
    }

    /**
     * The planned matrix: the versions the expansion stage is expected to implement, whether or not they exist
     * yet. This is what "all twenty-nine versions" means, and it is a declared flag rather than a name test, so a
     * profile that the plan does not name cannot be miscounted as one.
     */
    public static List<Entry> plannedTargets() {
        return ENTRIES.stream().filter(Entry::target).toList();
    }

    /**
     * Versions that exist but that the plan does not name - both Infdev builds and the Alpha v1.1.2_01 profile.
     * A screen shows them so they stay reachable; a count of the plan excludes them.
     */
    public static List<Entry> implementedExtras() {
        return ENTRIES.stream().filter(entry -> !entry.target() && entry.selectable()).toList();
    }

    /** How many versions are still to come, which is what the expansion stage will implement. */
    public static List<Entry> future() {
        return ENTRIES.stream().filter(entry -> !entry.selectable()).toList();
    }

    private static List<Entry> build() {
        List<Entry> entries = new ArrayList<>();

        // Implemented profiles that are NOT part of the planned matrix. Listed so they stay reachable, under
        // their own names, rather than being hidden or renamed to fit a list they predate.
        // Infdev builds are dated, and the registry keys them as era + month*100 + day (20100420 -> 300_420),
        // so the year is not part of the ordering and must not be.
        entries.add(new Entry("historical_experience:infdev_420", "Infdev 20100420", Era.INFDEV,
            chronologyOf("historical_experience:infdev_420", Era.INFDEV, 0, 4, 20), Status.IMPLEMENTED, "", "", false));
        entries.add(new Entry("historical_experience:infdev_611", "Infdev 20100611", Era.INFDEV,
            chronologyOf("historical_experience:infdev_611", Era.INFDEV, 0, 6, 11), Status.IMPLEMENTED, "", "", false));
        entries.add(new Entry("historical_experience:alpha", "Alpha v1.1.2_01", Era.ALPHA,
            chronologyOf("historical_experience:alpha", Era.ALPHA, 1, 1, 2), Status.IMPLEMENTED, "Alpha 1.1.2_01",
            "achieved; not one of the planned Alpha targets", false));

        // The planned matrix. A target that is already implemented carries the profile that represents it and
        // says so; a target that is not carries the reason it cannot be chosen.
        planned(entries, Era.ALPHA, "Alpha 1.0.1", 1, 0, 1, "historical_experience:alpha_1_0_1", null);
        planned(entries, Era.ALPHA, "Alpha 1.0.4", 1, 0, 4, "historical_experience:alpha_1_0_4", null);
        planned(entries, Era.ALPHA, "Alpha 1.0.6", 1, 0, 6, null, null);
        planned(entries, Era.ALPHA, "Alpha 1.0.11", 1, 0, 11, null, null);
        planned(entries, Era.ALPHA, "Alpha 1.0.14", 1, 0, 14, null, null);
        planned(entries, Era.ALPHA, "Alpha 1.0.17", 1, 0, 17, null, null);
        planned(entries, Era.ALPHA, "Alpha 1.1.0", 1, 1, 0, null, null);
        planned(entries, Era.ALPHA, "Alpha 1.2.0", 1, 2, 0, null, null);

        planned(entries, Era.BETA, "Beta 1.0", 1, 0, 0, null, null);
        planned(entries, Era.BETA, "Beta 1.2", 1, 2, 0, null, null);
        planned(entries, Era.BETA, "Beta 1.3", 1, 3, 0, null, null);
        planned(entries, Era.BETA, "Beta 1.4", 1, 4, 0, null, null);
        planned(entries, Era.BETA, "Beta 1.5", 1, 5, 0, null, null);
        planned(entries, Era.BETA, "Beta 1.6", 1, 6, 0, null, null);
        planned(entries, Era.BETA, "Beta 1.7", 1, 7, 0, "historical_experience:beta_1_7_3", "Beta 1.7.3");
        planned(entries, Era.BETA, "Beta 1.8", 1, 8, 0, "historical_experience:beta_1_8_1", "Beta 1.8.1");
        planned(entries, Era.BETA, "Beta 1.9 Prerelease 1", 1, 9, 1, null, null);
        planned(entries, Era.BETA, "Beta 1.9 Prerelease 2", 1, 9, 2, null, null);
        planned(entries, Era.BETA, "Beta 1.9 Prerelease 3", 1, 9, 3, "historical_experience:beta_1_9_pre_3",
            null);
        planned(entries, Era.BETA, "Beta 1.9 Prerelease 4", 1, 9, 4, null, null);
        planned(entries, Era.BETA, "Beta 1.9 Prerelease 5", 1, 9, 5, null, null);
        planned(entries, Era.BETA, "Beta 1.9 Prerelease 6", 1, 9, 6, null, null);

        planned(entries, Era.RELEASE, "Release 1.0", 1, 0, 0, "historical_experience:release_1_0_0", null);
        planned(entries, Era.RELEASE, "Release 1.1", 1, 1, 0, "historical_experience:release_1_1", null);
        planned(entries, Era.RELEASE, "Release 1.2", 1, 2, 0, "historical_experience:release_1_2_5",
            "Release 1.2.5");
        planned(entries, Era.RELEASE, "Release 1.3", 1, 3, 0, null, null);
        planned(entries, Era.RELEASE, "Release 1.4", 1, 4, 0, null, null);
        planned(entries, Era.RELEASE, "Release 1.5", 1, 5, 0, null, null);
        planned(entries, Era.RELEASE, "Release 1.6", 1, 6, 0, "historical_experience:release_1_6_4",
            "Release 1.6.4");

        entries.sort(Comparator.comparingInt(Entry::order).thenComparing(Entry::targetName));
        rejectDuplicates(entries);
        return List.copyOf(entries);
    }

    /**
     * Adds one planned target, using the profile that represents it when there is one.
     *
     * <p>The key of a future entry is the id that version will carry when it is implemented, so nothing has to be
     * renumbered later. An implemented target uses the profile's own id instead, because that is what worlds
     * already persist.
     */
    private static void planned(List<Entry> entries, Era era, String targetName, int major, int minor,
            int patch, String representedBy, String emulatedBuild) {
        String key = representedBy != null ? representedBy : plannedKey(era, major, minor, patch);
        Status status = representedBy != null ? Status.IMPLEMENTED : Status.FUTURE;
        String note = status == Status.IMPLEMENTED ? "" : "no gameplay profile or generation recipe yet";
        String emulated = status == Status.IMPLEMENTED ? (emulatedBuild == null ? "" : emulatedBuild) : "";

        entries.add(new Entry(key, targetName, era, chronologyOf(key, era, major, minor, patch), status, emulated, note,
            true));
    }

    /**
     * The key for a target that has no profile yet.
     *
     * <p>Namespaced under {@code planned_} on purpose. A planned key must not look like a profile id, because
     * anything holding one would then appear to name a playable version - and the two must never be confused when
     * a world's metadata or a menu selection is resolved. When the version is implemented, this entry gains a
     * {@code representedBy} profile and the key becomes that profile's id, which is what worlds persist.
     */
    private static String plannedKey(Era era, int major, int minor, int patch) {
        String eraPart = era.name().toLowerCase(java.util.Locale.ROOT);

        // Beta 1.9's prereleases use the registry's existing pre_N convention (beta_1_9_pre_3), so a planned key
        // lines up with the id it will become.
        if (era == Era.BETA && major == 1 && minor == 9 && patch > 0) {
            return "historical_experience:planned_beta_1_9_pre_" + patch;
        }

        return "historical_experience:planned_" + eraPart + "_" + major + "_" + minor + "_" + patch;
    }

    /**
     * The order of an entry that the registry does not declare yet.
     *
     * <p>Uses the registry's own numbering scheme - {@code eraOffset + major*1000 + minor*100 + patch} - rather
     * than a formula of its own. The first version of this used {@code major*10_000} and a separate Infdev branch,
     * which produced orderings that agreed with the registry while the numbers did not (410102 against the
     * registry's 401102 for the same Alpha build), so the field claimed to be on the registry's scale and was not.
     *
     * <p>Declared versions do not come through here at all: {@link #chronologyOf} asks the registry, because the
     * registry hand-writes at least one chronology (the Alpha profile) that no formula covering its display name
     * would reproduce.
     */
    private static int order(Era era, int major, int minor, int patch) {
        return era.chronologyBase() + major * 1_000 + minor * 100 + patch;
    }

    /**
     * The chronology for a row: the registry's own value when the version is declared, otherwise the scheme.
     *
     * <p>This is what makes "an implemented entry's order is its registry chronology" true rather than merely
     * plausible, and it is asserted by a test.
     */
    private static int chronologyOf(String versionId, Era era, int major, int minor, int patch) {
        if (versionId != null) {
            HistoricalVersion declared = HistoricalVersions.byId(versionId).orElse(null);
            if (declared != null) {
                return declared.chronology();
            }
        }
        return order(era, major, minor, patch);
    }

    private static void rejectDuplicates(List<Entry> entries) {
        Map<String, Entry> seen = new LinkedHashMap<>();
        for (Entry entry : entries) {
            Entry previous = seen.put(entry.key(), entry);
            if (previous != null) {
                throw new IllegalStateException("two catalogue entries share the key " + entry.key()
                    + ": " + previous.targetName() + " and " + entry.targetName()
                    + ". A key identifies a version, and a world or a menu may refer to it, so it must be "
                    + "unique.");
            }
        }
    }
}
