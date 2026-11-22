# Rebuild the README contact sheet from the actual item textures (8x nearest-neighbor).
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path $PSScriptRoot -Parent
$assetPath = 'assets/sophisticated_backpacks_digital_storages_expansion/textures/item'
$shared = Join-Path $root "shared/src/main/resources/$assetPath"
$integrated = Join-Path $root "targets/26.1.2/neoforge/src/main/resources/$assetPath"
$states = '', '_linked_ae2', '_linked_refinedstorage', '_linked_toms_storage', '_linked_scc', '_linked_integrateddynamics'
$scale = 8
$size = 16 * $scale
$gap = 24
$sheet = [System.Drawing.Bitmap]::new($states.Count * $size + ($states.Count - 1) * $gap, $size)
try {
    for ($index = 0; $index -lt $states.Count; $index++) {
        $folder = if ($states[$index] -eq '_linked_integrateddynamics') { $integrated } else { $shared }
        $sprite = [System.Drawing.Bitmap]::new((Join-Path $folder "remote_feeding_upgrade$($states[$index]).png"))
        try {
            if ($sprite.Width -ne 16 -or $sprite.Height -ne 16) { throw 'Expected a 16x16 item texture' }
            for ($y = 0; $y -lt $size; $y++) {
                for ($x = 0; $x -lt $size; $x++) {
                    $color = $sprite.GetPixel([int][Math]::Floor($x / $scale), [int][Math]::Floor($y / $scale))
                    $sheet.SetPixel($index * ($size + $gap) + $x, $y, $color)
                }
            }
        } finally { $sprite.Dispose() }
    }
    $sheet.Save((Join-Path $root 'docs/gallery/upgrade_link_states.png'), [System.Drawing.Imaging.ImageFormat]::Png)
} finally { $sheet.Dispose() }
