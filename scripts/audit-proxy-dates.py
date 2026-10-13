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

def normalise(text):
    """Reduce a version name or id to comparable letters and digits.

    "Alpha v1.0.6" and "alpha_1_0_6" both become "alpha106"; "Beta 1.7.3" and "beta_1_7_3" both become "beta173".
    Derived rather than looked up in a hand-written map, because the map went stale the moment a new build was
    declared and reported a false positive on the very entry it was supposed to clear.
    """
    lowered = re.sub(r'\b(v|V)(?=\d)', '', str(text))
    return re.sub(r'[^a-z0-9]', '', lowered.lower())


findings = []

for match in CALL.finditer(text):
    version = match.group(1)
    evidence = match.group(3)
    blocks = re.findall(r'"([^"]+)"', match.group(4) or '')
    mentioned = VERSION_IN_EVIDENCE.findall(evidence)

    if not mentioned:
        continue

    # Does the evidence name a build that is not the one this entry is dated to?
    named = mentioned[0]

    if normalise(named) not in normalise(version):
        findings.append((version, named, blocks, evidence[:150]))

print('entries whose evidence names a different build than the one they are dated to: %d' % len(findings))
print()
for version, named, blocks, evidence in findings:
    print('dated to %-38s evidence says %s' % (version, named))
    print('    blocks  : %s' % ', '.join(blocks[:6]))
    print('    evidence: %s' % evidence)
    print()
