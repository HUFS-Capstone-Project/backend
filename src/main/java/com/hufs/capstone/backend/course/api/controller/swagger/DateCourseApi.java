package com.hufs.capstone.backend.course.api.controller.swagger;

import com.hufs.capstone.backend.course.api.request.DateCourseGenerationRequest;
import com.hufs.capstone.backend.course.api.response.DateCourseGenerationResponse;
import com.hufs.capstone.backend.course.api.response.DateCoursePageResponse;
import com.hufs.capstone.backend.course.api.response.DateCourseResponse;
import com.hufs.capstone.backend.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping("/api/v1/rooms/{roomId}/date-courses")
@SecurityRequirement(name = "bearer-jwt")
public interface DateCourseApi {

	@Operation(
			tags = {"Date course"},
			summary = "데이트 코스 추천 생성 API",
			description = "방에 저장된 장소를 기반으로 GENERAL/TRENDY/POPULAR 코스 후보를 생성합니다. "
					+ "이미 저장된 코스와 동일한 장소 순서의 후보는 제외합니다."
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
			summary = "데이트 코스 저장 API",
			description = "생성된 코스 후보 중 하나를 선택해 저장합니다. "
					+ "이미 저장된 코스와 동일한 장소 순서이면 409를 반환합니다."
	)
	@ApiResponse(responseCode = "200", description = "저장 성공")
	@PostMapping("/{dateCourseId}/save")
	CommonResponse<Void> saveCourse(
			@PathVariable String roomId,
			@PathVariable String dateCourseId,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);

	@Operation(
			tags = {"Date course"},
			summary = "방 저장 데이트 코스 목록 조회 API",
			description = "방 멤버가 저장한 데이트 코스를 최신 저장순으로 페이지 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping
	CommonResponse<DateCoursePageResponse> listCourses(
			@PathVariable String roomId,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer limit
	);

	@Operation(
			tags = {"Date course"},
			summary = "데이트 코스 상세 조회 API",
			description = "특정 데이트 코스의 장소 목록과 직선 polyline용 orderedCoordinates를 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/{dateCourseId}")
	CommonResponse<DateCourseResponse> getCourse(
			@PathVariable String roomId,
			@PathVariable String dateCourseId
	);
}
