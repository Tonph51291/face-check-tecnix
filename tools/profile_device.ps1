# profile_device.ps1 - Automated Performance Profiling Script for FaceCheckTecnix
# Usage: powershell -ExecutionPolicy Bypass -File .\tools\profile_device.ps1 -TestName "Test2_Camera_NoFace" -DurationMinutes 10

param(
    [string]$PackageName = "com.example.facechecktecnix",
    [string]$TestName = "Baseline_Test",
    [int]$IntervalSeconds = 5,
    [int]$DurationMinutes = 10,
    [string]$OutputDir = ".\profiling_results"
)

# 1. Kiểm tra kết nối ADB
$devices = adb devices | Where-Object { $_ -match "\tdevice$" }
if (-not $devices) {
    Write-Host "[ERROR] Không tìm thấy thiết bị Android nào kết nối qua ADB!" -ForegroundColor Red
    Write-Host "Vui lòng:" -ForegroundColor Yellow
    Write-Host "  1. Cắm cáp USB và bật 'USB Debugging' trên thiết bị POS/Tablet."
    Write-Host "  2. Hoặc kết nối qua Wi-Fi bằng lệnh: adb connect <IP_THIET_BI>:5555"
    exit 1
}

$deviceName = ($devices[0] -split "\t")[0]
Write-Host "[OK] Đã kết nối với thiết bị: $deviceName" -ForegroundColor Green

# 2. Tạo thư mục kết quả
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$timestampStr = Get-Date -Format "yyyyMMdd_HHmmss"
$logFile = Join-Path $OutputDir "${TestName}_${timestampStr}.csv"
$rawDumpFile = Join-Path $OutputDir "${TestName}_${timestampStr}_dumpsys.txt"

# Ghi tiêu đề CSV
$csvHeader = "Timestamp,ElapsedSec,CPU_App_Percent,RAM_Total_PSS_MB,RAM_Java_MB,RAM_Native_MB,RAM_Gfx_MB,Battery_Temp_C,Thermal_Status,Thread_Count"
Set-Content -Path $logFile -Value $csvHeader

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " BẮT ĐẦU ĐO HIỆU NĂNG: $TestName" -ForegroundColor Cyan
Write-Host " Package: $PackageName" -ForegroundColor Cyan
Write-Host " Khoảng thời gian lấy mẫu: $IntervalSeconds s | Tổng thời gian: $DurationMinutes phút" -ForegroundColor Cyan
Write-Host " Log CSV: $logFile" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$startTime = Get-Date
$endTime = $startTime.AddMinutes($DurationMinutes)
$elapsedSec = 0

# Lấy PID của App
$pidApp = (adb shell "pidof $PackageName").Trim()
if (-not $pidApp) {
    Write-Host "[WARNING] Ứng dụng $PackageName chưa chạy trên thiết bị!" -ForegroundColor Yellow
    Write-Host "Đang khởi động ứng dụng..." -ForegroundColor Yellow
    adb shell "monkey -p $PackageName -c android.intent.category.LAUNCHER 1" | Out-Null
    Start-Sleep -Seconds 3
    $pidApp = (adb shell "pidof $PackageName").Trim()
}

Write-Host "[INFO] Process PID: $pidApp" -ForegroundColor Green

while ((Get-Date) -lt $endTime) {
    $now = Get-Date
    $timeStr = $now.Format("HH:mm:ss")
    $elapsedSec = [int]($now - $startTime).TotalSeconds

    # 1. Đo CPU của App bằng top
    $topOutput = adb shell "top -b -n 1 | grep $PackageName"
    $cpuApp = "0.0"
    if ($topOutput) {
        $parts = -split $topOutput.Trim()
        # Cột thứ 9 trong output 'top' chuẩn là CPU%
        if ($parts.Length -ge 9) {
            $cpuApp = $parts[8]
        }
    }

    # 2. Đo chi tiết RAM PSS qua dumpsys meminfo
    $memDump = adb shell "dumpsys meminfo $PackageName"
    $totalPssMb = "0.0"
    $javaHeapMb = "0.0"
    $nativeHeapMb = "0.0"
    $gfxMb = "0.0"

    if ($memDump) {
        foreach ($line in ($memDump -split "`n")) {
            if ($line -match "TOTAL PSS:\s+(\d+)") {
                $totalPssMb = [math]::Round([float]$matches[1] / 1024.0, 1)
            }
            if ($line -match "Java Heap:\s+(\d+)") {
                $javaHeapMb = [math]::Round([float]$matches[1] / 1024.0, 1)
            }
            if ($line -match "Native Heap:\s+(\d+)") {
                $nativeHeapMb = [math]::Round([float]$matches[1] / 1024.0, 1)
            }
            if ($line -match "Graphics:\s+(\d+)") {
                $gfxMb = [math]::Round([float]$matches[1] / 1024.0, 1)
            }
        }
    }

    # 3. Đo nhiệt độ Pin qua dumpsys battery
    $batteryTemp = "N/A"
    $batteryDump = adb shell "dumpsys battery"
    if ($batteryDump -match "temperature:\s*(\d+)") {
        $batteryTemp = [math]::Round([float]$matches[1] / 10.0, 1)
    }

    # 4. Đo Thermal Status của OS qua dumpsys thermalservice
    $thermalStatus = "NONE(0)"
    $thermalDump = adb shell "dumpsys thermalservice"
    if ($thermalDump -match "Thermal Status:\s*([^\r\n]+)") {
        $thermalStatus = $matches[1].Trim()
    }

    # 5. Đếm số lượng Thread
    $threadCount = "N/A"
    if ($pidApp) {
        $tCount = adb shell "ls /proc/$pidApp/task 2>/dev/null | wc -l"
        if ($tCount) {
            $threadCount = $tCount.Trim()
        }
    }

    # Ghi vào CSV
    $csvRow = "$timeStr,$elapsedSec,$cpuApp,$totalPssMb,$javaHeapMb,$nativeHeapMb,$gfxMb,$batteryTemp,$thermalStatus,$threadCount"
    Add-Content -Path $logFile -Value $csvRow

    # In ra console dạng bảng dễ đọc
    Write-Host "[$timeStr] +${elapsedSec}s | CPU: $($cpuApp.PadLeft(5))% | RAM PSS: $($totalPssMb.ToString().PadLeft(5)) MB (Java:$javaHeapMb MB, Native:$nativeHeapMb MB) | Temp: $($batteryTemp)°C | Thermal: $thermalStatus | Threads: $threadCount"

    Start-Sleep -Seconds $IntervalSeconds
}

Write-Host "==========================================================" -ForegroundColor Green
Write-Host " HOÀN TẤT ĐO HIỆU NĂNG: $TestName" -ForegroundColor Green
Write-Host " File CSV kết quả: $logFile" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
