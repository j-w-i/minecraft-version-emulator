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
package net.historicalexperience.client;

import net.historicalexperience.client.lighting.HistoricalWorldTweaks;
import net.historicalexperience.mixin.client.ImageButtonHistoricalSpritesAccess;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;

/**
 * Version-gated port of Nostalgic Tweaks' InventoryScreenHelper.setRecipeButton DISABLED branch.
 * The recipe-book panel's visibility does not hide its independent screen button, so both are handled.
 */
public final class HistoricalRecipeBookButton {
    private HistoricalRecipeBookButton() {
    }

    /**
     * Locate the vanilla recipe-book button by its WidgetSprites identity and hide/deactivate it for historical worlds.
     * This is called from the same init-tail callers as Nostalgic Tweaks: inventory, crafting, and furnace screens.
     */
    public static void setRecipeButton(Screen screen) {
        if (!HistoricalWorldTweaks.recipeBookDisabled()) {
            return;
        }
        for (GuiEventListener child : screen.children()) {
            if (child instanceof ImageButton button
                && ((ImageButtonHistoricalSpritesAccess) button).historicalExperience$getSprites()
                    .equals(RecipeBookComponent.RECIPE_BUTTON_SPRITES)) {
                // Direct port of InventoryScreenHelper.setRecipeButton's DISABLED branch.
                button.active = false;
                button.visible = false;
                return;
            }
        }
    }
}
