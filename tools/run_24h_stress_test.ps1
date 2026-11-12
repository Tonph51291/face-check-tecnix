# run_24h_stress_test.ps1 - Hệ thống giám sát kiểm thử độ bền 24 giờ cho FaceCheckTecnix
# Mục tiêu: Phát hiện rò rỉ bộ nhớ chậm (slow leak), đầy File Descriptors, nghẽn CameraX, hoặc Crash/ANR xuyên đêm.
# Cách chạy: powershell -ExecutionPolicy Bypass -File .\tools\run_24h_stress_test.ps1 -Hours 24

param(
    [string]$PackageName = "com.example.facechecktecnix",
    [int]$Hours = 24,
    [int]$IntervalSeconds = 60, # Lấy mẫu mỗi 1 phút (1.440 mẫu trong 24h)
    [string]$OutputDir = ".\profiling_results\24h_test"
)

# 1. Kiểm tra kết nối ADB
$devices = @(adb devices | Where-Object { $_ -match "\tdevice$" })
if ($devices.Count -eq 0) {
    Write-Host "[LỖI] Không tìm thấy máy ảo hoặc thiết bị nào kết nối qua ADB!" -ForegroundColor Red
    exit 1
}

$activeDevice = ($devices[0] -split "\t")[0].Trim()
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " [FaceCheckTecnix] HỆ THỐNG KIỂM THỬ ĐỘ BỀN LIÊN TỤC 24 GIỜ" -ForegroundColor Cyan
Write-Host " Thiết bị/Máy ảo: $activeDevice" -ForegroundColor Green
Write-Host " Thời gian chạy: $Hours giờ ($($Hours * 3600) giây)" -ForegroundColor Yellow
Write-Host " Chu kỳ lấy mẫu: Mỗi $IntervalSeconds giây / lần" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

# 2. Tạo thư mục output
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$startTime = Get-Date
$endTime = $startTime.AddHours($Hours)
$timestampStr = $startTime.ToString("yyyyMMdd_HHmmss")
$csvFile = Join-Path $OutputDir "stress_24h_${timestampStr}.csv"
$errorLogFile = Join-Path $OutputDir "stress_24h_errors_${timestampStr}.log"

# Tiêu đề file CSV
$csvHeader = "Timestamp,ElapsedMinutes,PID,CPU_Percent,RAM_PSS_MB,RAM_JavaHeap_MB,RAM_NativeHeap_MB,RAM_Gfx_MB,ThreadCount,FD_Count,Status"
Set-Content -Path $csvFile -Value $csvHeader

# Cấu hình máy ảo không sleep màn hình
adb -s $activeDevice shell "svc power stayon true" 2>$null

# Lấy PID ứng dụng
function Get-AppPid {
    $raw = adb -s $activeDevice shell "pidof $PackageName"
    if ($raw) {
        return ($raw.ToString().Trim() -split "\s+")[0]
    }
    return $null
}

$pidApp = Get-AppPid
if (-not $pidApp) {
    Write-Host "[INFO] Đang khởi động ứng dụng $PackageName..." -ForegroundColor Gray
    adb -s $activeDevice shell "monkey -p $PackageName -c android.intent.category.LAUNCHER 1" | Out-Null
    Start-Sleep -Seconds 4
    $pidApp = Get-AppPid
}

Write-Host "[OK] Ứng dụng đang chạy với PID: $pidApp" -ForegroundColor Green
Write-Host "[INFO] Bắt đầu theo dõi dài hạn. File CSV: $csvFile" -ForegroundColor Cyan
Write-Host "--------------------------------------------------------------------------------------------------" -ForegroundColor DarkGray

$sampleCount = 0
$restartCount = 0

while ((Get-Date) -lt $endTime) {
    $now = Get-Date
    $timeStr = $now.ToString("yyyy-MM-dd HH:mm:ss")
    $elapsedMin = [math]::Round(($now - $startTime).TotalMinutes, 1)

    $currentPid = Get-AppPid
    $status = "OK"

    # Kiểm tra nếu App bị Crash / Khai tử
    if (-not $currentPid) {
        $status = "CRASH_OR_KILLED"
        $restartCount++
        $crashTime = $now.ToString("HH:mm:ss")
        $msg = "[$timeStr] CẢNH BÁO: App $PackageName (PID cũ: $pidApp) đã bị tắt hoặc crash tại phút thứ $elapsedMin!"
        Write-Host $msg -ForegroundColor Red
        Add-Content -Path $errorLogFile -Value $msg

        # Dump logcat crash
        $crashDump = adb -s $activeDevice logcat -d -s "AndroidRuntime:E" "DEBUG:*"
        Add-Content -Path $errorLogFile -Value ($crashDump | Out-String)
        adb -s $activeDevice shell screencap -p "/sdcard/crash_${elapsedMin}m.png"
        adb -s $activeDevice pull "/sdcard/crash_${elapsedMin}m.png" "$OutputDir\" 2>$null

        # Tự động khởi động lại App để tiếp tục test
        Write-Host "[$crashTime] Watchdog tự động khởi động lại ứng dụng..." -ForegroundColor Yellow
        adb -s $activeDevice shell "monkey -p $PackageName -c android.intent.category.LAUNCHER 1" | Out-Null
        Start-Sleep -Seconds 5
        $currentPid = Get-AppPid
        $pidApp = $currentPid
    }

    # 1. Đo CPU
    $cpuUsage = "0.0"
    if ($currentPid) {
        $topRaw = (adb -s $activeDevice shell "top -b -n 1 -p $currentPid") | Out-String
        if ($topRaw) {
            foreach ($line in ($topRaw -split "`n")) {
                $trimmed = $line.Trim()
                if ($trimmed -match "^$currentPid\s+") {
                    $cols = -split $trimmed
                    if ($cols.Length -ge 9) {
                        $cpuUsage = $cols[8]
                    }
                }
            }
        }
    }

    # 2. Đo RAM PSS qua meminfo
    $pssMb = 0.0
    $javaMb = 0.0
    $nativeMb = 0.0
    $gfxMb = 0.0
    if ($currentPid) {
        $memRaw = (adb -s $activeDevice shell "dumpsys meminfo $currentPid") | Out-String
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
    }

    # 3. Đếm Thread
    $threadCount = "N/A"
    if ($currentPid) {
        $tOut = adb -s $activeDevice shell "ls /proc/$currentPid/task 2>/dev/null | wc -l"
        if ($tOut) {
            $threadCount = $tOut.Trim()
        }
    }

    # 4. Đếm File Descriptors (FD) - cảnh báo rò rỉ socket/file
    $fdCount = "N/A"
    if ($currentPid) {
        $fdOut = adb -s $activeDevice shell "run-as $PackageName ls /proc/$currentPid/fd 2>/dev/null | wc -l"
        if ($fdOut) {
            $fdCount = $fdOut.Trim()
        }
    }

    # Ghi log CSV
    $csvRow = "$timeStr,$elapsedMin,$currentPid,$cpuUsage,$pssMb,$javaMb,$nativeMb,$gfxMb,$threadCount,$fdCount,$status"
    Add-Content -Path $csvFile -Value $csvRow

    # In ra console
    $sampleCount++
    $color = if ($status -eq "OK") { "Green" } else { "Red" }
    Write-Host "[$timeStr] +$($elapsedMin.ToString().PadLeft(6)) min | CPU: $($cpuUsage.PadLeft(5))% | PSS: $($pssMb.ToString().PadLeft(5)) MB (Java: $javaMb MB, Native: $nativeMb MB) | Threads: $threadCount | FD: $fdCount | Status: $status" -ForegroundColor $color

    Start-Sleep -Seconds $IntervalSeconds
}

Write-Host "==========================================================" -ForegroundColor Green
Write-Host " HOÀN TẤT BÀI KIỂM THỬ 24 GIỜ!" -ForegroundColor Green
Write-Host " Tổng thời gian chạy: $([math]::Round(($endTime - $startTime).TotalHours, 2)) giờ" -ForegroundColor Green
Write-Host " Số lần restart do crash: $restartCount lần" -ForegroundColor $(if ($restartCount -eq 0) { "Green" } else { "Red" })
Write-Host " File CSV kết quả: $csvFile" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
