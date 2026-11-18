# Generates the Ruinforged Crucible's 16x16 block textures (mauve-grey stone, dark obsidian body, the octagonal
# violet fire lit and unlit, the glowing grate and side windows, the amethyst crystal and the lantern parts) into
# src/main/resources/assets/forgebound/textures/block.
# Run: powershell -File tools\texgen\gen_ruinforged_crucible.ps1
Add-Type -AssemblyName System.Drawing
$out = Join-Path $PSScriptRoot '..\..\src\main\resources\assets\forgebound\textures\block'
New-Item -ItemType Directory -Force $out | Out-Null
$rng = New-Object System.Random 7171

function New-Tex { return New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function Get-Col([string]$hex, [int]$a = 255) {
    $v = [Convert]::ToInt32($hex.TrimStart('#'), 16)
    return [System.Drawing.Color]::FromArgb($a, ($v -shr 16) -band 255, ($v -shr 8) -band 255, $v -band 255)
}
function Set-Px($b, [int]$x, [int]$y, [string]$hex) { if ($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16) { $b.SetPixel($x, $y, (Get-Col $hex)) } }
function Save-Tex($b, [string]$name) { $b.Save((Join-Path $out "ruinforged_crucible_$name.png"), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }
function Get-Pick([string[]]$cols, [int[]]$weights) {
    $n = $rng.Next(($weights | Measure-Object -Sum).Sum)
    for ($i = 0; $i -lt $cols.Count; $i++) { if ($n -lt $weights[$i]) { return $cols[$i] }; $n -= $weights[$i] }
    return $cols[-1]
}

# ---- stone: mauve-grey dressed blocks in two staggered courses, dark mortar, a lit top edge and a shadowed
# bottom edge on every block so the masonry reads at a distance
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $row = [Math]::Floor($y / 8); $ly = $y % 8
        $joint = if ($row -eq 0) { @(7) } else { @(3, 11) }
        if ($ly -eq 7 -or $joint -contains $x) { Set-Px $b $x $y '#4F4656'; continue }
        $c = Get-Pick @('#8E8494', '#978D9C', '#857B8C', '#A0969F') @(5, 4, 3, 1)
        if ($ly -eq 0) { $c = '#B0A6B3' }
        elseif ($ly -eq 6) { $c = '#6E6476' }
        elseif ($joint -contains ($x - 1)) { $c = '#A69CAA' }
        elseif (($joint -contains ($x + 1)) -or ($x -eq 15 -and $row -eq 0)) { $c = '#756B7D' }
        Set-Px $b $x $y $c
    }
}
Save-Tex $b 'stone'

# ---- body: dark obsidian with violet specks and faint vertical panel seams
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $c = Get-Pick @('#21142E', '#2A1A3B', '#1A0F25', '#352048', '#46295E') @(6, 5, 4, 2, 1)
        if ($x -eq 4 -or $x -eq 11) { $c = '#160C20' }
        Set-Px $b $x $y $c
    }
}
Save-Tex $b 'body'


# ---- fire (lit): dark hearth, violet tongues rising from a white-hot base
function New-Fire([bool]$lit) {
    $b = New-Tex
    for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { Set-Px $b $x $y (Get-Pick @('#150A1F', '#1C0E2A') @(3, 1)) } }
    for ($x = 0; $x -lt 16; $x++) {
        if ($lit) {
            $hgt = 7 + $rng.Next(6); if (($x % 4) -eq 1) { $hgt += 3 }
            for ($k = 0; $k -lt $hgt; $k++) {
                $y = 15 - $k; $t = $k / [double]$hgt
                $c = if ($t -lt 0.2) { '#F7E6FF' } elseif ($t -lt 0.4) { '#E2B3FF' } elseif ($t -lt 0.65) { '#C277FA' } elseif ($t -lt 0.85) { '#9447DD' } else { '#6A2BA8' }
                Set-Px $b $x $y $c
            }
        } else {
            $hgt = 1 + $rng.Next(3)
            for ($k = 0; $k -lt $hgt; $k++) { Set-Px $b $x (15 - $k) (Get-Pick @('#4A1F70', '#6A2BA8', '#33174D') @(3, 1, 3)) }
        }
    }
    return $b
}
Save-Tex (New-Fire $true) 'fire'
Save-Tex (New-Fire $false) 'fire_off'

# ---- grate: glowing violet slots between dark bars
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $col = $x % 4
        if ($col -eq 0) { Set-Px $b $x $y '#2A2030' }
        elseif ($y -lt 2 -or $y -gt 13) { Set-Px $b $x $y '#3A2E44' }
        elseif ($col -eq 2) { Set-Px $b $x $y '#F0D0FF' }
        else { Set-Px $b $x $y '#B35CF5' }
    }
}
Save-Tex $b 'grate'

# ---- side window: violet glow, brighter in the middle, dark rim
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $d = [Math]::Max([Math]::Abs($x - 7.5), [Math]::Abs($y - 7.5))
        $c = if ($d -gt 6.6) { '#3B1C5A' } elseif ($d -gt 5) { '#8A35D8' } elseif ($d -gt 2.5) { '#B25CF4' } else { '#DDA6FF' }
        Set-Px $b $x $y $c
    }
}
Save-Tex $b 'window'

# ---- crystal: amethyst facets, light on the left, deep on the right, a bright glint
$b = New-Tex
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $c = if ($x -lt 4) { '#D29BFF' } elseif ($x -lt 8) { '#B26AF5' } elseif ($x -lt 12) { '#9446E6' } else { '#7330C4' }
        if ((($x + 2 * $y) % 11) -eq 0) { $c = '#E6C2FF' }
        if ((($x - $y + 16) % 9) -eq 0 -and $x -ge 8) { $c = '#6326AE' }
        Set-Px $b $x $y $c
    }
}
Set-Px $b 2 2 '#FFFFFF'; Set-Px $b 3 3 '#F3E0FF'
Save-Tex $b 'crystal'

# ---- lantern parts: glass face (0..5, 0..6: iron frame, warm glow inside), iron plate (8..15, 0..1),
# cap (8..11, 2..4), copper chain links (12..14, 2..15, transparent between links)
$b = New-Tex
for ($y = 0; $y -lt 7; $y++) {
    for ($x = 0; $x -lt 6; $x++) {
        if ($x -eq 0 -or $x -eq 5 -or $y -eq 0 -or $y -eq 6) { Set-Px $b $x $y '#3A3330' }
        else {
            $d = [Math]::Abs($x - 2.5) + [Math]::Abs($y - 3.5) * 0.7
            $c = if ($d -lt 1.2) { '#FFF1B8' } elseif ($d -lt 2) { '#FFD36A' } else { '#F2A43A' }
            Set-Px $b $x $y $c
        }
    }
}
for ($x = 8; $x -lt 16; $x++) { Set-Px $b $x 0 '#4A423E'; Set-Px $b $x 1 '#352E2B' }
for ($x = 8; $x -lt 12; $x++) { for ($y = 2; $y -lt 5; $y++) { if ($y -eq 2) { Set-Px $b $x $y '#5A514C' } else { Set-Px $b $x $y '#3A3330' } } }
for ($y = 2; $y -lt 16; $y++) {
    $ph = ($y - 2) % 4
    if ($ph -eq 0 -or $ph -eq 3) { Set-Px $b 12 $y '#B8743A'; Set-Px $b 13 $y '#D99052'; Set-Px $b 14 $y '#8C5226' }
    else { Set-Px $b 12 $y '#B8743A'; Set-Px $b 14 $y '#8C5226' }
}
Save-Tex $b 'lantern'
'textures written'
