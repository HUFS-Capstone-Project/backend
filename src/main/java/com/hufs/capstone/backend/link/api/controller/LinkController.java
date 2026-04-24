package com.hufs.capstone.backend.link.api.controller;

import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.auth.security.SecurityUtils;
import com.hufs.capstone.backend.link.api.controller.swagger.LinkApi;
import com.hufs.capstone.backend.link.api.request.AnalyzeLinkRequest;
import com.hufs.capstone.backend.link.api.response.LinkAnalysisRequestResponse;
import com.hufs.capstone.backend.link.api.response.LinkAnalysisResponse;
import com.hufs.capstone.backend.link.application.LinkAnalysisRequestService;
import com.hufs.capstone.backend.link.application.LinkAnalysisStatusService;
import com.hufs.capstone.backend.link.application.dto.AnalyzeLinkCommand;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisRequestResult;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
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

	@Override
	public ResponseEntity<CommonResponse<LinkAnalysisRequestResponse>> analyzeLink(
			@PathVariable String roomId,
			@Valid @RequestBody AnalyzeLinkRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		LinkAnalysisRequestResult result = linkAnalysisRequestService.requestLinkAnalysis(
				userId,
				roomId,
				new AnalyzeLinkCommand(request.url(), request.source())
		);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.replacePath("/api/v1/rooms/{roomId}/links/{linkId}/analysis")
				.buildAndExpand(roomId, result.linkId())
				.toUri();
		CommonResponse<LinkAnalysisRequestResponse> body =
				CommonResponse.ok(LinkAnalysisRequestResponse.from(result));
		if (result.createdRequest()) {
			return ResponseEntity.created(location).body(body);
		}
		return ResponseEntity.ok(body);
	}

	@Override
	public CommonResponse<LinkAnalysisResponse> getLinkAnalysis(@PathVariable String roomId, @PathVariable Long linkId) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(userId, roomId, linkId);
		return CommonResponse.ok(LinkAnalysisResponse.from(result));
	}
}
