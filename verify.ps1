$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$jdk = if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME/bin/javac.exe")) { "$env:JAVA_HOME/bin" } else {
    Get-ChildItem "$env:ProgramFiles/Java" -Directory -Filter 'jdk*' -ErrorAction SilentlyContinue |
        Where-Object { Test-Path (Join-Path $_.FullName 'bin/javac.exe') } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1 |
        ForEach-Object { Join-Path $_.FullName 'bin' }
}
$compiler = if ($jdk) { "$jdk/javac.exe" } else { 'javac' }
$runtime = if ($jdk) { "$jdk/java.exe" } else { 'java' }
$sources = @(Get-ChildItem src/main/java,src/test/java -Recurse -Filter *.java | ForEach-Object FullName)
& $compiler --release 17 -encoding UTF-8 -cp 'lib/*' -d target/review-classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
$sandbox = Join-Path $PSScriptRoot ('target/check-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $sandbox | Out-Null
New-Item -ItemType File -Path (Join-Path $sandbox '.isolated-test') | Out-Null
Push-Location $sandbox
try {
    & $runtime '-Djava.awt.headless=true' -cp "$PSScriptRoot/target/review-classes;$PSScriptRoot/lib/*" vn.edu.ptit.cinema.RegressionChecks
    if ($LASTEXITCODE -ne 0) { throw 'Regression checks failed' }
} finally { Pop-Location }
