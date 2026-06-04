package com.hufs.capstone.backend.course.api.controller.swagger;

import com.hufs.capstone.backend.course.api.request.DateCourseGenerationRequest;
import com.hufs.capstone.backend.course.api.request.DateCourseSaveRequest;
import com.hufs.capstone.backend.course.api.request.DateCourseUpdateRequest;
import com.hufs.capstone.backend.course.api.response.DateCourseGenerationResponse;
import com.hufs.capstone.backend.course.api.response.DateCoursePageResponse;
import com.hufs.capstone.backend.course.api.response.DateCourseResponse;
import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.region.api.response.RegionOptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
			summary = "코스 생성용 방 시/도 필터 목록 조회 API",
			description = "방에 저장된 장소에 실제로 존재하는 시/도 옵션만 반환합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/sidos")
	CommonResponse<List<RegionOptionResponse>> listCourseGenerationSidos(@PathVariable String roomId);

	@Operation(
			tags = {"Date course"},
			summary = "코스 생성용 방 시/군/구 필터 목록 조회 API",
			description = "선택한 시/도에 속하며 방에 저장된 장소에 실제로 존재하는 시/군/구 옵션만 반환합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/sidos/{sidoCode}/sigungus")
	CommonResponse<List<RegionOptionResponse>> listCourseGenerationSigungus(
			@PathVariable String roomId,
			@PathVariable String sidoCode
	);

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
			description = "생성된 코스 후보 중 하나를 선택해 저장합니다."
					+ "이미 저장된 코스와 동일한 장소 순서이면 409를 반환합니다."
	)
	@ApiResponse(responseCode = "200", description = "저장 성공")
	@PostMapping("/{dateCourseId}/save")
	CommonResponse<Void> saveCourse(
			@PathVariable String roomId,
			@PathVariable String dateCourseId,
			@Valid @RequestBody DateCourseSaveRequest request,
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
			description = "특정 데이트 코스의 장소 목록과 직선 경로(polyline) 렌더링용 orderedCoordinates를 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/{dateCourseId}")
	CommonResponse<DateCourseResponse> getCourse(
			@PathVariable String roomId,
			@PathVariable String dateCourseId
	);

	@Operation(
			tags = {"Date course"},
			summary = "데이트 코스 수정 API",
			description = """
					저장된 데이트 코스의 이름과 장소 구성(순서 포함)을 전체 교체합니다.
					- 코스 이름 변경, 장소 순서 변경, 장소 삭제, 장소 추가 4가지를 한 번에 처리합니다.
					- roomPlaceIds는 최종 순서대로 전달합니다 (index = sequenceOrder).
					- 추가하는 장소는 반드시 해당 방에 이미 저장된 장소(roomPlaceId)여야 합니다.
					- 코스 생성자 또는 저장자만 수정할 수 있습니다.
					- 수정 결과가 같은 방의 다른 저장 코스와 동일한 장소 구성·순서이면 409를 반환합니다
					  (code=E409_DUPLICATE_DATE_COURSE).
					"""
	)
	@ApiResponse(responseCode = "200", description = "수정 성공, 수정된 코스 반환")
	@ApiResponse(responseCode = "400", description = "입력값 오류 (장소 0개, 중복 ID, 방 미소속 장소 등)")
	@ApiResponse(responseCode = "403", description = "수정 권한 없음")
	@ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
	@ApiResponse(responseCode = "409", description = "동일한 데이트 코스가 이미 저장되어 있음 (code=E409_DUPLICATE_DATE_COURSE)")
	@PutMapping("/{dateCourseId}")
	CommonResponse<DateCourseResponse> updateCourse(
			@PathVariable String roomId,
			@PathVariable String dateCourseId,
			@Valid @RequestBody DateCourseUpdateRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);

	@Operation(
			tags = {"Date course"},
			summary = "데이트 코스 삭제 API",
			description = """
					저장된 데이트 코스를 soft delete합니다. 삭제 후 목록/상세 조회에서 제외됩니다.
					코스 생성자 또는 저장자만 삭제할 수 있습니다.
					"""
	)
	@ApiResponse(responseCode = "200", description = "삭제 성공")
	@ApiResponse(responseCode = "403", description = "삭제 권한 없음")
	@ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
	@DeleteMapping("/{dateCourseId}")
	CommonResponse<Void> deleteCourse(
			@PathVariable String roomId,
			@PathVariable String dateCourseId,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);
}
