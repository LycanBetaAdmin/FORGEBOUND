# Generates FORGEBOUND's 16x16 textures into src/main/resources/assets/forgebound/textures:
#   item/  raw + ingot for Verdantite, Infernalite and Voidsteel, Arcane Shard, Crystal Dust, Ascension Core
#   block/ ore overlays (drawn over vanilla stone / deepslate / netherrack / end stone) and the Forge Anvil
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

$ingot = @(
    '................',
    '................',
    '................',
    '................',
    '................',
    '.....ooooooooo..',
    '....ohhhhhhhlo..',
    '...ohllllllllmo.',
    '..ohlllllllmmmo.',
    '.ommmmmmmmmmdo..',
    '.ohmmmmmmmmddo..',
    '.oddddddddddo...',
    '..oooooooooo....',
    '................',
    '................',
    '................')
$raw = @(
    '................',
    '................',
    '................',
    '................',
    '......oooo......',
    '....oommlloo....',
    '...ommlhhlmmo...',
    '..omlhhllmmddo..',
    '..omllmmmdddo...',
    '.ommmmmdddmmo...',
    '.odmmdddmmlmmo..',
    '.oddddmmmmmddo..',
    '..oodddddddoo...',
    '....oooooooo....',
    '................',
    '................')
foreach ($metal in 'verdantite', 'infernalite', 'voidsteel') {
    $b = New-Tex; Write-Mask $b $ingot $pals[$metal]; Save-Tex $b $itemDir "${metal}_ingot"
    $b = New-Tex; Write-Mask $b $raw $pals[$metal]; Save-Tex $b $itemDir "raw_$metal"
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

# ---- ore overlays: clusters of metal specks (transparent elsewhere)
$clusters = @(@(2, 2), @(9, 3), @(5, 7), @(11, 9), @(3, 11), @(8, 12), @(12, 1))
foreach ($metal in 'verdantite', 'infernalite', 'voidsteel') {
    $p = $pals[$metal]; $b = New-Tex
    foreach ($c in $clusters) {
        $x = $c[0]; $y = $c[1]
        Set-Px $b $x $y $p.l; Set-Px $b ($x + 1) $y $p.m; Set-Px $b $x ($y + 1) $p.m; Set-Px $b ($x + 1) ($y + 1) $p.d
        if ($rng.Next(2) -eq 0) { Set-Px $b ($x + 2) ($y + 1) $p.o } else { Set-Px $b ($x - 1) ($y + 1) $p.o }
        if ($rng.Next(3) -eq 0) { Set-Px $b ($x + 1) ($y - 1) $p.h }
    }
    Save-Tex $b $blockDir "${metal}_ore_overlay"
}

# ---- Forge Anvil
# steel: dark blued steel with faint hammer marks
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { Set-Px $b $x $y (Get-Pick @('#2E2A36', '#363140', '#27232E', '#403A4C') @(5, 4, 3, 1)) } }
Save-Tex $b $blockDir 'forge_anvil_steel'
# face: steel with an antique gold band top and bottom and gold corner rivets
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $c = Get-Pick @('#2E2A36', '#363140', '#27232E') @(5, 4, 3)
        if ($y -eq 0 -or $y -eq 15) { $c = '#D6A84B' } elseif ($y -eq 1 -or $y -eq 14) { $c = '#7A5A22' }
        Set-Px $b $x $y $c
    }
}
foreach ($p in @(@(1, 3), @(14, 3), @(1, 12), @(14, 12))) { Set-Px $b $p[0] $p[1] '#FFD15C' }
Save-Tex $b $blockDir 'forge_anvil_face'
# top: polished working face, gold rim
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $c = Get-Pick @('#4A4456', '#534C60', '#433D4E') @(5, 3, 3)
        if ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15) { $c = '#D6A84B' }
        elseif ($x -eq 1 -or $y -eq 1 -or $x -eq 14 -or $y -eq 14) { $c = '#7A5A22' }
        Set-Px $b $x $y $c
    }
}
Save-Tex $b $blockDir 'forge_anvil_top'
# base: obsidian with a gold trim along the top
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $c = Get-Pick @('#140F1E', '#1C1529', '#100C18', '#2A1F3D') @(5, 4, 3, 1)
        if ($y -eq 0) { $c = '#D6A84B' } elseif ($y -eq 1) { $c = '#7A5A22' }
        Set-Px $b $x $y $c
    }
}
Save-Tex $b $blockDir 'forge_anvil_base'
# runes: glowing arcane blue glyphs, transparent between them (overlay)
$b = New-Tex
# a band of small runes, 3 px tall on rows 6-8 (the model shows only this band), dots between them
$glyphs = @(
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    'h.l.lh.l.hl.l.lh',
    'l.lll..h.l..ll.l',
    'lh.l.hl.l.lh.l.l',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................')
Write-Mask $b $glyphs @{ l = '#3FA9E8'; h = '#C8F2FF' }
Save-Tex $b $blockDir 'forge_anvil_runes'
'textures written'
