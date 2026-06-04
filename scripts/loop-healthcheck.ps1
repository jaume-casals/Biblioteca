# Hourly health check for /loop QAHEALTH. Appends findings to todo.txt.
$ErrorActionPreference = 'Continue'
$base = Split-Path $PSScriptRoot -Parent
$stamp = Get-Date -Format 'yyyy-MM-dd HH:mm'
$mv = Join-Path $base '.loop-lib\apache-maven-3.9.9\bin\mvn.cmd'
if (-not $env:JAVA_HOME) {
  $env:JAVA_HOME = (java -XshowSettings:properties -version 2>&1 | Select-String 'java.home').ToString().Split('=')[1].Trim()
}
$lines = @("", "# Loop health check $stamp (scripts/loop-healthcheck.ps1)")

if (-not (Test-Path $mv)) {
  $lines += '[infra] Maven not in .loop-lib - run initial loop setup or install mvn.'
}
elseif (-not (Test-Path (Join-Path $base 'test'))) {
  $lines += '[infra] test/ still missing - mvn test will run 0 tests.'
}
else {
  $testOut = & $mv -f (Join-Path $base 'pom.xml') test 2>&1 | Out-String
  if ($testOut -match 'BUILD FAILURE|Failures: [1-9]|Errors: [1-9]') {
    $lines += "[test] mvn test FAILED at $stamp - see console log."
  }
  elseif ($testOut -match 'Tests run: 0') {
    $lines += "[test] mvn test: 0 tests run at $stamp."
  }
  else {
    $lines += "[test] mvn test OK at $stamp."
  }
}

$todo = Join-Path $base 'todo.txt'
Add-Content -Path $todo -Value ($lines -join "`n") -Encoding UTF8
Write-Host ($lines -join "`n")
