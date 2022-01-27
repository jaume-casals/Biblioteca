# ---------- RUN & COMPILE ---------- #

compile:
	@find ./src/ -name "*.java" > classes.txt
	@javac -g -cp src/jar/mysql-connector-java-8.0.23.jar:. @classes.txt -d bin
	@rm classes.txt

run:
	make clean
	make compile
	java -cp bin:src/jar/mysql-connector-java-8.0.23.jar:. main.Ejecutable

# ---------- CLEAN ---------- #

clean:
	@rm -Rf bin/*

