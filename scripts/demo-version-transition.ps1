# Demonstrates a historical version transition in ONE live world, driven entirely through RCON.
#
# What it proves, in order:
#   1. a real server runs a historical world and reports its version, generation revision and rules;
#   2. a build placed in that world exists at known coordinates, and the region file holding it is hashed
#      with the server stopped - the region file is locked while the server runs, which is why an earlier
#      attempt at this script could not read it;
#   3. the operator command stages an upgrade to a different historical version;
#   4. the transition survives a full stop and restart, because the generator advance is applied at
#      shutdown - which is itself a measured finding, since doing it at startup was undone by the
#      server's own save;
#   5. after the restart the world reports the NEW version with its OWN rules, the build is still exactly
#      where it was, and the region file bytes are unchanged: the transition changed the recipe, not the
#      existing world.
#
# Usage: pwsh -NoProfile -File scripts/demo-version-transition.ps1
#            [-From historical_experience:beta_1_7_3] [-To historical_experience:release_1_2_5]

param(
    [string] $From = 'historical_experience:beta_1_7_3',
    [string] $To = 'historical_experience:release_1_2_5',
    [int] $MarkerX = 40,
    [int] $MarkerY = 70,
    [int] $MarkerZ = 40
)

$ErrorActionPreference = 'Continue'
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo

$world = 'world-transition'
$short = $To.Split(':')[1]
$log = "logs/demo-transition-$short.log"
$evidence = [System.Collections.Generic.List[string]]::new()
function Note([string] $line) {
    $evidence.Add($line)
    Write-Host $line
}

function Rcon([string] $command) {
    return ((python scripts/rcon.py $command 2>&1) | Out-String).Trim()
}

function Start-HistoricalServer([string] $suffix) {
    $process = Start-Process -FilePath './gradlew.bat' `
        -ArgumentList @('--console=plain', 'runHistoricalServer') `
        -RedirectStandardOutput "$log$suffix" -RedirectStandardError "$log$suffix.err" `
        -PassThru -NoNewWindow
    $deadline = (Get-Date).AddSeconds(180)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 5
        $reply = Rcon 'list'
        if ($reply -and $reply -notmatch 'Traceback|refused|Connection refused') { return $process }
        if ($process.HasExited) { break }
    }
    throw "the server never accepted RCON (suffix '$suffix')"
}

function Stop-HistoricalServer($process) {
    Rcon 'stop' | Out-Null
    $deadline = (Get-Date).AddSeconds(150)
    while ((Get-Date) -lt $deadline -and -not $process.HasExited) { Start-Sleep -Seconds 3 }
    if (-not $process.HasExited) {
        Note 'WARNING: the server did not stop cleanly; killing it'
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Seconds 3
}

function Get-RegionHashes {
    $root = "run/probe/$world/dimensions/minecraft/overworld/region"
    if (-not (Test-Path $root)) { return @{} }
    $hashes = @{}
    Get-ChildItem $root -Filter '*.mca' | ForEach-Object {
        $hashes[$_.Name] = (Get-FileHash $_.FullName -Algorithm SHA256).Hash
    }
    return $hashes
}

function Format-Hashes($hashes) {
    if ($hashes.Count -eq 0) { return '(no region files)' }
    return (($hashes.GetEnumerator() | Sort-Object Name | ForEach-Object { "$($_.Key)=$($_.Value.Substring(0,16))" }) -join ' ')
}

# ---- 1. establish the world on the FROM version ------------------------------------------------
pwsh -NoProfile -File scripts/prepare-probe-run.ps1 -Mode establish -WorldName $world -Version $From -ResetWorld *> "logs/demo-transition-prep.log"
Note "prepared '$world' on $From (exit $LASTEXITCODE)"

# ---- 2. run it, build something, and record the state ------------------------------------------
$server = Start-HistoricalServer '.phase1'
$statusBefore = Rcon 'hex status'
Note "STATUS BEFORE: $statusBefore"

Rcon 'forceload add 32 32 63 63' | Out-Null
Start-Sleep -Seconds 4
Note "build marker: $(Rcon "setblock $MarkerX $MarkerY $MarkerZ minecraft:gold_block")"
Rcon "setblock $($MarkerX + 1) $MarkerY $MarkerZ minecraft:gold_block" | Out-Null
Rcon "setblock $MarkerX $MarkerY $($MarkerZ + 1) minecraft:diamond_block" | Out-Null
Rcon 'save-all flush' | Out-Null
Start-Sleep -Seconds 4
Note "marker verified in world: $(Rcon "execute if block $MarkerX $MarkerY $MarkerZ minecraft:gold_block run seed")"

Stop-HistoricalServer $server
$hashBefore = Get-RegionHashes
Note "REGION HASH BEFORE: $(Format-Hashes $hashBefore)"

# ---- 3. stage the upgrade ----------------------------------------------------------------------
$server = Start-HistoricalServer '.phase2'
Note "upgrade reply: $(Rcon "hex upgrade $To")"
Note "STATUS AFTER STAGING: $(Rcon 'hex status')"
Stop-HistoricalServer $server

# ---- 4. restart: the advance is applied at shutdown --------------------------------------------
$server = Start-HistoricalServer '.phase3'
$statusAfter = Rcon 'hex status'
Note "STATUS AFTER RESTART: $statusAfter"
Note "marker after transition: $(Rcon "execute if block $MarkerX $MarkerY $MarkerZ minecraft:gold_block run seed")"
Note "diamond after transition: $(Rcon "execute if block $MarkerX $MarkerY $($MarkerZ + 1) minecraft:diamond_block run seed")"
Stop-HistoricalServer $server

$hashAfter = Get-RegionHashes
Note "REGION HASH AFTER:  $(Format-Hashes $hashAfter)"
$same = ($hashBefore.Count -eq $hashAfter.Count) -and
        (($hashBefore.Keys | Where-Object { $hashBefore[$_] -ne $hashAfter[$_] }).Count -eq 0)
Note "REGION FILES UNCHANGED BY THE TRANSITION: $same"

$flowed = $statusAfter -match [regex]::Escape($To.Split(':')[1])
Note "STATUS NAMES THE TARGET VERSION: $flowed"

$evidence | Set-Content "logs/demo-transition-$short.evidence.txt"
Write-Host ''
Write-Host "evidence: logs/demo-transition-$short.evidence.txt"
