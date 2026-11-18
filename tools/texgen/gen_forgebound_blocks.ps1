# Generates FORGEBOUND's metal items and block textures into src/main/resources/assets/forgebound/textures:
#   item/  raw_<metal> (a crystal cluster on grey stone) and <metal>_ingot (a rimmed bar with an emblem)
#   block/ <metal>_ore_overlay, the Forge Anvil (violet steel, gold bands, rune grid, glyph, gem) and the Rune Engraving
#          Table (stone, gold trim, runner, magic circle, banners, windows, plaque, crystal, lantern, chain, books, rack)
# Metals: Verdantite (green, gold rim, leaf), Infernalite (red, orange rim, flame), Voidsteel (violet, lavender rim, ring).
# Run: powershell -File tools\texgen\gen_forgebound_blocks.ps1
Add-Type -AssemblyName System.Drawing
$root = Join-Path $PSScriptRoot '..\..\src\main\resources\assets\forgebound\textures'
$itemDir = Join-Path $root 'item'; $blockDir = Join-Path $root 'block'
New-Item -ItemType Directory -Force $itemDir, $blockDir | Out-Null
$rng = New-Object System.Random 4747

function New-Tex([int]$w = 16, [int]$h = 16) { return New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function Get-Col([string]$hex, [int]$a = 255) {
    $v = [Convert]::ToInt32($hex.TrimStart('#'), 16)
    return [System.Drawing.Color]::FromArgb($a, ($v -shr 16) -band 255, ($v -shr 8) -band 255, $v -band 255)
}
function Set-Px($b, [int]$x, [int]$y, [string]$hex, [int]$a = 255) { if ($x -ge 0 -and $x -lt $b.Width -and $y -ge 0 -and $y -lt $b.Height) { $b.SetPixel($x, $y, (Get-Col $hex $a)) } }
function Save-Tex($b, [string]$dir, [string]$name) { $b.Save((Join-Path $dir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }
function Get-Pick([string[]]$cols, [int[]]$weights) {
    $n = $rng.Next(($weights | Measure-Object -Sum).Sum)
    for ($i = 0; $i -lt $cols.Count; $i++) { if ($n -lt $weights[$i]) { return $cols[$i] }; $n -= $weights[$i] }
    return $cols[-1]
}
function Write-Mask($b, [string[]]$mask, [hashtable]$pal, [int]$dx = 0, [int]$dy = 0) {
    for ($y = 0; $y -lt $mask.Count; $y++) {
        for ($x = 0; $x -lt $mask[$y].Length; $x++) {
            $ch = [string]$mask[$y][$x]
            if ($pal.ContainsKey($ch)) { Set-Px $b ($x + $dx) ($y + $dy) $pal[$ch] }
        }
    }
}
function Fill-Noise($b, [string[]]$cols, [int[]]$w) { for ($y = 0; $y -lt $b.Height; $y++) { for ($x = 0; $x -lt $b.Width; $x++) { Set-Px $b $x $y (Get-Pick $cols $w) } } }

# crystal palettes: outline, dark, mid, light, highlight; ingot: rim, rim shade, face, face dark, emblem
$metals = [ordered]@{
    verdantite  = @{ o = '#0B3A16'; d = '#16722E'; m = '#2FB04A'; l = '#7BEF7E'; h = '#E4FFD8'; rim = '#F2D04E'; rimD = '#B8932B'; face = '#2A9A3E'; faceD = '#17602A'; emb = '#F7DE6A' }
    infernalite = @{ o = '#4A0B04'; d = '#9A2108'; m = '#E2461A'; l = '#FF8A4A'; h = '#FFE0B0'; rim = '#FF9A3A'; rimD = '#C0601A'; face = '#B8321A'; faceD = '#70180C'; emb = '#FFC24A' }
    voidsteel   = @{ o = '#14082A'; d = '#4A1E8C'; m = '#8A44E6'; l = '#C48CFF'; h = '#F3E2FF'; rim = '#B98CF0'; rimD = '#7A50B8'; face = '#3A2A58'; faceD = '#211736'; emb = '#E05CFF' }
}

# ---- raw metal: three crystal shards rising from grey stones
function Draw-Shard($b, [int]$cx, [int]$top, [int]$bottom, [int]$half, $p) {
    for ($y = $top; $y -le $bottom; $y++) {
        $w = [Math]::Min($half, [Math]::Max(0, $y - $top))
        for ($x = $cx - $w; $x -le $cx + $w; $x++) {
            $c = if ($x -eq $cx - $w -or $x -eq $cx + $w -or $y -eq $top) { $p.o } elseif ($x -lt $cx) { $p.l } elseif ($x -eq $cx) { $p.m } else { $p.d }
            Set-Px $b $x $y $c
        }
    }
    Set-Px $b ($cx - 1) ($top + 2) $p.h
}
foreach ($name in $metals.Keys) {
    $p = $metals[$name]; $b = New-Tex
    Draw-Shard $b 4 4 12 2 $p
    Draw-Shard $b 8 1 12 2 $p
    Draw-Shard $b 12 5 12 2 $p
    $stones = @(
        '.ssttss..sstts..',
        'sttuuttssttuutss',
        'stuuuutttuuuuuts',
        '.sstuussstuusss.')
    Write-Mask $b $stones @{ s = '#3A3A40'; t = '#6E6E78'; u = '#9A9AA4' } 0 12
    Save-Tex $b $itemDir "raw_$name"
}

# ---- ingot: the vanilla ingot silhouette (a bar lying diagonally, rising to the right): o outline, e end face,
# t top face, r bright ridge along the top edge, f front face; shaded with each metal's palette
$ingot = @(
    '................',
    '................',
    '..........oo....',
    '.......oootto...',
    '....oooettttto..',
    '.oooettttttttro.',
    'oerttttttttrrtfo',
    'oerttttttrrrtfeo',
    'oeerttrrrtffffeo',
    'oeeerrtfffffeeeo',
    'ooeetfffffeefoo.',
    '.ooetfffffooo...',
    '..ooefffoo......',
    '...oooo.........',
    '................',
    '................')
foreach ($name in $metals.Keys) {
    $p = $metals[$name]; $b = New-Tex
    Write-Mask $b $ingot @{ o = $p.o; e = $p.m; t = $p.l; r = $p.h; f = $p.d }
    Save-Tex $b $itemDir "${name}_ingot"
}

# ---- ore overlays: small crystal clusters breaking through the stone (transparent elsewhere)
$clusters = @(@(3, 4), @(11, 3), @(6, 10), @(13, 11), @(1, 13))
foreach ($name in $metals.Keys) {
    $p = $metals[$name]; $b = New-Tex
    foreach ($c in $clusters) {
        $x = $c[0]; $y = $c[1]
        Set-Px $b $x ($y - 1) $p.h; Set-Px $b $x $y $p.l; Set-Px $b ($x + 1) $y $p.m; Set-Px $b $x ($y + 1) $p.m; Set-Px $b ($x + 1) ($y + 1) $p.d
        Set-Px $b ($x - 1) ($y + 1) $p.l; Set-Px $b ($x - 1) ($y + 2) $p.o; Set-Px $b $x ($y + 2) $p.o; Set-Px $b ($x + 1) ($y + 2) $p.o
        Set-Px $b ($x + 2) ($y + 1) $p.o
    }
    Save-Tex $b $blockDir "${name}_ore_overlay"
}

# ---- Forge Anvil: violet steel, gold, rune grid, glyph, gem
$b = New-Tex; Fill-Noise $b @('#3E3156', '#47395F', '#352A4A', '#54456E') @(5, 4, 3, 1)
for ($x = 0; $x -lt 16; $x++) { Set-Px $b $x 0 '#66588A' }
Save-Tex $b $blockDir 'forge_anvil_steel'
$b = New-Tex; Fill-Noise $b @('#2E2440', '#362B4B', '#2A2138', '#40345A') @(5, 4, 3, 1)
Save-Tex $b $blockDir 'forge_anvil_steel_dark'
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $c = if ($x -eq 0 -or $y -eq 0) { '#FFE08A' } elseif ($x -eq 15 -or $y -eq 15) { '#8C6420' } elseif (($x + $y) % 7 -eq 0) { '#FFD36A' } else { Get-Pick @('#E8AE3A', '#F2C04E', '#D89A2E') @(4, 3, 2) }
        Set-Px $b $x $y $c
    }
}
Save-Tex $b $blockDir 'forge_anvil_gold'
# rune grid for the top face (overlay): a frame, cross lines and a central circle with spokes
$b = New-Tex
$grid = @(
    '................',
    '.vvvvvv..vvvvvv.',
    '.v....v..v....v.',
    '.v....vvvv....v.',
    '.v...........v..',
    '.vv...pppp...vv.',
    '..v..pv..vp..v..',
    '..vvvp.hh.pvvv..',
    '..vvvp.hh.pvvv..',
    '..v..pv..vp..v..',
    '.vv...pppp...vv.',
    '.v............v.',
    '.v....vvvv....v.',
    '.v....v..v....v.',
    '.vvvvvv..vvvvvv.',
    '................')
Write-Mask $b $grid @{ v = '#A54CF0'; p = '#D27CFF'; h = '#FFE6FF' }
Save-Tex $b $blockDir 'forge_anvil_top_runes'
# glyph for the waist (overlay): a staff rune with a crossbar between two curved marks
$b = New-Tex
$glyph = @(
    '................',
    '.......h........',
    '......hph.......',
    '.p...p.p.p...p..',
    '.p....ppp....p..',
    'p......p......p.',
    'p......p......p.',
    'p.....ppp.....p.',
    '.p.....p.....p..',
    '.p.....p.....p..',
    '..p...hph...p...',
    '.......h........',
    '................',
    '................',
    '................',
    '................')
Write-Mask $b $glyph @{ p = '#D25CFF'; h = '#FFD8FF' }
Save-Tex $b $blockDir 'forge_anvil_glyph'
# gem: faceted amethyst
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $d = [Math]::Abs($x - 7.5) + [Math]::Abs($y - 7.5)
        $c = if ($d -lt 3) { '#F0C8FF' } elseif ($d -lt 6) { '#B45CFF' } elseif ($x + $y -lt 15) { '#8A3AE0' } else { '#5A1E9C' }
        Set-Px $b $x $y $c
    }
}
Save-Tex $b $blockDir 'forge_anvil_gem'

# ---- Rune Engraving Table
# stone: purple-grey dressed blocks, darker variant for the body
function New-Stone([string[]]$cols, [string]$mortar, [string]$lit, [string]$shade) {
    $b = New-Tex
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $row = [Math]::Floor($y / 8); $ly = $y % 8
            $joint = if ($row -eq 0) { @(7) } else { @(3, 11) }
            if ($ly -eq 7 -or $joint -contains $x) { Set-Px $b $x $y $mortar; continue }
            $c = Get-Pick $cols @(5, 4, 3)
            if ($ly -eq 0) { $c = $lit } elseif ($ly -eq 6) { $c = $shade }
            Set-Px $b $x $y $c
        }
    }
    return $b
}
Save-Tex (New-Stone @('#5E5474', '#685E80', '#564C6A') '#2E2638' '#80749A' '#463C58') $blockDir 'rune_table_stone'
Save-Tex (New-Stone @('#3A2E50', '#43365C', '#352A48') '#221A30' '#56487A' '#2E2440') $blockDir 'rune_table_stone_dark'
# gold trim (same as the anvil's)
Copy-Item (Join-Path $blockDir 'forge_anvil_gold.png') (Join-Path $blockDir 'rune_table_gold.png') -Force
# runner: purple cloth with gold hems along the sides
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { Set-Px $b $x $y (Get-Pick @('#4B1C7E', '#56228C', '#3E176A') @(5, 3, 2)) } }
for ($y = 0; $y -lt 16; $y++) { Set-Px $b 0 $y '#E0A33A'; Set-Px $b 15 $y '#C9862A' }
Save-Tex $b $blockDir 'rune_table_runner'
# magic circle (overlay): two rings, rune ticks, a triangle star in the middle
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $d = [Math]::Sqrt(($x - 7.5) * ($x - 7.5) + ($y - 7.5) * ($y - 7.5))
        if ($d -ge 6.6 -and $d -lt 7.5) { Set-Px $b $x $y '#D27CFF' }
        elseif ($d -ge 5.0 -and $d -lt 5.7 -and (($x + $y) % 3) -ne 0) { Set-Px $b $x $y '#A54CF0' }
    }
}
foreach ($pt in @(@(7, 3), @(8, 3), @(6, 5), @(9, 5), @(5, 7), @(10, 7), @(4, 9), @(5, 9), @(6, 9), @(7, 9), @(8, 9), @(9, 9), @(10, 9), @(11, 9), @(7, 6), @(8, 6))) { Set-Px $b $pt[0] $pt[1] '#F0C8FF' }
Set-Px $b 7 7 '#FFFFFF'; Set-Px $b 8 7 '#FFFFFF'
Save-Tex $b $blockDir 'rune_table_circle'
# banner (overlay): purple cloth, gold border, glowing rune, swallow-tail point at the bottom
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    $cut = [Math]::Max(0, $y - 11)
    for ($x = 1 + $cut; $x -le 14 - $cut; $x++) {
        $edge = ($x -eq 1 + $cut -or $x -eq 14 - $cut -or $y -eq 0)
        Set-Px $b $x $y ($(if ($edge) { '#E0A33A' } else { Get-Pick @('#4B1C7E', '#56228C', '#3E176A') @(5, 3, 2) }))
    }
}
foreach ($pt in @(@(7, 3), @(8, 3), @(7, 4), @(8, 4), @(7, 5), @(8, 5), @(7, 6), @(8, 6), @(7, 7), @(8, 7), @(7, 8), @(8, 8), @(5, 5), @(6, 5), @(9, 5), @(10, 5), @(6, 4), @(9, 4), @(6, 9), @(9, 9))) { Set-Px $b $pt[0] $pt[1] '#F0A8FF' }
Save-Tex $b $blockDir 'rune_table_banner'
# window glow
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $d = [Math]::Max([Math]::Abs($x - 7.5), [Math]::Abs($y - 7.5))
        Set-Px $b $x $y ($(if ($d -gt 6.6) { '#3B1C5A' } elseif ($d -gt 4.5) { '#8A35D8' } elseif ($d -gt 2) { '#C46CFF' } else { '#F0C8FF' }))
    }
}
Save-Tex $b $blockDir 'rune_table_window'
# plaque: gold octagon with a dark centre and a glowing rune (cutout)
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $dx = [Math]::Min($x, 15 - $x); $dy = [Math]::Min($y, 15 - $y)
        if ($dx + $dy -lt 3) { continue }
        $inner = ($dx -ge 2 -and $dy -ge 2 -and $dx + $dy -ge 5)
        Set-Px $b $x $y ($(if ($inner) { '#241A36' } elseif ($x + $y -lt 15) { '#F2C04E' } else { '#B8862B' }))
    }
}
foreach ($pt in @(@(7, 4), @(8, 4), @(7, 5), @(8, 5), @(7, 6), @(8, 6), @(7, 7), @(8, 7), @(7, 8), @(8, 8), @(7, 9), @(8, 9), @(7, 10), @(8, 10), @(5, 6), @(6, 6), @(9, 6), @(10, 6), @(6, 11), @(9, 11))) { Set-Px $b $pt[0] $pt[1] '#E07CFF' }
Save-Tex $b $blockDir 'rune_table_plaque'
# crystal facets
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $c = if ($x -lt 4) { '#B866FF' } elseif ($x -lt 8) { '#9A40F0' } elseif ($x -lt 12) { '#7A28D0' } else { '#5A1AA8' }
        if ((($x + 2 * $y) % 11) -eq 0) { $c = '#E6B8FF' }
        Set-Px $b $x $y $c
    }
}
Save-Tex $b $blockDir 'rune_table_crystal'
# lantern: violet glass (0..5 x 0..6) in a dark iron frame, iron plate (8..15 x 0..1), cap (8..11 x 2..4)
$b = New-Tex
for ($y = 0; $y -lt 7; $y++) {
    for ($x = 0; $x -lt 6; $x++) {
        if ($x -eq 0 -or $x -eq 5 -or $y -eq 0 -or $y -eq 6) { Set-Px $b $x $y '#2A2234' }
        else { $d = [Math]::Abs($x - 2.5) + [Math]::Abs($y - 3.5) * 0.7; Set-Px $b $x $y ($(if ($d -lt 1.2) { '#FFE6FF' } elseif ($d -lt 2) { '#E07CFF' } else { '#A84CF0' })) }
    }
}
for ($x = 8; $x -lt 16; $x++) { Set-Px $b $x 0 '#4A4058'; Set-Px $b $x 1 '#2A2234' }
for ($x = 8; $x -lt 12; $x++) { for ($y = 2; $y -lt 5; $y++) { Set-Px $b $x $y ($(if ($y -eq 2) { '#5A4E6E' } else { '#2A2234' })) } }
Save-Tex $b $blockDir 'rune_table_lantern'
# chain: gold links, transparent between them
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    $ph = $y % 4
    if ($ph -eq 0 -or $ph -eq 3) { Set-Px $b 6 $y '#B8862B'; Set-Px $b 7 $y '#F2C04E'; Set-Px $b 8 $y '#F2C04E'; Set-Px $b 9 $y '#B8862B' }
    else { Set-Px $b 6 $y '#B8862B'; Set-Px $b 9 $y '#8C6420' }
}
Save-Tex $b $blockDir 'rune_table_chain'
# books: violet leather with gold bands; pages: cream with violet script
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { Set-Px $b $x $y (Get-Pick @('#5B1F7E', '#6A268F', '#4B186A') @(5, 3, 2)) } }
foreach ($bx in @(3, 12)) { for ($y = 0; $y -lt 16; $y++) { Set-Px $b $bx $y '#E0A33A' } }
Save-Tex $b $blockDir 'rune_table_book'
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { Set-Px $b $x $y ($(if (($x % 2) -eq 0) { '#F1E5D2' } else { '#EADBC4' })) } }
foreach ($ry in @(3, 5, 7, 10, 12)) { for ($x = 2; $x -lt 14; $x++) { if ((($x * 7 + $ry) % 5) -ne 0 -and $x -ne 7 -and $x -ne 8) { Set-Px $b $x $ry '#8A55C8' } } }
for ($y = 0; $y -lt 16; $y++) { Set-Px $b 7 $y '#C9B48E'; Set-Px $b 8 $y '#D8C6A4' }
Save-Tex $b $blockDir 'rune_table_pages'
# dark wood for the potion corks, glass for the potions
$b = New-Tex; Fill-Noise $b @('#4A2E1E', '#5A3826', '#3E2618') @(5, 3, 2); Save-Tex $b $blockDir 'rune_table_wood'
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { Set-Px $b $x $y ($(if ($x -lt 3) { '#E6A8FF' } elseif ($x -lt 9) { '#B44CF0' } else { '#7A28C8' })) } }
Save-Tex $b $blockDir 'rune_table_potion'
'block textures written'
