package com.hufs.capstone.backend.region.domain.vo;

public record ResolvedRegion(
		String sidoCode,
		String sidoName,
		String sigunguCode,
		String sigunguName
) {

	public static ResolvedRegion unresolved() {
		return new ResolvedRegion(null, null, null, null);
	}
}
