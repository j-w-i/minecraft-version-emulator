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
package net.historicalexperience.world.metadata;

/**
 * A single entry in the world's historical revision history.
 *
 * @param revision  monotonically increasing revision number
 * @param timestamp epoch milliseconds at which the revision was recorded
 * @param actor     free-form attribution, normally the mod id that performed the change
 * @param note      short human readable description of what changed
 */
public record MetadataRevision(long revision, long timestamp, String actor, String note) {
    public MetadataRevision {
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be >= 1, got " + revision);
        }
        if (timestamp < 0) {
            throw new IllegalArgumentException("timestamp must be >= 0, got " + timestamp);
        }
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("note must not be blank");
        }
    }
}
