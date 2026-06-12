package com.hufs.capstone.backend.course.api.controller.swagger;

import com.hufs.capstone.backend.course.api.response.MyDateCourseCursorPageResponse;
import com.hufs.capstone.backend.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/v1/users/me/date-courses")
@SecurityRequirement(name = "bearer-jwt")
public interface MeDateCourseApi {

	@Operation(
			tags = {"My date course"},
			summary = "내 저장 데이트 코스 목록 조회 API",
			description = "현재 사용자가 저장한 데이트 코스를 최신 저장순으로 페이지 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping
	CommonResponse<MyDateCourseCursorPageResponse> listMyDateCourses(
			@RequestParam(required = false) Integer limit,
			@RequestParam(required = false) String cursor
	);
}
