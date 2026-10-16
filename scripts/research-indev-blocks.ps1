# Minecraft Historical Experience
# Copyright (C) 2026 Minecraft Historical Experience contributors
#
# This program is free software: you can redistribute it and/or modify it under the terms of
# the GNU Lesser General Public License as published by the Free Software Foundation, either
# version 3 of the License, or (at your option) any later version.
#
# SPDX-License-Identifier: LGPL-3.0-or-later
#
# Researches what blocks existed in the Indev line, from minecraft.wiki's own per-build pages.
#
# WHY A SCRIPT: the answer to "did Indev 20100223 have this block?" is the union of the blocks every
# Indev build ADDED, minus the ones it removed. The wiki records that per build, in each version
# page's "Additions > Blocks" and "Changes > Blocks" sections, so the cumulative set has to be
# assembled from every page rather than read off one of them. Doing it by hand is how a table ends up
# inheriting an era's block set by assumption, which is exactly what this run exists to replace.
#
# Output: research/indev-block-availability.json, with the per-build source URLs, the raw wikitext
# bullets, and the resulting added/removed/available sets.

param(
    [string]$OutFile = "research/indev-block-availability.json",
    [string]$UserAgent = "historical-experience-research/0.1 (block availability sweep)"
)

$ErrorActionPreference = 'Stop'
$api = 'https://minecraft.wiki/api.php'

function Get-Wikitext([string]$Page) {
    $uri = "${api}?action=parse&page=$([uri]::EscapeDataString($Page))&prop=wikitext&format=json&formatversion=2&redirects=1"
    $response = Invoke-WebRequest -Uri $uri -UseBasicParsing -TimeoutSec 60 -Headers @{ 'User-Agent' = $UserAgent }
    $json = $response.Content | ConvertFrom-Json
    return $json.parse.wikitext
}

# ---- 1. the index of Indev builds, from the phase page's own version list -------------------------
$listUri = "${api}?action=query&list=allpages&apprefix=Java%20Edition%20Indev&aplimit=500&format=json&formatversion=2"
$listResponse = Invoke-WebRequest -Uri $listUri -UseBasicParsing -TimeoutSec 60 -Headers @{ 'User-Agent' = $UserAgent }
$pages = ($listResponse.Content | ConvertFrom-Json).query.allpages |
    ForEach-Object { $_.title } |
    Where-Object { $_ -match 'Indev\s+(0\.31|20\d{6})|Indev_?(0\.31|20\d{6})' } |
    Select-Object -Unique
Write-Output "prefix search found $($pages.Count) dated Indev build pages"

# The wiki's own list of block pages, so a bullet's links can be checked against it rather than
# guessed from capitalisation.
$catUri = "${api}?action=query&list=categorymembers&cmtitle=Category%3ABlocks&cmlimit=500&format=json&formatversion=2"
$catResponse = Invoke-WebRequest -Uri $catUri -UseBasicParsing -TimeoutSec 60 -Headers @{ 'User-Agent' = $UserAgent }
$blockTitles = @{}
foreach ($member in ($catResponse.Content | ConvertFrom-Json).query.categorymembers) {
    $blockTitles[$member.title.ToLowerInvariant()] = $true
}
Write-Output "wiki Blocks category: $($blockTitles.Count) titles"

$added = [ordered]@{}
$removed = [ordered]@{}
$sources = [ordered]@{}

foreach ($page in $pages) {
    $text = Get-Wikitext $page
    if (-not $text) { continue }
    $section = ''
    foreach ($line in ($text -split "`n")) {
        $trimmed = $line.Trim()
        if ($trimmed -match '^={2,4}\s*(.+?)\s*={2,4}$') { $section = $Matches[1]; continue }
        if ($trimmed -notmatch '^\*') { continue }
        if ($section -notmatch 'Blocks?') { continue }
        # The bullet's block names are its wiki links; the prose around them says added/removed.
        $names = [regex]::Matches($trimmed, '\[\[([^\]\|#]+)') | ForEach-Object { $_.Groups[1].Value.Trim() }
        if ($names.Count -eq 0) { continue }
        $isRemoval = $trimmed -match '(?i)\b(removed|no longer|replaced)\b'
        foreach ($name in $names) {
            if (-not $blockTitles.ContainsKey($name.ToLowerInvariant())) { continue }
            $bucket = if ($isRemoval) { $removed } else { $added }
            if (-not $bucket.Contains($name)) { $bucket[$name] = @() }
            if ($bucket[$name] -notcontains $page) { $bucket[$name] += $page }
            if (-not $sources.Contains($page)) { $sources[$page] = "${api}?action=parse&page=$([uri]::EscapeDataString($page))" }
        }
    }
}

$available = [ordered]@{}
foreach ($name in $added.Keys) {
    if ($removed.Contains($name)) { continue }
    $available[$name] = $added[$name]
}

$result = [ordered]@{
    note = 'Blocks the Indev line added and removed, per minecraft.wiki build pages. "available" is added minus removed and is the cumulative set for the last Indev build.'
    retrieved = (Get-Date -Format 'yyyy-MM-dd')
    pagesRead = $pages.Count
    sources = $sources
    added = $added
    removed = $removed
    available = $available
}
($result | ConvertTo-Json -Depth 6) | Set-Content -Path $OutFile -Encoding UTF8

Write-Output "pages read: $($pages.Count)"
Write-Output "blocks added somewhere in Indev: $($added.Count)"
Write-Output "blocks removed somewhere in Indev: $($removed.Count)"
Write-Output "cumulative available: $($available.Count)"
Write-Output '--- available ---'
$available.Keys | Sort-Object | ForEach-Object { "  $_" }
Write-Output '--- removed ---'
$removed.Keys | Sort-Object | ForEach-Object { "  $_" }
Write-Output "written: $OutFile"
