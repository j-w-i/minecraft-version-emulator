# Runtime-verifies every declared version that has not yet been loaded in a real world.
#
# A declared version is a data entry; whether it actually loads, applies its complete inherited preset
# and resolves its own identity is a separate question, and this project has twice found that a
# plausible data entry was wrong. Each case establishes a fresh world and reports the probe verdict.
#
# Runs sequentially on purpose: there is one Gradle tree, so there is one build coordinator.

$ErrorActionPreference = 'Continue'
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo

$cases = @(
    @{ Name = 'world-inf227';  Version = 'historical_experience:infdev_227';   Structures = $false },
    @{ Name = 'world-inf325';  Version = 'historical_experience:infdev_325';   Structures = $false },
    @{ Name = 'world-inf420';  Version = 'historical_experience:infdev_420';   Structures = $false },
    @{ Name = 'world-inf611';  Version = 'historical_experience:infdev_611';   Structures = $false },
    @{ Name = 'world-b19pre';  Version = 'historical_experience:beta_1_9_pre_3'; Structures = $true },
    @{ Name = 'world-r100';    Version = 'historical_experience:release_1_0_0'; Structures = $true },
    @{ Name = 'world-r110';    Version = 'historical_experience:release_1_1';  Structures = $true },
    @{ Name = 'world-r164';    Version = 'historical_experience:release_1_6_4'; Structures = $true }
)

$results = @()
foreach ($case in $cases) {
    $name = $case.Name
    $version = $case.Version
    Write-Host "=== $name ($version)"

    $prepareArgs = @('-NoProfile', '-File', 'scripts/prepare-probe-run.ps1',
        '-Mode', 'establish', '-WorldName', $name, '-Version', $version, '-ResetWorld')
    if ($case.Structures) { $prepareArgs += '-Structures' }
    & pwsh @prepareArgs *> "logs/continuation-sweep-prep-$name.log"
    $prepExit = $LASTEXITCODE

    & ./gradlew --console=plain runRuntimeProbe "-PprobeWorldName=$name" "-PprobeVersion=$version" `
        *> "logs/continuation-sweep-$name.log"
    $runExit = $LASTEXITCODE

    $verdictPath = "run/probe/$name/historical_experience_probe_verdict.txt"
    $verdict = 'no verdict file'
    if (Test-Path $verdictPath) {
        $line = Get-Content $verdictPath | Select-String 'PROBE_RESULT' | Select-Object -First 1
        if ($line) { $verdict = $line.Line.Trim() }
    }
    $resolved = 'unknown'
    $log = "logs/continuation-sweep-$name.log"
    if (Test-Path $log) {
        $r = Get-Content $log | Select-String 'CHECK version\.resolved=' | Select-Object -First 1
        if ($r) { $resolved = ($r.Line -split 'version.resolved=')[-1].Trim() }
    }

    $results += [pscustomobject]@{
        World = $name; Version = $version; PrepareExit = $prepExit; RunExit = $runExit
        Resolved = $resolved; Verdict = $verdict
    }
    Write-Host ("    prep={0} run={1} resolved={2} {3}" -f $prepExit, $runExit, $resolved, $verdict)
}

Write-Host ''
Write-Host '=== sweep summary ==='
$results | Format-Table -AutoSize | Out-String | Write-Host
$results | ConvertTo-Json -Depth 3 | Set-Content 'logs/continuation-sweep-summary.json'
Write-Host "summary written to logs/continuation-sweep-summary.json"
