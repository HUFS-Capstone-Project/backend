package com.hufs.capstone.backend.place.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.place.application.dto.BusinessHoursDisplayResult;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import com.hufs.capstone.backend.place.domain.enums.BusinessStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(5)
class BusinessHoursDisplayResolverTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void shouldResolveBeforeOpen() {
		BusinessHoursDisplayResult result = resolverAt("2026-05-12T01:00:00Z").resolve(weekly("11:30", "21:00"),
				BusinessHoursStatus.SUCCEEDED);

		assertThat(result.businessStatus()).isEqualTo(BusinessStatus.BEFORE_OPEN);
		assertThat(result.statusDisplayText()).isEqualTo("영업 전 · 11:30 영업 시작");
		assertThat(result.todayDisplayText()).isEqualTo("오늘 11:30 - 21:00");
		assertThat(result.nextOpenAt()).hasToString("2026-05-12T11:30+09:00");
		assertThat(result.nextStatusChangeAt()).hasToString("2026-05-12T11:30+09:00");
	}

	@Test
	void shouldResolveOpen() {
		BusinessHoursDisplayResult result = resolverAt("2026-05-12T04:00:00Z").resolve(weekly("11:30", "21:00"),
				BusinessHoursStatus.SUCCEEDED);

		assertThat(result.businessStatus()).isEqualTo(BusinessStatus.OPEN);
		assertThat(result.statusDisplayText()).isEqualTo("영업 중 · 21:00 영업 종료");
		assertThat(result.nextCloseAt()).hasToString("2026-05-12T21:00+09:00");
		assertThat(result.nextStatusChangeAt()).hasToString("2026-05-12T20:30+09:00");
	}

	@Test
	void shouldResolveClosingSoon() {
		BusinessHoursDisplayResult result = resolverAt("2026-05-12T11:40:00Z").resolve(weekly("11:30", "21:00"),
				BusinessHoursStatus.SUCCEEDED);

		assertThat(result.businessStatus()).isEqualTo(BusinessStatus.CLOSING_SOON);
		assertThat(result.statusDisplayText()).isEqualTo("곧 마감 · 21:00 영업 종료");
	}

	@Test
	void shouldResolveClosedAndNextOpenAt() {
		BusinessHoursDisplayResult result = resolverAt("2026-05-12T13:00:00Z").resolve(weekly("11:30", "21:00"),
				BusinessHoursStatus.SUCCEEDED);

		assertThat(result.businessStatus()).isEqualTo(BusinessStatus.CLOSED);
		assertThat(result.statusDisplayText()).isEqualTo("영업 종료 · 내일 11:30 영업 시작");
		assertThat(result.nextOpenAt()).hasToString("2026-05-13T11:30+09:00");
	}

	@Test
	void shouldResolveDayOff() {
		BusinessHoursDisplayResult result = resolverAt("2026-05-12T04:00:00Z").resolve("""
				{"daily_hours":[
				  {"day":"월","open":"11:30","close":"21:00"},
				  {"day":"화","raw":"정기휴무"},
				  {"day":"수","open":"11:30","close":"21:00"}
				]}
				""", BusinessHoursStatus.SUCCEEDED);

		assertThat(result.businessStatus()).isEqualTo(BusinessStatus.DAY_OFF);
		assertThat(result.statusDisplayText()).isEqualTo("오늘 휴무 · 내일 11:30 영업 시작");
		assertThat(result.todayDisplayText()).isEqualTo("오늘 휴무");
		assertThat(result.weeklyHours().get(1).displayText()).isEqualTo("휴무");
	}

	@Test
	void shouldResolveOpen24HoursWithoutTreatingYearRoundAs24Hours() {
		BusinessHoursDisplayResult open24 = resolverAt("2026-05-12T04:00:00Z").resolve("""
				{"daily_hours":[{"day":"화","raw":"24시간"}]}
				""", BusinessHoursStatus.SUCCEEDED);
		BusinessHoursDisplayResult yearRound = resolverAt("2026-05-12T04:00:00Z").resolve("""
				{"daily_hours":[{"day":"화","raw":"연중무휴"}]}
				""", BusinessHoursStatus.SUCCEEDED);

		assertThat(open24.businessStatus()).isEqualTo(BusinessStatus.OPEN_24_HOURS);
		assertThat(open24.statusDisplayText()).isEqualTo("24시간 영업");
		assertThat(yearRound.businessStatus()).isEqualTo(BusinessStatus.UNKNOWN);
	}

	@Test
	void shouldResolveActualObjectContractForBreakTimeAndLastOrder() throws IOException {
		BusinessHoursDisplayResult result = resolverAt("2026-05-12T06:30:00Z").resolve(
				fixture("fixtures/business-hours/kakao-place-success.json"),
				BusinessHoursStatus.SUCCEEDED
		);

		assertThat(result.businessStatus()).isEqualTo(BusinessStatus.BREAK_TIME);
		assertThat(result.statusDisplayText()).isEqualTo("브레이크타임 · 17:00 영업 시작");
		assertThat(result.weeklyHours().get(0).subTexts())
				.containsExactly("브레이크타임 15:00 - 17:00", "라스트오더 20:20");
		assertThat(result.weeklyHours().get(0).date()).isEqualTo("5/12");
	}

	@Test
	void shouldTreatMidnightToEndOfDayAsOpen24Hours() {
		BusinessHoursDisplayResult result = resolverAt("2026-05-12T04:00:00Z").resolve("""
				{"daily_hours":[
				  {"day":"화","raw":"00:00 ~ 24:00","date":"5/12","open":"00:00","close":"24:00"}
				]}
				""", BusinessHoursStatus.SUCCEEDED);

		assertThat(result.businessStatus()).isEqualTo(BusinessStatus.OPEN_24_HOURS);
		assertThat(result.statusDisplayText()).isEqualTo("24시간 영업");
		assertThat(result.todayDisplayText()).isEqualTo("오늘 24시간 영업");
		assertThat(result.nextStatusChangeAt()).hasToString("2026-05-13T00:00+09:00");
	}

	@Test
	void shouldPreferExactDateOverRecurringWeekday() {
		BusinessHoursDisplayResult result = resolverAt("2026-05-12T04:00:00Z").resolve("""
				{"daily_hours":[
				  {"day":"화","raw":"정기휴무","date":"2026-05-12"},
				  {"day":"화","open":"11:30","close":"21:00"}
				]}
				""", BusinessHoursStatus.SUCCEEDED);

		assertThat(result.businessStatus()).isEqualTo(BusinessStatus.DAY_OFF);
		assertThat(result.today().date()).isEqualTo(java.time.LocalDate.parse("2026-05-12"));
	}

	@Test
	void shouldNotReuseDatedHolidayOnSameWeekdayOfAnotherWeek() {
		BusinessHoursDisplayResult result = resolverAt("2026-05-19T04:00:00Z").resolve("""
				{"daily_hours":[
				  {"day":"화","raw":"임시휴무","date":"5/12"},
				  {"day":"화","open":"11:30","close":"21:00"}
				]}
				""", BusinessHoursStatus.SUCCEEDED);

		assertThat(result.businessStatus()).isEqualTo(BusinessStatus.OPEN);
	}

	@Test
	void shouldResolveOvernightHours() {
		BusinessHoursDisplayResult result = resolverAt("2026-05-12T14:30:00Z").resolve("""
				{"daily_hours":[{"day":"화","open":"22:00","close":"02:00"}]}
				""", BusinessHoursStatus.SUCCEEDED);

		assertThat(result.businessStatus()).isEqualTo(BusinessStatus.OPEN);
		assertThat(result.statusDisplayText()).isEqualTo("영업 중 · 02:00 영업 종료");
		assertThat(result.nextCloseAt()).hasToString("2026-05-13T02:00+09:00");
	}

	@Test
	void shouldResolveMidnightCloseWrittenAs24Hours() {
		BusinessHoursDisplayResult result = resolverAt("2026-05-20T08:00:00Z").resolve("""
				{"daily_hours":[
				  {"day":"수","raw":"12:00 ~ 24:00","date":"5/20","open":"12:00","close":"24:00"},
				  {"day":"목","raw":"12:00 ~ 24:00","date":"5/21","open":"12:00","close":"24:00"},
				  {"day":"금","raw":"12:00 ~ 01:00","date":"5/22","open":"12:00","close":"01:00"}
				]}
				""", BusinessHoursStatus.SUCCEEDED);

		assertThat(result.businessStatus()).isEqualTo(BusinessStatus.OPEN);
		assertThat(result.statusDisplayText()).isEqualTo("영업 중 · 24:00 영업 종료");
		assertThat(result.todayDisplayText()).isEqualTo("오늘 12:00 - 24:00");
		assertThat(result.nextCloseAt()).hasToString("2026-05-21T00:00+09:00");
	}

	@Test
	void shouldHidePayloadUnlessRefreshSucceeded() {
		assertThat(resolverAt("2026-05-12T04:00:00Z").resolve(null, BusinessHoursStatus.SUCCEEDED)).isNull();
		assertThat(resolverAt("2026-05-12T04:00:00Z").resolve(weekly("11:30", "21:00"),
				BusinessHoursStatus.FETCHING)).isNull();
		assertThat(resolverAt("2026-05-12T04:00:00Z").resolve(weekly("11:30", "21:00"),
				BusinessHoursStatus.FAILED)).isNull();
	}

	@Test
	void shouldResolveUnknownWhenJsonIsMalformed() {
		BusinessHoursDisplayResult result = resolverAt("2026-05-12T04:00:00Z").resolve("{", BusinessHoursStatus.SUCCEEDED);

		assertThat(result.businessStatus()).isEqualTo(BusinessStatus.UNKNOWN);
		assertThat(result.statusDisplayText()).isEqualTo("영업시간 정보 없음");
	}

	private BusinessHoursDisplayResolver resolverAt(String instant) {
		return new BusinessHoursDisplayResolver(objectMapper, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
	}

	private static String fixture(String path) throws IOException {
		try (var input = BusinessHoursDisplayResolverTest.class.getClassLoader().getResourceAsStream(path)) {
			if (input == null) {
				throw new IOException("Fixture not found: " + path);
			}
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static String weekly(String open, String close) {
		return """
				{"daily_hours":[
				  {"day":"월","open":"%s","close":"%s"},
				  {"day":"화","open":"%s","close":"%s"},
				  {"day":"수","open":"%s","close":"%s"}
				]}
				""".formatted(open, close, open, close, open, close);
	}
}
