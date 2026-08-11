package com.hufs.capstone.backend.place.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

record BusinessHoursSchedule(List<DailyBusinessHours> dailyHours) {

	BusinessHoursSchedule {
		dailyHours = dailyHours == null ? List.of() : List.copyOf(dailyHours);
	}
}

record DailyBusinessHours(
		String day,
		LocalDate date,
		String raw,
		LocalTime open,
		LocalTime close,
		boolean closesAtEndOfDay,
		BusinessHoursTimeRange breakTime,
		LocalTime lastOrder,
		String lastOrderRaw
) {
}

record BusinessHoursTimeRange(LocalTime open, LocalTime close, String raw) {
}
