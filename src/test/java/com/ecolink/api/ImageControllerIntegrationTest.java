package com.ecolink.api;

import com.ecolink.api.config.JwtUtil;
import com.ecolink.api.config.SecurityConfiguration;
import com.ecolink.api.controller.ImageController;
import com.ecolink.api.repository.UserRepository;
import com.ecolink.api.dto.UpdateImageRequest;
import com.ecolink.api.model.Image;
import com.ecolink.api.model.ImageMetaData;
import com.ecolink.api.service.ImageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageController.class)
@Import(SecurityConfiguration.class)
class ImageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ImageService imageService;

    @Test
    @WithMockUser(username = "editor", roles = {"CONTENT_CREATOR"})
    void shouldReturnImages() throws Exception {
        when(imageService.getImagesInList(null, null, 1, 20))
                .thenReturn(new PageImpl<>(List.of(
                        image("img-1", "Published image", "user1", true),
                        image("img-2", "Draft image", "user2", false)
                )));

        mockMvc.perform(get("/api/images?page=1&limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.limit").value(20));
    }

    @Test
    @WithMockUser(username = "editor", roles = {"CONTENT_CREATOR"})
    void shouldFilterByPublishedStatus() throws Exception {
        when(imageService.getImagesInList(true, null, 1, 20))
                .thenReturn(new PageImpl<>(List.of(image("img-1", "Published image", "user1", true))));

        mockMvc.perform(get("/api/images?isPublished=true&page=1&limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Published image"))
                .andExpect(jsonPath("$.data[0].isPublished").value(true));
    }

    @Test
    @WithMockUser(username = "editor", roles = {"CONTENT_CREATOR"})
    void shouldFilterByCreatedBy() throws Exception {
        when(imageService.getImagesInList(null, "user2", 1, 20))
                .thenReturn(new PageImpl<>(List.of(image("img-2", "Draft image", "user2", false))));

        mockMvc.perform(get("/api/images?createdBy=user2&page=1&limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].createdBy").value("user2"))
                .andExpect(jsonPath("$.data[0].title").value("Draft image"));
    }

    @Test
    @WithMockUser(username = "editor", roles = {"CONTENT_CREATOR"})
    void shouldReturnBadRequestForInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/images?page=0&limit=200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/images?page=1&limit=20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"CONTENT_CREATOR"})
    void shouldUploadImage() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile",
                "test.png",
                "image/png",
                validImageBytes()
        );

        when(imageService.uploadImage(any(), eq("Test title"), eq("Alt text"), eq("Description"), eq("Caption"), any(Authentication.class)))
                .thenReturn(Image.builder()
                        .id("img-1")
                        .title("Test title")
                        .alt_text("Alt text")
                        .description("Description")
                        .caption("Caption")
                        .createdBy("user1")
                        .imageMetaData(ImageMetaData.builder().format("image/png").build())
                        .build());

        org.springframework.mock.web.MockPart title =
                new org.springframework.mock.web.MockPart("title", "Test title".getBytes());
        title.getHeaders().setContentType(org.springframework.http.MediaType.TEXT_PLAIN);

        org.springframework.mock.web.MockPart altText =
                new org.springframework.mock.web.MockPart("alt_text", "Alt text".getBytes());
        altText.getHeaders().setContentType(org.springframework.http.MediaType.TEXT_PLAIN);

        org.springframework.mock.web.MockPart description =
                new org.springframework.mock.web.MockPart("description", "Description".getBytes());
        description.getHeaders().setContentType(org.springframework.http.MediaType.TEXT_PLAIN);

        org.springframework.mock.web.MockPart caption =
                new org.springframework.mock.web.MockPart("caption", "Caption".getBytes());
        caption.getHeaders().setContentType(org.springframework.http.MediaType.TEXT_PLAIN);

        mockMvc.perform(multipart("/api/images")
                        .file(imageFile)
                        .part(title)
                        .part(altText)
                        .part(description)
                        .part(caption))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test title"))
                .andExpect(jsonPath("$.alt_text").value("Alt text"))
                .andExpect(jsonPath("$.createdBy").value("user1"))
                .andExpect(jsonPath("$.imageMetaData.format").value("image/png"));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"CONTENT_CREATOR"})
    void shouldPatchImageMetadataAndUpdateUpdatedAt() throws Exception {
        Instant updatedAt = Instant.parse("2026-04-28T12:00:00Z");
        when(imageService.updateImage(eq("img123"), any(UpdateImageRequest.class), any(), any(Authentication.class)))
                .thenReturn(Image.builder()
                        .id("img123")
                        .title("New title")
                        .alt_text("New alt")
                        .description("New description")
                        .caption("New caption")
                        .createdBy("user1")
                        .updatedAt(updatedAt)
                        .build());

        MockMultipartFile title = new MockMultipartFile(
                "title", "", "text/plain", "New title".getBytes()
        );

        MockMultipartFile altText = new MockMultipartFile(
                "alt_text", "", "text/plain", "New alt".getBytes()
        );

        MockMultipartFile description = new MockMultipartFile(
                "description", "", "text/plain", "New description".getBytes()
        );

        MockMultipartFile caption = new MockMultipartFile(
                "caption", "", "text/plain", "New caption".getBytes()
        );

        mockMvc.perform(multipart("/api/images/{id}", "img123")
                        .file(title)
                        .file(altText)
                        .file(description)
                        .file(caption)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.alt_text").value("New alt"))
                .andExpect(jsonPath("$.description").value("New description"))
                .andExpect(jsonPath("$.caption").value("New caption"));

        ArgumentCaptor<UpdateImageRequest> requestCaptor = ArgumentCaptor.forClass(UpdateImageRequest.class);
        verify(imageService).updateImage(eq("img123"), requestCaptor.capture(), any(), any(Authentication.class));

        UpdateImageRequest request = requestCaptor.getValue();
        assertEquals("New title", request.getTitle());
        assertEquals("New alt", request.getAltText());
        assertEquals("New description", request.getDescription());
        assertEquals("New caption", request.getCaption());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"CONTENT_CREATOR"})
    void shouldDeleteImage() throws Exception {
        doNothing().when(imageService).deleteImage(eq("img123"), any(Authentication.class));

        mockMvc.perform(delete("/api/images/{id}", "img123"))
                .andExpect(status().isNoContent());

        verify(imageService).deleteImage(eq("img123"), any(Authentication.class));
    }

    private Image image(String id, String title, String createdBy, boolean isPublished) {
        return Image.builder()
                .id(id)
                .title(title)
                .alt_text("Alt text")
                .description("Description")
                .caption("Caption")
                .createdBy(createdBy)
                .isPublished(isPublished)
                .uploadedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private byte[] validImageBytes() throws Exception {
        java.awt.image.BufferedImage image =
                new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);

        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", outputStream);

        return outputStream.toByteArray();
    }
}
