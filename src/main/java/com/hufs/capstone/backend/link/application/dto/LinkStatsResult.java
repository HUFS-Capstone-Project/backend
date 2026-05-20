package com.hufs.capstone.backend.link.application.dto;

public record LinkStatsResult(
		Long likeCount,
		Long commentCount,
		String postedAt
) {
}
