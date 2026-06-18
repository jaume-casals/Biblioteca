# ---------- RUN & COMPILE ---------- #

# Multiple targets share the same `bin/` output (compile, fuzz-test-compile,
# populate, test). They must run serially to avoid javac-on-bin races and
# the resulting "rm: cannot remove classes.txt" / corrupted class files.
.NOTPARALLEL:

JAVAC := $(shell which javac 2>/dev/null || find /usr/lib/jvm -name javac 2>/dev/null | head -1)
ifeq ($(JAVAC),)
    JAVAC := /usr/lib/jvm/java-21-openjdk/bin/javac
endif

CP := lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar:.
TEST_CP := $(CP):lib/apiguardian-api-1.1.2.jar:lib/junit-jupiter-api-5.11.4.jar:lib/junit-jupiter-params-5.11.4.jar:lib/junit-jupiter-engine-5.11.4.jar:lib/junit-platform-launcher-1.11.4.jar:lib/junit-platform-engine-1.11.4.jar:lib/junit-platform-commons-1.11.4.jar:lib/opentest4j-1.3.0.jar:lib/assertj-core-3.26.3.jar:lib/mockito-core-5.14.2.jar:lib/mockito-junit-jupiter-5.14.2.jar:lib/byte-buddy-1.15.4.jar:lib/byte-buddy-agent-1.15.4.jar:lib/objenesis-3.3.jar

APIGUARDIAN_JAR := lib/apiguardian-api-1.1.2.jar
APIGUARDIAN_URL := https://repo1.maven.org/maven2/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar

compile:
	@if [ ! -f "$(JAVAC)" ]; then \
		echo "ERROR: javac not found. Install the JDK:"; \
		echo "  sudo dnf install java-21-openjdk-devel"; \
		exit 1; \
	fi
	@find ./src/ -name "*.java" > classes.txt
	@$(JAVAC) -g --release 21 -cp $(CP) @classes.txt -d bin
	@rm -f classes.txt
	@if [ -d src/herramienta ]; then cp src/herramienta/*.properties bin/herramienta/ 2>/dev/null || true; fi
	@if [ -d src/web ]; then cp -r src/web bin/web; fi
	@if [ -f src/LICENSE ]; then cp src/LICENSE bin/; fi

run:
	make clean
	make compile
	java -cp bin:$(CP) main.Executable

run-only:
	@if [ ! -f "bin/main/Executable.class" ]; then \
		echo "ERROR: Classes not found. Run: make compile"; \
		exit 1; \
	fi
	java -cp bin:$(CP) main.Executable

# ---------- TEST ----------- #

JUNIT5_STANDALONE := lib/junit-platform-console-standalone-1.11.4.jar

# ---------- FUZZ DEPS ----------

# jqwik 1.9.0 (property-based, JUnit 5)
JQWIK_API := lib/jqwik-api-1.9.0.jar
JQWIK_ENGINE := lib/jqwik-engine-1.9.0.jar
# Jazzer 0.24.0 (coverage-guided libFuzzer for the JVM)
# jazzer-junit-launcher was renamed to jazzer in 0.x — Maven Central
# only ships jazzer, jazzer-api, and jazzer-junit. The agent attaches
# dynamically via ByteBuddyAgent when JAZZER_FUZZ=1 is set.
JAZZER := lib/jazzer-0.24.0.jar
JAZZER_API := lib/jazzer-api-0.24.0.jar
JAZZER_JUNIT := lib/jazzer-junit-0.24.0.jar

# Fuzz jars are added to the test classpath so `make test` compiles the
# jqwik fuzz tests. The Jazzer @FuzzTest tests are excluded from
# `make test` (see the per-class JUnit 5 loop below) because Jazzer
# 0.24.0's agent install requires a working native-lib setup that
# conflicts with the regression-mode JUnit 5 run; they are run via
# `make fuzz-jazzer` instead.
TEST_CP := $(TEST_CP):$(JQWIK_API):$(JQWIK_ENGINE):$(JAZZER_API):$(JAZZER_JUNIT)
FUZZ_CP := $(TEST_CP):$(JAZZER):$(JAZZER_API):$(JAZZER_JUNIT)
# Jazzer 0.24.0 has a packaging inconsistency: the native libs in
# jazzer-0.24.0.jar live at com/.../driver/jazzer_driver_<os>_<arch>/
# but the bootstrap jar's RulesJni searches at
# com/.../driver/libjazzer_driver_<os>_<arch>/. The shim jar
# $(FUZZ_SHIM_JAR) duplicates the .so files at the expected path and
# must be added to the bootstrap classpath via -Xbootclasspath/a: at
# JVM startup (the bootstrap RulesJni is loaded by the bootstrap
# classloader at runtime, and on Java 21 Class.getResource on an
# unnamed-module bootstrap class delegates to
# ClassLoader.getBootstrapResource, which only searches the
# bootclasspath — NOT the application classpath).
FUZZ_SHIM_JAR := fuzz-shim/jazzer-native-shim.jar
BOOTCP := -Xbootclasspath/a:$(FUZZ_SHIM_JAR)

test-deps:
	@if [ ! -f "$(APIGUARDIAN_JAR)" ]; then \
		echo "Fetching $(APIGUARDIAN_JAR)..."; \
		mkdir -p lib; \
		curl -fsSL -o "$(APIGUARDIAN_JAR)" "$(APIGUARDIAN_URL)" \
		|| wget -q -O "$(APIGUARDIAN_JAR)" "$(APIGUARDIAN_URL)"; \
	fi

test: compile test-deps fuzz-deps
	@if [ -f scripts/patch_tests_after_web_removal.py ]; then \
		python3 scripts/patch_tests_after_web_removal.py 2>/dev/null \
		|| python scripts/patch_tests_after_web_removal.py; \
	fi
	@find ./test/ -name "*.java" > test_classes.txt
	@$(JAVAC) -g -cp bin:$(TEST_CP) @test_classes.txt -d bin
	@rm test_classes.txt
	@java -cp bin:$(TEST_CP) BibliotecaTest
	@DB_COUNTER=0; for cls in $$(find ./test/ -name '*Test.java' \
	    | grep -v '/BibliotecaTest\.java$$' \
	    | grep -v '/fuzz/' \
	    | sed -e 's|^\./test/||' -e 's|\.java$$||' -e 's|/|.|g'); do \
	    DB_COUNTER=$$((DB_COUNTER+1)); \
	    echo "=== Running $$cls (JUnit 5) ==="; \
	    java $(BOOTCP) -Dbiblioteca.test=true \
	        -Dbiblioteca.h2.url="jdbc:h2:mem:dyn_$$DB_COUNTER;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1" \
	        -jar $(JUNIT5_STANDALONE) execute \
	        --select-class=$$cls --details=summary \
	        --classpath bin:$(TEST_CP); \
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
	@find ./test/ -name "*.java" > test_classes.txt
	@$(JAVAC) -g -cp bin:$(TEST_CP) @test_classes.txt -d bin
	@rm test_classes.txt
	@java -cp bin:$(TEST_CP) -Dbiblioteca.root=$(shell pwd) test.PopulateDB $(MAX)

# ---------- INSTALLERS ----- #
#
# make jar               → biblioteca.jar (slim, needs lib/ alongside)
# make fat-jar           → biblioteca-fat.jar (all deps embedded)
# make icon              → packaging/Biblioteca.ico (needs: ImageMagick)
# make installer-windows → install.exe   (needs: Launch4j + makensis)
# make installer-linux   → *.rpm / *.deb (needs: jpackage from JDK 17+)
#
# Launch4j (cross-compile JAR → Windows EXE):
#   Unpack to /opt/launch4j or set LAUNCH4J=/path/to/launch4j
#   Download: https://launch4j.sourceforge.net/
#
# NSIS (Windows installer builder, runs on Linux):
#   sudo dnf install mingw32-nsis   OR   sudo apt install nsis

LAUNCH4J ?= $(shell \
    if   command -v launch4j       >/dev/null 2>&1; then echo "launch4j"; \
    elif [ -x /opt/launch4j/launch4j ]; then echo "/opt/launch4j/launch4j"; \
    elif [ -f /opt/launch4j/launch4jc.jar ]; then echo "java -jar /opt/launch4j/launch4jc.jar"; \
    fi)

# Pick a JDK whose bin/java actually exists (some distros leave stub dirs).
LAUNCH4J_JAVA_HOME := $(shell \
    for D in /usr/lib/jvm/temurin-21-jdk \
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
	@jar cfm biblioteca.jar <(printf 'Manifest-Version: 1.0\nMain-Class: main.Executable\nClass-Path: lib/h2-2.3.232.jar lib/mariadb-java-client-3.3.3.jar lib/gson-2.11.0.jar lib/javalin-6.3.0.jar lib/kotlin-stdlib-2.0.21.jar\n\n') -C bin .
	@echo "Built biblioteca.jar"

# ── fat JAR (all deps bundled, standalone) ────────────────────────────────────
fat-jar: compile
	@echo "Building fat JAR (bundling all dependencies)..."
	@rm -rf /tmp/_bib_fat && mkdir -p /tmp/_bib_fat
	@cd /tmp/_bib_fat && for J in \
	    h2-2.3.232.jar mariadb-java-client-3.3.3.jar gson-2.11.0.jar \
	    javalin-6.3.0.jar kotlin-stdlib-2.0.21.jar; do \
	    [ -f "$(CURDIR)/lib/$$J" ] && unzip -oq "$(CURDIR)/lib/$$J" 2>/dev/null || true; \
	done
	@cp -r $(CURDIR)/bin/. /tmp/_bib_fat/
	@# Remove JAR signature files — they break fat JARs
	@rm -f /tmp/_bib_fat/META-INF/*.SF /tmp/_bib_fat/META-INF/*.DSA \
	       /tmp/_bib_fat/META-INF/*.RSA /tmp/_bib_fat/META-INF/*.EC
	@cd /tmp/_bib_fat && jar cfe $(CURDIR)/biblioteca-fat.jar main.Executable .
	@rm -rf /tmp/_bib_fat
	@echo "Built biblioteca-fat.jar ($$(du -sh $(CURDIR)/biblioteca-fat.jar | cut -f1))"

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
	@echo "  install.exe ready  ($$(du -sh install.exe | cut -f1))"
	@echo "═══════════════════════════════════════════════"

# ── Standalone Windows .exe with bundled JRE via jpackage ──────────────────────
jpackage-win: fat-jar icon
	@command -v jpackage >/dev/null 2>&1 || \
	    { echo "ERROR: jpackage not found. Install JDK 17+."; exit 1; }
	@rm -rf /tmp/_bib_win && mkdir -p /tmp/_bib_win
	@jpackage \
	    --type app-image \
	    --input . \
	    --main-jar biblioteca-fat.jar \
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
	    --main-jar biblioteca-fat.jar \
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
	    --main-jar biblioteca-fat.jar \
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
	    --main-jar biblioteca-fat.jar \
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

# ---------- FUZZ ------------ #

# Downloads the 5 fuzz jars from Maven Central. Idempotent — skips
# jars already on disk. The fuzz jars are .gitignored.
# Also rebuilds the $(FUZZ_SHIM_JAR) jar containing the Jazzer
# native libs at the resource path RulesJni expects (see the FUZZ_SHIM
# note above). The shim jar is added to the bootstrap classpath via
# -Xbootclasspath/a:.
fuzz-deps: $(FUZZ_SHIM_JAR)
	@mkdir -p lib
	@for entry in \
	    jqwik-api:1.9.0:net/jqwik \
	    jqwik-engine:1.9.0:net/jqwik \
	    jazzer:0.24.0:com/code-intelligence \
	    jazzer-api:0.24.0:com/code-intelligence \
	    jazzer-junit:0.24.0:com/code-intelligence ; do \
	    art=$$(echo $$entry | cut -d: -f1); ver=$$(echo $$entry | cut -d: -f2); gp=$$(echo $$entry | cut -d: -f3); \
	    jar=lib/$$art-$$ver.jar; \
	    if [ ! -f $$jar ]; then \
	        curl -fsSL -o $$jar https://repo1.maven.org/maven2/$$gp/$$art/$$ver/$$art-$$ver.jar \
	        || wget -q -O $$jar https://repo1.maven.org/maven2/$$gp/$$art/$$ver/$$art-$$ver.jar; \
	        echo "  fetched $$jar"; \
	    fi; \
	done

$(FUZZ_SHIM_JAR):
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

# Run the jqwik property tests for the domini layer. Per-test try counts
# are encoded in the test class; `make test` also runs them but in
# regression-only mode (one try per property). This target uses the
# dedicated H2 instance name `fuzz_prop` so the in-memory DB is shared
# across all 5 properties within this run.
fuzz-property: compile fuzz-deps fuzz-test-compile
	@echo "=== jqwik: fuzz.domini.LlibreInvariantPropertiesTest ==="
	@java $(BOOTCP) -Dbiblioteca.test=true \
	    -Dbiblioteca.h2.url="jdbc:h2:mem:fuzz_prop;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1" \
	    -jar $(JUNIT5_STANDALONE) execute \
	    --select-class=fuzz.domini.LlibreInvariantPropertiesTest --details=tree \
	    --classpath bin:$(FUZZ_CP)

# Run the Jazzer harnesses. JAZZER_FUZZ=1 switches Jazzer from regression
# mode (one run with a seed) to coverage-guided fuzzing. The @FuzzTest
# annotation's maxDuration caps each harness.
#
# JAZZER_SEL=<fqn>: run a single harness by fully-qualified class name.
#                    Used by scripts/fuzz-nightly.sh to run both harnesses
#                    in parallel without them racing on .cifuzz-corpus/.
#                    If unset, runs every harness serially.
fuzz-jazzer: compile fuzz-deps fuzz-test-compile
	@if [ -n "$(JARZER_SEL)" ]; then \
	    echo "=== Jazzer: $(JARZER_SEL) ==="; \
	    JAZZER_FUZZ=1 java $(BOOTCP) -Dbiblioteca.test=true \
	        -jar $(JUNIT5_STANDALONE) execute \
	        --select-class=$(JARZER_SEL) --details=tree \
	        --classpath bin:$(FUZZ_CP); \
	else \
	    for h in fuzz.herramienta.Rfc4180FuzzTest \
	             fuzz.herramienta.CsvUtilsFuzzTest \
	             fuzz.domini.LlibreFilterBuilderFuzzTest \
	             fuzz.domini.SortSpecFuzzTest \
	             fuzz.domini.ShelfParserFuzzTest; do \
	        echo "=== Jazzer: $$h ==="; \
	        JAZZER_FUZZ=1 java $(BOOTCP) -Dbiblioteca.test=true \
	            -jar $(JUNIT5_STANDALONE) execute \
	            --select-class=$$h --details=tree \
	            --classpath bin:$(FUZZ_CP) || exit $$?; \
	    done; \
	fi

# Compile test sources (including fuzz tests) into bin/. Used by the
# fuzz targets so they don't depend on `make test` having run first.
fuzz-test-compile:
	@find ./test/ -name '*.java' > test_classes.txt
	@$(JAVAC) -g -cp bin:$(TEST_CP) @test_classes.txt -d bin
	@rm -f test_classes.txt

fuzz: fuzz-property fuzz-jazzer

# ---------- CLEAN ---------- #

clean:
	@rm -Rf bin/*
