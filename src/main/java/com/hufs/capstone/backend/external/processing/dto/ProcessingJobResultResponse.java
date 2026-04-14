package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * processing 서버 job 결과 조회 API 응답을 adapter 레벨에서 표현한다.
 * application/도메인 모델이 정해지면 변환 레이어에서 구조화할 것.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessingJobResultResponse(
		@JsonProperty("media_type")
		String mediaType,
		String caption,
		@JsonProperty("instagram_meta")
		Map<String, Object> instagramMeta,
		@JsonProperty("raw_candidates")
		List<Object> rawCandidates,
		List<Object> places,
		@JsonProperty("kakao_raw")
		Map<String, Object> kakaoRaw
) {
}
