/* SPDX-License-Identifier: LGPL-3.0-or-later */
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
