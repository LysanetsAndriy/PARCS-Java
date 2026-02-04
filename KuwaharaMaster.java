import parcs.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class KuwaharaMaster {
    public static void main(String[] args) throws Exception {
        task curtask = new task();
        curtask.addJarFile("Kuwahara.jar");

        // --- НАЛАШТУВАННЯ ---
        String inputFileName = "download2_1.png";
        int radius = 10; 
        int numWorkers = 10; 

        File f = new File(inputFileName);
        if (!f.exists()) {
            System.out.println("Error: File " + f.getAbsolutePath() + " not found!");
            return;
        }
        BufferedImage img = ImageIO.read(f);
        int w = img.getWidth();
        int h = img.getHeight();
        int[] allPixels = img.getRGB(0, 0, w, h, null, 0, w);
        
        System.out.println("Processing image " + w + "x" + h + " with radius " + radius);
        System.out.println("--------------------------------------------------");

        // ==========================================
        // ЧАСТИНА 1: ПАРАЛЕЛЬНИЙ ЗАПУСК (PARCS)
        // ==========================================
        System.out.println("Starting PARALLEL execution on " + numWorkers + " workers...");
        AMInfo info = new AMInfo(curtask, null);
        List<channel> channels = new ArrayList<>();
        long startPar = System.nanoTime();

        int stripHeight = h / numWorkers; 

        for (int i = 0; i < numWorkers; i++) {
            int startY = i * stripHeight;
            int endY = (i == numWorkers - 1) ? h : (i + 1) * stripHeight;
            
            // Padding для перекриття
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

        BufferedImage resImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        
        for (int i = 0; i < numWorkers; i++) {
            ImageChunk res = (ImageChunk) channels.get(i).readObject();
            int startY = i * stripHeight;
            int endY = (i == numWorkers - 1) ? h : (i + 1) * stripHeight;
            int localOffset = (i == 0) ? 0 : radius; 
            int rowsToTake = endY - startY;

            resImg.setRGB(0, startY, w, rowsToTake, res.pixels, localOffset * w, w);
        }

        long endPar = System.nanoTime();
        double timePar = (endPar - startPar) / 1e9;
        System.out.println("Parallel Time: " + timePar + " sec");
        
        ImageIO.write(resImg, "jpg", new File("output_parallel.jpg"));


        // ==========================================
        // ЧАСТИНА 2: ПОСЛІДОВНИЙ ЗАПУСК (На Master)
        // ==========================================
        System.out.println("--------------------------------------------------");
        System.out.println("Starting SEQUENTIAL execution (single thread)...");
        
        long startSeq = System.nanoTime();
        
        // Викликаємо метод обробки прямо тут
        int[] seqResult = sequentialAlgo(allPixels, w, h, radius);
        
        long endSeq = System.nanoTime();
        double timeSeq = (endSeq - startSeq) / 1e9;
        System.out.println("Sequential Time: " + timeSeq + " sec");

        // Зберігаємо для перевірки
        BufferedImage seqImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        seqImg.setRGB(0, 0, w, h, seqResult, 0, w);
        ImageIO.write(seqImg, "jpg", new File("output_sequential.jpg"));

        // ==========================================
        // ПІДСУМКИ
        // ==========================================
        System.out.println("--------------------------------------------------");
        System.out.printf("Speedup: %.2fx\n", timeSeq / timePar);
        
        curtask.end();
    }

    // Той самий алгоритм, що і в Kuwahara.java, але для локального запуску
    public static int[] sequentialAlgo(int[] src, int w, int h, int r) {
        int[] dest = new int[src.length];

        for (int y = r; y < h - r; y++) {
            for (int x = r; x < w - r; x++) {
                
                float minVariance = Float.MAX_VALUE;
                int bestColor = 0;

                for (int k = 0; k < 4; k++) {
                    float rSum = 0, gSum = 0, bSum = 0;
                    float rSqSum = 0, gSqSum = 0, bSqSum = 0;
                    int count = 0;

                    int startY = (k >= 2) ? 0 : -r;
                    int endY   = (k >= 2) ? r : 0; 
                    int startX = (k % 2 == 0) ? -r : 0;
                    int endX   = (k % 2 == 0) ? 0 : r;

                    for (int dy = startY; dy <= endY; dy++) {
                        for (int dx = startX; dx <= endX; dx++) {
                            int px = x + dx;
                            int py = y + dy;
                            
                            int rgb = src[py * w + px];
                            int red = (rgb >> 16) & 0xFF;
                            int green = (rgb >> 8) & 0xFF;
                            int blue = rgb & 0xFF;

                            rSum += red;
                            gSum += green;
                            bSum += blue;

                            rSqSum += red * red;
                            gSqSum += green * green;
                            bSqSum += blue * blue;
                            count++;
                        }
                    }

                    float rMean = rSum / count;
                    float gMean = gSum / count;
                    float bMean = bSum / count;

                    float rVar = (rSqSum / count) - (rMean * rMean);
                    float gVar = (gSqSum / count) - (gMean * gMean);
                    float bVar = (bSqSum / count) - (bMean * bMean);

                    float totalVariance = rVar + gVar + bVar;

                    if (totalVariance < minVariance) {
                        minVariance = totalVariance;
                        bestColor = (0xFF << 24) | ((int)rMean << 16) | ((int)gMean << 8) | (int)bMean;
                    }
                }
                dest[y * w + x] = bestColor;
            }
        }
        return dest;
    }
}
