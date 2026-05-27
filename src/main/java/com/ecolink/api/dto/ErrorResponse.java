package com.ecolink.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ErrorResponse {
    private boolean success;
    private int status;
    private String error;
    private String message;
    private Instant timestamp;
}