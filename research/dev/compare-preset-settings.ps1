# Temporary: compare key chunk-settings components across presets (deleted after use).
$ext = (Resolve-Path ".upstream\extracted").Path
$dir = Join-Path $ext "data\moderner_beta\moderner_beta\settings_preset"
$names = @("beta.json","beta_1_8_1.json","beta_1_9_pre_3.json","alpha.json","infdev_415.json","release_1_6_4.json","release_1_12_2.json","indev.json","classic_0_30.json")
$keys = @(
 "moderner_beta:provider",
 "moderner_beta:noise_generator_settings",
 "moderner_beta:use_surface_rules",
 "moderner_beta:deepslate_generation",
 "moderner_beta:cave_generation",
 "moderner_beta:noise_3d_settings",
 "moderner_beta:structure_modifiers",
 "moderner_beta:sea_level",
 "moderner_beta:surface_properties",
 "moderner_beta:perlin_noise_settings"
)
foreach ($n in $names) {
  $o = Get-Content (Join-Path $dir $n) -Raw | ConvertFrom-Json
  "=== $n ==="
  foreach ($k in $keys) {
    $v = $o.chunkSettings.$k
    if ($null -eq $v) { "   {0,-42} <absent -> codec default>" -f $k }
    else { "   {0,-42} {1}" -f $k, (($v | ConvertTo-Json -Compress -Depth 6)) }
  }
  "   biomeSettings keys: " + (($o.biomeSettings.PSObject.Properties.Name) -join ', ')
  "   caveBiomeSettings keys: " + (($o.caveBiomeSettings.PSObject.Properties.Name) -join ', ')
}
