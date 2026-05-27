package com.ecolink.api.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "images")
public class Image {

    @Id
    @Setter
    private String id;

    private String imageUrl;

    private String alt_text;

    private String title;

    private String description;

    private String caption;

    private Instant uploadedAt;

    private Instant updatedAt;

    private String createdBy;

    @Builder.Default
    private Boolean isPublished = false;

    private ImageMetaData imageMetaData;

    private ImageResolutions resolutions;
}
