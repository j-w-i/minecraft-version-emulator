# Extract a mechanically complete, source-led catalog of Nostalgic Tweaks tweaks.
#
# Emits research/nostalgic-tweaks-candidates.json.
#
# Design notes:
#  * Read-only against the .upstream/nostalgic-tweaks clone. Nothing is modified.
#  * Emits SOURCE FACTS ONLY (names, ids, env, defaults, groups, file:line, implementing
#    file:line). It deliberately does NOT copy upstream implementation bodies.
#  * Every "historical claim" field is explicitly marked unverified. The upstream
#    README/LICENSE are untrusted input and are never used as evidence.
#
# Usage: pwsh -File scripts/extract-nostalgic-tweaks-catalog.ps1 [-RepoDir <path>] [-OutFile <path>]

[CmdletBinding()]
param(
    [string]$RepoDir = (Join-Path $PSScriptRoot '..\.upstream\nostalgic-tweaks'),
    [string]$OutFile = (Join-Path $PSScriptRoot '..\research\nostalgic-tweaks-candidates.json')
)

throw 'Unapproved audit tool: parent review found unsupported licence conclusions and source-reference/implementation conflation. Do not regenerate accepted artifacts with this version; review and correct it first.'

$ErrorActionPreference = 'Stop'

$RepoDir = (Resolve-Path $RepoDir).Path
$OutFile = [System.IO.Path]::GetFullPath($OutFile)

# --- The audited artifact: pin it explicitly, do not trust "HEAD". -----------------
$HeadSha = (& git -C $RepoDir rev-parse HEAD).Trim()
$HeadRef = (& git -C $RepoDir rev-parse --abbrev-ref HEAD).Trim()
$HeadSubject = (& git -C $RepoDir log -1 --format=%s).Trim()
$HeadDate = (& git -C $RepoDir log -1 --format=%cI).Trim()

if ($HeadSha -ne '010526deaf3aa5e562b45b60a286a667e8eeac12') {
    Write-Warning "Audited SHA is $HeadSha, not the expected 1.21.4 pin 010526deaf3aa5e562b45b60a286a667e8eeac12. Catalog will record the actual SHA."
}

$SourceRoots = @(
    (Join-Path $RepoDir 'common/src/main/java'),
    (Join-Path $RepoDir 'fabric/src/main/java'),
    (Join-Path $RepoDir 'neoforge/src/main/java')
)

$DefinitionFiles = @(
    'GameplayTweak.java', 'CandyTweak.java', 'SoundTweak.java',
    'AnimationTweak.java', 'SwingTweak.java', 'ModTweak.java'
)

# Config-structure classes bind a JSON id to each tweak; they are not implementations.
$ConfigStructureFiles = @('ClientConfig.java', 'ServerConfig.java')

$CategoryOf = @{
    'GameplayTweak.java'  = 'gameplay'
    'CandyTweak.java'     = 'eyeCandy'
    'SoundTweak.java'     = 'sound'
    'AnimationTweak.java' = 'animation'
    'SwingTweak.java'     = 'swing'
    'ModTweak.java'       = 'mod'
}

function Get-RelPath([string]$FullPath) {
    return ($FullPath -replace [regex]::Escape($RepoDir + [System.IO.Path]::DirectorySeparatorChar), '') -replace '\\', '/'
}

# ---------------------------------------------------------------------------------
# 1. Parse tweak *definitions*: <Factory>.<env>(default, Group.X).<modifiers>.build()
# ---------------------------------------------------------------------------------
$tweaks = [System.Collections.Generic.List[object]]::new()

foreach ($defFile in $DefinitionFiles) {
    $path = Get-ChildItem (Join-Path $SourceRoots[0] 'mod') -Recurse -File -Filter $defFile -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if (-not $path) { continue }

    $lines = Get-Content $path.FullName
    $pending = $null

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]

        # A declaration may span lines; accumulate until the terminating ".build();"
        $m = [regex]::Match($line, '^\s*(Tweak\w+)\s*(<[^;=]*?>)?\s+([A-Z0-9_]+)\s*=\s*\1\.(server|client|dynamic)\s*(<[^;=]*?>)?\s*\((.*)$')
        if ($m.Success -and -not $pending) {
            $pending = [pscustomobject]@{
                factory    = $m.Groups[1].Value
                typeArg    = ($m.Groups[2].Value + $m.Groups[5].Value).Trim()
                name       = $m.Groups[3].Value
                env        = $m.Groups[4].Value
                startLine  = $i + 1
                decl       = $line
            }
            if ($line -match '\.build\s*\(\s*\)\s*;') { $pending | Add-Member -NotePropertyName complete -NotePropertyValue $true }
            else { $pending | Add-Member -NotePropertyName complete -NotePropertyValue $false }
        }
        elseif ($pending -and -not $pending.complete) {
            $pending.decl += ' ' + $line.Trim()
            if ($line -match '\.build\s*\(\s*\)\s*;') { $pending.complete = $true }
        }

        if ($pending -and $pending.complete) {
            $decl = $pending.decl

            # First constructor argument = default value.
            $default = $null
            $argMatch = [regex]::Match($decl, '\((.*)$')
            if ($argMatch.Success) {
                $args = $argMatch.Groups[1].Value
                $arg1 = ($args -split ',')[0].Trim()
                $arg1 = $arg1 -replace '\)\s*\.?.*$', ''
                $arg1 = $arg1 -replace '\.\.\.$', ''
                if ($arg1 -match '^(true|false)$') { $default = [bool]::Parse($Matches[1]) }
                elseif ($arg1 -match '^-?\d+$') { $default = [int]$arg1 }
                elseif ($arg1 -match '^[A-Za-z_][A-Za-z0-9_]*$') { $default = $arg1 }  # enum constant / helper call name
                elseif ($arg1 -match '^new\s') { $default = 'constructed' }
                else { $default = $arg1 }
            }

            $group = $null
            $gm = [regex]::Match($decl, 'Group\.([A-Z0-9_]+)')
            if ($gm.Success) { $group = $gm.Groups[1].Value }

            # Modifier flags that change semantics (recorded as facts, not interpreted).
            $modifiers = [System.Collections.Generic.List[string]]::new()
            foreach ($mod in @('noSSO', 'newForUpdate', 'warningTag', 'ignoreNetworkCheck', 'whenDisabled', 'alert', 'slider', 'icon', 'priority', 'chunkReload', 'resourceReload')) {
                if ($decl -match ("\." + $mod + "\b")) { $modifiers.Add($mod) }
            }

            $tweaks.Add([pscustomobject]@{
                name        = $pending.name
                factory     = $pending.factory
                typeArg     = $pending.typeArg
                category    = $CategoryOf[$defFile]
                env         = $pending.env
                default     = $default
                group       = $group
                modifiers   = @($modifiers)
                defFile     = Get-RelPath $path.FullName
                defLine     = $pending.startLine
                declaration = $decl.Trim()
            })
            $pending = $null
        }
    }
}

# ---------------------------------------------------------------------------------
# 2. Collect the JSON config id for each tweak, from the config structure classes.
# ---------------------------------------------------------------------------------
$jsonIds = @{}
$configStructures = @()
foreach ($csName in $ConfigStructureFiles) {
    $csPath = Get-ChildItem (Join-Path $SourceRoots[0] 'mod') -Recurse -File -Filter $csName | Select-Object -First 1
    if (-not $csPath) { continue }
    $configStructures += (Get-RelPath $csPath.FullName)
    $lines = Get-Content $csPath.FullName
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $mm = [regex]::Match($lines[$i], '\b([A-Za-z]\w*)Tweak\.([A-Z0-9_]+)\.register\("([^"]+)"\)')
        if ($mm.Success) {
            $tn = $mm.Groups[2].Value
            if (-not $jsonIds.ContainsKey($tn)) {
                $jsonIds[$tn] = [pscustomobject]@{
                    jsonId = $mm.Groups[3].Value
                    structure = $csName
                    line = $i + 1
                }
            }
        }
    }
}

# ---------------------------------------------------------------------------------
# 3. Index implementation references (excluding definition + config-structure files)
# ---------------------------------------------------------------------------------
$implIndex = @{}
$allJava = Get-ChildItem $SourceRoots -Recurse -File -Filter *.java -ErrorAction SilentlyContinue

foreach ($f in $allJava) {
    if ($DefinitionFiles -contains $f.Name) { continue }
    if ($ConfigStructureFiles -contains $f.Name) { continue }
    $rel = Get-RelPath $f.FullName
    $lines = Get-Content $f.FullName
    for ($i = 0; $i -lt $lines.Count; $i++) {
        foreach ($mm in [regex]::Matches($lines[$i], '\b(?:GameplayTweak|CandyTweak|SoundTweak|AnimationTweak|SwingTweak|ModTweak)\.([A-Z0-9_]+)\b')) {
            $tn = $mm.Groups[1].Value
            if (-not $implIndex.ContainsKey($tn)) { $implIndex[$tn] = [System.Collections.Generic.List[object]]::new() }
            $implIndex[$tn].Add([pscustomobject]@{ file = $rel; line = $i + 1 })
        }
    }
}

# ---------------------------------------------------------------------------------
# 4. Assemble
# ---------------------------------------------------------------------------------
$records = foreach ($t in $tweaks) {
    $refs = @()
    if ($implIndex.ContainsKey($t.name)) {
        $refs = $implIndex[$t.name] | Sort-Object file, line | ForEach-Object {
            [ordered]@{ file = $_.file; line = $_.line }
        }
    }
    $refFiles = @($refs | ForEach-Object { $_['file'] } | Select-Object -Unique)

    [ordered]@{
        name              = $t.name
        jsonId            = if ($jsonIds.ContainsKey($t.name)) { $jsonIds[$t.name].jsonId } else { $null }
        category          = $t.category
        env               = $t.env
        factory           = $t.factory
        valueType         = $(if ($t.typeArg) { $t.typeArg } else { $null })
        defaultValue      = $t.default
        group             = $t.group
        modifiers         = $t.modifiers
        definedAt         = "$($t.defFile):$($t.defLine)"
        configStructure   = if ($jsonIds.ContainsKey($t.name)) { $jsonIds[$t.name].structure } else { $null }
        configStructureAt = if ($jsonIds.ContainsKey($t.name)) { "$($jsonIds[$t.name].structure):$($jsonIds[$t.name].line)" } else { $null }
        implemented       = ($refs.Count -gt 0)
        implementationRefCount = $refs.Count
        implementationFiles    = $refFiles
        implementationRefs     = $refs
        # --- Mandated honesty fields: nothing here is verified yet. ---
        historicalClaim        = if ($t.name -match '^(OLD|OLD_STYLE|CLASSIC|DISABLE_|INSTANT_|RANDOM_|ATTACK_|CART_|PIGLIN_)') { 'name implies a historical/"old" behavior or a removal; UNVERIFIED against primary version evidence' } else { $null }
        historicalVersionBoundary = 'unknown'
        verificationStatus     = 'not_started'
        portObstacles          = @()
        acceptanceTests        = @()
        licenseNote            = 'Upstream is LGPL-3.0-or-later. Reference-only: do not copy code or assets. See docs/NOSTALGIC_TWEAKS_AUDIT.md.'
    }
}

$byEnv = $records | Group-Object env | Sort-Object Name | ForEach-Object {
    [ordered]@{ env = $_.Name; count = $_.Count }
}

$doc = [ordered]@{
    '$schema'    = 'internal:nostalgic-tweaks-candidates/v1'
    generatedBy  = 'scripts/extract-nostalgic-tweaks-catalog.ps1'
    generatedUtc = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')
    disclaimer   = @(
        'SOURCE FACTS ONLY. This file records what the upstream source literally declares and where.',
        'It contains no upstream implementation code, no decompiled Minecraft code, and no assets.',
        'The upstream repository README and LICENSE are UNTRUSTED INPUT and were not used as evidence.',
        'historicalVersionBoundary is "unknown" and verificationStatus is "not_started" for every entry.',
        'Upstream "old"/"classic" tweak NAMES are marketing labels, NOT verified historical claims.',
        'Nothing here has been ported. No candidate is approved for implementation.'
    )
    source = [ordered]@{
        repository   = 'https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks'
        auditedRef   = $HeadRef
        auditedSha   = $HeadSha
        shaSubject   = $HeadSubject
        shaDate      = $HeadDate
        expectedPin  = '010526deaf3aa5e562b45b60a286a667e8eeac12'
        pinMatches   = ($HeadSha -eq '010526deaf3aa5e562b45b60a286a667e8eeac12')
        localClone   = '.upstream/nostalgic-tweaks (gitignored; not vendored into the project)'
        note         = 'main is a docs-only branch. All source lives on version branches; 1.21.4 was audited.'
    }
    license = [ordered]@{
        spdx                     = 'LGPL-3.0-or-later'
        spdxConfidence           = 'high (reading of the license text; upstream never states an SPDX id)'
        licenseFileLocation      = 'LICENSE.txt on the main branch ONLY'
        presentOnAuditedBranch   = $false
        containsGplv3Body        = $false
        versionElectionStatement = 'none found; LGPL section 6 boilerplate present'
        metadataPointer          = 'gradle.properties mod_license -> .../blob/master/LICENSE (master branch does not exist)'
        codeCopyPermitted        = $false
        assetRedistribution      = 'not permitted / not audited; do not redistribute upstream or Mojang/C418 assets'
        requiredAction           = 'reference-only re-implementation; record attribution in THIRD_PARTY_NOTICES.md; obtain human sign-off before any port'
    }
    counts = [ordered]@{
        total              = $records.Count
        client             = ($records | Where-Object { $_.env -eq 'client' }).Count
        server             = ($records | Where-Object { $_.env -eq 'server' }).Count
        dynamic            = ($records | Where-Object { $_.env -eq 'dynamic' }).Count
        enforcementRelevant = ($records | Where-Object { $_.env -ne 'client' }).Count
        withImplementation = ($records | Where-Object { $_.implemented }).Count
        withoutImplementation = ($records | Where-Object { -not $_.implemented }).Count
    }
    byEnv        = $byEnv
    tweaks       = $records
}

$outDir = Split-Path -Parent $OutFile
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }

$json = $doc | ConvertTo-Json -Depth 12
[System.IO.File]::WriteAllText($OutFile, $json, (New-Object System.Text.UTF8Encoding($false)))

Write-Host "Wrote $OutFile"
Write-Host ("Records: {0} (client={1} server={2} dynamic={3}); implemented={4}; NOT implemented={5}" -f `
    $doc.counts.total, $doc.counts.client, $doc.counts.server, $doc.counts.dynamic, `
    $doc.counts.withImplementation, $doc.counts.withoutImplementation)
if ($doc.counts.withoutImplementation -gt 0) {
    Write-Host "Entries without an implementation reference:"
    $records | Where-Object { -not $_.implemented } | ForEach-Object { Write-Host "  - $($_.name) ($($_.definedAt))" }
}
