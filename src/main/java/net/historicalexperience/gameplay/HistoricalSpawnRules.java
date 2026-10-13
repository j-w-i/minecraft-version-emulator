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
        "minecraft:zombified_piglin"
        // NOT "zombie_pigman": that is the historical NAME, from the wiki and the per-build deltas, and it is not a
        // valid 1.21.1 entity id - the class is ZombifiedPiglin. It mattered more than a cosmetic mismatch, because
        // the spawn filter REMOVES any entity type absent from the allowed set. An id that never matches does not
        // merely fail to allow pigmen; it actively removes them from every Beta world. Found by a test asserting
        // that zombified_piglin predates Beta 1.0, which failed because the set did not contain it.
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

    /**
     * Alpha 1.0.14: the 1.1.2 set WITHOUT slimes, because this build disabled their natural spawning.
     *
     * <p>Derived by subtraction for the same reason as its neighbour: the relationship is the fact. Chickens arrive
     * here - {@code research/version-transitions.json}, Alpha v1.0.14, "chickens and eggs" - so this build is the
     * first declared Alpha with them, and the 1.0.11 set is this one minus the chicken.
     *
     * <p><b>Slimes are ABSENT here, and that is a bug rather than a design.</b> The same delta reads "slime natural
     * spawning (reduced and then disabled in this build due to a reported spawn-limit miscalculation; not a
     * permanent removal)" and its own incomplete list adds "slime spawns disabled due to a bug; do not infer
     * permanent removal". So this set removes them, and the NEXT declared Alpha build must not inherit that
     * removal - which is why the two sets are separate constants rather than one filtered at runtime.
     */
    public static final Set<String> ALPHA_1_0_14_MOBS = ALPHA_1_1_2_MOBS.stream()
        .filter(mob -> !mob.equals("minecraft:slime"))
        .collect(java.util.stream.Collectors.toUnmodifiableSet());

    /**
     * Alpha 1.2.0: ghasts and zombie pigmen arrive, slimes are removed from the spawning code.
     *
     * <p>This is the first Alpha set that differs from its predecessor by an ADDITION and a REMOVAL at once, and
     * unlike 1.0.14's removal, this one is not described as a bug: the delta reads "slime natural spawning (removed
     * from mob spawning code)". Its incomplete list does not walk that back, so the removal is taken at face value
     * here.
     *
     * <p>That raises a question this class cannot answer from the deltas: slimes are PRESENT in the Beta 1.7.3 set,
     * so they returned somewhere between Alpha 1.2.0 and Beta 1.7.3 - in a build that is not a declared target and
     * has no delta entry here. The absence is asserted for 1.2.0 and the return is left to the Beta profile, with
     * the gap recorded rather than guessed at.
     */
    public static final Set<String> ALPHA_1_2_0_MOBS = java.util.stream.Stream.concat(
        ALPHA_1_1_2_MOBS.stream().filter(mob -> !mob.equals("minecraft:slime")),
        java.util.stream.Stream.of("minecraft:ghast", "minecraft:zombified_piglin"))
        .collect(java.util.stream.Collectors.toUnmodifiableSet());

    /**
     * Beta 1.0: the Beta 1.7.3 set without squid and without wolves.
     *
     * <p>Derived by subtraction, because both removals are dated by the research and the relationship is the fact:
     * <ul>
     *   <li><b>squid arrive in Beta 1.2</b> - {@code research/version-transitions.json}, Beta 1.2: "squid (10 health,
     *       drops ink sac, no sounds, could be milked in this build)". So the 1.7.3 set has them and Beta 1.0 does
     *       not, and my earlier assumption that squid were ancient was wrong.</li>
     *   <li><b>wolves arrive in Beta 1.4</b> - the same file, Beta 1.4: "wolves (neutral, common in taiga, 10 health,
     *       tame via bones at 1/3 chance per bone)".</li>
     * </ul>
     *
     * <p>No mob is added anywhere in Beta 1.0, 1.3, 1.5 or 1.6, so this set is shared with 1.3 and Beta 1.2's is
     * this one plus the squid.
     */
    public static final Set<String> BETA_1_0_MOBS = BETA_1_7_3_MOBS.stream()
        .filter(mob -> !mob.equals("minecraft:squid") && !mob.equals("minecraft:wolf"))
        .collect(java.util.stream.Collectors.toUnmodifiableSet());

    /**
     * Every entity id in every declared spawn set that the ENTITY REGISTRY does not recognise.
     *
     * <p>This exists because an invalid id here is DESTRUCTIVE, not merely inert. The spawn filter removes every
     * entity type absent from the allowed set, so an id that never matches does not fail to allow a mob - it
     * actively removes that mob. {@code BETA_1_7_3_MOBS} listed {@code minecraft:zombie_pigman}, the historical
     * name from the wiki and the deltas, while the 1.21.1 id is {@code minecraft:zombified_piglin}; zombie pigmen
     * were being deleted from every Beta world because of a spelling.
     *
     * <p>It belongs here and is called by the generation probe, not by a unit test, for the same concrete reason the
     * village style check does: resolving an id needs a bootstrapped registry, and this project has no test
     * bootstrap. A unit test that tried died with {@code NoClassDefFoundError}.
     *
     * <p>The earlier hand-written test for this set asserted equality with a list that contained the SAME wrong id,
     * because it was written from the same source as the code. Checking against the registry instead of against a
     * transcription is the whole point.
     *
     * @return version id to the sorted unknown ids it allows, empty when every set is clean
     */
    public static java.util.Map<String, java.util.List<String>> unknownEntityIds() {
        java.util.Map<String, java.util.List<String>> unknown = new java.util.LinkedHashMap<>();
        for (net.historicalexperience.version.HistoricalVersion version
                : net.historicalexperience.version.HistoricalVersions.all()) {
            if (!version.rules().restrictsSpawns()) {
                // An unrestricted version makes no claim, so there is nothing to validate.
                continue;
            }
            java.util.List<String> bad = version.rules().allowedNaturalSpawns().stream()
                .filter(id -> {
                    try {
                        return !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                            .containsKey(net.minecraft.resources.ResourceLocation.parse(id));
                    } catch (RuntimeException malformed) {
                        return true;
                    }
                })
                .sorted()
                .toList();
            if (!bad.isEmpty()) {
                unknown.put(version.id(), bad);
            }
        }
        return unknown;
    }

    /**
     * Beta 1.2: the Beta 1.7.3 set without wolves AND without slimes.
     *
     * <p>Slimes were removed from the spawning code in Alpha 1.2.0 and do not return until Beta 1.3, where the
     * delta reads "slimes spawn again". My first version of this set derived it from Beta 1.7.3 by removing only
     * the wolf, which left slimes in - and reading Beta 1.3's deltas is what caught it. So this is Beta 1.0's set
     * PLUS the squid, and Beta 1.3 is this plus the slime.
     */
    public static final Set<String> BETA_1_2_MOBS = BETA_1_7_3_MOBS.stream()
        .filter(mob -> !mob.equals("minecraft:wolf") && !mob.equals("minecraft:slime"))
        .collect(java.util.stream.Collectors.toUnmodifiableSet());

    /** Beta 1.3: the same set with slimes back, because "slimes spawn again" is this build's own delta. */
    public static final Set<String> BETA_1_3_MOBS = BETA_1_7_3_MOBS.stream()
        .filter(mob -> !mob.equals("minecraft:wolf"))
        .collect(java.util.stream.Collectors.toUnmodifiableSet());

    /**
     * Beta 1.4: the wolf arrives, which makes this set identical to Beta 1.7.3's.
     *
     * <p>Read from {@code research/version-transitions.json}, {@code beta_1_4}, introduced: <i>"wolves (neutral,
     * common in taiga, 10 health, tame via bones at 1/3 chance per bone)"</i>, with the same entry recording that
     * wolves <i>"drop nothing when killed"</i> - a drop rule rather than a spawn rule.
     *
     * <p>It is written as the Beta 1.7.3 set rather than as a new list because that IS the claim: the wolf was the
     * last mob added before the Adventure Update. Beta 1.5, 1.6 and 1.7 add none (Beta 1.5 is weather and rails,
     * Beta 1.6 is maps, trapdoors and vegetation, Beta 1.7 is bug fixes), and the Adventure Update's enderman,
     * cave spider and silverfish arrive in Beta 1.8.1. Deriving it makes a future correction to the 1.7.3 set
     * propagate here instead of silently diverging.
     */
    public static final Set<String> BETA_1_4_MOBS = BETA_1_7_3_MOBS;

    /**
     * Beta 1.5: no mob is added, so the set is Beta 1.4's.
     *
     * <p>Its deltas are weather, birch and spruce saplings, detector and powered rails, cobweb and sixteen
     * achievements. None of them is an entity, so writing a new set here would be inventing a claim. The
     * distinction matters because the spawn filter REMOVES anything absent from the allow set: a set that
     * accidentally dropped the wolf would delete wolves from every Beta 1.5 world, which is the destructive
     * failure {@code SpawnRestrictionSemanticsTest} exists to prevent.
     */
    public static final Set<String> BETA_1_5_MOBS = BETA_1_4_MOBS;

    /**
     * Beta 1.6: no mob is added either, so the set is Beta 1.5's.
     *
     * <p>Beta 1.6's own additions are the map item, the wooden trapdoor, dead bush, short grass and fern, Nether
     * support in multiplayer, and the removal of the ten-tick missed-swing cooldown. The last is a combat rule and
     * the rest are items or blocks, so no entity is added - and the mob set is therefore the same one.
     */
    public static final Set<String> BETA_1_6_MOBS = BETA_1_5_MOBS;

    /**
     * The Release-era sets, each derived from the one before it by the mobs the research names for that build.
     *
     * <p>Until now every Release profile was declared through {@code terrainOnly}, which sets NO spawn restriction,
     * so Release worlds spawned modern 1.21.1 mobs - piglins, hoglins, striders and the rest. That was the largest
     * known content defect in this project, and it is the mob side of the owner's Nether report.
     *
     * <p>Each addition is cited, because a set that is not researched must not be invented:
     * <ul>
     *   <li>Release 1.0 adds no mob - its deltas are the sound update - so it is the Beta 1.9 Pre set, which is
     *       correct by construction: Release 1.0.0 IS the released form of the Beta 1.9 prerelease.</li>
     *   <li>Release 1.1 adds none either.</li>
     *   <li>Release 1.2 adds {@code ocelots/cats and iron golems}. Only the OCELOT is included: cats are a separate
     *       entity in 1.21.1 and did not spawn naturally until 1.14.</li>
     *   <li>Release 1.3 adds none.</li>
     *   <li>Release 1.4 adds {@code witch, bat, wither skeleton, zombie villager}. The Wither is not included
     *       because it is SUMMONED rather than a natural spawn.</li>
     *   <li>Release 1.5 adds none.</li>
     *   <li>Release 1.6 adds horses and variants, donkeys and mules, and says horses "spawn naturally in
     *       plains/savanna". Horse and donkey are included; the mule is bred rather than spawned.</li>
     * </ul>
     */
    public static final Set<String> RELEASE_1_0_MOBS = BETA_1_9_PRE_2_MOBS;

    public static final Set<String> RELEASE_1_2_MOBS = java.util.stream.Stream.concat(
        RELEASE_1_0_MOBS.stream(),
        java.util.stream.Stream.of("minecraft:ocelot", "minecraft:iron_golem"))
        .collect(java.util.stream.Collectors.toUnmodifiableSet());

    public static final Set<String> RELEASE_1_4_MOBS = java.util.stream.Stream.concat(
        RELEASE_1_2_MOBS.stream(),
        java.util.stream.Stream.of("minecraft:witch", "minecraft:bat",
            "minecraft:wither_skeleton", "minecraft:zombie_villager"))
        .collect(java.util.stream.Collectors.toUnmodifiableSet());

    public static final Set<String> RELEASE_1_6_MOBS = java.util.stream.Stream.concat(
        RELEASE_1_4_MOBS.stream(),
        java.util.stream.Stream.of("minecraft:horse", "minecraft:donkey"))
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

    /**
     * The first version whose <b>Nether</b> spawns endermen naturally: the 1.10 snapshot 16w20a.
     *
     * <p>This is a dimension boundary rather than a mob boundary, and the two are genuinely different. The
     * enderman arrived in Beta 1.8.1 and is Overworld content from then on - which is why it is in
     * {@link #BETA_1_8_1_MOBS} and every set derived from it. Its <b>Nether</b> spawning began much later:
     * 16w20a's change list records "Enderman can spawn in the Nether" (see
     * {@code https://minewiki.pl/16w20a#1}).
     *
     * <p><b>602000 is Release 1.10.0 on this project's own chronology scheme</b>, which is
     * {@code era + major * 1000 + minor * 100 + patch}: 600000 + 1 * 1000 + 10 * 100 + 0. It is written as
     * that arithmetic rather than as a bare literal because a first attempt at this constant used the wrong
     * scale (1010000), which is below no declared version and above the whole scheme - a value that happens
     * to leave every declared version on the correct side of the comparison while meaning nothing.
     * {@code NetherSpawnFilteringTest} pins the arithmetic rather than the literal, so the number cannot
     * drift from the scheme again.
     *
     * <p>Measured consequence, reported by the owner's runtime probe: a Release 1.0.0 world's Nether spawned
     * endermen, because 1.21.1's {@code nether_wastes} lists one and the flat allow set permits it. Every
     * version this project declares is older than 1.10, so for the declared set the rule reads "endermen never
     * spawn in the Nether" - but it is written as a dated boundary rather than as a flat veto so that a later
     * version inherits the correct answer instead of a wrong one.
     *
     * <p>SOURCE AND CONFIDENCE are weaker than the sets above, and that is recorded rather than glossed: the
     * mob sets are extracted from hash-verified client jars by this project, while this boundary rests on
     * 16w20a's published change list - no declared build is near it, so no jar here can be scanned for it. It
     * affects only the Nether, and Overworld endermen spawning is untouched by construction.
     */
    public static final int NETHER_ENDERMAN_CHRONOLOGY = 602_000;

    /**
     * True when endermen may spawn naturally <b>in the Nether</b> for this gameplay profile.
     *
     * <p>False for every version older than 16w20a, which is every declared version. A profile this build
     * does not declare answers false as well: an unknown world must not acquire a Nether mob it never had.
     */
    public static boolean netherEndermenAllowed(String profileId) {
        return net.historicalexperience.version.HistoricalVersions.byId(profileId)
            .map(version -> version.chronology() >= NETHER_ENDERMAN_CHRONOLOGY)
            .orElse(false);
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
