package com.tvd12.ezyhttp.server.core.test.controller;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.tvd12.ezyhttp.core.exception.HttpBadRequestException;
import com.tvd12.ezyhttp.core.response.ResponseEntity;
import com.tvd12.ezyhttp.server.core.annotation.ExceptionHandler;
import com.tvd12.ezyhttp.server.core.annotation.TryCatch;

@ExceptionHandler
public class GlobalExceptionHandler {

	@TryCatch(IllegalArgumentException.class)
	public ResponseEntity handleException(Exception e) {
		return ResponseEntity.badRequest("global: " + e.getMessage());
	}
	
	@TryCatch({IllegalStateException.class, NullPointerException.class})
	public String handleException2(Exception e) {
		return e.getMessage();
	}
	
	@TryCatch(java.lang.UnsupportedOperationException.class)
	public void handleException3(Exception e) {
	}
	
	@TryCatch(InvalidFormatException.class)
	public void handleException(InvalidFormatException e) {
		InvalidFormatException ex = (InvalidFormatException)e;
		Map<String, String> data = new HashMap<>();
		for(Reference reference : ex.getPath())
			data.put(reference.getFieldName(), "invalid");
		throw new HttpBadRequestException(data);
	}
	
}
