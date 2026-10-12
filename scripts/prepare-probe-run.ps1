<#
.SYNOPSIS
    Prepares the project-local, git-ignored runtime probe server directory.

.DESCRIPTION
    Creates run/probe with a reproducible, explicitly seeded server configuration for the
    Historical Experience runtime probe. Nothing here is machine specific and nothing outside
    the project directory is touched.

    The EULA is NOT accepted by this script unless -AcceptEula is passed explicitly, and even
    then only for this project-local directory. Running a Minecraft server requires the operator
    to accept Mojang's EULA; that decision belongs to the operator, not to a fixture script.

    Two independent seed-like files are produced:
      * server.properties, with an explicit level-seed so terrain is reproducible
      * probe-run.json, recording the run id, mode and the settings this run was created with,
        so a verdict can be matched against the run that produced it

.PARAMETER Mode
    establish : process 1. Creates the world, generates chunks, writes the block marker, saves.
    reload    : process 2. Fresh JVM over the SAME world; verifies the marker survived.

.PARAMETER Seed
    Explicit world seed. Defaults to a fixed value so runs are reproducible by default.

.PARAMETER RunId
    Identifier for this run. Defaults to a timestamp-based id. The validation harness passes its
    own value so a stale verdict can never be mistaken for a fresh pass.

.PARAMETER AcceptEula
    Explicitly write eula=true into the project-local run directory. Requires the operator to
    have actually read and accepted https://aka.ms/MinecraftEULA.

.PARAMETER ResetWorld
    Delete run/probe/world first. Use with -Mode establish to start clean; never with reload.

.EXAMPLE
    ./scripts/prepare-probe-run.ps1 -Mode establish -AcceptEula -ResetWorld

.EXAMPLE
    ./scripts/prepare-probe-run.ps1 -Mode reload
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('establish', 'reload')]
    [string] $Mode,

    [long] $Seed = 2508291534120694762,

    [string] $RunId = ("{0}-{1}" -f (Get-Date -Format 'yyyyMMddTHHmmss'), ([guid]::NewGuid().ToString('N').Substring(0, 8))),

    [switch] $AcceptEula,

    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9_-]{0,79}$')]
    [string] $WorldName = 'world',

    [switch] $UpstreamPreset,

    # Which Moderner Beta settings preset an upstream-scope world should use. Defaults to the pinned
    # Beta preset every existing upstream verdict was taken against.
    [string] $UpstreamSettingsPreset = 'moderner_beta:beta',

    [switch] $ContentPreview,

    # Which immutable content-preview generation revision the world preset must reference. Each
    # revision is a separate alias and a separate world preset; an already generated world keeps the
    # revision it was created with, so this switch only chooses what a NEW world is created from.
    [ValidateSet('1', '2', '3', '4', '5')]
    [string] $ContentPreviewRevision = '5',

    # Which declared historical version to create the world as. The value is the version's gameplay
    # profile id, which is also the name of its original world preset, so no list is maintained here:
    # the script derives the level type and then proves the resources exist.
    [string] $Version = 'historical_experience:beta_1_7_3',

    # Whether the version generated structures. Beta 1.7.3 had none; Beta 1.8.1 introduced mineshafts,
    # villages and strongholds. This is a version property, so it is passed explicitly rather than
    # assumed, and the probe fails when a world's option disagrees with its declared version.
    [switch] $Structures,

    [switch] $ResetWorld
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# Resolve the project root from this script's location. No absolute personal path is embedded.
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $projectRoot 'build.gradle.kts'))) {
    throw "project root not found from '$PSScriptRoot' (expected build.gradle.kts)"
}

$runDir = Join-Path $projectRoot 'run\probe'
$worldDir = [System.IO.Path]::GetFullPath((Join-Path $runDir $WorldName))
$expectedWorldPath = [System.IO.Path]::GetFullPath((Join-Path $projectRoot "run\probe\$WorldName"))
if ($worldDir -ne $expectedWorldPath) { throw "unexpected world target: $worldDir" }
if ($UpstreamPreset -and $ContentPreview) { throw 'Select only one probe scope' }
$contentPreviewPreset = "historical_experience:beta_1_7_3_content_preview_$ContentPreviewRevision"
$levelType = if ($UpstreamPreset) { 'moderner_beta:moderner_beta' }
    elseif ($ContentPreview) { $contentPreviewPreset }
    else { $Version }
$generatorSettings = if ($UpstreamPreset) {
    # Selectable so a world can be created DIRECTLY from Moderner Beta's own preset with no project
    # wrapper at all. That is what separates "this project's world preset is wrong for a given upstream
    # preset" from "the upstream preset does not work here", and the two need completely different fixes.
    '{"preset":"' + $UpstreamSettingsPreset + '"}'
} else { '{}' }

# Every declared version must have its two resource files. Checking here turns "remember the JSON" into
# a failure at preparation time rather than a world that will not load, and it is why the version list
# is not duplicated in this script.
if (-not $UpstreamPreset -and -not $ContentPreview) {
    if ($Version -notmatch '^[a-z0-9_]+:[a-z0-9_]+$') {
        throw "Version must be a namespaced identifier such as historical_experience:beta_1_7_3, got '$Version'"
    }
    $path = $Version.Split(':')[1]
    foreach ($kind in @(
        @{ Dir = 'worldgen\world_preset'; Label = 'world preset' },
        @{ Dir = 'moderner_beta\settings_preset'; Label = 'settings preset' })) {
        $file = Join-Path $projectRoot "src\main\resources\data\historical_experience\$($kind.Dir)\$path.json"
        if (-not (Test-Path $file)) {
            throw "version $Version has no $($kind.Label): $file"
        }
    }
}

if ($ContentPreview) {
    foreach ($kind in @(
        @{ Dir = 'worldgen\world_preset'; Label = 'world preset' },
        @{ Dir = 'moderner_beta\settings_preset'; Label = 'settings preset' })) {
        $file = Join-Path $projectRoot (
            "src\main\resources\data\historical_experience\$($kind.Dir)\beta_1_7_3_content_preview_$ContentPreviewRevision.json")
        if (-not (Test-Path $file)) {
            throw "content-preview revision $ContentPreviewRevision has no $($kind.Label): $file"
        }
    }
}

$reload = $Mode -eq 'reload'
if ($reload -and $ResetWorld) {
    throw "-Mode reload must never reset the world: the whole point is to read what the previous process wrote"
}
if ($reload -and -not (Test-Path $worldDir)) {
    throw "-Mode reload requires an existing world at '$worldDir'; run -Mode establish first"
}

if ($ResetWorld -and (Test-Path $worldDir)) {
    Write-Host "removing previous world at $worldDir"
    Remove-Item -Recurse -Force $worldDir
}
New-Item -ItemType Directory -Force -Path $runDir | Out-Null

# --- EULA -------------------------------------------------------------------------------------
$eulaPath = Join-Path $runDir 'eula.txt'
if ($AcceptEula) {
    @"
#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://aka.ms/MinecraftEULA).
#Accepted explicitly by the operator for this project-local, git-ignored test server only.
eula=true
"@ | Set-Content -Path $eulaPath -Encoding UTF8
    Write-Host "eula=true written to $eulaPath (project-local test server only)"
}
elseif (-not (Test-Path $eulaPath)) {
    @"
#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://aka.ms/MinecraftEULA).
#Not accepted. The operator must review https://aka.ms/MinecraftEULA and then either edit this
#file or re-run this script with -AcceptEula. The fixture script will not accept it silently.
eula=false
"@ | Set-Content -Path $eulaPath -Encoding UTF8
    Write-Host "eula=false written to $eulaPath; pass -AcceptEula to accept for this test server"
}

# Beta 1.7.3 had no structures: strongholds, mineshafts and villages all arrive in Beta 1.8.1, and
# dungeons are a placed feature rather than a structure. Structure generation is a WORLD OPTION, not
# a settings component, so a correctly created historical world must disable it.
#
# The ordinary/upstream scope deliberately keeps the vanilla default: that world is the
# non-interference control, and it must still be able to receive modern structures so that the
# historical restriction can be shown to be specific rather than global.
# An ordinary upstream world keeps the vanilla default; a historical version uses what the caller
# declares for it, because the answer is a property of the version.
$generateStructures = if ($UpstreamPreset) { $true } else { [bool] $Structures }

# --- server.properties ------------------------------------------------------------------------
# level-type selects THIS project's world preset, which references the project-owned settings
# preset. With the default level type the world would be ordinary terrain and the probe would
# correctly fail, because no project marker would be present.
$properties = @(
    '# Historical Experience runtime probe test server (project-local, git-ignored).',
    '# Generated by scripts/prepare-probe-run.ps1; no machine-specific paths are used.',
    "level-type=$levelType",
    "level-name=$WorldName",
    "generator-settings=$generatorSettings",
    "level-seed=$Seed",
    "generate-structures=$($generateStructures.ToString().ToLowerInvariant())",
    # Pinned, not left to the vanilla default. The player-rule probe drives a real exhausting food tick,
    # and on PEACEFUL difficulty vanilla itself refuses to drain the food level - so the control could
    # not tell "the version has no hunger" apart from "the difficulty suppressed it".
    'difficulty=normal',
    'online-mode=false',
    # Pinned because the server's own default is white-list=true, and an empty whitelist then rejects
    # every connection. A local, git-ignored development run directory with authentication already
    # disabled does not need a whitelist, and leaving this to the default made a client-join test fail
    # with "You are not white-listed on this server" - a harness defect, not a mod defect.
    'white-list=false',
    'enforce-whitelist=false',
    # RCON is enabled so a scripted run can drive a real server through Minecraft's own authenticated
    # admin protocol. The password is local to this git-ignored run directory and is read from this file
    # by scripts/rcon.py; it is never written into a tracked file, and this is a supported server feature
    # rather than a way around authentication.
    'enable-rcon=true',
    'rcon.port=25575',
    ('rcon.password=dev-' + [guid]::NewGuid().ToString('N').Substring(0, 12)),
    'server-ip=127.0.0.1',
    'server-port=25599',
    'enable-query=false',
    'sync-chunk-writes=true',
    'max-players=1',
    'spawn-protection=0'
) -join "`n"
$propertiesPath = Join-Path $runDir 'server.properties'
Set-Content -Path $propertiesPath -Value ($properties + "`n") -Encoding UTF8

# --- run context ------------------------------------------------------------------------------
# The validation harness compares this with the verdict the probe writes, so a verdict left over
# from an earlier run, or produced for the other mode, cannot be mistaken for a fresh pass.
$context = [ordered]@{
    runId       = $RunId
    mode        = $Mode
    seed        = $Seed
    levelType   = $levelType
    version     = $(if ($UpstreamPreset) { $null } else { $Version })
    generateStructures = $generateStructures
    scope       = $(if ($UpstreamPreset) { 'upstream-preset' } elseif ($ContentPreview) { 'content-preview' } else { 'historical' })
    contentPreviewPreset = $(if ($ContentPreview) { $contentPreviewPreset } else { $null })
    worldDir    = "run/probe/$WorldName"
    verdictFile = "run/probe/$WorldName/historical_experience_probe_verdict.txt"
    preparedAt  = (Get-Date).ToUniversalTime().ToString('o')
    resetWorld  = [bool] $ResetWorld
    eulaWritten = [bool] $AcceptEula
}
$contextPath = Join-Path $runDir 'probe-run.json'
Set-Content -Path $contextPath -Value ($context | ConvertTo-Json) -Encoding UTF8

# A verdict from a previous run must never survive into this one.
$staleVerdict = Join-Path $worldDir 'historical_experience_probe_verdict.txt'
if (Test-Path $staleVerdict) {
    Remove-Item -Force $staleVerdict
    Write-Host "removed stale verdict $staleVerdict"
}

Write-Host ""
Write-Host "probe run prepared"
Write-Host "  mode     : $Mode"
Write-Host "  runId    : $RunId"
Write-Host "  seed     : $Seed"
Write-Host "  run dir  : $runDir"
Write-Host "  context  : $contextPath"
Write-Host ""
Write-Host "next: .\gradlew.bat --console=plain -PprobeWorldName=$WorldName -PprobeUpstreamPreset=$($UpstreamPreset.IsPresent.ToString().ToLowerInvariant()) -PprobeContentPreview=$($ContentPreview.IsPresent.ToString().ToLowerInvariant()) $(if ($reload) { 'runRuntimeProbeReload' } else { 'runRuntimeProbe' })"

