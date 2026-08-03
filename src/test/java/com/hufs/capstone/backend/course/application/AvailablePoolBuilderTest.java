package com.hufs.capstone.backend.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.course.application.dto.DateCourseCandidate;
import com.hufs.capstone.backend.course.domain.repository.DateCourseCandidateRepository;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AvailablePoolBuilderTest {

	private final DateCourseCandidateRepository candidateRepository = mock(DateCourseCandidateRepository.class);
	private final BusinessHoursAtTimeChecker businessHoursAtTimeChecker = mock(BusinessHoursAtTimeChecker.class);
	private final AvailablePoolBuilder builder = new AvailablePoolBuilder(
			candidateRepository,
			businessHoursAtTimeChecker
	);

	@Test
	void buildsPoolFromSingleQueryProjectionAndFiltersClosedPlaces() {
		Long roomId = 10L;
		Instant startDateTime = Instant.parse("2026-08-03T12:00:00Z");
		List<CategorySlotCommand> slots = List.of(new CategorySlotCommand("FOOD", "KOREAN"));
		RoomPlace openRoomPlace = mock(RoomPlace.class);
		RoomPlace closedRoomPlace = mock(RoomPlace.class);
		DateCourseCandidate open = candidate(openRoomPlace, "open-json");
		DateCourseCandidate closed = candidate(closedRoomPlace, "closed-json");

		when(candidateRepository.findCandidates(eq(roomId), eq(slots), any(Instant.class), eq("11440")))
				.thenReturn(List.of(open, closed));
		when(businessHoursAtTimeChecker.isOpenAt("open-json", startDateTime)).thenReturn(true);
		when(businessHoursAtTimeChecker.isOpenAt("closed-json", startDateTime)).thenReturn(false);

		AvailablePool result = builder.build(roomId, slots, startDateTime, "11440");

		assertThat(result.all()).singleElement().satisfies(candidate -> {
			assertThat(candidate.roomPlace()).isSameAs(openRoomPlace);
			assertThat(candidate.categoryCode()).isEqualTo("FOOD");
			assertThat(candidate.tagCode()).isEqualTo("KOREAN");
			assertThat(candidate.businessHoursJson()).isEqualTo("open-json");
		});
		verify(candidateRepository).findCandidates(eq(roomId), eq(slots), any(Instant.class), eq("11440"));
	}

	private static DateCourseCandidate candidate(RoomPlace roomPlace, String businessHoursJson) {
		return new DateCourseCandidate(
				roomPlace,
				"FOOD",
				"KOREAN",
				new BigDecimal("37.550000"),
				new BigDecimal("126.920000"),
				Instant.parse("2026-08-01T00:00:00Z"),
				null,
				null,
				false,
				businessHoursJson
		);
	}
}
