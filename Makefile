all: run

clean:
	rm -f out/KuwaharaMaster.jar out/Kuwahara.jar out/KuwaharaBenchmark.jar

# --- Основна програма ---
out/KuwaharaMaster.jar: out/parcs.jar src/KuwaharaMaster.java src/ImageChunk.java
	@javac -cp out/parcs.jar src/KuwaharaMaster.java src/ImageChunk.java
	@jar cf out/KuwaharaMaster.jar -C src KuwaharaMaster.class -C src ImageChunk.class
	@rm -f src/KuwaharaMaster.class src/ImageChunk.class

# --- Воркер (Спільний для обох) ---
out/Kuwahara.jar: out/parcs.jar src/Kuwahara.java src/ImageChunk.java
	@javac -cp out/parcs.jar src/Kuwahara.java src/ImageChunk.java
	@jar cf out/Kuwahara.jar -C src Kuwahara.class -C src ImageChunk.class
	@rm -f src/Kuwahara.class src/ImageChunk.class

# --- Бенчмарк (Тести) ---
out/KuwaharaBenchmark.jar: out/parcs.jar src/KuwaharaBenchmark.java src/ImageChunk.java
	@javac -cp out/parcs.jar src/KuwaharaBenchmark.java src/ImageChunk.java
	@jar cf out/KuwaharaBenchmark.jar -C src KuwaharaBenchmark.class -C src ImageChunk.class
	@rm -f src/KuwaharaBenchmark.class src/ImageChunk.class

# --- Команди запуску ---

build: out/KuwaharaMaster.jar out/Kuwahara.jar out/KuwaharaBenchmark.jar

# Запуск звичайної програми (одна картинка)
run: out/KuwaharaMaster.jar out/Kuwahara.jar
	@cd out && java -cp 'parcs.jar:KuwaharaMaster.jar' KuwaharaMaster

# Запуск тестів (54 експерименти)
bench: out/KuwaharaBenchmark.jar out/Kuwahara.jar
	@cd out && java -cp 'parcs.jar:KuwaharaBenchmark.jar' KuwaharaBenchmark
