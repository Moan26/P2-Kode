package com.ecolink.api.model.enums;

public enum MaterialType {
    PLASTIC,
    GLASS,
    PAPER_CARDBOARD,
    METAL_ALUMINUM,
    ELECTRONICS,
    OTHER;

    public String getLabel() {
        return switch (this) {
            case PLASTIC         -> "Plastic";
            case GLASS           -> "Glass";
            case PAPER_CARDBOARD -> "Paper / Cardboard";
            case METAL_ALUMINUM  -> "Metal / Aluminum";
            case ELECTRONICS     -> "Electronics";
            case OTHER           -> "Other";
        };
    }
}