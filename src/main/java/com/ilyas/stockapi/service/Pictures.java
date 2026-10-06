package com.ilyas.stockapi.service;

import com.ilyas.stockapi.exception.BadRequestException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Turns an uploaded picture into a clean JPEG of at most MAX_SIDE pixels.
 *
 * Only the pixels are kept and saved again, so whatever else the file held (the GPS position of
 * a phone photo, comments, anything that isn't an image) is gone. The size is read from the
 * file's header before decoding, and big photos are decoded at a lower resolution directly, so
 * a huge or fake picture can't use up the server's memory.
 */
final class Pictures {

    static final int MAX_SIDE = 1200;
    private static final long MAX_PIXELS = 100_000_000L;
    private static final float JPEG_QUALITY = 0.85f;

    static {
        // Decode in memory, without temporary files
        ImageIO.setUseCache(false);
    }

    private Pictures() {
    }

    record Jpeg(byte[] content, int width, int height) {
    }

    static Jpeg toJpeg(InputStream input) throws IOException {
        BufferedImage picture = scaleDown(read(input));
        return new Jpeg(encode(picture), picture.getWidth(), picture.getHeight());
    }

    private static BufferedImage read(InputStream input) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(input)) {
            Iterator<ImageReader> readers = (stream == null) ? null : ImageIO.getImageReaders(stream);
            if (readers == null || !readers.hasNext()) {
                throw notAPicture();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if ((long) width * height > MAX_PIXELS) {
                    throw new BadRequestException("The picture is too large (" + width + " x " + height + " pixels): resize it first");
                }
                ImageReadParam param = reader.getDefaultReadParam();
                int step = Math.max(1, Math.max(width, height) / (2 * MAX_SIDE));
                param.setSourceSubsampling(step, step, 0, 0);
                return reader.read(0, param);
            } catch (IOException | RuntimeException e) {
                if (e instanceof BadRequestException bad) {
                    throw bad;
                }
                throw notAPicture();
            } finally {
                reader.dispose();
            }
        }
    }

    // White background, so transparent parts of a PNG don't turn black in the JPEG
    private static BufferedImage scaleDown(BufferedImage source) {
        double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return target;
    }

    private static byte[] encode(BufferedImage picture) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.setOutput(output);
            writer.write(null, new IIOImage(picture, null, null), param);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    private static BadRequestException notAPicture() {
        return new BadRequestException("The file is not a picture the server can read: use a JPEG or PNG");
    }
}
