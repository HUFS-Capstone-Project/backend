package com.hufs.capstone.backend.link.api.controller;

import com.hufs.capstone.backend.auth.security.SecurityUtils;
import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.link.api.controller.swagger.LinkApi;
import com.hufs.capstone.backend.link.api.request.CreateLinkAnalysisRequest;
import com.hufs.capstone.backend.link.api.request.OverrideLinkCandidateRequest;
import com.hufs.capstone.backend.link.api.request.SaveManualRoomPlaceRequest;
import com.hufs.capstone.backend.link.api.request.SaveRoomPlacesRequest;
import com.hufs.capstone.backend.link.api.response.LinkAnalysisRequestResponse;
import com.hufs.capstone.backend.link.api.response.LinkAnalysisResponse;
import com.hufs.capstone.backend.link.api.response.RoomLinkCandidateOverrideResponse;
import com.hufs.capstone.backend.link.api.response.RoomPlaceSaveResponse;
import com.hufs.capstone.backend.link.application.LinkAnalysisRequestService;
import com.hufs.capstone.backend.link.application.LinkAnalysisStatusService;
import com.hufs.capstone.backend.link.application.RoomLinkCandidateOverrideService;
import com.hufs.capstone.backend.link.application.RoomPlaceCommandService;
import com.hufs.capstone.backend.link.application.dto.AnalyzeLinkCommand;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisRequestResult;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import com.hufs.capstone.backend.link.application.dto.RoomLinkCandidateOverrideResult;
import com.hufs.capstone.backend.link.application.dto.SaveManualRoomPlaceCommand;
import com.hufs.capstone.backend.link.application.dto.SaveRoomPlacesCommand;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceSaveResult;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class LinkController implements LinkApi {

	private final LinkAnalysisRequestService linkAnalysisRequestService;
	private final LinkAnalysisStatusService linkAnalysisStatusService;
	private final RoomPlaceCommandService roomPlaceCommandService;
	private final RoomLinkCandidateOverrideService roomLinkCandidateOverrideService;

	@Override
	public ResponseEntity<CommonResponse<LinkAnalysisRequestResponse>> createLinkAnalysisRequest(
			@PathVariable String roomId,
			@Valid @RequestBody CreateLinkAnalysisRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		LinkAnalysisRequestResult result = linkAnalysisRequestService.requestLinkAnalysis(
				userId,
				roomId,
				new AnalyzeLinkCommand(request.originalUrl(), request.source())
		);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.replacePath("/api/v1/rooms/{roomId}/link-analysis-requests/{analysisRequestId}")
				.buildAndExpand(roomId, result.analysisRequestId())
				.toUri();
		CommonResponse<LinkAnalysisRequestResponse> body =
				CommonResponse.ok(LinkAnalysisRequestResponse.from(result));
		if (result.createdRequest()) {
			return ResponseEntity.created(location).body(body);
		}
		return ResponseEntity.ok(body);
	}

	@Override
	public CommonResponse<LinkAnalysisResponse> getLinkAnalysis(
			@PathVariable String roomId,
			@PathVariable Long analysisRequestId
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(userId, roomId, analysisRequestId);
		return CommonResponse.ok(LinkAnalysisResponse.from(result));
	}

	@Override
	public CommonResponse<RoomPlaceSaveResponse> saveRoomPlaces(
			@PathVariable String roomId,
			@PathVariable Long analysisRequestId,
			@Valid @RequestBody SaveRoomPlacesRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		RoomPlaceSaveResult result = roomPlaceCommandService.saveRoomPlaces(
				userId,
				roomId,
				analysisRequestId,
				new SaveRoomPlacesCommand(request.kakaoPlaceIds())
		);
		return CommonResponse.ok(RoomPlaceSaveResponse.from(result));
	}

	@Override
	public CommonResponse<RoomLinkCandidateOverrideResponse> overrideLinkCandidate(
			@PathVariable String roomId,
			@PathVariable Long analysisRequestId,
			@PathVariable Long candidateId,
			@Valid @RequestBody OverrideLinkCandidateRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		RoomLinkCandidateOverrideResult result = roomLinkCandidateOverrideService.overrideCandidate(
				userId,
				roomId,
				analysisRequestId,
				candidateId,
				request.toSnapshot()
		);
		return CommonResponse.ok(RoomLinkCandidateOverrideResponse.from(result));
	}

	@Override
	public CommonResponse<RoomPlaceSaveResponse> saveManualRoomPlace(
			@PathVariable String roomId,
			@PathVariable Long analysisRequestId,
			@Valid @RequestBody SaveManualRoomPlaceRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		RoomPlaceSaveResult result = roomPlaceCommandService.saveManualRoomPlace(
				userId,
				roomId,
				analysisRequestId,
				new SaveManualRoomPlaceCommand(request.toSnapshot())
		);
		return CommonResponse.ok(RoomPlaceSaveResponse.from(result));
	}
}
