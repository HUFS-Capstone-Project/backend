package com.hufs.capstone.backend.course.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.place.application.BusinessHoursDisplayResolver;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BusinessHoursAtTimeCheckerTest {

	private final BusinessHoursDisplayResolver resolver =
			new BusinessHoursDisplayResolver(new ObjectMapper(), Clock.systemUTC());

	private final BusinessHoursAtTimeChecker checker = new BusinessHoursAtTimeChecker(resolver);

	private static final String WEEKLY_JSON = """
			{"daily_hours":[
			  {"day":"월","open":"11:30","close":"21:00"},
			  {"day":"화","open":"11:30","close":"21:00"},
			  {"day":"수","open":"11:30","close":"21:00"},
			  {"day":"목","open":"11:30","close":"21:00"},
			  {"day":"금","open":"11:30","close":"21:00"},
			  {"day":"토","open":"11:30","close":"21:00"},
			  {"day":"일","open":"11:30","close":"21:00"}
			]}
			""";

	@Test
	void openDuringBusinessHours_returnsTrue() {
		// 2026-05-12 Tuesday 15:00 KST = 06:00 UTC
		Instant at = Instant.parse("2026-05-12T06:00:00Z");
		assertThat(checker.isOpenAt(WEEKLY_JSON, at)).isTrue();
	}

	@Test
	void beforeOpeningTime_returnsFalse() {
		// 2026-05-12 Tuesday 10:00 KST = 01:00 UTC (before 11:30)
		Instant at = Instant.parse("2026-05-12T01:00:00Z");
		assertThat(checker.isOpenAt(WEEKLY_JSON, at)).isFalse();
	}

	@Test
	void afterClosingTime_returnsFalse() {
		// 2026-05-12 Tuesday 22:00 KST = 13:00 UTC (after 21:00)
		Instant at = Instant.parse("2026-05-12T13:00:00Z");
		assertThat(checker.isOpenAt(WEEKLY_JSON, at)).isFalse();
	}

	@Test
	void closingSoon_returnsTrue() {
		// 2026-05-12 Tuesday 20:45 KST = 11:45 UTC (within 30 min of 21:00)
		Instant at = Instant.parse("2026-05-12T11:45:00Z");
		assertThat(checker.isOpenAt(WEEKLY_JSON, at)).isTrue();
	}

	@Test
	void open24Hours_alwaysReturnsTrue() {
		String json = """
				{"daily_hours":[
				  {"day":"월","raw":"24시간"},{"day":"화","raw":"24시간"},{"day":"수","raw":"24시간"},
				  {"day":"목","raw":"24시간"},{"day":"금","raw":"24시간"},{"day":"토","raw":"24시간"},
				  {"day":"일","raw":"24시간"}
				]}
				""";
		// Any time
		assertThat(checker.isOpenAt(json, Instant.parse("2026-05-12T00:00:00Z"))).isTrue();
		assertThat(checker.isOpenAt(json, Instant.parse("2026-05-12T12:00:00Z"))).isTrue();
	}

	@Test
	void dayOff_returnsFalse() {
		String json = """
				{"daily_hours":[
				  {"day":"화","raw":"정기휴무"},
				  {"day":"수","open":"11:30","close":"21:00"}
				]}
				""";
		// Tuesday KST
		Instant at = Instant.parse("2026-05-12T06:00:00Z");
		assertThat(checker.isOpenAt(json, at)).isFalse();
	}

	@Test
	void nullJson_returnsFalse() {
		assertThat(checker.isOpenAt(null, Instant.now())).isFalse();
	}

	@Test
	void blankJson_returnsFalse() {
		assertThat(checker.isOpenAt("", Instant.now())).isFalse();
	}
}
