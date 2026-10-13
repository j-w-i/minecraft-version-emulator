# Deploy the built mod jar into the PrismLauncher instance the owner actually plays.
#
# This exists because of a twelve-minute lie: for several rounds every fix I made was
# reported as "same issue", and the cause was that the instance loads a COPY of the jar
# from build/libs. My own client probes test the fresh build in run/, so they passed while
# the owner's game stayed on an old jar. Copying is now a step rather than an assumption.
#
# Usage:  pwsh -NoProfile -File scripts/deploy-to-prism.ps1

$ErrorActionPreference = 'Stop'
$source = Join-Path $PSScriptRoot '..\build\libs\historical-experience-1211-0.1.0.jar'
$target = Join-Path $env:APPDATA 'PrismLauncher\instances\1.21.1\minecraft\mods\historical-experience-1211-0.1.0.jar'

if (-not (Test-Path $source)) { throw "built jar not found at $source - run the Gradle build first" }
if (-not (Test-Path (Split-Path $target))) { throw "mods folder not found at $target" }

Copy-Item $source $target -Force
$sourceHash = (Get-FileHash $source).Hash
$targetHash = (Get-FileHash $target).Hash

if ($sourceHash -ne $targetHash) { throw 'copy did not take: hashes differ' }

Write-Host ("deployed {0} KB, sha256 {1}..." -f [math]::Round((Get-Item $target).Length / 1KB, 0), $targetHash.Substring(0, 16))
Write-Host 'Restart Minecraft completely - the mod is loaded at launch and a world reload will not pick it up.'
