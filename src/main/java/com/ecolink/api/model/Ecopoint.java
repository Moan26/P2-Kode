package com.ecolink.api.model;

import java.util.List;

import com.ecolink.api.model.enums.ConditionEcopoint;
import com.ecolink.api.model.enums.OperatingEcopoint;
import com.ecolink.api.model.enums.StatusEcopoint;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ecopoints")
public class Ecopoint {
    @Id
    private String id;
    private String ecopointName;
    private String ecopointAddress;
    private GPSLocations ecopointLocation;
    private StatusEcopoint ecopointStatus;
    private ConditionEcopoint ecopointCondition;
    private String operatingHours;
    private OperatingEcopoint ecopointOperating;
    private List<AcceptedMaterial> acceptedMaterials;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    public Ecopoint() {}

    public Ecopoint(String ecopointID, String ecopointName, String ecopointAddress,
                    GPSLocations ecopointLocation, StatusEcopoint ecopointStatus, ConditionEcopoint ecopointCondition,
                    String operatingHours, OperatingEcopoint ecopointOperating, GeoJsonPoint location) {
        super();
        this.id = ecopointID;
        this.ecopointName = ecopointName;
        this.ecopointAddress = ecopointAddress;
        this.ecopointLocation = ecopointLocation;
        this.ecopointStatus = ecopointStatus;
        this.ecopointCondition = ecopointCondition;
        this.operatingHours = operatingHours;
        this.ecopointOperating = ecopointOperating;
        this.location = location;
    }


    public String getEcopointID() {
        return id;
    }
    public String getEcopointName() {
        return ecopointName;
    }
    public GPSLocations getEcopointLocation() {
        return ecopointLocation;
    }
    public StatusEcopoint getEcopointStatus() {
        return ecopointStatus;
    }
    public ConditionEcopoint getEcopointCondition() {
        return ecopointCondition;
    }
    public void setEcopointStatus(StatusEcopoint ecopointStatus) {
        this.ecopointStatus = ecopointStatus;
    }
    public void setEcopointCondition(ConditionEcopoint ecopointCondition) {
        this.ecopointCondition = ecopointCondition;
    }
    public String getEcopointAddress() {
        return ecopointAddress;
    }
    public void setEcopointAddress(String ecopointAddress) {
        this.ecopointAddress = ecopointAddress;
    }
    public OperatingEcopoint getEcopointOperating(){
        return ecopointOperating;
    }
    public void setEcopointOperating(OperatingEcopoint ecopointOperating){
        this.ecopointOperating = ecopointOperating;
    }
    public String getOperatingHours() {
        return operatingHours;
    }
    public void setOperatingHours(String operatingHours) {
        this.operatingHours = operatingHours;
    }
    public GeoJsonPoint getLocation() {
        return location;
    }
    public void setLocation(GeoJsonPoint location) {
        this.location = location;
    }
    public List<AcceptedMaterial> getAcceptedMaterials() {
        return acceptedMaterials;
    }
    public void setAcceptedMaterials(List<AcceptedMaterial> acceptedMaterials) {
        this.acceptedMaterials = acceptedMaterials;
    }
    public void setEcopointName(String ecopointName) {
        this.ecopointName = ecopointName;
    }

    public void setEcopointLocation(GPSLocations ecopointLocation) {
        this.ecopointLocation = ecopointLocation;
    }
    public void setId(String id) {
        this.id = id;
    }
}