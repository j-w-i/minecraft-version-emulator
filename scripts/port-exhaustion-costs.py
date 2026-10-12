"""Beta 1.8's exhaustion costs, three times the modern ones in two places.

research/historical-features.json, entry beta18-exhaustion-action-costs (confidence: high):
    exhaustion.sprintJump     = 0.8
    exhaustion.jumpNonSprint  = 0.2
    exhaustion.attack         = 0.3
    exhaustion.sprintPerMeter = 0.1

and its change list dates the sprint-jump value twice: Release 1.3.1 lowered it to 0.2, Release 1.7.2 raised it
back to 0.8. So on Release 1.6.4 the sprint jump already costs the modern 0.2, while the five earlier hunger
profiles should cost 0.8.

Targets, from javap on Player:
    public void attack(...)             -> causeFoodExhaustion with float 0.1f
    public void jumpFromGround()        -> causeFoodExhaustion with float 0.2f   (sprint jump)
                                        -> causeFoodExhaustion with float 0.05f  (non-sprint jump)

Sprint SPEED needs nothing: the research gives 5.612 m/s against a 4.317 m/s walk, which is exactly the modern
1.3 multiplier over the same walk speed, so the two agree.

Each constant is matched by VALUE rather than ordinal where the values differ (0.2 against 0.05), because a value
is a fact and an ordinal is a guess. Where a value might repeat - 0.1f inside attack - an ambiguous match is fatal
at transformation, and the probe now loads Player, so that failure is caught automatically rather than by a player.
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

MIXIN = '''package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Restores Beta 1.8's exhaustion costs, which are several times the modern ones.
 *
 * <p>From research/historical-features.json, entry {@code beta18-exhaustion-action-costs} (confidence: high):
 * attacking costs 0.3 rather than 0.1, and a jump while airborne costs 0.2 rather than 0.05. Nostalgic Tweaks has
 * no equivalent, so this is from the research.
 *
 * <p>The sprint jump is era-dependent and handled separately: the research dates it at 0.8 in Beta 1.8, 0.2 from
 * Release 1.3.1 and back to 0.8 from Release 1.7.2, so Release 1.6.4 - the only declared profile inside the 0.2
 * window - keeps the modern value while the five earlier hunger profiles get 0.8.
 *
 * <p>Constants are matched by value, not by ordinal, wherever the values distinguish them: a value is a fact about
 * the method and an ordinal is a count that has already been wrong once in this project. An ambiguous value is
 * fatal at transformation, and {@code Player} is one of the classes the generation probe now loads, so such a
 * mistake is caught by an automated run.
 *
 * <p>Gated on the hunger boundary: exhaustion only exists where a hunger meter does.
 *
 * <p>A shared mixin: exhaustion is server-side player state.
 */
@Mixin(Player.class)
public abstract class PlayerHistoricalExhaustionMixin {
    /** Upstream has no equivalent. Attacking: 0.3 instead of 0.1. */
    @ModifyConstant(method = "attack", constant = @Constant(floatValue = 0.1F))
    private float historicalExperience$attackExhaustion(float exhaustion) {
        return exhaustsLikeBeta() ? 0.3F : exhaustion;
    }

    /** Upstream has no equivalent. A non-sprint jump: 0.2 instead of 0.05. */
    @ModifyConstant(method = "jumpFromGround", constant = @Constant(floatValue = 0.05F))
    private float historicalExperience$jumpExhaustion(float exhaustion) {
        return exhaustsLikeBeta() ? 0.2F : exhaustion;
    }

    /** Upstream has no equivalent. A sprint jump: 0.8 before Release 1.6.4, modern from it. */
    @ModifyConstant(method = "jumpFromGround", constant = @Constant(floatValue = 0.2F))
    private float historicalExperience$sprintJumpExhaustion(float exhaustion) {
        return HistoricalGameplayTweaks.betaSprintJumpExhaustion(HistoricalActiveSession.current())
            ? 0.8F
            : exhaustion;
    }

    private static boolean exhaustsLikeBeta() {
        return HistoricalGameplayTweaks.betaExhaustionCosts(HistoricalActiveSession.current());
    }
}
'''
(root / 'src/main/java/net/historicalexperience/mixin/PlayerHistoricalExhaustionMixin.java').write_text(
    MIXIN, encoding='utf-8')
print('wrote the exhaustion mixin')

config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
if 'PlayerHistoricalExhaustionMixin' not in document['mixins']:
    document['mixins'].append('PlayerHistoricalExhaustionMixin')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered; shared mixins =', len(document['mixins']))

tweaks = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java'
text = tweaks.read_text(encoding='utf-8')
old = '''    public static boolean betaGoldenApple(String versionId) {'''
new = '''    /**
     * Whether exhaustion costs are the Beta 1.8 ones: attacking 0.3 rather than 0.1, and a non-sprint jump 0.2
     * rather than 0.05.
     *
     * <p>Gated on the hunger boundary, since exhaustion only exists with a hunger meter. The research does not date
     * when the modern attack and jump costs arrived, and every declared profile with hunger is Release 1.6.4 or
     * earlier, so all six are treated as Beta-era - the same deliberate assumption as the regeneration model.
     */
    public static boolean betaExhaustionCosts(String versionId) {
        return isDeclaredVersion(versionId) && !BEFORE_BETA_1_8.contains(versionId);
    }

    /**
     * Whether a sprint jump costs the Beta 1.8 value of 0.8 rather than 0.2.
     *
     * <p>The research dates this one exactly: 0.8 in Beta 1.8, lowered to 0.2 in Release 1.3.1, raised back to 0.8
     * in Release 1.7.2. Release 1.6.4 is the only declared profile inside the 0.2 window, and Release 1.6.4 is
     * also the proxy for both changes since no profile is declared between them.
     */
    public static boolean betaSprintJumpExhaustion(String versionId) {
        return betaExhaustionCosts(versionId) && olderThan(versionId, RELEASE_1_6_4);
    }

    public static boolean betaGoldenApple(String versionId) {'''
assert old in text, 'tweaks anchor not found'
tweaks.write_text(text.replace(old, new, 1), encoding='utf-8')
print('added the two exhaustion predicates')
