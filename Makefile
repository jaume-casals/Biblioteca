# ---------- RUN & COMPILE ---------- #

JAVAC := $(shell which javac 2>/dev/null || find /usr/lib/jvm -name javac 2>/dev/null | head -1)
ifeq ($(JAVAC),)
    JAVAC := /usr/lib/jvm/java-21-openjdk/bin/javac
endif

CP := lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:.

compile:
	@if [ ! -f "$(JAVAC)" ]; then \
		echo "ERROR: javac not found. Install the JDK:"; \
		echo "  sudo dnf install java-21-openjdk-devel"; \
		exit 1; \
	fi
	@find ./src/ -name "*.java" > classes.txt
	@$(JAVAC) -g -cp $(CP) @classes.txt -d bin
	@rm classes.txt

run:
	make clean
	make compile
	java -cp bin:$(CP) main.Ejecutable

run-only:
	@if [ ! -f "bin/main/Ejecutable.class" ]; then \
		echo "ERROR: Classes not found. Run: make compile"; \
		exit 1; \
	fi
	java -cp bin:$(CP) main.Ejecutable

# ---------- TEST ----------- #

test: compile
	@find ./test/ -name "*.java" > test_classes.txt
	@$(JAVAC) -g -cp bin:$(CP) @test_classes.txt -d bin
	@rm test_classes.txt
	@java -cp bin:$(CP) test.BibliotecaTest

# ---------- POPULATE ------- #
# Fetches books from OpenLibrary and inserts them.
# Usage: make populate          (default 2000 books)
#        make populate MAX=500  (custom limit)

MAX ?= 2000

populate: compile
	@find ./test/ -name "*.java" > test_classes.txt
	@$(JAVAC) -g -cp bin:$(CP) @test_classes.txt -d bin
	@rm test_classes.txt
	@java -cp bin:$(CP) -Dbiblioteca.root=$(shell pwd) test.PopulateDB $(MAX)

# ---------- INSTALLERS ----- #
# Builds biblioteca.jar, install.sh (Linux) and install.exe (Windows SFX).
# Requires: 7-zip, and 7zSD/7z.sfx extracted from the 7-zip Windows installer.
# Usage: make installers SFX=/path/to/7z.sfx
#        (default SFX path: /tmp/7zsfx/7z.sfx)

SFX ?= /tmp/7zsfx/7z.sfx

jar: compile
	@jar cfm biblioteca.jar <(printf 'Manifest-Version: 1.0\nMain-Class: main.Ejecutable\nClass-Path: lib/h2-2.3.232.jar lib/mariadb-java-client-3.3.3.jar\n\n') -C bin .
	@echo "Built biblioteca.jar"

installers: jar
	@# Linux installer: just needs the script (ships alongside biblioteca.jar + lib/)
	@chmod +x install.sh
	@echo "install.sh ready"
	@# Windows installer: stage files → 7z archive → SFX EXE
	@rm -rf /tmp/_bib_installer && mkdir -p /tmp/_bib_installer/lib
	@cp biblioteca.jar /tmp/_bib_installer/
	@cp lib/*.jar /tmp/_bib_installer/lib/
	@cp /tmp/biblioteca-setup.bat /tmp/_bib_installer/setup.bat 2>/dev/null || \
	    echo "WARNING: setup.bat not found at /tmp/biblioteca-setup.bat"
	@7z a -t7z -mx=9 /tmp/_bib_payload.7z /tmp/_bib_installer/ >/dev/null
	@printf ';!@Install@!UTF-8!\nTitle="Biblioteca Installer"\nBeginPrompt="Install Biblioteca on this computer?"\nRunProgram="cmd /C setup.bat"\n;!@InstallEnd@!\n' > /tmp/_bib_sfx.cfg
	@cat "$(SFX)" /tmp/_bib_sfx.cfg /tmp/_bib_payload.7z > install.exe
	@echo "Built install.exe ($(shell du -sh install.exe | cut -f1))"

# ---------- CLEAN ---------- #

clean:
	@rm -Rf bin/*
