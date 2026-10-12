# Extracts the three preset/world-preset JSON files that the evidence file and docs quote, straight out of the
# published jar's data, and prints their sha256. Run from the workspace root:
#   pwsh -NoProfile -File research\dev\extract-preset-json.ps1
# Read-only: it prints and hashes, it does not write project files.
$ErrorActionPreference = 'Stop'
$jar = Get-ChildItem ".upstream\dist\*.jar" | Select-Object -First 1
if (-not $jar) { throw "No jar in .upstream/dist" }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
$targets = @(
  "data/moderner_beta/moderner_beta/settings_preset/beta.json",
  "data/moderner_beta/moderner_beta/settings_preset/beta_1_8_1.json",
  "data/moderner_beta/worldgen/world_preset/moderner_beta.json"
)
foreach ($t in $targets) {
  $e = $z.Entries | Where-Object { $_.FullName -eq $t }
  if (-not $e) { "MISSING $t"; continue }
  $sr = New-Object System.IO.StreamReader($e.Open()); $text = $sr.ReadToEnd(); $sr.Dispose()
  $bytes = [System.Text.Encoding]::UTF8.GetBytes($text)
  $sha = [System.Security.Cryptography.SHA256]::Create().ComputeHash($bytes)
  "=== $t  ($($e.Length) bytes, sha256 $(([BitConverter]::ToString($sha) -replace '-','').ToLower())) ==="
  $text
}
$z.Dispose()
