package com.hufs.capstone.backend.place.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class BusinessHoursPayloadParser {

	private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d");
	private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2})[:시](\\d{2})?");
	private static final List<String> DAYS = List.of("월", "화", "수", "목", "금", "토", "일");

	private final ObjectMapper objectMapper;

	BusinessHoursSchedule parse(String businessHoursJson, LocalDate referenceDate) throws JsonProcessingException {
		JsonNode root = objectMapper.readTree(businessHoursJson);
		JsonNode dailyHoursNode = root.path("daily_hours");
		if (!dailyHoursNode.isArray()) {
			return new BusinessHoursSchedule(List.of());
		}
		List<DailyBusinessHours> dailyHours = new ArrayList<>();
		for (JsonNode row : dailyHoursNode) {
			DailyBusinessHours parsed = parseRow(row, referenceDate);
			if (parsed != null) {
				dailyHours.add(parsed);
			}
		}
		return new BusinessHoursSchedule(dailyHours);
	}

	private static DailyBusinessHours parseRow(JsonNode row, LocalDate referenceDate) {
		String day = normalizeDay(text(row, "day"));
		if (day == null) {
			return null;
		}
		String closeRaw = firstText(row, "close", "close_time", "end", "end_time");
		ParsedLastOrder lastOrder = parseLastOrder(firstNode(row, "last_order", "lastOrder"));
		return new DailyBusinessHours(
				day,
				parseDate(text(row, "date"), referenceDate),
				text(row, "raw"),
				parseTime(firstText(row, "open", "open_time", "start", "start_time")),
				parseTime(closeRaw),
				isMidnightEndOfDay(closeRaw),
				parseRange(firstNode(row, "break_time", "breakTime")),
				lastOrder.time(),
				lastOrder.raw()
		);
	}

	private static BusinessHoursTimeRange parseRange(JsonNode value) {
		if (value == null || value.isNull()) {
			return null;
		}
		if (value.isObject()) {
			String raw = text(value, "raw");
			LocalTime open = parseTime(firstText(value, "open", "start", "open_time", "start_time"));
			LocalTime close = parseTime(firstText(value, "close", "end", "close_time", "end_time"));
			if (open != null && close != null) {
				return new BusinessHoursTimeRange(open, close, raw);
			}
			return parseRangeText(raw);
		}
		return value.isTextual() ? parseRangeText(value.asText()) : null;
	}

	private static BusinessHoursTimeRange parseRangeText(String value) {
		if (isBlank(value)) {
			return null;
		}
		Matcher matcher = TIME_PATTERN.matcher(value);
		List<LocalTime> times = new ArrayList<>();
		while (matcher.find()) {
			LocalTime parsed = parseTime(matcher.group());
			if (parsed != null) {
				times.add(parsed);
			}
		}
		return times.size() < 2 ? null : new BusinessHoursTimeRange(times.get(0), times.get(1), value);
	}

	private static ParsedLastOrder parseLastOrder(JsonNode value) {
		if (value == null || value.isNull()) {
			return ParsedLastOrder.EMPTY;
		}
		if (value.isObject()) {
			String raw = text(value, "raw");
			String timeText = firstText(value, "time", "last_order_time", "lastOrderTime");
			LocalTime time = parseTime(timeText);
			return new ParsedLastOrder(time == null ? parseTime(raw) : time, raw);
		}
		if (value.isTextual()) {
			return new ParsedLastOrder(parseTime(value.asText()), value.asText());
		}
		return ParsedLastOrder.EMPTY;
	}

	private static LocalDate parseDate(String value, LocalDate referenceDate) {
		if (isBlank(value)) {
			return null;
		}
		try {
			return LocalDate.parse(value.trim(), ISO_DATE_FORMATTER);
		} catch (DateTimeParseException ignored) {
			return parseShortDate(value.trim(), referenceDate);
		}
	}

	private static LocalDate parseShortDate(String value, LocalDate referenceDate) {
		try {
			var monthDay = java.time.MonthDay.parse(value, SHORT_DATE_FORMATTER);
			int referenceYear = referenceDate == null ? Year.now().getValue() : referenceDate.getYear();
			LocalDate reference = referenceDate == null ? LocalDate.now() : referenceDate;
			return List.of(referenceYear - 1, referenceYear, referenceYear + 1).stream()
					.map(year -> atYear(monthDay, year))
					.filter(java.util.Objects::nonNull)
					.min(java.util.Comparator.comparingLong(date -> Math.abs(ChronoUnit.DAYS.between(reference, date))))
					.orElse(null);
		} catch (DateTimeParseException ignored) {
			return null;
		}
	}

	private static LocalDate atYear(java.time.MonthDay monthDay, int year) {
		try {
			return monthDay.atYear(year);
		} catch (DateTimeException ignored) {
			return null;
		}
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
		if (hour > 23 || minute > 59) {
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
		String minuteGroup = matcher.group(2);
		return Integer.parseInt(matcher.group(1)) == 24
				&& (minuteGroup == null || Integer.parseInt(minuteGroup) == 0);
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

	private static JsonNode firstNode(JsonNode node, String... fieldNames) {
		for (String fieldName : fieldNames) {
			JsonNode value = node.get(fieldName);
			if (value != null && !value.isNull()) {
				return value;
			}
		}
		return null;
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

	private static String text(JsonNode node, String fieldName) {
		JsonNode value = node.path(fieldName);
		return value.isTextual() ? value.asText() : null;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private record ParsedLastOrder(LocalTime time, String raw) {

		private static final ParsedLastOrder EMPTY = new ParsedLastOrder(null, null);
	}
}
