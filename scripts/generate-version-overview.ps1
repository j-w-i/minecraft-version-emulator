# Generates the version selector's overview data from the project's own research.
#
# Source:  research/version-transitions.json   (what each build added, changed, removed, and did to world generation)
# Output:  src/main/resources/assets/historical_experience/version_overview.json
#
# Only the four lists the selector shows are carried over, keyed by the profile id the catalogue uses. The research
# file names some targets coarsely (beta_1_7, beta_1_8, release_1_2, release_1_6), so those are mapped to the build
# the project actually emulates. Nothing is invented: a version with no entry shows no overview rather than a guess.
#
# Run from the repository root:  pwsh -NoProfile -File scripts/generate-version-overview.ps1

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sourcePath = Join-Path $root 'research/version-transitions.json'
$outPath = Join-Path $root 'src/main/resources/assets/historical_experience/version_overview.json'

$profileFor = @{
    'beta_1_7' = 'beta_1_7_3'
    'beta_1_8' = 'beta_1_8_1'
    'release_1_2' = 'release_1_2_5'
    'release_1_6' = 'release_1_6_4'
}

$source = Get-Content $sourcePath -Raw -Encoding UTF8 | ConvertFrom-Json
$versions = [ordered]@{}
foreach ($version in $source.versions) {
    $name = if ($profileFor.ContainsKey($version.id)) { $profileFor[$version.id] } else { $version.id }
    $versions["historical_experience:$name"] = [ordered]@{
        introduced = @($version.introduced)
        changed = @($version.changed)
        removed = @($version.removed)
        worldgen = @($version.worldgen)
    }
}

$document = [ordered]@{
    schema = 1
    source = 'research/version-transitions.json'
    note = 'Not exhaustive: the research this comes from says so itself, and absence from a list is not evidence that a feature was absent.'
    versions = $versions
}

$json = $document | ConvertTo-Json -Depth 6
[System.IO.File]::WriteAllText($outPath, $json + "`n", (New-Object System.Text.UTF8Encoding($false)))
Write-Host "Wrote $($versions.Count) versions to $outPath"
