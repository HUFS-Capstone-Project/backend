package com.hufs.capstone.backend.region.api.controller.swagger;

import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.region.api.response.RegionOptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/regions")
@SecurityRequirement(name = "bearer-jwt")
public interface RegionApi {

	@Operation(
			tags = {"Region"},
			summary = "시/도 목록 조회 API",
			description = "지역 설정용 시/도 목록을 조회합니다. DB에 저장하지 않는 가상 전체 옵션을 맨 앞에 포함합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/sidos")
	CommonResponse<List<RegionOptionResponse>> getSidos();

	@Operation(
			tags = {"Region"},
			summary = "시/군/구 목록 조회 API",
			description = "특정 시/도의 시/군/구 목록을 조회합니다. DB에 저장하지 않는 해당 시/도 전체 옵션을 맨 앞에 포함합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/sidos/{sidoCode}/sigungus")
	CommonResponse<List<RegionOptionResponse>> getSigungus(@PathVariable String sidoCode);
}
