/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.mixin;

import net.historicalexperience.client.version.CreateWorldHistoricalVersion;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reserve a real footer row; vanilla then lays out, registers and resizes all its widgets normally. */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldHistoricalFooterMixin {
    @Inject(method = "openFresh", at = @At("HEAD"), require = 1)
    private static void historicalExperience$freshChoice(CallbackInfo ci) {
        CreateWorldHistoricalVersion.Selection.clear();
        net.historicalexperience.world.HistoricalSnapshotCreation.clear();
        net.historicalexperience.client.version.IndevMapSelection.clear();
    }

    @Inject(method = "createFromExisting", at = @At("HEAD"), require = 1)
    private static void historicalExperience$recreatedChoice(CallbackInfoReturnable<CreateWorldScreen> cir) {
        CreateWorldHistoricalVersion.Selection.clear();
        net.historicalexperience.world.HistoricalSnapshotCreation.clear();
        net.historicalexperience.client.version.IndevMapSelection.clear();
    }

    @Inject(method = "createNewWorldDirectory", at = @At("RETURN"), require = 1)
    private void historicalExperience$bindSnapshotDirectory(CallbackInfoReturnable<java.util.Optional<net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess>> cir) {
        cir.getReturnValue().ifPresent(access -> CreateWorldHistoricalVersion.bindSnapshotToNewWorld(
            (CreateWorldScreen) (Object) this, access.getLevelPath(net.minecraft.world.level.storage.LevelResource.ROOT)));
    }

    @Redirect(method = "init", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToFooter(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private LayoutElement historicalExperience$footer(HeaderAndFooterLayout layout, LayoutElement actions) {
        layout.setFooterHeight(60);
        LinearLayout rows = LinearLayout.vertical().spacing(4);
        rows.addChild(CreateWorldHistoricalVersion.createRow((CreateWorldScreen) (Object) this));
        rows.addChild(actions);
        layout.addToFooter(rows);
        // Vanilla continues populating its original horizontal action row, not our vertical wrapper.
        return actions;
    }
}
