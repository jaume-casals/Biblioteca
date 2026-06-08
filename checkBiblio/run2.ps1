# Compile and run StressTest from the project root.
# Equivalent to ./checkBiblio/run2.sh on Linux/macOS.
# Usage:
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
        Write-Error "java not found in PATH"; exit 1
    }
    foreach ($jar in @("lib\h2-2.3.232.jar", "lib\mariadb-java-client-3.3.3.jar", "lib\gson-2.11.0.jar")) {
        if (-not (Test-Path $jar)) {
            Write-Error "missing $jar"; exit 1
        }
    }

    if ($StressArgs -contains "-h" -or $StressArgs -contains "--help") {
        Write-Host "Usage: .\checkBiblio\run2.ps1 [StressTest args...]"
        Write-Host "  STRESS_EXTREME=1     extreme mode (default threads 100)"
        Write-Host "  STRESS_TIMEOUT=N     watchdog seconds (default 600)"
        Write-Host "  STRESS_THREADS=N     worker threads (default 50, 100 if extreme)"
        exit 0
    }

    # ── Configuration ──────────────────────────────────────────────────────
    if ($env:STRESS_EXTREME) {
        $timeout = if ($env:STRESS_TIMEOUT) { [int]$env:STRESS_TIMEOUT } else { 600 }
        $threads = if ($env:STRESS_THREADS) { [int]$env:STRESS_THREADS } else { 100 }
        $javaOpts = @("-Dbiblioteca.stress.extreme=true", "-Dbiblioteca.stress.threads=$threads")
        Write-Host "STRESS_EXTREME=1 (timeout=${timeout}s, threads=$threads)" -ForegroundColor Yellow
    } else {
        $timeout = if ($env:STRESS_TIMEOUT) { [int]$env:STRESS_TIMEOUT } else { 600 }
        $threads = if ($env:STRESS_THREADS) { [int]$env:STRESS_THREADS } else { 50 }
        $javaOpts = @("-Dbiblioteca.stress.threads=$threads")
    }

    $cp = "bin;lib\h2-2.3.232.jar;lib\mariadb-java-client-3.3.3.jar;lib\gson-2.11.0.jar"

    # ── Compile main app ──────────────────────────────────────────────────
    Write-Host ""
    Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  Compiling main app"
    Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
    & scripts\compile.bat
    if ($LASTEXITCODE -ne 0) {
        $stepErrors += "compile failed"
        $finalExit = 1
    }

    # ── Compile StressTest ─────────────────────────────────────────────────
    if ($stepErrors.Count -eq 0) {
        Write-Host ""
        Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "  Compiling StressTest"
        Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
        javac -Xlint:deprecation -cp $cp checkBiblio\StressTest.java -d bin
        if ($LASTEXITCODE -ne 0) {
            $stepErrors += "StressTest compile failed (javac)"
            $finalExit = 1
        }
    }

    # ── Run StressTest with timeout ────────────────────────────────────────
    if ($stepErrors.Count -eq 0) {
        Remove-Item checkBiblio\stress_report.txt -ErrorAction SilentlyContinue
        Get-ChildItem checkBiblio\screenshots\stress_*.png -ErrorAction SilentlyContinue | Remove-Item

        Write-Host ""
        Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "  Starting StressTest (timeout: ${timeout}s)"
        Write-Host "══════════════════════════════════════" -ForegroundColor Cyan

        $job = Start-Job -ScriptBlock {
            param($cp, $opts, $args)
            java -Xmx512m @opts -cp $cp checkBiblio.StressTest @args
        } -ArgumentList $cp, $javaOpts, $StressArgs

        $completed = Wait-Job $job -Timeout $timeout

        if ($completed) {
            Receive-Job $job
            if ($job.State -ne "Completed") {
                $stepErrors += "StressTest state: $($job.State)"
                $finalExit = 1
            }
        } else {
            Stop-Job $job
            $stepErrors += "StressTest did not finish (timeout ${timeout}s or crash)"
            $finalExit = 1
        }
        Remove-Job $job -Force
    }

    # ── Final summary ─────────────────────────────────────────────────────
    Write-Host ""
    Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  FINAL SUMMARY"
    Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan

    if ($stepErrors.Count -eq 0) {
        Write-Host "  Steps: no failures recorded." -ForegroundColor Green
    } else {
        Write-Host "  Step failures ($($stepErrors.Count)):" -ForegroundColor Red
        foreach ($err in $stepErrors) {
            Write-Host "    ✗ $err" -ForegroundColor Red
        }
    }

    if (Test-Path checkBiblio\stress_report.txt) {
        $stressTotals = Select-String -Path checkBiblio\stress_report.txt -Pattern 'SUMMARY|PASS :|FAIL :|WARN :|TOTAL:' |
            Select-Object -Last 6
        if ($stressTotals) {
            Write-Host ""
            Write-Host "  StressTest totals:"
            foreach ($t in $stressTotals) { Write-Host "    $($t.Line)" }
        }
        $failLine = Select-String -Path checkBiblio\stress_report.txt -Pattern 'FAIL :' | Select-Object -Last 1
        if ($failLine) {
            $failN = ([regex]::Replace($failLine.Line, '.*FAIL : ', '') -replace '[^\d]', '')
            if ($failN -and [int]$failN -gt 0) {
                $finalExit = 1
            }
        }
        $stressIssues = Select-String -Path checkBiblio\stress_report.txt -Pattern '✗ FAIL:|! WARN:|SCREENSHOT FAILED|FATAL:|APP LAUNCH ERROR'
        if ($stressIssues) {
            Write-Host ""
            Write-Host "  StressTest issues (FAIL / WARN / FATAL):"
            foreach ($i in $stressIssues) { Write-Host "    $($i.Line)" }
        }
    }

    Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
    if ($finalExit -eq 0) {
        Write-Host "  Result: PASS" -ForegroundColor Green
    } else {
        Write-Host "  Result: FAIL (see above)" -ForegroundColor Red
    }
    Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan

    exit $finalExit
} finally {
    Pop-Location
}
