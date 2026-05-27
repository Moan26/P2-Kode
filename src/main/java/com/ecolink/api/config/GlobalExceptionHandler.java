package com.ecolink.api.config;

import java.time.Instant;
import java.util.Map;

import com.ecolink.api.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
//Fejl beskeder hvis der opstår fejl.
public class GlobalExceptionHandler {

	//400: Ikke gyldigt format.
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<?> handleBadFormat(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of(
				"success", false,
				"message", "something whent wrong. Try another format."
			));
	}
	//413: For stor fil.
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<?> handleTooBigFile(IllegalStateException ex) {
		return ResponseEntity.status(413).body(Map.of(
				"success", false,
				"message", "something whent wrong. The file was too big."
			));	
	}

	//404: Ecopoint blev ikke fudnet.
	@ExceptionHandler(EcopointWasNotFoundException.class)
	public ResponseEntity<?> handlerEcopointWasNotFound(EcopointWasNotFoundException ex){
		return ResponseEntity.status(404).body(Map.of(
				"success", false,
				"message", ex.getMessage()
		));
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
		ErrorResponse response = ErrorResponse.builder()
				.success(false)
				.status(400)
				.error("Bad Request")
				.message("Missing request part: " + ex.getRequestPartName())
				.timestamp(Instant.now())
				.build();

		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException ex) {

		ErrorResponse response = ErrorResponse.builder()
				.success(false)
				.status(400)
				.error("Bad Request")
				.message("Missing parameter: " + ex.getParameterName())
				.timestamp(Instant.now())
				.build();

		return ResponseEntity.badRequest().body(response);
	}
}
