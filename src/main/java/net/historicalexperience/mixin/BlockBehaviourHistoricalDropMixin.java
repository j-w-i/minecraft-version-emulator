package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.List;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes ores drop themselves, in versions that had no raw metals.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/mechanics_block/BlockBehaviourMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.SELF_BLOCK_DROPS}. The injection point
 * - {@code @ModifyReturnValue} on {@code getDrops} at the second RETURN - and the replacement itself are upstream's:
 * the drops list is cleared and the block's own item is added.
 *
 * <h2>Why both RETURNs, and the bug that taught me</h2>
 *
 * <p>{@code getDrops} has two returns: the first fires when the block has <b>no loot table at all</b> and hands
 * back {@code Collections.emptyList()}, and the second fires after a loot table was rolled. The upstream port used
 * the second, which is the one an ore takes - and that was correct for ores and silently wrong for everything
 * without a loot table.
 *
 * <p>The cost of getting that wrong was a shipped defect the owner found by playing: Beta 1.9 Pre3's End portal
 * frames blew up (the explosion-resistance hook worked, and is a different mixin) and dropped <b>nothing</b>,
 * because {@code end_portal_frame} has no loot table in 1.21.1 - verified by listing the jar, where
 * {@code data/minecraft/loot_table/blocks/end_portal_frame.json} does not exist while its blockstate and models do
 * - so it takes the early return and the ordinal-1 injection never ran.
 *
 * <p>The pair below is therefore not redundancy. Each handler names the path it serves: the ordinal-0 one for
 * blocks with no loot table, the ordinal-1 one for rolled tables. The frame rule is claimed by the first, the ore
 * rule by the second, and the spawner rule by both - because "no loot table" is an implementation detail of which
 * blocks happen to have one, and a rule keyed on a block rather than on that detail must not care which path the
 * engine chose. Both handlers count RETURNs of the same type, so ordinal 1 is still the post-roll return; a third
 * return added by a future version would need this looked at again rather than assumed.
 *
 * <h2>Scope</h2>
 *
 * <p>Upstream's listing holds iron, gold and copper ore plus their deepslate variants. Four of the six cannot exist
 * in any declared profile - the newest is Release 1.6.4 - and they are kept so the port matches upstream. The
 * boundary is Release 1.17, where raw metals arrive, which is later than every profile; this project's own
 * availability data already dates raw_iron, raw_gold and raw_copper to 1.17.
 *
 * <p>A shared mixin: block drops are rolled server-side.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourHistoricalDropMixin {
    /**
     * The early return: this block has no loot table, so vanilla hands back an empty list.
     *
     * <p>Upstream did not have this handler. It exists because the End portal frame is one of these blocks and the
     * upstream-shaped injection could not reach it.
     */
    @ModifyReturnValue(method = "getDrops", at = @At(value = "RETURN", ordinal = 0))
    private List<ItemStack> historicalExperience$selfDropsWithoutLootTable(List<ItemStack> drops,
            BlockState blockState) {
        if (!HistoricalGameplayTweaks.oreDropsAsBlocks()) {
            return drops;
        }
        // Beta 1.9 Pre3 ONLY: the frame dropped as an item when an explosion destroyed it, which Pre4 fixed.
        // minecraft.wiki, End Portal Frame history: Pre3 "It can drop as an item due to explosions", Pre4 "The
        // End portal can no longer be broken by explosions". Mined directly it stays unobtainable - its hardness
        // is -1 - so an explosion is the only way to reach this, and the frame has NO loot table, which is exactly
        // why this handler and not the other one is the one that must carry it.
        //
        // Returns a NEW list rather than clearing the one it was given, and that is the second half of the owner's
        // bug rather than a style preference. This return path hands back `Collections.emptyList()`, which is
        // IMMUTABLE, so `drops.clear()` threw UnsupportedOperationException inside the explosion's own drop call -
        // the engine swallowed it and the frame dropped nothing. The runtime probe caught it as
        // `endPortalFrame: probeFailed: java.lang.UnsupportedOperationException` on Pre3 alone, which is also the
        // proof that the explosion really does route through this method. A mutable list is the fix; the two
        // mutation sites below were changed for the same reason, since a loot roll is not guaranteed mutable either.
        if (blockState.is(Blocks.END_PORTAL_FRAME) && HistoricalGameplayTweaks.endPortalFrameExplodes()) {
            return List.of(blockState.getBlock().asItem().getDefaultInstance());
        }
        return drops;
    }

    /** Upstream: {@code nt_mechanics_block$modifyBlockDrops}. The post-roll return, which an ore takes. */
    @ModifyReturnValue(method = "getDrops", at = @At(value = "RETURN", ordinal = 1))
    private List<ItemStack> historicalExperience$selfDrops(List<ItemStack> drops, BlockState blockState) {
        if (!HistoricalGameplayTweaks.oreDropsAsBlocks()) {
            return drops;
        }

        // The End portal frame rule is deliberately NOT here. It lives in the ordinal-0 handler above, because the
        // frame has no loot table and therefore never reaches this return. Keeping a copy here was the first shape
        // of the fix and it was wrong in a way worth naming: dead code that reads like the live rule, so a later
        // reader would find two frame rules and no way to tell which one the game takes.

        // Beta 1.9 Pre4 ONLY: Silk Touch made monster spawners drop themselves, and Pre5 removed it - so the MODERN
        // game is the one WITHOUT the drop, which is the opposite of how every other rule in this file reads.
        // minecraft.wiki, Monster Spawner history: Pre4 "Monster spawners now drop when mined using a tool
        // enchanted with the new Silk Touch enchantment", Pre5 "Monster spawners no longer drop when broken using a
        // tool enchanted with Silk Touch".
        //
        // The tool is not a parameter of getDrops - it reaches the loot pipeline as a `silk_touch` loot CONDITION,
        // and this mixin runs after the table has been rolled. That is a help rather than an obstacle: the list
        // returned here is non-empty only when Silk Touch was actually applied, so testing the RESULT is a
        // self-selecting Silk Touch test and cannot suppress ordinary mining. A version gate alone would be wrong -
        // it would empty the list for every other tool too.
        if (isSpawnerDrop(drops, blockState) && !HistoricalGameplayTweaks.silkySpawnerDrops()) {
            return List.of();
        }

        if (isOre(blockState)) {
            return List.of(blockState.getBlock().asItem().getDefaultInstance());
        }

        return drops;
    }

    /**
     * Whether a rolled drop list is the monster spawner's self-drop, which only Silk Touch produces.
     *
     * <h2>Why this reaches for the block's own item instead of {@code Items.SPAWNER}</h2>
     *
     * <p>Because an expected stack is not needed and constructing one costs more than it looks: the block is
     * already in hand, and {@code blockState.getBlock().asItem()} is the same item. An earlier version of this
     * built {@code new ItemStack(Items.SPAWNER)} to compare against and could not be tested at all, which is
     * recorded here rather than erased.
     *
     * <h2>What is NOT verified, stated plainly</h2>
     *
     * <p>There is no unit test of this method, and it must stay {@code private}. It was made {@code public} for one
     * round to let a test call it, and that <b>crashed every dedicated-server launch</b>:
     * {@code InvalidMixinException: Mixin ... contains non-private static method
     * isSpawnerDrop(Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;)Z}. Mixin refuses to
     * transform a mixin class whose static helpers are visible, because they would be merged onto the target and
     * become part of {@code BlockBehaviour}'s API. A green compile says nothing about that - only starting the game
     * does, which is the same lesson this project has recorded twice before.
     *
     * <p>So the version half of the rule is what is pinned in a unit test
     * ({@code HistoricalGameplayTweaks.appliesTo("silkySpawners", ...)} applies to exactly Pre4 across all 32
     * declared builds), and the rest is observed by the runtime block-drop probe, which reaches it through the
     * public {@code getDrops} entry point the explosion path itself uses.
     */
    private static boolean isSpawnerDrop(List<ItemStack> drops, BlockState blockState) {
        if (!blockState.is(Blocks.SPAWNER)) {
            // Not a spawner: no rolled list from any other block can be the spawner self-drop. This is also the
            // cheap guard, since the loop below only matters for the one block that has this rule.
            return false;
        }
        net.minecraft.world.item.Item spawnerItem = blockState.getBlock().asItem();
        for (ItemStack drop : drops) {
            if (drop.is(spawnerItem)) {
                return true;
            }
        }
        return false;
    }

    /** Upstream's listing, verbatim. */
    private static boolean isOre(BlockState blockState) {
        return blockState.is(Blocks.IRON_ORE)
            || blockState.is(Blocks.GOLD_ORE)
            || blockState.is(Blocks.COPPER_ORE)
            || blockState.is(Blocks.DEEPSLATE_IRON_ORE)
            || blockState.is(Blocks.DEEPSLATE_GOLD_ORE)
            || blockState.is(Blocks.DEEPSLATE_COPPER_ORE);
    }
}
