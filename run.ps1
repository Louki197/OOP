Set-Location -LiteralPath $PSScriptRoot
$jdkBin = if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME/bin/javac.exe")) {
    "$env:JAVA_HOME/bin"
} else {
    Get-ChildItem "$env:ProgramFiles/Java" -Directory -Filter 'jdk*' -ErrorAction SilentlyContinue |
        Where-Object { Test-Path (Join-Path $_.FullName 'bin/javac.exe') } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1 |
        ForEach-Object { Join-Path $_.FullName 'bin' }
}
$compiler = if ($jdkBin) { Join-Path $jdkBin 'javac.exe' } else { 'javac' }
$runtime = if ($jdkBin) { Join-Path $jdkBin 'java.exe' } else { 'java' }
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "   CINEMA MANAGEMENT SYSTEM - BTL OOP PTIT" -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Cyan

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

$sources = (Get-ChildItem -Path "src/main/java" -Recurse -Filter *.java | ForEach-Object { $_.FullName })
Write-Host "[1/2] Đang biên dịch mã nguồn Java..." -ForegroundColor Green
& $compiler --release 17 -cp "lib\gson-2.10.1.jar;lib\flatlaf-3.4.1.jar" -encoding UTF-8 -d bin $sources

if ($LASTEXITCODE -ne 0) {
    Write-Host "[LỖI] Biên dịch thất bại!" -ForegroundColor Red
    pause
    exit 1
}

Write-Host "[2/2] Đang khởi chạy hệ thống rạp..." -ForegroundColor Green
Write-Host "--------------------------------------------------------"
& $runtime -cp "bin;lib\gson-2.10.1.jar;lib\flatlaf-3.4.1.jar" vn.edu.ptit.cinema.App
Write-Host "--------------------------------------------------------"
Write-Host "[HOÀN THÀNH] Ứng dụng đã kết thúc." -ForegroundColor Cyan
