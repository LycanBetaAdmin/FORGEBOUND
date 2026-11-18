# Generates FORGEBOUND's metal items and block textures into src/main/resources/assets/forgebound/textures:
#   item/  raw_<metal> (a crystal cluster on grey stone) and <metal>_ingot (a rimmed bar with an emblem)
#   block/ <metal>_ore_overlay, the Forge Anvil (violet steel, gold bands, rune grid, glyph, gem) and the Rune Engraving
#          Table (stone, gold trim, runner, magic circle, banners, windows, plaque, amethyst, lantern, chain, books, pages, potions)
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

# ---- raw metal (vanilla raw-ore style): a cluster of chunky grey rock lumps, lit from the top left, with the metal
# showing through in patches at their lower edges and tips
$stone = @{ o = '#2A2A30'; d = '#5E5E66'; m = '#8E8E96'; l = '#B4B4BA'; h = '#D8D8DC' }
function Draw-Lump($b, [double]$cx, [double]$cy, [double]$rx, [double]$ry, $p) {
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $nx = ($x + 0.5 - $cx) / $rx; $ny = ($y + 0.5 - $cy) / $ry
            $d = [Math]::Sqrt($nx * $nx + $ny * $ny)
            if ($d -gt 1.0) { continue }
            $c = if ($d -gt 0.8) { $p.o } else {
                $s = $nx + $ny * 1.2
                if ($s -lt -0.75) { $p.h } elseif ($s -lt -0.2) { $p.l } elseif ($s -lt 0.45) { $p.m } else { $p.d }
            }
            Set-Px $b $x $y $c
        }
    }
}
foreach ($name in $metals.Keys) {
    $p = $metals[$name]; $b = New-Tex
    $metal = @{ o = $p.o; d = $p.d; m = $p.m; l = $p.l; h = $p.h }
    # back lumps first, the front lump last so its outline sits over them
    Draw-Lump $b 5.2 5.4 4.6 4.4 $stone
    Draw-Lump $b 11 6.2 4.2 4.4 $stone
    Draw-Lump $b 8 10.6 5.4 4.6 $stone
    # metal showing through at the lumps' lower edges and tips
    Draw-Lump $b 2.6 8.6 2.0 1.9 $metal
    Draw-Lump $b 13.2 9.4 2.0 2.0 $metal
    Draw-Lump $b 5.6 13.4 2.2 1.8 $metal
    Draw-Lump $b 11 13.2 2.0 1.8 $metal
    Draw-Lump $b 9.2 2.6 1.5 1.4 $metal
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
# runner (top face uses 12x16): plum cloth with a faint woven diamond, a dark inner line and gold hems down both sides;
# columns 12..15 are plain cloth for the thin edges
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 12; $x++) {
        $c = if ($x -eq 0 -or $x -eq 11) { '#E0A33A' } elseif ($x -eq 1 -or $x -eq 10) { '#33114F' }
             elseif (((($x + $y) % 6) -eq 0) -or ((($x - $y + 96) % 6) -eq 0)) { '#4E1E86' } else { '#46197A' }
        Set-Px $b $x $y $c
    }
    for ($x = 12; $x -lt 16; $x++) { Set-Px $b $x $y '#3E166C' }
}
Save-Tex $b $blockDir 'rune_table_runner'
# magic circle (glowing overlay, 10x10): an outer ring, a dashed inner ring, four rune marks and a star in the middle
$b = New-Tex
for ($y = 0; $y -lt 10; $y++) {
    for ($x = 0; $x -lt 10; $x++) {
        $dx = $x - 4.5; $dy = $y - 4.5; $d = [Math]::Sqrt($dx * $dx + $dy * $dy)
        if ($d -ge 3.9 -and $d -lt 4.9) { Set-Px $b $x $y '#C46CFF' }
        elseif ($d -ge 2.3 -and $d -lt 3.0 -and (($x + $y) % 2) -eq 0) { Set-Px $b $x $y '#9A40F0' }
    }
}
foreach ($pt in @(@(4, 1), @(5, 1), @(4, 8), @(5, 8), @(1, 4), @(1, 5), @(8, 4), @(8, 5))) { Set-Px $b $pt[0] $pt[1] '#F0C8FF' }
foreach ($pt in @(@(4, 3), @(5, 3), @(3, 4), @(6, 4), @(3, 5), @(6, 5), @(4, 6), @(5, 6))) { Set-Px $b $pt[0] $pt[1] '#E07CFF' }
foreach ($pt in @(@(4, 4), @(5, 4), @(4, 5), @(5, 5))) { Set-Px $b $pt[0] $pt[1] '#FFF4FF' }
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
# amethyst (vanilla amethyst palette and shard shapes, drawn at the table's pixel size; shown as crossed planes):
#   (0,0) 9x9 floating cluster, (9,0) 7x7 desk cluster, (9,8) 5x4 small bud, (14,8) 1x2 candle flame
$am = @{ h = '#FFFDD5'; t = '#FECBE6'; l = '#CFA0F3'; k = '#B38EF3'; m = '#8D6ACC'; d = '#7A5BB5'; x = '#64479E'; f = '#FFF2B0'; g = '#FFB347' }
$b = New-Tex
Write-Mask $b @(
    '....h....',
    '...tlm...',
    '...lkd...',
    '.t.lkd.t.',
    'tlmlkdtmd',
    '.lmlkdmd.',
    '.lklmdmd.',
    '..lmmdd..',
    '...mdx...') $am 0 0
Write-Mask $b @(
    '...h...',
    '..tlm..',
    't.lkd.t',
    'lmlkdmd',
    '.lkmdd.',
    '.lmmdx.',
    '..mdx..') $am 9 0
Write-Mask $b @(
    '..t..',
    '.tld.',
    'tlkmd',
    '.mdx.') $am 9 8
Write-Mask $b @('f', 'g') $am 14 8
Save-Tex $b $blockDir 'rune_table_crystal'
# books: (0,0) 3x4 violet cover, (4,0) 2x3 crimson cover, (8..15, 0..7) plain violet leather, (8..15, 8..15) plain
# crimson leather, (0,5) 4x2 violet spine and (6,5) 3x2 crimson spine with gold bands, (0,12) a row of page edges
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) { for ($x = 8; $x -lt 16; $x++) { Set-Px $b $x $y ($(if ($y -lt 8) { Get-Pick @('#5B1F7E', '#62228A', '#551C76') @(5, 2, 2) } else { Get-Pick @('#7A1E2A', '#842230', '#701A26') @(5, 2, 2) })) } }
Write-Mask $b @('dvv', 'dvv', 'dgv', 'dvv') @{ d = '#3E1258'; v = '#5B1F7E'; g = '#F2C04E' } 0 0
Write-Mask $b @('dr', 'dg', 'dr') @{ d = '#4E1018'; r = '#7A1E2A'; g = '#F2C04E' } 4 0
Write-Mask $b @('gvvg', 'gvvg') @{ v = '#4B186A'; g = '#E0A33A' } 0 5
Write-Mask $b @('grg', 'grg') @{ r = '#5E1620'; g = '#E0A33A' } 6 5
for ($x = 0; $x -lt 16; $x++) { Set-Px $b $x 12 ($(if (($x % 2) -eq 0) { '#F1E5D2' } else { '#DCCAA8' })) }
Save-Tex $b $blockDir 'rune_table_book'
# open book pages: (0,0) 3x3 left page and (3,0) 3x3 right page (violet script, a glowing rune, shaded gutter between),
# row 4 page edges
$b = New-Tex
Write-Mask $b @('ssq', 'ppq', 'ssq') @{ p = '#F1E5D2'; s = '#9A70C8'; q = '#D2BE98' } 0 0
Write-Mask $b @('qpp', 'qrp', 'qpp') @{ p = '#F1E5D2'; r = '#B44CF0'; q = '#D2BE98' } 3 0
for ($x = 0; $x -lt 16; $x++) { Set-Px $b $x 4 ($(if (($x % 2) -eq 0) { '#F1E5D2' } else { '#DCCAA8' })) }
Save-Tex $b $blockDir 'rune_table_pages'
# dark wood for corks and stands
$b = New-Tex; Fill-Noise $b @('#4A2E1E', '#5A3826', '#3E2618') @(5, 3, 2); Save-Tex $b $blockDir 'rune_table_wood'
# potions: (0,0) 3x3 violet body, (4,0) 2x2 violet, (7,0) 2x2 amber, (10,0) glass neck
$b = New-Tex
Write-Mask $b @('wlm', 'hmd', 'mdx') @{ w = '#E6C8FF'; l = '#C48CFF'; h = '#F3E2FF'; m = '#9A40F0'; d = '#7A28C8'; x = '#5A1AA8' } 0 0
Write-Mask $b @('hm', 'mx') @{ h = '#F3E2FF'; m = '#B44CF0'; x = '#6A20B8' } 4 0
Write-Mask $b @('hm', 'mx') @{ h = '#FFF2C8'; m = '#FFB347'; x = '#C9741E' } 7 0
Set-Px $b 10 0 '#D8C8F0'
Save-Tex $b $blockDir 'rune_table_potion'
'block textures written'
