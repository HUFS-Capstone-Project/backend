package com.hufs.capstone.backend.course.api.controller.swagger;

import com.hufs.capstone.backend.course.api.request.DateCourseGenerationRequest;
import com.hufs.capstone.backend.course.api.response.DateCourseBatchResponse;
import com.hufs.capstone.backend.course.api.response.DateCourseGenerationResponse;
import com.hufs.capstone.backend.course.api.response.DateCourseResponse;
import com.hufs.capstone.backend.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping("/api/v1/rooms/{roomId}/date-courses")
@SecurityRequirement(name = "bearer-jwt")
public interface DateCourseApi {

	@Operation(
			tags = {"Date course"},
			summary = "데이트 코스 생성 API",
			description = "방에 저장된 장소를 바탕으로 General/Trendy/Popular 3가지 코스를 생성하고 저장합니다."
	)
	@ApiResponse(responseCode = "201", description = "코스 생성 성공")
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	CommonResponse<DateCourseGenerationResponse> generateCourse(
			@PathVariable String roomId,
			@Valid @RequestBody DateCourseGenerationRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);

	@Operation(
			tags = {"Date course"},
			summary = "데이트 코스 목록 조회 API",
			description = "방에서 생성된 데이트 코스를 생성 배치(batchId) 단위로 최신순 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping
	CommonResponse<List<DateCourseBatchResponse>> listCourses(
			@PathVariable String roomId
	);

	@Operation(
			tags = {"Date course"},
			summary = "데이트 코스 상세 조회 API",
			description = "특정 코스의 장소 목록을 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/{coursePublicId}")
	CommonResponse<DateCourseResponse> getCourse(
			@PathVariable String roomId,
			@PathVariable String coursePublicId
	);
}
