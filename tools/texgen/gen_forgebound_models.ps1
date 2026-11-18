# Writes the Forge Anvil and Rune Engraving Table block models (and their item models) into
# src/main/resources/assets/forgebound/models. Fronts face north; blockstates rotate them by facing.
# Run: powershell -File tools\texgen\gen_forgebound_models.ps1
$assets = Join-Path $PSScriptRoot '..\..\src\main\resources\assets\forgebound'
$inv = [Globalization.CultureInfo]::InvariantCulture
function Format-N([double]$v) { return $v.ToString('0.##', $inv) }
function Format-V([double[]]$v) { return '[' + (($v | ForEach-Object { Format-N $_ }) -join ', ') + ']' }
$sides = @('north', 'south', 'east', 'west', 'up', 'down')
$glow = ', "shade": false, "neoforge_data": {"block_light": 15, "sky_light": 15}'
$full = '"uv": [0, 0, 16, 16], '

# Automatic UVs come from the element's position; slide each span back inside 0..16 so parts outside the block
# (above y 16, beyond x/z 0..16) don't sample neighbouring atlas sprites.
function Get-Wrap([double]$a, [double]$b) {
    $lo = [Math]::Min($a, $b); $hi = [Math]::Max($a, $b)
    $k = if ($lo -lt 0) { $lo } elseif ($hi -gt 16) { $hi - 16 } else { 0 }
    return @([Math]::Max(0, [Math]::Min(16, $a - $k)), [Math]::Max(0, [Math]::Min(16, $b - $k)))
}
function Get-AutoUV([string]$face, [double[]]$f, [double[]]$t) {
    $uv = switch ($face) {
        'north' { @((16 - $t[0]), (16 - $t[1]), (16 - $f[0]), (16 - $f[1])) }
        'south' { @($f[0], (16 - $t[1]), $t[0], (16 - $f[1])) }
        'east' { @((16 - $t[2]), (16 - $t[1]), (16 - $f[2]), (16 - $f[1])) }
        'west' { @($f[2], (16 - $t[1]), $t[2], (16 - $f[1])) }
        'up' { @($f[0], $f[2], $t[0], $t[2]) }
        'down' { @($f[0], (16 - $t[2]), $t[0], (16 - $f[2])) }
    }
    $u = Get-Wrap $uv[0] $uv[2]; $v = Get-Wrap $uv[1] $uv[3]
    return "`"uv`": $(Format-V @($u[0], $v[0], $u[1], $v[1])), "
}
function New-Elem([double[]]$from, [double[]]$to, [hashtable]$faces, [string]$extra = '') {
    $fs = ($sides | Where-Object { $faces.ContainsKey($_) } | ForEach-Object {
        $fb = $faces[$_]; if ($fb -notmatch '"uv"') { $fb = (Get-AutoUV $_ $from $to) + $fb }
        "`"$_`": {$fb}" }) -join ', '
    return "    {`"from`": $(Format-V $from), `"to`": $(Format-V $to)$extra, `"faces`": {$fs}}"
}
function Get-All([string]$tex, [string[]]$only = $sides) { $h = @{}; foreach ($s in $only) { $h[$s] = "`"texture`": `"#$tex`"" }; return $h }
function Get-Rot([string]$axis, [double]$angle, [double[]]$origin) { return ", `"rotation`": {`"angle`": $(Format-N $angle), `"axis`": `"$axis`", `"origin`": $(Format-V $origin)}" }
function Get-One([string]$face, [string]$tex, [string]$uv = '') { $h = @{}; $h[$face] = "$uv`"texture`": `"#$tex`""; return $h }
$utf8 = New-Object System.Text.UTF8Encoding $false
function Out-Model([string]$rel, [string]$textures, $elements) {
    $text = "{`n  `"parent`": `"minecraft:block/block`",`n  `"render_type`": `"minecraft:cutout`",`n  `"textures`": {`n$textures`n  },`n  `"elements`": [`n" + ($elements -join ",`n") + "`n  ]`n}`n"
    $p = Join-Path $assets $rel
    [IO.File]::WriteAllText($p, $text, $utf8)
    $null = Get-Content -Raw $p | ConvertFrom-Json
}

# =====================================================================  Forge Anvil (long side along x, horn to the east)
$e = New-Object System.Collections.Generic.List[string]
$foot = Get-All 'dark'; $foot['down'] = '"texture": "#dark", "cullface": "down"'
$e.Add((New-Elem @(1.5, 0, 3) @(14.5, 2.5, 13) $foot))                       # foot
$e.Add((New-Elem @(3, 2.5, 4) @(13, 4, 12) (Get-All 'steel')))                # step
$e.Add((New-Elem @(4.5, 4, 5) @(11.5, 8.5, 11) (Get-All 'steel')))            # waist
$e.Add((New-Elem @(3.5, 8.5, 4.2) @(12.5, 10, 11.8) (Get-All 'steel')))       # flare under the face
$e.Add((New-Elem @(0, 10, 3) @(13, 15.5, 13) (Get-All 'steel')))              # face block
$e.Add((New-Elem @(13, 11, 4.5) @(17, 15.5, 11.5) (Get-All 'steel')))         # horn
$e.Add((New-Elem @(17, 12.5, 6) @(19.5, 15.5, 10) (Get-All 'steel')))         # horn tip
$e.Add((New-Elem @(-1, 12.5, 4.5) @(0, 15.5, 11.5) (Get-All 'steel')))        # heel
# gold bands round both ends of the face, gold accents on its front
foreach ($bx in @(1, 10.4)) { $e.Add((New-Elem @($bx, 9.9, 2.9) @(($bx + 1.6), 15.6, 13.1) (Get-All 'gold'))) }
$e.Add((New-Elem @(3.5, 14, 2.95) @(5.5, 14.6, 3) (Get-One 'north' 'gold')))
$e.Add((New-Elem @(7.5, 14, 2.95) @(9.5, 14.6, 3) (Get-One 'north' 'gold')))
# gold bracket round the base front and back, with the octagonal gem in front
foreach ($z in @(@(2.9, 3, 'north'), @(13, 13.1, 'south'))) {
    $e.Add((New-Elem @(3, 3.5, $z[0]) @(13, 4, $z[1]) (Get-One $z[2] 'gold')))
    $e.Add((New-Elem @(3, 0.5, $z[0]) @(3.5, 3.5, $z[1]) (Get-One $z[2] 'gold')))
    $e.Add((New-Elem @(12.5, 0.5, $z[0]) @(13, 3.5, $z[1]) (Get-One $z[2] 'gold')))
}
$e.Add((New-Elem @(6.5, 0.4, 2.6) @(9.5, 3.4, 3) (Get-All 'gold' @('north', 'east', 'west', 'up', 'down'))))
$e.Add((New-Elem @(7, 0.9, 2.5) @(9, 2.9, 2.6) (Get-One 'north' 'gem' $full) $glow))
# glowing runes: the grid on the face, the glyph on the waist (front and back)
$e.Add((New-Elem @(0.2, 15.51, 3.2) @(12.8, 15.52, 12.8) (Get-One 'up' 'grid' $full) $glow))
$e.Add((New-Elem @(4.6, 4.2, 4.95) @(11.4, 8.4, 5) (Get-One 'north' 'glyph' '"uv": [0, 0, 16, 12], ') $glow))
$e.Add((New-Elem @(4.6, 4.2, 11) @(11.4, 8.4, 11.05) (Get-One 'south' 'glyph' '"uv": [0, 0, 16, 12], ') $glow))
Out-Model 'models\block\forge_anvil.json' @"
    "particle": "forgebound:block/forge_anvil_steel",
    "steel": "forgebound:block/forge_anvil_steel",
    "dark": "forgebound:block/forge_anvil_steel_dark",
    "gold": "forgebound:block/forge_anvil_gold",
    "grid": "forgebound:block/forge_anvil_top_runes",
    "glyph": "forgebound:block/forge_anvil_glyph",
    "gem": "forgebound:block/forge_anvil_gem"
"@ $e
"anvil: $($e.Count) elements"

# =====================================================================  Rune Engraving Table (two blocks, front = north)
# Authored in table space: X 0..32 across both blocks (X 32 is the viewer's left), Z 0 = front. The main block is
# X 16..32; the side block (X 0..16) is the placer's right and has an empty model. 1 unit = 1 texel throughout.
# A violet stone altar on a plinth: corner posts, gold bands, arched glowing windows, a runner with a draped front
# cloth and a magic circle under a large floating amethyst in a gold ring; behind it two rune pillars carry a beam with
# a crown and a rune plaque, with a lantern hanging each side. On the desk: a gold book stand with an open book (right),
# tomes, candles, potions and an amethyst cluster (left).
$script:OX = -16
function Get-TRot([string]$axis, [double]$angle, [double[]]$o) { return Get-Rot $axis $angle @(($o[0] + $script:OX), $o[1], $o[2]) }
function Add-T([double[]]$from, [double[]]$to, [hashtable]$faces, [string]$extra = '') {
    $f = @(([Math]::Max(0.0, $from[0]) + $script:OX), $from[1], $from[2]); $t = @(([Math]::Min(32.0, $to[0]) + $script:OX), $to[1], $to[2])
    $e.Add((New-Elem $f $t $faces $extra))
}
# long parts are split at X 16 so automatic UVs stay 1:1 with the texture
function Add-TSplit([double[]]$from, [double[]]$to, [string]$tex, [string[]]$only = $sides) {
    if ($from[0] -lt 16 -and $to[0] -gt 16) {
        Add-T $from @(16, $to[1], $to[2]) (Get-All $tex ($only | Where-Object { $_ -ne 'east' }))
        Add-T @(16, $from[1], $from[2]) $to (Get-All $tex ($only | Where-Object { $_ -ne 'west' }))
    } else { Add-T $from $to (Get-All $tex $only) }
}
function Get-Plain([string]$tex, [double]$u, [double]$v, [double[]]$from, [double[]]$to, [string[]]$only = $sides) {
    $dx = $to[0] - $from[0]; $dy = $to[1] - $from[1]; $dz = $to[2] - $from[2]; $h = @{}
    foreach ($s in $only) {
        $w, $hh = switch ($s) { { $_ -in 'north', 'south' } { $dx, $dy } { $_ -in 'east', 'west' } { $dz, $dy } default { $dx, $dz } }
        $h[$s] = "`"uv`": $(Format-V @($u, $v, ($u + $w), ($v + $hh))), `"texture`": `"#$tex`""
    }
    return $h
}
function Add-TPlain([string]$tex, [double]$u, [double]$v, [double[]]$from, [double[]]$to, [string]$extra = '', [string[]]$only = $sides) {
    Add-T $from $to (Get-Plain $tex $u $v $from $to $only) $extra
}
function Add-TCross([double]$cx, [double]$y0, [double]$cz, [double]$w, [double]$h, [string]$uv, [string]$tex, [string]$extra = '') {
    $f = "`"uv`": $uv, `"texture`": `"#$tex`""
    foreach ($a in @(45, -45)) {
        Add-T @(($cx - $w / 2), $y0, $cz) @(($cx + $w / 2), ($y0 + $h), $cz) @{ north = $f; south = $f } ((Get-TRot 'y' $a @($cx, $y0, $cz)) + $extra)
    }
}
function Get-Face([string]$tex, [double[]]$uv) { return "`"uv`": $(Format-V $uv), `"texture`": `"#$tex`"" }
$e = New-Object System.Collections.Generic.List[string]
$soft = ', "shade": false, "neoforge_data": {"block_light": 12, "sky_light": 15}'

# ---- base: plinth, gold trim, recessed body, corner posts with gold collars
$plinth = @('north', 'south', 'east', 'west', 'up', 'down')
Add-TSplit @(0.5, 0, 0.5) @(31.5, 2, 15.5) 'stone' $plinth
Add-TSplit @(0.3, 2, 0.3) @(31.7, 2.5, 15.7) 'gold' @('north', 'south', 'east', 'west', 'up')
Add-TSplit @(2, 2.5, 2) @(30, 10.5, 14) 'dark' @('north', 'south', 'east', 'west')
foreach ($c in @(@(0.5, 0.5), @(28.5, 0.5), @(0.5, 12.5), @(28.5, 12.5))) {
    Add-T @($c[0], 2.5, $c[1]) @(($c[0] + 3), 10.5, ($c[1] + 3)) (Get-All 'stone' @('north', 'south', 'east', 'west'))
    Add-T @(($c[0] - 0.2), 9.6, ($c[1] - 0.2)) @(($c[0] + 3.2), 10.5, ($c[1] + 3.2)) (Get-All 'gold' @('north', 'south', 'east', 'west'))
}
# arched glowing windows: two on the front, one on each end
foreach ($wx in @(5.5, 22.5)) { Add-T @($wx, 3.5, 1.95) @(($wx + 4), 9.5, 1.95) @{ north = (Get-Face 'glow' @(0, 0, 4, 6)) } $glow }
Add-T @(1.95, 3.5, 6) @(1.95, 9.5, 10) @{ west = (Get-Face 'glow' @(0, 0, 4, 6)) } $glow
Add-T @(30.05, 3.5, 6) @(30.05, 9.5, 10) @{ east = (Get-Face 'glow' @(0, 0, 4, 6)) } $glow
# gold band under the top, the top slab, gold corner caps
Add-TSplit @(-0.1, 10.5, -0.1) @(32.1, 11, 16.1) 'gold' @('north', 'south', 'east', 'west', 'down')
Add-TSplit @(0, 11, 0) @(32, 13, 16) 'stone' $sides
foreach ($c in @(@(-0.2, -0.2), @(28.7, -0.2), @(-0.2, 12.7), @(28.7, 12.7))) {
    Add-T @($c[0], 12.9, $c[1]) @(($c[0] + 3.5), 13.3, ($c[1] + 3.5)) (Get-All 'gold' @('north', 'south', 'east', 'west', 'up'))
}
# runner with the front drape and the magic circle
Add-T @(10, 13, 1) @(22, 13.2, 12.5) @{ up = (Get-Face 'runner' @(0, 0, 12, 11.5)); south = (Get-Face 'runner' @(12, 0, 16, 0.2)); east = (Get-Face 'runner' @(12, 0, 16, 0.2)); west = (Get-Face 'runner' @(12, 0, 16, 0.2)) }
Add-T @(10, 13, -0.15) @(22, 13.2, 1) @{ up = (Get-Face 'runner' @(0, 0, 12, 1.15)) }
Add-T @(10, 1.2, -0.15) @(22, 13.2, -0.15) @{ north = (Get-Face 'drape' @(0, 0, 12, 12)) } $soft
Add-T @(11, 13.21, 2) @(21, 13.22, 12) @{ up = (Get-Face 'circle' @(0, 0, 10, 10)) } $glow

# ---- the floating crystal: the user's pink-violet gem sprite built as a solid crystal. Each sprite row is a slab as wide
# as the row, its depth following the width (so it rounds out like a gem); the row's pixels go on the front and back,
# the edge pixels' colours on the sides. Held in a gold frame with violet rune plates and a gem mount; small copies of
# the same crystal float around it.
Add-Type -AssemblyName System.Drawing
$fcLight = ', "neoforge_data": {"block_light": 13, "sky_light": 15}'
$spr = New-Object System.Drawing.Bitmap (Join-Path $PSScriptRoot 'ref\floating_crystal.png')
$fcRows = @()
for ($r = 0; $r -lt $spr.Height; $r++) {
    $mn = -1; $mx = -1
    for ($x = 0; $x -lt $spr.Width; $x++) { if ($spr.GetPixel($x, $r).A -gt 0) { if ($mn -lt 0) { $mn = $x }; $mx = $x } }
    $fcRows += , @($mn, $mx)
}
$fcW = $spr.Width; $fcH = $spr.Height; $spr.Dispose()
function Get-Uv([double]$u0, [double]$v0, [double]$u1, [double]$v1) { return "`"uv`": $(Format-V @(($u0 / 2), ($v0 / 2), ($u1 / 2), ($v1 / 2))), `"texture`": `"#fcry`"" }
function Add-SpriteCrystal([double]$cx, [double]$yBot, [double]$cz, [double]$s) {
    $yTop = $yBot + $fcH * $s
    for ($r = 0; $r -lt $fcH; $r++) {
        $mn = $fcRows[$r][0]; $mx = $fcRows[$r][1]; if ($mn -lt 0) { continue }
        $w = $mx - $mn + 1; $d = [Math]::Max(1, $w * 0.4) * $s
        $x0 = $cx + ($mn - $fcW / 2) * $s; $x1 = $cx + ($mx + 1 - $fcW / 2) * $s
        $faces = @{
            north = Get-Uv $mn $r ($mx + 1) ($r + 1); south = Get-Uv ($mx + 1) $r $mn ($r + 1)
            west  = Get-Uv $mn $r ($mn + 1) ($r + 1); east = Get-Uv $mx $r ($mx + 1) ($r + 1)
            up    = Get-Uv $mn $r ($mn + 1) ($r + 1); down = Get-Uv $mx $r ($mx + 1) ($r + 1)
        }
        Add-T @($x0, ($yTop - ($r + 1) * $s), ($cz - $d / 2)) @($x1, ($yTop - $r * $s), ($cz + $d / 2)) $faces $fcLight
    }
}
Add-SpriteCrystal 16 15.2 7 0.4
# the gold frame round the crystal's middle: a thin band (1.2 high, 0.4 thick, inner faces left out) showing a line
# of violet rune plates, with a small gold mount and gem on the front
$fy = 18.3; $fh = 1.2; $ft = 0.4; $fx0 = 13.3; $fx1 = 18.7; $fz0 = 5.6; $fz1 = 8.4
$long = '"uv": [0, 0.5, 5.4, 1.7], "texture": "#band"'; $short = '"uv": [1, 5.5, 3, 6.7], "texture": "#band"'; $edge = '"uv": [9, 0.5, 9.4, 1.7], "texture": "#band"'; $rim = '"uv": [0, 10, 5.4, 10.4], "texture": "#band"'; $rimS = '"uv": [0, 9, 0.4, 11], "texture": "#band"'
Add-T @($fx0, $fy, $fz0) @($fx1, ($fy + $fh), ($fz0 + $ft)) @{ north = $long; east = $edge; west = $edge; up = $rim; down = $rim }
Add-T @($fx0, $fy, ($fz1 - $ft)) @($fx1, ($fy + $fh), $fz1) @{ south = $long; east = $edge; west = $edge; up = $rim; down = $rim }
Add-T @($fx0, $fy, ($fz0 + $ft)) @(($fx0 + $ft), ($fy + $fh), ($fz1 - $ft)) @{ west = $short; up = $rimS; down = $rimS }
Add-T @(($fx1 - $ft), $fy, ($fz0 + $ft)) @($fx1, ($fy + $fh), ($fz1 - $ft)) @{ east = $short; up = $rimS; down = $rimS }
Add-T @(15.3, ($fy - 0.2), ($fz0 - 0.3)) @(16.7, ($fy + 1.4), $fz0) (Get-Plain 'band' 11 0 @(15.3, ($fy - 0.2), ($fz0 - 0.3)) @(16.7, ($fy + 1.4), $fz0))
Add-T @(15.6, ($fy + 0.2), ($fz0 - 0.5)) @(16.4, ($fy + 1), ($fz0 - 0.3)) @{ north = '"uv": [12, 5, 14, 7], "texture": "#band"'; east = '"uv": [12, 5, 12.2, 7], "texture": "#band"'; west = '"uv": [12, 5, 12.2, 7], "texture": "#band"'; up = '"uv": [12, 5, 14, 5.2], "texture": "#band"'; down = '"uv": [12, 5, 14, 5.2], "texture": "#band"' } $glow
# small floating crystals (the same gem, small)
foreach ($c in @(@(11.8, 21.6, 5.2), @(20.2, 22, 5.6), @(11.6, 17.2, 8.6), @(20.4, 16.8, 9), @(12.6, 15.4, 4.4), @(19.4, 15.6, 4.6))) {
    Add-SpriteCrystal $c[0] $c[1] $c[2] 0.14
}

# ---- back: rune pillars, beam, crown, plaque, lanterns
foreach ($x in @(1, 27)) {
    Add-T @($x, 13, 12) @(($x + 4), 26.5, 16) (Get-All 'stone' @('north', 'south', 'east', 'west'))
    Add-T @(($x - 0.25), 13, 11.75) @(($x + 4.25), 14, 16.25) (Get-All 'gold' @('north', 'south', 'east', 'west', 'up'))
    Add-T @(($x - 0.4), 26.5, 11.6) @(($x + 4.4), 28, 16.4) (Get-All 'gold')
    Add-T @(($x + 1), 15.5, 11.95) @(($x + 3), 24.5, 11.95) @{ north = (Get-Face 'glow' @(5, 0, 7, 9)) } $glow
}
Add-TSplit @(5, 24, 12.5) @(27, 26.5, 15.5) 'stone' @('north', 'south', 'up', 'down')
Add-TSplit @(5, 23.6, 12.4) @(27, 24, 15.6) 'gold' @('north', 'south', 'down')
Add-TSplit @(9, 26.5, 13) @(23, 28, 15) 'stone' @('north', 'south', 'east', 'west', 'up')
Add-TSplit @(12, 28, 13.3) @(20, 29.2, 14.7) 'gold' @('north', 'south', 'east', 'west', 'up')
Add-T @(12, 23, 12.3) @(20, 31, 12.3) @{ north = (Get-Face 'plaque' @(0, 0, 8, 8)) } $glow
$lchain = '"uv": [6, 0, 10, 4], "texture": "#chain"'
$lglass = '"uv": [0, 0, 6, 7], "texture": "#lantern"'
$lplate = '"uv": [8, 0, 16, 2], "texture": "#lantern"'
foreach ($lx in @(8, 24)) {
    Add-T @(($lx - 0.4), 21.8, 14) @(($lx + 0.4), 23.6, 14) @{ north = $lchain; south = $lchain }
    Add-T @(($lx - 0.9), 21.3, 13.1) @(($lx + 0.9), 21.8, 14.9) (Get-All 'lantern' @('north', 'south', 'east', 'west', 'up'))
    Add-T @(($lx - 0.8), 19.1, 13.2) @(($lx + 0.8), 21.3, 14.8) @{ north = $lglass; south = $lglass; east = $lglass; west = $lglass } $glow
    Add-T @(($lx - 1), 18.8, 13) @(($lx + 1), 19.1, 15) @{ north = $lplate; south = $lplate; east = $lplate; west = $lplate; down = $lplate }
}

# ---- right (X 1..9): a gold book stand holding an open book, tilted towards the reader
Add-TPlain 'gold' 2 2 @(3, 13, 3) @(8, 13.6, 8) '' @('north', 'south', 'east', 'west', 'up')
Add-TPlain 'gold' 2 2 @(5, 13.6, 5) @(6, 16, 6) '' @('north', 'south', 'east', 'west')
$tilt = Get-TRot 'x' -22.5 @(5.5, 16, 5.5)
Add-TPlain 'gold' 2 2 @(1, 16, 1) @(10, 16.4, 10) $tilt
Add-TPlain 'book' 8 0 @(1.3, 16.4, 1.3) @(9.7, 16.8, 9.7) $tilt @('north', 'south', 'east', 'west', 'up')
$pg = Get-Plain 'pages' 0 6 @(1.6, 16.8, 1.6) @(5.5, 17.5, 9.4) @('north', 'south', 'west'); $pg['up'] = (Get-Face 'pages' @(0, 0, 3, 6))
Add-T @(1.6, 16.8, 1.6) @(5.5, 17.5, 9.4) $pg $tilt
$pg = Get-Plain 'pages' 0 6 @(5.5, 16.8, 1.6) @(9.4, 17.5, 9.4) @('north', 'south', 'east'); $pg['up'] = (Get-Face 'pages' @(3, 0, 6, 6))
Add-T @(5.5, 16.8, 1.6) @(9.4, 17.5, 9.4) $pg ($tilt + $soft)
Add-TPlain 'book' 8 8 @(7.6, 14.9, 1.45) @(8.1, 17.52, 1.65) $tilt @('north', 'east', 'west', 'up')

# a closed book lying flat, spine to the west: covers, page block and a banded spine
function Add-TBook([double]$x, [double]$y, [double]$z, [double]$w, [double]$d, [double[]]$cover, [double[]]$plain, [double[]]$spine, [string]$rot = '') {
    Add-TPlain 'book' $plain[0] $plain[1] @($x, $y, $z) @(($x + $w), ($y + 0.5), ($z + $d)) $rot @('north', 'south', 'east', 'west', 'down')
    Add-TPlain 'book' 0 15 @(($x + 0.4), ($y + 0.5), ($z + 0.3)) @(($x + $w - 0.3), ($y + 1.5), ($z + $d - 0.3)) $rot @('north', 'south', 'east')
    $top = Get-Plain 'book' $plain[0] $plain[1] @($x, ($y + 1.5), $z) @(($x + $w), ($y + 2), ($z + $d)) @('north', 'south', 'east', 'west')
    $top['up'] = (Get-Face 'book' @($cover[0], $cover[1], ($cover[0] + $w), ($cover[1] + $d)))
    Add-T @($x, ($y + 1.5), $z) @(($x + $w), ($y + 2), ($z + $d)) $top $rot
    Add-T @(($x - 0.1), ($y + 0.05), $z) @(($x + 0.4), ($y + 1.95), ($z + $d)) @{ west = (Get-Face 'book' @($spine[0], $spine[1], ($spine[0] + $d), ($spine[1] + 2))) } $rot
}
# ---- left (X 22..31): two tomes, two candles and two potions
Add-TBook 26.4 13 6.2 4 5 @(0, 0) @(8, 0) @(0, 6)
Add-TBook 26.9 15 6.8 3 4 @(4, 0) @(8, 8) @(0, 9) (Get-TRot 'y' -22.5 @(28.4, 15, 8.8))
foreach ($c in @(@(28.6, 1.4, 4), @(26.2, 2.2, 3))) {
    $cx = $c[0]; $cz = $c[1]; $ch = $c[2]; $top = 13 + $ch
    $side = Get-Face 'candle' @(0, 8, 2, (8 + $ch))
    Add-T @($cx, 13, $cz) @(($cx + 2), $top, ($cz + 2)) @{ north = $side; south = $side; east = $side; west = $side; up = (Get-Face 'candle' @(0, 6, 2, 8)) }
    Add-TCross ($cx + 1) $top ($cz + 1) 1 1 '[0, 5, 1, 6]' 'candle'
    Add-TCross ($cx + 1) ($top + 0.6) ($cz + 1) 1 2 '[14, 8, 15, 10]' 'crystal' $glow
}
foreach ($p in @(@(22.6, 1.4, 3, 0), @(23.4, 5.2, 2, 4))) {
    $px = $p[0]; $pz = $p[1]; $s = $p[2]; $u = $p[3]
    Add-TPlain 'potion' $u 0 @($px, 13, $pz) @(($px + $s), (13 + $s), ($pz + $s)) $glow @('north', 'south', 'east', 'west', 'up')
    $nx = $px + $s / 2 - 0.5; $nz = $pz + $s / 2 - 0.5; $ny = 13 + $s
    Add-TPlain 'potion' 10 0 @($nx, $ny, $nz) @(($nx + 1), ($ny + 0.8), ($nz + 1)) '' @('north', 'south', 'east', 'west')
    Add-TPlain 'wood' 2 2 @(($nx - 0.1), ($ny + 0.8), ($nz - 0.1)) @(($nx + 1.1), ($ny + 1.4), ($nz + 1.1)) '' @('north', 'south', 'east', 'west', 'up')
}

$tableTex = @"
    "particle": "forgebound:block/rune_table_stone",
    "stone": "forgebound:block/rune_table_stone",
    "dark": "forgebound:block/rune_table_stone_dark",
    "gold": "forgebound:block/rune_table_gold",
    "runner": "forgebound:block/rune_table_runner",
    "drape": "forgebound:block/rune_table_drape",
    "circle": "forgebound:block/rune_table_circle",
    "glow": "forgebound:block/rune_table_glow",
    "plaque": "forgebound:block/rune_table_plaque",
    "fcry": "forgebound:block/rune_table_fcrystal",
    "band": "forgebound:block/rune_table_band",
    "crystal": "forgebound:block/rune_table_crystal",
    "lantern": "forgebound:block/rune_table_lantern",
    "chain": "forgebound:block/rune_table_chain",
    "book": "forgebound:block/rune_table_book",
    "pages": "forgebound:block/rune_table_pages",
    "wood": "forgebound:block/rune_table_wood",
    "potion": "forgebound:block/rune_table_potion",
    "candle": "minecraft:block/white_candle",
    "gem": "forgebound:block/forge_anvil_gem"
"@
Out-Model 'models\block\rune_engraving_table.json' $tableTex $e
"table: $($e.Count) elements"
# the item model: the same table re-centred on the item's origin (X 0..32 -> -8..24)
$tableBlock = $e
$e = New-Object System.Collections.Generic.List[string]
$offset = 8
foreach ($el in $tableBlock) { $e.Add($el) }
$itemJson = (Get-Content -Raw (Join-Path $assets 'models\block\rune_engraving_table.json') | ConvertFrom-Json)
foreach ($el in $itemJson.elements) {
    $el.from[0] = [Math]::Round($el.from[0] + $offset, 4); $el.to[0] = [Math]::Round($el.to[0] + $offset, 4)
    if ($el.PSObject.Properties['rotation']) { $el.rotation.origin[0] = [Math]::Round($el.rotation.origin[0] + $offset, 4) }
}
$itemJson.PSObject.Properties.Remove('parent')
$itemJson | Add-Member -NotePropertyName 'parent' -NotePropertyValue 'minecraft:block/block' -Force
$itemJson | Add-Member -NotePropertyName 'display' -NotePropertyValue ([ordered]@{
        gui                   = @{ rotation = @(30, 225, 0); translation = @(0, -2.5, 0); scale = @(0.34, 0.34, 0.34) }
        ground                = @{ rotation = @(0, 0, 0); translation = @(0, 3, 0); scale = @(0.15, 0.15, 0.15) }
        fixed                 = @{ rotation = @(0, 0, 0); translation = @(0, -2, 0); scale = @(0.3, 0.3, 0.3) }
        thirdperson_righthand = @{ rotation = @(75, 45, 0); translation = @(0, 2.5, 0); scale = @(0.22, 0.22, 0.22) }
        firstperson_righthand = @{ rotation = @(0, 45, 0); translation = @(0, 0, 0); scale = @(0.25, 0.25, 0.25) }
        firstperson_lefthand  = @{ rotation = @(0, 225, 0); translation = @(0, 0, 0); scale = @(0.25, 0.25, 0.25) }
    }) -Force
[IO.File]::WriteAllText((Join-Path $assets 'models\item\rune_engraving_table.json'), ($itemJson | ConvertTo-Json -Depth 12 -Compress), $utf8)
# the side half draws nothing (the main half renders the whole table); it keeps a particle texture for break effects
[IO.File]::WriteAllText((Join-Path $assets 'models\block\rune_engraving_table_side.json'), "{`"textures`": {`"particle`": `"forgebound:block/rune_table_stone`"}}`n", $utf8)

# item models: the blocks, scaled to fit their extra height and the anvil's horn
[IO.File]::WriteAllText((Join-Path $assets 'models\item\forge_anvil.json'), @"
{
  "parent": "forgebound:block/forge_anvil",
  "display": {
    "gui": {"rotation": [30, 225, 0], "translation": [-0.5, 0, 0], "scale": [0.55, 0.55, 0.55]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.45, 0.45, 0.45]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.35, 0.35, 0.35]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.38, 0.38, 0.38]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.38, 0.38, 0.38]}
  }
}
"@.Replace("`r`n", "`n"), $utf8)
'models written, json ok'
