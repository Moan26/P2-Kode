package com.ecolink.api;

import com.ecolink.api.dto.VideoResponseDTO;
import com.ecolink.api.model.Video;
import com.ecolink.api.repository.VideoRepository;
import com.ecolink.api.service.VideoService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class VideoServiceTest {

    private VideoRepository videoRepository;
    private GridFsTemplate gridFsTemplate;
    private GridFsOperations gridFsOperations;
    private VideoService videoService;

    @BeforeEach
    void setUp() {
        videoRepository = mock(VideoRepository.class);
        gridFsTemplate = mock(GridFsTemplate.class);
        gridFsOperations = mock(GridFsOperations.class);
        videoService = new VideoService(videoRepository, gridFsTemplate, gridFsOperations);
    }

    @Test
    void getAllVideos_ingenFiltre_returnerSideMedAlle() {
        Page<Video> page = new PageImpl<>(List.of(video("vid-1", "Testfilm")));
        when(videoRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<VideoResponseDTO> result = videoService.getAllVideos(null, null, 1, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals("Testfilm", result.getContent().get(0).getTitle());
    }

    @Test
    void getAllVideos_filterPaaPublished_kalder_findByIsPublished() {
        Page<Video> page = new PageImpl<>(List.of(video("vid-1", "Udgivet video")));
        when(videoRepository.findByIsPublished(eq(true), any(Pageable.class))).thenReturn(page);

        Page<VideoResponseDTO> result = videoService.getAllVideos(true, null, 1, 10);

        assertEquals(1, result.getTotalElements());
        verify(videoRepository).findByIsPublished(eq(true), any(Pageable.class));
    }

    @Test
    void getAllVideos_filterPaaCreatedBy_kalder_findByCreatedBy() {
        Page<Video> page = new PageImpl<>(List.of(video("vid-1", "Min video")));
        when(videoRepository.findByCreatedBy(eq("user1"), any(Pageable.class))).thenReturn(page);

        Page<VideoResponseDTO> result = videoService.getAllVideos(null, "user1", 1, 10);

        assertEquals(1, result.getTotalElements());
        verify(videoRepository).findByCreatedBy(eq("user1"), any(Pageable.class));
    }

    @Test
    void getAllVideos_limitOver25_bliveskaaret() {
        when(videoRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        videoService.getAllVideos(null, null, 1, 100);

        // Verificer at pageable bruger max 25
        verify(videoRepository).findAll(argThat(
                (Pageable p) -> p.getPageSize() == 25
        ));
    }

    @Test
    void getVideo_eksisterendeId_returnerDTO() {
        when(videoRepository.findById("vid-1")).thenReturn(Optional.of(video("vid-1", "Testfilm")));

        VideoResponseDTO result = videoService.getVideo("vid-1");

        assertEquals("vid-1", result.getId());
        assertEquals("Testfilm", result.getTitle());
    }

    @Test
    void getVideo_ukendt_id_kasterException() {
        when(videoRepository.findById("ukendt")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> videoService.getVideo("ukendt"));
    }

    @Test
    void uploadVideo_forkertFormat_kasterException() {
        MockMultipartFile fil = new MockMultipartFile(
                "video", "test.txt", "text/plain", "data".getBytes()
        );

        assertThrows(IllegalArgumentException.class,
                () -> videoService.uploadVideo(fil, "Titel", "Beskrivelse", null));
    }

    @Test
    @Disabled("Kræver ffprobe installeret på systemet")
    void uploadVideo_under240p_kasterException() throws Exception {
        // Denne test kræver en rigtig videofil under 240p og ffprobe installeret
        // Kør manuelt ved at erstatte bytes med en faktisk lav-opløsnings videofil
        MockMultipartFile fil = new MockMultipartFile(
                "video", "lav.mp4", "video/mp4", new byte[0]
        );
        assertThrows(IllegalArgumentException.class,
                () -> videoService.uploadVideo(fil, "Titel", "Beskrivelse", null));
    }

    @Test
    void uploadVideo_forStor_kasterException() {
        byte[] storData = new byte[101 * 1024 * 1024]; // 101 MB
        MockMultipartFile fil = new MockMultipartFile(
                "video", "stor.mp4", "video/mp4", storData
        );

        assertThrows(IllegalStateException.class,
                () -> videoService.uploadVideo(fil, "Titel", "Beskrivelse", null));
    }

    @Test
    void uploadVideo_gyldigFil_gemmerOgReturnerDTO() throws Exception {
        MockMultipartFile fil = new MockMultipartFile(
                "video", "film.mp4", "video/mp4", "videodata".getBytes()
        );

        ObjectId gridFsId = new ObjectId("507f1f77bcf86cd799439011");
        when(gridFsTemplate.store(any(), anyString(), anyString())).thenReturn(gridFsId);
        // gridFsTemplate.findOne bruges i generateThumbnailAndDuration — returnerer null → tidlig retur
        when(gridFsTemplate.findOne(any())).thenReturn(null);

        Video gemt = video("vid-ny", "Min film");
        when(videoRepository.save(any(Video.class))).thenReturn(gemt);

        VideoResponseDTO result = videoService.uploadVideo(fil, "Min film", "Beskrivelse", null);

        assertNotNull(result);
        assertEquals("vid-ny", result.getId());
        verify(videoRepository).save(any(Video.class));
    }

    @Test
    void deleteVideo_eksisterendeId_sletterFraRepoOgGridFS() {
        String gridFsHex = "507f1f77bcf86cd799439011";
        Video v = video("vid-1", "Testfilm");
        v.setVideoUrl("/api/videos/stream/" + gridFsHex);

        when(videoRepository.findById("vid-1")).thenReturn(Optional.of(v));
        doNothing().when(gridFsTemplate).delete(any());

        videoService.deleteVideo("vid-1");

        verify(gridFsTemplate).delete(any());
        verify(videoRepository).deleteById("vid-1");
    }

    @Test
    void deleteVideo_ukendt_id_kasterException() {
        when(videoRepository.findById("ukendt")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> videoService.deleteVideo("ukendt"));
    }

    private Video video(String id, String title) {
        return Video.builder()
                .id(id)
                .title(title)
                .uploadedAt(Instant.now())
                .updatedAt(Instant.now())
                .isPublished(false)
                .build();
    }
}