"""Two fixes in one pass.

1. Fallout from the newest beta_1_7_3 revision moving from preview_9 to preview_10.
2. The sprint gate must consult the ACTIVE SESSION, not require a ServerLevel. Requiring a server level
   meant the client fell through and kept sprinting locally, so a player still sprinted, still saw the
   field-of-view change, and only the server disagreed with them - which is the worst of both.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]
tests = root / 'src/test/java/net/historicalexperience'

for name, path in [
    ('World1211GenerationPersistenceTest.java',
     tests / 'world/upgrade/World1211GenerationPersistenceTest.java'),
    ('HistoricalUpgradeService1211Test.java',
     tests / 'server/HistoricalUpgradeService1211Test.java'),
]:
    text = path.read_text(encoding='utf-8')
    old = '"historical_experience:beta_1_7_3_content_preview_9"'
    assert old in text, 'anchor not found in ' + name
    path.write_text(text.replace(old, '"historical_experience:beta_1_7_3_content_preview_10"', 1),
                    encoding='utf-8')
    print('patched', name)

envelope = tests / 'generation/GenerationBackport1211Test.java'
text = envelope.read_text(encoding='utf-8')
old = 'assertEquals(31, reduced);'
assert old in text
envelope.write_text(text.replace(
    old, '// 31 before the four pre-Beta-1.8 revisions were added; each carries the same envelope.\n'
         '        assertEquals(35, reduced);', 1), encoding='utf-8')
print('patched envelope count')

# The sprint gate.
sprint = root / 'src/main/java/net/historicalexperience/mixin/PlayerHistoricalSprintMixin.java'
text = sprint.read_text(encoding='utf-8')
old = '''        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        String profileId = HistoricalProfileLookup.profileIdForLevel(serverLevel);
        if (profileId != null && !HistoricalPlayerRules.allowsSprinting(profileId)) {'''
new = '''        // The ACTIVE SESSION, not the level. Requiring a ServerLevel meant the client took the early return
        // above and kept sprinting locally while the server refused, so the player still sprinted and still
        // saw the field-of-view change - the worst of both, and reported by the owner after loading a Beta
        // 1.7.3 world. HistoricalVersionSync sets this on both sides, which is exactly what a rule that has
        // to hold on both sides needs.
        String profileId = HistoricalActiveSession.current();
        if (profileId != null && !HistoricalPlayerRules.allowsSprinting(profileId)) {'''
assert old in text, 'sprint anchor not found'
text = text.replace(old, new, 1)
text = text.replace('import net.historicalexperience.gameplay.HistoricalProfileLookup;\n',
                    'import net.historicalexperience.gameplay.HistoricalActiveSession;\n')
text = text.replace('import net.minecraft.server.level.ServerLevel;\n', '')
sprint.write_text(text, encoding='utf-8')
print('patched sprint gate to the active session')
