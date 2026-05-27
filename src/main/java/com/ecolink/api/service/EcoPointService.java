package com.ecolink.api.service;

import com.ecolink.api.config.EcopointWasNotFoundException;
import com.ecolink.api.dto.*;
import com.ecolink.api.model.Ecopoint;
import com.ecolink.api.model.enums.StatusEcopoint;
import com.ecolink.api.model.enums.ConditionEcopoint;
import com.ecolink.api.repository.EcopointRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

//Håndtere logik for Ecopoints
@Service
public class EcoPointService {

    private final EcopointRepository repository;
    private final MongoTemplate mongoTemplate;

    public EcoPointService(EcopointRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }
    //Henter liste af Ecopoints. Sortere med Geosortering hvis koordinater er givet eller alfabetisk.
    public Page<EcopointListItemDTO> getEcoPoints(Double lat, Double lng, int page, int limit) {

        PageRequest pageable = PageRequest.of(page - 1, limit, Sort.by("ecopointName").ascending());

        if (lat != null && lng != null) {
            NearQuery nearQuery = NearQuery
                    .near(new Point(lng, lat), Metrics.KILOMETERS)
                    .spherical(true)
                    .limit(limit)
                    .skip((long) (page - 1) * limit);

            GeoResults<Ecopoint> results = mongoTemplate.geoNear(nearQuery, Ecopoint.class);

            List<EcopointListItemDTO> items = new ArrayList<>();
            for (GeoResult<Ecopoint> geoResult : results.getContent()) {
                Ecopoint ep = geoResult.getContent();
                double distanceKm = geoResult.getDistance().getValue();

                items.add(new EcopointListItemDTO(
                        ep.getEcopointID(),
                        ep.getEcopointName(),
                        ep.getEcopointAddress(),
                        distanceKm,
                        null,
                        ep.getEcopointStatus() != null ? ep.getEcopointStatus().toString() : null,
                        null
                ));
            }

            long total = results.getContent().size();
            return new PageImpl<>(items, PageRequest.of(page - 1, limit), total);

        } else {
            Page<Ecopoint> ecoPoints = repository.findAll(pageable);

            return ecoPoints.map(ep -> new EcopointListItemDTO(
                    ep.getEcopointID(),
                    ep.getEcopointName(),
                    ep.getEcopointAddress(),
                    null,
                    null,
                    ep.getEcopointStatus() != null ? ep.getEcopointStatus().toString() : null,
                    null
            ));
        }
    }
    //Henter hvert enkelt Ecopoint og giver besked hvis ikke fundet by ID.
    public EcopointDetailDTO getEcopointById(String id) {
        Ecopoint ep = repository.findById(id)
                .orElseThrow(() -> new EcopointWasNotFoundException(id));

        double latitude = 0.0;
        double longitude = 0.0;
        if (ep.getEcopointLocation() != null) {
            latitude = ep.getEcopointLocation().getLatitude();
            longitude = ep.getEcopointLocation().getLongitude();
        } else if (ep.getLocation() != null) {
            longitude = ep.getLocation().getX();
            latitude = ep.getLocation().getY();
        }
        StatusEcopoint status = null;
        ConditionEcopoint condition = null;
        if (ep.getEcopointStatus() != null) {
            status = StatusEcopoint.valueOf(ep.getEcopointStatus().name());
        }
        if (ep.getEcopointCondition() != null) {
            condition = ConditionEcopoint.valueOf(ep.getEcopointCondition().name());
        }
        return EcopointDetailDTO.builder()
                .id(ep.getEcopointID())
                .name(ep.getEcopointName())
                .address(ep.getEcopointAddress())
                .coordinates(new GeoJsonPointDTO(longitude, latitude))
                .conditionEcopoint(condition)
                .status(status != null ? status.toString() : null)
                .operatingHours(ep.getOperatingHours())
                .operating(ep.getEcopointOperating() != null ?
                        ep.getEcopointOperating().toString() : null)
                .acceptedMaterials(ep.getAcceptedMaterials())
                .build();
    }
}
