package net.historicalexperience.gameplay;

import net.minecraft.world.entity.LivingEntity;

/**
 * Re-runs a living entity's equipment pass, so version-dependent attribute modifiers take effect.
 *
 * <p>Called once the world's historical version is known - on the server when a player joins, and on the
 * client when the server's payload arrives. Without it, an item already in hand keeps the modern attribute
 * map until the player happens to switch items, which is a change nobody would think to make.
 */
public final class HistoricalEquipmentRefresh {
    private HistoricalEquipmentRefresh() {
    }

    /** Recomputes the entity's equipment-derived attributes, if it is one this build can refresh. */
    public static void refresh(LivingEntity entity) {
        if (entity instanceof net.historicalexperience.mixin.LivingEntityEquipmentInvoker invoker) {
            try {
                invoker.invokeCollectEquipmentChanges();
            } catch (RuntimeException e) {
                // A refresh failure must not break a login: the worst case is the modern attribute map, which
                // is exactly the state this call was trying to improve on.
                net.historicalexperience.HistoricalExperienceMod.LOGGER.warn(
                    "could not refresh equipment attributes after the version was set", e);
            }
        }
    }
}
