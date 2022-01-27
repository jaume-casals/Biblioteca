# ---------- RUN & COMPILE ---------- #

compile:
	@find ./src/ -name "*.java" > classes.txt
	@javac -g @classes.txt -d bin
	@rm classes.txt

run:
	make clean
	make compile
	java -cp bin main.Ejecutable

# ---------- CLEAN ---------- #

clean:
	@rm -Rf bin/*

