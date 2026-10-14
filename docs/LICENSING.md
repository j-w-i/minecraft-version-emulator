# Licensing

This document is the project's own account of **which licence covers what**, **why that licence was chosen**
and **what has to be true before a build is published**. It is written to be checked against the repository, not
taken on trust. The notices themselves are in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md); the
file-by-file record of third-party material is in [`PROVENANCE.md`](PROVENANCE.md).

## 1. The project licence

**GNU Lesser General Public License, version 3 or later — SPDX `LGPL-3.0-or-later`.**

| File | Contents |
| --- | --- |
| [`LICENSE`](../LICENSE) | The official, unmodified GNU LGPL v3 text (7,652 bytes) |
| [`COPYING`](../COPYING) | The official, unmodified GNU GPL v3 text (35,149 bytes) |
| [`COPYING.LESSER`](../COPYING.LESSER) | The same GNU LGPL v3 text as `LICENSE`, under GNU's conventional name |

The LGPL v3 is a supplement to the GPL v3: it incorporates the GPL's terms and adds permissions. LGPL v3
section 4(b) requires a Combined Work to be accompanied by a copy of the GNU GPL *and* the GNU LGPL, which is why
all three files exist and why all three are packaged into the JAR and the sources JAR under `META-INF/`.

No file in this repository is MIT-licensed, and no file's licence header makes it so. The `tools/` directory
contains GitHub's `gh` CLI (MIT) as developer tooling, which is not part of the mod.

### Why `-or-later` for this project's own work

The rights holder's preference, stated for this task, is LGPL-3.0-or-later, and nothing in the project's own
contribution history prevents it. The election is stated in the per-file headers
(`SPDX-License-Identifier: LGPL-3.0-or-later`) and in `gradle.properties` (`mod_license`) and
`fabric.mod.json` (`license`).

### What the licence does *not* cover

The project licence covers **this project's own contributions**. It does not relicense anything else:

| Component | Governing licence | Covered by this project's LGPL-3.0-or-later? |
| --- | --- | --- |
| This project's own source | LGPL-3.0-or-later | Yes |
| Material copied/adapted from Nostalgic Tweaks | **LGPL-3.0-only** | **No** — remains `-only` |
| Legacy-Nether biome data | CC BY-SA 4.0 | No — and none is present at this revision; the port was removed |
| OldVillages village generation | MIT | No |
| Two Moderner Beta data files | MIT | No |
| `tools/` GitHub CLI | MIT | No |

Where a single file mixes the project's own work with adapted Nostalgic Tweaks material — which is most of the 95
ported files — the file's `SPDX-License-Identifier` is the compound expression
`LGPL-3.0-or-later AND LGPL-3.0-only`. That is the accurate statement: the file as a whole requires compliance
with both, and the adapted portions are not offered under `-or-later` by this project.

## 2. Why LGPL-3.0 is compatible with the incorporated code

**Nostalgic Tweaks is LGPL-3.0-only, and this project's own code is LGPL-3.0-or-later.** The two are compatible
in the direction that matters:

- LGPL-3.0-**only** material may be incorporated into a work that is offered under LGPL-3.0-**or-later**, because
  a recipient of the whole may take the work under LGPL-3.0 (the `-only` component's terms) and separately take
  the project's own contributions under LGPL-3.0 or any later version. No one is granted *more* permission over
  the adapted Nostalgic Tweaks portions than upstream granted.
- The reverse would not be acceptable, and the project does not do it: the adapted material is **not** offered
  under `-or-later` by this project, and no claim is made that Nostalgic Tweaks has been relicensed.
- Nothing here is `GPL-3.0-only` incorporating `LGPL-2.1-only`, or any other combination whose versions conflict.
  Both components are version 3, and LGPL v3 is defined as a supplement to GPL v3, so there is no version skew
  to resolve.

The project also **does not** claim that the LGPL-2.1 line, the GPL-2.0 line, or any other GNU version is
involved. Only version 3 is.

### Copying source is not linking

This project **copies** Nostalgic Tweaks source into its own source tree; it does not link against a Nostalgic
Tweaks library. The consequence is that LGPL-3.0 section 4's linking/replacement options (4(d)(1), the "suitable
shared library mechanism") are **not** an available shortcut here, because the adapted code is not a separate
library the user could replace. What applies is 4(d)(0) and section 6: **the Corresponding Source for the adapted
portions must be conveyed**, together with the notices and licence texts. That is what section 4 below addresses.

## 3. Open points, and how each was closed

These were the questions that could not be settled from the repository alone. Each is now either **decided**
(recorded with what the decision rests on) or **still open**. None is swept under a claim of full compliance,
and the reason each was ever in doubt is kept rather than deleted — the point of this section is that the next
person can see how the question was closed, not just that it was.

1. **Which licence governs Nostalgic Tweaks' source branches — DECIDED.**
   The finding that made this open stands: upstream's source branches (`1.21`, `1.21.4`, …) carry **no in-tree
   licence file**. The LGPL v3 text lives on the docs-only default branch `main`, added on 2026-06-01 by a commit
   that touches only `.github/`, `README.md` and `LICENSE.txt` on that branch, and before that the only licence
   ever recorded against a source-carrying branch was the proprietary **Minecraft Mod Public License
   v2.0.3-Modified**, deleted from source branches between 2022-10-10 and 2025-01-15 and stating that its author
   "may change the Mod license, but cannot do so retroactively".

   **The rights holder has confirmed the position on 2026-10-14: it is fine.** The adapted portions are treated
   as governed by **LGPL-3.0-only**, which is also how the rights holder distributes the mod (Modrinth reports
   `{"id": "LGPL-3.0-only"}`) and what the LGPL text published in the upstream repository states. This project's
   obligations are discharged against that licence and no other. Should that confirmation ever be withdrawn or
   contradicted, the question reopens and the Minecraft Mod Public License's section 7 terms (no revenue, credit
   to the Author, compliance with the Mod license) would apply to the adapted portions instead.

2. **Authorship of the 30 copied chest textures — DECIDED.**
   The owner confirmed on 2026-10-14 that the chest and ender-chest assets came **from Nostalgic Tweaks**. They
   are therefore Nostalgic Tweaks' own material and are covered by exactly the same **LGPL-3.0-only** analysis as
   the copied source: attribution in [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md), the licence texts
   shipped with the artefacts, and the Corresponding Source obligation. The bytes are unchanged apart from the
   namespace rewrite, and they are listed individually in [`PROVENANCE.md`](PROVENANCE.md).

   The earlier assertion in the notices that these were all original Mojang art remains **withdrawn** — it was
   unsupported, and the cube ender chest in particular is a Nostalgic Tweaks aesthetic rather than historical
   vanilla geometry. The record shows the assertion was wrong, because a future asset import that repeats the
   same reasoning would repeat the same error.

3. **OldVillages' copyright line — OPEN, minor.**
   Upstream declares `mod_license=MIT` in `gradle.properties` but ships no `LICENSE` file and states no copyright
   line. The notices record the authorship declaration that does exist (`mod_authors=MaxTechnik`) rather than
   inventing a notice, and reproduce MIT's permission text in full. This is a gap in upstream's hygiene, not
   something this project can close; MIT's permission text is included either way, which is what the licence
   requires.

4. **No upstream has been contacted about points 1 and 2** — both were settled by the owner, not by the Nostalgic
   Tweaks rights holder. Point 1's confirmation is the project owner's decision to proceed on the licence
   Nostalgic Tweaks publishes; it is not a statement from Adrenix. Recorded so the basis of the decision is not
   mistaken for correspondence.

5. **The licence work is not on the remote yet — OPEN, blocking for a release.**
   The repository is public, but at the time of writing the licensing changes are **uncommitted and unpushed**
   (the remote's `master` has no `LICENSE` file). Until they are pushed, a published binary would not satisfy
   LGPL-3.0 section 4/6 source availability, because the Corresponding Source at the published URL would not
   contain the licence or the notices. See "Before you publish a build", item 1.

## 4. Distribution obligations, and how each is met

### How this project is actually distributed

| Channel | Format | Status |
| --- | --- | --- |
| Git repository (`origin`) | Source | **Public** since 2026-10-14 (verified anonymously) |
| Local Gradle build | `build/libs/*.jar` (remapped) and `*-sources.jar` | Produced on the developer's machine |
| Mod-platform release (Modrinth / CurseForge) | Compiled JAR | Not yet published |
| Modpack inclusion | Compiled JAR | Not yet published |
| A library for other mods | — | Not applicable; this is not a library |

There is **no release process other than "run the Gradle build and upload what it produces"**, so the obligations
are met in the build itself — see `build.gradle`'s `jar`, `sourcesJar` and `verifyLicenceArtifacts` tasks.

### LGPL-3.0 section 4, item by item

| Obligation | How it is met |
| --- | --- |
| 4(a) prominent notice that the library is used and is covered by this licence | `META-INF/THIRD_PARTY_NOTICES.md` inside every JAR; the same statement is in `fabric.mod.json`'s `description` and in this repository's notices file |
| 4(b) accompany the work with the GNU GPL and the GNU LGPL | `META-INF/COPYING` (GPL-3.0) and `META-INF/COPYING.LESSER` + `META-INF/LICENSE` (LGPL-3.0), copied into both the JAR and the sources JAR by `build.gradle` |
| 4(c) include the library's copyright notice among runtime copyright displays, with a pointer to the texts | The mod displays no copyright notices during execution, so there is nothing to add to; `THIRD_PARTY_NOTICES.md` carries the notices and the pointers |
| 4(d)(0) convey the Minimal Corresponding Source and permit relinking | The **whole of this repository is the Corresponding Source** for the adapted portions, because the adapted code is compiled into the same mod rather than being a separate library. The `-sources.jar` is built from it, and a recipient may modify and rebuild with the standard Gradle build. Relinking is plain recompilation here; the mod is not linked against a Nostalgic Tweaks binary |
| 4(d)(1) shared-library mechanism | **Not used**, and deliberately not relied on: see "Copying source is not linking" above |
| 4(e) Installation Information | Only required where GPL section 6 would require it for a User Product. A Minecraft mod JAR is not a User Product, and this project distributes no hardware |
| 5 no additional restrictions on the LGPL-covered portions | This project imposes none. There is no EULA, no anti-reverse-engineering term, and no licence term anywhere in the repository that restricts modification of the adapted portions |

### Source availability

LGPL-3.0 requires that a recipient of the object code can get the Corresponding Source.
**https://github.com/j-w-i/minecraft-version-emulator is public** (confirmed 2026-10-14: the GitHub API
reports it anonymously over unauthenticated HTTP). It contains the whole of the Corresponding Source,
including the modified Nostalgic Tweaks portions, the licence texts and the notices.

**One condition remains: the licensing work must actually be pushed.** At the time of writing it is
uncommitted and unpushed, and the remote's `master` has no `LICENSE` file — so a binary published *today* would
point at a repository that does not yet carry the licence or notices. Push before publishing a build; the check
below is what makes the difference between "the repository exists" and "the repository carries the licence".

### Before you publish a build

1. **Push the licensing changes.** `git push` with `LICENSE`, `COPYING`, `COPYING.LESSER`,
   `THIRD_PARTY_NOTICES.md`, `docs/LICENSING.md`, `docs/PROVENANCE.md` and the source headers in the commit.
   Then confirm it, rather than assuming:

   ```
   ./gradlew verifyPublishedSource
   ```

   That task fetches the four required files from `raw.githubusercontent.com` **anonymously** — an
   authenticated request would pass on a private repository, which is the case it exists to catch. At the time
   of writing it reports `THIRD_PARTY_NOTICES.md` served (an older revision, 9,812 bytes) and
   `LICENSE`, `COPYING` and `COPYING.LESSER` as HTTP 404, because the licensing commit is not pushed yet. A
   sources JAR attached to each release also works, and already contains the licence texts and the notices, but
   a current repository is the simpler guarantee.
2. **Keep the notices with the download.** If the platform has a licence field, set it to `LGPL-3.0-or-later`
   for the project and link `THIRD_PARTY_NOTICES.md`. Do not claim a plain `LGPL-3.0-only` for the whole project,
   because the project's own code is `-or-later`; and do not claim `-or-later` for the adapted portions, because
   they are not.
3. **Run the checks.** `./gradlew build` covers the source-tree and artefact checks described below.
   `verifyPublishedSource` is the one check that needs the network and a push, which is why it is a separate
   task rather than part of `build`. Nothing else needs doing for the points decided in section 3.

## 5. Automated checks

The build fails when the licence structure breaks. Nothing here needs to be run by hand before a release: it
runs as part of `./gradlew build`.

- **`LicenceComplianceTest`** (`src/test/java/net/historicalexperience/licensing/`) checks the source tree:
  that `LICENSE`, `COPYING` and `COPYING.LESSER` exist and are still the FSF texts (matched on size, title and
  version line, so a replaced file cannot pass); that the declared identifiers in `gradle.properties`,
  `fabric.mod.json` and the source headers agree; that every Java file carries a header; that the count of
  Nostalgic Tweaks-derived files matches the `NT_FILES` list and the numbers stated in this document and in
  `THIRD_PARTY_NOTICES.md`; that the notices name the upstream project, its licence, its revision and where the
  Corresponding Source is; and that no active project guidance tells a reader to disregard licensing.
- **`verifyLicenceArtifacts`** (a `build.gradle` task, wired into `check`) opens the built JAR and sources JAR
  and fails if `META-INF/LICENSE`, `META-INF/COPYING`, `META-INF/COPYING.LESSER` or
  `META-INF/THIRD_PARTY_NOTICES.md` is missing, or if the manifest's `Bundle-License` disagrees with
  `mod_license`. Compliance is a property of the artefact, so it is checked on the artefact.
- **`verifyPublishedSource`** (a `build.gradle` task, run by hand) checks the *remote*: it derives the GitHub
  slug and branch from `origin` and fetches the four required files anonymously from
  `raw.githubusercontent.com`. It is deliberately not part of `build` — it needs the network and a pushed
  commit, and a build that fails offline would be worse than useless.
- **`scripts/apply-licence-headers.py --check`** reports any Java source whose licence header is missing,
  wrong, or stacked with a stale one.
- **`scripts/compare-nt-assets.py`** reports how this project's copied assets differ from the pinned Nostalgic
  Tweaks checkout (identical / namespace-only / other).
- **`scripts/verify-header-only-change.py`** proves the header migration changed no code: it strips the leading
  licence comments from the committed and working versions of every tracked Java file and compares the rest.
