package com.ecolink.api.model.embedded;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatingHoursEntry {
    private String open;
    private String close;
    private boolean closed;
}