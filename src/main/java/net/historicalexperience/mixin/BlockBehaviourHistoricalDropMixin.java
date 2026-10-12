package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.List;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.item.ItemStack;
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
 * <h2>Why the second RETURN</h2>
 *
 * <p>{@code getDrops} returns early when a block has no loot table and again after rolling one, so the ordinal
 * selects the one that runs for an ore. Written against the first, this would be a silent no-op.
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
    /** Upstream: {@code nt_mechanics_block$modifyBlockDrops}. */
    @ModifyReturnValue(method = "getDrops", at = @At(value = "RETURN", ordinal = 1))
    private List<ItemStack> historicalExperience$selfDrops(List<ItemStack> drops, BlockState blockState) {
        if (!HistoricalGameplayTweaks.oreDropsAsBlocks()) {
            return drops;
        }

        if (isOre(blockState)) {
            drops.clear();
            drops.add(blockState.getBlock().asItem().getDefaultInstance());
        }

        return drops;
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
