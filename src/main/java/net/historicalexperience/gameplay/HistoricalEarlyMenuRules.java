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
package net.historicalexperience.gameplay;

import java.util.Set;

/** Output policy for specifically audited non-recipe menus; NOT a general block/item availability oracle.
 * Anvil: research/feature-timeline.json (1.4.2/12w41a); grindstone: Minecraft Wiki history (1.14/18w44a,
 * functional 18w48a). Both postdate every offered Indev/Infdev artifact. Other menus need their own audit.
 */
public final class HistoricalEarlyMenuRules {
    private static final Set<String> MENUS = Set.of("anvil", "grindstone");
    private HistoricalEarlyMenuRules() { }
    public static boolean blocksResult(String profile, String menu) {
        return menu != null && MENUS.contains(menu) && EarlyGameplayRecipes.supports(profile);
    }
}
