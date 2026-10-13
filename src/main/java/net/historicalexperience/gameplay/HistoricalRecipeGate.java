package net.historicalexperience.gameplay;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.historicalexperience.generation.HistoricalBlockAvailability;
import net.historicalexperience.generation.HistoricalBlockContent;import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * Refuses to craft something the active version never had, which is the crafting half of "every version has only
 * the content its own build had".
 *
 * <h2>The owner's report, and why this is not a recipe list</h2>
 *
 * <p>The owner asked for "crafting checkpoints" and gave the example that "I can craft a plethora of way newer
 * stuff", naming beds specifically: beds are Beta 1.3, so a bed must not be craftable in an Alpha world. The
 * obvious implementation is a table of recipe id to introduction version, and it is the wrong one here for two
 * reasons. It would duplicate a fact this project already holds, and it would cover only the recipes somebody
 * remembered to list - the same shape as the hand-written feature exclusions that kept being found incomplete.
 *
 * <p>So the gate asks the <b>existing reviewed availability table</b> instead:
 * {@link HistoricalBlockContent#isAnachronistic} already answers "could this version have contained this block?"
 * for all 29 declared profiles, from the same dated table the world-generation veto uses. A recipe whose result is
 * a block the version could not have is refused, and that covers beds, pistons, enchanting tables, nether brick,
 * quartz, hay, anvils, beacons and everything else in that table at once. When the table gains a boundary the gate
 * gains it too, with no second place to keep in step.
 *
 * <h2>What this deliberately does not do</h2>
 *
 * <p>It does not filter or remove recipes from the recipe manager, which the project forbids, and it does not
 * touch recipe data. It answers the narrower question at the moment a result is offered: is this the wrong era?
 * A refusal is therefore a gameplay decision made per world, and the recipe itself is untouched.
 *
 * <p>It also only judges <b>blocks</b> today - the availability table is a block table - so an item that is not a
 * block (mutton, for instance, which the jars show arriving in Release 1.8) is not covered here. That is a real
 * limit, recorded in research/implementation-coverage.md rather than hidden, and the fix is an item-availability
 * table that does not exist yet.
 *
 * <p>An UNKNOWN verdict is not a refusal. The availability table returns UNKNOWN for a block it cannot date, and
 * the project's rule is that no date means no claim, so an undated block stays craftable - the same direction the
 * generation veto takes, and the safe one: wrongly refusing deletes content the version really had.
 */
public final class HistoricalRecipeGate {
    /** Results already reported, so the log names each distinct refusal once instead of once per click. */
    private static final Set<String> REFUSED = ConcurrentHashMap.newKeySet();
    private static long refusals;

    private HistoricalRecipeGate() {
    }

    /**
     * Whether this crafting result must be refused for this version, without recording anything.
     *
     * <p>Pure so the decision can be unit-tested against the real table, and so the probe can ask it without
     * polluting the counters.
     */
    public static boolean isUnavailable(String versionId, ItemStack result) {
        if (versionId == null || result == null || result.isEmpty()) {
            return false;
        }
        if (!(result.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        return isBlockUnavailable(versionId,
            BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString());
    }

    /**
     * The same question asked of a block id, which is the form a unit test can reach.
     *
     * <p>Split out because {@link #isUnavailable} needs the block registry to turn an {@link ItemStack} into an
     * id, and this project has no test bootstrap - a unit test that touched {@code BuiltInRegistries} died with
     * {@code NoClassDefFoundError} when the village style check tried it. The decision itself is a pure table
     * lookup, so it is kept where it can be tested directly and the registry lookup stays a thin wrapper.
     *
     * <h2>Why only a STATED boundary may refuse a craft</h2>
     *
     * <p>Two kinds of date live in the availability table, and they are not equally strong. A <b>stated</b> one
     * comes from the reviewed research - "beds are Beta 1.3", "cobweb is Beta 1.5". An <b>inferred</b> one comes
     * from an extracted palette: the block's NAME appears in that version's client, so it is dated to the earliest
     * extraction containing it. The extracted set jumps from Alpha v1.1.2_01 straight to Beta 1.7.3, so every
     * block introduced in between is inferred as Beta 1.7.3 - a lower bound on the evidence, not a claim that the
     * block arrived then.
     *
     * <p>For world generation that over-refusal was mostly invisible, because none of those blocks generate
     * naturally. For <b>crafting</b> it is not: the owner's own request was to stop beds being craftable too
     * early, and the first version of this gate would have refused a <b>cake</b> in a Beta 1.2 world - cake is
     * dated {@code beta_1_7_3} from the palette, while the reviewed research states Beta 1.2 introduced it - along
     * with the jukebox, the note block and the piston for the same reason. Refusing content a version really had
     * is the worse error of the two, so an inferred date is not allowed to refuse anything here.
     *
     * <p>A block with NO recorded boundary at all is still refused, and that is the strongest case rather than an
     * oversight: those come from {@link HistoricalBlockAnachronisms}, which lists content later than <b>every</b>
     * declared version - copper, deepslate, the 1.13 ocean blocks, the 1.21 plants.
     */
    public static boolean isBlockUnavailable(String versionId, String blockId) {
        if (versionId == null || blockId == null
            || !HistoricalBlockContent.isAnachronistic(versionId, blockId)) {
            return false;
        }
        var boundary = HistoricalBlockAvailability.boundaryFor(blockId);
        return boundary == null || boundary.timeline();
    }

    /** The same decision, recording the refusal and the evidence for the probe to report. */
    public static boolean refuses(String versionId, ItemStack result) {
        if (!isUnavailable(versionId, result)) {
            return false;
        }
        refusals++;
        String blockId = BuiltInRegistries.BLOCK.getKey(((BlockItem) result.getItem()).getBlock()).toString();
        if (REFUSED.add(versionId + " -> " + blockId)) {
            var boundary = HistoricalBlockAvailability.boundaryFor(blockId);
            net.historicalexperience.HistoricalExperienceMod.LOGGER.info(
                "refusing to craft {} in {}: {}",
                blockId, versionId,
                boundary == null
                    ? "later than every declared version"
                    : "the earliest declared version that had it is " + boundary.versionId());
        }
        return true;
    }

    /** How many crafts this gate has refused since process start. */
    public static long refusalCount() {
        return refusals;
    }

    /** Which version-to-block pairs were refused, for the probe. */
    public static Set<String> refusedPairs() {
        return java.util.Collections.unmodifiableSet(REFUSED);
    }
}
