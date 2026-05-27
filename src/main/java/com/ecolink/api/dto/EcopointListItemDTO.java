package com.ecolink.api.dto;

import java.util.List;

import com.ecolink.api.model.AcceptedMaterial;

public class EcopointListItemDTO {

    private String id;
    private String name;
    private String address;
    private Double distanceKm;
    private List<AcceptedMaterial> acceptedMaterials;
    private String status;
    private String thumbnailUrl;

    public EcopointListItemDTO(String id, String name, String address,
                               Double distanceKm, List<AcceptedMaterial> acceptedMaterials,
                               String status, String thumbnailUrl) {
        this.id = id; // unikt ID på EcoPoint
        this.name = name; // navn fx "UNB Ecopoint 1"
        this.address = address; // adresse
        this.distanceKm = distanceKm; // Hvor langt brugeren er fra dette EcoPoint i km
        this.acceptedMaterials = acceptedMaterials; // liste af materialer fx Aluminium, Plastic
        this.status = status; // om den er fuld eller ej
        this.thumbnailUrl = thumbnailUrl; // billedlink
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public Double getDistanceKm() { return distanceKm; }
    public List<AcceptedMaterial> getAcceptedMaterials() { return acceptedMaterials; }
    public String getStatus() { return status; }
    public String getThumbnailUrl() { return thumbnailUrl; }
}

// EcoPointListItemDTO.java definerer praecis hvilke felter der sendes tilbage til appen som svar,
// fx navn, adresse og distanceKm.
