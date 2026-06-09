# Compile, run unit/integration tests, and optionally UIAudit.
# Equivalent to ./checkBiblio/run.sh on Linux/macOS.
# Usage:
#   .\checkBiblio\run.ps1
#   .\checkBiblio\run.ps1 -TestOnly
#   .\checkBiblio\run.ps1 -AuditOnly
#   .\checkBiblio\run.ps1 -Auto

[CmdletBinding()]
param(
    [switch]$TestOnly,
    [switch]$AuditOnly,
    [switch]$Auto,
    [Parameter(ValueFromRemainingArguments=$true)]
    [string[]]$AuditArgs
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

    if ($AuditArgs -contains "-h" -or $AuditArgs -contains "--help") {
        Write-Host "Usage: .\checkBiblio\run.ps1 [-TestOnly | -AuditOnly] [-Auto] [audit args...]"
        exit 0
    }

    # ── Tests ──────────────────────────────────────────────────────────────
    if (-not $AuditOnly) {
        Write-Host ""
        Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "  Running scripts\test.bat (BibliotecaTest + JUnit 5)"
        Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
        & scripts\test.bat
        if ($LASTEXITCODE -ne 0) {
            $stepErrors += "scripts\test.bat failed (exit $LASTEXITCODE)"
            $finalExit = 1
        }
    }

    # ── UIAudit ────────────────────────────────────────────────────────────
    if (-not $TestOnly) {
        $cp = "bin;lib\h2-2.3.232.jar;lib\mariadb-java-client-3.3.3.jar;lib\gson-2.11.0.jar"

        Write-Host ""
        Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "  Compiling UIAudit"
        Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
        javac -Xlint:deprecation -cp $cp checkBiblio\UiTestSupport.java checkBiblio\UIAudit.java checkBiblio\I18nAudit.java -d bin
        if ($LASTEXITCODE -ne 0) {
            $stepErrors += "UIAudit compile failed (javac)"
            $finalExit = 1
        } else {
            Remove-Item checkBiblio\audit_report.txt -ErrorAction SilentlyContinue

            Write-Host ""
            Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
            Write-Host "  Starting UIAudit"
            Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
            $argsList = @()
            if ($Auto) { $argsList += "--auto" }
            $argsList += $AuditArgs
            & java -Xmx512m -cp $cp checkBiblio.UIAudit @argsList
            if ($LASTEXITCODE -ne 0) {
                $stepErrors += "UIAudit exited with code $LASTEXITCODE"
                $finalExit = 1
            }
        }
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

    if (Test-Path checkBiblio\audit_report.txt) {
        $auditTotals = Select-String -Path checkBiblio\audit_report.txt -Pattern '\] FAIL: \d+  WARN: \d+|^\[AUTO\] Audit complete' |
            Select-Object -Last 2
        if ($auditTotals) {
            Write-Host ""
            Write-Host "  UIAudit totals:"
            foreach ($t in $auditTotals) { Write-Host "    $($t.Line)" }
        }
        $auditIssues = Select-String -Path checkBiblio\audit_report.txt -Pattern '\] (FAIL|WARN|ERROR):|FATAL:|APP LAUNCH ERROR' |
            Where-Object { $_.Line -notmatch '\] FAIL: \d+  WARN: \d+$' }
        if ($auditIssues) {
            Write-Host ""
            Write-Host "  UIAudit issues (FAIL / WARN / ERROR):"
            foreach ($i in $auditIssues) { Write-Host "    $($i.Line)" }
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
