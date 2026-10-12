"""Read-only parity audit of 26.3 generation sources/data versus the 1.21.1 port."""
import difflib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OLD = ROOT / 'src/main'
NEW = ROOT / 'src/main'


def differences(a, b, key=''):
    if isinstance(a, dict) and isinstance(b, dict):
        for k in sorted(a.keys() | b.keys()):
            yield from differences(a.get(k), b.get(k), key + '/' + k)
    elif a != b:
        yield {'key': key, 'old': a, 'new': b}


def main():
    report = {'sources': [], 'data': []}
    java = OLD / 'java/net/historicalexperience'
    sources = set()
    for folder in ['generation', 'integration/modernerbeta', 'version', 'profile', 'world']:
        sources.update((java / folder).rglob('*.java'))
    for relative in ['HistoricalMarker.java', 'HistoricalExperience.java',
            'gameplay/HistoricalSpawnRules.java', 'gameplay/HistoricalSpawnFilter.java',
            'mixin/FeaturePlacerHistoricalContentMixin.java', 'mixin/ChunkGeneratorHistoricalOreMixin.java',
            'mixin/TreeDecoratorContextHistoricalSuppressionMixin.java',
            'mixin/StructureHistoricalSuppressionMixin.java', 'mixin/NaturalSpawnerHistoricalSpawnMixin.java']:
        sources.add(java / relative)
    for src in sorted(sources):
        relative = src.relative_to(OLD)
        dest = NEW / relative
        a = src.read_text(encoding='utf-8')
        b = dest.read_text(encoding='utf-8') if dest.exists() else ''
        # Word boundaries are essential: HistoricalIdentifier is our own unchanged value type.
        normalized = re.sub(r'\bIdentifier\b', 'ResourceLocation', a).replace('.identifier()', '.location()')
        report['sources'].append({'path': str(relative), 'present': dest.exists(),
            'exact': a == b, 'mappingNormalizedEqual': normalized == b,
            'diff': list(difflib.unified_diff(normalized.splitlines(), b.splitlines(), n=2))
                    if normalized != b else []})
    for src in sorted((OLD / 'resources/data').rglob('*.json')):
        relative = src.relative_to(OLD)
        dest = NEW / relative
        a = json.loads(src.read_text(encoding='utf-8'))
        b = json.loads(dest.read_text(encoding='utf-8')) if dest.exists() else None
        report['data'].append({'path': str(relative), 'present': dest.exists(),
                              'differences': list(differences(a, b))})
    report['allGenerationSourcesPresent'] = all(s['present'] for s in report['sources'])
    out = ROOT / 'logs/sol-1211-parity-audit.json'
    out.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print('Source files:', len(report['sources']), 'all present:', report['allGenerationSourcesPresent'])
    for s in report['sources']:
        if not s['mappingNormalizedEqual']:
            print('API/source delta:', s['path'], 'diff lines:', len(s['diff']))
    for d in report['data']:
        if d['differences']:
            print(d['path'])
            for delta in d['differences']:
                # Avoid dumping a complete copied noise/router document into the console summary.
                print(' ', delta if d['present'] else {'key': delta['key'], 'old': '<reference-only data>', 'new': '<replaced by supported component>'})
    print('Full audit:', out)
    if not report['allGenerationSourcesPresent']:
        raise SystemExit('A reference generation source is missing from the backport')


if __name__ == '__main__':
    main()
