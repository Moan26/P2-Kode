package com.ecolink.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request fields for updating image metadata")
public class UpdateImageRequest {

    @JsonProperty("alt_text")
    @Schema(description = "Alternative text for the image", example = "A red bicycle parked outside")
    @Size(max = 255)
    private String altText;

    @Schema(description = "Image title", example = "Bike photo")
    @Size(max = 255)
    private String title;

    @Schema(description = "Image description", example = "Taken outside the office")
    @Size(max = 1000)
    private String description;

    @Schema(description = "Image caption", example = "Company bike")
    @Size(max = 500)
    private String caption;

    @Schema(description = "Whether the image is published", example = "true")
    private Boolean isPublished;
}