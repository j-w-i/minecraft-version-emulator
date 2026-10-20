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
package net.historicalexperience.version;

/**
 * One mob's death drop for a version.
 *
 * <p>A pure data carrier owned by the version model (see {@link Exclusion}). It moved here unchanged from
 * {@code DropRule}; the per-version drop tables and the code that applies them are still in
 * {@code net.historicalexperience.gameplay}.
 *
 * @param entityTypeId      the modern entity type this applies to
 * @param itemId            the item the version dropped, by its modern identifier
 * @param maxExclusiveCount the count bound: the version dropped {@code random.nextInt} of this, so
 *                          Beta's {@code nextInt(3)} is 3 and means 0 to 2 items
 * @param evidence          the client class and item field the value was read from
 */
public record DropRule(
    String entityTypeId,
    String itemId,
    int maxExclusiveCount,
    String evidence
) {
}
