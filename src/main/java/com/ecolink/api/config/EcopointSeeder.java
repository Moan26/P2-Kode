package com.ecolink.api.config;

import com.ecolink.api.model.Ecopoint;
import com.ecolink.api.model.GPSLocations;
import com.ecolink.api.model.enums.StatusEcopoint;
import com.ecolink.api.model.enums.ConditionEcopoint;
import com.ecolink.api.repository.EcopointRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
//simuleret testdata med rigtige locationer i databasen lokalt. Kan kun bruges med @Profile("dev").
@Profile("dev")
public class EcopointSeeder implements CommandLineRunner {
    private final EcopointRepository repository;

    public EcopointSeeder(EcopointRepository repository){
        this.repository = repository;
    }

    @Override
    public void run(String... args){
        //Indsætter ikke testdata hvis databasen har indhold i forvejen.
        if (repository.count() > 0) return;

        record SeedPoint(double lat, double lng, StatusEcopoint status, ConditionEcopoint condition) {}

        Map<String, SeedPoint> points = new LinkedHashMap<>();
        points.put("AAU København Hovedindgang",  new SeedPoint(55.6505052, 12.5428032, StatusEcopoint.NOTFULL, ConditionEcopoint.WORKS));
        points.put("AAU København B-bygning",     new SeedPoint(55.6496122, 12.5417175, StatusEcopoint.NOTFULL, ConditionEcopoint.WORKS));
        points.put("AAU CCT Grupperum",           new SeedPoint(55.6498480, 12.5415099, StatusEcopoint.FULL,    ConditionEcopoint.WORKS));
        points.put("AAU C-bygning Indgang",       new SeedPoint(55.6491063, 12.5421807, StatusEcopoint.NOTFULL, ConditionEcopoint.BROKEN));
        points.put("AAU København Globegangen",   new SeedPoint(55.6503206, 12.5426869, StatusEcopoint.NOTFULL, ConditionEcopoint.WORKS));
        points.put("AAU København Klimagangen",   new SeedPoint(55.6506660, 12.5430127, StatusEcopoint.FULL,    ConditionEcopoint.WORKS));
        points.put("AAU København Pool Lokale",   new SeedPoint(55.6502322, 12.5432527, StatusEcopoint.NOTFULL, ConditionEcopoint.WORKS));
        points.put("AAU København Kantine",       new SeedPoint(55.6505033, 12.5433225, StatusEcopoint.NOTFULL, ConditionEcopoint.WORKS));

        for (Map.Entry<String, SeedPoint> entry : points.entrySet()) {
            SeedPoint sp = entry.getValue();
            Ecopoint ep = new Ecopoint();
            ep.setEcopointName(entry.getKey());
            ep.setEcopointLocation(new GPSLocations(sp.lat(), sp.lng()));
            ep.setLocation(new GeoJsonPoint(sp.lng(), sp.lat()));
            ep.setEcopointStatus(sp.status());
            ep.setEcopointCondition(sp.condition());
            repository.save(ep);
        }
    }
}
