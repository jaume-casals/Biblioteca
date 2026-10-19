#!/usr/bin/env bash
#
# scripts/mariadb_test.sh — executa el contracte de persistència contra un
# MariaDB real, aïllat, sense tocar la base de dades personal de l'usuari.
#
# Què fa:
#   1. Arrenca un contenidor MariaDB de rebut (podman; docker com a reserva)
#      amb un port d'amfitrió lliure assignat pel runtime i una contrasenya
#      de root temporal.
#   2. Espera que el servidor accepti consultes.
#   3. Executa el contracte JUnit 5 en una JVM **separada** de `make test`,
#      sense `biblioteca.h2.url` (aquesta propietat forçaria dbType=h2).
#   4. Esborra el contenidor i el seu volum anònim en la sortida, passi el
#      que passi (trap).
#
# Què NO fa (a propòsit):
#   · No toca cap contenidor, imatge, volum o port que no sigui el seu.
#   · No llegeix ni escriu ~/.biblioteca.
#   · No fa servir la base de dades BIBLIOTECA de producció: l'esquema de
#     proves és BIBLIOTECA_TEST.
#
# Variables d'entorn:
#   MARIADB_TEST_HOST      usa un servidor extern enlloc de contenidor
#   MARIADB_TEST_PORT      port del servidor extern (per defecte 3306)
#   MARIADB_TEST_USER      usuari (per defecte root)
#   MARIADB_TEST_PASSWORD  contrasenya
#   MARIADB_TEST_DATABASE  esquema de proves (per defecte BIBLIOTECA_TEST)
#   MARIADB_TEST_IMAGE     imatge (per defecte docker.io/library/mariadb:11.4)
#   KEEP_CONTAINER=1       no esborra el contenidor al final
#
# Sortida: 0 si el contracte passa, 1 si falla, 2 si la infraestructura no
# s'ha pogut aixecar.

set -euo pipefail

# ── Configuració ───────────────────────────────────────────────────────────
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

IMAGE="${MARIADB_TEST_IMAGE:-docker.io/library/mariadb:11.4}"
DB_NAME="${MARIADB_TEST_DATABASE:-BIBLIOTECA_TEST}"
DB_USER="${MARIADB_TEST_USER:-root}"
NAME_PREFIX="biblioteca-mariadb-test"
CONTAINER="${NAME_PREFIX}-$$-$(date +%s)"

EXTERNAL_HOST="${MARIADB_TEST_HOST:-}"

say() { printf '\n\033[1m[mariadb-test] %s\033[0m\n' "$*"; }
run() { printf '\033[36m+ %s\033[0m\n' "$*"; "$@"; }

# ── Neteja garantida ───────────────────────────────────────────────────────
cleanup() {
    local rc=$?
    if [ "${KEEP_CONTAINER:-0}" = "1" ]; then
        say "KEEP_CONTAINER=1: el contenidor $CONTAINER es deixa en peu"
        say "  per aturar-lo: $RUNTIME rm -f -v $CONTAINER"
        return $rc
    fi
    if [ -n "${CONTAINER:-}" ] && [ -n "${RUNTIME:-}" ]; then
        say "Esborrant el contenidor de rebut $CONTAINER (i el seu volum anònim)"
        run "$RUNTIME" rm -f -v "$CONTAINER" || true
    fi
    return $rc
}
trap cleanup EXIT

# ── Runtime de contenidors ─────────────────────────────────────────────────
if [ -n "$EXTERNAL_HOST" ]; then
    RUNTIME=""
    say "Mode servidor extern: $EXTERNAL_HOST (no s'arrenca cap contenidor)"
else
    if command -v podman >/dev/null 2>&1; then
        RUNTIME="podman"
    elif command -v docker >/dev/null 2>&1; then
        RUNTIME="docker"
    else
        say "ERROR: no hi ha ni podman ni docker instal·lats."
        say "  Instal·la un dels dos, o apunta MARIADB_TEST_HOST a un servidor MariaDB existent."
        exit 2
    fi
    say "Runtime de contenidors: $RUNTIME"
    say "Imatge: $IMAGE"

    if ! "$RUNTIME" image exists "$IMAGE" 2>/dev/null; then
        say "Baixant la imatge $IMAGE (pot trigar una estona la primera vegada)"
        run "$RUNTIME" pull "$IMAGE"
    fi

    # Contrasenya de rebut: no serveix per a res fora del contenidor.
    ROOT_PASSWORD="$(head -c 16 /dev/urandom | od -An -tx1 | tr -d ' \n')"

    say "Arrencant el contenidor de rebut $CONTAINER"
    # -p 127.0.0.1::3306 deixa que el runtime assigni un port lliure de
    # l'amfitrió: no es tria cap port concret, així que no se'n pot
    # xoc amb cap servei personal.
    run "$RUNTIME" run -d --rm \
        --name "$CONTAINER" \
        -e MARIADB_ROOT_PASSWORD="$ROOT_PASSWORD" \
        -e MARIADB_ROOT_HOST=% \
        -p 127.0.0.1::3306 \
        "$IMAGE"

    say "Espera que MariaDB accepti consultes (fins a 90 s)"
    # La consulta es fa per TCP (127.0.0.1) i no pel socket UNIX: durant la
    # fase d'inicialització el contenidor arrenca un mysqld temporal que
    # només escolta al socket, de manera que un ping pel socket pot
    # respondre just abans que el servidor definitiu substitueixi el
    # temporal. El bootstrap es fa amb --skip-networking, així que per TCP
    # només respon el servidor definitiu.
    ready=0
    for _ in $(seq 1 90); do
        if "$RUNTIME" exec "$CONTAINER" mariadb -h 127.0.0.1 -P 3306 --protocol=TCP \
                -uroot -p"$ROOT_PASSWORD" -N -e "SELECT 1;" >/dev/null 2>&1; then
            ready=1
            break
        fi
        sleep 1
    done
    if [ "$ready" != "1" ]; then
        say "ERROR: MariaDB no s'ha preparat dins del temps previst."
        say "--- dmesg del contenidor ---"
        "$RUNTIME" logs --tail 50 "$CONTAINER" || true
        exit 2
    fi

    PORT_LINE="$("$RUNTIME" port "$CONTAINER" 3306/tcp | head -1)"
    HOST_PORT="${PORT_LINE##*:}"
    HOST="127.0.0.1:${HOST_PORT}"
    DB_USER="root"
    DB_PASSWORD="$ROOT_PASSWORD"
    say "MariaDB a punt: $HOST (port assignat pel runtime)"
    run "$RUNTIME" exec "$CONTAINER" mariadb -h 127.0.0.1 -P 3306 --protocol=TCP \
        -uroot -p"$DB_PASSWORD" -N -e "SELECT VERSION();"
fi

# ── Servidor extern: parametres ───────────────────────────────────────────
if [ -n "$EXTERNAL_HOST" ]; then
    HOST="$EXTERNAL_HOST"
    PORT="${MARIADB_TEST_PORT:-3306}"
    DB_PASSWORD="${MARIADB_TEST_PASSWORD:-}"
    if [ "$PORT" != "3306" ]; then
        HOST="$HOST:$PORT"
    fi
    say "Mode servidor extern: $HOST"
fi

# ── Classpath (la mateixa font de veritat que el Makefile) ─────────────────
if [ ! -d bin ]; then
    say "ERROR: no hi ha el directori bin/. Executa 'make compile' primer."
    exit 2
fi

PY="$(command -v python3 || command -v python)"
CP_RUNTIME="$("$PY" scripts/fetch_deps.py classpath --tag runtime --dir lib)"
CP_TEST="$("$PY" scripts/fetch_deps.py classpath --tag test --dir lib)"
CP_FUZZ="$("$PY" scripts/fetch_deps.py classpath --tag fuzz --dir lib)"
CP="bin:${CP_RUNTIME}:.:${CP_TEST}:${CP_FUZZ}"

# El llançador JUnit: el primer jar del classpath de proves que en sigui,
# exactament com ho tria el Makefile (variable JUNIT5_STANDALONE).
JUNIT_JAR=""
IFS=':' read -r -a _cp_parts <<< "$CP_TEST"
for _p in "${_cp_parts[@]}"; do
    case "$(basename "$_p")" in
        junit-platform-console-standalone*) JUNIT_JAR="$_p"; break;;
    esac
done
if [ -z "$JUNIT_JAR" ]; then
    say "ERROR: no s'ha trobat el llançador JUnit. Executa 'make deps' primer."
    exit 2
fi

# ── Compilació dels fitxers de test ────────────────────────────────────────
# `make compile` només compila src/; les classes de test s'han de compilar
# aquí (com fa el target `test` del Makefile) perquè el contracte existeixi.
say "Compilant les classes de test"
TEST_LIST="test_classes.mariadb.$$.txt"
find test/ -name "*.java" > "$TEST_LIST"
run javac -g -cp "bin:${CP_RUNTIME}:.:${CP_TEST}:${CP_FUZZ}" "@$TEST_LIST" -d bin
rm -f "$TEST_LIST"

# ── Execució del contracte ─────────────────────────────────────────────────
#
# Sense -Dbiblioteca.h2.url: aquesta propietat fa que ControladorPersistencia
# connecti amb H2 en lloc de MariaDB.
say "Contracte de persistència contra MariaDB (JVM separada, sense biblioteca.h2.url)"

set -x
java -Dbiblioteca.test=true \
     -Dbiblioteca.mariadb.host="$HOST" \
     -Dbiblioteca.mariadb.user="$DB_USER" \
     -Dbiblioteca.mariadb.password="$DB_PASSWORD" \
     -Dbiblioteca.mariadb.database="$DB_NAME" \
     -jar "$JUNIT_JAR" execute \
     --class-path "$CP" \
     --select-class=persistencia.ContractPersistenciaMariaDBTest \
     --details=summary \
     --disable-ansi-colors
set +x
