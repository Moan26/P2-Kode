package com.ecolink.api;

import com.ecolink.api.dto.UpdateImageRequest;
import com.ecolink.api.model.Image;
import com.ecolink.api.model.ImageResolutions;
import com.ecolink.api.repository.ImageRepository;
import com.ecolink.api.service.ImageProcessingService;
import com.ecolink.api.service.ImageService;
import com.mongodb.client.gridfs.GridFSBucket;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private GridFsTemplate gridFsTemplate;

    @Mock
    private GridFSBucket imagesGridFsBucket;

    @Mock
    private GridFSBucket thumbnailsGridFsBucket;

    @Mock
    private GridFSBucket mediumGridFsBucket;

    @Mock
    private GridFSBucket largeGridFsBucket;

    @Mock
    private ImageProcessingService imageProcessingService;

    @Mock
    private Authentication authentication;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        imageService = new ImageService(
                imageRepository,
                gridFsTemplate,
                imagesGridFsBucket,
                thumbnailsGridFsBucket,
                mediumGridFsBucket,
                largeGridFsBucket,
                imageProcessingService
        );
    }

    @Test
    void uploadImage_whenFileIsMissing_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                imageService.uploadImage(null, "Title", "Alt", "desc", "caption", authentication)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Image file is required"));
    }

    @Test
    void uploadImage_whenTitleIsBlank_throwsBadRequest() {
        MockMultipartFile imageFile = validPngFile("test.png");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                imageService.uploadImage(imageFile, "   ", "Alt", "desc", "caption", authentication)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Title is required"));
    }

    @Test
    void uploadImage_whenAltTextIsBlank_throwsBadRequest() {
        MockMultipartFile imageFile = validPngFile("test.png");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                imageService.uploadImage(imageFile, "Title", "   ", "desc", "caption", authentication)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("alt_text is required"));
    }

    @Test
    void uploadImage_whenUnsupportedFormat_throwsBadRequest() {
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile",
                "test.txt",
                "text/plain",
                "not-an-image".getBytes()
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                imageService.uploadImage(imageFile, "Title", "Alt", "desc", "caption", authentication)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Unsupported image format"));
    }

    @Test
    void uploadImage_whenFileTooLarge_throwsPayloadTooLarge() throws Exception {
        MultipartFile imageFile = mock(MultipartFile.class);

        when(imageFile.isEmpty()).thenReturn(false);
        when(imageFile.getOriginalFilename()).thenReturn("huge.png");
        when(imageFile.getContentType()).thenReturn("image/png");
        when(imageFile.getSize()).thenReturn(10L * 1024 * 1024 + 1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                imageService.uploadImage(imageFile, "Title", "Alt", "desc", "caption", authentication)
        );

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, ex.getStatusCode());
        assertTrue(ex.getReason().contains("File too large"));
    }

    @Test
    void findById_whenImageMissing_throwsNotFound() {
        when(imageRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                imageService.findById("missing")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Image with id missing not found"));
    }

    @Test
    void updateImage_whenUserIsNotOwnerOrAdmin_throwsForbidden() throws Exception {
        Image existing = Image.builder()
                .id("img123")
                .createdBy("someoneElse")
                .title("Old")
                .alt_text("Old alt")
                .build();

        UpdateImageRequest request = new UpdateImageRequest();
        request.setTitle("New title");

        when(imageRepository.findById("img123")).thenReturn(Optional.of(existing));
        when(authentication.getName()).thenReturn("user1");
        when(authentication.getAuthorities()).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                imageService.updateImage("img123", request, null, authentication)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("not allowed"));
    }

    @Test
    void updateImage_whenOwnerUpdatesMetadata_savesUpdatedImage() throws Exception {
        Image existing = Image.builder()
                .id("img123")
                .createdBy("user1")
                .title("Old")
                .alt_text("Old alt")
                .description("Old desc")
                .caption("Old caption")
                .isPublished(false)
                .build();

        UpdateImageRequest request = new UpdateImageRequest();
        request.setTitle("New title");
        request.setAltText("New alt");
        request.setDescription("New desc");
        request.setCaption("New caption");
        request.setIsPublished(true);

        when(imageRepository.findById("img123")).thenReturn(Optional.of(existing));
        when(authentication.getName()).thenReturn("user1");
        when(authentication.getAuthorities()).thenReturn(List.of());
        when(imageRepository.save(any(Image.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Image result = imageService.updateImage("img123", request, null, authentication);

        assertEquals("New title", result.getTitle());
        assertEquals("New alt", result.getAlt_text());
        assertEquals("New desc", result.getDescription());
        assertEquals("New caption", result.getCaption());
        assertTrue(result.getIsPublished());
        assertNotNull(result.getUpdatedAt());

        verify(imageRepository).save(existing);
    }

    @Test
    void deleteImage_whenUserIsNotOwnerOrAdmin_throwsForbidden() {
        Image existing = Image.builder()
                .id("img123")
                .createdBy("someoneElse")
                .build();

        when(imageRepository.findById("img123")).thenReturn(Optional.of(existing));
        when(authentication.getName()).thenReturn("user1");
        when(authentication.getAuthorities()).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                imageService.deleteImage("img123", authentication)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("not allowed"));
    }

    @Test
    void deleteImage_whenOwner_deletesGridFsFilesAndRepositoryRecord() {
        Image existing = Image.builder()
                .id("img123")
                .createdBy("user1")
                .imageUrl("507f1f77bcf86cd799439011")
                .resolutions(ImageResolutions.builder()
                        .thumbnail("507f1f77bcf86cd799439012")
                        .medium("507f1f77bcf86cd799439013")
                        .large("507f1f77bcf86cd799439014")
                        .build())
                .build();

        when(imageRepository.findById("img123")).thenReturn(Optional.of(existing));
        when(authentication.getName()).thenReturn("user1");
        when(authentication.getAuthorities()).thenReturn(List.of());

        imageService.deleteImage("img123", authentication);

        verify(imagesGridFsBucket).delete(new ObjectId("507f1f77bcf86cd799439011"));
        verify(thumbnailsGridFsBucket).delete(new ObjectId("507f1f77bcf86cd799439012"));
        verify(mediumGridFsBucket).delete(new ObjectId("507f1f77bcf86cd799439013"));
        verify(largeGridFsBucket).delete(new ObjectId("507f1f77bcf86cd799439014"));
        verify(imageRepository).deleteById("img123");
    }

    private MockMultipartFile validPngFile(String filename) {
        try {
            BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);

            return new MockMultipartFile(
                    "imageFile",
                    filename,
                    "image/png",
                    outputStream.toByteArray()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}