#!/usr/bin/env bash
set -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! command -v java >/dev/null 2>&1; then
    echo "Error: Java is not installed. Please install Java 25 or later." >&2
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1,2)
if [ "$(echo "$JAVA_VER < 25" | bc 2>/dev/null || echo 1)" = "1" ]; then
    echo "Error: Java $JAVA_VER found, but Java 25+ is required." >&2
    exit 1
fi

INST_APP="${HOME}/.local/share/Biblioteca"
INST_BIN="${HOME}/.local/bin"
INST_DKT="${HOME}/.local/share/applications"

echo "Installing Biblioteca to ${INST_APP}..."
mkdir -p "${INST_APP}/lib" "${INST_BIN}" "${INST_DKT}"

cp "${DIR}/build/artifacts/biblioteca-fat.jar" "${INST_APP}/biblioteca.jar"

# Només els jars declarats a build/deps.lock (etiqueta runtime). La carpeta
# lib/ pot contenir versions antigues que no tenen res a veure amb el
# build actual: instal·lar-les era una inconsistència entre el lock i el
# paquet instal·lat. Si el lock no hi és, es manten el comportament antic.
LOCK="${DIR}/build/deps.lock"
if [ -f "$LOCK" ]; then
    while read -r gav sha tag; do
        case "$gav" in \#*|"") continue ;; esac
        [ "$tag" = "runtime" ] || continue
        art=$(echo "$gav" | cut -d: -f2)
        ver=$(echo "$gav" | cut -d: -f3)
        [ -f "${DIR}/lib/${art}-${ver}.jar" ] && cp "${DIR}/lib/${art}-${ver}.jar" "${INST_APP}/lib/"
    done < "$LOCK"
else
    cp "${DIR}/lib/"*.jar "${INST_APP}/lib/"
fi

# Copy icon if available
[ -f "${DIR}/packaging/icon-256.png" ] && cp "${DIR}/packaging/icon-256.png" "${INST_APP}/Biblioteca.png"

# Launcher script.
# El classpath inclou els jars de lib/ a més del JAR gras: el JAR gras
# ja les porta embegudes, però tenir-les al classpath evita que un JAR
# construit sense dependències (o amb una versió antiga) arrenqui i mori
# en el primer accés a la base de dades. Els jars de lib/ provenen del
# lock (etiqueta runtime), de manera que les versions coincideixen amb
# les del JAR gras.
cat > "${INST_BIN}/biblioteca" << LAUNCHER
#!/usr/bin/env bash
exec java -Xmx512m -cp "\${HOME}/.local/share/Biblioteca/biblioteca.jar:\${HOME}/.local/share/Biblioteca/lib/*" main.Executable "\$@"
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
