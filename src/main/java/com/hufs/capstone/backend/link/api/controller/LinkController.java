package com.hufs.capstone.backend.link.api.controller;

import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.auth.security.SecurityUtils;
import com.hufs.capstone.backend.link.api.controller.swagger.LinkApi;
import com.hufs.capstone.backend.link.api.request.RegisterLinkRequest;
import com.hufs.capstone.backend.link.api.response.LinkStatusResponse;
import com.hufs.capstone.backend.link.api.response.RegisterLinkResponse;
import com.hufs.capstone.backend.link.application.LinkCommandService;
import com.hufs.capstone.backend.link.application.LinkQueryService;
import com.hufs.capstone.backend.link.application.dto.LinkStatusResult;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkCommand;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkResult;
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

	private final LinkCommandService linkCommandService;
	private final LinkQueryService linkQueryService;

	@Override
	public ResponseEntity<CommonResponse<RegisterLinkResponse>> register(
			@Valid @RequestBody RegisterLinkRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		RegisterLinkResult result = linkCommandService.register(
				userId,
				new RegisterLinkCommand(request.url(), request.roomId(), request.source())
		);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{linkId}")
				.buildAndExpand(result.linkId())
				.toUri();
		return ResponseEntity.created(location).body(CommonResponse.ok(RegisterLinkResponse.from(result)));
	}

	@Override
	public CommonResponse<LinkStatusResponse> getLink(@PathVariable Long linkId) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		LinkStatusResult result = linkQueryService.getLinkStatus(userId, linkId);
		return CommonResponse.ok(LinkStatusResponse.from(result));
	}
}
