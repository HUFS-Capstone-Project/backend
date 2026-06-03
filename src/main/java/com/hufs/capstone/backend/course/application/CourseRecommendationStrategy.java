package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.application.dto.AvailableCandidate;
import com.hufs.capstone.backend.course.application.dto.NormalizationContext;
import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import java.util.Map;

interface CourseRecommendationStrategy {

	CourseMode mode();

	default boolean supports(CourseMode mode) {
		return mode() == mode;
	}

	default boolean isCandidateAllowed(AvailableCandidate candidate) {
		return true;
	}

	default NormalizationContext normalizationContext(AvailablePool pool) {
		return new NormalizationContext(Map.of());
	}
}
