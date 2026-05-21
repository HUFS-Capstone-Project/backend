package com.hufs.capstone.backend.link.api.controller.swagger;

import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.link.api.request.CreateLinkAnalysisRequest;
import com.hufs.capstone.backend.link.api.request.OverrideLinkCandidateRequest;
import com.hufs.capstone.backend.link.api.request.SaveManualRoomPlaceRequest;
import com.hufs.capstone.backend.link.api.request.SaveRoomPlacesRequest;
import com.hufs.capstone.backend.link.api.response.LinkAnalysisRequestResponse;
import com.hufs.capstone.backend.link.api.response.LinkAnalysisResponse;
import com.hufs.capstone.backend.link.api.response.RoomLinkCandidateOverrideResponse;
import com.hufs.capstone.backend.link.api.response.RoomPlaceSaveResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/rooms/{roomId}")
@SecurityRequirement(name = "bearer-jwt")
public interface LinkApi {

	@Operation(
			tags = {"Link"},
			summary = "링크 분석 요청 생성 API",
			description = "방 멤버십을 검증한 뒤 링크 분석 후보를 생성합니다. 이 API는 방에 링크나 장소를 저장하지 않습니다."
	)
	@ApiResponse(responseCode = "201", description = "Created")
	@ApiResponse(responseCode = "200", description = "OK")
	@PostMapping("/link-analysis-requests")
	ResponseEntity<CommonResponse<LinkAnalysisRequestResponse>> createLinkAnalysisRequest(
			@PathVariable String roomId,
			@Valid @RequestBody CreateLinkAnalysisRequest request,
			@Parameter(description = "CSRF 토큰 헤더 값")
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);

	@Operation(
			tags = {"Link"},
			summary = "링크 분석 결과 조회 API",
			description = "방 멤버십과 analysisRequestId의 방 소속을 검증한 뒤 processing resolved_places 기반 분석 상태와 후보 장소를 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/link-analysis-requests/{analysisRequestId}")
	CommonResponse<LinkAnalysisResponse> getLinkAnalysis(
			@PathVariable String roomId,
			@PathVariable Long analysisRequestId
	);

	@Operation(
			tags = {"Link"},
			summary = "링크 분석 요청 재시도 API",
			description = "재시도 가능한 링크 분석 요청을 새로 생성하지 않고 기존 요청을 다시 분석합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@PostMapping("/link-analysis-requests/{analysisRequestId}/retry")
	CommonResponse<LinkAnalysisRequestResponse> retryLinkAnalysisRequest(
			@PathVariable String roomId,
			@PathVariable Long analysisRequestId,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);

	@Operation(
			tags = {"Link"},
			summary = "링크 분석 후보 장소 저장 API",
			description = "선택한 후보 장소를 저장합니다. 이 순간 RoomLink가 생성되며, 장소는 RoomPlaceSource를 통해 해당 링크 출처와 연결됩니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@PostMapping("/link-analysis-requests/{analysisRequestId}/places")
	CommonResponse<RoomPlaceSaveResponse> saveRoomPlaces(
			@PathVariable String roomId,
			@PathVariable Long analysisRequestId,
			@Valid @RequestBody SaveRoomPlacesRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);

	@Operation(
			tags = {"Link"},
			summary = "링크 분석 후보 장소 수정 API",
			description = "전역 Link 원본 후보를 변경하지 않고, 현재 방의 RoomLink 컨텍스트에 후보 override를 저장합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@PutMapping("/link-analysis-requests/{analysisRequestId}/candidates/{candidateId}/override")
	CommonResponse<RoomLinkCandidateOverrideResponse> overrideLinkCandidate(
			@PathVariable String roomId,
			@PathVariable Long analysisRequestId,
			@PathVariable Long candidateId,
			@Valid @RequestBody OverrideLinkCandidateRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);

	@Operation(
			tags = {"Link"},
			summary = "링크 분석 수동 검색 장소 저장 API",
			description = "분석 실패 또는 후보 없음 상태에서도 카카오 검색으로 직접 선택한 장소를 해당 링크 출처로 저장합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@PostMapping("/link-analysis-requests/{analysisRequestId}/places/manual")
	CommonResponse<RoomPlaceSaveResponse> saveManualRoomPlace(
			@PathVariable String roomId,
			@PathVariable Long analysisRequestId,
			@Valid @RequestBody SaveManualRoomPlaceRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);
}
