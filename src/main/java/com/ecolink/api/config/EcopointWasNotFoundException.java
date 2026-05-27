package com.ecolink.api.config;
//Bruges i Fejlhåndteringer
public class EcopointWasNotFoundException extends RuntimeException {
    public EcopointWasNotFoundException(String id) {
        super("EcoPoint Was not found: " + id);
    }
}
