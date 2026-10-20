<#
.SYNOPSIS
    Verifies at runtime that a historical world has the build-limit envelope its build had.

.DESCRIPTION
    The audit item G8 (minecraft.wiki 12w07a raised the build limit from 128 to 256) is a data change: a new 0..256
    dimension type and an appended `_height_256` generation revision for Release 1.2.5 and later. Unit tests can only
    prove the JSON and the revision list; the engine's build limit is enforced at runtime, so this probe creates a
    fresh world from a world preset and asks the server itself where the world ends.

    RCON is used rather than a human at the console: it is Minecraft's own authenticated admin route, and the password
    is read out of the probe's own server.properties by scripts/rcon.py.

    The probe leaves run/heightprobe (the world, and heightprobe-server.log) as evidence, and restores
    run/server.properties from the backup it makes first. It does not touch any other world.

.PARAMETER LevelType
    World preset id to create the probe world from. Defaults to the Release 1.2.5 preset, which points at the
    `_height_256` revision.

.PARAMETER ExpectedMinY
    Lowest buildable y. Default 0.

.PARAMETER ExpectedMaxY
    Highest buildable y. Default 255. Passing 127 checks the pre-1.2 envelope.

.EXAMPLE
    ./scripts/verify-release-height.ps1

.EXAMPLE
    ./scripts/verify-release-height.ps1 -LevelType historical_experience:release_1_1 -ExpectedMaxY 127
#>
[CmdletBinding()]
param(
    [string] $LevelType = 'historical_experience:release_1_2_5',
    [int] $ExpectedMinY = 0,
    [int] $ExpectedMaxY = 255,
    [string] $WorldName = 'heightprobe',
    [int] $RconPort = 25575
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$properties = Join-Path $repo 'run/server.properties'
$backup = "$properties.bak"
$log = Join-Path $repo 'run/heightprobe-server.log'

if (-not (Test-Path $properties)) { throw "No run/server.properties; start the server once first." }
Copy-Item $properties $backup -Force
$world = Join-Path $repo "run/$WorldName"
if (Test-Path $world) { Remove-Item $world -Recurse -Force }

$text = Get-Content $properties -Raw
function Set-Key([string] $key, [string] $value) {
    if ($text -match "(?m)^$key=") { return ($text -replace "(?m)^$key=.*$", "$key=$value") }
    return ($text.TrimEnd() + "`n$key=$value`n")
}
$text = Set-Key 'level-name' $WorldName
$text = Set-Key 'level-type' $LevelType
$text = Set-Key 'enable-rcon' 'true'
$text = Set-Key 'rcon.password' 'probe'
$text = Set-Key 'rcon.port' "$RconPort"
$text = Set-Key 'online-mode' 'false'
Set-Content $properties $text -Encoding utf8

$job = Start-Job -ArgumentList $repo, $log -ScriptBlock {
    param($repo, $log)
    Set-Location $repo
    ./gradlew --offline runServer --console=plain 2>&1 | Tee-Object -FilePath $log
}
try {
    $ready = $false
    for ($i = 0; $i -lt 48; $i++) {
        Start-Sleep -Seconds 5
        $out = Receive-Job $job -Keep 2>&1 | Out-String
        if ($out -match 'Done \(') { $ready = $true; break }
    }
    if (-not $ready) { throw "Server did not reach 'Done' in time; see $log" }

    function Rcon([string] $command) {
        python "$repo/scripts/rcon.py" --server-properties $properties $command 2>&1 | Out-String
    }

    Rcon "forceload add 0 0" | Out-Null
    $minimum = [int] $ExpectedMinY
    $maximum = [int] $ExpectedMaxY
    $tooLow = $minimum - 1
    $tooHigh = $maximum + 1
    $results = [ordered]@{}
    foreach ($y in @($minimum, ($minimum + 1), $maximum, $tooHigh, $tooLow)) {
        $results[[string] $y] = (Rcon "setblock 0 $y 0 minecraft:stone").Trim()
    }
    Rcon "stop" | Out-Null

    $failed = @()
    foreach ($y in @($minimum, $maximum)) {
        if ($results[[string] $y] -notmatch 'Changed the block') {
            $failed += "y=$y should be buildable: $($results[[string] $y])"
        }
    }
    foreach ($y in @($tooHigh, $tooLow)) {
        if ($results[[string] $y] -match 'Changed the block') {
            $failed += "y=$y should be outside this world: $($results[[string] $y])"
        }
    }
    $results.GetEnumerator() | ForEach-Object { "y=$($_.Key): $($_.Value)" }
    if ($failed.Count -gt 0) { $failed | ForEach-Object { "FAIL: $_" }; exit 1 }
    Write-Host "PASS: $LevelType is $ExpectedMinY..$ExpectedMaxY (evidence in $log)"
} finally {
    for ($i = 0; $i -lt 24; $i++) { Start-Sleep -Seconds 3; if ($job.State -ne 'Running') { break } }
    Stop-Job $job -ErrorAction SilentlyContinue
    Remove-Job $job -Force -ErrorAction SilentlyContinue
    Copy-Item $backup $properties -Force
    Remove-Item $backup -Force -ErrorAction SilentlyContinue
}
