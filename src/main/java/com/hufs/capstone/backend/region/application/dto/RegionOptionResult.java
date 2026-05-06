package com.hufs.capstone.backend.region.application.dto;

public record RegionOptionResult(
		String code,
		String name,
		Integer displayOrder,
		boolean all
) {
}
