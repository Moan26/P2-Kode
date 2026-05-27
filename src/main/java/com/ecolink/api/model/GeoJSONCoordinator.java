package com.ecolink.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
@Data
@NoArgsConstructor(force = true)
public class GeoJSONCoordinator {

    private final String geoJson; // final bruger vi når man ikke skal ændre værdien igen.

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)  // bruger vi til vores nearsphere 2d
    private final GPSLocations location;
}