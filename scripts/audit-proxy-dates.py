"""Find block dates that disagree with the evidence written next to them.

Written because the block veto made dates consequential. Until enforcement existed, a date was documentation and a
wrong one cost nothing. Now every date decides whether a block is written into generated terrain, so a date that
names one version while its own evidence names another is a live defect in one of two directions:

  * dated too LATE  -> the block is vetoed in builds that really had it (empty dungeons, missing snow)
  * dated too EARLY -> the block appears in builds that did not have it yet (clay, sugar cane)

The spawner entry was the first kind: it named `alpha` as a proxy for "after Infdev 20100625-1917", which was true
while alpha was the only version past that boundary and became a false claim the moment Alpha 1.0.1 was declared
between them.

This reads the GENERATED table rather than the generator's source, because the generated `introduce(...)` calls are
structured and the generator's own TIMELINE list is not.
"""
import pathlib
import re

TABLE = pathlib.Path('src/main/java/net/historicalexperience/generation/HistoricalBlockAvailability.java')
text = TABLE.read_text(encoding='utf-8')

# introduce("version", timeline, "evidence", new String[] { "block", ... });
CALL = re.compile(
    r'introduce\(\s*"([^"]+)"\s*,\s*(true|false)\s*,\s*"((?:[^"\\]|\\.)*)"\s*,\s*'
    r'new String\[\]\s*\{([^}]*)\}\s*\)',
    re.S)

VERSION_IN_EVIDENCE = re.compile(
    r'\b(Alpha v?1\.\d+(?:\.\d+)?|Beta 1\.\d+(?:\.\d+)?|Release 1\.\d+(?:\.\d+)?|'
    r'Infdev \d{8}(?:-\d+)?|Indev \d{8}(?:-\d+)?|Classic [\d.a-z_]+|Beta 1\.9 Prerelease ?\d*)\b')

# Where each declared version sits historically, by the name its evidence would use.
DECLARED = {
    'historical_experience:infdev_420': 'Infdev 20100420',
    'historical_experience:infdev_611': 'Infdev 20100611',
    'historical_experience:alpha_1_0_1': 'Alpha v1.0.1',
    'historical_experience:alpha_1_0_4': 'Alpha v1.0.4',
    'historical_experience:alpha': 'Alpha v1.1.2_01',
    'historical_experience:beta_1_7_3': 'Beta 1.7.3',
    'historical_experience:beta_1_8_1': 'Beta 1.8.1',
    'historical_experience:beta_1_9_pre_3': 'Beta 1.9 Prerelease 3',
    'historical_experience:release_1_0_0': 'Release 1.0.0',
    'historical_experience:release_1_1': 'Release 1.1',
    'historical_experience:release_1_2_5': 'Release 1.2.5',
    'historical_experience:release_1_6_4': 'Release 1.6.4',
}

findings = []

for match in CALL.finditer(text):
    version = match.group(1)
    evidence = match.group(3)
    blocks = re.findall(r'"([^"]+)"', match.group(4) or '')
    mentioned = VERSION_IN_EVIDENCE.findall(evidence)

    if not mentioned:
        continue

    # Does the evidence name a build that is not the version this entry is dated to?
    named = mentioned[0]
    dated = DECLARED.get(version, version)

    if named.lower().replace(' ', '') not in dated.lower().replace(' ', ''):
        findings.append((version, dated, named, blocks, evidence[:150]))

print('entries whose evidence names a different build than the one they are dated to: %d' % len(findings))
print()
for version, dated, named, blocks, evidence in findings:
    print('dated to %-38s evidence says %s' % (version, named))
    print('    blocks  : %s' % ', '.join(blocks[:6]))
    print('    evidence: %s' % evidence)
    print()
