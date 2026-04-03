package com.hufs.capstone.backend.global.api.controller;

import com.hufs.capstone.backend.global.api.controller.swagger.HealthCheckApi;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController implements HealthCheckApi {

	@Value("${spring.application.name}")
	private String applicationName;

	@Override
	public Map<String, Object> healthCheck() {
		return Map.of(
				"health", "UP",
				"timestamp", Instant.now().toString(),
				"application", applicationName
		);
	}
}
