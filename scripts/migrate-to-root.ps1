# Migrate the 1.21.1 mod from mc1211/ into the repository root and remove the 26.3 tree.
# Fails fast; every step is checked before anything is deleted.
$ErrorActionPreference = 'Stop'
$root = 'C:\Users\jwalr\Minecraft Version Emulator'
Set-Location $root

function Move-Checked($from, $to) {
    if (-not (Test-Path -LiteralPath $from)) { throw "missing source: $from" }
    Move-Item -LiteralPath $from -Destination $to -Force
    if (-not (Test-Path -LiteralPath $to)) { throw "move failed: $from -> $to" }
    Write-Output "moved $from -> $to"
}

# 1. The shared unit tests live in the 26.3 tree today and are included by mc1211's build through srcDir.
#    Copy the ones mc1211 does NOT exclude into mc1211/src/test BEFORE the 26.3 tree goes away.
$shared = @(Get-ChildItem 'src/test/java' -Recurse -File -Filter *.java)
$excluded = @('HistoricalSettingsPresetDataTest', 'HistoricalUpgradeServiceTest', 'RuntimeProbePathsTest',
              'RuntimeProbeScopeTest', 'BetaEnvelopeDataTest', 'UpgradeTransactionTest',
              'WorldGenSettingsAdvanceTest')
$copied = 0
foreach ($file in $shared) {
    if ($excluded -contains $file.BaseName) { continue }
    $relative = $file.FullName.Substring((Join-Path $root 'src/test/java').Length).TrimStart('\')
    $destination = Join-Path (Join-Path $root 'mc1211/src/test/java') $relative
    New-Item -ItemType Directory -Force -Path (Split-Path $destination) | Out-Null
    Copy-Item -LiteralPath $file.FullName -Destination $destination -Force
    $copied++
}
Write-Output "copied $copied shared tests into mc1211/src/test/java"
if ($copied -lt 15) { throw "expected the shared test suite, copied only $copied" }

# 2. Remove the 26.3 project. The sources, the Kotlin build scripts and the stale build output all go;
#    research/, scripts/, docs/, logs/, run/ and the notices stay.
Remove-Item -LiteralPath 'src' -Recurse -Force
Remove-Item -LiteralPath 'build.gradle.kts' -Force
Remove-Item -LiteralPath 'settings.gradle.kts' -Force
Remove-Item -LiteralPath 'build' -Recurse -Force -ErrorAction SilentlyContinue
# The Gradle project cache at the root belongs to the 26.3 build; the real caches live in ~/.gradle, so
# removing it only forces this project to reconfigure once.
Remove-Item -LiteralPath '.gradle' -Recurse -Force -ErrorAction SilentlyContinue
Write-Output 'removed the 26.3 sources, build scripts, build output and project cache'

# 3. Promote mc1211 to the root.
Move-Checked 'mc1211/src' 'src'
Move-Checked 'mc1211/build.gradle' 'build.gradle'
Move-Checked 'mc1211/settings.gradle' 'settings.gradle'
Move-Checked 'mc1211/gradle.properties' 'gradle.properties'
Move-Checked 'mc1211/gradle' 'gradle'
Move-Checked 'mc1211/gradlew' 'gradlew'
Move-Checked 'mc1211/gradlew.bat' 'gradlew.bat'

# 4. Merge the 1.21.1 run directory (EULA, server.properties, verification fixtures) into the root run/.
if (Test-Path 'mc1211/run') {
    Get-ChildItem 'mc1211/run' -Force | ForEach-Object {
        $target = Join-Path $root ('run/' + $_.Name)
        if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Recurse -Force }
        Move-Item -LiteralPath $_.FullName -Destination $target -Force
        Write-Output "merged run/$($_.Name)"
    }
}

# 5. Remove what is left of mc1211 (Gradle caches and build output only by now).
Remove-Item -LiteralPath 'mc1211' -Recurse -Force
if (Test-Path 'mc1211') { throw 'mc1211 still exists' }
Write-Output 'mc1211 removed'
Write-Output 'MIGRATION STEPS DONE'
