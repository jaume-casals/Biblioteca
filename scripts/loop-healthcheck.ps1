# Hourly health check for /loop QAHEALTH. Appends findings to todo.txt.
$ErrorActionPreference = 'Continue'
$base = Split-Path $PSScriptRoot -Parent
$stamp = Get-Date -Format 'yyyy-MM-dd HH:mm'
$mv = Join-Path $base '.loop-lib\apache-maven-3.9.9\bin\mvn.cmd'
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = (java -XshowSettings:properties -version 2>&1 | Select-String 'java.home').ToString().Split('=')[1].Trim() }
$lines = @("", "# ── Loop health check $stamp (scripts/loop-healthcheck.ps1) ──")

if (-not (Test-Path $mv)) { $lines += '[infra] Maven not in .loop-lib — run initial loop setup or install mvn.' }
elseif (-not (Test-Path (Join-Path $base 'test'))) { $lines += '[infra] test/ still missing — mvn test will run 0 tests.' }
else {
  $testOut = & $mv -f (Join-Path $base 'pom.xml') test 2>&1 | Out-String
  if ($testOut -match 'BUILD FAILURE|Failures: [1-9]|Errors: [1-9]') { $lines += "[test] mvn test FAILED at $stamp — see console log." }
  elseif ($testOut -match 'Tests run: 0') { $lines += "[test] mvn test: 0 tests run at $stamp." }
  else { $lines += "[test] mvn test OK at $stamp." }
}

$cpFile = Join-Path $base '.loop-lib\classpath.txt'
if ((Test-Path $mv) -and -not (Test-Path $cpFile)) {
  & $mv -f (Join-Path $base 'pom.xml') -q dependency:build-classpath "-Dmdep.outputFile=$cpFile" "-Dmdep.pathSeparator=;" | Out-Null
  & $mv -f (Join-Path $base 'pom.xml') -q compile | Out-Null
}
if (Test-Path $cpFile) {
  $cp = "$base\target\classes;" + (Get-Content $cpFile -Raw).Trim()
  $h2 = 'jdbc:h2:mem:loopcheck;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1'
  $errLog = Join-Path $base '.loop-lib\loop-web-err.txt'
  Remove-Item $errLog -ErrorAction SilentlyContinue
  $p = Start-Process java -ArgumentList @('-Dbiblioteca.test=true',"-Dbiblioteca.h2.url=$h2",'-cp',$cp,'main.WebLauncher','--web') -PassThru -RedirectStandardError $errLog -NoNewWindow
  Start-Sleep -Seconds 6
  if (-not $p.HasExited) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue }
  $err = (Get-Content $errLog -ErrorAction SilentlyContinue) -join ' '
  if ($err -match 'Error inicialitzant|No s''ha pogut connectar') { $lines += '[bug] WebLauncher still fails DB init at ' + $stamp }
}

$todo = Join-Path $base 'todo.txt'
Add-Content -Path $todo -Value ($lines -join "`n") -Encoding UTF8
Write-Host ($lines -join "`n")
