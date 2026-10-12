"""Port SELF_BLOCK_DROPS: ores drop the ore block, not raw metal.

Carrier: gameplay/mechanics_block/BlockBehaviourMixin (37 lines), which does NOT touch loot tables - it modifies
the result of BlockBehaviour.getDrops at the second RETURN, clears it, and adds the block's own item. The listing
it consults (DefaultListing.selfBlockDrops) holds six blocks: iron, gold and copper ore plus their deepslate
variants.

The owner asked for this from Nostalgic Tweaks rather than from my own reasoning, and upstream's shape is the
right one: the drops list is replaced wholesale, so the block's own loot table never contributes.

Two notes carried into the port:
  * upstream's listing is setDisabled(true) - default off. Per the owner's standing instruction, behaviour here is
    gated per version rather than by a config flag, so that does not disqualify it.
  * four of the six blocks - copper ore and all three deepslate ores - cannot exist in any declared profile, since
    the newest is Release 1.6.4. They are listed anyway so the port matches upstream, and said so rather than
    implied.

Boundary: raw metals arrive in Release 1.17, which is later than every declared profile, so every profile drops
the block. That dating is already in this project's own availability data, which marks raw_iron, raw_gold,
raw_copper and the raw blocks as 1.17 additions.
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

MIXIN = '''package net.historicalexperience.mixin;

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
'''
(root / 'src/main/java/net/historicalexperience/mixin/BlockBehaviourHistoricalDropMixin.java').write_text(
    MIXIN, encoding='utf-8')
print('wrote the self block drop mixin')

config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
if 'BlockBehaviourHistoricalDropMixin' not in document['mixins']:
    document['mixins'].append('BlockBehaviourHistoricalDropMixin')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered; shared mixins =', len(document['mixins']))

tweaks = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java'
text = tweaks.read_text(encoding='utf-8')
old = '''    public static boolean betaFoodHungerValues(String versionId) {'''
new = '''    /**
     * Whether ores drop the ore block rather than a raw metal.
     *
     * <p>Raw iron, gold and copper arrive in Release 1.17, which is later than every declared profile, so all ten
     * drop the block. The dating is already in this project's availability data, which marks the raw items and
     * their blocks as 1.17 additions.
     *
     * <p>Nostalgic Tweaks implements this as {@code SELF_BLOCK_DROPS}, a listing of six ore blocks, with the server
     * flag defaulting to off; here the version decides.
     */
    public static boolean oreDropsAsBlocks() {
        return isDeclaredVersion(HistoricalActiveSession.current());
    }

    public static boolean betaFoodHungerValues(String versionId) {'''
assert old in text, 'tweaks anchor not found'
tweaks.write_text(text.replace(old, new, 1), encoding='utf-8')
print('added oreDropsAsBlocks')
