package com.ecolink.api;

import com.ecolink.api.config.EcopointWasNotFoundException;
import com.ecolink.api.dto.EcopointDetailDTO;
import com.ecolink.api.dto.EcopointListItemDTO;
import com.ecolink.api.model.Ecopoint;
import com.ecolink.api.model.GPSLocations;
import com.ecolink.api.model.enums.StatusEcopoint;
import com.ecolink.api.repository.EcopointRepository;
import com.ecolink.api.service.EcoPointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.NearQuery;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EcoPointServiceTest {

    private EcopointRepository repository;
    private MongoTemplate mongoTemplate;
    private EcoPointService ecoPointService;

    @BeforeEach
    void setUp() {
        repository = mock(EcopointRepository.class);
        mongoTemplate = mock(MongoTemplate.class);
        ecoPointService = new EcoPointService(repository, mongoTemplate);
    }

    @Test
    void getEcoPoints_udenKoordinater_returnerAlleAlphabetisk() {
        Page<Ecopoint> page = new PageImpl<>(List.of(
                ecopoint("id-1", "Aalborg Genbrugscenter", "Vesterbro 1"),
                ecopoint("id-2", "Brabrand Genbrugscenter", "Silkeborgvej 2")
        ));
        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        Page<EcopointListItemDTO> result = ecoPointService.getEcoPoints(null, null, 1, 10);

        assertEquals(2, result.getTotalElements());
        verify(repository).findAll(any(Pageable.class));
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void getEcoPoints_medKoordinater_brugerGeoSoegning() {
        Ecopoint ep = ecopoint("id-1", "Nærmeste Genbrugscenter", "Testvej 1");
        GeoResult<Ecopoint> geoResult = new GeoResult<>(ep, new Distance(1.2, Metrics.KILOMETERS));
        GeoResults<Ecopoint> geoResults = new GeoResults<>(List.of(geoResult));

        when(mongoTemplate.geoNear(any(NearQuery.class), eq(Ecopoint.class))).thenReturn(geoResults);

        Page<EcopointListItemDTO> result = ecoPointService.getEcoPoints(55.67, 12.57, 1, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals("Nærmeste Genbrugscenter", result.getContent().get(0).getName());
        verify(mongoTemplate).geoNear(any(NearQuery.class), eq(Ecopoint.class));
        verifyNoInteractions(repository);
    }

    @Test
    void getEcopointById_eksisterendeId_returnerDetailDTO() {
        Ecopoint ep = ecopoint("id-1", "Testcenter", "Testvej 1");
        ep.setEcopointLocation(new GPSLocations(55.67, 12.57));
        when(repository.findById("id-1")).thenReturn(Optional.of(ep));

        EcopointDetailDTO result = ecoPointService.getEcopointById("id-1");

        assertEquals("id-1", result.getId());
        assertEquals("Testcenter", result.getName());
        assertEquals("Testvej 1", result.getAddress());
        assertNotNull(result.getCoordinates());
    }

    @Test
    void getEcopointById_ukendt_id_kasterEcopointWasNotFoundException() {
        when(repository.findById("ukendt")).thenReturn(Optional.empty());

        assertThrows(EcopointWasNotFoundException.class,
                () -> ecoPointService.getEcopointById("ukendt"));
    }

    @Test
    void getEcopointById_medStatus_returnerStatusSomStreng() {
        Ecopoint ep = ecopoint("id-1", "Testcenter", "Testvej 1");
        ep.setEcopointStatus(StatusEcopoint.FULL);
        when(repository.findById("id-1")).thenReturn(Optional.of(ep));

        EcopointDetailDTO result = ecoPointService.getEcopointById("id-1");

        assertEquals("FULL", result.getStatus());
    }

    private Ecopoint ecopoint(String id, String name, String address) {
        Ecopoint ep = new Ecopoint();
        ep.setId(id);
        ep.setEcopointName(name);
        ep.setEcopointAddress(address);
        return ep;
    }
}