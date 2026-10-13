package net.historicalexperience.mixin;

import net.historicalexperience.client.SlotPositionAccess;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Makes a slot's coordinates writable so it can be moved off-screen.
 *
 * <p><b>Ported from Nostalgic Tweaks</b>, package {@code mod.adrenix.nostalgic.mixin.duck} and
 * {@code mod.adrenix.nostalgic.mixin.access}. Upstream moves the offhand slot with
 * {@code SlotTracker.OFF_HAND.move(slot, -9999, -9999)}, writing {@code slot.x} and {@code slot.y} directly - which
 * does not compile against 1.21.1, where both fields are {@code final}. {@code @Mutable} on a shadowed field is the
 * supported way to lift that, and it is the same convention upstream uses wherever it needs to write a final field.
 *
 * <p>Registered as a CLIENT mixin. The slot's position is client-side layout, so nothing on the server needs it, and
 * keeping it client-side means the server's view of the menu is untouched - which is what the earlier
 * "Network Protocol Error" taught.
 */
@Mixin(Slot.class)
public abstract class SlotPositionMixin implements SlotPositionAccess {
    @Mutable
    @Shadow
    public int x;

    @Mutable
    @Shadow
    public int y;

    @Unique
    @Override
    public void historicalExperience$setSlotPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
