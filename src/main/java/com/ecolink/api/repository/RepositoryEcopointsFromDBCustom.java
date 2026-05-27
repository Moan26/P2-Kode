package com.ecolink.api.repository;

import java.util.List;
import com.ecolink.api.model.Ecopoint;

public interface RepositoryEcopointsFromDBCustom {
    List<Ecopoint> findNearby(double lng, double lat, double radiusKm);

    List<Ecopoint> findWithinBounds(
            double swLng,
            double swLat,
            double neLng,
            double neLat,
            int limit
    );
}