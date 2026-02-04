import java.io.Serializable;

public class ImageChunk implements Serializable {
    public int[] pixels;
    public int width;
    public int height;
    public int radius; 

    public ImageChunk(int[] pixels, int width, int height, int radius) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
        this.radius = radius;
    }
}
