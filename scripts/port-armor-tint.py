"""Port OLD_DAMAGE_ARMOR_TINT: armour flashes red when its wearer is hurt.

Carrier: fabric/.../candy/armor_damage/HumanoidArmorLayerMixin plus helper/candy/ArmorHelper (67 lines).
The entity is tracked while armour renders, the vertex consumer is swapped when hurt, and the packed overlay is
replaced with the red one - OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(true)).

TWO DELIBERATE DIFFERENCES from upstream, both stated:

1. The entity is captured in renderArmorPiece, which RECEIVES the entity as a parameter, rather than in render with
   a HEAD/RETURN pair as upstream does. Same result, one injection instead of two, and no window where the field
   is stale between a HEAD that sets it and a RETURN that clears it.
2. Upstream's two renderTrim injections are NOT ported. Armour trims arrive in Release 1.20.5; no declared profile
   is within a decade of that, so the trim path cannot render here and porting it would add code no version can
   reach - the same reasoning that left out stray drops and the off-hand shield case.

The boundary is UNSOURCED and said so: no research row names this tweak, and nothing dates when armour stopped
taking the hurt tint. It is applied to every declared version, which is the assumption that the tint is old
behaviour in all of them - if that is ever contradicted, this gate is where the correction goes.
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

(root / 'src/main/java/net/historicalexperience/client/render/ArmorHelper.java').write_text(
'''package net.historicalexperience.client.render;

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
''', encoding='utf-8')
print('wrote ArmorHelper')

MIXIN = '''package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.historicalexperience.client.render.ArmorHelper;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes armour take the red hurt tint, in versions where it did.
 *
 * <p>PORTED from Nostalgic Tweaks
 * {@code fabric/src/main/java/mod/adrenix/nostalgic/fabric/mixin/tweak/candy/armor_damage/HumanoidArmorLayerMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code CandyTweak.OLD_DAMAGE_ARMOR_TINT}. The consumer
 * swap and the overlay argument are upstream's.
 *
 * <p>Two deliberate differences, both in this file's own history rather than implied: the entity is captured in
 * {@code renderArmorPiece}, which receives it as a parameter, instead of upstream's HEAD/RETURN pair around
 * {@code render}; and upstream's {@code renderTrim} injections are not ported, because armour trims arrive in
 * Release 1.20.5 and no declared profile can render one.
 *
 * <p>This is a CLIENT mixin, registered under {@code client}.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerHistoricalTintMixin<T extends LivingEntity,
        M extends HumanoidModel<T>, A extends HumanoidModel<T>> {
    @Unique private LivingEntity historicalExperience$entity;

    /** Captures the wearer, which {@code renderModel} does not receive. */
    @Inject(method = "renderArmorPiece", at = @At("HEAD"))
    private void historicalExperience$captureEntity(PoseStack poseStack, MultiBufferSource buffer,
            T entity, EquipmentSlot slot, int packedLight, A model, CallbackInfo callback) {
        this.historicalExperience$entity = entity;
    }

    /** Upstream: {@code nt_fabric_armor_damage$setRenderTypeConsumerForModel}. */
    @ModifyExpressionValue(
        method = "renderModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer("
                + "Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer historicalExperience$damagedConsumer(VertexConsumer vertexConsumer, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, A model, int dyeColor, ResourceLocation location) {
        return ArmorHelper.getDamagedConsumer(this.historicalExperience$entity, vertexConsumer, bufferSource,
            location);
    }

    /** Upstream: {@code nt_fabric_armor_damage$setOverlayTextureForModel}. Index 3 is the packed overlay. */
    @ModifyArg(
        method = "renderModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer("
                + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
        index = 3)
    private int historicalExperience$damagedOverlay(int packedOverlay) {
        return ArmorHelper.getDamagedPackedOverlay(this.historicalExperience$entity, packedOverlay);
    }
}
'''
(root / 'src/main/java/net/historicalexperience/mixin/HumanoidArmorLayerHistoricalTintMixin.java').write_text(
    MIXIN, encoding='utf-8')
print('wrote the armour tint mixin')

config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
document.setdefault('client', [])
if 'HumanoidArmorLayerHistoricalTintMixin' not in document['client']:
    document['client'].append('HumanoidArmorLayerHistoricalTintMixin')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered; client mixins =', len(document['client']))
