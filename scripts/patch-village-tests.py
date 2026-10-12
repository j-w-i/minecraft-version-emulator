"""One-off: point the structure tests at the corrected allow-list (our village, not vanilla's)."""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]
profile = root / 'src/test/java/net/historicalexperience/generation/ProfileGenerationRules1211Test.java'
text = profile.read_text(encoding='utf-8')

old = ('        assertEquals(Set.of("minecraft:mineshaft", "minecraft:stronghold", "minecraft:village_plains",\n'
       '            "minecraft:village_desert"), beta181);')
new = ('        // The version\'s villages are this project\'s own structure; the vanilla jigsaw village ids are\n'
       '        // suppressed in its favour and are therefore NOT in any allowed set.\n'
       '        assertEquals(Set.of("minecraft:mineshaft", "minecraft:stronghold",\n'
       '            HistoricalStructureRules.HISTORICAL_VILLAGE), beta181);')
assert old in text, 'expected allowed-set assertion not found'
text = text.replace(old, new, 1)

old2 = 'assertTrue(HistoricalStructureRules.VANILLA_STRUCTURE_IDS.contains(allowedId),'
new2 = ('assertTrue(HistoricalStructureRules.VANILLA_STRUCTURE_IDS.contains(allowedId)\n'
        '                        || HistoricalStructureRules.HISTORICAL_VILLAGE.equals(allowedId),')
assert old2 in text, 'allowed-id assertion not found'
text = text.replace(old2, new2, 1)
profile.write_text(text, encoding='utf-8')
print('patched', profile.name)
