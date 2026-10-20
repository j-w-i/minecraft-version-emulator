/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * Contains material copied or adapted from Nostalgic Tweaks
 * (https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks), Copyright (C) Adrenix and
 * Nostalgic Tweaks contributors, licensed LGPL-3.0-only, and MODIFIED for this project.
 * The upstream file or tweak this class came from is named in its documentation comment.
 *
 * This file is part of a work distributed under the GNU Lesser General Public License,
 * version 3. See LICENSE and THIRD_PARTY_NOTICES.md.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only
 */
package net.historicalexperience.client.hud;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalPlayerRules;
import net.historicalexperience.mixin.client.GuiAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Moves the heads-up display elements the way versions without a hunger bar had them.
 *
 * <p>PORTED from Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/helper/candy/hud/HudHelper.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md). The pose handling, the offsets, the element order and the armour and
 * air drawing routines are upstream's; the changes are listed here rather than left to be discovered:
 *
 * <ul>
 *   <li>{@code CandyTweak.HIDE_HUNGER_BAR.get()} is replaced by {@link #hideHungerBar()}, which answers for
 *       THIS world's version instead of a global config flag.</li>
 *   <li>{@code STAMINA} and {@code StaminaRenderer} are dropped, with the enum member left in place. Stamina is
 *       a Nostalgic Tweaks feature in its own right that this project does not have; stubbing a renderer for it
 *       would have drawn a bar that means nothing.</li>
 *   <li>{@code runAfterSave} is dropped: it exists to poke Exordium, which is not involved here.</li>
 *   <li>{@code GuiUtil}, {@code NullableResult} and {@code NullableAction} are not vendored. The two window
 *       measurements and the null-tolerant reads they provided are inlined below, so the arithmetic is
 *       unchanged without carrying three utility classes for it.</li>
 * </ul>
 *
 * <p>How the hunger bar is actually hidden is worth stating, because it is not what it looks like: upstream
 * does not cancel {@code renderFood}. It PUSHES the pose and translates the food icons ten billion units up,
 * so they are drawn off-screen, and the layout of everything after them is left alone. An earlier version of
 * this project cancelled the render call instead, which hides the same pixels but changes what the surrounding
 * elements believe about the space they are drawing into.
 */
public final class HudHelper {
    /** Whether this mod is currently managing the heads-up display, so the pose stack stays balanced. */
    private static final FlagHolder HUD_MANAGED = FlagHolder.off();

    /** Whether the food element's position has been modified. */
    private static final FlagHolder FOOD_LEVEL_PUSHED = FlagHolder.off();

    /** Whether an armour element has modified the heads-up display positioning. */
    private static final FlagHolder ARMOR_LEVEL_PUSHED = FlagHolder.off();

    /** Whether an air bubble element has modified the heads-up display positioning. */
    private static final FlagHolder AIR_LEVEL_PUSHED = FlagHolder.off();

    /** Every element that pushes a pose, so {@code end} can pop exactly what was pushed. */
    private static final List<FlagHolder> PUSHED_ELEMENTS =
        List.of(FOOD_LEVEL_PUSHED, ARMOR_LEVEL_PUSHED, AIR_LEVEL_PUSHED);

    private HudHelper() {
    }

    /** Whether this world's version had no hunger, so the modern bar must not be shown. */
    public static boolean hideHungerBar() {
        String session = ActiveVersion.current();
        return session != null && HistoricalPlayerRules.suppressesHungerTick(session);
    }

    /** Whether this world's version awarded no experience, so its bar must not be shown. */
    public static boolean hideExperienceBar() {
        String session = ActiveVersion.current();
        return session != null && net.historicalexperience.gameplay.HistoricalLootRules.suppressesExperience(session);
    }

    /**
     * Whether this world's version drew no experience LEVEL NUMBER above the bar.
     *
     * <p>A second boundary, not a corollary of {@link #hideExperienceBar()}: the bar arrives in Beta 1.8's Adventure
     * Update and the number only in Beta 1.9 Prerelease 4, so a Beta 1.8.1 world draws the bar and must not draw the
     * number. See {@code HistoricalGameplayTweaks.experienceLevelShown} for the source of that boundary.
     */
    public static boolean hideExperienceLevel() {
        String session = ActiveVersion.current();
        return session != null
            && Features.active(Feature.HIDDEN_XP_LEVEL, session);
    }

    /** Upstream: {@code begin}. */
    public static void begin(GuiGraphics graphics) {
        if (HUD_MANAGED.ifDisabledThenEnable()) {
            graphics.pose().pushPose();
        }
    }

    /** Upstream: {@code end}. */
    public static void end(GuiGraphics graphics) {
        if (HUD_MANAGED.ifEnabledThenDisable()) {
            PUSHED_ELEMENTS.stream()
                .filter(FlagHolder::ifEnabledThenDisable)
                .forEach(holder -> graphics.pose().popPose());

            graphics.pose().popPose();
        }
    }

    /** Upstream: {@code pop}. */
    public static void pop(GuiGraphics graphics) {
        if (HUD_MANAGED.get() && PUSHED_ELEMENTS.stream().anyMatch(FlagHolder::ifEnabledThenDisable)) {
            graphics.pose().popPose();
        }
    }

    /** Upstream: {@code apply}. */
    public static void apply(GuiGraphics graphics, HudElement element) {
        if (!HUD_MANAGED.get()) {
            return;
        }

        switch (element) {
            case VEHICLE_HEALTH -> {
                if (isVehicleHealthShown() && armorOf(getPlayer()) > 0) {
                    graphics.pose().translate(0.0F, -10.0F, 0.0F);
                }
            }
            case EXPERIENCE_BAR -> {
                if (!isJumpMeterShown()) {
                    graphics.pose().translate(0.0F, 7.0F, 0.0F);
                }
            }
            case FOOD -> {
                FOOD_LEVEL_PUSHED.enable();

                graphics.pose().pushPose();
                graphics.pose().translate(0.0F, 9e10F, 0.0F);
            }
            case ARMOR -> {
                ARMOR_LEVEL_PUSHED.enable();

                graphics.pose().pushPose();
                graphics.pose().translate((float) (guiWidth() / 2 + 90), 0.0F, 0.0F);

                renderArmor(graphics, 39, 0);
            }
            case AIR -> {
                if (hideHungerBar()) {
                    AIR_LEVEL_PUSHED.enable();

                    graphics.pose().pushPose();
                    graphics.pose().translate((float) (guiWidth() / 2 - 100), 0.0F, 0.0F);

                    renderAir(graphics, getHeightOffsetFromHearts(), 0);
                }
            }
            case STAMINA -> {
                // Deliberately empty: stamina is a Nostalgic Tweaks feature this project does not have.
            }
        }
    }

    /** Upstream: {@code isArmorEmpty}. */
    public static boolean isArmorEmpty() {
        return armorOf(getPlayer()) <= 0;
    }

    /** Upstream: {@code renderInverseHalfArmor}. */
    private static void renderInverseHalfArmor(GuiGraphics graphics, int x, int y) {
        TextureAtlasSprite sprite =
            Minecraft.getInstance().getGuiSprites().getSprite(GuiAccess.armorHalfSprite());

        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        builder.addVertex(matrix, x, y + 9, 0.0F).setUv(sprite.getU1(), sprite.getV1());
        builder.addVertex(matrix, x + 9, y + 9, 0.0F).setUv(sprite.getU0(), sprite.getV1());
        builder.addVertex(matrix, x + 9, y, 0.0F).setUv(sprite.getU0(), sprite.getV0());
        builder.addVertex(matrix, x, y, 0.0F).setUv(sprite.getU1(), sprite.getV0());

        BufferUploader.drawWithShader(builder.build());
    }

    /** Upstream: {@code renderArmor}. Draws the armour icons on the right, where the hunger bar used to be. */
    public static void renderArmor(GuiGraphics graphics, int offsetHeight, int offsetRight) {
        int right = offsetRight;
        int armor = armorOf(getPlayer());
        int top = guiHeight() - offsetHeight;

        RenderSystem.enableBlend();

        for (int i = 1; armor > 0 && i < 20; i += 2) {
            right -= 8;

            if (i == armor) {
                renderInverseHalfArmor(graphics, right, top);
            } else if (i < armor) {
                graphics.blitSprite(GuiAccess.armorFullSprite(), right, top, 9, 9);
            } else {
                graphics.blitSprite(GuiAccess.armorEmptySprite(), right, top, 9, 9);
            }
        }

        RenderSystem.disableBlend();
    }

    /** Upstream: {@code renderAir}. Draws the air bubbles above the hearts on the left. */
    public static void renderAir(GuiGraphics graphics, int offsetHeight, int offsetLeft) {
        int air = airOf(getPlayer());
        int top = guiHeight() - offsetHeight;

        int full = Mth.ceil((double) (air - 2) * 10.0D / 300.0D);
        int partial = Mth.ceil((double) air * 10.0D / 300.0D) - full;

        RenderSystem.enableBlend();

        for (int i = 0; i < full + partial; ++i) {
            ResourceLocation sprite = i < full ? GuiAccess.airSprite() : GuiAccess.airBurstingSprite();
            graphics.blitSprite(sprite, offsetLeft + i * 8 + 9, top, 9, 9);
        }

        RenderSystem.disableBlend();
    }

    /** Upstream: {@code getHeightOffsetFromHearts}. */
    private static int getHeightOffsetFromHearts() {
        Player player = getPlayer();
        int absorptionAmount = Mth.ceil(player == null ? 0.0F : player.getAbsorptionAmount());
        int currentHealth = Mth.ceil(player == null ? 0.0F : player.getHealth());
        int displayHealth = ((GuiAccess) Minecraft.getInstance().gui).displayHealth();
        float playerHealth = player == null ? 0.0F : (float) player.getAttributeValue(Attributes.MAX_HEALTH);
        float maxHealth = Math.max(playerHealth, (float) Math.max(displayHealth, currentHealth));
        int numberOfRows = Mth.ceil((maxHealth + (float) absorptionAmount) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (numberOfRows - 2), 3);

        return 39 + (numberOfRows - 1) * rowHeight + 10;
    }

    /** Upstream: {@code isVehicleHealthShown}. */
    private static boolean isVehicleHealthShown() {
        Player player = getPlayer();
        return player != null && player.getVehicle() instanceof LivingEntity;
    }

    /** Upstream: {@code isJumpMeterShown}. */
    private static boolean isJumpMeterShown() {
        Player player = getPlayer();
        return player instanceof LocalPlayer local && local.jumpableVehicle() != null;
    }

    /** Upstream: {@code getPlayer}, reading the camera entity so a spectator sees the right bars. */
    private static Player getPlayer() {
        if (Minecraft.getInstance().getCameraEntity() instanceof Player player) {
            return player;
        }
        return null;
    }

    private static int armorOf(Player player) {
        return player == null ? 0 : player.getArmorValue();
    }

    private static int airOf(Player player) {
        return player == null ? 0 : player.getAirSupply();
    }

    private static int guiWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    private static int guiHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }
}
