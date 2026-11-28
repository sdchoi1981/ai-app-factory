package factory.kit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;

public final class GifWriterTest {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int[] pixels = new int[64 * 64];
        try (GifWriter writer = new GifWriter(bytes, 64, 64)) {
            Arrays.fill(pixels, 0xffff0000);
            writer.frame(pixels, 50);
            Arrays.fill(pixels, 0xff0000ff);
            writer.frame(pixels, 3000);
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            try {
                reader.setInput(input);
                if (reader.getNumImages(true) != 2 || reader.getWidth(0) != 64 || reader.getHeight(0) != 64) {
                    throw new AssertionError("Frame count or dimensions");
                }
                checkFrame(reader, 0, 0xffff0000, "5");
                checkFrame(reader, 1, 0xff0000ff, "300");
                IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(0).getAsTree("javax_imageio_gif_image_1.0");
                IIOMetadataNode extension = (IIOMetadataNode) root.getElementsByTagName("ApplicationExtension").item(0);
                if (!"NETSCAPE".equals(extension.getAttribute("applicationID")) ||
                        !Arrays.equals((byte[]) extension.getUserObject(), new byte[]{1, 0, 0})) {
                    throw new AssertionError("GIF loop extension");
                }
            } finally {
                reader.dispose();
            }
        }
        System.out.println("GifKit: frame order, palette, looping and duration bounds decoded successfully");
    }

    private static void checkFrame(ImageReader reader, int index, int color, String delay) throws Exception {
        if (reader.read(index).getRGB(63, 63) != color) throw new AssertionError("Frame order or color");
        IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(index).getAsTree("javax_imageio_gif_image_1.0");
        IIOMetadataNode control = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
        if (!delay.equals(control.getAttribute("delayTime"))) throw new AssertionError("Frame delay");
    }
}
