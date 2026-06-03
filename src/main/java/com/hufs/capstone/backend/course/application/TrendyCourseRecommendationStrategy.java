package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import org.springframework.stereotype.Component;

@Component
class TrendyCourseRecommendationStrategy implements CourseRecommendationStrategy {

	@Override
	public CourseMode mode() {
		return CourseMode.TRENDY;
	}
}
