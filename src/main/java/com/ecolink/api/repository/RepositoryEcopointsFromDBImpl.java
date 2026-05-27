package com.ecolink.api.repository;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.stage;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import com.ecolink.api.model.Ecopoint;

public class RepositoryEcopointsFromDBImpl implements RepositoryEcopointsFromDBCustom {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public RepositoryEcopointsFromDBImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Ecopoint> findNearby(double lng, double lat, double radiusKm) {
        double radiusMeters = radiusKm * 1000;

        try {
            Aggregation aggregation = newAggregation(
                    stage(String.format(Locale.US, """
                                { $geoNear: {
                                    near: { type: "Point", coordinates: [%f, %f] },
                                    distanceField: "distance",
                                    maxDistance: %f,
                                    spherical: true,
                                    key: "location"
                                }}
                            """, lng, lat, radiusMeters)),
                    Aggregation.limit(50));

            AggregationResults<Ecopoint> results = mongoTemplate.aggregate(
                    aggregation, "ecopoints", Ecopoint.class);

            return results.getMappedResults();

        } catch (Exception e) {
            e.printStackTrace(); // printer til konsollen
            throw e;
        }
    }

    @Override
    public List<Ecopoint> findWithinBounds(double swLng, double swLat, double neLng, double neLat, int limit) {
        // Bygger en $geoWithin + $box query der finder alle EcoPoints inden for
        // viewport'en
        try {
            Aggregation aggregation = newAggregation(
                    stage(String.format(Locale.US, """
                                { $match: {
                                    "location": {
                                        $geoWithin: {
                                            $box: [
                                                [%f, %f],
                                                [%f, %f]
                                            ]
                                        }
                                    }
                                }}
                            """, swLng, swLat, neLng, neLat)),
                    Aggregation.limit(limit));

            AggregationResults<Ecopoint> results = mongoTemplate.aggregate(
                    aggregation, "ecopoints", Ecopoint.class);

            return results.getMappedResults();

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
