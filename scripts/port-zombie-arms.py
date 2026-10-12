"""Port OLD_ZOMBIE_ARMS, with a gate that carries its own boundary.

Carrier: animation/mob_arms/AbstractZombieModelMixin, two @ModifyArg on the animateZombieArms call inside setupAnim.

Boundary (research/feature-timeline.json): "Zombies' armor has been removed, as well as their arm swinging attack
animations" - Indev 20100202. Every declared profile is later than that date (the oldest is Infdev 20100420), so
the swinging arms are wrong in all ten. The gate is therefore true for every declared version, which is why it is
added to the same case arm as cooldown/sweep/swim - but under its OWN name and with its OWN citation, because the
enabling fact is a different one and a future reader must be able to check it independently.
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

MIXIN = '''package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.client.model.AbstractZombieModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Stops a zombie raising and swinging its arms, which it did not do in these versions.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/animation/mob_arms/AbstractZombieModelMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code AnimationTweak.OLD_ZOMBIE_ARMS}. Both argument
 * modifications, their indices and the target call are upstream's.
 *
 * <h2>Why this one IS ported while the crit particle was not</h2>
 *
 * <p>Both are default-true upstream client flags, and the difference is evidence. The research records that
 * zombie arm swinging was REMOVED in <b>Indev 20100202</b> - "Zombies' armor has been removed, as well as their
 * arm swinging attack animations" - and every declared profile is later than that date, the oldest being Infdev
 * 20100420. So the swinging arms are modern behaviour in all ten profiles. The crit particle, by contrast, is
 * older than six of the profiles, which is why that flag stayed out. A flag being on by default says nothing
 * either way; the version the behaviour changed is what decides.
 *
 * <h2>What each argument does</h2>
 *
 * <p>{@code isAggressive} makes the arms rise when the zombie notices a target, and {@code attackTime} drives the
 * swing. Upstream sets the first false and the second to zero, which leaves the classic outstretched arms in
 * every state rather than only when idle.
 *
 * <p>This is a CLIENT mixin, registered under {@code client}.
 */
@Mixin(AbstractZombieModel.class)
public abstract class AbstractZombieModelHistoricalArmsMixin {
    /** Upstream: {@code nt_mob_arms$setIsZombieAggressive}. */
    @ModifyArg(
        index = 2,
        method = "setupAnim(Lnet/minecraft/world/entity/monster/Monster;FFFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/AnimationUtils;animateZombieArms("
                + "Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;ZFF)V"))
    private boolean historicalExperience$setIsZombieAggressive(boolean isAggressive) {
        if (HistoricalGameplayTweaks.appliesTo("mobArms", HistoricalActiveSession.current())) {
            return false;
        }
        return isAggressive;
    }

    /** Upstream: {@code nt_mob_arms$setZombieAttackTime}. */
    @ModifyArg(
        index = 3,
        method = "setupAnim(Lnet/minecraft/world/entity/monster/Monster;FFFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/AnimationUtils;animateZombieArms("
                + "Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;ZFF)V"))
    private float historicalExperience$setZombieAttackTime(float attackTime) {
        if (HistoricalGameplayTweaks.appliesTo("mobArms", HistoricalActiveSession.current())) {
            return 0.0F;
        }
        return attackTime;
    }
}
'''
(root / 'src/main/java/net/historicalexperience/mixin/AbstractZombieModelHistoricalArmsMixin.java').write_text(
    MIXIN, encoding='utf-8')
print('wrote the zombie arms mixin')

config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
document.setdefault('client', [])
if 'AbstractZombieModelHistoricalArmsMixin' not in document['client']:
    document['client'].append('AbstractZombieModelHistoricalArmsMixin')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered; client mixins =', len(document['client']))

tweaks = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java'
text = tweaks.read_text(encoding='utf-8')

old = '            case "cooldown", "sweep", "swim" -> isDeclaredVersion(versionId);'
assert old in text, 'appliesTo anchor not found'
text = text.replace(old, '            case "cooldown", "sweep", "swim" -> isDeclaredVersion(versionId);\n'
    '            // True for every declared version for its OWN reason, not because it shares an era with the\n'
    '            // line above: zombie arm swinging was removed in Indev 20100202, earlier than every profile.\n'
    '            case "mobArms" -> isDeclaredVersion(versionId);', 1)

old = '"customDeathLoot", "itemPickup", "monsterEnchant"'
assert old in text, 'gateNames anchor not found'
text = text.replace(old, '"customDeathLoot", "itemPickup", "mobArms", "monsterEnchant"', 1)

old = '        Map.entry("customDeathLoot",'
assert old in text, 'evidence anchor not found'
text = text.replace(old, '''        Map.entry("mobArms",
            "Zombie arm-swinging attack animations were REMOVED in Indev 20100202, and every declared profile "
                + "is later (the oldest is Infdev 20100420), so the swinging arms are modern behaviour in all "
                + "ten. Upstream tweak: OLD_ZOMBIE_ARMS."),
        Map.entry("customDeathLoot",''', 1)
tweaks.write_text(text, encoding='utf-8')
print('added the mobArms gate, name and evidence')

test = root / 'src/test/java/net/historicalexperience/gameplay/HistoricalGameplayTweaksTest.java'
text = test.read_text(encoding='utf-8')
old = 'List.of("cooldown", "sweep", "swim");'
assert old in text, 'test list anchor not found'
test.write_text(text.replace(old, 'List.of("cooldown", "sweep", "swim", "mobArms");', 1), encoding='utf-8')
print('test boundary list updated')
