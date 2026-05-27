package com.ecolink.api;

import com.ecolink.api.config.JwtUtil;
import com.ecolink.api.config.SecurityConfiguration;
import com.ecolink.api.controller.VideoController;
import com.ecolink.api.repository.UserRepository;
import com.ecolink.api.dto.VideoResponseDTO;
import com.ecolink.api.service.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VideoController.class)
@Import(SecurityConfiguration.class)
class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private VideoService videoService;

    @Test
    @WithMockUser
    void getPage1() throws Exception {
        when(videoService.getAllVideos(null, null, 1, 10)).thenReturn(videoPage());

        mockMvc.perform(get("/api/videos?page=1&limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pagination.page").value(1));
    }

    @Test
    @WithMockUser
    void getPage2() throws Exception {
        when(videoService.getAllVideos(null, null, 2, 10)).thenReturn(Page.empty());

        mockMvc.perform(get("/api/videos?page=2&limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pagination.page").value(2));
    }

    @Test
    @WithMockUser
    void getLastPage() throws Exception {
        when(videoService.getAllVideos(null, null, 1, 25)).thenReturn(videoPage());

        mockMvc.perform(get("/api/videos?page=1&limit=25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pagination.page").value(1));
    }

    @Test
    @WithMockUser
    void getWithNoLatLngValuesFallback() throws Exception {
        when(videoService.getAllVideos(null, null, 1, 10)).thenReturn(videoPage());

        mockMvc.perform(get("/api/videos").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.limit").value(10));
    }

    private Page<VideoResponseDTO> videoPage() {
        return new PageImpl<>(List.of(
                VideoResponseDTO.builder()
                        .id("vid-1")
                        .title("Test video")
                        .watchUrl("/videos/vid-1")
                        .build()
        ));
    }
}
