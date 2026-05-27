package com.ecolink.api.dto;

public class ClosestEcopointSearch {

	private double Latitude;
	private double Longitude;
	private double RadiusInKM;
	
public ClosestEcopointSearch(double latitude, double longitude, double radiusInKM) {
	
	if(radiusInKM < 10) {
		throw new IllegalArgumentException("Radius can't be less than 10 km or less than 10 km");
	}
	
	Latitude = latitude;
	Longitude = longitude;
	RadiusInKM = radiusInKM;
}

public double getLatitude() {
	return Latitude;
}
public double getLongitude() {
	return Longitude;
}
public double getRadiusInKM() {
	return RadiusInKM;
}
public void setRadiusInKM(double radiusInKM) {
	RadiusInKM = radiusInKM;
}

}