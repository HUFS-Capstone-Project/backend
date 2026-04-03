package com.hufs.capstone.backend.global.api.controller.swagger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Health")
@RequestMapping("/api/v1/health")
public interface HealthCheckApi {

	@Operation(
			summary = "헬스 체크",
			description = "L7/클라이언트용 상태 확인. 인프라 프로브는 /actuator/health 를 사용한다."
	)
	@ApiResponse(
			responseCode = "200",
			description = "정상",
			content = @Content(
					schema = @Schema(implementation = Map.class),
					examples = @ExampleObject(
							value = "{\"health\":\"UP\",\"timestamp\":\"2026-04-03T12:00:00Z\",\"application\":\"udidura-backend\"}"
					)
			)
	)
	@GetMapping
	Map<String, Object> healthCheck();
}
