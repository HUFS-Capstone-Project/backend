package com.hufs.capstone.backend.place.application;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BusinessHoursDisplayResolver {

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
	private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2})[:시](\\d{2})?");
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
			return resolve(payloadParser.parse(businessHoursJson), now);
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
			BusinessHoursDisplayResult result = resolve(payloadParser.parse(businessHoursJson), at);
			return result == null ? BusinessStatus.UNKNOWN : result.businessStatus();
		} catch (Exception ex) {
			return BusinessStatus.UNKNOWN;
		}
	}

	BusinessHoursDisplayResult resolve(JsonNode businessHours, ZonedDateTime now) {
		if (businessHours == null || businessHours.isNull()) {
			return null;
		}
		List<DailyHours> dailyHours = readDailyHours(businessHours);
		List<WeeklyBusinessHoursResult> weeklyHours = weeklyHours(dailyHours, now);
		DailyHours today = findByDay(dailyHours, dayName(now.getDayOfWeek())).orElse(null);
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
		String previousDay = dayName(now.minusDays(1).getDayOfWeek());
		return findByDay(dailyHours, previousDay)
				.filter(DailyHours::hasOpenClose)
				.filter(DailyHours::isOvernight);
	}

	private Optional<ZonedDateTime> findNextOpenAt(List<DailyHours> dailyHours, ZonedDateTime now, int startOffsetDays) {
		for (int offset = startOffsetDays; offset <= 7; offset++) {
			LocalDate date = now.toLocalDate().plusDays(offset);
			String day = dayName(date.getDayOfWeek());
			Optional<DailyHours> candidate = findByDay(dailyHours, day);
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

	private List<DailyHours> readDailyHours(JsonNode businessHours) {
		JsonNode dailyHoursNode = businessHours.path("daily_hours");
		if (!dailyHoursNode.isArray()) {
			return List.of();
		}
		List<DailyHours> result = new ArrayList<>();
		for (JsonNode row : dailyHoursNode) {
			String day = normalizeDay(text(row, "day"));
			if (day == null) {
				continue;
			}
			String raw = text(row, "raw");
			LocalTime open = parseTime(firstText(row, "open", "open_time", "start", "start_time"));
			String closeRaw = firstText(row, "close", "close_time", "end", "end_time");
			LocalTime close = parseTime(closeRaw);
			TimeRange breakTime = parseRange(firstText(row, "break_time", "breakTime"));
			String lastOrder = firstText(row, "last_order", "lastOrder");
			result.add(new DailyHours(day, raw, open, close, isMidnightEndOfDay(closeRaw), breakTime,
					parseTime(lastOrder), lastOrder));
		}
		return result;
	}

	private List<WeeklyBusinessHoursResult> weeklyHours(List<DailyHours> dailyHours, ZonedDateTime now) {
		return dailyHours.stream()
				.map(row -> {
					boolean isToday = row.day().equals(dayName(now.getDayOfWeek()));
					List<String> subTexts = new ArrayList<>();
					if (row.breakTime() != null) {
						subTexts.add("브레이크타임 " + row.breakTime().displayText());
					}
					if (row.lastOrder() != null) {
						subTexts.add("라스트오더 " + formatTime(row.lastOrder()));
					}
					return new WeeklyBusinessHoursResult(
							row.day(),
							isToday ? now.format(DateTimeFormatter.ofPattern("M/d")) : null,
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
				new TodayBusinessHoursResult(now.toLocalDate(), dayName(now.getDayOfWeek()), "영업시간 정보 없음"),
				weeklyHours
		);
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

	private static Optional<DailyHours> findByDay(List<DailyHours> dailyHours, String day) {
		return dailyHours.stream()
				.filter(row -> row.day().equals(day))
				.findFirst();
	}

	private static String normalizeDay(String value) {
		if (isBlank(value)) {
			return null;
		}
		String normalized = value.trim().replace("요일", "");
		if (DAYS.contains(normalized)) {
			return normalized;
		}
		return switch (normalized.toUpperCase(Locale.ROOT)) {
			case "MON", "MONDAY" -> "월";
			case "TUE", "TUESDAY" -> "화";
			case "WED", "WEDNESDAY" -> "수";
			case "THU", "THURSDAY" -> "목";
			case "FRI", "FRIDAY" -> "금";
			case "SAT", "SATURDAY" -> "토";
			case "SUN", "SUNDAY" -> "일";
			default -> null;
		};
	}

	private static String dayName(DayOfWeek dayOfWeek) {
		return DAYS.get(dayOfWeek.getValue() - 1);
	}

	private static TimeRange parseRange(String value) {
		if (isBlank(value)) {
			return null;
		}
		Matcher matcher = TIME_PATTERN.matcher(value);
		List<LocalTime> times = new ArrayList<>();
		while (matcher.find()) {
			times.add(parseTime(matcher.group()));
		}
		if (times.size() < 2) {
			return null;
		}
		LocalDate today = LocalDate.now(SEOUL_ZONE);
		ZonedDateTime openAt = today.atTime(times.get(0)).atZone(SEOUL_ZONE);
		return new TimeRange(openAt, closeAt(openAt, times.get(1)), false);
	}

	private static LocalTime parseTime(String value) {
		if (isBlank(value)) {
			return null;
		}
		Matcher matcher = TIME_PATTERN.matcher(value.trim());
		if (!matcher.find()) {
			return null;
		}
		int hour = Integer.parseInt(matcher.group(1));
		String minuteGroup = matcher.group(2);
		int minute = minuteGroup == null ? 0 : Integer.parseInt(minuteGroup);
		if (hour == 24 && minute == 0) {
			return LocalTime.MIDNIGHT;
		}
		if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
			return null;
		}
		return LocalTime.of(hour, minute);
	}

	private static boolean isMidnightEndOfDay(String value) {
		if (isBlank(value)) {
			return false;
		}
		Matcher matcher = TIME_PATTERN.matcher(value.trim());
		if (!matcher.find()) {
			return false;
		}
		int hour = Integer.parseInt(matcher.group(1));
		String minuteGroup = matcher.group(2);
		int minute = minuteGroup == null ? 0 : Integer.parseInt(minuteGroup);
		return hour == 24 && minute == 0;
	}

	private static String text(JsonNode node, String fieldName) {
		JsonNode value = node.path(fieldName);
		return value.isTextual() ? value.asText() : null;
	}

	private static String firstText(JsonNode node, String... fieldNames) {
		for (String fieldName : fieldNames) {
			String value = text(node, fieldName);
			if (!isBlank(value)) {
				return value;
			}
		}
		return null;
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
			return normalized.contains("24시간");
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
