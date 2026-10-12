"""Correct the sweep redirect: the loop uses the two-argument getEntitiesOfClass, and my regex ate a semicolon.

Two mistakes, both from generating the mixin instead of reading what I generated:
  * the descriptor regex consumed the terminating ';' of the return type, so the target was malformed and
    remapJar refused it ("is neither field descriptor nor method descriptor");
  * the call is getEntitiesOfClass(Class, AABB) without a Predicate, so the handler had one parameter too
    many.
"""
import pathlib

path = (pathlib.Path(__file__).resolve().parents[1]
        / 'src/main/java/net/historicalexperience/mixin/PlayerHistoricalSweepMixin.java')
text = path.read_text(encoding='utf-8')

old_target = 'target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List")'
new_target = 'target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;")'
assert old_target in text, 'target anchor not found'
text = text.replace(old_target, new_target, 1)

old_handler = '''    private List<Entity> historicalExperience$noSweepDamage(Level level, Class<Entity> type, AABB box,
            Predicate<? super Entity> predicate) {
        if (HistoricalGameplayTweaks.appliesTo("sweep", HistoricalActiveSession.current())) {
            net.historicalexperience.gameplay.HistoricalCombatDiagnostics.recordSweepCancel();
            return List.of();
        }
        return level.getEntitiesOfClass(type, box, predicate);
    }'''
new_handler = '''    private List<Entity> historicalExperience$noSweepDamage(Level level, Class<Entity> type, AABB box) {
        if (HistoricalGameplayTweaks.appliesTo("sweep", HistoricalActiveSession.current())) {
            net.historicalexperience.gameplay.HistoricalCombatDiagnostics.recordSweepCancel();
            return List.of();
        }
        return level.getEntitiesOfClass(type, box);
    }'''
assert old_handler in text, 'handler anchor not found'
text = text.replace(old_handler, new_handler, 1)
text = text.replace('import java.util.function.Predicate;\n', '')
path.write_text(text, encoding='utf-8')
print('corrected the sweep redirect')
