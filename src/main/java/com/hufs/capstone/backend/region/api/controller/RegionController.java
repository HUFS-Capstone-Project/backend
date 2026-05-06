package com.hufs.capstone.backend.region.api.controller;

import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.region.api.controller.swagger.RegionApi;
import com.hufs.capstone.backend.region.api.response.RegionOptionResponse;
import com.hufs.capstone.backend.region.application.RegionQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RegionController implements RegionApi {

	private final RegionQueryService regionQueryService;

	@Override
	public CommonResponse<List<RegionOptionResponse>> getSidos() {
		return CommonResponse.ok(regionQueryService.getSidos().stream()
				.map(RegionOptionResponse::from)
				.toList());
	}

	@Override
	public CommonResponse<List<RegionOptionResponse>> getSigungus(@PathVariable String sidoCode) {
		return CommonResponse.ok(regionQueryService.getSigungus(sidoCode).stream()
				.map(RegionOptionResponse::from)
				.toList());
	}
}
