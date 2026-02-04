import parcs.*;

public class Kuwahara implements AM {
    public void run(AMInfo info) {
        try {
            // Отримуємо дані
            ImageChunk chunk = (ImageChunk) info.parent.readObject();
            int w = chunk.width;
            int h = chunk.height;
            int r = chunk.radius;
            int[] src = chunk.pixels;
            
            // Масив для результату.
            // Ми не можемо обробити самі краї (padding), тому вони залишаться чорними,
            // але Мастер це врахує при склейці.
            int[] dest = new int[src.length];

            // Проходимо по пікселях, відступаючи на r від країв (щоб вікно влізло)
            for (int y = r; y < h - r; y++) {
                for (int x = r; x < w - r; x++) {
                    
                    // Змінні для збереження найкращого результату
                    float minVariance = Float.MAX_VALUE;
                    int bestColor = 0;

                    // Перевіряємо 4 квадранти
                    // 0: top-left, 1: top-right, 2: bottom-left, 3: bottom-right
                    for (int k = 0; k < 4; k++) {
                        float rSum = 0, gSum = 0, bSum = 0;
                        float rSqSum = 0, gSqSum = 0, bSqSum = 0;
                        int count = 0;

                        // Визначаємо межі квадранта залежно від k
                        int startY = (k >= 2) ? 0 : -r; // 0,1 -> верх (-r..0)
                        int endY   = (k >= 2) ? r : 0;  // 2,3 -> низ (0..r)
                        int startX = (k % 2 == 0) ? -r : 0; // 0,2 -> ліво (-r..0)
                        int endX   = (k % 2 == 0) ? 0 : r;  // 1,3 -> право (0..r)

                        // Проходимо по пікселях квадранта
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

                        // Середні значення
                        float rMean = rSum / count;
                        float gMean = gSum / count;
                        float bMean = bSum / count;

                        // Дисперсія (Variance) для кожного каналу
                        // Var = (SumSq / N) - Mean^2
                        float rVar = (rSqSum / count) - (rMean * rMean);
                        float gVar = (gSqSum / count) - (gMean * gMean);
                        float bVar = (bSqSum / count) - (bMean * bMean);

                        // Загальна дисперсія сектора
                        float totalVariance = rVar + gVar + bVar;

                        // Якщо знайшли більш однорідний сектор - запам'ятовуємо його
                        if (totalVariance < minVariance) {
                            minVariance = totalVariance;
                            bestColor = (0xFF << 24) | ((int)rMean << 16) | ((int)gMean << 8) | (int)bMean;
                        }
                    }
                    
                    // Записуємо колір переможця
                    dest[y * w + x] = bestColor;
                }
            }

            // Відправляємо результат назад
            info.parent.write(new ImageChunk(dest, w, h, r));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
