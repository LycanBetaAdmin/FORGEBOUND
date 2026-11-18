# Builds the 1.2 item icons from the user's reference art (tools/texgen/ref/*.png: the pixel grids recovered from the
# reference pictures with Pixelize.java) plus two hand-drawn icons:
#   raw_<metal>, <metal>_ingot  - reference shapes recoloured by brightness onto each metal's original palette
#   fortune_dice, crystal_dust  - reference art as drawn
#   ascension_core, sealing_rune - drawn here after the reference pictures (they were painted, not pixel art)
# Output: 32x32 icons (crystal_dust 16x16) in src/main/resources/assets/forgebound/textures/item.
# Run: powershell -File tools\texgen\gen_forgebound_refs.ps1
Add-Type -AssemblyName System.Drawing
$refDir = Join-Path $PSScriptRoot 'ref'
$itemDir = Join-Path $PSScriptRoot '..\..\src\main\resources\assets\forgebound\textures\item'

function Get-Col([string]$hex) { $v = [Convert]::ToInt32($hex.TrimStart('#'), 16); return [System.Drawing.Color]::FromArgb(255, ($v -shr 16) -band 255, ($v -shr 8) -band 255, $v -band 255) }
function New-Tex([int]$n) { return New-Object System.Drawing.Bitmap $n, $n, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function Set-Px($b, [int]$x, [int]$y, [string]$hex) { if ($x -ge 0 -and $y -ge 0 -and $x -lt $b.Width -and $y -lt $b.Height) { $b.SetPixel($x, $y, (Get-Col $hex)) } }
function Save-Icon($b, [string]$name) { $b.Save((Join-Path $itemDir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }
function Get-Lum($c) { return 0.299 * $c.R + 0.587 * $c.G + 0.114 * $c.B }

# brightness ramps (dark outline .. highlight): each metal's original colours
$ramps = @{
    verdantite  = @('#06200C', '#0B3A16', '#16722E', '#2FB04A', '#7BEF7E', '#E4FFD8')
    infernalite = @('#2A0602', '#4A0B04', '#9A2108', '#E2461A', '#FF8A4A', '#FFE0B0')
    voidsteel   = @('#0A0418', '#14082A', '#4A1E8C', '#8A44E6', '#C48CFF', '#F3E2FF')
}
function Get-Ramp([string[]]$ramp, [double]$t) {
    $t = [Math]::Max(0.0, [Math]::Min(1.0, [double]$t)) * ($ramp.Count - 1)
    $i = [Math]::Min($ramp.Count - 2, [Math]::Floor($t)); $f = $t - $i
    $a = Get-Col $ramp[$i]; $b = Get-Col $ramp[$i + 1]
    return [System.Drawing.Color]::FromArgb(255, [int]($a.R + ($b.R - $a.R) * $f), [int]($a.G + ($b.G - $a.G) * $f), [int]($a.B + ($b.B - $a.B) * $f))
}

# copies a reference grid onto an n x n canvas, centred on its opaque pixels; $clear lists x0,y0,x1,y1 boxes to drop
# first; $ramp recolours by brightness (saturated pixels - e.g. ore tips - are pushed up the ramp by $accent)
function Convert-Ref([string]$src, [string]$name, [int]$n, [object[]]$clear = @(), [string[]]$ramp = $null, [double]$accent = 0, [double]$gamma = 1) {
    $s = New-Object System.Drawing.Bitmap (Join-Path $refDir "$src.png")
    $w = $s.Width; $h = $s.Height
    $keep = New-Object 'bool[,]' $w, $h
    $lo = 999; $hi = -1; $x0 = $w; $y0 = $h; $x1 = -1; $y1 = -1
    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            if ($s.GetPixel($x, $y).A -eq 0) { continue }
            $drop = $false
            foreach ($r in $clear) { if ($x -ge $r[0] -and $y -ge $r[1] -and $x -le $r[2] -and $y -le $r[3]) { $drop = $true } }
            if ($drop) { continue }
            $keep[$x, $y] = $true
            $l = Get-Lum $s.GetPixel($x, $y); $lo = [Math]::Min($lo, $l); $hi = [Math]::Max($hi, $l)
            $x0 = [Math]::Min($x0, $x); $x1 = [Math]::Max($x1, $x); $y0 = [Math]::Min($y0, $y); $y1 = [Math]::Max($y1, $y)
        }
    }
    if ($x1 - $x0 + 1 -gt $n -or $y1 - $y0 + 1 -gt $n) { throw "$src does not fit $n x $n ($($x1 - $x0 + 1) x $($y1 - $y0 + 1))" }
    $dx = [Math]::Floor(($n - ($x1 - $x0 + 1)) / 2) - $x0; $dy = [Math]::Floor(($n - ($y1 - $y0 + 1)) / 2) - $y0
    $b = New-Tex $n
    for ($y = $y0; $y -le $y1; $y++) {
        for ($x = $x0; $x -le $x1; $x++) {
            if (-not $keep[$x, $y]) { continue }
            $c = $s.GetPixel($x, $y)
            if ($ramp) {
                $t = [Math]::Pow([Math]::Max(0.0, ((Get-Lum $c) - $lo) / [Math]::Max(1.0, $hi - $lo)), $gamma)
                $mx = [Math]::Max($c.R, [Math]::Max($c.G, $c.B)); $mn = [Math]::Min($c.R, [Math]::Min($c.G, $c.B))
                if ($accent -gt 0 -and $mx - $mn -gt 30) { $t = [Math]::Min(1.0, $t + $accent) }
                $c = Get-Ramp $ramp $t
            }
            $b.SetPixel($x + $dx, $y + $dy, $c)
        }
    }
    $s.Dispose()
    Save-Icon $b $name
}

# ---- ingots (Verdantite: sparkling bar, Infernalite: burning bar, Voidsteel: rune bar with floating shards); the
# small preview icon in each picture's corner is dropped
Convert-Ref 'ingot_verdantite' 'verdantite_ingot' 32 @(, @(3, 1, 7, 5)) $ramps.verdantite
Convert-Ref 'ingot_infernalite' 'infernalite_ingot' 32 @(, @(2, 2, 7, 7)) $ramps.infernalite 0 2.2
Convert-Ref 'ingot_voidsteel' 'voidsteel_ingot' 32 @(, @(2, 1, 6, 6)) $ramps.voidsteel 0 0.8
# ---- raw metals: the three rock shapes in the metal's colour (Infernalite keeps its glowing tips brighter)
Convert-Ref 'raw_verdantite' 'raw_verdantite' 32 @() $ramps.verdantite
Convert-Ref 'raw_infernalite' 'raw_infernalite' 32 @() $ramps.infernalite 0.35 1.2
Convert-Ref 'raw_voidsteel' 'raw_voidsteel' 32 @(, @(32, 0, 32, 30)) $ramps.voidsteel
# ---- Fortune Dice (teal d20) and Crystal Dust (red heap), as drawn
Convert-Ref 'fortune_dice' 'fortune_dice' 32
Convert-Ref 'crystal_dust' 'crystal_dust' 16

# ---- Ascension Core: a faceted violet orb with a bright star, a tilted gold ring wrapping round it, gold settings with
# violet gems on top, bottom and the ring, and a few floating motes
$b = New-Tex 32
$cx = 15.5; $cy = 15.5
function Get-RingPt([double]$th) {
    $ex = 13.2 * [Math]::Cos($th); $ey = 4.6 * [Math]::Sin($th); $r = -0.38
    return @(($cx + $ex * [Math]::Cos($r) - $ey * [Math]::Sin($r)), ($cy + 1 + $ex * [Math]::Sin($r) + $ey * [Math]::Cos($r)))
}
function Draw-Ring([bool]$front) {
    for ($k = 0; $k -lt 720; $k++) {
        $th = $k / 720 * 2 * [Math]::PI
        if (([Math]::Sin($th) -gt 0) -ne $front) { continue }
        $p = Get-RingPt $th; $x = [Math]::Round($p[0]); $y = [Math]::Round($p[1])
        Set-Px $b $x ($y - 1) '#2A1A06'; Set-Px $b $x ($y + 2) '#2A1A06'
    }
    for ($k = 0; $k -lt 720; $k++) {
        $th = $k / 720 * 2 * [Math]::PI
        if (([Math]::Sin($th) -gt 0) -ne $front) { continue }
        $p = Get-RingPt $th; $x = [Math]::Round($p[0]); $y = [Math]::Round($p[1])
        Set-Px $b $x $y ($(if ($front) { '#FFD86A' } else { '#B8862B' })); Set-Px $b $x ($y + 1) ($(if ($front) { '#D99A2E' } else { '#8C6420' }))
    }
}
Draw-Ring $false
for ($y = 0; $y -lt 32; $y++) {
    for ($x = 0; $x -lt 32; $x++) {
        $ddx = $x - $cx; $ddy = $y - $cy; $d = [Math]::Sqrt($ddx * $ddx + $ddy * $ddy)
        if ($d -gt 9.6) { continue }
        if ($d -gt 8.7) { Set-Px $b $x $y '#1A0630'; continue }
        $lit = (- $ddx - $ddy) / 12.3 + 0.15 - $d / 30
        $facet = ((([Math]::Floor(($x + 1) / 2) + [Math]::Floor(($y + 1) / 2)) % 2) -eq 0)
        $c = if ($lit -gt 0.75) { '#E6A8FF' } elseif ($lit -gt 0.4) { if ($facet) { '#C76CFF' } else { '#B455F5' } } elseif ($lit -gt 0.0) { if ($facet) { '#9A3AE6' } else { '#8A2FD6' } } elseif ($lit -gt -0.45) { if ($facet) { '#6E1FB8' } else { '#62199E' } } else { '#46107A' }
        Set-Px $b $x $y $c
    }
}
# the star in the middle
foreach ($p in @(@(0, 0, '#FFFFFF'), @(1, 0, '#FFFFFF'), @(0, 1, '#FFFFFF'), @(1, 1, '#FFFFFF'))) { Set-Px $b (15 + $p[0]) (15 + $p[1]) $p[2] }
foreach ($k in 2..4) { $c = $(if ($k -lt 4) { '#FFC8FF' } else { '#F07CFF' }); Set-Px $b (15 - $k + 1) 15 $c; Set-Px $b (15 - $k + 1) 16 $c; Set-Px $b (16 + $k - 1) 15 $c; Set-Px $b (16 + $k - 1) 16 $c; Set-Px $b 15 (15 - $k + 1) $c; Set-Px $b 16 (15 - $k + 1) $c; Set-Px $b 15 (16 + $k - 1) $c; Set-Px $b 16 (16 + $k - 1) $c }
foreach ($p in @(@(13, 13), @(18, 13), @(13, 18), @(18, 18))) { Set-Px $b $p[0] $p[1] '#E07CFF' }
Draw-Ring $true
# gold settings: a diamond with a violet gem, outlined
function Draw-Setting([int]$x, [int]$y, [int]$r) {
    for ($j = -$r - 1; $j -le $r + 1; $j++) { for ($i = -$r - 1; $i -le $r + 1; $i++) {
        $m = [Math]::Abs($i) + [Math]::Abs($j)
        if ($m -eq $r + 1) { Set-Px $b ($x + $i) ($y + $j) '#2A1A06' }
        elseif ($m -le $r) { Set-Px $b ($x + $i) ($y + $j) ($(if ($m -le $r - 2) { if ($i + $j -lt 0) { '#F07CFF' } else { '#A52CE0' } } elseif ($i + $j -lt 0) { '#FFE08A' } else { '#D99A2E' })) }
    } }
}
Draw-Setting 16 4 3
Set-Px $b 16 0 '#2A1A06'; Set-Px $b 15 0 '#2A1A06'; Set-Px $b 15 1 '#FFE08A'; Set-Px $b 16 1 '#D99A2E'
Draw-Setting 15 27 2
Set-Px $b 15 30 '#D99A2E'; Set-Px $b 15 31 '#2A1A06'
$p = Get-RingPt ([Math]::PI); Draw-Setting ([Math]::Round($p[0])) ([Math]::Round($p[1])) 2
$p = Get-RingPt 1.25; Draw-Setting ([Math]::Round($p[0])) ([Math]::Round($p[1])) 2
foreach ($m in @(@(5, 7, '#C76CFF'), @(27, 8, '#E07CFF'), @(26, 25, '#C76CFF'), @(7, 26, '#B455F5'), @(28, 18, '#9A3AE6'))) { Set-Px $b $m[0] $m[1] $m[2] }
Save-Icon $b 'ascension_core'

# ---- Sealing Rune: a dark indigo seal stone with a light rim highlight, a glowing cyan rune and drifting bubbles
$b = New-Tex 32
for ($y = 0; $y -lt 32; $y++) {
    for ($x = 0; $x -lt 32; $x++) {
        $ddx = $x - 15.5; $ddy = $y - 15.5; $d = [Math]::Sqrt($ddx * $ddx + $ddy * $ddy)
        if ($d -gt 13.4) { continue }
        if ($d -gt 12.4) { Set-Px $b $x $y '#07061A'; continue }
        $ang = [Math]::Atan2($ddy, $ddx) * 180 / [Math]::PI
        if ($d -gt 9.6 -and $d -le 11.4 -and $ang -gt -165 -and $ang -lt -105) { Set-Px $b $x $y ($(if ($d -gt 10.4 -and $ang -gt -150 -and $ang -lt -120) { '#E4E0FF' } else { '#9C94DC' })); continue }
        $lit = (- $ddx - $ddy) / 17.5 - $d / 40
        $c = if ($d -gt 11.4 -and $lit -lt -0.2) { '#120F38' } elseif ($lit -gt 0.35) { '#3B3496' } elseif ($lit -gt -0.05) { '#2E2880' } elseif ($lit -gt -0.45) { '#25206C' } else { '#1B1752' }
        Set-Px $b $x $y $c
    }
}
$rune = @(
    '....hhhh....',
    '..hhcccchh..',
    '.hcc....cch.',
    'hc........ch',
    'c..........c',
    '.....hc.....',
    '...hhcccc...',
    '.....cc.....',
    '.....cc.....',
    '....hccc....',
    '...hc.cc.c..',
    '..hc..cc..c.',
    '.hc...cc...c',
    '......c.....')
foreach ($pass in 0, 1) {
    for ($j = 0; $j -lt $rune.Count; $j++) {
        for ($i = 0; $i -lt 12; $i++) {
            $ch = [string]$rune[$j][$i]; if ($ch -eq '.') { continue }
            if ($pass -eq 0) { Set-Px $b (11 + $i) (10 + $j) '#0E3A4A' }
            else { Set-Px $b (10 + $i) (9 + $j) ($(if ($ch -eq 'h') { '#B8FFF8' } else { '#4FD8E0' })) }
        }
    }
}
foreach ($m in @(@(5, 26, 2), @(3, 22, 1), @(25, 4, 2), @(28, 7, 1), @(8, 6, 1), @(27, 24, 1))) {
    $x = $m[0]; $y = $m[1]
    if ($m[2] -eq 2) { foreach ($q in @(@(0, -1), @(-1, 0), @(1, 0), @(0, 1))) { Set-Px $b ($x + $q[0]) ($y + $q[1]) '#2E8CC8' }; Set-Px $b $x $y '#A8F4FF' }
    else { Set-Px $b $x $y '#5FC8F0' }
}
Save-Icon $b 'sealing_rune'
'reference icons written'
