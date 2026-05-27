package com.ecolink.api.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageMetaData {

    private Long fileSize;
    private String format;
    private Dimensions dimensions;
}