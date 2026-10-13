package net.historicalexperience.mixin;

import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.AxeItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code AxeItem.STRIPPABLES}, so "can this axe strip this block" is answered by the game's own table.
 *
 * <p>PORTED from Nostalgic Tweaks {@code mixin/access/AxeItemAccess.java} (LGPL-3.0; see THIRD_PARTY_NOTICES.md).
 *
 * <h2>Why an accessor rather than a copied list</h2>
 *
 * <p>This is the table the game itself consults when an axe right-clicks a log, so asking it means a block that a
 * datapack, another mod or a later version adds to stripping is respected automatically. A hand-written list would
 * disagree with the game the first time anything changed, and the disagreement would surface as a bug in this mod.
 *
 * <p>Verified against the 1.21.1 jar before writing: the field is
 * {@code protected static final Map<Block, Block> STRIPPABLES}. A wrong name here is a fatal mixin error at
 * start-up, not a warning.
 */
@Mixin(AxeItem.class)
public interface AxeAccess {
    @Accessor("STRIPPABLES")
    static Map<Block, Block> strippables() {
        throw new AssertionError();
    }
}
