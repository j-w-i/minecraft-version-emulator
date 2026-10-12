"""Close the last level-based gate call from a mixin, and make the guard cover every mixin.

The audit found exactly one remaining: AnimalHistoricalBreedingMixin calls animalBreedingDisabled(animal.level()).
Breeding runs in server-side goal AI, so it is not broken today - but it is the same shape as the two bugs that
were broken, and "this one is server-only" is a judgement that stops being true silently. Asking the session
costs nothing and removes the judgement.

The test then bans the whole pattern rather than checking one file, because checking one file is what let the
second instance survive the first fix.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

mixin = root / 'src/main/java/net/historicalexperience/mixin/AnimalHistoricalBreedingMixin.java'
text = mixin.read_text(encoding='utf-8')
old = 'if (HistoricalGameplayTweaks.animalBreedingDisabled(animal.level())) {'
new = ('// The session, not the level: this rule is server-side AI today, but a level-based question is the exact\n'
       '        // shape that silently switched sprinting and sword blocking off on the client, and "this one is\n'
       '        // server-only" is a judgement that can stop being true without anyone noticing.\n'
       '        if (HistoricalGameplayTweaks.appliesTo("breeding", HistoricalActiveSession.current())) {')
assert old in text, 'breeding gate anchor not found'
text = text.replace(old, new, 1)
if 'import net.historicalexperience.gameplay.HistoricalActiveSession;' not in text:
    text = text.replace('import net.historicalexperience.gameplay.HistoricalGameplayTweaks;',
                        'import net.historicalexperience.gameplay.HistoricalActiveSession;\n'
                        'import net.historicalexperience.gameplay.HistoricalGameplayTweaks;', 1)
mixin.write_text(text, encoding='utf-8')
print('breeding mixin now asks the session')

test = root / 'src/test/java/net/historicalexperience/gameplay/HistoricalClientSideGateTest.java'
text = test.read_text(encoding='utf-8')
addition = '''
    @Test
    void noMixinAsksAVersionGateAboutALevel() throws Exception {
        java.util.List<String> offenders = new java.util.ArrayList<>();
        Path mixins = Path.of("src/main/java/net/historicalexperience/mixin");

        try (java.util.stream.Stream<Path> files = Files.list(mixins)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("(?:Disabled|Enabled)\\\\(\\\\s*[\\\\w.]+\\\\.level\\\\(\\\\)")
                    .matcher(source);

                if (matcher.find()) {
                    offenders.add(file.getFileName().toString());
                }
            }
        }

        assertTrue(offenders.isEmpty(),
            "a mixin asks a version gate about a level, which answers false on the client and switches the "
                + "behaviour off on the side that draws it: " + offenders);
    }
}'''
index = text.rstrip().rfind('}')
text = text[:index] + addition + '\n'
test.write_text(text, encoding='utf-8')
print('test now bans level-based gate calls in every mixin')
