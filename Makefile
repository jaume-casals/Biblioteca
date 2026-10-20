# ---------- RUN & COMPILE ---------- #

# Multiple targets share the same `bin/` output (compile, fuzz-test-compile,
# populate, test). They must run serially to avoid javac-on-bin races and
# the resulting "rm: cannot remove classes.txt" / corrupted class files.
.NOTPARALLEL:

# Staging dir for a compile. The build lands here first and is swapped into
# bin/ in one step, so a half-written bin/ can never replace a good one.
BUILD_DIR := .build-tmp.$(shell echo $$$$)

# Unique per make invocation. Recipe lines run in separate shells, so a
# shell-level $$ would differ between the find, the guard and the javac call.
# These tags keep concurrent make runs from sharing one source list.
SRC_LIST := classes.$(shell echo $$$$).txt
TEST_LIST := test_classes.$(shell echo $$$$).txt

# These targets must always run: a stray file named "compile" or "run" in the
# project root would otherwise make make skip the recipe entirely.
.PHONY: compile run run-only clean test populate jar fat-jar audit

# Pick the first javac that actually runs. A zero-byte or truncated launcher
# exits 0 without compiling anything, which silently produces an empty build
# and then fails at launch with "Could not find or load main class".
# A zero-byte launcher exits 0 when the shell runs it as an empty script, so
# the probe must look at the OUTPUT: a real javac prints "javac <version>".
JAVAC := $(shell for c in $$(which javac 2>/dev/null) $$(find /usr/lib/jvm /home/j/opt /usr/java -name javac -type f 2>/dev/null | sort); do if [ -x "$$c" ] && "$$c" -version 2>&1 | grep -q "javac"; then echo "$$c"; break; fi; done)
ifeq ($(JAVAC),)
    JAVAC := /usr/lib/jvm/java-25-openjdk/bin/javac
endif

# Same rule for the runtime: a zero-byte launcher exits 0 as an empty script,
# so pick the first java that actually reports a version.
JAVA := $(shell for c in $$(which java 2>/dev/null) $$(find /usr/lib/jvm /home/j/opt /usr/java -name java -type f 2>/dev/null | sort); do if [ -x "$$c" ] && "$$c" -version 2>&1 | grep -qi "version"; then echo "$$c"; break; fi; done)
ifeq ($(JAVA),)
    JAVA := java
endif

# ---------- DEPENDÈNCIES ----------
#
# build/deps.lock és l'única font de veritat de les versions: cada línia
# és "grup:artifact:versió sha256 etiqueta". El classpath es deriva del lock,
# de manera que Makefile i lock no es poden desincronitzar.
#
#   make deps         baixa els jars que falten i verifica els SHA-256
#   make deps-verify  verifica els SHA-256 sense baixar res
#   make deps-update  consulta Maven Central i actualitza el lock
#
# Sense aquest pas, un checkout net no compila: els jars de lib/ no són
# versionats (*.jar és al .gitignore) i gson és necessari per compilar.

DEPS_LOCK := build/deps.lock
DEPS_SCRIPT := scripts/fetch_deps.py
PYTHON := $(shell command -v python3 2>/dev/null || command -v python 2>/dev/null)

# Directori on hi ha els jars. Per defecte lib/, però es pot apuntar a un
# directori alternatiu (p. ex. un @"farm" de enllaços simbòlics amb només
# els jars de build/deps.lock) per comprovar que la compilació i els tests
# no depenen de cap jar sobrant de lib/.
DEPS_DIR ?= lib

DEPS_CP_RUNTIME := $(shell $(PYTHON) $(DEPS_SCRIPT) classpath --tag runtime --dir $(DEPS_DIR) 2>/dev/null)
DEPS_CP_TEST := $(shell $(PYTHON) $(DEPS_SCRIPT) classpath --tag test --dir $(DEPS_DIR) 2>/dev/null)
DEPS_CP_FUZZ := $(shell $(PYTHON) $(DEPS_SCRIPT) classpath --tag fuzz --dir $(DEPS_DIR) 2>/dev/null)

CP := $(DEPS_CP_RUNTIME):.
TEST_CP := $(CP):$(DEPS_CP_TEST)
FUZZ_CP := $(TEST_CP):$(DEPS_CP_FUZZ)

# $(filter) separa la llista per espais, no per ':', cal convertir el
# classpath en paraules abans de filtrar-lo. A més, $(filter) de GNU Make
# només admet un únic comodí %, així que el cercament es fa amb findstring.
CP_RUNTIME_WORDS := $(subst :, ,$(DEPS_CP_RUNTIME))
CP_TEST_WORDS := $(subst :, ,$(DEPS_CP_TEST))
CP_FUZZ_WORDS := $(subst :, ,$(DEPS_CP_FUZZ))

# jar-with <llista de paraules> <text>: retorna els jars que contenen el text.
jar-with = $(foreach j,$(1),$(if $(findstring $(2),$(j)),$(j)))

JUNIT5_STANDALONE := $(call jar-with,$(CP_TEST_WORDS),junit-platform-console-standalone)
JQWIK_API := $(call jar-with,$(CP_TEST_WORDS),jqwik-api)
JQWIK_ENGINE := $(call jar-with,$(CP_TEST_WORDS),jqwik-engine)
JAZZER_API := $(call jar-with,$(CP_FUZZ_WORDS),jazzer-api)
JAZZER_JUNIT := $(call jar-with,$(CP_FUZZ_WORDS),jazzer-junit)
# El bootstrap de Jazzer és l'únic "jazzer-" que no és api ni junit.
JAZZER := $(filter-out $(JAZZER_API) $(JAZZER_JUNIT),$(call jar-with,$(CP_FUZZ_WORDS),jazzer-))

.PHONY: deps deps-verify deps-update

deps:
	@if [ -z "$(PYTHON)" ]; then echo "ERROR: python3 no trobat (necessari per a scripts/$(DEPS_SCRIPT))"; exit 1; fi
	@$(PYTHON) $(DEPS_SCRIPT) fetch --lock $(DEPS_LOCK) --dir $(DEPS_DIR)

deps-verify:
	@if [ -z "$(PYTHON)" ]; then echo "ERROR: python3 no trobat (necessari per a scripts/$(DEPS_SCRIPT))"; exit 1; fi
	@$(PYTHON) $(DEPS_SCRIPT) verify --lock $(DEPS_LOCK) --dir $(DEPS_DIR)

deps-update:
	@if [ -z "$(PYTHON)" ]; then echo "ERROR: python3 no trobat (necessari per a scripts/$(DEPS_SCRIPT))"; exit 1; fi
	@$(PYTHON) $(DEPS_SCRIPT) update --lock $(DEPS_LOCK)

# ---------- COMPILACIÓ INCREMENTAL ----------
#
# Compilar src/ i test/ des de zero cada vegada és la major part del temps
# de `make test`. Els segells (stamps) permeten reutilitzar bin/ quan res
# ha canviat:
#
#   build/stamp/src.stamp   línia 1 = nombre de .java a src/
#                          línia 2 = id de build (checksum de deps.lock + timestamp)
#   build/stamp/test.stamp  línia 1 = nombre de .java a test/
#                          línia 2 = id de build de src/ amb què es van compilar
#
# Es recompila quan: el nombre de fitxers canvia (fitxer afegit o esborrat),
# algun .java és més nou que el segell, deps.lock ha canviat, o el build id
# de src/ és diferent del que hi ha al segell de test/. Qualsevol altre cas
# reutilitza les classes ja compilades.
STAMP_DIR := build/stamp
SRC_STAMP := $(STAMP_DIR)/src.stamp
TEST_STAMP := $(STAMP_DIR)/test.stamp

compile: deps
	@if [ ! -f "$(JAVAC)" ]; then \
		echo "ERROR: javac not found. Install the JDK:"; \
		echo "  sudo dnf install java-25-openjdk-devel"; \
		exit 1; \
	fi
	@if ! "$(JAVAC)" -version 2>&1 | grep -q "javac"; then \
		echo "ERROR: javac at $(JAVAC) is not a working compiler."; \
		echo "  It prints no version - the file is empty, truncated or not a JDK."; \
		echo "  Fix JAVA_HOME / PATH, then run: make compile"; \
		exit 1; \
	fi
	@mkdir -p $(STAMP_DIR)
	@src_count=`find ./src/ -name "*.java" | wc -l | tr -d ' '`; \
	 prev_count=`sed -n '1p' $(SRC_STAMP) 2>/dev/null | tr -d ' '`; \
	 newer=`find ./src/ -name "*.java" -newer $(SRC_STAMP) -print -quit 2>/dev/null`; \
	 newer_deps=`find $(DEPS_LOCK) -newer $(SRC_STAMP) -print -quit 2>/dev/null`; \
	 if [ -f "$(SRC_STAMP)" ] && [ "$$src_count" = "$$prev_count" ] \
	     && [ -z "$$newer" ] && [ -z "$$newer_deps" ] \
	     && [ -f bin/main/Executable.class ]; then \
		echo "compile: src/ sense canvis — es reutilitza bin/"; \
	 else \
		echo "compile: recompilant src/ ..."; \
		find ./src/ -name "*.java" > $(SRC_LIST); \
		if [ ! -s $(SRC_LIST) ]; then \
			echo "ERROR: no Java sources found under ./src/ (source list empty)"; \
			rm -f $(SRC_LIST); \
			exit 1; \
		fi; \
		rm -rf $(BUILD_DIR) && mkdir -p $(BUILD_DIR); \
		$(JAVAC) -g --release 25 -cp $(CP) @$(SRC_LIST) -d $(BUILD_DIR) || { rm -rf $(BUILD_DIR); rm -f $(SRC_LIST); exit 1; }; \
		rm -f $(SRC_LIST); \
		rm -rf bin && mv $(BUILD_DIR) bin; \
		printf '%s\n%s\n' "$$src_count" "`cksum $(DEPS_LOCK) | tr -d ' '`-`date +%s%N`" > $(SRC_STAMP); \
	fi
	@if [ -d src/herramienta ]; then mkdir -p bin/herramienta && cp src/herramienta/*.properties bin/herramienta/ 2>/dev/null || true; fi
	@if [ -d src/web ]; then rm -rf bin/web && cp -r src/web bin/web; fi
	@if [ -f src/LICENSE ]; then cp src/LICENSE bin/; fi
	@# El ABOUT (QuantADialeg) llegeix /version.properties del classpath: si no
	@# es copia, el diàleg no mostra la versió en una build de make.
	@if [ -f src/version.properties ]; then cp src/version.properties bin/; fi
	@if [ ! -f bin/main/Executable.class ]; then \
		echo "ERROR: la compilacio no ha produït bin/main/Executable.class"; \
		exit 1; \
	fi

# Comprova que una base de dades creada amb una versió anterior del driver
# H2 s'obre amb la versió actual sense perdre dades. Fa servir una base de
# dades temporal: la base de dades real de l'usuari no s'ha de tocar.
#
# L'ordre de definició importa: CP_LEGACY_WORDS s'ha de definir ABANS que
# H2_LEGACY, perquè amb := (expansió immediata) una referència endavant
# donaria una llista buida i la comprovació de compatibilitat es faria
# amb el driver actual (és a dir, no es comprovava res).
CP_LEGACY_WORDS := $(subst :, ,$(shell $(PYTHON) $(DEPS_SCRIPT) classpath --tag legacy --dir $(DEPS_DIR) 2>/dev/null))
H2_LEGACY := $(call jar-with,$(CP_LEGACY_WORDS),h2-)

h2-compat: compile deps
	@mkdir -p build/h2compat build/h2compat-db
	@if [ -z "$(H2_LEGACY)" ]; then \
		echo "ERROR: no s'ha resolt cap jar H2 legacy (etiqueta 'legacy' de build/deps.lock)."; \
		echo "  La comprovació de compatibilitat seria buida: el 'create' usaria el driver actual."; \
		exit 1; \
	fi
	@$(JAVAC) -g -cp bin:$(CP) -d build/h2compat scripts/h2compat/CheckH2Compat.java
	@rm -f build/h2compat-db/biblioteca.mv.db
	@echo "== Crea la base de dades amb H2 $(H2_LEGACY) =="
	@$(JAVA) -cp bin:build/h2compat:$(H2_LEGACY):$(DEPS_CP_RUNTIME) \
		h2compat.CheckH2Compat create "$(CURDIR)/build/h2compat-db/biblioteca"
	@echo "== Obre la mateixa base de dades amb la versió actual =="
	@$(JAVA) -cp bin:build/h2compat:$(DEPS_CP_RUNTIME) \
		h2compat.CheckH2Compat verify "$(CURDIR)/build/h2compat-db/biblioteca"
	@rm -rf build/h2compat-db
	@echo "Compatibilitat H2 verificada."

run: compile
	@if [ -z "$(JAVA)" ]; then echo "ERROR: no working java runtime found."; exit 1; fi
	@$(JAVA) -version 2>&1 | grep -qi "version" || { echo "ERROR: java is not a working runtime: $(JAVA)"; \
		echo "  It prints no version - the file is empty, truncated or not a JDK."; \
		echo "  Fix JAVA_HOME / PATH."; exit 1; }
	$(JAVA) -cp bin:$(CP) main.Executable

run-only:
	@if [ ! -f "bin/main/Executable.class" ]; then \
		echo "ERROR: Classes not found. Run: make compile"; \
		exit 1; \
	fi
	@if [ -z "$(JAVA)" ]; then echo "ERROR: no working java runtime found."; exit 1; fi
	@$(JAVA) -version 2>&1 | grep -qi "version" || { echo "ERROR: java is not a working runtime: $(JAVA)"; exit 1; }
	$(JAVA) -cp bin:$(CP) main.Executable

# ---------- TEST ----------- #

# Els noms de classe de test es deriven dels fitxers font de test/ en lloc
# de fer coincidir un glob: així es cobreixen les dues convencions
# (FooTest.java i TestFoo.java) i el glob antic (*Test.java) deixava 30
# classes sense executar mai.
#
# Els fitxers que importen com.code_intelligence.jazzer són arnesos
# coverage-guided: només s'executen amb `make fuzz-jazzer` (necessiten
# l'agent de Jazzer). Per això el bootstrap jar de Jazzer (JAZZER) queda
# fora del classpath de la suite obligatòria, i només hi entren
# jazzer-api i jazzer-junit perquè les classes es puguin compilar i
# carregar sense arrencar l'agent.
TEST_SRC_DIR := test

# Classes de test: paquet (llegit del fitxer) + nom de la classe.
TEST_CLASSES := $(shell \
	for f in $$(find $(TEST_SRC_DIR) -name '*.java' | sort); do \
		pkg=$$(sed -n 's/^package[[:space:]]\+\([^;]*\);[[:space:]]*$$/\1/p' "$$f" | head -1); \
		cls=$$(basename "$$f" .java); \
		if [ -n "$$pkg" ]; then echo "$$pkg.$$cls"; else echo "$$cls"; fi; \
	done)

# Arnesos Jazzer: s'exclouen de la suite obligatòria.
JAZZER_TEST_CLASSES := $(shell \
	for f in $$(grep -rl 'com\.code_intelligence\.jazzer' $(TEST_SRC_DIR) --include='*.java' | sort); do \
		pkg=$$(sed -n 's/^package[[:space:]]\+\([^;]*\);[[:space:]]*$$/\1/p' "$$f" | head -1); \
		cls=$$(basename "$$f" .java); \
		if [ -n "$$pkg" ]; then echo "$$pkg.$$cls"; else echo "$$cls"; fi; \
	done)

# Suite obligatòria: totes les classes de test menys els arnesos Jazzer.
MANDATORY_TEST_CLASSES := $(filter-out $(JAZZER_TEST_CLASSES),$(TEST_CLASSES))

# Selectors de classe per al launcher de la plataforma JUnit.
SELECT_MANDATORY := $(foreach c,$(MANDATORY_TEST_CLASSES),--select-class=$(c))
SELECT_JAZZER := $(foreach c,$(JAZZER_TEST_CLASSES),--select-class=$(c))

TEST_REPORT_DIR := build/reports/tests

# ---------- FUZZ DEPS ----------
#
# El shim duplica les biblioteques natives de Jazzer al path que RulesJni
# espera (com/.../driver/libjazzer_driver_<os>_<arch>/) i s'afegeix al
# bootstrap classpath: les classes de driver es carreguen pel bootstrap
# classloader, que no cerca al classpath de l'aplicació.
FUZZ_SHIM_JAR := fuzz-shim/jazzer-native-shim.jar
BOOTCP := -Xbootclasspath/a:$(FUZZ_SHIM_JAR)

test: compile
	@mkdir -p $(STAMP_DIR)
	@test_count=`find $(TEST_SRC_DIR)/ -name "*.java" | wc -l | tr -d ' '`; \
	 prev_count=`sed -n '1p' $(TEST_STAMP) 2>/dev/null | tr -d ' '`; \
	 prev_src=`sed -n '2p' $(TEST_STAMP) 2>/dev/null | tr -d ' '`; \
	 cur_src=`sed -n '2p' $(SRC_STAMP) 2>/dev/null | tr -d ' '`; \
	 newer=`find $(TEST_SRC_DIR)/ -name "*.java" -newer $(TEST_STAMP) -print -quit 2>/dev/null`; \
	 if [ -f "$(TEST_STAMP)" ] && [ "$$test_count" = "$$prev_count" ] \
	     && [ -z "$$newer" ] && [ "$$prev_src" = "$$cur_src" ] \
	     && [ -f bin/BibliotecaTest.class ]; then \
		echo "test: test/ sense canvis — es reutilitzen les classes compilades"; \
	 else \
		echo "test: recompilant test/ ..."; \
		find $(TEST_SRC_DIR)/ -name "*.java" > $(TEST_LIST); \
		if [ ! -s $(TEST_LIST) ]; then \
			echo "ERROR: no s'ha trobat cap fitxer de test a $(TEST_SRC_DIR)/"; \
			rm -f $(TEST_LIST); exit 1; \
		fi; \
		$(JAVAC) -g -cp bin:$(FUZZ_CP) @$(TEST_LIST) -d bin || { rm -f $(TEST_LIST); exit 1; }; \
		rm -f $(TEST_LIST); \
		printf '%s\n%s\n' "$$test_count" "$$cur_src" > $(TEST_STAMP); \
	fi
	@echo "=== Suite legacy (BibliotecaTest) ==="
	@$(JAVA) -cp bin:$(FUZZ_CP) BibliotecaTest
	@echo ""
	@echo "=== Suite JUnit 5 ($$(echo $(MANDATORY_TEST_CLASSES) | wc -w) classes) ==="
	@echo "    Arnesos Jazzer exclosos de la suite obligatoria: $(JAZZER_TEST_CLASSES)"
	@echo "    (executa 'make fuzz-jazzer' per a la suite coverage-guided)"
	@mkdir -p $(TEST_REPORT_DIR)
	@$(JAVA) $(BOOTCP) -Dbiblioteca.test=true \
	    -Dbiblioteca.h2.url="jdbc:h2:mem:biblioteca_suite;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1" \
	    -jar $(JUNIT5_STANDALONE) execute \
	    $(SELECT_MANDATORY) \
	    --details=summary --disable-ansi-colors \
	    --fail-if-no-tests \
	    --reports-dir=$(TEST_REPORT_DIR) \
	    --class-path bin:$(FUZZ_CP)

# ---------- CONTRACTE MARIADB ---------- #
#
# `make test` arrenca tota la suite en UNA JVM amb
# -Dbiblioteca.h2.url=..., cosa que obliga el tipus de base de dades a H2.
# El contracte de MariaDB necessita una JVM separada, sense aquesta
# propietat, i un servidor MariaDB real: el script aixeca un contenidor de
# rebut (podman/docker), hi fa correr el contracte i l'esborra al final.
#
#   make test-mariadb              → contenidor de rebut, esborra al final
#   MARIADB_TEST_HOST=... make test-mariadb
#                                  → servidor MariaDB existent
#
# Detall: docs/MARIADB-TEST.md
MARIADB_TEST_SCRIPT := scripts/mariadb_test.sh

.PHONY: test-mariadb mariadb-up mariadb-down

test-mariadb: compile
	@if [ ! -x "$(MARIADB_TEST_SCRIPT)" ]; then \
		echo "ERROR: $(MARIADB_TEST_SCRIPT) no és executable (chmod +x)"; exit 1; \
	fi
	@$(MARIADB_TEST_SCRIPT)

# mariadb-up / mariadb-down deixen el servidor de proves en peu per fer
# proves manuals. Són una comoditat: el flux recomanat és `make test-mariadb`.
mariadb-up: compile
	@if [ ! -x "$(MARIADB_TEST_SCRIPT)" ]; then \
		echo "ERROR: $(MARIADB_TEST_SCRIPT) no és executable (chmod +x)"; exit 1; \
	fi
	@KEEP_CONTAINER=1 $(MARIADB_TEST_SCRIPT)

mariadb-down:
	@for c in $$(podman ps -a --format '{{.Names}}' 2>/dev/null | grep '^biblioteca-mariadb-test-' || true); do \
		echo "Esborrant $$c"; podman rm -f -v "$$c" || true; \
	done
	@for c in $$(docker ps -a --format '{{.Names}}' 2>/dev/null | grep '^biblioteca-mariadb-test-' || true); do \
		echo "Esborrant $$c"; docker rm -f -v "$$c" || true; \
	done

# Optional UI audit (requires display; skipped in headless CI unless DISPLAY set)
audit: compile
	@if [ -z "$$DISPLAY" ]; then echo "SKIP audit: no DISPLAY"; exit 0; fi
	@java -cp bin:$(CP) checkBiblio.UIAudit --auto

# ---------- POPULATE ------- #
# Fetches books from OpenLibrary and inserts them.
# Usage: make populate          (default 2000 books)
#        make populate MAX=500  (custom limit)

MAX ?= 2000

populate: compile
	@find ./test/ -name "*.java" > $(TEST_LIST)
	@$(JAVAC) -g -cp bin:$(FUZZ_CP) @$(TEST_LIST) -d bin
	@rm -f $(TEST_LIST)
	@$(JAVA) -cp bin:$(FUZZ_CP) -Dbiblioteca.root=$(shell pwd) test.PopulateDB $(MAX)

# ---------- INSTALLERS ----- #
#
# make jar                       → build/artifacts/biblioteca.jar (slim, needs lib/ alongside)
# make fat-jar                   → build/artifacts/biblioteca-fat.jar (all deps embedded)
# make icon                      → packaging/Biblioteca.ico (needs: ImageMagick)
# make installer-windows         → build/artifacts/install.exe   (needs: Launch4j + makensis)
# make installer-windows-bundle  → build/artifacts/install-bundle.exe (JRE + DB snapshot bundled)
# make installer-linux           → *.rpm / *.deb (needs: jpackage from JDK 17+)
#
# Launch4j (cross-compile JAR → Windows EXE):
#   Unpack to /opt/launch4j or set LAUNCH4J=/path/to/launch4j
#   Download: https://launch4j.sourceforge.net/
#
# NSIS (Windows installer builder, runs on Linux):
#   sudo dnf install mingw32-nsis   OR   sudo apt install nsis
#
# installer-windows-bundle also needs a Windows Temurin 25 JRE at
# packaging/jre/ and a DB snapshot at packaging/data/biblioteca.mv.db.
# Both are produced automatically by the target — just close the app
# before running.

LAUNCH4J ?= $(shell \
    if   command -v launch4j       >/dev/null 2>&1; then echo "launch4j"; \
    elif [ -x /opt/launch4j/launch4j ]; then echo "/opt/launch4j/launch4j"; \
    elif [ -f /opt/launch4j/launch4jc.jar ]; then echo "java -jar /opt/launch4j/launch4jc.jar"; \
    fi)

# Pick a JDK whose bin/java actually exists (some distros leave stub dirs).
LAUNCH4J_JAVA_HOME := $(shell \
    for D in /usr/lib/jvm/temurin-25-jdk \
             /usr/lib/jvm/java-25-temurin-jdk \
             /usr/lib/jvm/java-25-openjdk \
             /usr/lib/jvm/temurin-21-jdk \
             /usr/lib/jvm/java-21-temurin-jdk \
             /usr/lib/jvm/java-21-openjdk \
             /usr/lib/jvm/temurin-17-jdk \
             /usr/lib/jvm/java-17-temurin-jdk; do \
        [ -x "$$D/bin/java" ] && echo "$$D" && exit 0; \
    done; \
    JC="$$(command -v javac 2>/dev/null)"; \
    [ -n "$$JC" ] && dirname "$$(dirname "$$(readlink -f "$$JC")")")

# ── slim JAR (manifest points to external lib/ JARs) ─────────────────────────
jar: compile
	@mkdir -p build/artifacts
	@jar cfm build/artifacts/biblioteca.jar <(printf 'Manifest-Version: 1.0\nMain-Class: main.Executable\nClass-Path: $(subst $(CURDIR)/,,$(DEPS_CP_RUNTIME))\n\n') -C bin .
	@echo "Built build/artifacts/biblioteca.jar"

# ── fat JAR (all deps bundled, standalone) ────────────────────────────────────
fat-jar: compile
	@echo "Building fat JAR (bundling all dependencies)..."
	@rm -rf /tmp/_bib_fat && mkdir -p /tmp/_bib_fat
	@cd /tmp/_bib_fat && for J in $(DEPS_CP_RUNTIME); do \
	    [ -f "$(CURDIR)/$$J" ] && unzip -oq "$(CURDIR)/$$J" 2>/dev/null || true; \
	done
	@cp -r $(CURDIR)/bin/. /tmp/_bib_fat/
	@# Remove JAR signature files — they break fat JARs
	@rm -f /tmp/_bib_fat/META-INF/*.SF /tmp/_bib_fat/META-INF/*.DSA \
	       /tmp/_bib_fat/META-INF/*.RSA /tmp/_bib_fat/META-INF/*.EC
	@mkdir -p $(CURDIR)/build/artifacts
	@cd /tmp/_bib_fat && jar cfe $(CURDIR)/build/artifacts/biblioteca-fat.jar main.Executable .
	@rm -rf /tmp/_bib_fat
	@echo "Built build/artifacts/biblioteca-fat.jar ($$(du -sh $(CURDIR)/build/artifacts/biblioteca-fat.jar | cut -f1))"

# ── icon: SVG → multi-size ICO via ImageMagick ───────────────────────────────
icon: packaging/icon.svg
	@command -v convert >/dev/null 2>&1 || \
	    { echo "ERROR: ImageMagick not found.  sudo dnf install ImageMagick"; exit 1; }
	@mkdir -p packaging
	@# PNG at 256×256 (used by Linux .desktop entry)
	@convert packaging/icon.svg -resize 256x256 packaging/icon-256.png
	@# ICO with multiple embedded sizes (Windows)
	@convert packaging/icon.svg \
	    \( -clone 0 -resize 256x256 \) \
	    \( -clone 0 -resize  64x64  \) \
	    \( -clone 0 -resize  48x48  \) \
	    \( -clone 0 -resize  32x32  \) \
	    \( -clone 0 -resize  16x16  \) \
	    -delete 0 packaging/Biblioteca.ico
	@echo "Built packaging/Biblioteca.ico"

# ── Windows installer: fat-jar → Biblioteca.exe → install.exe ────────────────
installer-windows: fat-jar icon
	@echo ""
	@# ── Step 1: wrap fat JAR as Biblioteca.exe (Launch4j) ──────────────────
	@if [ -n "$(LAUNCH4J)" ]; then \
	    echo "Step 1: Launch4j → packaging/Biblioteca.exe"; \
	    JAVA_HOME="$(LAUNCH4J_JAVA_HOME)" $(LAUNCH4J) packaging/launch4j.xml && \
	    echo "  Biblioteca.exe ready ($$(du -sh packaging/Biblioteca.exe | cut -f1))"; \
	else \
	    echo "WARNING: Launch4j not found — packaging/Biblioteca.exe not built."; \
	    echo "  Download Launch4j: https://launch4j.sourceforge.net/"; \
	    echo "  Unpack to /opt/launch4j  OR  export LAUNCH4J=/path/to/launch4j"; \
	    echo "  Then re-run: make installer-windows"; \
	    exit 1; \
	fi
	@# ── Step 2: build install.exe with NSIS ────────────────────────────────
	@command -v makensis >/dev/null 2>&1 || \
	    { echo "ERROR: makensis not found.  sudo dnf install mingw32-nsis"; exit 1; }
	@echo "Step 2: NSIS → install.exe"
	@cd packaging && makensis installer.nsi
	@echo ""
	@echo "═══════════════════════════════════════════════"
	@echo "  build/artifacts/install.exe ready  ($$(du -sh build/artifacts/install.exe | cut -f1))"
	@echo "═══════════════════════════════════════════════"

# ── All-in-one Windows installer: bundles JRE + DB snapshot + app ─────────────
# Adds a Windows Temurin 21 JRE (packaging/jre/) and a snapshot of the
# user's current H2 database (packaging/data/biblioteca.mv.db) on top of
# the standard installer-windows chain. The app's SeedorBaseDades copies
# the snapshot into the user's profile on first launch.
installer-windows-bundle: fat-jar icon packaging/jre packaging/data/biblioteca.mv.db
	@echo ""
	@echo "Step 1: Launch4j → packaging/Biblioteca.exe (bundled JRE)"
	@if [ -n "$(LAUNCH4J)" ]; then \
	    JAVA_HOME="$(LAUNCH4J_JAVA_HOME)" $(LAUNCH4J) packaging/launch4j.xml && \
	    echo "  Biblioteca.exe ready ($$(du -sh packaging/Biblioteca.exe | cut -f1))"; \
	else \
	    echo "WARNING: Launch4j not found — packaging/Biblioteca.exe not built."; \
	    echo "  Download Launch4j: https://launch4j.sourceforge.net/"; \
	    echo "  Unpack to /opt/launch4j  OR  export LAUNCH4J=/path/to/launch4j"; \
	    echo "  Then re-run: make installer-windows-bundle"; \
	    exit 1; \
	fi
	@command -v makensis >/dev/null 2>&1 || \
	    { echo "ERROR: makensis not found.  sudo dnf install mingw32-nsis"; exit 1; }
	@echo "Step 2: NSIS → install-bundle.exe (with JRE + DB snapshot)"
	@mkdir -p build/artifacts
	@cd packaging && makensis -DOUTFILE=../build/artifacts/install-bundle.exe installer.nsi
	@echo ""
	@echo "═══════════════════════════════════════════════"
	@echo "  build/artifacts/install-bundle.exe ready  ($$(du -sh build/artifacts/install-bundle.exe | cut -f1))"
	@echo "  Double-clickable; bundles Temurin 25 JRE + your DB snapshot."
	@echo "  No Java 25 install required on the target machine."
	@echo "═══════════════════════════════════════════════"

# ── Windows JRE for the bundled installer (~150 MB Temurin 25 x64) ────────────
# Downloads once. If packaging/jre/ already contains bin/java.exe the
# download is skipped.
packaging/jre:
	@echo "Downloading Temurin 25 JRE (Windows x64)…"
	@mkdir -p packaging/jre
	@cd packaging/jre && curl -fsSL -o temurin.zip \
	    "https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jre/hotspot/normal/eclipse?project=jdk" \
	    && unzip -q temurin.zip \
	    && (mv jdk-25*/* . 2>/dev/null || mv jre-25*/* . 2>/dev/null || true) \
	    && rm -f temurin.zip \
	    && rmdir jdk-25* jre-25* 2>/dev/null || true
	@test -x packaging/jre/bin/java.exe || \
	    { echo "ERROR: Temurin JRE download failed or did not unpack to packaging/jre/"; exit 1; }
	@echo "  packaging/jre/ ready ($$(du -sh packaging/jre | cut -f1))"

# ── DB snapshot for the bundled installer ─────────────────────────────────────
# Refuses to run while the app holds the H2 file lock. Closes the app
# before re-running.
packaging/data/biblioteca.mv.db:
	@# Fail fast si l'app té el fitxer H2 blocat (no podem copiar-ne
	@# una versió consistent mentre s'hi escriu).
	@if pgrep -af 'java.*main\.Executable' >/dev/null 2>&1; then \
	    echo "ERROR: Biblioteca is running. Close it before snapshotting the DB."; \
	    pgrep -af 'java.*main\.Executable'; \
	    exit 1; \
	fi
	@test -f $(HOME)/.biblioteca/biblioteca.mv.db || \
	    { echo "ERROR: $(HOME)/.biblioteca/biblioteca.mv.db not found."; \
	      echo "  Run the app once to create it, then re-run this target."; exit 1; }
	@mkdir -p packaging/data
	@cp $(HOME)/.biblioteca/biblioteca.mv.db packaging/data/biblioteca.mv.db
	@echo "  DB snapshot ready ($$(du -sh packaging/data/biblioteca.mv.db | cut -f1))"

# ── Standalone Windows .exe with bundled JRE via jpackage ──────────────────────
jpackage-win: fat-jar icon
	@command -v jpackage >/dev/null 2>&1 || \
	    { echo "ERROR: jpackage not found. Install JDK 17+."; exit 1; }
	@rm -rf /tmp/_bib_win && mkdir -p /tmp/_bib_win
	@jpackage \
	    --type app-image \
	    --input . \
	    --main-jar build/artifacts/biblioteca-fat.jar \
	    --main-class main.Executable \
	    --name Biblioteca \
	    --app-version 1.0 \
	    --description "Gestio Personal de Llibres" \
	    --icon packaging/icon-256.png \
	    --dest /tmp/_bib_win \
	    --win-console
	cp -r /tmp/_bib_win/Biblioteca /home/j/Documents/DeV/Biblioteca/
	@echo ""
	@echo "═══════════════════════════════════════════════"
	@echo "  Standalone Windows app: ./Biblioteca/"
	@echo "  Run: ./Biblioteca/Biblioteca.exe"
	@echo "═══════════════════════════════════════════════"

# ── Windows installer (.exe) that installs to Program Files + desktop shortcut ──
installer-win-standalone: fat-jar icon
	@command -v jpackage >/dev/null 2>&1 || \
	    { echo "ERROR: jpackage not found. Install JDK 17+."; exit 1; }
	@command -v makensis >/dev/null 2>&1 || \
	    { echo "ERROR: makensis not found. sudo dnf install mingw32-nsis"; exit 1; }
	@echo "Step 1: jpackage → app-image"
	@rm -rf /tmp/_bib_win_img && mkdir -p /tmp/_bib_win_img
	@jpackage \
	    --type app-image \
	    --input . \
	    --main-jar build/artifacts/biblioteca-fat.jar \
	    --main-class main.Executable \
	    --name Biblioteca \
	    --app-version 1.0 \
	    --description "Gestio Personal de Llibres" \
	    --icon packaging/icon-256.png \
	    --dest /tmp/_bib_win_img \
	    --win-console
	@echo "Step 2: NSIS → install.exe"
	@mkdir -p /tmp/_bib_nsis
	cp -r /tmp/_bib_win_img/Biblioteca /tmp/_bib_nsis/Biblioteca
	@{ \
	    echo 'Name "Biblioteca"'; \
	    echo 'OutFile "install-win.exe"'; \
	    echo 'InstallDir "$$PROGRAMFILES64\Biblioteca"'; \
	    echo 'InstallDirRegKey HKLM "Software\Biblioteca" "InstallDir"'; \
	    echo 'Section install'; \
	    echo '  SetOutPath "$$INSTDIR"'; \
	    echo '  File /r "Biblioteca\*.*"'; \
	    echo '  CreateDirectory "$$SMPROGRAMS\Biblioteca"'; \
	    echo '  CreateShortCut "$$DESKTOP\Biblioteca.lnk" "$$INSTDIR\Biblioteca.exe"'; \
	    echo '  CreateShortCut "$$SMPROGRAMS\Biblioteca\Biblioteca.lnk" "$$INSTDIR\Biblioteca.exe"'; \
	    echo '  WriteRegStr HKLM "Software\Biblioteca" "InstallDir" "$$INSTDIR"'; \
	    echo '  WriteUninstaller "$$INSTDIR\uninstall.exe"'; \
	    echo 'SectionEnd'; \
	    echo 'Section uninstall'; \
	    echo '  Delete "$$DESKTOP\Biblioteca.lnk"'; \
	    echo '  Delete "$$SMPROGRAMS\Biblioteca\Biblioteca.lnk"'; \
	    echo '  RMDir /r "$$INSTDIR"'; \
	    echo 'SectionEnd'; \
	} > /tmp/_bib_nsis/installer.nsi
	cd /tmp/_bib_nsis && makensis installer.nsi
	cp /tmp/_bib_nsis/install-win.exe /home/j/Documents/DeV/Biblioteca/install-win.exe
	rm -rf /tmp/_bib_win_img /tmp/_bib_nsis
	@echo ""
	@echo "═══════════════════════════════════════════════"
	@echo "  Windows installer: install-win.exe"
	@echo "  Copy to Windows PC and run."
	@echo "  Installs to Program Files, desktop shortcut."
	@echo "═══════════════════════════════════════════════"

# ── Linux installer: fat-jar → RPM/DEB via jpackage ─────────────────────────
installer-linux: fat-jar icon
	@command -v jpackage >/dev/null 2>&1 || \
	    { echo "ERROR: jpackage not found. Install JDK 17+."; exit 1; }
	@rm -rf /tmp/_bib_pkg && mkdir -p /tmp/_bib_pkg
	@jpackage \
	    --input . \
	    --main-jar build/artifacts/biblioteca-fat.jar \
	    --main-class main.Executable \
	    --name Biblioteca \
	    --app-version 1.0 \
	    --description "Gestio Personal de Llibres" \
	    --icon packaging/icon-256.png \
	    --dest /tmp/_bib_pkg \
	    --type rpm \
	    --linux-menu-group "Office" \
	    --linux-shortcut 2>/dev/null || \
	jpackage \
	    --input . \
	    --main-jar build/artifacts/biblioteca-fat.jar \
	    --main-class main.Executable \
	    --name Biblioteca \
	    --app-version 1.0 \
	    --description "Gestio Personal de Llibres" \
	    --icon packaging/icon-256.png \
	    --dest /tmp/_bib_pkg \
	    --type deb \
	    --linux-menu-group "Office" \
	    --linux-shortcut
	@cp /tmp/_bib_pkg/*.rpm . 2>/dev/null || cp /tmp/_bib_pkg/*.deb . 2>/dev/null
	@echo "Linux installer built."

# ---------- STRINGS ---------- #

strings:
	@python3 scripts/gen_strings.py

# Comprova que els fitxers generats (I18n.java i els tres .properties)
# coincideixen amb strings/*.csv. És el mateix gate que executa la CI.
# Abans només es gatejaven els .properties: la taula Java — l'única que
# l'aplicació llegeix en temps d'execució — podia quedar obsoleta sense
# que la CI ho notés (veure docs/I18N-NEWLINES.md).
strings-check:
	@python3 scripts/gen_strings.py --check

# Proves de les invariants d'escapament i de llegenda del generador.
strings-test:
	@python3 scripts/test_gen_strings.py

# ---------- FUZZ ------------ #

# Les dependències de fuzz ja provenen del lock (etiqueta `fuzz`), així que
# el target només ha de reconstruir el shim de biblioteques natives.
fuzz-deps: deps $(FUZZ_SHIM_JAR)

# El shim conté les biblioteques natives del JAR de Jazzer: si la versió de
# Jazzer canvia, el shim s'ha de reconstruir (sinó es queda amb les .so
# de la versió anterior i l'agent no es pot instal·lar).
$(FUZZ_SHIM_JAR): $(JAZZER)
	@mkdir -p fuzz-shim
	@cd fuzz-shim && rm -rf META-INF com && \
	    for pair in \
	        jazzer_driver_linux_x86_64:libjazzer_driver.so \
	        jazzer_fuzzed_data_provider_linux_x86_64:libjazzer_fuzzed_data_provider.so \
	        jazzer_signal_handler_linux_x86_64:libjazzer_signal_handler.so ; do \
	        src=$$(echo $$pair | cut -d: -f1); dst=$$(echo $$pair | cut -d: -f2); \
	        target=com/code_intelligence/jazzer/driver/$$src; \
	        mkdir -p $$target; \
	        unzip -j -oq ../$(JAZZER) "com/code_intelligence/jazzer/driver/$$src/$$dst" -d $$target 2>/dev/null \
	            || echo "  WARN: $$src/$$dst not in $(JAZZER) (build/arch mismatch?)"; \
	    done && \
	    mkdir -p com/code_intelligence/jazzer/utils && \
	    unzip -j -oq ../$(JAZZER) 'com/code_intelligence/jazzer/utils/Log.class' \
	        -d com/code_intelligence/jazzer/utils 2>/dev/null \
	        || echo "  WARN: Log.class not in $(JAZZER)"; \
	    jar cf ../$(FUZZ_SHIM_JAR) .

# Property tests de jqwik. La classe es selecciona pel nom real del fitxer
# (fuzz/domini/TestPropietatsInvariantsLlibre.java): el nom antic
# fuzz.domini.LlibreInvariantPropertiesTest no existia i el target no
# executava cap test.
fuzz-property: compile fuzz-deps fuzz-test-compile
	@echo "=== jqwik: fuzz.domini.TestPropietatsInvariantsLlibre ==="
	@$(JAVA) $(BOOTCP) -Dbiblioteca.test=true \
	    -Dbiblioteca.h2.url="jdbc:h2:mem:fuzz_prop;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1" \
	    -jar $(JUNIT5_STANDALONE) execute \
	    --select-class=fuzz.domini.TestPropietatsInvariantsLlibre --details=tree \
	    --fail-if-no-tests \
	    --class-path bin:$(FUZZ_CP)

# Run the Jazzer harnesses. JAZZER_FUZZ=1 switches Jazzer from regression
# mode (one run with a seed) to coverage-guided fuzzing. The @FuzzTest
# annotation's maxDuration caps each harness.
#
# JAZZER_SEL=<fqn>: run a single harness by fully-qualified class name.
#                    Used by scripts/fuzz-nightly.sh to run both harnesses
#                    in parallel without them racing on .cifuzz-corpus/.
#                    If unset, runs every harness serially.
#
# La llista d'arnesos es deriva dels fitxers que importen
# com.code_intelligence.jazzer, no d'una llista escrita a mà: així no
# es pot quedar obsoleta en silenci.
fuzz-jazzer: compile fuzz-deps fuzz-test-compile
	@if [ -n "$(JARZER_SEL)" ]; then \
	    echo "=== Jazzer: $(JARZER_SEL) ==="; \
	    JAZZER_FUZZ=1 $(JAVA) $(BOOTCP) -Dbiblioteca.test=true \
	        -jar $(JUNIT5_STANDALONE) execute \
	        --select-class=$(JARZER_SEL) --details=tree \
	        --class-path bin:$(FUZZ_CP); \
	else \
	    for h in $(JAZZER_TEST_CLASSES); do \
	        echo "=== Jazzer: $$h ==="; \
	        JAZZER_FUZZ=1 $(JAVA) $(BOOTCP) -Dbiblioteca.test=true \
	            -jar $(JUNIT5_STANDALONE) execute \
	            --select-class=$$h --details=tree \
	            --class-path bin:$(FUZZ_CP) || exit $$?; \
	    done; \
	fi

# Compile test sources (including fuzz tests) into bin/. Used by the
# fuzz targets so they don't depend on `make test` having run first.
fuzz-test-compile: compile
	@find ./test/ -name '*.java' > $(TEST_LIST)
	@$(JAVAC) -g -cp bin:$(FUZZ_CP) @$(TEST_LIST) -d bin || { rm -f $(TEST_LIST); exit 1; }
	@rm -f $(TEST_LIST)

fuzz: fuzz-property fuzz-jazzer

# ---------- CLEAN ---------- #

clean:
	@rm -Rf bin/*
