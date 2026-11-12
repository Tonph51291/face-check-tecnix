# test_emulator_suite.ps1 - Bộ công cụ kiểm thử hiệu năng tự động trên máy ảo Android (AVD)
# Dự án: FaceCheckTecnix
# Cách chạy: powershell -ExecutionPolicy Bypass -File .\tools\test_emulator_suite.ps1 -Scenario 2 -DurationSec 60

param(
    [string]$PackageName = "com.example.facechecktecnix",
    [int]$Scenario = 2,
    [int]$DurationSec = 60,
    [int]$SampleIntervalSec = 3,
    [string]$OutputDir = ".\profiling_results"
)

$scenarioNames = @{
    1 = "Scenario1_Baseline_Idle"
    2 = "Scenario2_Camera_NoFace_MLKitUnthrottled"
    3 = "Scenario3_Camera_FaceTracking_Recomposition"
    4 = "Scenario4_Attendance_Inference"
    5 = "Scenario5_Enrollment_LeakCheck"
    6 = "Scenario6_Endurance_10Min_SoakTest"
}

$testName = if ($scenarioNames.ContainsKey($Scenario)) { $scenarioNames[$Scenario] } else { "Scenario_Custom" }

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " [FaceCheckTecnix] BỘ TEST HIỆU NĂNG MÁY ẢO ANDROID (AVD) " -ForegroundColor Cyan
Write-Host " Kịch bản: $testName ($Scenario)" -ForegroundColor Yellow
Write-Host " Thời lượng đo: $DurationSec giây (lấy mẫu mỗi $SampleIntervalSec s)" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Kiểm tra thiết bị ADB
$devices = @(adb devices | Where-Object { $_ -match "\tdevice$" })
if ($devices.Count -eq 0) {
    Write-Host "[LỖI] Không tìm thấy thiết bị hoặc máy ảo nào đang chạy qua ADB!" -ForegroundColor Red
    Write-Host "Vui lòng bật Android Emulator (Pixel_7, Pixel_4 hoặc Small_Tablet) rồi chạy lại." -ForegroundColor Yellow
    exit 1
}

$activeDevice = ($devices[0] -split "\t")[0].Trim()
Write-Host "[OK] Đã kết nối với Android Device/Emulator: $activeDevice" -ForegroundColor Green

# 2. Kiểm tra và cài đặt APK Debug nếu cần
$apkPath = ".\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    Write-Host "[INFO] Đang cài đặt/cập nhật APK debug lên máy ảo..." -ForegroundColor Gray
    adb -s $activeDevice install -r $apkPath | Out-Null
    Write-Host "[OK] Đã cài đặt APK thành công!" -ForegroundColor Green
    
    # Cấp quyền Camera tự động
    adb -s $activeDevice shell pm grant $PackageName android.permission.CAMERA 2>$null
    Write-Host "[OK] Đã cấp quyền Camera (android.permission.CAMERA)." -ForegroundColor Green
} else {
    Write-Host "[CẢNH BÁO] Chưa tìm thấy $apkPath. Hãy build trước bằng: .\gradlew assembleDebug" -ForegroundColor Yellow
}

# 3. Tạo thư mục lưu kết quả
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$csvFile = Join-Path $OutputDir "${testName}_${timestamp}.csv"
$header = "Timestamp,ElapsedSec,CPU_App_Percent,RAM_PSS_MB,RAM_JavaHeap_MB,RAM_NativeHeap_MB,RAM_Gfx_MB,ThreadCount,GC_Count"
Set-Content -Path $csvFile -Value $header

# 4. Xác định PID của ứng dụng
$pidRaw = adb -s $activeDevice shell "pidof $PackageName"
$pidApp = if ($pidRaw) { ($pidRaw.ToString().Trim() -split "\s+")[0] } else { "" }
if (-not $pidApp) {
    Write-Host "[INFO] Đang khởi động ứng dụng $PackageName..." -ForegroundColor Gray
    adb -s $activeDevice shell "monkey -p $PackageName -c android.intent.category.LAUNCHER 1" | Out-Null
    Start-Sleep -Seconds 3
    $pidRaw = adb -s $activeDevice shell "pidof $PackageName"
    $pidApp = if ($pidRaw) { ($pidRaw.ToString().Trim() -split "\s+")[0] } else { "" }
}

Write-Host "[OK] Tiến trình đang chạy với PID: $pidApp" -ForegroundColor Green
Write-Host "Bắt đầu thu thập dữ liệu..." -ForegroundColor Cyan
Write-Host "------------------------------------------------------------------------------------------------" -ForegroundColor DarkGray

$startTime = Get-Date
$endTime = $startTime.AddSeconds($DurationSec)
$prevGcCount = 0

# Xóa bộ đệm logcat để đếm GC chuẩn xác
adb -s $activeDevice logcat -c

while ((Get-Date) -lt $endTime) {
    $now = Get-Date
    $elapsed = [int]($now - $startTime).TotalSeconds
    $timeStr = $now.ToString("HH:mm:ss")

    # 1. Đo CPU bằng top
    $topRaw = (adb -s $activeDevice shell "top -b -n 1 -p $pidApp") | Out-String
    $cpuUsage = "0.0"
    if ($topRaw) {
        $lines = $topRaw -split "`n"
        foreach ($line in $lines) {
            $trimmed = $line.Trim()
            if ($trimmed -match "^$pidApp\s+") {
                $cols = -split $trimmed
                if ($cols.Length -ge 9) {
                    $cpuUsage = $cols[8]
                }
            }
        }
    }

    # 2. Đo RAM PSS qua dumpsys meminfo
    $memRaw = (adb -s $activeDevice shell "dumpsys meminfo $pidApp") | Out-String
    $pssMb = 0.0
    $javaMb = 0.0
    $nativeMb = 0.0
    $gfxMb = 0.0

    if ($memRaw) {
        if ($memRaw -match "TOTAL PSS:\s*(\d+)") {
            $pssMb = [math]::Round([float]$matches[1] / 1024.0, 1)
        }
        if ($memRaw -match "Java Heap:\s*(\d+)") {
            $javaMb = [math]::Round([float]$matches[1] / 1024.0, 1)
        }
        if ($memRaw -match "Native Heap:\s*(\d+)") {
            $nativeMb = [math]::Round([float]$matches[1] / 1024.0, 1)
        }
        if ($memRaw -match "Graphics:\s*(\d+)") {
            $gfxMb = [math]::Round([float]$matches[1] / 1024.0, 1)
        }
    }

    # 3. Đếm Thread
    $threadCount = "N/A"
    $tOut = adb -s $activeDevice shell "ls /proc/$pidApp/task 2>/dev/null | wc -l"
    if ($tOut) {
        $threadCount = $tOut.Trim()
    }

    # 4. Đếm số lần GC trong logcat từ đầu phiên
    $gcLog = adb -s $activeDevice logcat -d -s "art:I" | Where-Object { $_ -match "concurrent copying GC" -or $_ -match "explicit concurrent" }
    $gcCount = if ($gcLog) { ($gcLog | Measure-Object).Count } else { 0 }

    # Ghi CSV
    $row = "$timeStr,$elapsed,$cpuUsage,$pssMb,$javaMb,$nativeMb,$gfxMb,$threadCount,$gcCount"
    Add-Content -Path $csvFile -Value $row

    # Cảnh báo màu theo ngưỡng hiệu năng
    $cpuColor = if ([float]$cpuUsage -ge 40.0) { "Red" } elseif ([float]$cpuUsage -ge 20.0) { "Yellow" } else { "Green" }
    Write-Host "[$timeStr] +$($elapsed.ToString().PadLeft(4))s | CPU: $($cpuUsage.PadLeft(5))% " -NoNewline -ForegroundColor $cpuColor
    Write-Host "| RAM PSS: $($pssMb.ToString().PadLeft(5)) MB (Java: $javaMb MB, Native: $nativeMb MB, Gfx: $gfxMb MB) | Threads: $threadCount | GC Count: $gcCount" -ForegroundColor White

    Start-Sleep -Seconds $SampleIntervalSec
}

Write-Host "------------------------------------------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host "[HOÀN THÀNH] Kết quả đã lưu tại: $csvFile" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
