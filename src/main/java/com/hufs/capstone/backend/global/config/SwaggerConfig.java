package com.hufs.capstone.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!prod")
public class SwaggerConfig {

	@Bean
	public OpenAPI udiduraOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("어디더라 API")
						.version("v1")
						.description("인스타 릴스 공유 기반 데이트 코스 추천 서비스 어디더라 API"))
				.components(new Components()
						.addSecuritySchemes("bearer-jwt",
								new SecurityScheme()
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")
										.description("Authorization: Bearer {access_token}")));
	}
}
