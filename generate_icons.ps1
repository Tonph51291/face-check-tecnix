Add-Type -AssemblyName System.Drawing

$srcPath = "app\src\main\res\drawable\logo_tecnix.png"
$srcImg = [System.Drawing.Image]::FromFile((Resolve-Path $srcPath))

$densities = @(
    @{ Name = "mdpi"; Size = 48 },
    @{ Name = "hdpi"; Size = 72 },
    @{ Name = "xhdpi"; Size = 96 },
    @{ Name = "xxhdpi"; Size = 144 },
    @{ Name = "xxxhdpi"; Size = 192 }
)

foreach ($item in $densities) {
    $d = $item.Name
    $size = $item.Size
    $dir = "app\src\main\res\mipmap-$d"

    # 1. Square icon on white
    $bmpSquare = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmpSquare)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.Clear([System.Drawing.Color]::White)

    $pad = [int]($size * 0.10)
    $drawSize = $size - 2 * $pad
    $g.DrawImage($srcImg, $pad, $pad, $drawSize, $drawSize)
    $g.Dispose()

    # 2. Round icon on white circle
    $bmpRound = New-Object System.Drawing.Bitmap $size, $size
    $g2 = [System.Drawing.Graphics]::FromImage($bmpRound)
    $g2.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g2.Clear([System.Drawing.Color]::Transparent)

    $brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
    $g2.FillEllipse($brush, 0, 0, $size - 1, $size - 1)
    $brush.Dispose()

    $padR = [int]($size * 0.14)
    $drawSizeR = $size - 2 * $padR
    $g2.DrawImage($srcImg, $padR, $padR, $drawSizeR, $drawSizeR)
    $g2.Dispose()

    # Remove old webp files
    Remove-Item "$dir\ic_launcher.webp" -ErrorAction SilentlyContinue
    Remove-Item "$dir\ic_launcher_round.webp" -ErrorAction SilentlyContinue

    # Save as PNG
    $bmpSquare.Save("$dir\ic_launcher.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmpRound.Save("$dir\ic_launcher_round.png", [System.Drawing.Imaging.ImageFormat]::Png)

    $bmpSquare.Dispose()
    $bmpRound.Dispose()
}

$srcImg.Dispose()
Write-Host "Icons generated successfully!"

