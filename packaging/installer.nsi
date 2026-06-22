; ─── Biblioteca Installer ─────────────────────────────────────────────────────
; Build: cd packaging && makensis installer.nsi
; Requires in packaging/:
;   Biblioteca.exe             — Launch4j-wrapped JAR
;   Biblioteca.ico             — multi-size Windows icon
;   jre/                       — Temurin 21 JRE for Windows x64 (bundledJrePath)
;   data/biblioteca.mv.db      — H2 snapshot seeded on first launch
;
; Output: ../build/artifacts/install.exe (or ../build/artifacts/install-bundle.exe)
; ─────────────────────────────────────────────────────────────────────────────

!define APP_NAME      "Biblioteca"
!define APP_VERSION   "1.0"
!define APP_PUBLISHER "Jordi Casals"
!define APP_URL       "https://github.com/jordic/Biblioteca"
!define APP_EXE       "Biblioteca.exe"
!define APP_ICO       "Biblioteca.ico"
!define INST_DIR      "$PROGRAMFILES64\Biblioteca"
!define UNREG_KEY     "Software\Microsoft\Windows\CurrentVersion\Uninstall\Biblioteca"

Name            "${APP_NAME} ${APP_VERSION}"
!ifndef OUTFILE
  OutFile       "../build/artifacts/install.exe"
!else
  OutFile       "${OUTFILE}"
!endif
InstallDir      "${INST_DIR}"
InstallDirRegKey HKLM "${UNREG_KEY}" "InstallLocation"
RequestExecutionLevel admin
SetCompressor   /SOLID lzma

Icon    "${APP_ICO}"
UninstallIcon   "${APP_ICO}"

; ─── MUI2 ─────────────────────────────────────────────────────────────────────
!include "MUI2.nsh"
!define MUI_ICON                         "${APP_ICO}"
!define MUI_UNICON                       "${APP_ICO}"
!define MUI_ABORTWARNING
!define MUI_WELCOMEPAGE_TITLE            "Instal·la Biblioteca ${APP_VERSION}"
!define MUI_WELCOMEPAGE_TEXT             "Gestió personal de la teva col·lecció de llibres.$\r$\n$\r$\nFet clic a Següent per continuar."
!define MUI_FINISHPAGE_RUN               "$INSTDIR\${APP_EXE}"
!define MUI_FINISHPAGE_RUN_TEXT          "Obre Biblioteca ara"
!define MUI_FINISHPAGE_LINK              "Vés al repositori"
!define MUI_FINISHPAGE_LINK_LOCATION     "${APP_URL}"

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "Catalan"
!insertmacro MUI_LANGUAGE "Spanish"
!insertmacro MUI_LANGUAGE "English"

; ─── Java 21 check ────────────────────────────────────────────────────────────
Function CheckJava
  ReadRegStr $0 HKLM "SOFTWARE\JavaSoft\JRE" "CurrentVersion"
  StrCmp $0 "" 0 jre_found
  ReadRegStr $0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  StrCmp $0 "" no_java jre_found
  no_java:
    MessageBox MB_YESNO|MB_ICONQUESTION \
      "Java 21 o superior no s'ha detectat.$\r$\nBiblioteca requereix Java 21+.$\r$\n$\r$\nVols anar a la pàgina de descàrrega?" \
      IDNO continue_anyway
    ExecShell "open" "https://adoptium.net/temurin/releases/?version=21"
  continue_anyway:
  jre_found:
FunctionEnd

; ─── Install ──────────────────────────────────────────────────────────────────
Section "Biblioteca" SecMain
  SectionIn RO
  Call CheckJava

  ; EXE + icon a l'arrel
  SetOutPath "$INSTDIR"
  File "${APP_EXE}"
  File "${APP_ICO}"

  ; JRE empaquetat — Launch4j el busca a $INSTDIR\jre via bundledJrePath=jre
  SetOutPath "$INSTDIR\jre"
  File /r "jre\*.*"

  ; Snapshot de la BD — sembrada al perfil de l'usuari al primer llançament
  SetOutPath "$INSTDIR\data"
  File "data\biblioteca.mv.db"

  WriteUninstaller "$INSTDIR\Uninstall.exe"

  ; Add/Remove Programs entry
  WriteRegStr   HKLM "${UNREG_KEY}" "DisplayName"     "${APP_NAME}"
  WriteRegStr   HKLM "${UNREG_KEY}" "DisplayVersion"  "${APP_VERSION}"
  WriteRegStr   HKLM "${UNREG_KEY}" "Publisher"       "${APP_PUBLISHER}"
  WriteRegStr   HKLM "${UNREG_KEY}" "URLInfoAbout"    "${APP_URL}"
  WriteRegStr   HKLM "${UNREG_KEY}" "InstallLocation" "$INSTDIR"
  WriteRegStr   HKLM "${UNREG_KEY}" "UninstallString" '"$INSTDIR\Uninstall.exe"'
  WriteRegStr   HKLM "${UNREG_KEY}" "DisplayIcon"     '"$INSTDIR\${APP_EXE}"'
  WriteRegDWORD HKLM "${UNREG_KEY}" "NoModify"        1
  WriteRegDWORD HKLM "${UNREG_KEY}" "NoRepair"        1
  ; Mida estimada (KB): JRE ~150 MB + app ~7 MB + BD ~1 MB ≈ 180000 KB
  WriteRegDWORD HKLM "${UNREG_KEY}" "EstimatedSize"   180000

  ; Start Menu
  CreateDirectory "$SMPROGRAMS\${APP_NAME}"
  CreateShortcut  "$SMPROGRAMS\${APP_NAME}\${APP_NAME}.lnk" \
                  "$INSTDIR\${APP_EXE}" "" "$INSTDIR\${APP_ICO}" 0
  CreateShortcut  "$SMPROGRAMS\${APP_NAME}\Desinstal·la.lnk" \
                  "$INSTDIR\Uninstall.exe"

  ; Desktop shortcut
  CreateShortcut  "$DESKTOP\${APP_NAME}.lnk" \
                  "$INSTDIR\${APP_EXE}" "" "$INSTDIR\${APP_ICO}" 0
SectionEnd

; ─── Uninstall ────────────────────────────────────────────────────────────────
Section "Uninstall"
  Delete "$INSTDIR\${APP_EXE}"
  Delete "$INSTDIR\${APP_ICO}"
  Delete "$INSTDIR\Uninstall.exe"
  ; Neteja el JRE empaquetat i el snapshot de BD.
  ; RMDir /r falla silenciós si algun fitxer està en ús — acceptable
  ; en un desinstal·lador; l'usuari ho tornarà a provar.
  RMDir /r "$INSTDIR\jre"
  RMDir /r "$INSTDIR\data"
  RMDir  "$INSTDIR"

  Delete "$SMPROGRAMS\${APP_NAME}\${APP_NAME}.lnk"
  Delete "$SMPROGRAMS\${APP_NAME}\Desinstal·la.lnk"
  RMDir  "$SMPROGRAMS\${APP_NAME}"
  Delete "$DESKTOP\${APP_NAME}.lnk"

  DeleteRegKey HKLM "${UNREG_KEY}"
SectionEnd
