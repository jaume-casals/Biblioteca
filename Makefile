# ---------- RUN & COMPILE ---------- #

# Find javac in common locations
JAVAC := $(shell which javac 2>/dev/null || find /usr/lib/jvm -name javac 2>/dev/null | head -1)
ifeq ($(JAVAC),)
    JAVAC := /usr/lib/jvm/java-21-openjdk/bin/javac
endif

compile:
	@if [ ! -f "$(JAVAC)" ]; then \
		echo "ERROR: javac not found. Please install the Java Development Kit (JDK):"; \
		echo "  sudo dnf install java-21-openjdk-devel"; \
		echo ""; \
		echo "Alternatively, if you already have the JDK installed, set JAVA_HOME:"; \
		echo "  export JAVA_HOME=/usr/lib/jvm/java-XX-openjdk"; \
		exit 1; \
	fi
	@find ./src/ -name "*.java" > classes.txt
	@$(JAVAC) -g -cp jar/mariadb-java-client-3.3.3.jar:. @classes.txt -d bin
	@rm classes.txt

run:
	make clean
	make compile
	java -cp bin:jar/mariadb-java-client-3.3.3.jar:. main.Ejecutable

# Run without recompiling (if classes already exist)
run-only:
	@if [ ! -f "bin/main/Ejecutable.class" ]; then \
		echo "ERROR: Classes not found. Please compile first: make compile"; \
		exit 1; \
	fi
	java -cp bin:jar/mariadb-java-client-3.3.3.jar:. main.Ejecutable

# ---------- TEST ----------- #

test: compile
	@$(JAVAC) -g -cp bin:jar/mariadb-java-client-3.3.3.jar $(shell find ./test/ -name "*.java") -d bin
	@java -cp bin:jar/mariadb-java-client-3.3.3.jar BibliotecaTest

# ---------- CLEAN ---------- #

clean:
	@rm -Rf bin/*

