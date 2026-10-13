package net.historicalexperience.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu integration: the config button in the mod list opens this project's options screen.
 *
 * <h2>Why this is a separate entrypoint class rather than a factory handed over from the client initialiser</h2>
 *
 * <p>Mod Menu discovers this class by name through the {@code modmenu} entrypoint in {@code fabric.mod.json}, so it
 * is loaded <b>only when Mod Menu is present</b>. That is what keeps Mod Menu optional: the mod lists it under
 * {@code suggests} and not {@code depends}, so a player without Mod Menu is not refused entry to the game, and the
 * main client path never references a class from a mod that may be absent. A factory registered from
 * {@code HistoricalExperienceClient} would be a hard dependency in practice, because the class would be resolved on
 * every launch whether or not Mod Menu existed.
 */
public final class HistoricalModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HistoricalConfigScreen::new;
    }
}
