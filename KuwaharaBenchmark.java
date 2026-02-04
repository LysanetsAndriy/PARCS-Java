import parcs.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class KuwaharaBenchmark {
    public static void main(String[] args) {
        // --- КОНФІГУРАЦІЯ ЕКСПЕРИМЕНТІВ ---
        int[] workerCounts = {1, 2, 4, 6, 8, 10};
        int[] radiuses = {5, 10, 20};
        String[] imageFiles = {"1280x720.jpg", "4000x2250.jpg", "4000x7000.jpg"};

        // Ініціалізація PARCS
        task curtask = new task();
        curtask.addJarFile("Kuwahara.jar"); 

        System.out.println("=================================================");
        System.out.println("STARTING BENCHMARK SUITE (54 Experiments)");
        System.out.println("=================================================");

        // ЦИКЛ 1: ПО ЗОБРАЖЕННЯХ
        for (String imgName : imageFiles) {
            System.out.println("\n>>> Processing Image: " + imgName);
            
            File f = new File(imgName);
            if (!f.exists()) {
                System.err.println("Error: File " + imgName + " not found! Skipping...");
                continue;
            }

            try {
                // Завантажуємо зображення один раз для набору тестів
                BufferedImage img = ImageIO.read(f);
                int w = img.getWidth();
                int h = img.getHeight();
                int[] allPixels = img.getRGB(0, 0, w, h, null, 0, w);

                // Створюємо CSV файл для звіту
                String csvFileName = "results_" + imgName.replace(".jpg", "") + ".csv";
                PrintWriter writer = new PrintWriter(new FileWriter(csvFileName));
                
                // Заголовок CSV
                writer.println("Radius,Workers,Time(sec),Speedup");
                
                // ЦИКЛ 2: ПО РАДІУСАХ (Складність)
                for (int r : radiuses) {
                    double timeT1 = 0.0; // Час виконання на 1 воркері (для розрахунку Speedup)

                    // ЦИКЛ 3: ПО КІЛЬКОСТІ ВОРКЕРІВ (Масштабованість)
                    for (int numWorkers : workerCounts) {
                        System.out.printf("Running: R=%-2d | Workers=%-2d ... ", r, numWorkers);
                        
                        // --- ЗАПУСК ТЕСТУ ---
                        long start = System.nanoTime();
                        
                        runSingleExperiment(curtask, allPixels, w, h, numWorkers, r);
                        
                        long end = System.nanoTime();
                        double duration = (end - start) / 1e9;
                        
                        // Розрахунок прискорення
                        if (numWorkers == 1) {
                            timeT1 = duration;
                        }
                        double speedup = (timeT1 > 0) ? timeT1 / duration : 0.0;

                        System.out.printf("Done in %.3fs | Speedup: %.2fx\n", duration, speedup);

                        // Запис у файл (одразу зберігаємо, щоб не втратити дані при збої)
                        // Формат: Radius, Workers, Time, Speedup
                        writer.printf("%d,%d,%.4f,%.2f\n", r, numWorkers, duration, speedup);
                        writer.flush();
                    }
                }
                writer.close();
                System.out.println("Saved results to " + csvFileName);

            } catch (Exception e) {
                System.err.println("Experiment failed for " + imgName);
                e.printStackTrace();
            }
        }
        
        System.out.println("\nAll benchmarks completed.");
        curtask.end();
    }

    /**
     * Логіка одного запуску.
     * Ми виконуємо повний цикл (розбиття -> відправка -> збір), 
     * щоб виміряти реальний час роботи системи.
     */
    public static void runSingleExperiment(task curtask, int[] allPixels, int w, int h, int numWorkers, int radius) {
        AMInfo info = new AMInfo(curtask, null);
        List<channel> channels = new ArrayList<>();
        int stripHeight = h / numWorkers;

        // 1. Розсилка задач
        for (int i = 0; i < numWorkers; i++) {
            int startY = i * stripHeight;
            int endY = (i == numWorkers - 1) ? h : (i + 1) * stripHeight;
            
            // Padding
            int paddedStartY = Math.max(0, startY - radius);
            int paddedEndY = Math.min(h, endY + radius);
            int chunkH = paddedEndY - paddedStartY;
            
            int[] chunkPixels = new int[w * chunkH];
            System.arraycopy(allPixels, paddedStartY * w, chunkPixels, 0, w * chunkH);

            point p = info.createPoint();
            channel c = p.createChannel();
            p.execute("Kuwahara");
            
            c.write(new ImageChunk(chunkPixels, w, chunkH, radius));
            channels.add(c);
        }

        // 2. Збір результатів (обов'язково чекаємо відповіді!)
        // Ми не записуємо результат у BufferedImage, щоб зекономити трохи пам'яті в JVM,
        // але ми ОБОВ'ЯЗКОВО читаємо дані з мережі, інакше тест буде некоректним.
        for (int i = 0; i < numWorkers; i++) {
            try {
                // Блокуюче читання - чекаємо поки воркер закінчить
                channels.get(i).readObject(); 
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
