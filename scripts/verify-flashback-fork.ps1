# Verification harness for the j-w-i Flashback fork.
#
# Owned by the Flashback compatibility worker (bootstrap scripts carve-out). Writes nothing into the
# fork checkout. Results are reported in docs/FLASHBACK_COMPATIBILITY.md and
# research/flashback-fork-evidence.json.
#
# Usage:
#   pwsh -NoProfile -File scripts/verify-flashback-fork.ps1
#   pwsh -NoProfile -File scripts/verify-flashback-fork.ps1 -JavaPath 'C:\nope\java.exe'   # negative control
#
# Design constraints this harness enforces (see the notes at each site):
#   1. Validate tool paths before use; a missing tool is a hard failure, never a pass.
#   2. Real exit codes only. Start-Process is used because $LASTEXITCODE is not reliably set for
#      .bat/.cmd wrappers and PowerShell reuses the previous value when a command fails to start.
#      A null exit code means "never ran" and is reported as INVALID, never PASS.
#   3. The classpath comes from the fork's own Gradle-resolved runtimeClasspath and compileClasspath,
#      so no unrelated cached version can leak in and no copy step can silently fail.
#   4. The scratch directory is asserted project-local before anything is deleted.
#   5. Behaviour suites are counted separately from static source checks.
#   6. Invocations run with the fork as the working directory, because several suites open paths
#      like 'src/main/java/...' and 'build/classes/java/main/...' relative to it.

[CmdletBinding()]
param(
    [string]$JavaPath  = 'C:\Program Files\Java\jdk-25.0.4.1\bin\java.exe',
    [string]$JavacPath = 'C:\Program Files\Java\jdk-25.0.4.1\bin\javac.exe',
    [string]$ProjectRoot = 'C:\Users\jwalr\Minecraft Version Emulator',
    [string]$ForkPath = 'C:\Users\jwalr\Minecraft Version Emulator\.upstream\flashback-j-w-i',
    [switch]$SkipClasspathDump
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

# ---------------------------------------------------------------- paths and safety

$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$ForkPath    = (Resolve-Path -LiteralPath $ForkPath).Path

$Scratch     = Join-Path $ProjectRoot 'logs\fork-verify-work'
$RuntimeCp   = Join-Path $ProjectRoot 'logs\fork-verify-cp-runtime.txt'
$CompileCp   = Join-Path $ProjectRoot 'logs\fork-verify-cp-compile.txt'
$DumpLog     = Join-Path $ProjectRoot 'logs\fork-verify-classpath-dump.log'
$InvokeLog   = Join-Path $ProjectRoot 'logs\fork-verify-invocations.log'
$InitScript  = Join-Path $ProjectRoot 'scripts\dsh-dump-classpath.init.gradle'

function Assert-ProjectLocal([string]$Path, [string]$What) {
    $full = [System.IO.Path]::GetFullPath($Path)
    $root = $ProjectRoot.TrimEnd('\') + '\'
    if (-not $full.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "$What resolved to '$full', outside the project root. Refusing to continue."
    }
    if ($full.TrimEnd('\') -eq $ProjectRoot.TrimEnd('\')) {
        throw "$What resolved to the project root itself. Refusing to continue."
    }
    return $full
}

function Assert-ToolExists([string]$Tool, [string]$Label) {
    if (-not (Test-Path -LiteralPath $Tool -PathType Leaf)) {
        throw "$Label not found at '$Tool'. Cannot run the harness. (This is expected for the negative control.)"
    }
    return (Resolve-Path -LiteralPath $Tool).Path
}

# ---------------------------------------------------------------- invocation

$script:Launches = 0
$script:InvokeSeq = 0
function Invoke-Native([string]$Tool, [string[]]$Arguments) {
    $ErrorActionPreference = 'Continue'
    $script:InvokeSeq++
    $seq = $script:InvokeSeq
    # Persist every invocation's full output. Start-Process cannot write to both a file and a
    # pipeline, so the captured output is appended here; this is the audit trail for a passing run,
    # where the console only shows the one-line summary.
    $record = {
        param($Header, $Lines)
        try {
            $Header | Out-File -LiteralPath $InvokeLog -Append -Encoding utf8
            if ($Lines) { $Lines | Out-File -LiteralPath $InvokeLog -Append -Encoding utf8 }
            '' | Out-File -LiteralPath $InvokeLog -Append -Encoding utf8
        } catch { }
    }
    if (-not (Test-Path -LiteralPath $Tool -PathType Leaf)) {
        & $record "### [$seq] $Tool -> LAUNCH_FAILED (tool does not exist)" @()
        return [pscustomobject]@{ ExitCode = $null; Output = @("LAUNCH_FAILED: '$Tool' does not exist"); Started = $false }
    }
    $stdout = Join-Path $Scratch 'invoke-stdout.txt'
    $stderr = Join-Path $Scratch 'invoke-stderr.txt'
    Remove-Item -LiteralPath $stdout, $stderr -ErrorAction SilentlyContinue
    # Start-Process does not quote arguments for us, so any argument containing whitespace must be
    # quoted or the command line parser splits it.
    $quoted = $Arguments | ForEach-Object { if ($_ -match '\s') { '"' + $_ + '"' } else { $_ } }
    $proc = $null
    try {
        $proc = Start-Process -FilePath $Tool -ArgumentList $quoted -Wait -PassThru -NoNewWindow `
            -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    } catch {
        & $record "### [$seq] $Tool -> LAUNCH_FAILED: $($_.Exception.Message)" @()
        return [pscustomobject]@{ ExitCode = $null; Output = @("LAUNCH_FAILED: $($_.Exception.Message)"); Started = $false }
    }
    $out = @()
    if (Test-Path -LiteralPath $stdout) { $out += Get-Content -LiteralPath $stdout -ErrorAction SilentlyContinue }
    if (Test-Path -LiteralPath $stderr) { $out += Get-Content -LiteralPath $stderr -ErrorAction SilentlyContinue }
    $code = if ($null -ne $proc) { $proc.ExitCode } else { $null }
    $started = ($null -ne $code)
    if ($started) { $script:Launches++ }
    # Record a short, useful header. The classpath is the same in almost every invocation and would
    # otherwise dominate the log, so only non-classpath arguments are shown, and never in full.
    $interesting = @()
    for ($i = 0; $i -lt $Arguments.Count; $i++) {
        $a = $Arguments[$i]
        if ($a -eq '-cp') { $i++; continue }
        if ($a -match '[\\/]' -and $a.Length -gt 80) { $interesting += (Split-Path $a -Leaf); continue }
        if ($a.Length -gt 80) { $interesting += ($a.Substring(0, 60) + '…'); continue }
        $interesting += $a
    }
    if ($interesting.Count -gt 8) { $interesting = $interesting[0..7] + @('…') }
    & $record "### [$seq] $(Split-Path $Tool -Leaf) $($interesting -join ' ') -> exit=$code started=$started" $out
    return [pscustomobject]@{ ExitCode = $code; Output = @($out); Started = $started }
}

# Run with the fork as the working directory: several suites resolve relative paths against it.
function Invoke-InFork([string]$Tool, [string[]]$Arguments) {
    Push-Location -LiteralPath $ForkPath
    try { return Invoke-Native $Tool $Arguments } finally { Pop-Location }
}

# ---------------------------------------------------------------- counters

$script:BehaviourPass = 0; $script:BehaviourFail = 0; $script:BehaviourInvalid = 0
$script:SourcePass = 0;    $script:SourceFail = 0;    $script:SourceInvalid = 0

function Report([string]$Name, $Result, [string]$Kind) {
    $last = ($Result.Output | Where-Object { $_ -match '\S' } | Select-Object -Last 1)
    $isBehaviour = ($Kind -eq 'behaviour')
    if (-not $Result.Started) {
        if ($isBehaviour) { $script:BehaviourInvalid++ } else { $script:SourceInvalid++ }
        Write-Output ("INVALID {0,-40} process did not run (no exit status)" -f $Name)
        $Result.Output | Select-Object -Last 6 | ForEach-Object { Write-Output "        $_" }
    } elseif ($Result.ExitCode -ne 0) {
        if ($isBehaviour) { $script:BehaviourFail++ } else { $script:SourceFail++ }
        Write-Output ("FAIL    {0,-40} exit={1}" -f $Name, $Result.ExitCode)
        $Result.Output | Select-Object -Last 12 | ForEach-Object { Write-Output "        $_" }
    } else {
        if ($isBehaviour) { $script:BehaviourPass++ } else { $script:SourcePass++ }
        Write-Output ("PASS    {0,-40} {1}" -f $Name, $last)
    }
}

# ---------------------------------------------------------------- start

Write-Output "verify-flashback-fork"
Write-Output "  project_root = $ProjectRoot"
Write-Output "  fork         = $ForkPath"
Write-Output "  start_utc    = $(Get-Date -Format o)"

$Java  = Assert-ToolExists $JavaPath  'java'
$Javac = Assert-ToolExists $JavacPath 'javac'
Write-Output "  java  = $Java"
Write-Output "  javac = $Javac"

$v = Invoke-Native $Java @('-version')
Write-Output "  java -version exit=$($v.ExitCode) started=$($v.Started)"
$v.Output | Select-Object -First 1 | ForEach-Object { Write-Output "    $_" }

$Scratch   = Assert-ProjectLocal $Scratch   'scratch directory'
$RuntimeCp = Assert-ProjectLocal $RuntimeCp 'runtime classpath file'
$CompileCp = Assert-ProjectLocal $CompileCp 'compile classpath file'
$DumpLog   = Assert-ProjectLocal $DumpLog   'classpath dump log'
$InvokeLog = Assert-ProjectLocal $InvokeLog 'invocation log'

# Start the audit trail fresh so a stale file cannot be mistaken for this run's evidence.
Set-Content -LiteralPath $InvokeLog -Value "# verify-flashback-fork invocation log`n# started $(Get-Date -Format o)`n" -Encoding utf8

if (Test-Path -LiteralPath $Scratch) {
    Write-Output "  removing scratch (verified project-local): $Scratch"
    Remove-Item -LiteralPath $Scratch -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $Scratch, (Join-Path $Scratch 'stubs'), (Join-Path $Scratch 'classes') | Out-Null

# ---------------------------------------------------------------- resolved classpaths

if (-not $SkipClasspathDump) {
    Write-Output "=== resolved classpaths via Gradle init script ==="
    if (-not (Test-Path -LiteralPath $InitScript)) { throw "init script not found: $InitScript" }
    $gradlew = Join-Path $ForkPath 'gradlew.bat'
    if (-not (Test-Path -LiteralPath $gradlew)) { throw "gradle wrapper not found: $gradlew" }
    $env:JAVA_HOME = Split-Path (Split-Path $Java)

    Push-Location -LiteralPath $ForkPath
    try {
        # Guard: invoked from the project root, the wrapper would use the root project's
        # build.gradle.kts and fail on an unrelated Fabric Loom variant.
        if (-not (Test-Path -LiteralPath '.\gradlew.bat' -PathType Leaf)) {
            throw "gradlew.bat is not in the working directory after entering the fork: $ForkPath"
        }
        $dump = Invoke-Native $gradlew @(
            '--no-daemon', '--console=plain',
            '--init-script', $InitScript,
            "-PdshOutFile=$RuntimeCp",
            "-PdshOutCompileFile=$CompileCp",
            'dshDumpRuntimeClasspath', 'dshDumpCompileClasspath'
        )
    } finally {
        Pop-Location
    }
    # Ignored output: kept for diagnosis, not allowed to decide the result.
    $dump.Output | Set-Content -LiteralPath $DumpLog
    Write-Output "  gradle cwd=$ForkPath exit=$($dump.ExitCode) started=$($dump.Started); log=$DumpLog"
    $dump.Output | Where-Object { $_ -match 'DSH_DUMP' } | ForEach-Object { Write-Output "  $_" }
    if (-not $dump.Started -or $dump.ExitCode -ne 0) {
        $dump.Output | Select-Object -Last 25 | ForEach-Object { Write-Output "    $_" }
        throw "Classpath dump failed (exit=$($dump.ExitCode)). Refusing to fall back to scanning ~/.gradle."
    }
}

function Read-Classpath([string]$File, [string]$Label) {
    if (-not (Test-Path -LiteralPath $File)) { throw "$Label file not produced: $File" }
    $entries = Get-Content -LiteralPath $File | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim() }
    if (-not $entries) { throw "$Label file is empty: $File" }
    $missing = $entries | Where-Object { -not (Test-Path -LiteralPath $_ -PathType Leaf) }
    if ($missing) {
        $missing | Select-Object -First 5 | ForEach-Object { Write-Output "  MISSING $Label ENTRY: $_" }
        throw "$($missing.Count) $Label entries do not exist."
    }
    Write-Output "  $Label entries: $($entries.Count)"
    return $entries
}

$runtimeEntries = Read-Classpath $RuntimeCp 'runtimeClasspath'
$compileEntries = Read-Classpath $CompileCp 'compileClasspath'

# runtime + compileOnly, which is what the fork's own source checks are written against.
$combinedEntries = @($runtimeEntries + $compileEntries | Sort-Object -Unique)
Write-Output "  combined (used for compile and source checks) entries: $($combinedEntries.Count)"
$LIBS_RUN = ($runtimeEntries -join ';')
$LIBS_ALL = ($combinedEntries -join ';')

# ---------------------------------------------------------------- compile

[string[]]$stubPaths = Get-ChildItem -LiteralPath (Join-Path $ForkPath 'devtools\checks\stubs') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
[string[]]$srcPaths  = Get-ChildItem -LiteralPath (Join-Path $ForkPath 'devtools\checks\src')   -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
$stubs    = Join-Path $Scratch 'stubs'
$classes  = Join-Path $Scratch 'classes'
$forkMain = Join-Path $ForkPath 'build\classes\java\main'

if (-not (Test-Path -LiteralPath $forkMain -PathType Container)) {
    throw "fork build output not found: $forkMain. Build the fork first."
}
Write-Output "=== compile ==="
Write-Output "  stub_sources=$($stubPaths.Count) test_sources=$($srcPaths.Count)"

$rc = Invoke-Native $Javac (@('-nowarn', '-cp', "$forkMain;$LIBS_ALL", '-d', $stubs) + $stubPaths)
Write-Output "  javac stubs exit=$($rc.ExitCode) started=$($rc.Started)"
if (-not $rc.Started -or $rc.ExitCode -ne 0) {
    $rc.Output | Select-Object -Last 25 | ForEach-Object { Write-Output "    $_" }
    throw "Stub compilation failed."
}

$rc = Invoke-Native $Javac (@('-nowarn', '-cp', "$stubs;$forkMain;$LIBS_ALL", '-d', $classes) + $srcPaths)
Write-Output "  javac suites exit=$($rc.ExitCode) started=$($rc.Started)"
if (-not $rc.Started -or $rc.ExitCode -ne 0) {
    $rc.Output | Select-Object -Last 25 | ForEach-Object { Write-Output "    $_" }
    throw "Suite compilation failed."
}

# Behaviour suites run against the runtime classpath alone; the source checks need compileOnly too.
$RUN      = "$stubs;$classes;$forkMain;$LIBS_RUN"
$RUN_ALL  = "$stubs;$classes;$forkMain;$LIBS_ALL"

# ---------------------------------------------------------------- behaviour suites

Write-Output "=== behaviour suites (exercise fork code) ==="
$suites = @(
    'com.moulberry.flashback.state.MigrationTest',
    'com.moulberry.flashback.state.EvaluationTest',
    'com.moulberry.flashback.state.CameraObjectTest',
    'com.moulberry.flashback.state.CameraInspectorRegressionTest',
    'com.moulberry.flashback.state.CutTest',
    'com.moulberry.flashback.state.LoadCompatTest',
    'com.moulberry.flashback.editor.ui.timeline.LayoutTest',
    'com.moulberry.flashback.keyframe.OrbitTest',
    'com.moulberry.flashback.keybind.ScrollBindingsTest',
    'com.moulberry.flashback.gui.GuiLogTest',
    'com.moulberry.flashback.gui.ContainerWireTest',
    'com.moulberry.flashback.playback.PlaybackTimingTest',
    'com.moulberry.flashback.playback.AccuratePositionTimelineTest',
    'com.moulberry.flashback.playback.WeatherColumnOrientationTest',
    'com.moulberry.flashback.playback.WeatherTextureCoordinatesTest'
)
foreach ($s in $suites) {
    Report (($s -split '\.')[-1]) (Invoke-InFork $Java @('-cp', $RUN, $s)) 'behaviour'
}

Write-Output "=== standalone behaviour model ==="
$standaloneOut = Join-Path $Scratch 'standalone'
New-Item -ItemType Directory -Force -Path $standaloneOut | Out-Null
$sa = Invoke-Native $Javac @('-nowarn', '-d', $standaloneOut,
    (Join-Path $ForkPath 'src\main\java\com\moulberry\flashback\gui\InventoryMenuSlots.java'),
    (Join-Path $ForkPath 'devtools\checks\ReplayInventorySlotsTest.java'))
if (-not $sa.Started -or $sa.ExitCode -ne 0) {
    Report 'ReplayInventorySlotsTest' ([pscustomobject]@{ ExitCode = $null; Output = @('standalone compile failed'); Started = $false }) 'behaviour'
    $sa.Output | Select-Object -Last 10 | ForEach-Object { Write-Output "        $_" }
} else {
    Report 'ReplayInventorySlotsTest' (Invoke-InFork $Java @('-cp', $standaloneOut, 'ReplayInventorySlotsTest')) 'behaviour'
}

# ---------------------------------------------------------------- static source checks

Write-Output "=== static source checks (parse/validate source; NOT behaviour) ==="
Report 'ImGuiPairingCheck' (Invoke-InFork $Java @('-cp', $RUN_ALL, 'com.moulberry.flashback.ImGuiPairingCheck',
    (Join-Path $ForkPath 'src\main\java\com\moulberry\flashback\editor\ui\windows\TimelineWindow.java'),
    (Join-Path $ForkPath 'src\main\java\com\moulberry\flashback\editor\ui\windows\CameraInspectorWindow.java'))) 'source'
Report 'ContainerGuiCheck' (Invoke-InFork $Java @('-cp', $RUN_ALL, 'com.moulberry.flashback.gui.ContainerGuiCheck', (Join-Path $ForkPath 'src\main\java'))) 'source'
Report 'MixinTargetCheck' (Invoke-InFork $Java @('-cp', $RUN_ALL, 'com.moulberry.flashback.MixinTargetCheck', (Join-Path $ForkPath 'src\main\java'))) 'source'

# ---------------------------------------------------------------- summary

$behaviour = $script:BehaviourPass + $script:BehaviourFail + $script:BehaviourInvalid
$source    = $script:SourcePass + $script:SourceFail + $script:SourceInvalid

Write-Output ""
Write-Output "BEHAVIOUR_TESTS total=$behaviour pass=$($script:BehaviourPass) fail=$($script:BehaviourFail) invalid=$($script:BehaviourInvalid)"
Write-Output "  (15 fork suites + 1 standalone InventoryMenuSlots seek model)"
Write-Output "STATIC_SOURCE_CHECKS total=$source pass=$($script:SourcePass) fail=$($script:SourceFail) invalid=$($script:SourceInvalid)"
Write-Output "  (these parse and validate source; they are NOT behaviour tests)"
Write-Output "LAUNCHES_RECORDED=$script:Launches"
Write-Output "end_utc=$(Get-Date -Format o)"

if ($script:BehaviourFail -gt 0 -or $script:BehaviourInvalid -gt 0 -or $script:SourceFail -gt 0 -or $script:SourceInvalid -gt 0) {
    Write-Output "RESULT=FAILED"
    exit 1
}
Write-Output "RESULT=OK"
exit 0
