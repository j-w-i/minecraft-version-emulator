package net.historicalexperience.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * PORTED from Nostalgic Tweaks {@code mixin/access/GuiAccess.java} (LGPL-3.0; see THIRD_PARTY_NOTICES.md).
 *
 * <p>Exposes the vanilla armour and air sprites, which the old heads-up display redraws in the positions the
 * hunger bar used to occupy, plus {@code displayHealth}, which the heart-row height calculation reads.
 *
 * <p>These being vanilla fields matters: the old HUD is Minecraft's own icons MOVED, not third-party artwork,
 * so no textures travel with this port.
 */
@Mixin(Gui.class)
public interface GuiAccess {
    @Accessor("ARMOR_FULL_SPRITE")
    static ResourceLocation armorFullSprite() {
        throw new AssertionError();
    }

    @Accessor("ARMOR_HALF_SPRITE")
    static ResourceLocation armorHalfSprite() {
        throw new AssertionError();
    }

    @Accessor("ARMOR_EMPTY_SPRITE")
    static ResourceLocation armorEmptySprite() {
        throw new AssertionError();
    }

    @Accessor("AIR_SPRITE")
    static ResourceLocation airSprite() {
        throw new AssertionError();
    }

    @Accessor("AIR_BURSTING_SPRITE")
    static ResourceLocation airBurstingSprite() {
        throw new AssertionError();
    }

    @Accessor("displayHealth")
    int displayHealth();
}
