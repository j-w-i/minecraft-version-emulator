package net.historicalexperience.mixin;

import java.util.Map;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code Sheep.ITEM_BY_DYE}, the dye-to-wool map.
 *
 * <p>PORTED from Nostalgic Tweaks {@code mixin/access/SheepAccess.java} (LGPL-3.0; see THIRD_PARTY_NOTICES.md).
 * Old sheep drops give the wool of the sheep's own colour rather than white wool, which is what this map is for.
 *
 * <p>Verified against the 1.21.1 jar before writing: the field is
 * {@code private static final Map<DyeColor, ItemLike> ITEM_BY_DYE}. A wrong field name here is a fatal mixin
 * error at start-up, not a warning.
 */
@Mixin(Sheep.class)
public interface SheepAccess {
    @Accessor("ITEM_BY_DYE")
    static Map<DyeColor, ItemLike> itemByDye() {
        throw new AssertionError();
    }
}
