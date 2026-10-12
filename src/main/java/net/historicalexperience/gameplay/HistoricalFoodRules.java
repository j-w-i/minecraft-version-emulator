package net.historicalexperience.gameplay;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Direct food healing for versions that had no hunger system.
 *
 * <h2>What these versions actually did</h2>
 *
 * <p>Beta 1.7.3 and earlier had no hunger: no food level, no saturation, no exhaustion, no starvation
 * and no natural regeneration. Food healed health <b>directly</b>, and you could always eat - there was
 * no "full" state to gate it on. This project already removes the hunger system by cancelling
 * {@code FoodData.tick}, which left a real gap: eating did nothing at all.
 *
 * <h2>The two halves, and why both are needed</h2>
 *
 * <p>Healing alone is not enough. Modern Minecraft refuses to let you eat unless you need food
 * ({@code Player.canEat(canAlwaysEat)} returns {@code invulnerable || canAlwaysEat || needsFood()}), so
 * a player at full food level - which is everyone, since the level never drains - could never eat and
 * therefore never heal. So {@link #allowsEatingWithoutHunger} un-gates eating and
 * {@link #healPointsFor} supplies the health it restored.
 *
 * <h2>Where the values come from</h2>
 *
 * <p>Every value was read as an integer literal from the hash-verified Beta 1.7.3 client
 * (SHA-1 {@code 43db9b498cb67058d2e12d394e6507722e71bb45}, 1,465,375 bytes, checked against Mojang's own
 * version manifest before use). In that client {@code yw} is the food class, its constructor's second
 * argument is health points, and {@code gs.c(int)} resolves to {@code ls.c(int)}, which adds to health
 * and clamps at 20. The evidence string on each entry names the class field and the initialiser it came
 * from. See {@code research/beta173-food-healing.json}.
 *
 * <h2>Known gaps, recorded rather than papered over</h2>
 *
 * <ul>
 *   <li><b>Cake is not modelled.</b> It healed 3 points per slice through a <em>block</em> interaction
 *       (right-clicking a placed cake, never consuming the held item), which is a different code path
 *       from eating. It is the only heal path outside the {@code yw} family in that version.</li>
 *   <li><b>The wolf feeding quirk is not modelled.</b> Feeding a wolf any meat in Beta healed it the
 *       hard-coded raw-porkchop value of 3 rather than the value of the item actually fed. That is a
 *       genuine bug in the original, and reproducing it is a decision, not an oversight.</li>
 *   <li><b>The hunger bar is still drawn.</b> Hiding it is a client-side presentation change and this
 *       project has no client package yet. The food level it displays is inert, because
 *       {@code FoodData.tick} is cancelled, but a player will still see a full bar.</li>
 * </ul>
 */
public final class HistoricalFoodRules {
    /**
     * One food item's direct heal amount.
     *
     * @param itemId     the modern item identifier
     * @param betaName   the item's name in the Beta client, for traceability
     * @param healPoints health points restored, in vanilla's 20-point scale
     * @param evidence   the class, field and initialiser the value was read from
     */
    public record FoodHeal(String itemId, String betaName, int healPoints, String evidence) {
        public FoodHeal {
            if (itemId == null || itemId.isBlank()) {
                throw new IllegalArgumentException("itemId must not be blank");
            }
            if (healPoints <= 0) {
                throw new IllegalArgumentException(
                    "healPoints must be positive for " + itemId + ", got " + healPoints);
            }
            if (evidence == null || evidence.isBlank()) {
                throw new IllegalArgumentException("evidence must not be blank for " + itemId);
            }
        }
    }

    /**
     * Beta 1.7.3's direct heals, in a fixed order so reports and tests are stable.
     *
     * <p>The golden apple's 42 deliberately exceeds the 20-point cap, so one always refills a full
     * health bar; that is what the client's literal says, not a transcription slip. The cookie's 1 point
     * is half a heart.
     */
    public static final List<FoodHeal> BETA_1_7_3 = List.of(
        new FoodHeal("minecraft:apple", "apple", 4, "gm.h = new yw(4,4,false); gm:13"),
        new FoodHeal("minecraft:mushroom_stew", "mushroomStew", 10,
            "gm.D = new bz(26,10); gm:35, bz delegates to super yw"),
        new FoodHeal("minecraft:bread", "bread", 5, "gm.S = new yw(41,5,false); gm:50"),
        new FoodHeal("minecraft:porkchop", "porkchopRaw", 3, "gm.ao = new yw(63,3,true); gm:72"),
        new FoodHeal("minecraft:cooked_porkchop", "porkchopCooked", 8,
            "gm.ap = new yw(64,8,true); gm:73"),
        new FoodHeal("minecraft:golden_apple", "appleGold", 42, "gm.ar = new yw(66,42,false); gm:75"),
        new FoodHeal("minecraft:cod", "fishRaw", 2, "gm.aS = new yw(93,2,false); gm:102"),
        new FoodHeal("minecraft:cooked_cod", "fishCooked", 5, "gm.aT = new yw(94,5,false); gm:103"),
        new FoodHeal("minecraft:cookie", "cookie", 1, "gm.ba = new qk(101,1,false,8); gm:110"),
        // Cake is recorded here for completeness and traceability, but the code below deliberately does
        // not apply it: it healed through a block interaction, not by eating.
        new FoodHeal("minecraft:cake", "cake", 3,
            "um.c(...):83 gs2.c(3), gated on gs.Y<20, 6 slices; uu.bh = new um(92,121), uu:116")
    );

    /** Beta 1.7.3's heals keyed by modern item id, excluding the block-interaction case. */
    public static final Map<String, FoodHeal> BETA_1_7_3_BY_ITEM = BETA_1_7_3.stream()
        .filter(heal -> !heal.itemId().equals("minecraft:cake"))
        .collect(java.util.stream.Collectors.toUnmodifiableMap(FoodHeal::itemId, heal -> heal));

    /** The heal points a version declares, keyed by item id, as the registry holds them. */
    public static final Map<String, Integer> BETA_1_7_3_HEAL_POINTS = BETA_1_7_3_BY_ITEM.values()
        .stream()
        .collect(java.util.stream.Collectors.toUnmodifiableMap(FoodHeal::itemId, FoodHeal::healPoints));

    /** Cake, recorded but not applied, so the omission is visible to a reader and to diagnostics. */
    public static final String UNMODELLED_BLOCK_FOOD = "minecraft:cake";

    /**
     * Alpha v1.1.2_01's direct heals: six foods, all verified from that client.
     *
     * <p>The health field is {@code ge.E} and the heal method {@code ge.b(int)} (class {@code ge} is the
     * registered "Mob"), byte-identical in shape to Beta 1.7.3's {@code ls.c(int)}: if health is not
     * positive, return; add; clamp at 20. The food class is {@code oj} with subclass {@code ap} for the
     * bowl-returning stew, and resolving every call through the real superclass chain found exactly two
     * callers jar-wide - the eat path {@code oj.a(...)} and Peaceful-only regeneration.
     *
     * <p><b>Each value has version-internal proof, not an analogy to Beta.</b> Alpha does not merely look
     * like Beta here: the golden apple's recipe in that jar (eight gold blocks around an apple) proves
     * which field is the apple and which is the golden apple; bread's recipe proves its field; the stew
     * recipe plus its bowl-returning subclass proves that pair; and the pig's drop method plus the
     * smelting table prove the two porkchops. Every one also matches Beta 1.7.3 on numeric id, field name
     * and icon index, which is corroboration rather than the basis.
     *
     * <p>Alpha had <b>no</b> cookie, fish or cake: its item registry holds ids 0-90 only, and every food
     * construction site in the jar is one of these six. Milk exists with no heal call, as in Beta.
     */
    public static final List<FoodHeal> ALPHA_1_1_2 = List.of(
        new FoodHeal("minecraft:apple", "apple", 4,
            "di.h, id 4, new oj(4,4,false); proved by being the centre of the golden-apple recipe"),
        new FoodHeal("minecraft:mushroom_stew", "mushroomStew", 10,
            "di.D, id 26, new ap(...); hr.a(Ldw;)V recipe bowl di.C + both mushrooms -> di.D"),
        new FoodHeal("minecraft:bread", "bread", 5,
            "di.S, id 41, new oj(41,5,false); dw.<init> recipe three di.R -> di.S"),
        new FoodHeal("minecraft:porkchop", "porkchopRaw", 3,
            "di.ao, id 63; Pig mv.g()I returns it as the drop"),
        new FoodHeal("minecraft:cooked_porkchop", "porkchopCooked", 8,
            "di.ap, id 64; ke.d(I)I smelts di.ao -> di.ap"),
        new FoodHeal("minecraft:golden_apple", "appleGold", 42,
            "di.ar, id 66; dw.<init> recipe eight ly.ai + centre di.h -> di.ar")
    );

    /** Alpha's heals keyed by modern item id. */
    public static final Map<String, Integer> ALPHA_1_1_2_HEAL_POINTS = ALPHA_1_1_2.stream()
        .collect(java.util.stream.Collectors.toUnmodifiableMap(FoodHeal::itemId, FoodHeal::healPoints));

    private static final LongAdder HEALS_APPLIED = new LongAdder();
    private static final LongAdder HEALTH_RESTORED = new LongAdder();
    private static final LongAdder EAT_GATE_OVERRIDES = new LongAdder();
    private static final Map<String, Integer> HEALS_BY_ITEM = new ConcurrentHashMap<>();
    private static final Set<String> HEALED_ITEMS = ConcurrentHashMap.newKeySet();

    private HistoricalFoodRules() {
    }

    /** True when a version has no hunger system but does declare direct heals. */
    public static boolean healsFoodDirectly(String versionId) {
        return !hasHunger(versionId) && !healPoints(versionId).isEmpty();
    }

    /**
     * True when eating must not be gated on needing food.
     *
     * <p>Driven by the absence of hunger rather than by the presence of heal values: a version with no
     * hunger cannot gate eating on a food level, whether or not its heal table has been researched yet.
     */
    public static boolean allowsEatingWithoutHunger(String versionId) {
        return !hasHunger(versionId);
    }

    /** The health a version's food restored directly, or empty when it is not a direct-healing food. */
    public static Optional<Integer> healPointsFor(String versionId, String itemId) {
        if (versionId == null || itemId == null) {
            return Optional.empty();
        }
        Integer points = healPoints(versionId).get(itemId);
        return Optional.ofNullable(points);
    }

    /** The full table a version declares, for diagnostics and tests. */
    public static Map<String, Integer> healPoints(String versionId) {
        return net.historicalexperience.version.HistoricalVersions.byId(versionId)
            .map(version -> version.rules().foodHealPoints())
            .orElse(Map.of());
    }

    /** Every version that declares direct food healing. */
    public static Set<String> healingProfiles() {
        return net.historicalexperience.version.HistoricalVersions.all().stream()
            .filter(version -> !version.rules().foodHealPoints().isEmpty())
            .map(net.historicalexperience.version.HistoricalVersion::id)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public static void recordHeal(String itemId, int points) {
        HEALS_APPLIED.increment();
        HEALTH_RESTORED.add(points);
        HEALED_ITEMS.add(itemId);
        HEALS_BY_ITEM.merge(itemId, 1, Integer::sum);
    }

    public static void recordEatGateOverride() {
        EAT_GATE_OVERRIDES.increment();
    }

    public static long healsApplied() {
        return HEALS_APPLIED.sum();
    }

    public static long healthRestored() {
        return HEALTH_RESTORED.sum();
    }

    public static long eatGateOverrides() {
        return EAT_GATE_OVERRIDES.sum();
    }

    /** What actually happened, with the version's declared table alongside it. */
    public static Map<String, String> diagnostics() {
        Map<String, String> report = new LinkedHashMap<>();
        for (var version : net.historicalexperience.version.HistoricalVersions.all()) {
            report.put(version.id(), "foodHealPoints=" + version.rules().foodHealPoints().size()
                + " hasHunger=" + version.rules().hasHunger());
        }
        report.put("healingProfiles", healingProfiles().toString());
        report.put("healsApplied", Long.toString(HEALS_APPLIED.sum()));
        report.put("healthRestored", Long.toString(HEALTH_RESTORED.sum()));
        report.put("eatGateOverrides", Long.toString(EAT_GATE_OVERRIDES.sum()));
        report.put("healedItems", new java.util.TreeSet<>(HEALED_ITEMS).toString());
        report.put("healsByItem", new java.util.TreeMap<>(HEALS_BY_ITEM).toString());
        report.put("notModelled", "block-interaction food: " + UNMODELLED_BLOCK_FOOD
            + " (3 points per slice via right-click, a different code path from eating)");
        return report;
    }

    /** True when a version has a hunger system, read from its own rule set. */
    public static boolean hasHunger(String versionId) {
        return net.historicalexperience.version.HistoricalVersions.byId(versionId)
            .map(version -> version.rules().hasHunger())
            .orElse(true);
    }

    /** Processes still holding an override count, so a test can assert without global state. */
}
