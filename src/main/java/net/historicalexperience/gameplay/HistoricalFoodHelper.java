package net.historicalexperience.gameplay;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * PORTED from Nostalgic Tweaks {@code helper/gameplay/FoodHelper.java} (LGPL-3.0; see THIRD_PARTY_NOTICES.md).
 *
 * <p>Upstream's test is {@code isEdible(itemStack) && GameplayTweak.INSTANT_EAT && !IGNORED_EDIBLES}.
 *
 * <p>The version condition replaces upstream's global flag, and deliberately reuses the hunger rule rather than
 * introducing a second boundary: the eating delay and hunger arrive together in the Beta 1.8 Pre-release, so
 * "this version has no hunger" and "this version eats instantly" are the same fact stated twice, and stating it
 * once is how they stay in agreement.
 *
 * <p>Upstream's {@code IGNORED_EDIBLES} list is not ported: it is a data listing of items to exempt, and no
 * exemption is needed until one is asked for. Nothing here silently drops it - every edible is instantaneous in
 * these versions.
 */
public final class HistoricalFoodHelper {
    private HistoricalFoodHelper() {
    }

    /** Whether this item is edible at all. */
    public static boolean isEdible(ItemStack stack) {
        return stack.has(DataComponents.FOOD);
    }

    /**
     * What each food restored BEFORE hunger existed, from research/historical-features.json entry
     * {@code beta173-instant-food-healing} (confidence: high). The key is the modern item id of the same food.
     *
     * <p>Only the seven foods that entry gives a direct-heal value for are here. Fish and the golden apple are
     * recorded as foods present in the b1.7.3 client but have no value in the entry, so they keep the modern
     * nutrition fallback - a known approximation, narrowed from every food to two.
     */
    private static final java.util.Map<String, Integer> DIRECT_HEAL_BY_ID = java.util.Map.of(
        "minecraft:apple", 4,
        "minecraft:bread", 5,
        "minecraft:mushroom_stew", 10,
        "minecraft:porkchop", 3,
        "minecraft:cooked_porkchop", 8,
        "minecraft:cookie", 1);

    /**
     * The hunger a food filled before the change that moved it, or null when the research records no change.
     *
     * <p>From {@code beta18-food-values-table} in research/historical-features.json: cookies filled 1 hunger until
     * Release 1.3.1, and mushroom stew filled 8 until Release 1.4.2.
     */
    private static final java.util.Map<String, Integer> BETA_HUNGER = java.util.Map.of(
        "minecraft:cookie", 1,
        "minecraft:mushroom_stew", 8);

    /**
     * The golden apple's pre-Release-1.1 values: 10 hunger, and Regeneration for 30 seconds at amplifier 0.
     *
     * <p>Its nutrition and its effect list both change, which is why it is handled apart from the cookie-and-stew
     * table - those are nutrition alone.
     *
     * <p>CORRECTED after two research artefacts in this repository disagreed and the conflict was settled against
     * the primary source. The first implementation gave 10 hunger and NO Regeneration, because only the hunger and
     * saturation were read out of {@code research/historical-features.json}; the effect clause sat in the same
     * evidence string and was missed. That evidence is an official client artifact - the Beta 1.8 client jar,
     * class {@code sv} item registry:
     *
     * <pre>(66,10,1.2F,false).o().a(Regeneration,30,0,1.0F).a("appleGold")</pre>
     *
     * <p>which reads as hunger 10, saturation 1.2, always edible ({@code o()}), and Regeneration applied for 30
     * seconds at amplifier 0 with probability 1.0. {@code research/minecraft-version-history.md} Part IV records
     * the same form, so the two now agree.
     *
     * <p>The numbers are plain values rather than constructed game objects because a unit test cannot build an
     * effect instance without a registry; the mixin assembles the instance from these.
     */
    private static final int GOLDEN_APPLE_BETA_HUNGER = 10;

    /** 30 seconds: the duration the client artifact passes to the effect. */
    private static final int GOLDEN_APPLE_BETA_REGENERATION_TICKS = 600;

    /** Amplifier 0, i.e. Regeneration I. The artifact passes 0, not 1. */
    private static final int GOLDEN_APPLE_BETA_REGENERATION_AMPLIFIER = 0;

    /** Upstream has no equivalent. The pre-Release-1.1 golden apple hunger value. */
    public static int goldenAppleBetaHunger() {
        return GOLDEN_APPLE_BETA_HUNGER;
    }

    /** How long the pre-Release-1.1 golden apple applies Regeneration for. */
    public static int goldenAppleBetaRegenerationTicks() {
        return GOLDEN_APPLE_BETA_REGENERATION_TICKS;
    }

    /** At which amplifier the pre-Release-1.1 golden apple applies Regeneration. */
    public static int goldenAppleBetaRegenerationAmplifier() {
        return GOLDEN_APPLE_BETA_REGENERATION_AMPLIFIER;
    }

    /** The pre-change hunger value for an item id, or null when there is none. */
    public static Integer betaHunger(String itemId) {
        return BETA_HUNGER.get(itemId);
    }

    /**
     * The researched direct-heal value for an item id, or null when the research gives none.
     *
     * <p>Keyed by id rather than by {@code Item} so the values can be asserted in a unit test: this project's tests
     * cannot construct {@code ItemStack}, whose static initialiser needs Minecraft's registries bootstrapped.
     */
    public static Integer researchedHeal(String itemId) {
        return DIRECT_HEAL_BY_ID.get(itemId);
    }

    /**
     * How much health this food restores directly: the researched value where one exists, the modern nutrition
     * value otherwise.
     */
    public static int directHealAmount(net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.food.FoodProperties food) {
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Integer researched = researchedHeal(itemId);
        return researched != null ? researched : food.nutrition();
    }

    /** Whether this item is eaten instantly in the active version. */
    public static boolean isInstantaneousEdible(ItemStack stack) {
        String session = HistoricalActiveSession.current();

        if (session == null || !HistoricalPlayerRules.suppressesHungerTick(session)) {
            return false;
        }

        return isEdible(stack);
    }
}
