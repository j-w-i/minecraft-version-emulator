# Deploy the built mod jar into the PrismLauncher instance the owner actually plays.
#
# This exists because of a twelve-minute lie: for several rounds every fix I made was
# reported as "same issue", and the cause was that the instance loads a COPY of the jar
# from build/libs. My own client probes test the fresh build in run/, so they passed while
# the owner's game stayed on an old jar. Copying is now a step rather than an assumption.
#
# ---------------------------------------------------------------------------------------
# WHY THIS WRITES A TEMPORARY FILE AND RENAMES IT INSTEAD OF USING Copy-Item
# ---------------------------------------------------------------------------------------
#
# Copy-Item onto an existing file opens it for writing, which TRUNCATES it to zero and then
# fills it in. For however long that takes - and the jar is ~630 KB, so it is measurable -
# a file of partial content exists at the live path in the mods folder. If Minecraft is
# already running, its class loader can read the file in that window, and the result is
# exactly what the owner hit:
#
#     java.util.zip.ZipException: ZipFile invalid LOC header (bad signature)
#     Failed to load class file for 'net.historicalexperience.server.HistoricalUpgradeService'!
#
# Note the shape of that failure, because it is why this was hard to recognise: the mod
# STARTED. Classes early in the archive were read before the write reached them, so the mod
# initialised, a world loaded, commands registered - and then a class late in the archive
# could not be read, so `hex upgrade` failed with "an unexpected error occurred" and the
# Manage World button crashed. A half-written jar does not look like a corrupt jar; it looks
# like specific features being broken.
#
# So the file at the live path is now only ever replaced by a RENAME, which is atomic on the
# same volume: readers see either the whole old file or the whole new one, never a hybrid.
# The temporary file is written beside the target so the rename cannot cross a volume.
#
# ---------------------------------------------------------------------------------------
# WHY THERE IS NO "IS MINECRAFT RUNNING" CHECK
# ---------------------------------------------------------------------------------------
#
# An earlier revision of this script refused to deploy while ANY java process existed. That
# was wrong twice over: this project always has Gradle daemons alive, which never touch the
# mods folder, so the check would cry wolf on almost every deploy; and a process NAME is not
# evidence about a FILE.
#
# The atomic rename makes the check unnecessary, and better. If a running game genuinely has
# the jar open, Windows refuses the rename with a sharing violation and nothing is written -
# the failure is by construction, and reported. If nothing has it open, the rename succeeds
# and the deploy is safe whatever else is running. The operation itself is the test, so the
# guard cannot disagree with reality the way a name-based one can.
#
# Usage:  pwsh -NoProfile -File scripts/deploy-to-prism.ps1

$ErrorActionPreference = 'Stop'
$source = Join-Path $PSScriptRoot '..\build\libs\historical-experience-1211-0.1.0.jar'
$target = Join-Path $env:APPDATA 'PrismLauncher\instances\1.21.1\minecraft\mods\historical-experience-1211-0.1.0.jar'
$targetDir = Split-Path $target
# Same directory as the target, so the rename is same-volume and therefore atomic.
$staging = Join-Path $targetDir 'historical-experience-1211-0.1.0.jar.incoming'

if (-not (Test-Path $source)) { throw "built jar not found at $source - run the Gradle build first" }
if (-not (Test-Path $targetDir)) { throw "mods folder not found at $targetDir" }

Add-Type -AssemblyName System.IO.Compression.FileSystem

# Verify the build output is itself a readable archive BEFORE it goes anywhere near the live
# path. A truncated build would otherwise be deployed as faithfully as a good one.
$sourceHash = (Get-FileHash $source -Algorithm SHA256).Hash
try {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($source)
    $entryCount = $zip.Entries.Count
    $zip.Dispose()
} catch {
    throw "the built jar is not a readable zip archive, refusing to deploy: $($_.Exception.Message)"
}

if (Test-Path $staging) { Remove-Item $staging -Force }

# Copy to the staging path first; the live path is not touched until this returns.
Copy-Item $source $staging -Force
$stagedHash = (Get-FileHash $staging -Algorithm SHA256).Hash
if ($stagedHash -ne $sourceHash) {
    Remove-Item $staging -Force -ErrorAction SilentlyContinue
    throw "staged copy did not take: hashes differ ($sourceHash vs $stagedHash)"
}

# The atomic step, and the only one that touches the live path. On the same volume a rename
# replaces the destination in a single operation, so no partial file is ever visible there.
try {
    Move-Item $staging $target -Force
} catch {
    Remove-Item $staging -Force -ErrorAction SilentlyContinue
    throw ("could not replace the jar - it is likely open in a running game. Nothing was " +
        "written to the live path, so the existing jar is untouched. Close Minecraft fully " +
        "and run this again. Underlying error: $($_.Exception.Message)")
}

$targetHash = (Get-FileHash $target -Algorithm SHA256).Hash
if ($targetHash -ne $sourceHash) {
    throw "rename did not produce the expected file: hashes differ ($sourceHash vs $targetHash)"
}

# And prove the LIVE file is a readable archive, not merely byte-identical. The hash proves
# the bytes match the build; this proves the archive those bytes describe can actually be
# opened, which is the property the class loader needs.
try {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($target)
    $liveCount = $zip.Entries.Count
    $zip.Dispose()
} catch {
    throw "the deployed jar is not a readable zip archive: $($_.Exception.Message)"
}
if ($liveCount -ne $entryCount) {
    throw "the deployed jar has $liveCount entries but the build has $entryCount"
}

Write-Host ("deployed {0} KB, sha256 {1}..., {2} entries, archive verified readable" -f `
    [math]::Round((Get-Item $target).Length / 1KB, 0), $targetHash.Substring(0, 16), $liveCount)
Write-Host 'Restart Minecraft completely - the mod is loaded at launch and a world reload will not pick it up.'
