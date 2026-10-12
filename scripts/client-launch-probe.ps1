# Launches the development client into a historical world and captures evidence about whether it reaches
# the world, rather than waiting and guessing.
#
# What it collects, per the causal-chain requirement:
#   * the client's own log
#   * a JVM thread dump while the client is stalled, so background-thread network failures can be told
#     apart from a blocked main thread
#   * whether the world directory was actually touched, which is the ground truth for "did a world load"
#   * the Quick Play argument as the client received it
#
# Usage:
#   pwsh -NoProfile -File scripts/client-launch-probe.ps1 -Label baseline -NoFlashback -WaitSeconds 90
#   pwsh -NoProfile -File scripts/client-launch-probe.ps1 -Label withflashback -WaitSeconds 90

param(
    [Parameter(Mandatory = $true)][string] $Label,
    [switch] $NoFlashback,
    [int] $WaitSeconds = 90,
    [string] $World = 'hexflash'
)

$ErrorActionPreference = 'Continue'
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo

$log = "logs/continuation-client-$Label.log"
$dump = "logs/continuation-client-$Label-threads.txt"
$worldDir = "run/client/saves/$World"

# Stamp the world so "did it load" is answerable from file times alone.
$stamp = Join-Path $worldDir 'level.dat'
$beforeWrite = if (Test-Path $stamp) { (Get-Item $stamp).LastWriteTime } else { $null }
Write-Host "$Label : world level.dat before = $beforeWrite"

$gradleArgs = @('--console=plain', 'runClientQuickPlay')
if ($NoFlashback) { $gradleArgs += '-PnoFlashback=true' }

$client = Start-Process -FilePath './gradlew.bat' -ArgumentList $gradleArgs `
    -RedirectStandardOutput $log -RedirectStandardError "$log.err" -PassThru -NoNewWindow
Write-Host "$Label : gradle pid $($client.Id), waiting ${WaitSeconds}s"

Start-Sleep -Seconds $WaitSeconds

# Find the actual game JVM: the newest large java process.
$game = Get-Process java -ErrorAction SilentlyContinue |
    Where-Object { $_.StartTime -gt (Get-Date).AddSeconds(-($WaitSeconds + 60)) -and $_.WorkingSet64 -gt 200MB } |
    Sort-Object StartTime -Descending | Select-Object -First 1

"$Label : game jvm = $(if ($game) { "pid $($game.Id) rss $([math]::Round($game.WorkingSet64/1MB))MB cpu $([math]::Round($game.CPU,1))s" } else { 'not found' })" |
    Tee-Object -FilePath "$log.summary" -Append

if ($game) {
    & "$env:JAVA_HOME\bin\jstack.exe" $game.Id *> $dump
    Write-Host "$Label : thread dump -> $dump ($((Get-Item $dump).Length) bytes)"
}

$afterWrite = if (Test-Path $stamp) { (Get-Item $stamp).LastWriteTime } else { $null }
$worldTouched = ($beforeWrite -ne $afterWrite)

# Which threads are doing what, from the dump.
$mainState = 'unknown'
if (Test-Path $dump) {
    $lines = Get-Content $dump
    $mainIndex = ($lines | Select-String -Pattern '^"main"' | Select-Object -First 1).LineNumber
    if ($mainIndex) { $mainState = ($lines[$mainIndex] + ' | ' + $lines[$mainIndex + 1]).Trim() }
}

$summary = [pscustomobject]@{
    Label          = $Label
    NoFlashback    = [bool] $NoFlashback
    GamePid        = if ($game) { $game.Id } else { $null }
    WorldTouched   = $worldTouched
    LevelDatBefore = "$beforeWrite"
    LevelDatAfter  = "$afterWrite"
    MainThread     = $mainState
    LogBytes       = if (Test-Path $log) { (Get-Item $log).Length } else { 0 }
    DumpBytes      = if (Test-Path $dump) { (Get-Item $dump).Length } else { 0 }
}
$summary | ConvertTo-Json -Depth 3 | Tee-Object -FilePath "logs/continuation-client-$Label.json"

# Stop the client so runs do not overlap - GRACEFULLY.
#
# This used to force-kill every large java process the moment the wait window ended. Twice that landed while the
# game was mid-save and half-wrote the player data, which the owner then saw as "invalid player data" when
# opening the world. A window-close is sent first (taskkill without /F), the process is given up to 30 seconds to
# flush its save and exit on its own, and only then is force used.
$games = @(Get-Process java -ErrorAction SilentlyContinue |
    Where-Object { $_.StartTime -gt (Get-Date).AddSeconds(-($WaitSeconds + 60)) -and $_.WorkingSet64 -gt 200MB })

foreach ($game in $games) {
    Write-Host "$Label : asking the game process $($game.Id) to close so it can save"
    taskkill /PID $game.Id 2>$null | Out-Null
}

$deadline = (Get-Date).AddSeconds(30)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
    $stillUp = @($games | Where-Object { $_.Refresh(); -not $_.HasExited })
    if ($stillUp.Count -eq 0) { break }
}

foreach ($game in $games) {
    $game.Refresh()
    if (-not $game.HasExited) {
        Write-Host "$Label : game process $($game.Id) did not exit in 30s, forcing"
        Stop-Process -Id $game.Id -Force -ErrorAction SilentlyContinue
    }
}

if ($client -and -not $client.HasExited) {
    Start-Sleep -Seconds 3
    $client.Refresh()
    if (-not $client.HasExited) { Stop-Process -Id $client.Id -Force -ErrorAction SilentlyContinue }
}
Write-Host "$Label : done"
