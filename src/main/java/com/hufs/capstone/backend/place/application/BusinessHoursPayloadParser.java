package com.hufs.capstone.backend.place.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class BusinessHoursPayloadParser {

	private final ObjectMapper objectMapper;

	JsonNode parse(String businessHoursJson) throws JsonProcessingException {
		return objectMapper.readTree(businessHoursJson);
	}
}
