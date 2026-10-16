/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.server;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;

/** Passive probe-only observation of ACCEPTED new entities, including sections not query-visible yet.
 * Scope is synchronous, thread-local and world-specific. Never captures or deletes preexisting entities.
 */
public final class EarlyMobDropCapture implements AutoCloseable {
    private static final ThreadLocal<EarlyMobDropCapture> ACTIVE = new ThreadLocal<>();
    private final ServerLevel level;
    private final List<Entity> accepted = new ArrayList<>();
    private EarlyMobDropCapture(ServerLevel level) { this.level = level; }
    static EarlyMobDropCapture open(ServerLevel level) {
        if (!Boolean.getBoolean("historical_experience.generationProbe") || ACTIVE.get() != null) throw new IllegalStateException("Owned non-nested probe only");
        var capture = new EarlyMobDropCapture(level); ACTIVE.set(capture); return capture;
    }
    public static void recordAccepted(ServerLevel level, Entity entity) {
        var capture = ACTIVE.get();
        if (capture != null && capture.level == level && (entity instanceof ItemEntity || entity instanceof ExperienceOrb)) capture.accepted.add(entity);
    }
    List<Entity> entities() { return List.copyOf(accepted); }
    @Override public void close() {
        if (ACTIVE.get() != this) throw new IllegalStateException("Mismatched drop observer scope");
        ACTIVE.remove(); accepted.forEach(Entity::discard);
    }
}
