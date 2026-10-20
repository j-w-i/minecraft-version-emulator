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
package net.historicalexperience.verification;

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
