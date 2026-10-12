"""Port PREVENT_HUNGER_EFFECT: the Hunger status effect does nothing in a version with no hunger.

Carrier: food_health/LivingEntityMixin, @ModifyReturnValue on canBeAffected. I read this file in round 38 and set
it aside as out of scope because the worklist did not name it - which the flag-to-research join has since shown was
the subset error again. The row is `confidence: high` and names PREVENT_HUNGER_EFFECT under the hunger feature,
whose quote is "Hunger was introduced" in the Beta 1.8 Pre-release.

The reasoning matches the rest of the hunger family: if a version has no hunger meter, an effect that drains it has
nothing to drain. The gate is therefore the same rule the food and hunger ports use, not a second boundary.
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

MIXIN = '''package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalPlayerRules;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops the Hunger effect applying, in versions that had no hunger for it to drain.
 *
 * <p>DIRECT PORT of part of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/food_health/LivingEntityMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.PREVENT_HUNGER_EFFECT}. The target
 * and the {@code @ModifyReturnValue} shape are upstream's.
 *
 * <h2>Why it was set aside, and why that was wrong</h2>
 *
 * <p>I read this file in the hunger round and concluded the remaining injections were out of scope, because a
 * search of the port worklist found none of the flags. The worklist is a 200-row subset of the research; the join
 * against the research itself shows this row is <b>confidence: high</b> and names the tweak under the hunger
 * feature. "Not in the worklist" has now produced four wrong conclusions.
 *
 * <p>The gate is the hunger rule itself rather than a new boundary: an effect that drains a meter cannot matter in
 * a version that has no meter, and stating that once is how the two stay in agreement.
 *
 * <p>This is a shared mixin - {@code LivingEntity} exists on both sides, and the effect is applied server-side.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHistoricalHungerEffectMixin {
    /** Upstream: {@code nt_food_health$shouldAddHungerEffect}. */
    @ModifyReturnValue(method = "canBeAffected", at = @At("RETURN"))
    private boolean historicalExperience$preventHungerEffect(boolean canBeAffected,
            MobEffectInstance effectInstance) {
        String session = HistoricalActiveSession.current();

        if (effectInstance.getEffect() == MobEffects.HUNGER
            && session != null
            && HistoricalPlayerRules.suppressesHungerTick(session)) {
            return false;
        }

        return canBeAffected;
    }
}
'''
(root / 'src/main/java/net/historicalexperience/mixin/LivingEntityHistoricalHungerEffectMixin.java').write_text(
    MIXIN, encoding='utf-8')
print('wrote the hunger effect mixin')

config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
if 'LivingEntityHistoricalHungerEffectMixin' not in document['mixins']:
    document['mixins'].append('LivingEntityHistoricalHungerEffectMixin')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered; shared mixins =', len(document['mixins']))
