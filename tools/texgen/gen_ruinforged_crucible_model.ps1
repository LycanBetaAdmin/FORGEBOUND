# Writes the Ruinforged Crucible's block models (unlit + lit), item model and blockstate into
# src/main/resources/assets/forgebound. The model's front (firebox, gem) faces north.
# Run: powershell -File tools\texgen\gen_ruinforged_crucible_model.ps1
$assets = Join-Path $PSScriptRoot '..\..\src\main\resources\assets\forgebound'
$inv = [Globalization.CultureInfo]::InvariantCulture
function Format-N([double]$v) { return $v.ToString('0.##', $inv) }
function Format-V([double[]]$v) { return '[' + (($v | ForEach-Object { Format-N $_ }) -join ', ') + ']' }
$sides = @('north', 'south', 'east', 'west', 'up', 'down')
$glow = ', "shade": false, "neoforge_data": {"block_light": 15, "sky_light": 15}'
$full = '"uv": [0, 0, 16, 16], '

# Vanilla's automatic UVs come from the element's position, so parts above y 16 or outside x/z 0..16 would sample
# outside their 16x16 texture (and pick up neighbouring atlas sprites). Work them out here, wrapped into 0..16.
function Get-Wrap([double]$a, [double]$b) {
    # slide the span back inside 0..16 whole (a face that straddles the edge keeps its full height)
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
# one element; $faces maps face name -> face json body (without braces); faces not listed are left out
function New-Elem([double[]]$from, [double[]]$to, [hashtable]$faces, [string]$extra = '') {
    $fs = ($sides | Where-Object { $faces.ContainsKey($_) } | ForEach-Object {
        $fb = $faces[$_]; if ($fb -notmatch '"uv"') { $fb = (Get-AutoUV $_ $from $to) + $fb }
        "`"$_`": {$fb}" }) -join ', '
    return "    {`"from`": $(Format-V $from), `"to`": $(Format-V $to)$extra, `"faces`": {$fs}}"
}
function Get-All([string]$body, [string[]]$only = $sides) { $h = @{}; foreach ($s in $only) { $h[$s] = $body }; return $h }
function Get-Rot([string]$axis, [double]$angle, [double[]]$origin) { return ", `"rotation`": {`"angle`": $(Format-N $angle), `"axis`": `"$axis`", `"origin`": $(Format-V $origin)}" }

$stone = '"texture": "#stone"'; $body = '"texture": "#body"'; $iron = '"uv": [8, 0, 16, 2], "texture": "#lantern"'
$e = New-Object System.Collections.Generic.List[string]

# ---- base: plinth, lower tier with the glowing grate, four corner footings
$plinth = Get-All $stone; $plinth['down'] = '"texture": "#stone", "cullface": "down"'
$e.Add((New-Elem @(0, 0, 0) @(16, 1.5, 16) $plinth))
$e.Add((New-Elem @(1, 1.5, 1) @(15, 3.5, 15) (Get-All $stone)))
foreach ($c in @(@(0, 0), @(12.5, 0), @(0, 12.5), @(12.5, 12.5))) {
    $e.Add((New-Elem @($c[0], 1.5, $c[1]) @(($c[0] + 3.5), 5, ($c[1] + 3.5)) (Get-All $stone)))
}
$e.Add((New-Elem @(5, 1.6, 0.9) @(11, 3.4, 1) @{ north = "$full`"texture`": `"#grate`"" } $glow))

# ---- body and corner pillars
$e.Add((New-Elem @(2, 3.5, 2) @(14, 15, 14) (Get-All $body)))
foreach ($c in @(@(1, 1), @(12.5, 1), @(1, 12.5), @(12.5, 12.5))) {
    $e.Add((New-Elem @($c[0], 5, $c[1]) @(($c[0] + 2.5), 15, ($c[1] + 2.5)) (Get-All $stone)))
}
# glowing side and back windows
$win = "$full`"texture`": `"#window`""
$e.Add((New-Elem @(14, 6, 5.5) @(14.1, 12, 10.5) @{ east = $win } $glow))
$e.Add((New-Elem @(1.9, 6, 5.5) @(2, 12, 10.5) @{ west = $win } $glow))
$e.Add((New-Elem @(5.5, 6, 14) @(10.5, 12, 14.1) @{ south = $win } $glow))

# ---- firebox: sill, jambs, lintel and the fire behind them
$e.Add((New-Elem @(4, 3.5, 0.3) @(12, 4.5, 2) (Get-All $stone)))
$e.Add((New-Elem @(4, 4.5, 0.8) @(5, 8.5, 2) (Get-All $stone)))
$e.Add((New-Elem @(11, 4.5, 0.8) @(12, 8.5, 2) (Get-All $stone)))
$e.Add((New-Elem @(3.5, 8.5, 0.5) @(12.5, 9.5, 2) (Get-All $stone)))
$e.Add((New-Elem @(5, 4.5, 1.8) @(11, 8.5, 1.9) @{ north = "$full`"texture`": `"#fire`"" } $glow))

# keystone over the firebox, standing proud of the lintel
$e.Add((New-Elem @(7.25, 8.2, 0.2) @(8.75, 10.2, 1.9) (Get-All $stone)))

# ---- amethyst gem in a diamond-shaped stone setting on the upper front, a trim band under the cap
$e.Add((New-Elem @(6.25, 10.5, 1) @(9.75, 14, 2) (Get-All $stone) (Get-Rot 'z' 45 @(8, 12.25, 1.5))))
$e.Add((New-Elem @(6.9, 11.15, 0.6) @(9.1, 13.35, 1) (Get-All '"texture": "#crystal"' @('north', 'east', 'west', 'up', 'down')) ((Get-Rot 'z' 45 @(8, 12.25, 0.8)) + $glow)))
$e.Add((New-Elem @(1.5, 14.3, 1.5) @(14.5, 15, 14.5) (Get-All $stone)))

# ---- cap, raised corners, upper step and the crystal's pedestal
$e.Add((New-Elem @(0, 15, 0) @(16, 16.5, 16) (Get-All $stone)))
foreach ($c in @(@(0, 0), @(12.5, 0), @(0, 12.5), @(12.5, 12.5))) {
    $e.Add((New-Elem @($c[0], 16.5, $c[1]) @(($c[0] + 3.5), 17.5, ($c[1] + 3.5)) (Get-All $stone)))
}
$e.Add((New-Elem @(3.5, 16.5, 3.5) @(12.5, 17.5, 12.5) (Get-All $body)))
$e.Add((New-Elem @(5, 17.5, 5) @(11, 18.5, 11) (Get-All $stone)))

# ---- amethyst crystal cluster (glowing): a tall turned centre spire with a tip, four shards leaning out
$cry = Get-All '"texture": "#crystal"' @('north', 'south', 'east', 'west', 'up')
$e.Add((New-Elem @(6.5, 18.5, 6.5) @(9.5, 24.5, 9.5) $cry ((Get-Rot 'y' 45 @(8, 18.5, 8)) + $glow)))
$e.Add((New-Elem @(7.25, 24.5, 7.25) @(8.75, 26, 8.75) $cry ((Get-Rot 'y' 45 @(8, 24.5, 8)) + $glow)))
$e.Add((New-Elem @(4.5, 18.5, 7) @(6.5, 22, 9) $cry ((Get-Rot 'z' 22.5 @(5.5, 18.5, 8)) + $glow)))
$e.Add((New-Elem @(9.5, 18.5, 7) @(11.5, 22, 9) $cry ((Get-Rot 'z' -22.5 @(10.5, 18.5, 8)) + $glow)))
$e.Add((New-Elem @(7, 18.5, 4.5) @(9, 21.5, 6.5) $cry ((Get-Rot 'x' -22.5 @(8, 18.5, 5.5)) + $glow)))
$e.Add((New-Elem @(7, 18.5, 9.5) @(9, 21.5, 11.5) $cry ((Get-Rot 'x' 22.5 @(8, 18.5, 10.5)) + $glow)))

# ---- two lanterns hanging on chains from iron brackets under the cap, outside the front of each side
$glass = '"uv": [0, 0, 6, 7], "texture": "#lantern"'; $cap = '"uv": [8, 2, 12, 5], "texture": "#lantern"'; $chain = '"uv": [12, 2, 15, 8], "texture": "#lantern"'
foreach ($cx in @(-0.5, 16.5)) {
    $cz = 5
    $bx0 = if ($cx -lt 8) { -1.25 } else { 16 }; $bx1 = if ($cx -lt 8) { 0 } else { 17.25 }
    $e.Add((New-Elem @($bx0, 14.4, ($cz - 0.4)) @($bx1, 15, ($cz + 0.4)) (Get-All $iron)))
    $e.Add((New-Elem @(($cx - 0.75), 12.8, $cz) @(($cx + 0.75), 14.4, $cz) @{ north = $chain; south = $chain }))
    $e.Add((New-Elem @($cx, 12.8, ($cz - 0.75)) @($cx, 14.4, ($cz + 0.75)) @{ east = $chain; west = $chain }))
    $e.Add((New-Elem @(($cx - 1), 12.1, ($cz - 1)) @(($cx + 1), 12.8, ($cz + 1)) (Get-All $cap)))
    $e.Add((New-Elem @(($cx - 1.75), 11.6, ($cz - 1.75)) @(($cx + 1.75), 12.1, ($cz + 1.75)) (Get-All $iron)))
    $e.Add((New-Elem @(($cx - 1.5), 8.1, ($cz - 1.5)) @(($cx + 1.5), 11.6, ($cz + 1.5)) (Get-All $glass @('north', 'south', 'east', 'west')) $glow))
    $e.Add((New-Elem @(($cx - 1.75), 7.6, ($cz - 1.75)) @(($cx + 1.75), 8.1, ($cz + 1.75)) (Get-All $iron)))
}

$tex = @"
  "textures": {
    "particle": "forgebound:block/ruinforged_crucible_stone",
    "stone": "forgebound:block/ruinforged_crucible_stone",
    "body": "forgebound:block/ruinforged_crucible_body",
    "fire": "forgebound:block/ruinforged_crucible_fire_off",
    "grate": "forgebound:block/ruinforged_crucible_grate",
    "window": "forgebound:block/ruinforged_crucible_window",
    "crystal": "forgebound:block/ruinforged_crucible_crystal",
    "lantern": "forgebound:block/ruinforged_crucible_lantern"
  },
"@
$model = "{`n  `"parent`": `"minecraft:block/block`",`n  `"render_type`": `"minecraft:cutout`",`n$tex`n  `"elements`": [`n" + ($e -join ",`n") + "`n  ]`n}`n"
$lit = "{`n  `"parent`": `"forgebound:block/ruinforged_crucible`",`n  `"textures`": {`n    `"fire`": `"forgebound:block/ruinforged_crucible_fire`"`n  }`n}`n"
$item = @"
{
  "parent": "forgebound:block/ruinforged_crucible_lit",
  "display": {
    "gui": {"rotation": [30, 225, 0], "translation": [0, -1.75, 0], "scale": [0.46, 0.46, 0.46]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.22, 0.22, 0.22]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, -1, 0], "scale": [0.42, 0.42, 0.42]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.32, 0.32, 0.32]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.32, 0.32, 0.32]}
  }
}
"@
$variants = foreach ($l in @('false', 'true')) {
    foreach ($f in @(@('north', 0), @('east', 90), @('south', 180), @('west', 270))) {
        $m = if ($l -eq 'true') { 'forgebound:block/ruinforged_crucible_lit' } else { 'forgebound:block/ruinforged_crucible' }
        $y = if ($f[1] -ne 0) { ", `"y`": $($f[1])" } else { '' }
        "    `"facing=$($f[0]),lit=$l`": {`"model`": `"$m`"$y}"
    }
}
$state = "{`n  `"variants`": {`n" + ($variants -join ",`n") + "`n  }`n}`n"

$utf8 = New-Object System.Text.UTF8Encoding $false
[IO.File]::WriteAllText((Join-Path $assets 'models\block\ruinforged_crucible.json'), $model, $utf8)
[IO.File]::WriteAllText((Join-Path $assets 'models\block\ruinforged_crucible_lit.json'), $lit, $utf8)
[IO.File]::WriteAllText((Join-Path $assets 'models\item\ruinforged_crucible.json'), $item.Replace("`r`n", "`n"), $utf8)
[IO.File]::WriteAllText((Join-Path $assets 'blockstates\ruinforged_crucible.json'), $state, $utf8)
foreach ($p in @('models\block\ruinforged_crucible.json', 'models\block\ruinforged_crucible_lit.json', 'models\item\ruinforged_crucible.json', 'blockstates\ruinforged_crucible.json')) {
    Get-Content -Raw (Join-Path $assets $p) | ConvertFrom-Json | Out-Null
}
"model written: $($e.Count) elements, json ok"
