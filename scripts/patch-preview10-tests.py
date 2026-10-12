"""One-off: the newest beta_1_7_3 revision moved from preview_9 to preview_10, and four presets were added."""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]
tests = root / 'src/test/java/net/historicalexperience'

swaps = [
    (tests / 'world/upgrade/World1211GenerationPersistenceTest.java',
     '"historical_experience:beta_1_7_3_content_preview_9"',
     '"historical_experience:beta_1_7_3_content_preview_10"'),
    (tests / 'server/HistoricalUpgradeService1211Test.java',
     '"historical_experience:beta_1_7_3_content_preview_9"',
     '"historical_experience:beta_1_7_3_content_preview_10"'),
    (tests / 'generation/GenerationBackport1211Test.java',
     'assertEquals(31, reduced);',
     '// 31 before the four pre-Beta-1.8 revisions were added; each new preset carries the same envelope.\n'
     '        assertEquals(35, reduced);'),
]
for path, old, new in swaps:
    text = path.read_text(encoding='utf-8')
    assert old in text, 'anchor not found in ' + path.name
    path.write_text(text.replace(old, new, 1), encoding='utf-8')
    print('patched', path.name)
