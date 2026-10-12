"""Stop the sweep at the area-of-effect call, which is inside attack() and not inside sweepAttack().

Measured, not assumed: javap on Player shows the getEntitiesOfClass sweep loop inside `attack`, while
`sweepAttack()` is called separately and only plays the sound and particles. Cancelling sweepAttack therefore
removed the presentation and left the damage - exactly what the owner reported: "sweeping remains".

The redirect is on the getEntitiesOfClass call itself, so the loop iterates nothing when sweep is disabled.
That is surgical: it does not require identifying the boolean that gates the sweep, which is a local variable
and would need an ordinal guess.
"""
import pathlib
import re

root = pathlib.Path(__file__).resolve().parents[1]
log = root / 'logs/player.txt'
lines = log.read_text(encoding='utf-8', errors='replace').splitlines()

descriptor = None
for line in lines:
    if 'getEntitiesOfClass' in line and '//' in line:
        owner = line.split('//')[1].strip()
        # e.g. "InterfaceMethod net/minecraft/world/level/Level.getEntitiesOfClass:(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"
        match = re.match(r'(?:InterfaceMethod|Method)\s+([\w/$]+)\.(\w+):(\(.*\).+);', owner)
        if match:
            owner_class, name, desc = match.groups()
            descriptor = 'L%s;%s%s' % (owner_class, name, desc)
            break
assert descriptor, 'could not read the getEntitiesOfClass descriptor from ' + str(log)
print('descriptor:', descriptor)

mixin = root / 'src/main/java/net/historicalexperience/mixin/PlayerHistoricalSweepMixin.java'
mixin.write_text('''package net.historicalexperience.mixin;

import java.util.List;
import java.util.function.Predicate;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Removes the sweep attack's area damage, which no declared version had.
 *
 * <h2>Why this is a second mixin and not the first one fixed</h2>
 *
 * <p>The first attempt cancelled {@code Player.sweepAttack()}, on the reasonable-looking assumption that the
 * method named after the sweep performs it. It does not. Disassembling {@code Player} shows the
 * {@code getEntitiesOfClass} loop that damages nearby entities sitting inside <b>{@code attack}</b>, while
 * {@code sweepAttack()} is called separately and only plays the sound and the particles. So cancelling it
 * removed the presentation and left the damage, which is exactly what the owner saw: "sweeping remains".
 *
 * <p>The redirect is on the area-of-effect call itself, so the loop iterates nothing. That avoids having to
 * identify the local boolean that gates the sweep, which would mean guessing a local variable ordinal - the
 * kind of guess this project keeps having to undo.
 *
 * <p>Boundary: the sweep attack is 1.9 (15w34c) and the newest declared profile is Release 1.6.4, so it is
 * wrong in every one of the ten. The gate is the same tested table entry as the cooldown's.
 */
@Mixin(Player.class)
public abstract class PlayerHistoricalSweepMixin {
    @Redirect(method = "attack",
        at = @At(value = "INVOKE", target = "%s"))
    private List<Entity> historicalExperience$noSweepDamage(Level level, Class<Entity> type, AABB box,
            Predicate<? super Entity> predicate) {
        if (HistoricalGameplayTweaks.appliesTo("sweep", HistoricalActiveSession.current())) {
            net.historicalexperience.gameplay.HistoricalCombatDiagnostics.recordSweepCancel();
            return List.of();
        }
        return level.getEntitiesOfClass(type, box, predicate);
    }
}
''' % descriptor, encoding='utf-8')
print('wrote PlayerHistoricalSweepMixin')

import json
config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
if 'PlayerHistoricalSweepMixin' not in document['mixins']:
    document['mixins'].append('PlayerHistoricalSweepMixin')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered; mixins =', len(document['mixins']))
