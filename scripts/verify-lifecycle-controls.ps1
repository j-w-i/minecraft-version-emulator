<#
.SYNOPSIS
Runs real-server negative controls in unique project-local worlds, preserving probe configuration.
.DESCRIPTION
Requires an already authorised run/probe/eula.txt and a successfully established historical
metadata document. Never accepts the EULA, resets worlds, or touches personal Minecraft data.
Run only when no other Gradle build or probe uses this workspace. Retains worlds/logs for review.
The probe deliberately rejects ordinary worlds; expected nonzero exits alone are NOT success.
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
