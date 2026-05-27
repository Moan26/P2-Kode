package com.ecolink.api.service;

import java.util.List;
import java.util.Map;

import com.ecolink.api.model.Ecopoint;

public interface LocationEcopointService {
    Map<String, Object> findInBounds(
            double swLat,
            double swLng,
            double neLat,
            double neLng
    );
}