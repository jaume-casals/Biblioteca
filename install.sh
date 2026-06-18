#!/usr/bin/env bash
set -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v java >/dev/null 2>&1; then
    echo "Error: Java is not installed. Please install Java 21 or later." >&2
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1,2)
if [ "$(echo "$JAVA_VER < 21" | bc 2>/dev/null || echo 1)" = "1" ]; then
    echo "Error: Java $JAVA_VER found, but Java 21+ is required." >&2
    exit 1
fi

INST_APP="${HOME}/.local/share/Biblioteca"
INST_BIN="${HOME}/.local/bin"
INST_DKT="${HOME}/.local/share/applications"

echo "Installing Biblioteca to ${INST_APP}..."
mkdir -p "${INST_APP}/lib" "${INST_BIN}" "${INST_DKT}"

cp "${DIR}/biblioteca-fat.jar" "${INST_APP}/biblioteca.jar"
cp "${DIR}/lib/"*.jar       "${INST_APP}/lib/"

# Copy icon if available
[ -f "${DIR}/packaging/icon-256.png" ] && cp "${DIR}/packaging/icon-256.png" "${INST_APP}/Biblioteca.png"

# Launcher script
cat > "${INST_BIN}/biblioteca" << LAUNCHER
#!/usr/bin/env bash
exec java -Xmx512m -cp "\${HOME}/.local/share/Biblioteca/biblioteca.jar" main.Executable "\$@"
LAUNCHER
chmod +x "${INST_BIN}/biblioteca"

# Desktop entry
cat > "${INST_DKT}/biblioteca.desktop" << DESKTOP
[Desktop Entry]
Name=Biblioteca
Comment=Gestió Personal de Llibres
Exec=${INST_BIN}/biblioteca
Icon=${INST_APP}/Biblioteca.png
Terminal=false
Type=Application
Categories=Office;Education;
StartupNotify=true
DESKTOP
chmod +x "${INST_DKT}/biblioteca.desktop"
update-desktop-database "${INST_DKT}" 2>/dev/null || true

echo ""
echo "Done. Biblioteca installed."
echo "  App:     ${INST_APP}"
echo "  Cmd:     ${INST_BIN}/biblioteca"
echo "  Desktop: ${INST_DKT}/biblioteca.desktop"
echo ""
echo "If ${INST_BIN} is not in PATH, add to ~/.bashrc:"
echo '  export PATH="$HOME/.local/bin:$PATH"'
