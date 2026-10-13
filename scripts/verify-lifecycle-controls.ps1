<#
.SYNOPSIS
Runs real-server negative controls in unique project-local worlds, preserving probe configuration.
.DESCRIPTION
Requires an already authorised run/probe/eula.txt and a successfully established historical
metadata document. Never accepts the EULA, resets worlds, or touches personal Minecraft data.
Run only when no other Gradle build or probe uses this workspace. Retains worlds/logs for review.
The probe deliberately rejects ordinary worlds; expected nonzero exits alone are NOT success.

.NOTES
*** NOT RUNNABLE AS WRITTEN - REPAIR REQUIRED BEFORE TRUSTING IT. ***

The three SCENARIOS below are the project's fail-closed safety controls and are worth keeping: an ordinary world
must be left alone (LEAVE_ORDINARY), unreadable historical metadata must fail closed, and valid metadata with no
overworld marker must fail closed. What is dead is the MECHANISM, inherited from the deleted 26.3 tree:

  * line ~73 runs `runRuntimeProbe`, a Gradle task that no longer exists. The working equivalent is
    `runServer -PgenerationRunDir=run/probe -PgenerationProbe=true`.
  * it reads `historical_experience_probe_verdict.txt`, which the current probe does not write. The current
    artifact is `run/probe/<world>/generation-probe.json`, and the marker decision it should assert on is the
    `Historical marker decision for ...: action=... reason=...` log line, which every run prints.
  * its exit-code expectation (`$commandExit -eq 1`) is a 26.3 assumption. With `runServer`, a LEAVE_ORDINARY
    case exits 0 because the probe halts the server normally, while only the FAIL_CLOSED cases exit nonzero - so
    the expectation has to become per-case before this can pass.

This marker exists because an unrunnable verification script is worse than no script: it looks like coverage.
`scripts/verify-matrix-runtime.ps1` is the working sweep and does run.
#>
[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
$runDir = Join-Path $projectRoot 'run\probe'
$propertiesPath = Join-Path $runDir 'server.properties'
$sourceMetadata = Join-Path $runDir 'world\historical_experience_metadata.json'
$eulaPath = Join-Path $runDir 'eula.txt'
if (-not (Test-Path -LiteralPath $propertiesPath -PathType Leaf)) { throw 'Prepare run/probe first' }
if (-not (Test-Path -LiteralPath $sourceMetadata -PathType Leaf)) { throw 'Establish the historical baseline first' }
if (-not (Test-Path -LiteralPath $eulaPath -PathType Leaf) -or
    -not (Select-String -LiteralPath $eulaPath -Pattern '^eula=true\s*$' -Quiet)) {
    throw 'This control never accepts the EULA; run/probe must already be explicitly authorised'
}
$runId = (Get-Date -Format 'yyyyMMddTHHmmssfff') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8)
$evidenceDir = Join-Path $projectRoot "logs\lifecycle-$runId"
New-Item -ItemType Directory -Path $evidenceDir | Out-Null
$propertiesBackup = Join-Path $evidenceDir 'server.properties.backup'
Copy-Item -LiteralPath $propertiesPath -Destination $propertiesBackup
$baselineHash = (Get-FileHash -LiteralPath $sourceMetadata -Algorithm SHA256).Hash
$verdictPath = Join-Path $runDir 'world\historical_experience_probe_verdict.txt'
if (Test-Path -LiteralPath $verdictPath -PathType Leaf) {
    Copy-Item -LiteralPath $verdictPath -Destination (Join-Path $evidenceDir 'baseline-verdict.txt')
}
$results = [System.Collections.Generic.List[object]]::new()
$cases = @(
    @{ Name='ordinary'; LevelType='minecraft:normal'; ExpectedAction='LEAVE_ORDINARY'; Reason='metadata did not load as a historical document: Normal'; Metadata='none' },
    @{ Name='corrupt'; LevelType='historical_experience:beta_1_7_3'; ExpectedAction='FAIL_CLOSED'; Reason='historical metadata is unreadable'; Metadata='corrupt' },
    @{ Name='missing-marker'; LevelType='minecraft:normal'; ExpectedAction='FAIL_CLOSED'; Reason='overworld level carries no'; Metadata='valid' }
)
try {
    Push-Location $projectRoot
    try {
        foreach ($case in $cases) {
            $worldName = "lifecycle-$runId-$($case.Name)"
            $worldDir = Join-Path $runDir $worldName
            if (Test-Path -LiteralPath $worldDir) { throw "Refusing to reuse $worldDir" }
            New-Item -ItemType Directory -Path $worldDir | Out-Null
            $metadataPath = Join-Path $worldDir 'historical_experience_metadata.json'
            if ($case.Metadata -eq 'valid') {
                Copy-Item -LiteralPath $sourceMetadata -Destination $metadataPath
            } elseif ($case.Metadata -eq 'corrupt') {
                Set-Content -LiteralPath $metadataPath -Value '{deliberately invalid metadata' -Encoding utf8
            }
            $beforeHash = if ($case.Metadata -ne 'none') {
                (Get-FileHash -LiteralPath $metadataPath -Algorithm SHA256).Hash
            } else { $null }
            @(
                "level-name=$worldName",
                "level-type=$($case.LevelType)",
                'level-seed=2508291534120694762',
                'online-mode=false',
                'server-ip=127.0.0.1',
                'server-port=25599',
                'enable-rcon=false',
                'enable-query=false',
                'management-server-enabled=false',
                'max-players=1',
                'spawn-protection=0',
                'sync-chunk-writes=true'
            ) | Set-Content -LiteralPath $propertiesPath -Encoding utf8
            $logPath = Join-Path $evidenceDir "$($case.Name).log"
            & .\gradlew.bat --console=plain runRuntimeProbe *> $logPath
            $commandExit = $LASTEXITCODE
            $actionSeen = Select-String -LiteralPath $logPath -Pattern "action=$($case.ExpectedAction) metadataPresent=" -Quiet
            $reasonSeen = $case.Reason -eq '' -or
                (Select-String -LiteralPath $logPath -SimpleMatch -Pattern $case.Reason -Quiet)
            $preserved = if ($case.Metadata -eq 'none') {
                -not (Test-Path -LiteralPath $metadataPath)
            } else {
                (Test-Path -LiteralPath $metadataPath -PathType Leaf) -and
                ((Get-FileHash -LiteralPath $metadataPath -Algorithm SHA256).Hash -eq $beforeHash)
            }
            $passed = $commandExit -eq 1 -and $actionSeen -and $reasonSeen -and $preserved
            $results.Add([ordered]@{
                case=$case.Name; passed=$passed; exitCode=$commandExit;
                expectedAction=$case.ExpectedAction; actionSeen=$actionSeen; reasonSeen=$reasonSeen;
                metadataPreserved=$preserved; metadataHashBefore=$beforeHash;
                worldDirectory=$worldDir; log=$logPath
            })
            Write-Host "$($case.Name): passed=$passed exit=$commandExit action=$actionSeen preservation=$preserved"
        }
    } finally { Pop-Location }
} finally {
    # Restore only the exact original project fixture, not any personal settings or worlds.
    Copy-Item -LiteralPath $propertiesBackup -Destination $propertiesPath -Force
    $baselinePreserved = (Get-FileHash -LiteralPath $sourceMetadata -Algorithm SHA256).Hash -eq $baselineHash
    [ordered]@{
        runId=$runId; baselineMetadataPreserved=$baselinePreserved;
        cases=@($results.ToArray()); evidenceDirectory=$evidenceDir
    } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $evidenceDir 'results.json') -Encoding utf8
}
if (-not $baselinePreserved -or $results.Count -ne $cases.Count -or
    @($results | Where-Object { -not $_.passed }).Count -gt 0) {
    throw "Lifecycle controls failed; inspect $evidenceDir"
}
Write-Host "All $($cases.Count) real-server controls passed. Evidence: $evidenceDir"
