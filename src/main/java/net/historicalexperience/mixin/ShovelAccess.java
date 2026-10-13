package net.historicalexperience.mixin;

import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ShovelItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code ShovelItem.FLATTENABLES}, so "can this shovel make a path here" is answered by the game's own table.
 *
 * <p>PORTED from Nostalgic Tweaks {@code mixin/access/ShovelItemAccess.java} (LGPL-3.0; see THIRD_PARTY_NOTICES.md).
 * The same reasoning as {@link AxeAccess}: the game's table is the authority, and a copied list would drift.
 *
 * <p>Verified against the 1.21.1 jar before writing: the field is
 * {@code protected static final Map<Block, BlockState> FLATTENABLES}.
 */
@Mixin(ShovelItem.class)
public interface ShovelAccess {
    @Accessor("FLATTENABLES")
    static Map<Block, BlockState> flattenables() {
        throw new AssertionError();
    }
}
