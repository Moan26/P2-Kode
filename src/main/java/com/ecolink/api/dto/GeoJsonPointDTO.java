package com.ecolink.api.dto;

import java.util.List;
import lombok.Data;

@Data

public class GeoJsonPointDTO {
    private final String type = "Point";
    private final List<Double> coordinates;

    public GeoJsonPointDTO(double longitude, double latitude) {
        this.coordinates = List.of(longitude, latitude);
    }
}