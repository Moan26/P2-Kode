package com.ecolink.api.controller;

import com.ecolink.api.dto.*;
import com.ecolink.api.service.EcoPointService;
import com.ecolink.api.service.ImageService;
import com.ecolink.api.service.LocationEcopointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;


//Håndtere HTTP request for EcoPoints
@RestController
@RequestMapping("/api")
public class EcopointController {


	//Using constructor injection
	private final EcoPointService ecoPointService;
	private final ImageService imageService;
	private final LocationEcopointService locationService;

	public EcopointController(
			EcoPointService ecoPointService,
			ImageService imageService,
			LocationEcopointService locationService) {
		this.ecoPointService = ecoPointService;
		this.imageService = imageService;
		this.locationService = locationService;
	}

	@GetMapping("/test")
	public String test() {
		return "API is running";
	}
	//Sortere en liste af EcoPoints efter navn og eller Geo sortering ved lat og long.
	@GetMapping("/ecopoints")
	public ResponseEntity<?> getEcoPoints(
			@RequestParam(required = false) Double lat,
			@RequestParam(required = false) Double lng,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int limit) {

		if (page < 1 || limit < 1 || limit > 50) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", "Invalid pagination parameters"
			));
		}

		Page<EcopointListItemDTO> result = ecoPointService.getEcoPoints(lat, lng, page, limit);

		return ResponseEntity.ok(Map.of(
				"success", true,
				"data", result.getContent(),
				"pagination", Map.of(
						"page", page,
						"limit", limit,
						"total", result.getTotalElements(),
						"totalPages", result.getTotalPages()
				)
		));
	}
	@Operation(summary = "Get EcoPoint by ID", description = "Returns a single EcoPoint. Coordinates are GeoJSON [longitude, latitude].")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "EcoPoint found"),
			@ApiResponse(responseCode = "400", description = "Invalid ObjectId format"),
			@ApiResponse(responseCode = "404", description = "EcoPoint not found")
	})
	//Validere om id er gyldigt mongoDB ObjectId og returnere detaljer for hvert enkelt EcoPoint.
	@GetMapping("/ecopoints/{id}")
	public ResponseEntity<?> getEcopointId(@PathVariable("id") String id) {
		if (!ObjectId.isValid(id)) {
			return ResponseEntity.badRequest().body(new APIResponse(false, null, "Invalid Ecopoint ID format"));
		}
		EcopointDetailDTO dto = ecoPointService.getEcopointById(id);
		return ResponseEntity.ok(new APIResponse(true, dto, "Ecopoint found by ID"));
	}
	@GetMapping("/ecopoints/bounds")
	public ResponseEntity<?> getEcopointsInBounds(
			@RequestParam Double swLat,
			@RequestParam Double swLng,
			@RequestParam Double neLat,
			@RequestParam Double neLng) {

		try {
			Map<String, Object> result = locationService.findInBounds(
					swLat,
					swLng,
					neLat,
					neLng
			);

			return ResponseEntity.ok(
					new APIResponse(true, result, "EcoPoints found within bounds")
			);

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(
					new APIResponse(false, null, e.getMessage())
			);
		}
	}

}
