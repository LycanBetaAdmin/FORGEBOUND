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

# =====================================================================  Rune Engraving Table (front = north)
$e = New-Object System.Collections.Generic.List[string]
$plinth = Get-All 'stone'; $plinth['down'] = '"texture": "#stone", "cullface": "down"'
$e.Add((New-Elem @(0, 0, 0.5) @(16, 2, 15.5) $plinth))                                   # plinth
$e.Add((New-Elem @(1, 2, 1.5) @(15, 10, 14.5) (Get-All 'dark')))                         # body
foreach ($x in @(0.5, 12.5)) { $e.Add((New-Elem @($x, 2, 1) @(($x + 3), 10, 4) (Get-All 'stone'))) }   # front corner posts
foreach ($x in @(0.5, 12.5)) { $e.Add((New-Elem @($x, 2, 12) @(($x + 3), 10, 15) (Get-All 'stone'))) } # back corner posts
# glowing windows: two in the centre front, one on each post, one each side
foreach ($wx in @(5, 9.5)) { $e.Add((New-Elem @($wx, 3.5, 1.45) @(($wx + 1.5), 6, 1.5) (Get-One 'north' 'window' $full) $glow)) }
foreach ($wx in @(1.25, 13.25)) { $e.Add((New-Elem @($wx, 3.5, 0.95) @(($wx + 1.5), 7, 1) (Get-One 'north' 'window' $full) $glow)) }
$e.Add((New-Elem @(14.5, 4, 6) @(14.55, 8, 10) (Get-One 'east' 'window' $full) $glow))
$e.Add((New-Elem @(1.45, 4, 6) @(1.5, 8, 10) (Get-One 'west' 'window' $full) $glow))
# gold trim: under the top, framing the centre panel, and the gem setting with its gem
$e.Add((New-Elem @(0.5, 9.5, 0.9) @(15.5, 10, 15.1) (Get-All 'gold' @('north', 'south', 'east', 'west'))))
$e.Add((New-Elem @(4.5, 2.5, 1.4) @(4.9, 9.5, 1.5) (Get-One 'north' 'gold')))
$e.Add((New-Elem @(11.1, 2.5, 1.4) @(11.5, 9.5, 1.5) (Get-One 'north' 'gold')))
$e.Add((New-Elem @(7, 6.3, 1.2) @(9, 8.3, 1.5) (Get-All 'gold' @('north', 'up', 'down', 'east', 'west')) (Get-Rot 'z' 45 @(8, 7.3, 1.35))))
$e.Add((New-Elem @(7.4, 6.7, 1.1) @(8.6, 7.9, 1.2) (Get-One 'north' 'gem' $full) ((Get-Rot 'z' 45 @(8, 7.3, 1.15)) + $glow)))
# top slab, the purple runner and the glowing magic circle
$e.Add((New-Elem @(0, 10, 0) @(16, 12, 16) (Get-All 'stone')))
foreach ($c in @(@(0, 0), @(13, 0), @(0, 13), @(13, 13))) { $e.Add((New-Elem @(($c[0] - 0.1), 11.5, ($c[1] - 0.1)) @(($c[0] + 3.1), 12.1, ($c[1] + 3.1)) (Get-All 'gold' @('north', 'south', 'east', 'west', 'up')))) }
$e.Add((New-Elem @(2.5, 12, -0.05) @(13.5, 12.2, 12) (Get-All 'runner' @('north', 'up', 'east', 'west'))))
$e.Add((New-Elem @(3, 12.21, 0.5) @(13, 12.22, 10.5) (Get-One 'up' 'circle' $full) $glow))
# banners hanging over the front edge
foreach ($bx in @(2.8, 9.7)) { $e.Add((New-Elem @($bx, 3.5, -0.1) @(($bx + 3.5), 12.2, -0.05) (Get-One 'north' 'banner' $full))) }
# back pillars with rune windows, gold caps, the arch, the plaque
foreach ($x in @(0.5, 12.5)) {
    $e.Add((New-Elem @($x, 12, 12.5) @(($x + 3), 24, 15.5) (Get-All 'dark')))
    $e.Add((New-Elem @(($x + 0.75), 15, 12.45) @(($x + 2.25), 21, 12.5) (Get-One 'north' 'window' $full) $glow))
    $e.Add((New-Elem @(($x - 0.3), 24, 12.2) @(($x + 3.3), 25.5, 15.8) (Get-All 'gold')))
    $e.Add((New-Elem @(($x - 0.2), 12, 12.3) @(($x + 3.2), 13, 15.7) (Get-All 'gold' @('north', 'south', 'east', 'west'))))
}
$e.Add((New-Elem @(3.5, 22, 13) @(12.5, 24, 15) (Get-All 'dark')))
$e.Add((New-Elem @(5.5, 21.5, 12.6) @(10.5, 26.5, 12.9) (Get-All 'gold' @('south', 'east', 'west', 'up', 'down'))))
$e.Add((New-Elem @(5.5, 21.5, 12.55) @(10.5, 26.5, 12.6) (Get-One 'north' 'plaque' $full) $glow))
# chains down from the arch to a gold ring round the floating crystal
foreach ($cx in @(5.5, 10.5)) {
    $e.Add((New-Elem @(($cx - 0.5), 16.5, 13.5) @(($cx + 0.5), 22, 13.5) @{ north = "`"uv`": [6, 0, 10, 11], `"texture`": `"#chain`""; south = "`"uv`": [6, 0, 10, 11], `"texture`": `"#chain`"" }))
}
$e.Add((New-Elem @(5, 16, 11.5) @(11, 16.6, 12) (Get-All 'gold')))
$e.Add((New-Elem @(5, 16, 15) @(11, 16.6, 15.5) (Get-All 'gold')))
$e.Add((New-Elem @(5, 16, 12) @(5.5, 16.6, 15) (Get-All 'gold')))
$e.Add((New-Elem @(10.5, 16, 12) @(11, 16.6, 15) (Get-All 'gold')))
$cry = Get-All 'crystal' @('north', 'south', 'east', 'west', 'up', 'down')
$e.Add((New-Elem @(7, 15, 12.75) @(9, 20, 14.75) $cry ((Get-Rot 'y' 45 @(8, 15, 13.75)) + $glow)))
$e.Add((New-Elem @(7.5, 13.8, 13.25) @(8.5, 15, 14.25) $cry ((Get-Rot 'y' 45 @(8, 13.8, 13.75)) + $glow)))
# violet lanterns hanging from the arch ends
foreach ($lx in @(4.5, 11.5)) {
    $e.Add((New-Elem @(($lx - 0.4), 19.5, 14) @(($lx + 0.4), 22, 14) @{ north = "`"uv`": [6, 0, 10, 5], `"texture`": `"#chain`""; south = "`"uv`": [6, 0, 10, 5], `"texture`": `"#chain`"" }))
    $e.Add((New-Elem @(($lx - 1), 19, 13) @(($lx + 1), 19.5, 15) (Get-All 'lantern' @('north', 'south', 'east', 'west', 'up')) ''))
    $e.Add((New-Elem @(($lx - 1), 16.5, 13) @(($lx + 1), 19, 15) @{ north = '"uv": [0, 0, 6, 7], "texture": "#lantern"'; south = '"uv": [0, 0, 6, 7], "texture": "#lantern"'; east = '"uv": [0, 0, 6, 7], "texture": "#lantern"'; west = '"uv": [0, 0, 6, 7], "texture": "#lantern"' } $glow))
    $e.Add((New-Elem @(($lx - 1.2), 16.1, 12.8) @(($lx + 1.2), 16.5, 15.2) @{ north = '"uv": [8, 0, 16, 2], "texture": "#lantern"'; south = '"uv": [8, 0, 16, 2], "texture": "#lantern"'; east = '"uv": [8, 0, 16, 2], "texture": "#lantern"'; west = '"uv": [8, 0, 16, 2], "texture": "#lantern"'; down = '"uv": [8, 0, 16, 2], "texture": "#lantern"' }))
}
# on the desk: a stack of books and an open book (left), quill and ink, crystals and a potion rack (right)
$e.Add((New-Elem @(0.8, 12, 7.5) @(4.6, 13.6, 11.5) (Get-All 'book' @('north', 'south', 'east', 'west', 'up'))))
$e.Add((New-Elem @(1.1, 13.6, 7.8) @(4.3, 15, 11.2) (Get-All 'book' @('north', 'south', 'east', 'west', 'up'))))
$e.Add((New-Elem @(1.5, 12, 2.5) @(6.5, 12.6, 6.5) (Get-All 'book' @('north', 'south', 'east', 'west'))))
$e.Add((New-Elem @(1.7, 12.6, 2.7) @(4, 13.3, 6.3) (Get-One 'up' 'pages' '"uv": [0, 0, 8, 16], ') (Get-Rot 'z' -22.5 @(4, 12.6, 4.5))))
$e.Add((New-Elem @(4, 12.6, 2.7) @(6.3, 13.3, 6.3) (Get-One 'up' 'pages' '"uv": [8, 0, 16, 16], ') (Get-Rot 'z' 22.5 @(4, 12.6, 4.5))))
$e.Add((New-Elem @(10.6, 12, 3) @(11.6, 13, 4) (Get-All 'dark' @('north', 'south', 'east', 'west', 'up'))))
$e.Add((New-Elem @(11.2, 12.8, 3.4) @(11.4, 16, 3.6) (Get-All 'quill' @('north', 'south', 'east', 'west')) (Get-Rot 'z' -22.5 @(11.3, 12.8, 3.5))))
$e.Add((New-Elem @(12.5, 12, 8.5) @(14, 16, 10) $cry ((Get-Rot 'y' 45 @(13.25, 12, 9.25)) + $glow)))
$e.Add((New-Elem @(11.3, 12, 9.2) @(12.3, 14.5, 10.2) $cry ((Get-Rot 'z' 22.5 @(11.8, 12, 9.7)) + $glow)))
$e.Add((New-Elem @(10.8, 12, 5) @(15, 12.6, 7.5) (Get-All 'wood' @('north', 'south', 'east', 'west', 'up'))))
foreach ($px in @(11.3, 12.6, 13.9)) { $e.Add((New-Elem @($px, 12.6, 5.6) @(($px + 0.9), 14.2, 6.6) (Get-All 'potion' @('north', 'south', 'east', 'west', 'up')) $glow)) }
Out-Model 'models\block\rune_engraving_table.json' @"
    "particle": "forgebound:block/rune_table_stone",
    "stone": "forgebound:block/rune_table_stone",
    "dark": "forgebound:block/rune_table_stone_dark",
    "gold": "forgebound:block/rune_table_gold",
    "runner": "forgebound:block/rune_table_runner",
    "circle": "forgebound:block/rune_table_circle",
    "banner": "forgebound:block/rune_table_banner",
    "window": "forgebound:block/rune_table_window",
    "plaque": "forgebound:block/rune_table_plaque",
    "crystal": "forgebound:block/rune_table_crystal",
    "lantern": "forgebound:block/rune_table_lantern",
    "chain": "forgebound:block/rune_table_chain",
    "book": "forgebound:block/rune_table_book",
    "pages": "forgebound:block/rune_table_pages",
    "wood": "forgebound:block/rune_table_wood",
    "potion": "forgebound:block/rune_table_potion",
    "quill": "forgebound:block/rune_table_quill",
    "gem": "forgebound:block/forge_anvil_gem"
"@ $e
"table: $($e.Count) elements"

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
[IO.File]::WriteAllText((Join-Path $assets 'models\item\rune_engraving_table.json'), @"
{
  "parent": "forgebound:block/rune_engraving_table",
  "display": {
    "gui": {"rotation": [30, 225, 0], "translation": [0, -2.5, 0], "scale": [0.42, 0.42, 0.42]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.2, 0.2, 0.2]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, -2, 0], "scale": [0.38, 0.38, 0.38]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.32, 0.32, 0.32]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.32, 0.32, 0.32]}
  }
}
"@.Replace("`r`n", "`n"), $utf8)
'models written, json ok'
