package com.ecolink.api.model;

public class GPSLocations {
	private double latitude;
	private double longitude;

	public GPSLocations(double latitude, double longitude) {
		if(latitude < -90 || latitude > 90) {
			throw new IllegalArgumentException("Needs to be from -90 to 90");
		}
		if(longitude < -180 || longitude > 180) {
			throw new IllegalArgumentException("Needs to be from -180 to 180");
		}
	    this.latitude = latitude;
	    this.longitude = longitude;
	}
	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	
	
	
}
