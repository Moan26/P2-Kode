package com.ecolink.api;

import com.ecolink.api.config.EcopointWasNotFoundException;
import com.ecolink.api.config.JwtUtil;
import com.ecolink.api.config.SecurityConfiguration;
import com.ecolink.api.controller.EcopointController;
import com.ecolink.api.dto.EcopointDetailDTO;
import com.ecolink.api.dto.EcopointListItemDTO;
import com.ecolink.api.dto.GeoJsonPointDTO;
import com.ecolink.api.repository.UserRepository;
import com.ecolink.api.service.EcoPointService;
import com.ecolink.api.service.ImageService;
import com.ecolink.api.service.LocationEcopointService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EcopointController.class)
@Import(SecurityConfiguration.class)
class EcoPointControllerTest {

    private static final String TEST_ID = "507f1f77bcf86cd799439011";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private EcoPointService ecoPointService;

    @MockBean
    private ImageService imageService;

    @MockBean
    private LocationEcopointService locationService;

    @Test
    @WithMockUser
    void getPage1() throws Exception {
        when(ecoPointService.getEcoPoints(null, null, 1, 10)).thenReturn(pageWithSingleEcopoint());

        mockMvc.perform(get("/api/ecopoints?page=1&limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pagination.page").value(1));
    }

    @Test
    @WithMockUser
    void getPage2() throws Exception {
        when(ecoPointService.getEcoPoints(null, null, 2, 10)).thenReturn(Page.empty());

        mockMvc.perform(get("/api/ecopoints?page=2&limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pagination.page").value(2));
    }

    @Test
    @WithMockUser
    void getLastPage() throws Exception {
        when(ecoPointService.getEcoPoints(null, null, 1, 50)).thenReturn(pageWithSingleEcopoint());

        mockMvc.perform(get("/api/ecopoints?page=1&limit=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pagination.page").value(1));
    }

    @Test
    @WithMockUser
    void getWithNoLatLngValuesFallback() throws Exception {
        when(ecoPointService.getEcoPoints(null, null, 1, 10)).thenReturn(pageWithSingleEcopoint());

        mockMvc.perform(get("/api/ecopoints").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.limit").value(10));
    }

    @Test
    @WithMockUser
    void getEcopointById_validId_returns200() throws Exception {
        when(ecoPointService.getEcopointById(TEST_ID)).thenReturn(detailDto());

        mockMvc.perform(get("/api/ecopoints/" + TEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(TEST_ID));
    }

    @Test
    @WithMockUser
    void getEcopointById_invalidId_returns400() throws Exception {
        mockMvc.perform(get("/api/ecopoints/notvalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser
    void getEcopointById_notFound_returns404() throws Exception {
        when(ecoPointService.getEcopointById(TEST_ID))
                .thenThrow(new EcopointWasNotFoundException(TEST_ID));

        mockMvc.perform(get("/api/ecopoints/" + TEST_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser
    void getBoundsHappyPath() throws Exception {
        when(locationService.findInBounds(55.60, 12.50, 55.70, 12.60))
                .thenReturn(Map.of("items", List.of(), "truncated", false));

        mockMvc.perform(get("/api/ecopoints/bounds")
                        .param("swLat", "55.60")
                        .param("swLng", "12.50")
                        .param("neLat", "55.70")
                        .param("neLng", "12.60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.truncated").isBoolean());
    }

    @Test
    @WithMockUser
    void getBoundsEmptyViewport() throws Exception {
        when(locationService.findInBounds(0.00, 0.00, 0.01, 0.01))
                .thenReturn(Map.of("items", List.of(), "truncated", false));

        mockMvc.perform(get("/api/ecopoints/bounds")
                        .param("swLat", "0.00")
                        .param("swLng", "0.00")
                        .param("neLat", "0.01")
                        .param("neLng", "0.01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @WithMockUser
    void getBoundsMissingParam() throws Exception {
        mockMvc.perform(get("/api/ecopoints/bounds")
                        .param("swLat", "57.00")
                        .param("swLng", "9.80"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getBoundsSwappedCorners() throws Exception {
        when(locationService.findInBounds(eq(57.10), eq(9.80), eq(57.00), eq(10.00)))
                .thenThrow(new IllegalArgumentException("swLat cannot be greater than neLat"));

        mockMvc.perform(get("/api/ecopoints/bounds")
                        .param("swLat", "57.10")
                        .param("swLng", "9.80")
                        .param("neLat", "57.00")
                        .param("neLng", "10.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private Page<EcopointListItemDTO> pageWithSingleEcopoint() {
        return new PageImpl<>(List.of(
                new EcopointListItemDTO(TEST_ID, "Test punkt", "Testvej 1", null, null, "ACTIVE", null)
        ));
    }

    private EcopointDetailDTO detailDto() {
        return EcopointDetailDTO.builder()
                .id(TEST_ID)
                .name("Test punkt")
                .address("Testvej 1")
                .coordinates(new GeoJsonPointDTO(12.54, 55.65))
                .status("ACTIVE")
                .build();
    }
}
