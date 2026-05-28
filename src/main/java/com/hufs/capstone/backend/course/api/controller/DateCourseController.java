package com.hufs.capstone.backend.course.api.controller;

import com.hufs.capstone.backend.auth.security.SecurityUtils;
import com.hufs.capstone.backend.course.api.controller.swagger.DateCourseApi;
import com.hufs.capstone.backend.course.api.request.DateCourseGenerationRequest;
import com.hufs.capstone.backend.course.api.response.DateCourseBatchResponse;
import com.hufs.capstone.backend.course.api.response.DateCourseGenerationResponse;
import com.hufs.capstone.backend.course.api.response.DateCourseResponse;
import com.hufs.capstone.backend.course.application.DateCourseGenerationService;
import com.hufs.capstone.backend.course.application.DateCourseQueryService;
import com.hufs.capstone.backend.global.response.CommonResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DateCourseController implements DateCourseApi {

	private final DateCourseGenerationService generationService;
	private final DateCourseQueryService queryService;

	@Override
	public CommonResponse<DateCourseGenerationResponse> generateCourse(
			@PathVariable String roomId,
			@Valid @RequestBody DateCourseGenerationRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		return CommonResponse.ok(
				DateCourseGenerationResponse.from(generationService.generate(request.toCommand(roomId), userId))
		);
	}

	@Override
	public CommonResponse<List<DateCourseBatchResponse>> listCourses(@PathVariable String roomId) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		return CommonResponse.ok(
				queryService.listBatches(roomId, userId).stream()
						.map(DateCourseBatchResponse::from)
						.toList()
		);
	}

	@Override
	public CommonResponse<DateCourseResponse> getCourse(
			@PathVariable String roomId,
			@PathVariable String coursePublicId
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		return CommonResponse.ok(DateCourseResponse.from(queryService.getCourse(roomId, coursePublicId, userId)));
	}
}
