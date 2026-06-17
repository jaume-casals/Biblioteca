# Compila i executa StressTest des de l'arrel del projecte.
# Equivalent a ./checkBiblio/run2.sh a Linux/macOS.
# Ús:
#   .\checkBiblio\run2.ps1
#   $env:STRESS_EXTREME=1; .\checkBiblio\run2.ps1
#   $env:STRESS_TIMEOUT=900; $env:STRESS_THREADS=80; .\checkBiblio\run2.ps1

[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments=$true)]
    [string[]]$StressArgs
)

$ProjectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $ProjectRoot
try {
    $stepErrors = @()
    $finalExit = 0

    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        Write-Error "java no es troba al PATH"; exit 1
    }
    foreach ($jar in @("lib\h2-2.3.232.jar", "lib\mariadb-java-client-3.3.3.jar", "lib\gson-2.11.0.jar")) {
        if (-not (Test-Path $jar)) {
            Write-Error "manca $jar"; exit 1
        }
    }

    if ($StressArgs -contains "-h" -or $StressArgs -contains "--help") {
        Write-Host "Ús: .\checkBiblio\run2.ps1 [StressTest args...]"
        Write-Host "  STRESS_EXTREME=1     mode extrem (per defecte threads 100, timeout 1800s)"
        Write-Host "  STRESS_TIMEOUT=N     segons del watchdog (per defecte 600, 1800 en extrem)"
        Write-Host "  STRESS_THREADS=N     fils de treball (per defecte 50, 100 en extrem)"
        Write-Host "  STRESS_INSTANCES=N   en extrem: llança N JVMs fills (per defecte 3)"
        Write-Host "  STRESS_SOAK=N        en extrem: activitat de BD en segon pla durant N segons (0=off)"
        Write-Host "  STRESS_FUZZ=N        en extrem: cadenes aleatòries per diàleg a la fase de fuzz (per defecte 25)"
        Write-Host "  STRESS_MEMPROBE=0|1  en extrem: sondatge del creixement del heap (per defecte 1)"
        exit 0
    }

    # ── Configuració ──────────────────────────────────────────────────────
    if ($env:STRESS_EXTREME) {
        $timeout = if ($env:STRESS_TIMEOUT) { [int]$env:STRESS_TIMEOUT } else { 1800 }
        $threads = if ($env:STRESS_THREADS) { [int]$env:STRESS_THREADS } else { 100 }
        $instances = if ($env:STRESS_INSTANCES) { [int]$env:STRESS_INSTANCES } else { 3 }
        $soak     = if ($env:STRESS_SOAK)      { [int]$env:STRESS_SOAK      } else { 0 }
        $fuzz     = if ($env:STRESS_FUZZ)      { [int]$env:STRESS_FUZZ      } else { 25 }
        $memprobe = if ($env:STRESS_MEMPROBE)  { [int]$env:STRESS_MEMPROBE  } else { 1 }
        $javaOpts = @("-Dbiblioteca.stress.extreme=true", "-Dbiblioteca.stress.threads=$threads")
        if ($instances -gt 0) { $javaOpts += "-Dbiblioteca.stress.instances=$instances" }
        if ($soak -gt 0)      { $javaOpts += "-Dbiblioteca.stress.soak=$soak" }
        $javaOpts += "-Dbiblioteca.stress.fuzz=$fuzz"
        if ($memprobe -eq 1) { $javaOpts += "-Dbiblioteca.stress.memprobe=true" }
        Write-Host "STRESS_EXTREME=1 (timeout=${timeout}s, threads=$threads)" -ForegroundColor Yellow
        Write-Host "                  instances=$instances, soak=${soak}s, fuzz=$fuzz)" -ForegroundColor Yellow
    } else {
        $timeout = if ($env:STRESS_TIMEOUT) { [int]$env:STRESS_TIMEOUT } else { 600 }
        $threads = if ($env:STRESS_THREADS) { [int]$env:STRESS_THREADS } else { 50 }
        $javaOpts = @("-Dbiblioteca.stress.threads=$threads")
    }

    $cp = "bin;lib\h2-2.3.232.jar;lib\mariadb-java-client-3.3.3.jar;lib\gson-2.11.0.jar"

    # ── Compila l'aplicació principal ─────────────────────────────────────
    Write-Host ""
    Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  Compilant l'aplicació principal"
    Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
    & scripts\compile.bat
    if ($LASTEXITCODE -ne 0) {
        $stepErrors += "la compilació ha fallat"
        $finalExit = 1
    }

    # ── Compila StressTest ─────────────────────────────────────────────────
    if ($stepErrors.Count -eq 0) {
        Write-Host ""
        Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "  Compilant StressTest"
        Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
        javac -Xlint:deprecation -cp $cp checkBiblio\UiTestSupport.java checkBiblio\StressTest.java -d bin
        if ($LASTEXITCODE -ne 0) {
            $stepErrors += "La compilació de StressTest ha fallat (javac)"
            $finalExit = 1
        }
    }

    # ── Executa StressTest amb timeout ────────────────────────────────────
    if ($stepErrors.Count -eq 0) {
        Remove-Item checkBiblio\stress_report.txt -ErrorAction SilentlyContinue

        Write-Host ""
        Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "  Iniciant StressTest (timeout: ${timeout}s)"
        Write-Host "══════════════════════════════════════" -ForegroundColor Cyan

        $job = Start-Job -ScriptBlock {
            param($cp, $opts, $args)
            Set-Location $using:ProjectRoot
            java -Xmx512m @opts -cp $cp checkBiblio.StressTest @args
        } -ArgumentList $cp, $javaOpts, $StressArgs

        $completed = Wait-Job $job -Timeout $timeout

        if ($completed) {
            Receive-Job $job
            if ($job.State -ne "Completed") {
                $stepErrors += "Estat de StressTest: $($job.State)"
                $finalExit = 1
            }
        } else {
            Stop-Job $job
            $stepErrors += "StressTest no ha acabat (timeout ${timeout}s o fallada)"
            $finalExit = 1
        }
        Remove-Job $job -Force
    }

    # ── Resum final ───────────────────────────────────────────────────────
    Write-Host ""
    Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  RESUM FINAL"
    Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan

    if ($stepErrors.Count -eq 0) {
        Write-Host "  Passos: no s'han registrat fallades." -ForegroundColor Green
    } else {
        Write-Host "  Fallades de pas ($($stepErrors.Count)):" -ForegroundColor Red
        foreach ($err in $stepErrors) {
            Write-Host "    ✗ $err" -ForegroundColor Red
        }
    }

    if (Test-Path checkBiblio\stress_report.txt) {
        $stressTotals = Select-String -Path checkBiblio\stress_report.txt -Pattern 'SUMMARY|PASS :|FAIL :|WARN :|TOTAL:' |
            Select-Object -Last 6
        if ($stressTotals) {
            Write-Host ""
            Write-Host "  Totals de StressTest:"
            foreach ($t in $stressTotals) { Write-Host "    $($t.Line)" }
        }
        $failLine = Select-String -Path checkBiblio\stress_report.txt -Pattern 'FAIL :' | Select-Object -Last 1
        if ($failLine) {
            $failN = ([regex]::Replace($failLine.Line, '.*FAIL : ', '') -replace '[^\d]', '')
            if ($failN -and [int]$failN -gt 0) {
                $finalExit = 1
            }
        }
        $stressIssues = Select-String -Path checkBiblio\stress_report.txt -Pattern '✗ FAIL:|! WARN:|FATAL:|APP LAUNCH ERROR'
        if ($stressIssues) {
            Write-Host ""
            Write-Host "  Problemes de StressTest (FAIL / WARN / FATAL):"
            foreach ($i in $stressIssues) { Write-Host "    $($i.Line)" }
        }
    }

    Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
    if ($finalExit -eq 0) {
        Write-Host "  Resultat: PASS" -ForegroundColor Green
    } else {
        Write-Host "  Resultat: FAIL (mira amunt)" -ForegroundColor Red
    }
    Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan

    exit $finalExit
} finally {
    Pop-Location
}
