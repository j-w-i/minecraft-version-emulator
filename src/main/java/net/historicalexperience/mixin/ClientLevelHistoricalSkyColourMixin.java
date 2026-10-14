package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.client.lighting.HistoricalWorldTweaks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The old uniform sky colour, applied where the sky renderer actually reads it.
 *
 * <h2>Source</h2>
 *
 * <p>Ported from Nostalgic Tweaks {@code SkyHelper.getOldColor} (LGPL-3.0; see THIRD_PARTY_NOTICES.md). The colours
 * are upstream's and unchanged; only the CHOICE comes from the world's version, by the owner's rule: <b>alpha
 * versions get the alpha sky, beta versions get the beta sky, and the releases get the modern sky.</b>
 *
 * <h2>Why this method, and not the biome</h2>
 *
 * <p>{@code LevelRenderer.renderSky} calls {@code ClientLevel.getSkyColor(Vec3, float)} for the colour of the sky
 * dome, so this is the value on screen. The biome also has a {@code getSkyColor} method, but which overload the
 * renderer reaches is a chain of internal calls, and hooking the method the renderer calls directly is both simpler
 * and certain. The Nether's branch lives here too, which is why one hook covers the overworld and the nether sky
 * together - {@code ClientLevel} is where the two dimensions are told apart.
 *
 * <h2>Nothing is substituted without a version</h2>
 *
 * <p>{@code skyColourOverride()} returns null for an ordinary world, for the releases, and when the player turned
 * the option off, and this returns the game's own colour in every one of those cases. The nether colour obeys the
 * same switch, so an ordinary world's nether is untouched.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelHistoricalSkyColourMixin {
    @ModifyReturnValue(method = "getSkyColor", at = @At("RETURN"), require = 1)
    private Vec3 historicalExperience$oldSkyColour(Vec3 colour, Vec3 cameraPos, float partialTick) {
        if (HistoricalWorldTweaks.skyColourOverride() == null) {
            return colour;
        }
        Integer nether = HistoricalWorldTweaks.netherSkyColour();
        if (nether != null && ((Level) (Object) this).dimension() == Level.NETHER) {
            return vecFromPacked(nether);
        }
        Integer era = HistoricalWorldTweaks.eraSkyColour(
            net.historicalexperience.client.HistoricalClientRules.activeVersion());
        return era == null ? colour : vecFromPacked(era);
    }

    /** Unpacks a 24-bit colour into the 0-1 components the sky colour is expressed in. */
    private static Vec3 vecFromPacked(int packed) {
        return new Vec3(
            (packed >> 16 & 0xFF) / 255.0D,
            (packed >> 8 & 0xFF) / 255.0D,
            (packed & 0xFF) / 255.0D);
    }
}
