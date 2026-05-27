package com.ecolink.api.service;

import java.util.List;
import java.util.Map;

import com.ecolink.api.dto.EcopointMarkerDTO;
import org.springframework.stereotype.Service;

import com.ecolink.api.model.Ecopoint;
import com.ecolink.api.repository.RepositoryEcopointsFromDB;

@Service
public class LocationServiceEcopointImplement implements LocationEcopointService {

	private final RepositoryEcopointsFromDB repositoryEcopointsFromDB;
	

	public List<Ecopoint> findNearby(double longitude, double latitude, double maxDistance) {
		
		if(latitude < -90 || latitude > 90) {
			throw new IllegalArgumentException("Needs to be from -90 to 90");
		}
		if(longitude < -180 || longitude > 180) {
			throw new IllegalArgumentException("Needs to be from -180 to 180");
		}
		if (maxDistance > 50) {
			throw new IllegalArgumentException("Cant be more than 50 km  - Maximum is 50 km");
		}
		return repositoryEcopointsFromDB.findNearby(longitude, latitude, maxDistance);
	}

	public LocationServiceEcopointImplement(RepositoryEcopointsFromDB repositoryEcopointsFromDB) {
		super();
		this.repositoryEcopointsFromDB = repositoryEcopointsFromDB;
	}
	@Override
	public Map<String, Object> findInBounds(
			double swLat,
			double swLng,
			double neLat,
			double neLng

	){
		if (swLat < -90 || swLat > 90 || neLat < -90 || neLat > 90) {
			throw new IllegalArgumentException("Latitude must be between -90 and 90");
		}

		if (swLng < -180 || swLng > 180 || neLng < -180 || neLng > 180) {
			throw new IllegalArgumentException("Longitude must be between -180 and 180");
		}

		if (swLat > neLat) {
			throw new IllegalArgumentException("swLat cannot be greater than neLat");
		}

		if (swLng > neLng) {
			throw new IllegalArgumentException("swLng cannot be greater than neLng");


		}

		List<Ecopoint> ecopoints = repositoryEcopointsFromDB.findWithinBounds(
				swLng,
				swLat,
				neLng,
				neLat,
				501
		);

		boolean truncated = ecopoints.size() > 500;

		if (truncated) {
			ecopoints = ecopoints.subList(0, 500);
		}

		List<EcopointMarkerDTO> markers = ecopoints.stream()
				.map(ecopoint -> new EcopointMarkerDTO(
						ecopoint.getEcopointID(),
						ecopoint.getEcopointName(),
						ecopoint.getLocation().getCoordinates(),
						ecopoint.getEcopointStatus().toString(),
						ecopoint.getEcopointOperating().toString()
				))
				.toList();

		return Map.of(
				"items", markers,
				"truncated", truncated
		);
	}

}
