Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "   CINEMA MANAGEMENT SYSTEM - BTL OOP PTIT" -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Cyan

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

$sources = (Get-ChildItem -Path "src" -Recurse -Filter *.java | ForEach-Object { $_.FullName })
Write-Host "[1/2] Đang biên dịch mã nguồn Java..." -ForegroundColor Green
& javac -cp "lib\gson-2.10.1.jar" -encoding UTF-8 -d bin $sources

if ($LASTEXITCODE -ne 0) {
    Write-Host "[LỖI] Biên dịch thất bại!" -ForegroundColor Red
    pause
    exit 1
}

Write-Host "[2/2] Đang khởi chạy hệ thống rạp..." -ForegroundColor Green
Write-Host "--------------------------------------------------------"
& java -cp "bin;lib\gson-2.10.1.jar" vn.edu.ptit.cinema.App
Write-Host "--------------------------------------------------------"
Write-Host "[HOÀN THÀNH] Ứng dụng đã kết thúc." -ForegroundColor Cyan
