package com.ecolink.api.dto;

import java.time.Instant;

import com.ecolink.api.model.VideoMetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class VideoResponseDTO {
	
	private String id;
	private String watchUrl;
	private String title;
	private String description;
	private String thumbnail;
	private Double duration;
	private String captions;
	private Instant uploadedAt;
	private Instant updatedAt;
	private String createdBy, organizationId;
	private boolean isPublished;
	private VideoMetadata metadata;
}
