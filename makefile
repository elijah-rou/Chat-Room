JC = javac
J = java
P = python3
JAVA_SRC = $(wildcard java-src/*.java)
JAVA_DIR = java-src
BIN = bin
PY_SRC = py-src

default:
	@- make clean
	$(JC) $(JAVA_SRC)
	find $(JAVA_DIR) -name "*.class" -exec mv -i {} bin \;

java-server:
	@make
	$(J) -cp $(BIN) ChatServer

java-client:
	@make
	$(J) -cp $(BIN) ChatClient

py-server:
	$(P) $(PY_SRC)/chatServer.py

py-client:
	$(P) $(PY_SRC)/chatClient.py

clean:
	rm bin/*.class