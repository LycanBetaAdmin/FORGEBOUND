# Generates FORGEBOUND's small item icons (Arcane Shard, Crystal Dust, Ascension Core, Fortune Dice, Soul Essence,
# Fortune Crystal, Sealing Rune) and the Sundered effect icon. Ores, ingots, the Forge Anvil and the Rune Engraving Table
# come from gen_forgebound_blocks.ps1.
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


# ---- Arcane Shard: a tall blue crystal with a small one beside it
$shard = @(
    '................',
    '..........oo....',
    '.........ohho...',
    '........ohllo...',
    '.......ohlmmo...',
    '......ohlmmdo...',
    '.....ohlmmdo....',
    '....ohlmmdo.....',
    '...ohlmmdo..oo..',
    '..ohlmmdo..ohlo.',
    '..olmmdo...olmo.',
    '..odddo.....odo.',
    '...ooo.......o..',
    '................',
    '................',
    '................')
$b = New-Tex; Write-Mask $b $shard @{ o = '#0B2350'; d = '#1B4F99'; m = '#2E86E0'; l = '#7FC8FF'; h = '#F2FBFF' }; Save-Tex $b $itemDir 'arcane_shard'

# ---- Crystal Dust: a violet heap with sparkles
$dust = @(
    '................',
    '................',
    '................',
    '..........h.....',
    '.....h..........',
    '................',
    '........o.......',
    '......oomoo.....',
    '.....omlhlmo....',
    '....omlhllmmo...',
    '...ommlllmmddo..',
    '..ommmmmmmddddo.',
    '.oddmmmmdddddddo',
    '.oooooooooooooo.',
    '................',
    '................')
$b = New-Tex; Write-Mask $b $dust @{ o = '#2A1240'; d = '#5B2A8C'; m = '#8C4FD0'; l = '#C08CF5'; h = '#F6E8FF' }; Save-Tex $b $itemDir 'crystal_dust'

# ---- Ascension Core: a glowing orb in a gold setting with four prongs
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $d = [Math]::Sqrt(($x - 7.5) * ($x - 7.5) + ($y - 7.5) * ($y - 7.5))
        if ($d -lt 1.6) { Set-Px $b $x $y '#FFFFFF' }
        elseif ($d -lt 2.8) { Set-Px $b $x $y '#B8F4FF' }
        elseif ($d -lt 4.0) { Set-Px $b $x $y '#5FD0F5' }
        elseif ($d -lt 4.9) { Set-Px $b $x $y '#7A4FD8' }
        elseif ($d -lt 5.9) { if ($x + $y -lt 15) { Set-Px $b $x $y '#F2C04E' } else { Set-Px $b $x $y '#B8862B' } }
        elseif ($d -lt 6.6) { Set-Px $b $x $y '#5A3D10' }
    }
}
foreach ($p in @(@(7, 0), @(8, 0), @(7, 15), @(8, 15), @(0, 7), @(0, 8), @(15, 7), @(15, 8))) { Set-Px $b $p[0] $p[1] '#F2C04E' }
foreach ($p in @(@(7, 1), @(8, 1), @(7, 14), @(8, 14), @(1, 7), @(1, 8), @(14, 7), @(14, 8))) { Set-Px $b $p[0] $p[1] '#B8862B' }
Set-Px $b 6 5 '#FFFFFF'; Set-Px $b 5 6 '#E8FCFF'
Save-Tex $b $itemDir 'ascension_core'

# ---- Fortune Dice: an ivory die with gold edges, seen corner-on (top face and two sides with pips)
$die = @(
    '................',
    '..oooooooooooo..',
    '.ohhhhhhhhhhhlo.',
    '.ohpphhhhhhpplo.',
    '.ohpphhhhhhpplo.',
    '.ohhhhhhhhhhhlo.',
    '.ohhhhhpphhhhlo.',
    '.ohhhhhpphhhhlo.',
    '.ohhhhhhhhhhhlo.',
    '.ohpphhhhhhpplo.',
    '.ohpphhhhhhpplo.',
    '.ohhhhhhhhhhhlo.',
    '.olllllllllllmo.',
    '..oooooooooooo..',
    '................',
    '................')
$b = New-Tex; Write-Mask $b $die @{ o = '#8C6420'; h = '#FFF6DC'; l = '#DCCBA0'; m = '#B8A478'; p = '#B8862B' }
Save-Tex $b $itemDir 'fortune_dice'

# ---- Soul Essence: a pale teal wisp curling up from a glowing core
$wisp = @(
    '................',
    '.........hh.....',
    '........hlh.....',
    '.......hll......',
    '......hlm.......',
    '......lmm..h....',
    '.....lmmd.hl....',
    '....lmmdd.lm....',
    '....mmdddlmm....',
    '...lmdddddmml...',
    '...mddhhhddmm...',
    '...mddhhhddml...',
    '....mddhhddm....',
    '.....mmdddm.....',
    '......mmmm......',
    '................')
$b = New-Tex; Write-Mask $b $wisp @{ d = '#1F8C8C'; m = '#3FC4C0'; l = '#9CF0EA'; h = '#F2FFFD' }; Save-Tex $b $itemDir 'soul_essence'

# ---- Fortune Crystal: a faceted rose-gold gem with a bright glint
$gem = @(
    '................',
    '................',
    '.....oooooo.....',
    '....ohhllmmo....',
    '...ohhllmmmdo...',
    '..oooooooooooo..',
    '..ohhlllmmmmdo..',
    '...ohllmmmmdo...',
    '....ohlmmmdo....',
    '.....ohmmdo.....',
    '......omdo......',
    '.......oo.......',
    '................',
    '................',
    '................',
    '................')
$b = New-Tex; Write-Mask $b $gem @{ o = '#7A3A1A'; d = '#C2603A'; m = '#F09A6A'; l = '#FFD08A'; h = '#FFF4D6' } 1; Set-Px $b 5 5 '#FFFFFF'; Save-Tex $b $itemDir 'fortune_crystal'

# ---- Sealing Rune: a dark stone tablet with a glowing gold seal glyph
$b = New-Tex
for ($y = 2; $y -lt 15; $y++) {
    for ($x = 3; $x -lt 13; $x++) {
        $edge = ($x -eq 3 -or $x -eq 12 -or $y -eq 2 -or $y -eq 14)
        if ($edge) { Set-Px $b $x $y '#1E1828' } else { Set-Px $b $x $y (Get-Pick @('#3A3046', '#433852', '#332A3E') @(4, 3, 3)) }
    }
}
foreach ($p in @(@(7, 4), @(8, 4), @(6, 5), @(9, 5), @(5, 7), @(10, 7), @(5, 8), @(10, 8), @(6, 10), @(9, 10), @(7, 11), @(8, 11))) { Set-Px $b $p[0] $p[1] '#D6A84B' }
foreach ($p in @(@(7, 6), @(8, 6), @(7, 7), @(8, 7), @(7, 8), @(8, 8), @(7, 9), @(8, 9))) { Set-Px $b $p[0] $p[1] '#FFE08A' }
Save-Tex $b $itemDir 'sealing_rune'

# ---- Sundered effect icon (18 x 18): a cracked steel plate
$effDir = Join-Path $root 'mob_effect'; New-Item -ItemType Directory -Force $effDir | Out-Null
$b = New-Object System.Drawing.Bitmap 18, 18, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
for ($y = 3; $y -lt 15; $y++) { for ($x = 3; $x -lt 15; $x++) { $b.SetPixel($x, $y, (Get-Col ($(if ($x -eq 3 -or $y -eq 3) { '#B8B0C4' } elseif ($x -eq 14 -or $y -eq 14) { '#4A4456' } else { '#8A8296' })))) } }
foreach ($p in @(@(9, 3), @(9, 4), @(8, 5), @(8, 6), @(9, 7), @(10, 8), @(10, 9), @(9, 10), @(8, 11), @(8, 12), @(7, 13), @(7, 14), @(11, 9), @(12, 10))) { $b.SetPixel($p[0], $p[1], (Get-Col '#2A2333')) }
$b.Save((Join-Path $effDir 'sundered.png'), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose()

'textures written'
