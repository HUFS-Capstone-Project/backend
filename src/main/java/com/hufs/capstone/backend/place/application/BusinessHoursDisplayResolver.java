package com.hufs.capstone.backend.place.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.place.application.dto.BusinessHoursDisplayResult;
import com.hufs.capstone.backend.place.application.dto.BusinessHoursDisplayResult.TodayBusinessHoursResult;
import com.hufs.capstone.backend.place.application.dto.BusinessHoursDisplayResult.WeeklyBusinessHoursResult;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import com.hufs.capstone.backend.place.domain.enums.BusinessStatus;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BusinessHoursDisplayResolver {

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
	private static final Duration CLOSING_SOON_THRESHOLD = Duration.ofMinutes(30);
	private static final List<String> DAYS = List.of("월", "화", "수", "목", "금", "토", "일");

	private final BusinessHoursPayloadParser payloadParser;
	private final Clock clock;

	@Autowired
	public BusinessHoursDisplayResolver(BusinessHoursPayloadParser payloadParser, Clock clock) {
		this.payloadParser = payloadParser;
		this.clock = clock;
	}

	public BusinessHoursDisplayResolver(ObjectMapper objectMapper, Clock clock) {
		this(new BusinessHoursPayloadParser(objectMapper), clock);
	}

	public BusinessHoursDisplayResult resolve(String businessHoursJson, BusinessHoursStatus businessHoursStatus) {
		if (businessHoursStatus != BusinessHoursStatus.SUCCEEDED || isBlank(businessHoursJson)) {
			return null;
		}
		ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(SEOUL_ZONE);
		try {
			return resolve(payloadParser.parse(businessHoursJson, now.toLocalDate()), now);
		} catch (RuntimeException ex) {
			return unknown(now, List.of());
		} catch (Exception ex) {
			return unknown(now, List.of());
		}
	}

	public BusinessStatus statusAt(String businessHoursJson, ZonedDateTime at) {
		if (isBlank(businessHoursJson)) {
			return BusinessStatus.UNKNOWN;
		}
		try {
			ZonedDateTime seoulTime = at.withZoneSameInstant(SEOUL_ZONE);
			BusinessHoursDisplayResult result = resolve(
					payloadParser.parse(businessHoursJson, seoulTime.toLocalDate()),
					seoulTime
			);
			return result == null ? BusinessStatus.UNKNOWN : result.businessStatus();
		} catch (Exception ex) {
			return BusinessStatus.UNKNOWN;
		}
	}

	BusinessHoursDisplayResult resolve(BusinessHoursSchedule schedule, ZonedDateTime now) {
		if (schedule == null) {
			return null;
		}
		List<DailyHours> dailyHours = readDailyHours(schedule);
		List<WeeklyBusinessHoursResult> weeklyHours = weeklyHours(dailyHours, now);
		DailyHours today = findByDate(dailyHours, now.toLocalDate()).orElse(null);
		if (today == null) {
			return unknown(now, weeklyHours);
		}
		if (today.isDayOff()) {
			ZonedDateTime nextOpenAt = findNextOpenAt(dailyHours, now, 1).orElse(null);
			return result(
					BusinessStatus.DAY_OFF,
					"오늘 휴무" + nextOpenSuffix(nextOpenAt, now),
					todayDisplayText(today),
					nextOpenAt,
					null,
					today,
					now,
					weeklyHours
			);
		}
		if (today.isOpen24Hours()) {
			return result(
					BusinessStatus.OPEN_24_HOURS,
					"24시간 영업",
					todayDisplayText(today),
					null,
					null,
					today,
					now,
					weeklyHours
			);
		}
		if (!today.hasOpenClose()) {
			return unknown(now, weeklyHours);
		}
		return resolveTimedHours(dailyHours, today, now, weeklyHours);
	}

	private BusinessHoursDisplayResult resolveTimedHours(
			List<DailyHours> dailyHours,
			DailyHours today,
			ZonedDateTime now,
			List<WeeklyBusinessHoursResult> weeklyHours
	) {
		TimeRange activeRange = activeRange(dailyHours, today, now).orElse(null);
		if (activeRange != null) {
			TimeRange breakTime = today.breakTime();
			if (breakTime != null && breakTime.contains(now.toLocalDate(), now.toLocalTime())) {
				ZonedDateTime nextOpenAt = atToday(now, breakTime.end());
				return result(
						BusinessStatus.BREAK_TIME,
						"브레이크타임 · " + formatTime(breakTime.end()) + " 영업 시작",
						todayDisplayText(today),
						nextOpenAt,
						activeRange.closeAt(),
						today,
						now,
						weeklyHours
				);
			}
			Duration untilClose = Duration.between(now, activeRange.closeAt());
			BusinessStatus status = !untilClose.isNegative()
					&& untilClose.compareTo(CLOSING_SOON_THRESHOLD) <= 0
					? BusinessStatus.CLOSING_SOON
					: BusinessStatus.OPEN;
			String prefix = status == BusinessStatus.CLOSING_SOON ? "곧 마감" : "영업 중";
			return result(
					status,
					prefix + " · " + formatCloseTime(activeRange) + " 영업 종료",
					todayDisplayText(today),
					null,
					activeRange.closeAt(),
					today,
					now,
					weeklyHours
			);
		}
		ZonedDateTime todayOpenAt = atToday(now, today.open());
		if (now.isBefore(todayOpenAt)) {
			return result(
					BusinessStatus.BEFORE_OPEN,
					"영업 전 · " + formatTime(today.open()) + " 영업 시작",
					todayDisplayText(today),
					todayOpenAt,
					closeAt(todayOpenAt, today.close()),
					today,
					now,
					weeklyHours
			);
		}
		ZonedDateTime nextOpenAt = findNextOpenAt(dailyHours, now, 1).orElse(null);
		return result(
				BusinessStatus.CLOSED,
				"영업 종료" + nextOpenSuffix(nextOpenAt, now),
				todayDisplayText(today),
				nextOpenAt,
				null,
				today,
				now,
				weeklyHours
		);
	}

	private Optional<TimeRange> activeRange(List<DailyHours> dailyHours, DailyHours today, ZonedDateTime now) {
		List<TimeRange> candidates = new ArrayList<>();
		if (today.hasOpenClose()) {
			candidates.add(timeRange(today, now.toLocalDate()));
		}
		previousDayOvernight(dailyHours, now)
				.map(previous -> timeRange(previous, now.toLocalDate().minusDays(1)))
				.ifPresent(candidates::add);
		return candidates.stream()
				.filter(range -> !now.isBefore(range.openAt()) && now.isBefore(range.closeAt()))
				.min(Comparator.comparing(TimeRange::closeAt));
	}

	private Optional<DailyHours> previousDayOvernight(List<DailyHours> dailyHours, ZonedDateTime now) {
		return findByDate(dailyHours, now.toLocalDate().minusDays(1))
				.filter(DailyHours::hasOpenClose)
				.filter(DailyHours::isOvernight);
	}

	private Optional<ZonedDateTime> findNextOpenAt(List<DailyHours> dailyHours, ZonedDateTime now, int startOffsetDays) {
		for (int offset = startOffsetDays; offset <= 7; offset++) {
			LocalDate date = now.toLocalDate().plusDays(offset);
			Optional<DailyHours> candidate = findByDate(dailyHours, date);
			if (candidate.isPresent() && candidate.get().hasOpenClose()
					&& !candidate.get().isDayOff() && !candidate.get().isOpen24Hours()) {
				return Optional.of(date.atTime(candidate.get().open()).atZone(SEOUL_ZONE));
			}
			if (candidate.isPresent() && candidate.get().isOpen24Hours()) {
				return Optional.of(date.atStartOfDay(SEOUL_ZONE));
			}
		}
		return Optional.empty();
	}

	private List<DailyHours> readDailyHours(BusinessHoursSchedule schedule) {
		return schedule.dailyHours().stream()
				.map(row -> new DailyHours(
						row.day(),
						row.date(),
						row.raw(),
						row.open(),
						row.close(),
						row.closesAtEndOfDay(),
						toTimeRange(row.breakTime()),
						row.lastOrder(),
						row.lastOrderRaw()
				))
				.toList();
	}

	private List<WeeklyBusinessHoursResult> weeklyHours(List<DailyHours> dailyHours, ZonedDateTime now) {
		DailyHours today = findByDate(dailyHours, now.toLocalDate()).orElse(null);
		return dailyHours.stream()
				.map(row -> {
					boolean isToday = row.equals(today);
					List<String> subTexts = new ArrayList<>();
					if (row.breakTime() != null) {
						subTexts.add("브레이크타임 " + row.breakTime().displayText());
					}
					if (row.lastOrder() != null) {
						subTexts.add("라스트오더 " + formatTime(row.lastOrder()));
					}
					return new WeeklyBusinessHoursResult(
							row.day(),
							row.date() == null ? null : row.date().format(DateTimeFormatter.ofPattern("M/d")),
							isToday,
							row.displayText(),
							subTexts
					);
				})
				.toList();
	}

	private BusinessHoursDisplayResult result(
			BusinessStatus businessStatus,
			String statusDisplayText,
			String todayDisplayText,
			ZonedDateTime nextOpenAt,
			ZonedDateTime nextCloseAt,
			DailyHours today,
			ZonedDateTime now,
			List<WeeklyBusinessHoursResult> weeklyHours
	) {
		return new BusinessHoursDisplayResult(
				businessStatus,
				statusDisplayText,
				todayDisplayText,
				toOffset(nextOpenAt),
				toOffset(nextCloseAt),
				toOffset(nextStatusChangeAt(businessStatus, nextOpenAt, nextCloseAt, today, now)),
				new TodayBusinessHoursResult(now.toLocalDate(), today.day(), today.displayText()),
				weeklyHours
		);
	}

	private BusinessHoursDisplayResult unknown(ZonedDateTime now, List<WeeklyBusinessHoursResult> weeklyHours) {
		return new BusinessHoursDisplayResult(
				BusinessStatus.UNKNOWN,
				"영업시간 정보 없음",
				"영업시간 정보 없음",
				null,
				null,
				toOffset(now.toLocalDate().plusDays(1).atStartOfDay(SEOUL_ZONE)),
				new TodayBusinessHoursResult(now.toLocalDate(), dayName(now.getDayOfWeek()), "영업시간 정보 없음"),
				weeklyHours
		);
	}

	private static ZonedDateTime nextStatusChangeAt(
			BusinessStatus status,
			ZonedDateTime nextOpenAt,
			ZonedDateTime nextCloseAt,
			DailyHours today,
			ZonedDateTime now
	) {
		List<ZonedDateTime> candidates = new ArrayList<>();
		candidates.add(now.toLocalDate().plusDays(1).atStartOfDay(SEOUL_ZONE));
		if (status == BusinessStatus.BEFORE_OPEN
				|| status == BusinessStatus.BREAK_TIME
				|| status == BusinessStatus.CLOSED
				|| status == BusinessStatus.DAY_OFF) {
			candidates.add(nextOpenAt);
		}
		if (status == BusinessStatus.OPEN || status == BusinessStatus.CLOSING_SOON) {
			candidates.add(nextCloseAt);
		}
		if (status == BusinessStatus.OPEN && nextCloseAt != null) {
			candidates.add(nextCloseAt.minus(CLOSING_SOON_THRESHOLD));
			if (today.breakTime() != null) {
				ZonedDateTime breakStartsAt = atToday(now, today.breakTime().openAt().toLocalTime());
				if (breakStartsAt.isBefore(nextCloseAt)) {
					candidates.add(breakStartsAt);
				}
			}
		}
		return candidates.stream()
				.filter(java.util.Objects::nonNull)
				.filter(candidate -> candidate.isAfter(now))
				.min(Comparator.naturalOrder())
				.orElse(null);
	}

	private static String nextOpenSuffix(ZonedDateTime nextOpenAt, ZonedDateTime now) {
		if (nextOpenAt == null) {
			return "";
		}
		return " · " + relativeDateText(nextOpenAt, now) + " " + formatTime(nextOpenAt.toLocalTime()) + " 영업 시작";
	}

	private static String relativeDateText(ZonedDateTime dateTime, ZonedDateTime now) {
		if (dateTime.toLocalDate().equals(now.toLocalDate())) {
			return "오늘";
		}
		if (dateTime.toLocalDate().equals(now.toLocalDate().plusDays(1))) {
			return "내일";
		}
		return dateTime.format(DateTimeFormatter.ofPattern("M/d"));
	}

	private static String todayDisplayText(DailyHours today) {
		if (today.isDayOff()) {
			return "오늘 휴무";
		}
		if (today.isOpen24Hours()) {
			return "오늘 24시간 영업";
		}
		if (today.hasOpenClose()) {
			return "오늘 " + today.displayText();
		}
		return "영업시간 정보 없음";
	}

	private static TimeRange timeRange(DailyHours row, LocalDate openDate) {
		ZonedDateTime openAt = openDate.atTime(row.open()).atZone(SEOUL_ZONE);
		return new TimeRange(openAt, closeAt(openAt, row.close()), row.closesAtEndOfDay());
	}

	private static ZonedDateTime closeAt(ZonedDateTime openAt, LocalTime close) {
		LocalDate closeDate = close.isAfter(openAt.toLocalTime())
				|| close.equals(openAt.toLocalTime())
				? openAt.toLocalDate()
				: openAt.toLocalDate().plusDays(1);
		return closeDate.atTime(close).atZone(SEOUL_ZONE);
	}

	private static ZonedDateTime atToday(ZonedDateTime now, LocalTime time) {
		return now.toLocalDate().atTime(time).atZone(SEOUL_ZONE);
	}

	private static Optional<DailyHours> findByDate(List<DailyHours> dailyHours, LocalDate date) {
		String day = dayName(date.getDayOfWeek());
		Optional<DailyHours> exactDate = dailyHours.stream()
				.filter(row -> date.equals(row.date()))
				.findFirst();
		return exactDate.or(() -> dailyHours.stream()
				.filter(row -> row.date() == null)
				.filter(row -> row.day().equals(day))
				.findFirst());
	}

	private static String dayName(DayOfWeek dayOfWeek) {
		return DAYS.get(dayOfWeek.getValue() - 1);
	}

	private static TimeRange toTimeRange(BusinessHoursTimeRange range) {
		if (range == null || range.open() == null || range.close() == null) {
			return null;
		}
		LocalDate anchorDate = LocalDate.of(2000, 1, 1);
		ZonedDateTime openAt = anchorDate.atTime(range.open()).atZone(SEOUL_ZONE);
		return new TimeRange(openAt, closeAt(openAt, range.close()), false);
	}

	private static String formatTime(LocalTime time) {
		return time.format(TIME_FORMATTER);
	}

	private static String formatCloseTime(DailyHours row) {
		return row.closesAtEndOfDay() ? "24:00" : formatTime(row.close());
	}

	private static String formatCloseTime(TimeRange range) {
		return range.closesAtEndOfDay() ? "24:00" : formatTime(range.closeAt().toLocalTime());
	}

	private static java.time.OffsetDateTime toOffset(ZonedDateTime dateTime) {
		return dateTime == null ? null : dateTime.toOffsetDateTime();
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private record DailyHours(
			String day,
			LocalDate date,
			String raw,
			LocalTime open,
			LocalTime close,
			boolean closesAtEndOfDay,
			TimeRange breakTime,
			LocalTime lastOrder,
			String lastOrderRaw
	) {

		boolean hasOpenClose() {
			return open != null && close != null;
		}

		boolean isOvernight() {
			return hasOpenClose() && close.isBefore(open);
		}

		boolean isDayOff() {
			String normalized = normalizeRaw();
			return normalized.contains("휴무") || normalized.contains("휴무일") || normalized.contains("정기휴무");
		}

		boolean isOpen24Hours() {
			String normalized = normalizeRaw();
			return normalized.contains("24시간")
					|| (hasOpenClose()
					&& open.equals(LocalTime.MIDNIGHT)
					&& close.equals(LocalTime.MIDNIGHT)
					&& closesAtEndOfDay);
		}

		String displayText() {
			if (isDayOff()) {
				return "휴무";
			}
			if (isOpen24Hours()) {
				return "24시간 영업";
			}
			if (hasOpenClose()) {
				return formatTime(open) + " - " + formatCloseTime(this);
			}
			if (!isBlank(raw)) {
				return raw.trim();
			}
			return "영업시간 정보 없음";
		}

		private String normalizeRaw() {
			return raw == null ? "" : raw.replace(" ", "").trim();
		}
	}

	private record TimeRange(
			ZonedDateTime openAt,
			ZonedDateTime closeAt,
			boolean closesAtEndOfDay
	) {

		boolean contains(LocalDate date, LocalTime time) {
			ZonedDateTime open = date.atTime(openAt.toLocalTime()).atZone(SEOUL_ZONE);
			ZonedDateTime close = BusinessHoursDisplayResolver.closeAt(open, closeAt.toLocalTime());
			ZonedDateTime target = date.atTime(time).atZone(SEOUL_ZONE);
			return !target.isBefore(open) && target.isBefore(close);
		}

		LocalTime end() {
			return closeAt.toLocalTime();
		}

		String displayText() {
			return formatTime(openAt.toLocalTime()) + " - " + formatCloseTime(this);
		}
	}
}
