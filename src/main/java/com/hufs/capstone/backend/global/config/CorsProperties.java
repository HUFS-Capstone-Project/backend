package com.hufs.capstone.backend.global.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

	private List<String> allowedOrigins = List.of();
	private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
	private List<String> allowedHeaders = List.of(
			"Authorization",
			"Content-Type",
			"X-Requested-With",
			"X-XSRF-TOKEN",
			"X-Client-Platform"
	);
	private List<String> exposedHeaders = List.of();
	private boolean allowCredentials = true;
	private long maxAge = 3_600;
}
