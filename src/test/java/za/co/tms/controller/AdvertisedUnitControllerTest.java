package za.co.tms.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import za.co.tms.domain.GalleryImage;
import za.co.tms.service.GalleryImageService;
import za.co.tms.service.RoomService;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class AdvertisedUnitControllerTest {

    private static final int SOURCE_WIDTH = 800;
    private static final int SOURCE_HEIGHT = 600;

    @Mock
    private RoomService roomService;

    @Mock
    private GalleryImageService galleryImageService;

    @InjectMocks
    private AdvertisedUnitController controller;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testServeUnitImage_noWidthReturnsOriginalBytes() throws IOException {
        byte[] original = sourceJpeg();
        stubImage(original, "image/jpeg");

        ResponseEntity<byte[]> response = controller.serveUnitImage(1L, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(original, response.getBody());
    }

    @Test
    public void testServeUnitImage_smallerWidthDownscalesPreservingAspectRatio() throws IOException {
        byte[] original = sourceJpeg();
        stubImage(original, "image/jpeg");

        ResponseEntity<byte[]> response = controller.serveUnitImage(2L, 400);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        BufferedImage resized = ImageIO.read(new ByteArrayInputStream(response.getBody()));
        assertNotNull(resized, "Resized body should be a decodable image");
        assertTrue(resized.getWidth() <= 400,
                "Resized width should be <= requested width, was " + resized.getWidth());
        assertEquals(400, resized.getWidth(), "Aspect-ratio downscale should hit the target width");
    }

    @Test
    public void testServeUnitImage_oversizedWidthReturnsOriginal() throws IOException {
        byte[] original = sourceJpeg();
        stubImage(original, "image/jpeg");

        ResponseEntity<byte[]> response = controller.serveUnitImage(3L, 2000);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(original, response.getBody());
    }

    @Test
    public void testServeUnitImage_zeroAndNegativeWidthReturnOriginal() throws IOException {
        byte[] original = sourceJpeg();
        stubImage(original, "image/jpeg");

        assertArrayEquals(original, controller.serveUnitImage(4L, 0).getBody());
        assertArrayEquals(original, controller.serveUnitImage(5L, -100).getBody());
    }

    // --- EXIF orientation parsing ---------------------------------------------------------------

    @Test
    public void testReadJpegOrientation_bigEndianReturnsValue() {
        byte[] jpeg = jpegWithExifOrientation(6, true);
        assertEquals(6, controller.readJpegOrientation(jpeg));
    }

    @Test
    public void testReadJpegOrientation_littleEndianReturnsValue() {
        byte[] jpeg = jpegWithExifOrientation(8, false);
        assertEquals(8, controller.readJpegOrientation(jpeg));
    }

    @Test
    public void testReadJpegOrientation_noExifSegmentReturnsOne() throws IOException {
        // A plain JPEG produced by ImageIO carries no EXIF Orientation tag.
        assertEquals(1, controller.readJpegOrientation(sourceJpeg()));
    }

    @Test
    public void testReadJpegOrientation_outOfRangeReturnsOne() {
        assertEquals(1, controller.readJpegOrientation(jpegWithExifOrientation(0, true)));
        assertEquals(1, controller.readJpegOrientation(jpegWithExifOrientation(9, false)));
    }

    @Test
    public void testReadJpegOrientation_garbageAndEmptyNeverThrowReturnOne() {
        assertEquals(1, controller.readJpegOrientation(new byte[0]));
        assertEquals(1, controller.readJpegOrientation(new byte[]{(byte) 0xFF, (byte) 0xD8}));
        assertEquals(1, controller.readJpegOrientation(new byte[]{1, 2, 3, 4, 5}));
        assertEquals(1, controller.readJpegOrientation(null));
    }

    // --- Orientation transform ------------------------------------------------------------------

    @Test
    public void testApplyOrientation_normalReturnsSameInstance() {
        BufferedImage src = new BufferedImage(10, 4, BufferedImage.TYPE_INT_RGB);
        assertSame(src, controller.applyOrientation(src, 1, "jpg"));
    }

    @Test
    public void testApplyOrientation_outOfRangeReturnsSameInstance() {
        BufferedImage src = new BufferedImage(10, 4, BufferedImage.TYPE_INT_RGB);
        assertSame(src, controller.applyOrientation(src, 0, "jpg"));
        assertSame(src, controller.applyOrientation(src, 99, "jpg"));
    }

    @Test
    public void testApplyOrientation_rotate180KeepsDimensions() {
        BufferedImage src = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        BufferedImage out = controller.applyOrientation(src, 3, "jpg");
        assertEquals(800, out.getWidth());
        assertEquals(600, out.getHeight());
    }

    @Test
    public void testApplyOrientation_rotate90SwapsDimensionsAndMovesPixels() {
        // 4x2 source; mark the top-left corner so we can assert a 90 CW rotation moved it to the
        // top-right of the (now 2x4) output.
        BufferedImage src = new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = src.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 4, 2);
        src.setRGB(0, 0, Color.RED.getRGB());
        g.dispose();

        BufferedImage out = controller.applyOrientation(src, 6, "jpg"); // rotate 90 CW
        assertEquals(2, out.getWidth(), "90-degree rotation should swap width/height");
        assertEquals(4, out.getHeight());
        // Top-left of source should now be at the top-right of the rotated image.
        assertEquals(Color.RED.getRGB() & 0xFFFFFF,
                out.getRGB(out.getWidth() - 1, 0) & 0xFFFFFF,
                "Orientation 6 should rotate the source top-left to the top-right");
    }

    @Test
    public void testApplyOrientation_orientation8SwapsDimensions() {
        BufferedImage src = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        BufferedImage out = controller.applyOrientation(src, 8, "jpg");
        assertEquals(600, out.getWidth());
        assertEquals(800, out.getHeight());
    }

    // --- Resize-path integration ----------------------------------------------------------------

    @Test
    public void testServeUnitImage_nonJpegPassesThroughOrientationLogicUnchanged() throws IOException {
        // A PNG carries no EXIF orientation; oversized width means original bytes are returned.
        byte[] original = sourcePng();
        stubImage(original, "image/png");

        ResponseEntity<byte[]> response = controller.serveUnitImage(6L, 2000);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(original, response.getBody());
    }

    private void stubImage(byte[] data, String contentType) {
        GalleryImage image = new GalleryImage();
        image.setImageData(data);
        image.setContentType(contentType);
        when(galleryImageService.getGalleryImageById(anyLong())).thenReturn(image);
    }

    private byte[] sourceJpeg() throws IOException {
        BufferedImage img = new BufferedImage(SOURCE_WIDTH, SOURCE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, SOURCE_WIDTH, SOURCE_HEIGHT);
        g.setColor(Color.WHITE);
        g.fillRect(100, 100, 200, 150);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    private byte[] sourcePng() throws IOException {
        BufferedImage img = new BufferedImage(SOURCE_WIDTH, SOURCE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, SOURCE_WIDTH, SOURCE_HEIGHT);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    /**
     * Builds a minimal but valid JPEG whose only content is the SOI marker followed by an APP1
     * segment carrying an EXIF IFD0 with a single Orientation entry. This is enough to exercise
     * {@code readJpegOrientation}, which only scans marker segments for the Exif APP1 block.
     *
     * @param orientation the Orientation value to encode
     * @param bigEndian   true for "MM" (big-endian) TIFF byte order, false for "II" (little-endian)
     */
    private byte[] jpegWithExifOrientation(int orientation, boolean bigEndian) {
        // TIFF block: header (8) + IFD0 (2 count + 12 entry + 4 next-offset).
        byte[] tiff = new byte[8 + 2 + 12 + 4];
        int p = 0;
        if (bigEndian) {
            tiff[p++] = 'M';
            tiff[p++] = 'M';
        } else {
            tiff[p++] = 'I';
            tiff[p++] = 'I';
        }
        putShort(tiff, p, 0x002A, bigEndian);
        p += 2;
        putInt(tiff, p, 8, bigEndian); // IFD0 starts right after the 8-byte header
        p += 4;
        putShort(tiff, p, 1, bigEndian); // one directory entry
        p += 2;
        putShort(tiff, p, 0x0112, bigEndian); // tag: Orientation
        p += 2;
        putShort(tiff, p, 3, bigEndian); // type: SHORT
        p += 2;
        putInt(tiff, p, 1, bigEndian); // count: 1
        p += 4;
        // Value field (4 bytes): SHORT stored in the first 2 bytes.
        putShort(tiff, p, orientation, bigEndian);
        p += 2;
        putShort(tiff, p, 0, bigEndian); // padding of the value field
        p += 2;
        putInt(tiff, p, 0, bigEndian); // next IFD offset: none

        byte[] exifHeader = new byte[]{'E', 'x', 'i', 'f', 0, 0};
        int app1DataLength = exifHeader.length + tiff.length;
        int segLength = app1DataLength + 2; // length field includes its own 2 bytes

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xFF);
        out.write(0xD8); // SOI
        out.write(0xFF);
        out.write(0xE1); // APP1 marker
        out.write((segLength >> 8) & 0xFF);
        out.write(segLength & 0xFF);
        out.write(exifHeader, 0, exifHeader.length);
        out.write(tiff, 0, tiff.length);
        out.write(0xFF);
        out.write(0xD9); // EOI
        return out.toByteArray();
    }

    private void putShort(byte[] b, int offset, int value, boolean bigEndian) {
        if (bigEndian) {
            b[offset] = (byte) ((value >> 8) & 0xFF);
            b[offset + 1] = (byte) (value & 0xFF);
        } else {
            b[offset] = (byte) (value & 0xFF);
            b[offset + 1] = (byte) ((value >> 8) & 0xFF);
        }
    }

    private void putInt(byte[] b, int offset, int value, boolean bigEndian) {
        if (bigEndian) {
            b[offset] = (byte) ((value >> 24) & 0xFF);
            b[offset + 1] = (byte) ((value >> 16) & 0xFF);
            b[offset + 2] = (byte) ((value >> 8) & 0xFF);
            b[offset + 3] = (byte) (value & 0xFF);
        } else {
            b[offset] = (byte) (value & 0xFF);
            b[offset + 1] = (byte) ((value >> 8) & 0xFF);
            b[offset + 2] = (byte) ((value >> 16) & 0xFF);
            b[offset + 3] = (byte) ((value >> 24) & 0xFF);
        }
    }
}
