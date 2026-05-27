package com.ecolink.api.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ecolink.api.dto.VideoResponseDTO;
import com.ecolink.api.model.Video;
import com.ecolink.api.model.VideoMetadata;
import com.ecolink.api.repository.VideoRepository;
import com.mongodb.client.gridfs.model.GridFSFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final GridFsTemplate gridFsTemplate;
    private final GridFsOperations gridFsOperations;

    // Tilladte videoformater
    private static final List<String> ALLOWED_FORMATS = List.of("video/mp4", "video/webm", "video/quicktime");

    // Maksimal filstørrelse: 100MB i bytes
    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024;

    // ===================== OPRET VIDEO =====================

    public VideoResponseDTO uploadVideo(MultipartFile videoFile, String title, String description, MultipartFile captionFile) throws IOException {

        // Valider filformat
        if (!ALLOWED_FORMATS.contains(videoFile.getContentType())) {
            throw new IllegalArgumentException("File format not supported. Use MP4, WebM or MOV.");
        }

        // Valider filstørrelse
        if (videoFile.getSize() > MAX_FILE_SIZE) {
            throw new IllegalStateException("File is too large. Maximum 100MB.");
        }
        File tempForProbe = File.createTempFile("probe_", ".mp4");
        try {
            FileUtils.copyInputStreamToFile(
                    videoFile.getInputStream(), tempForProbe);

            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe", "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=height",
                    "-of", "csv=p=0",
                    tempForProbe.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            int height = Integer.parseInt(output);
            if (height < 240) {
                throw new IllegalArgumentException(
                        "Video skal være mindst 240p. Upload er " + height + "p.");
            }
        } catch (IllegalArgumentException e) {
            throw e; // videokrav ikke opfyldt — send videre
        } catch (Exception e) {
            // ffprobe ikke tilgængeligt — spring tjek over
            System.err.println("240p-tjek kunne ikke gennemføres: " + e.getMessage());
        } finally {
            tempForProbe.delete();
        }

        // Gem videofilen i GridFS
        ObjectId videoFileId;
        try (InputStream inputStream = videoFile.getInputStream()) {
            videoFileId = gridFsTemplate.store(inputStream, videoFile.getOriginalFilename(), videoFile.getContentType());
        }

        // Gem undertekstfil i GridFS hvis den er vedhæftet
        String captionUrl = null;
        if (captionFile != null && !captionFile.isEmpty()) {
            try (InputStream inputStream = captionFile.getInputStream()) {
                ObjectId captionFileId = gridFsTemplate.store(inputStream, captionFile.getOriginalFilename(), captionFile.getContentType());
                captionUrl = "/api/videos/captions/" + captionFileId.toHexString();
            }
        }

        // Byg video-metadata (uden duration — sættes asynkront af FFmpeg)
        VideoMetadata metadata = VideoMetadata.builder()
                .fileSize(videoFile.getSize())
                .format(videoFile.getContentType())
                .resolution("unknown")
                .build();

        // Gem video-dokumentet i MongoDB
        Video video = Video.builder()
                .videoUrl("/api/videos/stream/" + videoFileId.toHexString())
                .title(title)
                .description(description)
                .captions(captionUrl)
                .uploadedAt(Instant.now())
                .updatedAt(Instant.now())
                .isPublished(false) // altid kladde som standard
                .metadata(metadata)
                .build();

        Video saved = videoRepository.save(video);

        // Start asynkron thumbnail + duration generering
        generateThumbnailAndDuration(saved.getId(), videoFileId);

        return mapToDTO(saved);
    }

    // ===================== HENT ALLE VIDEOER (pagineret) =====================

    public Page<VideoResponseDTO> getAllVideos(Boolean isPublished, String createdBy, int page, int limit) {

        // Sørg for at grænsen er mellem 1 og 25
        int cappedLimit = Math.min(limit, 25);
        Pageable pageable = PageRequest.of(page - 1, cappedLimit, Sort.by(Sort.Direction.DESC, "uploadedAt"));

        Page<Video> videos;

        if (isPublished != null && createdBy != null) {
            // Filtrer på begge: udgivetstatus OG opretter
            videos = videoRepository.findByIsPublishedAndCreatedBy(isPublished, createdBy, pageable);
        } else if (isPublished != null) {
            // Filtrer kun på udgivetstatus
            videos = videoRepository.findByIsPublished(isPublished, pageable);
        } else if (createdBy != null) {
            // Filtrer kun på opretter
            videos = videoRepository.findByCreatedBy(createdBy, pageable);
        } else {
            // Ingen filtre — hent alle
            videos = videoRepository.findAll(pageable);
        }

        return videos.map(this::mapToDTO);
    }

    // ===================== HENT ÉN VIDEO =====================

    public VideoResponseDTO getVideo(String id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found: " + id));
        return mapToDTO(video);
    }

    // ===================== OPDATER VIDEO =====================

    public VideoResponseDTO updateVideo(String id, String newTitle, String newDescription, Boolean isPublished, MultipartFile newVideoFile) throws IOException {

        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found: " + id));

        // Opdater titel hvis den er angivet
        if (newTitle != null && !newTitle.isBlank()) {
            video.setTitle(newTitle);
        }

        // Opdater beskrivelse hvis den er angivet
        if (newDescription != null) {
            video.setDescription(newDescription);
        }

        // Skift udgivetstatus hvis angivet
        if (isPublished != null) {
            video.setPublished(isPublished);
        }

        // Erstat videofilen hvis en ny er uploadet
        if (newVideoFile != null && !newVideoFile.isEmpty()) {

            // Valider ny fil
            if (!ALLOWED_FORMATS.contains(newVideoFile.getContentType())) {
                throw new IllegalArgumentException("File format not supported.");
            }
            if (newVideoFile.getSize() > MAX_FILE_SIZE) {
                throw new IllegalStateException("File is too large. Maximum 100MB.");
            }

            // Slet den gamle videofil fra GridFS
            String oldUrl = video.getVideoUrl();
            if (oldUrl != null) {
                String oldId = oldUrl.substring(oldUrl.lastIndexOf("/") + 1);
                gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(oldId))));
            }

            // Gem den nye videofil i GridFS
            ObjectId newFileId;
            try (InputStream inputStream = newVideoFile.getInputStream()) {
                newFileId = gridFsTemplate.store(inputStream, newVideoFile.getOriginalFilename(), newVideoFile.getContentType());
            }

            video.setVideoUrl("/api/videos/stream/" + newFileId.toHexString());
        }

        video.setUpdatedAt(Instant.now());
        Video updated = videoRepository.save(video);
        return mapToDTO(updated);
    }

    // ===================== SLET VIDEO =====================

    public void deleteVideo(String id) {

        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found: " + id));

        // Slet videofilen fra GridFS
        if (video.getVideoUrl() != null) {
            String fileId = video.getVideoUrl().substring(video.getVideoUrl().lastIndexOf("/") + 1);
            gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
        }

        // Slet thumbnail fra GridFS
        if (video.getThumbnail() != null) {
            String thumbnailId = video.getThumbnail().substring(video.getThumbnail().lastIndexOf("/") + 1);
            gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(thumbnailId))));
        }

        // Slet video-dokumentet fra MongoDB
        videoRepository.deleteById(id);
    }

    // ===================== ASYNKRON THUMBNAIL + DURATION =====================

    @Async
    public void generateThumbnailAndDuration(String videoId, ObjectId gridFsFileId) {
        try {
            // Hent videofilen fra GridFS
            GridFSFile gridFSFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(gridFsFileId)));
            if (gridFSFile == null) return;

            // Opret midlertidige filer
            File tempThumbnail = File.createTempFile("thumbnail_", ".jpg");
            File tempVideo = File.createTempFile("video_", ".mp4");

            // Kopiér video fra GridFS til midlertidig fil
            try (InputStream videoStream = gridFsOperations.getResource(gridFSFile).getInputStream()) {
                FileUtils.copyInputStreamToFile(videoStream, tempVideo);
            }

            // Kør FFmpeg kommando: udtræk første frame ved 1 sekund
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-i", tempVideo.getAbsolutePath(),
                    "-ss", "00:00:01",
                    "-vframes", "1",
                    "-q:v", "2",
                    tempThumbnail.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();

            // Gem thumbnail i GridFS
            ObjectId thumbnailId;
            try (InputStream thumbnailStream = new FileInputStream(tempThumbnail)) {
                thumbnailId = gridFsTemplate.store(thumbnailStream, "thumbnail_" + videoId + ".jpg", "image/jpeg");
            }

            // Opdater video-dokumentet med thumbnail URL
            Optional<Video> optionalVideo = videoRepository.findById(videoId);
            if (optionalVideo.isPresent()) {
                Video video = optionalVideo.get();
                video.setThumbnail("/api/videos/thumbnail/" + thumbnailId.toHexString());
                videoRepository.save(video);
            }

            // Ryd op i midlertidige filer
            tempVideo.delete();
            tempThumbnail.delete();

        } catch (Exception e) {
            // Log fejl men lad ikke den asynkrone fejl crashe applikationen
            System.err.println("Error generating thumbnail for video " + videoId + ": " + e.getMessage());
        }
    }
    public GridFsResource getVideoStream(String fileId) { //Henter videofil fra GridFS ved fil ID. kaldes af StreamVideo i Video Controller.
        GridFSFile file = gridFsTemplate.findOne(
                new Query(Criteria.where("_id").is(new ObjectId(fileId)))
        );
        if (file == null) {
            throw new RuntimeException("Video file not found: " + fileId);
        }
        return gridFsOperations.getResource(file); //Returnere videofil i GridFS og kaster fejl hvis den ikke findes ellers vil den være klar til at streame.
    }
    public GridFsResource getThumbnailStream(String fileId) { //Gør det samme men bare med Thumbnail i stedet for Videofil. og bruges også af den samme controller.
        GridFSFile file = gridFsTemplate.findOne(
                new Query(Criteria.where("_id").is(new ObjectId(fileId)))
        );
        if (file == null) {
            throw new RuntimeException("Thumbnail not found: " + fileId);
        }
        return gridFsOperations.getResource(file);
    }

    private VideoResponseDTO mapToDTO(Video video) {
        return VideoResponseDTO.builder()
                .id(video.getId())
                .watchUrl(video.getVideoUrl())
                .title(video.getTitle())
                .description(video.getDescription())
                .thumbnail(video.getThumbnail())
                .duration(video.getDuration())
                .captions(video.getCaptions())
                .uploadedAt(video.getUploadedAt())
                .updatedAt(video.getUpdatedAt())
                .createdBy(video.getCreatedBy())
                .isPublished(video.isPublished())
                .metadata(video.getMetadata())
                .build();
    }
}