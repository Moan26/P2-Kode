package com.ecolink.api.dto;

import java.util.List;

public class EcopointMarkerDTO {

    private final String id;
    private final String name;
    private final List<Double> coordinates; // GeoJSON format: [longitude, latitude]
    private final String status;
    private final String operating;

    public EcopointMarkerDTO(String id, String name, List<Double> coordinates,
                             String status, String operating) {
        this.id = id;
        this.name = name;
        this.coordinates = coordinates; // [lng, lat] samme format som Google Maps bruger
        this.status = status;           // fx "ACTIVE" eller "FULL"
        this.operating = operating;     // fx "OPEN" eller "CLOSED"
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<Double> getCoordinates() { return coordinates; }
    public String getStatus() { return status; }
    public String getOperating() { return operating; }
}