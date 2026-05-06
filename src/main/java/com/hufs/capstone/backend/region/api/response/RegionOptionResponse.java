package com.hufs.capstone.backend.region.api.response;

import com.hufs.capstone.backend.region.application.dto.RegionOptionResult;

public record RegionOptionResponse(
		String code,
		String name,
		Integer displayOrder,
		boolean all
) {

	public static RegionOptionResponse from(RegionOptionResult result) {
		return new RegionOptionResponse(
				result.code(),
				result.name(),
				result.displayOrder(),
				result.all()
		);
	}
}
