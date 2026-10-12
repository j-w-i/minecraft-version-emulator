package net.historicalexperience.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * The red damage tint old versions applied to armour.
 *
 * <p>PORTED from Nostalgic Tweaks {@code common/.../helper/candy/ArmorHelper.java} (LGPL-3.0; see
 * THIRD_PARTY_NOTICES.md), which answers {@code CandyTweak.OLD_DAMAGE_ARMOR_TINT}. The three predicates and both
 * return values are upstream's.
 *
 * <p>What it is: modern Minecraft tints only the BODY red when an entity is hurt. Before that, the armour tinted
 * with it, so a hit was visible on the armour as well - which is why its absence reads as armour not reacting to
 * damage at all.
 */
public final class ArmorHelper {
    private ArmorHelper() {
    }

    /** Upstream: {@code isEntityHurt}. */
    public static boolean isEntityHurt(LivingEntity entity) {
        return entity != null && (entity.hurtTime > 0 || entity.deathTime > 0);
    }

    /** Upstream: {@code useOldTint}. */
    public static boolean useOldTint(LivingEntity entity) {
        return isEntityHurt(entity);
    }

    /** Upstream: {@code getDamagedConsumer}. */
    public static VertexConsumer getDamagedConsumer(LivingEntity entity, VertexConsumer armorConsumer,
            MultiBufferSource bufferSource, ResourceLocation armorLocation) {
        if (useOldTint(entity)) {
            return bufferSource.getBuffer(RenderType.entityCutoutNoCullZOffset(armorLocation));
        }
        return armorConsumer;
    }

    /** Upstream: {@code getDamagedPackedOverlay}. {@code v(true)} is the red hurt overlay. */
    public static int getDamagedPackedOverlay(LivingEntity entity, int packedOverlay) {
        if (useOldTint(entity)) {
            return OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(true));
        }
        return packedOverlay;
    }
}
