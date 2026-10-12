"""Give the five pre-1.6 profiles Beta 1.8's critical hit formula.

research/historical-features.json, entry beta18-combat-critical-hits (medium confidence):
    criticalHitFormulaBeta18 = "random +0%..+100% of weapon damage, plus 2 HP"
    criticalHitMultiplierModern = 1.5 ("explicitly not the Beta 1.8 behaviour")

Where it lives in 1.21.1, from the bytecode of Player.attack:
    343: ldc  1.5f
    345: fmul          <- the critical hit multiplier
    350: fadd          <- the enchantment bonus, added AFTER it
So the modern flat 1.5 is applied before enchantments, which is exactly the 1.6/1.7-era formula. Release 1.6.4
therefore already matches and needs nothing; the five profiles before it need the random multiplier.

WHAT IS IMPLEMENTED: the multiplier, replacing the flat 1.5 with a random value in [0, 1]. That is the headline
difference - damage varies from no bonus to double instead of a flat +50%.

WHAT IS NOT, and why it is stated rather than implied: the "+2 HP" and the "after enchantments" ordering. The +2
belongs at a second instruction, and the crit flag is not in scope at the enchantment call without knowing a local
ordinal, which is a guess this project has paid for. The ordering nuance only diverges when the weapon carries
Sharpness. Both are recorded in the mixin header so the gap is visible.
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

tweaks = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java'
text = tweaks.read_text(encoding='utf-8')
old = '''    public static boolean swordDamageBeforeReleaseOnePointZero(String versionId) {'''
new = '''    /**
     * Whether critical hits use the Beta 1.8 formula rather than the flat +50%.
     *
     * <p>The formula changed in the "1.6/1.7 era" per research/historical-features.json, so every declared profile
     * older than Release 1.6.4 is on the earlier one - and critical hits do not exist before Beta 1.8 at all, which
     * the {@code criticalHit} gate already enforces by never letting a pre-1.8 player crit.
     *
     * <p>Release 1.6.4 is the proxy for the change: no profile is declared between the era's start and it.
     */
    public static boolean betaCriticalHitFormula(String versionId) {
        return isDeclaredVersion(versionId)
            && !BEFORE_BETA_1_8.contains(versionId)
            && olderThan(versionId, RELEASE_1_6_4);
    }

    public static boolean swordDamageBeforeReleaseOnePointZero(String versionId) {'''
assert old in text, 'tweaks anchor not found'
tweaks.write_text(text.replace(old, new, 1), encoding='utf-8')
print('added betaCriticalHitFormula')

MIXIN = '''package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Rolls Beta 1.8's random critical hit instead of the flat +50%.
 *
 * <p>From Nostalgic Tweaks' subject matter - it has no critical hit FORMULA tweak, only DISABLE_CRITICAL_HIT, which
 * is ported separately - so this comes from research/historical-features.json, entry
 * {@code beta18-combat-critical-hits} (confidence: medium), which records the Beta 1.8 formula as
 * <b>"random +0%..+100% of weapon damage, plus 2 HP"</b> against a modern {@code criticalHitMultiplierModern} of
 * 1.5 that it describes as "explicitly not the Beta 1.8 behaviour".
 *
 * <h2>Where the constant is, and why that settles half the question</h2>
 *
 * <p>In 1.21.1's {@code Player.attack} the multiplier is {@code ldc 1.5f; fmul} and the enchantment bonus is added
 * immediately after with {@code fadd}. So modern applies the crit BEFORE enchantments - which is the 1.6/1.7-era
 * formula, not the Beta 1.8 one. That means Release 1.6.4 already matches the research and needs nothing, and the
 * five profiles before it need this.
 *
 * <h2>What is not here</h2>
 *
 * <p>The formula's <b>+2 HP</b> and its "after enchantments" ordering are not implemented. The +2 belongs at a
 * second instruction, and reaching the critical hit flag there needs a local ordinal - a guess, and this project
 * has already paid for two of those. The ordering only diverges when the weapon carries Sharpness. Both are
 * recorded as gaps rather than approximated silently.
 *
 * <p>A shared mixin: {@code Player.attack} runs on both sides, and the multiplier is read where the damage is
 * computed.
 */
@Mixin(Player.class)
public abstract class PlayerHistoricalCriticalHitMixin {
    @ModifyConstant(method = "attack", constant = @Constant(floatValue = 1.5F))
    private float historicalExperience$randomCriticalHit(float multiplier) {
        if (HistoricalGameplayTweaks.betaCriticalHitFormula(HistoricalActiveSession.current())) {
            Player self = (Player) (Object) this;
            return Mth.nextFloat(self.getRandom(), 0.0F, 1.0F);
        }
        return multiplier;
    }
}
'''
(root / 'src/main/java/net/historicalexperience/mixin/PlayerHistoricalCriticalHitMixin.java').write_text(
    MIXIN, encoding='utf-8')
print('wrote the crit formula mixin')

config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
if 'PlayerHistoricalCriticalHitMixin' not in document['mixins']:
    document['mixins'].append('PlayerHistoricalCriticalHitMixin')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered; shared mixins =', len(document['mixins']))
