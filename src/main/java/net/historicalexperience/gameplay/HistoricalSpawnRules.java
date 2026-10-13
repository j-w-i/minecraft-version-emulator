package net.historicalexperience.gameplay;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import net.historicalexperience.profile.HistoricalProfiles;

/**
 * Which entity types a historical gameplay profile may spawn naturally.
 *
 * <p>This is a <b>gameplay</b> rule, not a generation-recipe rule, and it is keyed by the gameplay
 * profile id rather than by a generation revision. That distinction is deliberate: a spawn list does
 * not change any stored chunk, so applying it to every world of an era is correct, whereas a
 * generation-content change would have to be a new immutable revision. It also means the rule
 * applies to every revision of Beta 1.7.3, including already-generated worlds, without
 * reinterpreting what any world's recipe recorded.
 *
 * <h2>Why an allow list, and where it comes from</h2>
 *
 * <p>The set is the complete entity registry of the Beta 1.7.3 client, extracted from the
 * hash-verified jar's {@code jc.class} constant pool. An allow list is used rather than a deny list
 * because the historical set is fully enumerable and closed: anything not in it - including an
 * entity type that a future Minecraft version adds - is denied by construction, where a deny list
 * would quietly admit it.
 *
 * <p>What this establishes is <b>which entity types may spawn</b>, not the historical weights or
 * pack sizes; those remain unestablished. Non-mob entities (arrows, boats, minecarts, items,
 * falling sand, primed TNT, snowballs, paintings) are not natural spawns and are unaffected, and so
 * are spawn eggs, breeding, spawner blocks and mob conversion.
 */
public final class HistoricalSpawnRules {
    /**
     * The mobs registered by the Beta 1.7.3 client, as modern entity type identifiers.
     *
     * <p>Derived one-to-one from the {@code jc.class} constant pool; see
     * {@code research/beta173-entity-types.json}. {@code minecraft:giant} is included because it
     * existed in Beta, even though neither version gives it a natural spawn entry - including it is
     * inert and avoids inventing a restriction that the evidence does not support.
     */
    public static final Set<String> BETA_1_7_3_MOBS = Set.of(
        "minecraft:chicken",
        "minecraft:cow",
        "minecraft:creeper",
        "minecraft:ghast",
        "minecraft:giant",
        "minecraft:pig",
        "minecraft:sheep",
        "minecraft:skeleton",
        "minecraft:slime",
        "minecraft:spider",
        "minecraft:squid",
        "minecraft:wolf",
        "minecraft:zombie",
        "minecraft:zombie_pigman"
    );

    /**
     * Beta 1.8.1's natural spawns: the Beta 1.7.3 set plus the four the Adventure Update added.
     *
     * <p>Verified from the b1.8.1 client's entity registry (class {@code um}, SHA1
     * {@code 0db6b648...}, 28 names, 17 of them mobs): endermen, cave spiders and silverfish appear,
     * while bats, ocelots and every later mob still do not.
     *
     * <p><b>Villagers are deliberately NOT here.</b> A provisional version of this set included them on
     * the assumption that villages and villagers arrived together in Beta 1.8. The registry scan shows
     * no villager in b1.8.1 at all - it arrives in 1.0 - so the assumption was wrong and the entry was
     * removed rather than kept because it seemed likely. See {@code research/version-boundaries.json}.
     */
    public static final Set<String> BETA_1_8_1_MOBS = java.util.stream.Stream.concat(
            BETA_1_7_3_MOBS.stream(),
            java.util.stream.Stream.of(
                "minecraft:enderman",
                "minecraft:cave_spider",
                "minecraft:silverfish"))
        .collect(java.util.stream.Collectors.toUnmodifiableSet());

    /**
     * Alpha v1.1.2_01's natural spawns: ten mobs, verified from that client's own entity registry.
     *
     * <p>Read from class {@code ew} (SHA-1 of the client: {@code daa4b9f192d2c260837d3b98c39432324da28e86},
     * 897,164 bytes, checked against Mojang's own version metadata before use). The registry holds 20
     * registrations: these ten mobs, eight non-mob entities and the two base classes `Mob` and `Monster`.
     *
     * <p><b>Four of Beta 1.7.3's mobs are absent and were confirmed absent</b> as UTF-8 constants
     * anywhere in the jar, not merely missing from a list: ghasts, zombie pigmen, squid and wolves.
     *
     * <p>Giant is included for consistency with the Beta 1.7.3 set, which also registers it. It is
     * registered here and there alike and is not understood to have spawned naturally in either; the
     * registry is the convention this project uses, and the self-check that fixed that convention
     * reproduced Beta 1.7.3's 14 mobs exactly.
     */
    public static final Set<String> ALPHA_1_1_2_MOBS = Set.of(
        "minecraft:chicken",
        "minecraft:cow",
        "minecraft:creeper",
        "minecraft:giant",
        "minecraft:pig",
        "minecraft:sheep",
        "minecraft:skeleton",
        "minecraft:slime",
        "minecraft:spider",
        "minecraft:zombie"
    );

    /**
     * The mobs the Alpha line gained after 1.0.1, which the 1.0.1 set is derived by removing.
     *
     * <p>Per build, from the research:
     * <ul>
     *   <li><b>Cows arrive in Alpha v1.0.8</b> - {@code research/historical-features.json}, entry
     *       {@code beta18-cow-drops}, confidence high: "Cows added; they drop leather when killed". They were
     *       WRONGLY allowed in the 1.0.1 set until the owner asked whether the spawn sets had been checked, which
     *       they had not.</li>
     *   <li>Slimes arrive in Alpha 1.0.11, per the per-build deltas.</li>
     *   <li>Chickens arrive in Alpha 1.0.14, per the same deltas and the {@code beta18-chicken-drops} entry.</li>
     * </ul>
     *
     * <p>Cows arrive at 1.0.8, which falls between these declared builds and the next one, so removing them here is
     * correct for 1.0.1, 1.0.4 AND 1.0.6 - all three predate 1.0.8. The later Alpha profile keeps them, because it
     * is v1.1.2_01.
     */
    private static final Set<String> ALPHA_MOBS_ADDED_AFTER_1_0_1 = Set.of(
        "minecraft:cow",
        "minecraft:slime",
        "minecraft:chicken");

    /**
     * Alpha 1.0.1's natural spawn set: the 1.1.2 set without the two mobs added later.
     *
     * <p>DERIVED by subtraction rather than written out, so if the 1.1.2 set is ever corrected for a reason that
     * also applies here, this set follows. Written out, the two would drift and the earlier build would silently
     * spawn a mob that did not exist yet - which is the whole reason the Alpha versions cannot share a ruleset.
     */
    public static final Set<String> ALPHA_1_0_1_MOBS = ALPHA_1_1_2_MOBS.stream()
        .filter(mob -> !ALPHA_MOBS_ADDED_AFTER_1_0_1.contains(mob))
        .collect(java.util.stream.Collectors.toUnmodifiableSet());

    /**
     * Infdev 20100325's mobs, from the only place that build enumerates them.
     *
     * <p>That jar has <b>no entity-name registry at all</b> - a case-sensitive scan for every entity name
     * returns zero hits - so the set comes from the one place the build creates mobs: a
     * {@code random.nextInt(6)} switch that constructs zombie, creeper, skeleton, spider, pig and sheep.
     * Giant is present with a renderer but is never constructed.
     *
     * <p><b>Inferred rather than proven</b>, and the entry says so: the switch calls the world spawn
     * method and a sound, which fits a natural spawner - but it would also fit an effect entity, and
     * settling it needs a runtime observation rather than a static read.
     */
    /**
     * Beta 1.9 Prerelease 1: Beta 1.8.1's mobs plus the five that build adds.
     *
     * <p>From {@code research/version-transitions.json}, which lists Pre1 introducing "blaze, magma cube, mooshroom,
     * snow golem and villagers". Four of the five are natural spawns; <b>snow golems are built by a player</b> from
     * two snow blocks and a pumpkin, so they are not a spawn-set entry and are deliberately absent.
     *
     * <p>Villagers matter most here: villages arrive in Beta 1.8.1 but are uninhabited, because villagers arrive in
     * Pre1. Without this entry a Pre1 world would have no villagers and a Beta 1.8.1 world would wrongly have them.
     *
     * <p>Blaze and magma cubes are Nether mobs, and this is a single flat set rather than a per-dimension one, so
     * they are allowed everywhere the game would otherwise allow them. That is a known simplification, recorded in
     * research/implementation-coverage.md.
     *
     * <p>A MYCELIUM-derived cow variant is a separate entity type - {@code minecraft:mooshroom} - so it belongs in
     * the set, unlike the block-driven mooshroom question in Beta 1.8.1.
     */
    public static final Set<String> BETA_1_9_PRE_1_MOBS = java.util.stream.Stream.concat(
        BETA_1_8_1_MOBS.stream(),
        java.util.stream.Stream.of(
            "minecraft:blaze",
            "minecraft:magma_cube",
            "minecraft:mooshroom",
            "minecraft:villager"))
        .collect(java.util.stream.Collectors.toUnmodifiableSet());

    /**
     * Beta 1.9 Prerelease 2 adds no mob at all.
     *
     * <p>Its deltas are potions and status effects, hardcore mode, animal breeding, combining damaged tools to
     * repair, glass bottles and brewing ingredients, and nine music discs. Breeding changes how existing mobs are
     * made, not which exist, so the set is Pre1's - and sharing it is a statement about the research, not a
     * convenience.
     */
    public static final Set<String> BETA_1_9_PRE_2_MOBS = BETA_1_9_PRE_1_MOBS;

    /**
     * Alpha 1.0.11: the 1.1.2 set without chickens, because cows and slimes have both arrived by this build.
     *
     * <p>Derived by subtraction rather than listed, so the relationship to the other Alpha sets is visible:
     * <ul>
     *   <li><b>cows</b> arrive in Alpha v1.0.8 - {@code research/historical-features.json}, entry
     *       {@code beta18-cow-drops}, confidence high - so a build at 1.0.11 has them and 1.0.1, 1.0.4 and 1.0.6 do
     *       not.</li>
     *   <li><b>chickens</b> arrive in Alpha 1.0.14, so this build is the last declared Alpha that lacks them.</li>
     *   <li><b>slimes</b> are the uncertain one, and the uncertainty is named rather than hidden. The per-build
     *       deltas have NO 1.0.11 entry, and {@code research/feature-timeline.json} has no mobs section at all, so
     *       there is no structured statement that slimes arrive here. What there IS is the Alpha 1.0.14 delta
     *       saying slime natural spawning was "reduced and then disabled in this build (due to a reported
     *       spawn-limit miscalculation)" - which means slimes were spawning BEFORE 1.0.14. Including them at 1.0.11
     *       is inference from that sentence, not a citation. If they actually arrived at 1.0.14, this build wrongly
     *       has them; the alternative was omitting a mob the evidence says existed, which is the silent failure
     *       mode this class keeps warning about.</li>
     * </ul>
     */
    public static final Set<String> ALPHA_1_0_11_MOBS = ALPHA_1_1_2_MOBS.stream()
        .filter(mob -> !mob.equals("minecraft:chicken"))
        .collect(java.util.stream.Collectors.toUnmodifiableSet());

    public static final Set<String> INFDEV_20100325_MOBS = Set.of(
        "minecraft:zombie",
        "minecraft:creeper",
        "minecraft:skeleton",
        "minecraft:spider",
        "minecraft:pig",
        "minecraft:sheep"
    );

    /**
     * Infdev 20100420 and 20100611's mobs: the registry-backed set, seven species.
     *
     * <p>Both builds carry entity registry class {@code net/minecraft/a/c/e} with an identical
     * {@code <clinit>} instruction sequence and 13 registrations: the base classes Mob and Monster, seven
     * species (creeper, skeleton, spider, giant, zombie, pig, sheep) and four non-mob entities (arrow,
     * item, painting, primed TNT). There is no numeric entity id yet in that registry - the class/id form
     * Beta uses arrives later - so the set is by class and name.
     */
    public static final Set<String> INFDEV_20100420_AND_20100611_MOBS = Set.of(
        "minecraft:creeper",
        "minecraft:skeleton",
        "minecraft:spider",
        "minecraft:giant",
        "minecraft:zombie",
        "minecraft:pig",
        "minecraft:sheep"
    );

    /**
     * Infdev 20100227's mobs: <b>none</b>.
     *
     * <p>Not an extraction failure. That jar has no entity-name registry and no {@code (Class, String)}
     * registrar anywhere; a case-sensitive constant-pool scan for every entity name returns zero hits;
     * twelve entity-derived classes exist and the client even ships seven mob renderers, but a scan of
     * every {@code new} instruction in all 289 classes finds <b>no mob ever instantiated</b> - only
     * primed TNT, an item stack and an arrow.
     *
     * <p>So the correct rule is "nothing spawns naturally", which is why this is an EMPTY set that still
     * restricts rather than an empty set meaning "unrestricted".
     */
    public static final Set<String> INFDEV_20100227_MOBS = Set.of();

    /** One era's natural-spawn rule and the evidence behind it. */
    public record SpawnRule(String profileId, Set<String> allowedEntityTypes, String evidence) {
    }

    private static final LongAdder FILTER_INVOCATIONS = new LongAdder();
    private static final LongAdder REMOVED_ENTRIES = new LongAdder();
    private static final Set<String> REMOVED_ENTITY_TYPES = ConcurrentHashMap.newKeySet();

    private HistoricalSpawnRules() {
    }

    /** Every version that currently restricts natural spawning, for diagnostics and tests. */
    public static Set<String> restrictedProfiles() {
        return net.historicalexperience.version.HistoricalVersions.all().stream()
            .filter(version -> version.rules().restrictsSpawns())
            .map(net.historicalexperience.version.HistoricalVersion::id)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** The spawn rule a version declares, or empty when that version is unrestricted. */
    public static java.util.Optional<SpawnRule> forProfile(String profileId) {
        return net.historicalexperience.version.HistoricalVersions.byId(profileId)
            .filter(version -> version.rules().restrictsSpawns())
            .map(version -> new SpawnRule(
                version.id(), version.rules().allowedNaturalSpawns(), version.rules().evidence()));
    }

    /**
     * The gameplay profile a persisted generation-recipe alias belongs to, or {@code null} when the
     * alias is not one this build declares.
     *
     * <p>Resolved through the same binding table the lifecycle uses, so a world cannot be treated as
     * historical by this rule while being rejected as historical everywhere else.
     */
    public static String profileIdForAlias(String rawAlias) {
        return HistoricalProfiles.byGenerationPreset(rawAlias)
            .map(binding -> binding.profile().id().value())
            .orElse(null);
    }

    /** True when this entity type may spawn naturally under the given rule. */
    public static boolean isAllowed(SpawnRule rule, String entityTypeId) {
        return rule == null || rule.allowedEntityTypes().contains(entityTypeId);
    }

    /** Records that a spawn-list filter ran. */
    public static void recordFilterInvocation() {
        FILTER_INVOCATIONS.increment();
    }

    /** Records that one candidate spawn entry was removed, and which entity type it named. */
    public static void recordRemoval(String entityTypeId) {
        REMOVED_ENTRIES.increment();
        REMOVED_ENTITY_TYPES.add(entityTypeId);
    }

    /** How many spawn-list filters have run since process start. */
    public static long filterInvocations() {
        return FILTER_INVOCATIONS.sum();
    }

    /** How many candidate spawn entries have been removed since process start. */
    public static long removedEntries() {
        return REMOVED_ENTRIES.sum();
    }

    /** The distinct entity types removed so far, for verdict reporting. */
    public static Set<String> removedEntityTypes() {
        return Set.copyOf(REMOVED_ENTITY_TYPES);
    }

    /** Diagnostics as an ordered map, for verdict reporting. */
    public static Map<String, String> diagnostics() {
        Map<String, String> report = new LinkedHashMap<>();
        report.put("restrictedProfiles", restrictedProfiles().toString());
        report.put("allowedEntityTypes", Integer.toString(BETA_1_7_3_MOBS.size()));
        report.put("filterInvocations", Long.toString(FILTER_INVOCATIONS.sum()));
        report.put("removedEntries", Long.toString(REMOVED_ENTRIES.sum()));
        report.put("removedEntityTypes", REMOVED_ENTITY_TYPES.toString());
        return report;
    }

    /** The ordered allow list, so tests and reports do not depend on set iteration order. */
    public static List<String> beta173MobsInOrder() {
        return BETA_1_7_3_MOBS.stream().sorted().toList();
    }
}
