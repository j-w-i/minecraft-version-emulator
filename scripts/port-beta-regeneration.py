"""Give the hunger profiles Beta 1.8's regeneration model.

research/historical-features.json, entry beta18-natural-regeneration-rule (confidence: high):
    regenerationFoodThreshold      = 18
    regenerationInterval           = 80
    regenerationHealPerInterval    = 1
    regenerationCostsExhaustion    = False

Modern FoodData.tick has TWO regeneration branches. The bytecode shows the first one gated on
"foodLevel >= 20" with a 10-tick timer and an exhaustion charge; the second is the one Beta 1.8 had, gated on
foodLevel >= 18 with an 80-tick timer. So the Beta model is the second branch alone, with no exhaustion cost.

Two hooks, both surgical:
  1. @ModifyConstant on the int 20 comparison - returned as 21, so "foodLevel >= 20" can never hold (the maximum
     food level is 20) and the fast branch is skipped rather than deleted.
  2. @ModifyArg on the SECOND addExhaustion call - the slow branch's charge, returned as 0.

ASSUMPTION, stated because it is not sourced: the entry does not say when the fast branch or the exhaustion cost
arrived. Every declared profile with hunger is Release 1.6.4 or earlier, so all six are treated as being on the
Beta model. If a change date is ever sourced between Beta 1.8 and Release 1.6.4, this gate is where it goes.
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

MIXIN = '''package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Restores Beta 1.8's regeneration: one slow branch, and no exhaustion cost.
 *
 * <p>From research/historical-features.json, entry {@code beta18-natural-regeneration-rule} (confidence: high):
 * regeneration needs food 18 or more, heals once per 80 ticks, and <b>costs no exhaustion</b>. Modern
 * {@code FoodData.tick} has an additional branch - food 20 or more, every 10 ticks, charging exhaustion - so a
 * modern player regenerates far faster than these versions did and pays saturation for it.
 *
 * <p>Nostalgic Tweaks has no equivalent, so this is implemented from the research. The two hooks are placed against
 * the bytecode rather than the source: the fast branch is gated on an {@code int 20} comparison, and the slow
 * branch charges exhaustion through its own call.
 *
 * <p>The gate is the hunger boundary, because a version with no hunger meter has no regeneration to model. The
 * entry does not date the arrival of the fast branch or the exhaustion cost, so every hunger-bearing profile here
 * is treated as Beta-era; that assumption is written down rather than implied.
 *
 * <p>A shared mixin: {@code FoodData} is server-side state.
 */
@Mixin(FoodData.class)
public abstract class FoodDataHistoricalRegenerationMixin {
    /** Upstream has no equivalent. Makes the fast branch unreachable; the maximum food level is 20. */
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 20))
    private int historicalExperience$skipFastRegeneration(int threshold) {
        return usesBetaRegeneration() ? 21 : threshold;
    }

    /**
     * Upstream has no equivalent. Zeroes the exhaustion charge on the SLOW branch, which is the second
     * {@code addExhaustion} call in the method.
     */
    @ModifyArg(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;addExhaustion(F)V",
            ordinal = 1),
        index = 0)
    private float historicalExperience$noExhaustionFromRegeneration(float exhaustion) {
        return usesBetaRegeneration() ? 0.0F : exhaustion;
    }

    /** Whether the active version regenerates the Beta 1.8 way. */
    private static boolean usesBetaRegeneration() {
        return HistoricalGameplayTweaks.betaRegenerationModel(HistoricalActiveSession.current());
    }
}
'''
(root / 'src/main/java/net/historicalexperience/mixin/FoodDataHistoricalRegenerationMixin.java').write_text(
    MIXIN, encoding='utf-8')
print('wrote the regeneration mixin')

config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
if 'FoodDataHistoricalRegenerationMixin' not in document['mixins']:
    document['mixins'].append('FoodDataHistoricalRegenerationMixin')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered; shared mixins =', len(document['mixins']))

tweaks = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java'
text = tweaks.read_text(encoding='utf-8')
old = '''    public static boolean betaCriticalHitFormula(String versionId) {'''
new = '''    /**
     * Whether regeneration uses the Beta 1.8 model: food 18 or more, once per 80 ticks, and no exhaustion cost.
     *
     * <p>Gated on the hunger boundary, since a version with no hunger meter has no regeneration to model. The
     * research does not date when the modern fast branch or the exhaustion charge arrived, and every declared
     * profile with hunger is Release 1.6.4 or earlier, so all six are treated as Beta-era. That assumption is
     * deliberate and would be replaced by a real boundary the moment one is sourced.
     */
    public static boolean betaRegenerationModel(String versionId) {
        return isDeclaredVersion(versionId) && !BEFORE_BETA_1_8.contains(versionId);
    }

    public static boolean betaCriticalHitFormula(String versionId) {'''
assert old in text, 'tweaks anchor not found'
tweaks.write_text(text.replace(old, new, 1), encoding='utf-8')
print('added betaRegenerationModel')
