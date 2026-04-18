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
	@$(JAVAC) -g -cp bin:$(CP) $(shell find ./test/ -name "*.java") -d bin
	@java -cp bin:$(CP) BibliotecaTest

# ---------- CLEAN ---------- #

clean:
	@rm -Rf bin/*
