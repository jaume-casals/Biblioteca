# Compila, executa els tests unitaris/d'integració i, opcionalment, UIAudit.
# Equivalent a ./checkBiblio/run.sh a Linux/macOS.
# Ús:
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
        Write-Error "java no es troba al PATH"; exit 1
    }
    foreach ($jar in @("lib\h2-2.3.232.jar", "lib\mariadb-java-client-3.3.3.jar", "lib\gson-2.11.0.jar")) {
        if (-not (Test-Path $jar)) {
            Write-Error "manca $jar"; exit 1
        }
    }

    if ($AuditArgs -contains "-h" -or $AuditArgs -contains "--help") {
        Write-Host "Ús: .\checkBiblio\run.ps1 [-TestOnly | -AuditOnly] [-Auto] [audit args...]"
        exit 0
    }

    # ── Tests ──────────────────────────────────────────────────────────────
    if (-not $AuditOnly) {
        Write-Host ""
        Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "  Executant scripts\test.bat (BibliotecaTest + JUnit 5)"
        Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
        & scripts\test.bat
        if ($LASTEXITCODE -ne 0) {
            $stepErrors += "scripts\test.bat ha fallat (sortida $LASTEXITCODE)"
            $finalExit = 1
        }
    }

    # ── UIAudit ────────────────────────────────────────────────────────────
    if (-not $TestOnly) {
        $cp = "bin;lib\h2-2.3.232.jar;lib\mariadb-java-client-3.3.3.jar;lib\gson-2.11.0.jar"

        Write-Host ""
        Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "  Compilant UIAudit"
        Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
        javac -Xlint:deprecation -cp $cp checkBiblio\UiTestSupport.java checkBiblio\UIAudit.java checkBiblio\I18nAudit.java -d bin
        if ($LASTEXITCODE -ne 0) {
            $stepErrors += "La compilació d'UIAudit ha fallat (javac)"
            $finalExit = 1
        } else {
            Remove-Item checkBiblio\audit_report.txt -ErrorAction SilentlyContinue

            Write-Host ""
            Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
            Write-Host "  Iniciant UIAudit"
            Write-Host "══════════════════════════════════════" -ForegroundColor Cyan
            $argsList = @()
            if ($Auto) { $argsList += "--auto" }
            $argsList += $AuditArgs
            & java -Xmx512m -cp $cp checkBiblio.UIAudit @argsList
            if ($LASTEXITCODE -ne 0) {
                $stepErrors += "UIAudit ha sortit amb codi $LASTEXITCODE"
                $finalExit = 1
            }
        }
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

    if (Test-Path checkBiblio\audit_report.txt) {
        $auditTotals = Select-String -Path checkBiblio\audit_report.txt -Pattern '\] FAIL: \d+  WARN: \d+|^\[AUTO\] Audit complete' |
            Select-Object -Last 2
        if ($auditTotals) {
            Write-Host ""
            Write-Host "  Totals d'UIAudit:"
            foreach ($t in $auditTotals) { Write-Host "    $($t.Line)" }
        }
        $auditIssues = Select-String -Path checkBiblio\audit_report.txt -Pattern '\] (FAIL|WARN|ERROR):|FATAL:|APP LAUNCH ERROR' |
            Where-Object { $_.Line -notmatch '\] FAIL: \d+  WARN: \d+$' }
        if ($auditIssues) {
            Write-Host ""
            Write-Host "  Problemes d'UIAudit (FAIL / WARN / ERROR):"
            foreach ($i in $auditIssues) { Write-Host "    $($i.Line)" }
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
