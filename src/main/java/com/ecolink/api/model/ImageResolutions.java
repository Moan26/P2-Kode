package com.ecolink.api.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageResolutions {
    private String thumbnail;
    private String medium;
    private String large;
}