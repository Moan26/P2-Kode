package com.ecolink.api;

import com.ecolink.api.model.Ecopoint;
import com.ecolink.api.repository.RepositoryEcopointsFromDB;
import com.ecolink.api.service.LocationServiceEcopointImplement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocationServiceEcopointImplementTest {

    private final RepositoryEcopointsFromDB repository = mock(RepositoryEcopointsFromDB.class);
    private final LocationServiceEcopointImplement service =
            new LocationServiceEcopointImplement(repository);

    @Test
    void findInBoundsThrowsWhenSwLatIsTooLow() {
        assertThrows(IllegalArgumentException.class, () ->
                service.findInBounds(-91, 9.80, 57.10, 10.00)
        );
    }

    @Test
    void findInBoundsThrowsWhenNeLatIsTooHigh() {
        assertThrows(IllegalArgumentException.class, () ->
                service.findInBounds(57.00, 9.80, 91, 10.00)
        );
    }

    @Test
    void findInBoundsThrowsWhenSwLngIsTooLow() {
        assertThrows(IllegalArgumentException.class, () ->
                service.findInBounds(57.00, -181, 57.10, 10.00)
        );
    }

    @Test
    void findInBoundsThrowsWhenNeLngIsTooHigh() {
        assertThrows(IllegalArgumentException.class, () ->
                service.findInBounds(57.00, 9.80, 57.10, 181)
        );
    }

    @Test
    void findInBoundsThrowsWhenSwLatIsGreaterThanNeLat() {
        assertThrows(IllegalArgumentException.class, () ->
                service.findInBounds(58.00, 9.80, 57.10, 10.00)
        );
    }

    @Test
    void findInBoundsThrowsWhenSwLngIsGreaterThanNeLng() {
        assertThrows(IllegalArgumentException.class, () ->
                service.findInBounds(57.00, 11.00, 57.10, 10.00)
        );
    }

    @Test
    void findInBoundsReturnsEmptyListWhenNoEcopointsFound() {
        when(repository.findWithinBounds(9.80, 57.00, 10.00, 57.10, 501))
                .thenReturn(List.of());

        Map<String, Object> result = service.findInBounds(57.00, 9.80, 57.10, 10.00);

        assertEquals(List.of(), result.get("items"));
        assertEquals(false, result.get("truncated"));
    }

    @Test
    void findInBoundsCallsRepositoryWithCorrectLngLatOrder() {
        when(repository.findWithinBounds(9.80, 57.00, 10.00, 57.10, 501))
                .thenReturn(List.of());

        service.findInBounds(57.00, 9.80, 57.10, 10.00);

        verify(repository).findWithinBounds(9.80, 57.00, 10.00, 57.10, 501);
    }
}