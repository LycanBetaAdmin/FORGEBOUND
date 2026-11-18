# Generates FORGEBOUND's Sundered effect icon. All item icons now come from gen_forgebound_refs.ps1; the Forge Anvil,
# ores and the Rune Engraving Table from gen_forgebound_blocks.ps1.
# Run: powershell -File tools\texgen\gen_forgebound.ps1
Add-Type -AssemblyName System.Drawing
$root = Join-Path $PSScriptRoot '..\..\src\main\resources\assets\forgebound\textures'
$itemDir = Join-Path $root 'item'; $blockDir = Join-Path $root 'block'
New-Item -ItemType Directory -Force $itemDir, $blockDir | Out-Null
$rng = New-Object System.Random 9090

function New-Tex { return New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function Get-Col([string]$hex) {
    $v = [Convert]::ToInt32($hex.TrimStart('#'), 16)
    return [System.Drawing.Color]::FromArgb(255, ($v -shr 16) -band 255, ($v -shr 8) -band 255, $v -band 255)
}
function Set-Px($b, [int]$x, [int]$y, [string]$hex) { if ($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16) { $b.SetPixel($x, $y, (Get-Col $hex)) } }
function Save-Tex($b, [string]$dir, [string]$name) { $b.Save((Join-Path $dir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }
function Get-Pick([string[]]$cols, [int[]]$weights) {
    $n = $rng.Next(($weights | Measure-Object -Sum).Sum)
    for ($i = 0; $i -lt $cols.Count; $i++) { if ($n -lt $weights[$i]) { return $cols[$i] }; $n -= $weights[$i] }
    return $cols[-1]
}
# draws a 16-row mask; letters pick from the palette: o outline, d dark, m mid, l light, h highlight
function Write-Mask($b, [string[]]$mask, [hashtable]$pal, [int]$dy = 0) {
    for ($y = 0; $y -lt $mask.Count; $y++) {
        $row = $mask[$y]
        for ($x = 0; $x -lt [Math]::Min(16, $row.Length); $x++) {
            $ch = [string]$row[$x]
            if ($pal.ContainsKey($ch)) { Set-Px $b $x ($y + $dy) $pal[$ch] }
        }
    }
}

$pals = @{
    verdantite  = @{ o = '#0B2A15'; d = '#17552A'; m = '#26893F'; l = '#4BC46A'; h = '#B9F7C9' }
    infernalite = @{ o = '#3A0A04'; d = '#7A1B08'; m = '#C43A14'; l = '#FF7A33'; h = '#FFD49A' }
    voidsteel   = @{ o = '#08060F'; d = '#231A3A'; m = '#3F3168'; l = '#7C66C6'; h = '#DCD0FF' }
}


# ---- Sundered effect icon (18 x 18): a cracked steel plate
$effDir = Join-Path $root 'mob_effect'; New-Item -ItemType Directory -Force $effDir | Out-Null
$b = New-Object System.Drawing.Bitmap 18, 18, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
for ($y = 3; $y -lt 15; $y++) { for ($x = 3; $x -lt 15; $x++) { $b.SetPixel($x, $y, (Get-Col ($(if ($x -eq 3 -or $y -eq 3) { '#B8B0C4' } elseif ($x -eq 14 -or $y -eq 14) { '#4A4456' } else { '#8A8296' })))) } }
foreach ($p in @(@(9, 3), @(9, 4), @(8, 5), @(8, 6), @(9, 7), @(10, 8), @(10, 9), @(9, 10), @(8, 11), @(8, 12), @(7, 13), @(7, 14), @(11, 9), @(12, 10))) { $b.SetPixel($p[0], $p[1], (Get-Col '#2A2333')) }
$b.Save((Join-Path $effDir 'sundered.png'), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose()

'textures written'
