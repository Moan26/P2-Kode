package com.ecolink.api.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Document(collection = "videos")
public class Video {
	
	@Id
	private String id;
	private String videoUrl;
	
	@NotBlank
	private String title;
	private String description;
	private String thumbnail;
	private Double duration;
	private String captions;
	
	private Instant uploadedAt;
	
	private Instant updatedAt;
	
	@Indexed
	private String createdBy, organizationId;
	private boolean isPublished;
	private VideoMetadata metadata;
	
}
