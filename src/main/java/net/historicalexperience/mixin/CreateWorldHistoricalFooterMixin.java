package net.historicalexperience.mixin;

import net.historicalexperience.client.version.CreateWorldHistoricalVersion;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Reserve a real footer row; vanilla then lays out, registers and resizes all its widgets normally. */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldHistoricalFooterMixin {
    @Redirect(method = "init", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToFooter(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private LayoutElement historicalExperience$footer(HeaderAndFooterLayout layout, LayoutElement actions) {
        layout.setFooterHeight(60);
        LinearLayout rows = LinearLayout.vertical().spacing(4);
        rows.addChild(CreateWorldHistoricalVersion.createButton((CreateWorldScreen) (Object) this));
        rows.addChild(actions);
        layout.addToFooter(rows);
        // Vanilla continues populating its original horizontal action row, not our vertical wrapper.
        return actions;
    }
}
