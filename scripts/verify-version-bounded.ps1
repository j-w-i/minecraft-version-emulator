# Runtime-verifies ONE declared version, with a hard timeout.
#
# A version whose world never finishes loading hangs the server BEFORE the probe's own watchdog arms, so
# the sweep had to be killed by hand. Here the run is bounded from outside: the Gradle task is started,
# given a deadline, and killed if no verdict appears. A hang then costs the deadline instead of the
# round.
#
# Usage: pwsh -NoProfile -File scripts/verify-version-bounded.ps1 -Version historical_experience:infdev_420 [-Structures] [-TimeoutSeconds 150]

param(
    [Parameter(Mandatory = $true)][string] $Version,
    [switch] $Structures,
    [int] $TimeoutSeconds = 150
)

$ErrorActionPreference = 'Continue'
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo

$path = $Version.Split(':')[1]
$world = "world-v-$path"
$caseLog = "logs/continuation-bounded-$path.log"
$verdictPath = "run/probe/$world/historical_experience_probe_verdict.txt"

if (Test-Path $verdictPath) { Remove-Item $verdictPath -Force }

$prepareArgs = @('-NoProfile', '-File', 'scripts/prepare-probe-run.ps1',
    '-Mode', 'establish', '-WorldName', $world, '-Version', $Version, '-ResetWorld')
if ($Structures) { $prepareArgs += '-Structures' }
& pwsh @prepareArgs *> "logs/continuation-bounded-prep-$path.log"
Write-Host "prepared $world (exit $LASTEXITCODE) for $Version"

$gradle = Start-Process -FilePath './gradlew.bat' `
    -ArgumentList @('--console=plain', 'runRuntimeProbe', "-PprobeWorldName=$world", "-PprobeVersion=$Version") `
    -RedirectStandardOutput $caseLog -RedirectStandardError "$caseLog.err" -PassThru -NoNewWindow

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$verdict = $null
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 5
    if (Test-Path $verdictPath) {
        $line = Get-Content $verdictPath | Select-String 'PROBE_RESULT' | Select-Object -First 1
        if ($line) { $verdict = $line.Line.Trim(); break }
    }
    if ($gradle.HasExited) { break }
}

$timedOut = $false
if (-not $verdict) {
    if (-not $gradle.HasExited) {
        $timedOut = $true
        Write-Host "no verdict within ${TimeoutSeconds}s - killing the run (this is a hang, not a failure)"
        Stop-Process -Id $gradle.Id -Force -ErrorAction SilentlyContinue
        Get-Process java -ErrorAction SilentlyContinue |
            Where-Object { $_.StartTime -gt (Get-Date).AddMinutes(-10) -and $_.WorkingSet64 -gt 200MB } |
            ForEach-Object { Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue }
    }
}

$resolved = 'unknown'
if (Test-Path $caseLog) {
    $r = Get-Content $caseLog | Select-String 'CHECK version\.resolved=' | Select-Object -First 1
    if ($r) { $resolved = ($r.Line -split 'version.resolved=')[-1].Trim() }
}
$preset = 'none'
if (Test-Path $caseLog) {
    $p = Get-Content $caseLog | Select-String 'CHECK preset\.application=' | Select-Object -First 1
    if ($p) { $preset = ($p.Line -split 'CHECK preset.application=')[-1].Trim() }
}

Write-Host ''
Write-Host "=== result for $Version ==="
Write-Host "world      : $world"
Write-Host "timedOut   : $timedOut"
Write-Host "resolved   : $resolved"
Write-Host "verdict    : $verdict"
Write-Host "preset     : $preset"

[pscustomobject]@{
    Version = $Version; World = $world; TimedOut = $timedOut
    Resolved = $resolved; Verdict = $verdict; Preset = $preset
} | ConvertTo-Json -Depth 3 | Set-Content "logs/continuation-bounded-$path.json"
