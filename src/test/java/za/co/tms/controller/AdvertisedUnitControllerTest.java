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
}
