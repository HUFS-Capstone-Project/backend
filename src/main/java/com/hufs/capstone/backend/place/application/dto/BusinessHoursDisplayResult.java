package com.hufs.capstone.backend.place.application.dto;

import com.hufs.capstone.backend.place.domain.enums.BusinessStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record BusinessHoursDisplayResult(
		BusinessStatus businessStatus,
		String statusDisplayText,
		String todayDisplayText,
		OffsetDateTime nextOpenAt,
		OffsetDateTime nextCloseAt,
		TodayBusinessHoursResult today,
		List<WeeklyBusinessHoursResult> weeklyHours
) {

	public BusinessHoursDisplayResult {
		weeklyHours = weeklyHours == null ? List.of() : List.copyOf(weeklyHours);
	}

	public record TodayBusinessHoursResult(
			LocalDate date,
			String day,
			String displayText
	) {
	}

	public record WeeklyBusinessHoursResult(
			String day,
			String date,
			boolean isToday,
			String displayText,
			List<String> subTexts
	) {

		public WeeklyBusinessHoursResult {
			subTexts = subTexts == null ? List.of() : List.copyOf(subTexts);
		}
	}
}
