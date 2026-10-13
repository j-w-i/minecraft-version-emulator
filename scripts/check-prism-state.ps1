# Check the owner's actual PrismLauncher instance: is the jar current, and did their last run fail?
#
# Written because my client probes launch the DEV instance in run/ - the same code, but not the same
# instance. That instance has its own mods folder and lacks the dozen other mods the owner runs, so it
# cannot answer "does it work in the game they play". This reads their side directly instead.
#
# Usage: pwsh -NoProfile -File scripts/check-prism-state.ps1

$ErrorActionPreference = 'Continue'
$instance = Join-Path $env:APPDATA 'PrismLauncher\instances\1.21.1\minecraft'
$built    = Join-Path $PSScriptRoot '..\build\libs\historical-experience-1211-0.1.0.jar'
$deployed = Join-Path $instance 'mods\historical-experience-1211-0.1.0.jar'
$log      = Join-Path $instance 'logs\latest.log'

Write-Host '=== jar freshness ==='
if ((Test-Path $built) -and (Test-Path $deployed)) {
    $a = Get-FileHash $built; $b = Get-FileHash $deployed
    Write-Host ("built    : {0}  {1}..." -f (Get-Item $built).LastWriteTime, $a.Hash.Substring(0,16))
    Write-Host ("deployed : {0}  {1}..." -f (Get-Item $deployed).LastWriteTime, $b.Hash.Substring(0,16))
    if ($a.Hash -eq $b.Hash) {
        Write-Host 'IN SYNC - the client will load the current build.'
    } else {
        Write-Host 'STALE - run scripts/deploy-to-prism.ps1 before believing any behaviour report.'
        Write-Host ("         built is {0:N0} minutes newer" -f ((Get-Item $built).LastWriteTime - (Get-Item $deployed).LastWriteTime).TotalMinutes)
    }
} else { Write-Host 'one of the jars is missing' }

Write-Host ''
Write-Host '=== their last run ==='
if (Test-Path $log) {
    Write-Host ("latest.log last written: {0}" -f (Get-Item $log).LastWriteTime)
    $failed = Select-String -Path $log -Pattern 'Failed to parse|Not a JSON object|prevented the world from loading|Unbound values|InjectionError|Network Protocol'
    if ($failed) {
        Write-Host 'FAILURES FOUND:'
        $failed | Select-Object -Last 8 | ForEach-Object { Write-Host ('  ' + $_.Line.Trim()) }
    } else {
        Write-Host 'no pack-parse, injection or protocol failures in that run'
    }
    $joined = Select-String -Path $log -Pattern 'joined the game' | Select-Object -Last 1
    if ($joined) { Write-Host ('reached a world: ' + $joined.Line.Trim()) }
} else { Write-Host 'no latest.log - the instance has not been run yet' }
