package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * processing 서버 job 상태 조회 API 응답을 adapter 레벨에서 표현한다.
 * TODO: 외부 OpenAPI/실제 응답과 맞춰 필드를 추가하고, application 계층 DTO로 승격할 때 매핑 규칙을 둔다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessingJobResponse(
		@JsonProperty("job_id")
		String jobId,
		String status,
		@JsonProperty("source_url")
		String sourceUrl,
		@JsonProperty("room_id")
		String roomId,
		String source,
		@JsonProperty("error_code")
		String errorCode,
		@JsonProperty("error_message")
		String errorMessage
) {
}
