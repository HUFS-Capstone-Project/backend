package com.hufs.capstone.backend.course.api.controller;

import com.hufs.capstone.backend.auth.security.SecurityUtils;
import com.hufs.capstone.backend.course.api.controller.swagger.MeDateCourseApi;
import com.hufs.capstone.backend.course.api.response.MyDateCoursePageResponse;
import com.hufs.capstone.backend.course.application.DateCourseQueryService;
import com.hufs.capstone.backend.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeDateCourseController implements MeDateCourseApi {

	private final DateCourseQueryService queryService;

	@Override
	public CommonResponse<MyDateCoursePageResponse> listMyDateCourses(
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer limit
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		return CommonResponse.ok(
				MyDateCoursePageResponse.from(queryService.listMySavedCourses(userId, page, limit))
		);
	}
}
