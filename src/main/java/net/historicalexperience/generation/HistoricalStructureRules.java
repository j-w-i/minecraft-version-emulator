package net.historicalexperience.generation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import net.historicalexperience.version.HistoricalVersions;

/**
 * Which structures a version could have generated, with the boundary evidence for each.
 *
 * <h2>Why per structure rather than one flag</h2>
 *
 * <p>The registry already carried a single {@code hasStructures} boolean, which is enough to say "this
 * version had none" but cannot say "villages and mineshafts arrived in Beta 1.8.1 while ocean monuments
 * did not arrive until Release 1.8". A boolean that coarse either lets later structures into an early
 * world or keeps early structures out of a world that should have them, and either way the mistake is
 * invisible.
 *
 * <h2>Every boundary here is sourced</h2>
 *
 * <p>The introduction dates come from {@code research/feature-timeline.json}'s {@code structures} rows,
 * each of which carries the Minecraft Wiki's own History line as a quote. The dates, oldest first:
 *
 * <table>
 *   <caption>Structure introductions in the range this project declares</caption>
 *   <tr><th>Structure</th><th>Introduced</th><th>First declared profile that has it</th></tr>
 *   <tr><td>monster room (dungeon)</td><td>Infdev 20100625-1917</td><td>Alpha v1.1.2_01</td></tr>
 *   <tr><td>mineshaft</td><td>Beta 1.8 Pre-release</td><td>Beta 1.8.1</td></tr>
 *   <tr><td>village</td><td>Beta 1.8 Pre-release</td><td>Beta 1.8.1</td></tr>
 *   <tr><td>stronghold</td><td>Beta 1.8 Pre-release</td><td>Beta 1.8.1</td></tr>
 *   <tr><td>nether fortress</td><td>Beta 1.9 Prerelease</td><td>Beta 1.9 Prerelease 3</td></tr>
 *   <tr><td>desert well</td><td>Release 1.2.1 (12w04a)</td><td>Release 1.2.5 - and it is a PLACED
 *       FEATURE, not a structure, so it is excluded through the generation-revision feature list</td></tr>
 *   <tr><td>desert pyramid</td><td>Release 1.3.1 (12w21a)</td><td>Release 1.6.4</td></tr>
 *   <tr><td>jungle pyramid</td><td>Release 1.3.1 (12w22a)</td><td>Release 1.6.4</td></tr>
 *   <tr><td>witch hut</td><td>Release 1.4.2 (12w40a)</td><td>Release 1.6.4</td></tr>
 *   <tr><td>ocean monument</td><td>Release 1.8 (14w25a)</td><td>none - outside every declared profile</td></tr>
 * </table>
 *
 * <p>Two entries are deliberately absent from the allowed sets even though their introduction date would
 * otherwise admit them:
 *
 * <ul>
 *   <li><b>{@code minecraft:mineshaft_mesa}</b> is a mineshaft in a badlands (mesa) biome, and the mesa biome
 *       is Release 1.7.2 - later than the newest profile declared here, Release 1.6.4. Allowing it would
 *       place a structure in a biome that cannot exist, so it is suppressed for every profile.</li>
 *   <li><b>dungeons are not structures on 1.21.1.</b> {@code minecraft:monster_room} lives under
 *       {@code worldgen/placed_feature} and not under {@code worldgen/structure} (verified against the
 *       pinned 1.21.1 jar), so this rule cannot and must not be read as controlling them. Their boundary is
 *       enforced through the generation-revision feature exclusions instead, where Infdev 20100420 and
 *       Infdev 20100611 - both older than Infdev 20100625-1917 - exclude them.</li>
 * </ul>
 *
 * <h2>An allow-list, and what that means</h2>
 *
 * <p>A profile that declares a rule suppresses <b>every</b> structure not named in its allowed set, not
 * merely a deny-list of known-later ones. That is the fail-safe direction: a structure added by a later
 * Minecraft release, by another mod or by a datapack is suppressed in a historical world instead of
 * silently appearing in it. The cost is that a pack which intentionally adds a structure to a historical
 * world will not see it, which is the intended behaviour rather than an accident.
 *
 * <p>Profiles with no rule are left completely alone - which is now no profile, since every declared
 * profile has a sourced boundary. The unit test enumerates the pinned jar's own structure registry and
 * fails if any structure is neither allowed nor denied for a profile, so a new vanilla structure cannot
 * slip through unclassified.
 */
public final class HistoricalStructureRules {
    /** A version's structure rule. An empty allowed set that restricts means "no structures at all". */
    public record StructureRule(boolean restricts, Set<String> allowedStructureIds, String evidence) {
        public StructureRule {
            allowedStructureIds = Set.copyOf(allowedStructureIds);
            if (!restricts && !allowedStructureIds.isEmpty()) {
                throw new IllegalArgumentException(
                    "a rule that does not restrict must not name allowed structures");
            }
            if (evidence == null || evidence.isBlank()) {
                throw new IllegalArgumentException("a structure rule must state its evidence");
            }
        }

        /** Whether this rule permits a structure. A non-restricting rule permits everything. */
        public boolean allows(String structureId) {
            return !restricts || allowedStructureIds.contains(structureId);
        }
    }

    /** Every structure the pinned 1.21.1 jar registers, so an unclassified one is a visible failure. */
    public static final Set<String> VANILLA_STRUCTURE_IDS = Set.of(
        "minecraft:ancient_city", "minecraft:bastion_remnant", "minecraft:buried_treasure",
        "minecraft:desert_pyramid", "minecraft:end_city", "minecraft:fortress", "minecraft:igloo",
        "minecraft:jungle_pyramid", "minecraft:mansion", "minecraft:mineshaft", "minecraft:mineshaft_mesa",
        "minecraft:monument", "minecraft:nether_fossil", "minecraft:ocean_ruin_cold",
        "minecraft:ocean_ruin_warm", "minecraft:pillager_outpost", "minecraft:ruined_portal",
        "minecraft:ruined_portal_desert", "minecraft:ruined_portal_jungle",
        "minecraft:ruined_portal_mountain", "minecraft:ruined_portal_nether",
        "minecraft:ruined_portal_ocean", "minecraft:ruined_portal_swamp", "minecraft:shipwreck",
        "minecraft:shipwreck_beached", "minecraft:stronghold", "minecraft:swamp_hut",
        "minecraft:trail_ruins", "minecraft:trial_chambers", "minecraft:village_desert",
        "minecraft:village_plains", "minecraft:village_savanna", "minecraft:village_snowy",
        "minecraft:village_taiga");

    /**
     * The village variants a profile may generate.
     *
     * <p>Beta 1.8 villages "only generated in plains and desert biomes" (feature-timeline structures row
     * "village"). The savanna, taiga and snowy variants come later than the newest profile declared here,
     * so no profile this project declares may generate them. The historical village structure, ported from
     * the pinned {@code old_villages} reference, places them under the same biome rule instead.
     *
     * <p><b>These ids are deliberately NOT in any allowed set.</b> The version's village is
     * {@link #HISTORICAL_VILLAGE}; the modern jigsaw villages are suppressed by the vanilla-village rule in
     * {@code HistoricalVillageRules}, which {@link #suppresses} consults before the allow-list. Leaving them
     * allowed is what produced the owner's report: a Beta 1.8.1 world whose villages "looked the same" as a
     * current one, because modern villages were still generating beside ours.
     */
    private static final Set<String> VILLAGES_PLAINS_AND_DESERT =
        Set.of("minecraft:village_plains", "minecraft:village_desert");

    /** This project's own village. The version's villages are this structure, not the jigsaw ones. */
    public static final String HISTORICAL_VILLAGE = "historical_experience:historical_village";

    private static final Set<String> BETA_1_8_STRUCTURES =
        Set.of("minecraft:mineshaft", "minecraft:stronghold", HISTORICAL_VILLAGE);

    private static final Set<String> RELEASE_1_6_4_STRUCTURES = union(BETA_1_8_STRUCTURES,
        Set.of("minecraft:fortress", "minecraft:desert_pyramid", "minecraft:jungle_pyramid",
            "minecraft:swamp_hut"));

    private static final String NO_STRUCTURES_EVIDENCE_SUFFIX =
        " placed no structure-set structure. Mineshafts, villages and strongholds all arrive in the Beta "
            + "1.8 Pre-release and the nether fortress in Beta 1.9 Prerelease, so the allowed set is empty. "
            + "This is NOT the same as saying the version had no underground rooms at all: monster rooms "
            + "(dungeons) go back to Infdev 20100625-1917 and are a PLACED FEATURE on 1.21.1 rather than a "
            + "structure, so this rule neither touches them nor may be read as though it did. Their own "
            + "boundary is enforced by the generation-revision feature exclusions, which exclude "
            + "minecraft:monster_room and minecraft:monster_room_deep for the two Infdev profiles that "
            + "predate 20100625-1917.";

    private static final Map<String, StructureRule> RULES = Map.ofEntries(
        Map.entry("historical_experience:infdev_420",
            noStructures("Infdev 20100420", "It also predates the monster room by two months")),
        Map.entry("historical_experience:infdev_611",
            noStructures("Infdev 20100611",
                "It predates the 20100625-1917 monster room, so its dungeon feature is excluded too")),
        Map.entry("historical_experience:alpha_1_0_4", noStructures("Alpha v1.0.4",
            "It has monster rooms, which are features, but no structure-system structure")),
        Map.entry("historical_experience:alpha_1_0_1", noStructures("Alpha v1.0.1",
            "It has monster rooms, which are features, but no structure-system structure")),
        Map.entry("historical_experience:alpha", noStructures("Alpha v1.1.2_01",
            "It has monster rooms, which are features, but no structure-system structure")),
        Map.entry("historical_experience:beta_1_7_3", noStructures("Beta 1.7.3",
            "It has monster rooms, which are features, but no structure-system structure")),
        Map.entry("historical_experience:beta_1_8_1",
            allowed("Beta 1.8.1", BETA_1_8_STRUCTURES,
                "feature-timeline structures: mineshaft, village and stronghold all introduced in the Beta "
                    + "1.8 Pre-release. Villages only generated in plains and desert biomes, so the "
                    + "savanna, taiga and snowy village variants are NOT allowed even though the structure "
                    + "system now exists. Nether fortresses are Beta 1.9 Prerelease content and are not yet "
                    + "allowed, and no other vanilla structure existed.")),
        Map.entry("historical_experience:beta_1_9_pre_3",
            allowed("Beta 1.9 Prerelease 3", union(BETA_1_8_STRUCTURES, Set.of("minecraft:fortress")),
                "Adds the nether fortress, introduced in the Beta 1.9 Prerelease (feature-timeline "
                    + "structures row \"nether fortress\", quote |{{HistoryLine||1.0.0|dev=Beta 1.9 "
                    + "Prerelease|Added Nether fortresses.). Everything else that exists is the Beta 1.8 "
                    + "set.")),
        Map.entry("historical_experience:release_1_0_0",
            allowed("Release 1.0.0", union(BETA_1_8_STRUCTURES, Set.of("minecraft:fortress")),
                "Release 1.0.0 is the released form of Beta 1.9 Prerelease, so it has the same structure "
                    + "set. The desert well (Release 1.2.1) is a placed feature and is excluded through the "
                    + "generation revision instead.")),
        Map.entry("historical_experience:release_1_1",
            allowed("Release 1.1", union(BETA_1_8_STRUCTURES, Set.of("minecraft:fortress")),
                "Release 1.1 added no structure; its set is Release 1.0.0's.")),
        Map.entry("historical_experience:release_1_2_5",
            allowed("Release 1.2.5", union(BETA_1_8_STRUCTURES, Set.of("minecraft:fortress")),
                "Release 1.2.5 adds the desert well, which is introduced in Release 1.2.1 (12w04a) and is "
                    + "a PLACED FEATURE rather than a structure, so it is granted by removing it from this "
                    + "profile's feature exclusions rather than by listing it here. No structure was added "
                    + "in Release 1.2.")),
        Map.entry("historical_experience:release_1_6_4",
            allowed("Release 1.6.4", RELEASE_1_6_4_STRUCTURES,
                "Adds the desert pyramid and jungle pyramid (both Release 1.3.1) and the witch hut (Release "
                    + "1.4.2). The ocean monument is Release 1.8 and is outside every declared profile. "
                    + "KNOWN CONTENT GAP: these three are generated by the modern structure code, whose "
                    + "build materials are not the Release 1.6.4 ones - the modern desert pyramid uses "
                    + "sandstone variants introduced in Release 1.13, for example. The structure-level "
                    + "boundary is correct; the block-level fidelity of these three is recorded as an open "
                    + "gap rather than claimed.")));

    private static StructureRule noStructures(String displayName, String extra) {
        return new StructureRule(true, Set.of(), displayName + extra + NO_STRUCTURES_EVIDENCE_SUFFIX);
    }

    private static StructureRule allowed(String displayName, Set<String> allowed, String evidence) {
        return new StructureRule(true, allowed, displayName + ": " + evidence);
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        Set<String> result = new java.util.LinkedHashSet<>(first);
        result.addAll(second);
        return Set.copyOf(result);
    }

    private static final LongAdder SUPPRESSED = new LongAdder();
    private static final Set<String> SUPPRESSED_IDS = ConcurrentHashMap.newKeySet();

    private HistoricalStructureRules() {
    }

    /** The rule for a version, or empty when no boundary has been established for it. */
    public static Optional<StructureRule> ruleForVersion(String versionId) {
        return Optional.ofNullable(versionId == null ? null : RULES.get(versionId));
    }

    /**
     * Whether a structure must not be generated in the world the given alias belongs to.
     *
     * <p>Resolves the alias to its owning version, because the alias names a generation recipe and the
     * structure boundary is a property of the version.
     */
    public static boolean suppresses(String alias, String structureId) {
        if (alias == null || structureId == null) {
            return false;
        }
        String versionId = HistoricalVersions.byRevisionAlias(alias)
            .map(version -> version.id())
            .orElse(alias);
        // The historical village REPLACES the modern jigsaw villages, so this runs before the allow-list: a
        // version at or after Beta 1.8.1 has villages, and they are ours. Without this the allow-list's
        // village entries let the modern ones through beside ours, which is exactly what the owner saw - a
        // Beta 1.8.1 world whose villages "looked the same" as a current one.
        if (net.historicalexperience.worldgen.HistoricalVillageRules
                .vanillaVillageStructures().containsKey(structureId)
                && net.historicalexperience.worldgen.HistoricalVillageRules
                    .suppressVanillaVillagesForAlias(alias)) {
            SUPPRESSED.increment();
            SUPPRESSED_IDS.add(structureId);
            return true;
        }
        StructureRule rule = RULES.get(versionId);
        if (rule == null || rule.allows(structureId)) {
            return false;
        }
        SUPPRESSED.increment();
        SUPPRESSED_IDS.add(structureId);
        return true;
    }

    public static long suppressedCount() {
        return SUPPRESSED.sum();
    }

    public static Set<String> suppressedStructureIds() {
        return java.util.Collections.unmodifiableSet(SUPPRESSED_IDS);
    }

    /** Every version that declares a structure rule, for reporting. */
    public static List<String> declaredVersions() {
        return RULES.keySet().stream().sorted().toList();
    }

    /** The declared rule for a profile id, for tests. */
    public static Set<String> allowedForVersion(String versionId) {
        StructureRule rule = RULES.get(versionId);
        return rule == null ? Set.of() : rule.allowedStructureIds();
    }
}
