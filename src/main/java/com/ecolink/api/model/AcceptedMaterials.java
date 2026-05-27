package com.ecolink.api.model;

public class AcceptedMaterials {

	private String Aluminium;
	private String Plastic;
	private String Material;
	
	public AcceptedMaterials() {
		if(!Material.equals ("Plastic") && !Material.equals ("Aluminium")) {
			throw new IllegalArgumentException("Only Aluminium or Plastic, try again");
		}
	}

	public String getAluminium() {
		return Aluminium;
	}

	public void setAluminium(String aluminium) {
		Aluminium = aluminium;
	}

	public String getPlastic() {
		return Plastic;
	}

	public void setPlastic(String plastic) {
		Plastic = plastic;
	}
	
}
