package com.ecolink.api.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;

import com.ecolink.api.dto.VideoResponseDTO;
import com.ecolink.api.service.VideoService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

	private final VideoService videoService;
	
	public VideoController(VideoService videoService) {
		this.videoService = videoService;
	}
	@Operation(summary = "Upload Video", description = "Uploading of a new video")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> uploadVideo(
	@RequestParam MultipartFile videoFile,
	@RequestParam String title,
	@RequestParam(required=false) String description,
	@RequestParam(required=false) MultipartFile captions) throws IOException {
		VideoResponseDTO dto = videoService.uploadVideo(videoFile, title, description, captions);
		return ResponseEntity.status(HttpStatus.CREATED).body(dto);
	}
	@Operation(summary = "Get all videos", description = "showing a list of all videos by isPublished and createdBy")
	@GetMapping()
	public ResponseEntity<?> getAllVideos(
			@RequestParam(required=false) Boolean isPublished,
			@RequestParam(required=false) String createdBy,
			@RequestParam(defaultValue="1") int page,
			@RequestParam(defaultValue="10") int limit) {
				Page<VideoResponseDTO> result = videoService.getAllVideos(isPublished, createdBy, page, limit);
				return ResponseEntity.ok(Map.of(
						"success", true,
						"data", result.getContent(),
						"pagination", Map.of(
								"page", page,
								"limit", limit,
								"total", result.getTotalElements(),
								"totalPages", result.getTotalPages()
							)
					));
			}
	@Operation(summary = "Get video by ID", description = "finding a specific video by it's ID")
	@GetMapping("/{id}")
	public ResponseEntity<?> getVideoId(
			@PathVariable("id") String id) {
		VideoResponseDTO dto = videoService.getVideo(id);
		return ResponseEntity.ok(dto);
	}
	@Operation(summary = "Update video by id", description = "Updating video title, description, isPublished, and newFile by ID")
	@PatchMapping("/{id}")
	public ResponseEntity<?> updateVideoId(
			@PathVariable("id") String id,
			@RequestParam(required=false) String title,
			@RequestParam(required=false) String description,
			@RequestParam(required=false) Boolean isPublished,
			@RequestParam(required=false) MultipartFile newFile) throws IOException {
		VideoResponseDTO dto = videoService.updateVideo(id, title, description, isPublished, newFile);
		return ResponseEntity.ok(dto);
	}
	@Operation(summary = "Delete video by id", description = "Deleting a video by it's ID")
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteVideoId(@PathVariable("id") String id){
		videoService.deleteVideo(id);
				return ResponseEntity.noContent().build();
	}
	@GetMapping("/stream/{fileId}") //Streamer video direkte og gør det muligt at kunne se video.
	public ResponseEntity<InputStreamResource> streamVideo(
			@PathVariable String fileId) throws IOException {
		GridFsResource resource = videoService.getVideoStream(fileId);
		return ResponseEntity.ok()//Returnere videofilen i størrelse og filformat.
				.contentType(org.springframework.http.MediaType
						.parseMediaType(resource.getContentType()))
				.contentLength(resource.contentLength())
				.body(new InputStreamResource(resource.getInputStream()));
	}
	@GetMapping("/thumbnail/{fileId}")//Thumbnail billede inden videon afspilles.
	public ResponseEntity<InputStreamResource> getThumbnail(
			@PathVariable String fileId) throws IOException {
		GridFsResource resource = videoService.getThumbnailStream(fileId);
		return ResponseEntity.ok()//returnere thumbnail i JPEG som standard filformat.
				.contentType(MediaType.IMAGE_JPEG)
				.contentLength(resource.contentLength())
				.body(new InputStreamResource(resource.getInputStream()));
	}

}
