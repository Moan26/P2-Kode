package com.ecolink.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoMetadata {
	private Long fileSize;
	private String format;
	private String resolution;
}
