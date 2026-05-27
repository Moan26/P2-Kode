package com.ecolink.api.dto;

import com.ecolink.api.model.AcceptedMaterial;
import com.ecolink.api.model.enums.ConditionEcopoint;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detail info for Every single Ecopoint")
//Bruges af GET/api/ecopoints/{id} til at hente infomationer fra.
public class EcopointDetailDTO {

    private String address;
    private ConditionEcopoint conditionEcopoint;
    //GeoJSON coordinat format.
    @Schema(description = "GeoJson format [long, lat]")
    private GeoJsonPointDTO coordinates;
    private String id;
    private String operatingHours;
    private String operating;
    private String thumbnailURL;
    private String name;
    private List<AcceptedMaterial> acceptedMaterials;
    private String status;
}
