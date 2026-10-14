# Minecraft Historical Experience

A Fabric mod for Minecraft **1.21.1** that emulates historical Minecraft: pre-Classic through Release 1.7.10,
plus Release 1.8.9 as a requested target. It generates historical terrain, dates generated content per
historical version, and gates gameplay rules to the version a world declares.

Historical terrain comes from **[Moderner Beta](https://github.com/BlueStaggo/moderner-beta)** as a pinned
dependency; this project does not reimplement terrain generation. Content rules — which blocks, structures,
mobs, recipes and mechanics a given build had — are this project's own, driven by the research data in
[`research/`](research/).

## Licence

**GNU Lesser General Public License, version 3 or later** (`LGPL-3.0-or-later`).

| | |
| --- | --- |
| [`LICENSE`](LICENSE) | The GNU LGPL v3 text |
| [`COPYING`](COPYING) | The GNU GPL v3 text, which the LGPL incorporates |
| [`COPYING.LESSER`](COPYING.LESSER) | The same LGPL v3 text under GNU's conventional name |

This project **incorporates source code copied and adapted from
[Nostalgic Tweaks](https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks)**, which is **LGPL-3.0-only**. Those
portions are not relicensed by this project and remain `LGPL-3.0-only`. Where a file mixes this project's own
work with adapted Nostalgic Tweaks material, its `SPDX-License-Identifier` is the compound expression
`LGPL-3.0-or-later AND LGPL-3.0-only`.

- **[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)** — every third-party work incorporated, linked, fetched
  or distributed, with its licence and the obligations that follow.
- **[`docs/LICENSING.md`](docs/LICENSING.md)** — the full licensing position: what covers what, why the chosen
  licence is compatible with the incorporated code, how each LGPL-3.0 distribution obligation is met, which
  questions were open and how each was closed, and what must happen before a public release.
- **[`docs/PROVENANCE.md`](docs/PROVENANCE.md)** — the file-by-file record of where each third-party portion
  came from and what was changed.

If you are distributing this mod, read `docs/LICENSING.md` first. In particular, a published binary needs the
Corresponding Source to be reachable, and **a notice alone does not satisfy that**. The repository is public
and the build packages the licence texts and notices into both JARs; `./gradlew verifyPublishedSource` confirms
the published repository actually serves them.

## Building

Java 21 is required, or a JDK Gradle can run on (Java 25 is what this checkout is developed against).

```
./gradlew build
```

The built mod JAR and its sources JAR appear in `build/libs/`. The `sourcesJar` is the Corresponding Source for
the LGPL-covered material and carries the licence texts and notices under `META-INF/`.

Run the tests, including the licensing checks:

```
./gradlew test
```

See [`docs/TESTING.md`](docs/TESTING.md) for the test levels, the probes, and what has and has not been
verified at runtime.

## Documentation

| Document | Contents |
| --- | --- |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Module layout and invariants |
| [`docs/LICENSING.md`](docs/LICENSING.md) | Licensing position and obligations |
| [`docs/PROVENANCE.md`](docs/PROVENANCE.md) | File-by-file third-party provenance |
| [`docs/TESTING.md`](docs/TESTING.md) | Test levels, commands and evidence |
| [`docs/GAMEPLAY_RULES.md`](docs/GAMEPLAY_RULES.md) | Per-version gameplay rules |
| [`docs/HISTORICAL_GENERATION_CONTENT.md`](docs/HISTORICAL_GENERATION_CONTENT.md) | Generation-content rules |
| [`docs/ADDING-A-VERSION.md`](docs/ADDING-A-VERSION.md) | How to add a historical profile |
| [`AGENTS.md`](AGENTS.md) | Working contract, including the third-party code policy |

## Contributing

By contributing you agree that your contribution may be distributed under this project's licence
(`LGPL-3.0-or-later`). Do not add code, assets or data from another project without recording its licence and
provenance as [`AGENTS.md`](AGENTS.md)'s Third-Party Code and Licensing Policy describes; the build's
`LicenceComplianceTest` will fail if a source file arrives without a licence header.

## Status

Under active development. Nothing here is production-ready, and the runtime evidence behind each player-facing
claim is tracked in [`PROGRESS.md`](PROGRESS.md) and [`docs/TESTING.md`](docs/TESTING.md).
