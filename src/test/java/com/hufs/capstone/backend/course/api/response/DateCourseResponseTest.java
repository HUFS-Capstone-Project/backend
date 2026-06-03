package com.hufs.capstone.backend.course.api.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.hufs.capstone.backend.course.application.dto.DateCoursePlaceResult;
import com.hufs.capstone.backend.course.application.dto.DateCourseResult;
import com.hufs.capstone.backend.course.application.dto.MyDateCourseResult;
import com.hufs.capstone.backend.course.api.response.DateCourseGenerationResponse;
import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DateCourseResponseTest {

	@Test
	void placesAndCoordinatesAreOrderedBySequenceOrder() {
		DateCourseResult result = new DateCourseResult(
				"course-1",
				CourseMode.GENERAL,
				"batch-1",
				Instant.parse("2026-06-03T12:00:00Z"),
				Instant.parse("2026-06-03T14:00:00Z"),
				Instant.parse("2026-06-03T11:00:00Z"),
				List.of(
						place(2, "third", "37.3", "127.3"),
						place(0, "first", "37.1", "127.1"),
						place(1, "second", null, null)
				),
				List.of(),
				null,
				null,
				null,
				null
		);

		DateCourseResponse response = DateCourseResponse.from(result);

		assertThat(response.places())
				.extracting(DateCoursePlaceResponse::sequenceOrder)
				.containsExactly(0, 1, 2);
		assertThat(response.places())
				.extracting(DateCoursePlaceResponse::roomPlaceId)
				.containsExactly(1L, 2L, 3L);
		assertThat(response.places())
				.extracting(DateCoursePlaceResponse::name)
				.containsExactly("first", "second", "third");
		assertThat(response.orderedCoordinates())
				.extracting(DateCourseCoordinateResponse::sequenceOrder)
				.containsExactly(0, 2);
		assertThat(response.orderedCoordinates().get(0).latitude())
				.isEqualByComparingTo("37.1");
		assertThat(response.orderedCoordinates().get(1).longitude())
				.isEqualByComparingTo("127.3");
	}

	@Test
	void generationResponsePreservesRoomPlaceIdOnEveryPlace() {
		DateCourseGenerationResponse response = DateCourseGenerationResponse.from(
				new com.hufs.capstone.backend.course.application.dto.DateCourseGenerationResult(
						"batch-1",
						List.of(
								new DateCourseResult(
										"course-1",
										CourseMode.GENERAL,
										"batch-1",
										Instant.parse("2026-06-03T12:00:00Z"),
										Instant.parse("2026-06-03T14:00:00Z"),
										Instant.parse("2026-06-03T11:00:00Z"),
										List.of(place(0, "first", "37.1", "127.1")),
										List.of(),
										null,
										null,
										null,
										null
								)
						)
				)
		);

		assertThat(response.courses()).hasSize(1);
		assertThat(response.courses().get(0).places())
				.extracting(DateCoursePlaceResponse::roomPlaceId)
				.containsExactly(1L);
	}

	@Test
	void myDateCourseResponseIncludesOrderedCoordinates() {
		MyDateCourseResult result = new MyDateCourseResult(
				"course-1",
				CourseMode.GENERAL,
				"batch-1",
				Instant.parse("2026-06-03T12:00:00Z"),
				Instant.parse("2026-06-03T14:00:00Z"),
				Instant.parse("2026-06-03T15:00:00Z"),
				"room-1",
				"room",
				List.of(
						place(1, "second", "37.2", "127.2"),
						place(0, "first", "37.1", "127.1")
				),
				List.of()
		);

		MyDateCourseResponse response = MyDateCourseResponse.from(result);

		assertThat(response.places())
				.extracting(DateCoursePlaceResponse::sequenceOrder)
				.containsExactly(0, 1);
		assertThat(response.places())
				.extracting(DateCoursePlaceResponse::roomPlaceId)
				.containsExactly(1L, 2L);
		assertThat(response.orderedCoordinates())
				.extracting(DateCourseCoordinateResponse::sequenceOrder)
				.containsExactly(0, 1);
	}

	private static DateCoursePlaceResult place(int sequenceOrder, String name, String latitude, String longitude) {
		return new DateCoursePlaceResult(
				1L + sequenceOrder,
				10L + sequenceOrder,
				"kakao-" + sequenceOrder,
				name,
				"address",
				"roadAddress",
				latitude == null ? null : new BigDecimal(latitude),
				longitude == null ? null : new BigDecimal(longitude),
				"FOOD",
				"Food",
				"KOREAN",
				"Korean",
				sequenceOrder
		);
	}
}
