# Generates FORGEBOUND's metal items and block textures into src/main/resources/assets/forgebound/textures:
#   (raw metals and ingots now come from gen_forgebound_refs.ps1)
#   block/ <metal>_ore_overlay, the Forge Anvil (violet steel, gold bands, rune grid, glyph, gem) and the Rune Engraving
#          Table (stone, gold trim, runner, drape, magic circle, glow windows, plaque, amethyst, lantern, chain, books, pages, potions)
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
# drape (12x12, hangs over the front edge): purple cloth with gold edges, two soft folds, a glowing rune and a
# pointed hem
$b = New-Tex
for ($y = 0; $y -lt 12; $y++) {
    $cut = [Math]::Max(0, $y - 8) * 2
    for ($x = $cut; $x -le 11 - $cut; $x++) {
        $edge = ($x -eq $cut -or $x -eq 11 - $cut -or $y -eq 0 -or ($y -ge 9 -and ($x -le $cut + 1 -or $x -ge 10 - $cut)))
        Set-Px $b $x $y ($(if ($edge) { if ($x -lt 6) { '#F2C04E' } else { '#D99A2E' } } elseif ($x -eq 3 -or $x -eq 8) { '#3E176A' } elseif ($x -eq 4 -or $x -eq 9) { '#55248E' } else { '#4B1C7E' }))
    }
}
Write-Mask $b @(
    '..rr..',
    '.r..r.',
    'r.hh.r',
    '.r..r.',
    '..rr..',
    '..rr..',
    '.rrrr.') @{ r = '#E07CFF'; h = '#FFE6FF' } 3 2
Save-Tex $b $blockDir 'rune_table_drape'
# glow: (0,0) 4x6 arched window in a gold frame, (5,0) 2x9 pillar window
$b = New-Tex
Write-Mask $b @('.gg.', 'gllg', 'ghhg', 'ghlg', 'gmmg', 'gggg') @{ g = '#D99A2E'; h = '#F6D8FF'; l = '#E07CFF'; m = '#A84CF0' } 0 0
Write-Mask $b @('mm', 'll', 'hh', 'hh', 'hh', 'hh', 'll', 'mm', 'dd') @{ m = '#A84CF0'; l = '#E07CFF'; h = '#F6D8FF'; d = '#6A2AB0' } 5 0
Save-Tex $b $blockDir 'rune_table_glow'
# plaque (8x8): a gold octagon with a dark face and a glowing rune ring
$b = New-Tex
Write-Mask $b @(
    '..gggg..',
    '.gyyyyg.',
    'gykrrkyg',
    'gyrhhryg',
    'gyrhhryg',
    'gykrrkyg',
    '.gyyyyg.',
    '..gggg..') @{ g = '#8C6420'; y = '#F2C04E'; k = '#241A36'; r = '#E07CFF'; h = '#FFE6FF' } 0 0
Save-Tex $b $blockDir 'rune_table_plaque'
# smooth gem (every crystal on the table): a soft light-to-deep violet gradient across each face with a bright edge
# line on the lit side and a faint glowing band through the middle
$b = New-Tex
$stops = @(@(0, 244, 226, 255), @(0.2, 220, 176, 255), @(0.55, 168, 104, 240), @(0.85, 124, 50, 198), @(1, 92, 30, 160))
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $t = $x / 15; $k = 0; while ($t -gt $stops[$k + 1][0]) { $k++ }
        $f = ($t - $stops[$k][0]) / ($stops[$k + 1][0] - $stops[$k][0])
        $glowRow = [Math]::Max(0, 1 - [Math]::Abs($y - 7.5) / 4) * 22
        $c = 1..3 | ForEach-Object { [int][Math]::Min(255, $stops[$k][$_] + ($stops[$k + 1][$_] - $stops[$k][$_]) * $f + $glowRow) }
        if ($x -eq 1) { $c = @(255, 246, 255) }
        $b.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $c[0], $c[1], $c[2]))
    }
}
Save-Tex $b $blockDir 'rune_table_gem'
# books: (0,0) 4x5 violet cover, (4,0) 3x4 crimson cover, (0,6) 5x2 violet spine, (0,9) 4x2 crimson spine,
# (8..15, 0..7) violet leather, (8..15, 8..13) crimson leather, row 15 page edges
$b = New-Tex
for ($y = 0; $y -lt 14; $y++) { for ($x = 8; $x -lt 16; $x++) { Set-Px $b $x $y ($(if ($y -lt 8) { Get-Pick @('#5B1F7E', '#62228A', '#551C76') @(5, 2, 2) } else { Get-Pick @('#7A1E2A', '#842230', '#701A26') @(5, 2, 2) })) } }
Write-Mask $b @('dvvv', 'dvgv', 'dggg', 'dvgv', 'dvvv') @{ d = '#3E1258'; v = '#5B1F7E'; g = '#F2C04E' } 0 0
Write-Mask $b @('drr', 'drg', 'drr', 'drr') @{ d = '#4E1018'; r = '#7A1E2A'; g = '#F2C04E' } 4 0
Write-Mask $b @('gvvvg', 'gvvvg') @{ v = '#4B186A'; g = '#E0A33A' } 0 6
Write-Mask $b @('grrg', 'grrg') @{ r = '#5E1620'; g = '#E0A33A' } 0 9
for ($x = 0; $x -lt 16; $x++) { Set-Px $b $x 15 ($(if (($x % 2) -eq 0) { '#F1E5D2' } else { '#DCCAA8' })) }
Save-Tex $b $blockDir 'rune_table_book'
# open book pages: (0,0) 3x6 left page and (3,0) 3x6 right page, row 6 page edges
$b = New-Tex
Write-Mask $b @('ssq', 'ppq', 'ssq', 'ppq', 'spq', 'ppq') @{ p = '#F1E5D2'; s = '#9A70C8'; q = '#D2BE98' } 0 0
Write-Mask $b @('qpp', 'qrp', 'qrr', 'qpp', 'qss', 'qpp') @{ p = '#F1E5D2'; r = '#B44CF0'; s = '#9A70C8'; q = '#D2BE98' } 3 0
for ($x = 0; $x -lt 16; $x++) { Set-Px $b $x 6 ($(if (($x % 2) -eq 0) { '#F1E5D2' } else { '#DCCAA8' })) }
Save-Tex $b $blockDir 'rune_table_pages'
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
