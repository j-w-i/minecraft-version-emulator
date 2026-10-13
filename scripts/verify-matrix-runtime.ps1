# Runtime-verifies EVERY declared version in the matrix, one fresh world each.
#
# Why this exists: a declared version is a data entry, and whether it actually loads, applies its inherited
# preset, resolves its own identity and generates without anachronisms is a separate question that this project
# has repeatedly found wrong. The previous attempt at this script was a 26.3 artifact - it named
# `infdev_227`/`infdev_325`, called a `runRuntimeProbe` task and read a `historical_experience_probe_verdict.txt`
# that no longer exist anywhere in the tree - so it could not run at all and the matrix had never been swept.
#
# The version list is DERIVED, not typed: it comes from the world-preset tag the menu itself is built from, so a
# newly declared version is swept automatically and a typo cannot silently drop one from the sweep.
#
# Sequentially, on purpose: there is one Gradle tree, so there is one build coordinator.

param([int] $PerVersionTimeoutSeconds = 180, [string[]] $OnlyVersions = @())

$ErrorActionPreference = 'Continue'
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.4.1'

# Reads the run's own log rather than asking the process object, because the process object is the thing that
# lied. A missing log is not a finished run, so that answers false instead of throwing.
function Test-LogSaysBuildFinished([string] $path) {
    if (-not (Test-Path $path)) { return $false }
    return [bool](Select-String -Path $path -Pattern '^BUILD (SUCCESSFUL|FAILED)' -Quiet -ErrorAction SilentlyContinue)
}

# Separate from the above on purpose. "The build ended" and "the build passed" are different questions, and
# collapsing them is what let a launch that died with InvalidMixinException be reported as exit=0.
function Test-LogSaysBuildSucceeded([string] $path) {
    if (-not (Test-Path $path)) { return $false }
    return [bool](Select-String -Path $path -Pattern '^BUILD SUCCESSFUL' -Quiet -ErrorAction SilentlyContinue)
}

# The third question, and the one that actually catches a broken mixin: Gradle prints BUILD SUCCESSFUL for a server
# that failed during bootstrap, because the forked process exits 0. These are the server's own words.
function Test-LogShowsServerStartupFailure([string] $path) {
    if (-not (Test-Path $path)) { return $false }
    return [bool](Select-String -Path $path -Quiet -ErrorAction SilentlyContinue -Pattern @(
        'Failed to start the minecraft server',
        'Mixin apply for mod .* failed',
        'InvalidMixinException',
        'Mixin transformation of .* failed'
    ))
}

# Positive evidence that the server RAN, which outranks any failure-looking text. A version that reached "Done" or
# printed its own verification line demonstrably started, so no string in its log can make it a startup failure.
#
# This exists because the first version of the failure detector produced `exit=1` beside `success=True` for alpha:
# the message scan is evidently sensitive to when it is asked, and rather than guess at that, the conclusion it
# draws is now gated on evidence that cannot be misread. A tool that reports a contradiction in its own summary is
# worse than one that reports nothing.
function Test-LogShowsServerStarted([string] $path) {
    if (-not (Test-Path $path)) { return $false }
    return [bool](Select-String -Path $path -Quiet -ErrorAction SilentlyContinue -Pattern @(
        'Done \(.*\)! For help, type',
        'GENERATION_VERIFICATION'
    ))
}

# A probe verdict counts only if it was written after this version's run began. A verdict from an earlier run is
# indistinguishable from a fresh one by content alone - that is the whole problem - so the file's own timestamp is
# what decides. Ticks are compared rather than DateTime values so no unit confusion is possible.
function Test-ProbeVerdictIsFresh([string] $path, [datetime] $since) {
    if (-not (Test-Path $path)) { return $false }
    return (Get-Item $path).LastWriteTimeUtc.Ticks -ge $since.ToUniversalTime().Ticks
}

$tagPath = 'src/main/resources/data/minecraft/tags/worldgen/world_preset/normal.json'
if (-not (Test-Path $tagPath)) { throw "matrix tag not found at $tagPath" }
$tag = Get-Content $tagPath -Raw | ConvertFrom-Json
$versions = $tag.values | ForEach-Object { $_.ToString() } | Sort-Object
# `pwsh -File` passes a comma-separated value as ONE literal argument, so accept both forms: split on commas here
# rather than depending on how the caller's shell quoted it. The first attempt at a partial re-run silently swept
# zero versions because of exactly that, and a sweep that reports success while running nothing is the worst
# possible outcome for a verification script.
if ($OnlyVersions.Count -gt 0) {
    $wanted = @($OnlyVersions | ForEach-Object { $_ -split ',' } | Where-Object { $_ -ne '' })
    $versions = @($versions | Where-Object { $wanted -contains $_ })
    if ($versions.Count -eq 0) { throw "no declared version matched: $($wanted -join ', ')" }
}
Write-Host "sweeping $($versions.Count) declared versions from the menu tag"

$properties = 'run/probe/server.properties'
$results = @()

foreach ($version in $versions) {
    $short = $version.Split(':')[-1]
    $world = "sol-matrix-$short"
    # Timed from here, because "how long does one version actually take" was the question this sweep could not
    # answer: it once reported a version as a 180-second hang while the run had finished in 30s, and round 69 had to
    # reconstruct the real cost by hand from log timestamps. A verification tool that cannot report its own cost
    # cannot be used to plan, so each row now carries its measured wall time.
    $versionStarted = Get-Date
    Write-Host "=== $short"

    # DELETE the world directory first. The first version of this sweep only set a new level-name, and for any
    # version whose name had been used by an earlier manual probe the server loaded the EXISTING world and the
    # probe re-sampled chunks generated by an older build - which is how beta_1_6 reported deepslate 8366 and
    # emerald ore 422, numbers identical to a pre-fix measurement taken hours earlier. Stale evidence that looks
    # like a fresh finding is the exact failure this project keeps being caught by, so freshness is enforced here
    # rather than assumed from the world name.
    $worldDir = "run/probe/$world"
    if (Test-Path $worldDir) { Remove-Item -Recurse -Force $worldDir }

    $lines = Get-Content $properties
    $lines = $lines -replace '^level-name=.*', "level-name=$world"
    $lines = $lines -replace '^level-type=.*', "level-type=$($version -replace ':','\:')"
    $lines = $lines -replace '^generate-structures=.*', 'generate-structures=true'
    Set-Content $properties ($lines -join "`r`n") -NoNewline

    # DELETE the log as well as the world, for the same reason the world is deleted: completion is now judged
    # from these files, and a log left behind by an earlier run of the same version still says BUILD SUCCESSFUL.
    # Without this the very first check of the run passes against the PREVIOUS run's output and the version is
    # reported finished before its server has started. Caught by this script's own first post-fix run, which
    # wrote runExit=-1 for three versions that had in fact all succeeded.
    $log = "logs/matrix-sweep-$short.txt"
    Remove-Item $log, "$log.err" -Force -ErrorAction SilentlyContinue

    # BOUNDED FROM OUTSIDE, deliberately. A version whose world never finishes loading hangs the server before any
    # in-process watchdog can arm - a Classic world did exactly that here and had to be killed by hand - so a
    # single bad version would otherwise stall the whole sweep with no evidence about which one it was. This idea
    # came from verify-version-bounded.ps1, which was wired to the dead 26.3 probe and could not run; the useful
    # part is kept and the dead part is gone.
    #
    # WHAT IS *NOT* TRUSTED HERE, AND WHY. This loop used to wait on `$proc.HasExited`. That test never fired: the
    # redirected log said `BUILD SUCCESSFUL in 28s` and the probe verdict beside it said success=true, and the
    # sweep still reported "no verdict within 180s - killing the run (a hang, not a failure)" and burned the whole
    # timeout. Three versions of that cost the owner six minutes, and the sweep's own message blamed the mod. The
    # mod was never hung - the run finished in thirty seconds, twice, in front of a stopwatch. What stays true for
    # minutes after the work is done is a handle on the redirected pipe, held by a JVM that outlives the wrapper.
    #
    # So completion is now judged from ARTIFACTS the run writes, never from the process object:
    #   * "GENERATION_VERIFICATION" in the log - the probe printed its verdict;
    #   * the probe JSON exists - the verdict is readable;
    #   * "BUILD SUCCESSFUL"/"BUILD FAILED" - the run is over even if it wrote no verdict at all.
    # A run that produces none of those within the timeout is a genuine hang and is still reported as one.
    $probePath = "run/probe/$world/generation-probe.json"
    # Timestamp the moment before the run, so a verdict left behind by an EARLIER run cannot be mistaken for this
    # one. This is the same class of defect as the stale log fixed in round 69 and it produced the same symptom:
    # alpha was reported as `exit=1 success=True` - a contradiction - because the world delete had not taken effect
    # (a leftover lock), the previous run's probe JSON was still sitting there, the wait loop broke on its first
    # poll, and the row then read its success from that stale file. Deleting the directory is best-effort; requiring
    # the verdict to be NEWER than the run start is not.
    $runStarted = Get-Date
    $proc = Start-Process -FilePath './gradlew.bat' -PassThru -NoNewWindow `
        -ArgumentList @('--console=plain', 'runServer', '-PgenerationRunDir=run/probe',
            '-PgenerationProbe=true', "-PprobeToken=matrix-$short", '-PprobeRadius=1') `
        -RedirectStandardOutput $log -RedirectStandardError "$log.err"
    $gradlePid = $proc.Id

    $deadline = (Get-Date).AddSeconds($PerVersionTimeoutSeconds)
    $timedOut = $false
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 2
        if (Test-ProbeVerdictIsFresh $probePath $runStarted) { break }
        if (Test-LogSaysBuildFinished $log) { break }
    }
    $freshVerdict = Test-ProbeVerdictIsFresh $probePath $runStarted
    $finished = $freshVerdict -or (Test-LogSaysBuildFinished $log)
    if (-not $finished) {
        $timedOut = $true
        Write-Host "    no verdict within ${PerVersionTimeoutSeconds}s - killing the run (a hang, not a failure)"
        # Kill the tree THIS sweep started, by pid, and nothing else. The previous version ran
        # `Get-Process java | Where-Object { $_.StartTime -gt (Get-Date).AddMinutes(-15) -and $_.WorkingSet64 -gt
        # 200MB }` and killed every match. That is not scoped to this project in any way: `java` in this
        # workspace is also the owner's OTHER Gradle builds - C:\Users\jwalr\Flashback launches its wrapper as
        # `java -jar gradle-wrapper.jar`, which is both recent and over 200MB, so a single timeout here would
        # silently kill an unrelated project's build. Killing our own pid and its children is sufficient.
        Get-CimInstance Win32_Process -Filter "ParentProcessId=$gradlePid" -ErrorAction SilentlyContinue |
            ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
        Stop-Process -Id $gradlePid -Force -ErrorAction SilentlyContinue
    }
    # Three questions, kept separate because collapsing them is what hid a dead server behind a green build:
    #   1. did the run end?            -> BUILD SUCCESSFUL or BUILD FAILED in the log
    #   2. did the BUILD pass?         -> BUILD SUCCESSFUL
    #   3. did the SERVER actually run? -> no fatal startup error in the log
    # Question 3 is not redundant and this is the round that proved it. A launch that died during bootstrap with
    # `InvalidMixinException: ... contains non-private static method` still printed **BUILD SUCCESSFUL**, because the
    # forked server process exits 0 after reporting "Failed to start the minecraft server" - so Gradle itself says
    # success for a server that never started, and runExit=0 was FAITHFUL, not broken. Reading only the Gradle
    # verdict is therefore not enough to say a version ran, and the probe verdict is what settles it.
    $serverStarted = Test-LogShowsServerStarted $log
    $serverDied = (-not $serverStarted) -and (Test-LogShowsServerStartupFailure $log)
    $buildSucceeded = Test-LogSaysBuildSucceeded $log
    $exit = if ($timedOut -or $serverDied) { -1 } elseif ($buildSucceeded) { 0 } else { 1 }

    $row = [ordered]@{ version = $version; world = $world; runExit = $exit; timedOut = $timedOut
        serverStartupFailed = $serverDied; seconds = [math]::Round(((Get-Date) - $versionStarted).TotalSeconds, 1) }

    if ($freshVerdict) {
        $probe = Get-Content $probePath -Raw | ConvertFrom-Json
        $row.success = $probe.success
        $row.alias = $probe.alias
        $row.judgedVersionId = $probe.judgedVersionId
        $row.levelHeight = $probe.levelHeight
        $row.generatorDepth = $probe.generatorDepth
        $row.appliedPreset = $probe.appliedSettings.expectedPreset
        $row.presetMatches = $probe.appliedSettings.chunkMatches
        $row.sampledChunks = $probe.sampledFullChunks
        $paletteFailures = @($probe.sampledPaletteFailures.PSObject.Properties)
        $row.anachronisticBlockKinds = $paletteFailures.Count
        $row.paletteFailureSample = ($paletteFailures | Select-Object -First 3 |
            ForEach-Object { "$($_.Name)=$($_.Value)" }) -join ', '
        $mixinFailures = @($probe.mixinTargets.PSObject.Properties | Where-Object { $_.Value -like 'FAILED*' })
        $row.mixinFailures = $mixinFailures.Count
        $row.spawnFilterInvocations = $probe.spawns.filterInvocations
    }
    else {
        $row.success = $false
        $row.note = 'no probe verdict was written'
    }

    $results += [pscustomobject]$row
    Write-Host ("    exit={0} success={1} preset={2} anachronisms={3} mixinFailures={4} seconds={5}" -f `
        $row.runExit, $row.success, $row.appliedPreset, $row.anachronisticBlockKinds, $row.mixinFailures,
        $row.seconds)
}

$out = 'logs/matrix-sweep.json'
$results | ConvertTo-Json -Depth 5 | Set-Content $out -Encoding UTF8

Write-Host ''
Write-Host '=== summary ==='
$results | Format-Table version, success, timedOut, presetMatches, anachronisticBlockKinds, mixinFailures, seconds -AutoSize
$totalSeconds = ($results | Measure-Object -Property seconds -Sum).Sum
$meanSeconds = if ($results.Count -gt 0) { [math]::Round($totalSeconds / $results.Count, 1) } else { 0 }
Write-Host ("measured: {0} versions, {1}s total, {2}s mean per version" -f `
    $results.Count, [math]::Round($totalSeconds, 1), $meanSeconds)

$bad = $results | Where-Object { $_.success -ne $true -or $_.mixinFailures -gt 0 -or $_.timedOut }
if ($bad) {
    Write-Host ''
    Write-Host "PROBLEMS in $($bad.Count) version(s):"
    $bad | ForEach-Object { Write-Host ("  {0}: success={1} mixinFailures={2} {3}" -f `
        $_.version, $_.success, $_.mixinFailures, $_.paletteFailureSample) }
}
else {
    Write-Host 'every declared version loaded, applied its preset and reported no mixin failure'
}
Write-Host "evidence: $out"
