package com.hufs.capstone.backend.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.course.application.dto.AvailableCandidate;
import com.hufs.capstone.backend.course.application.dto.NormalizationContext;
import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import com.hufs.capstone.backend.link.domain.LinkSourceType;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CourseScorerTest {

	private final CourseScorer scorer = new CourseScorer();

	@Test
	void general_noPrev_returnsOne() {
		AvailableCandidate candidate = candidateWithCoords(37.5, 127.0);
		double score = scorer.score(candidate, null, CourseMode.GENERAL, noCtx(), Instant.now());
		assertThat(score).isCloseTo(1.0, within(0.001));
	}

	@Test
	void general_withPrev_returnsInverseDistance() {
		AvailableCandidate prev = candidateWithCoords(37.5665, 126.9780);
		// ~1 km away
		AvailableCandidate candidate = candidateWithCoords(37.5755, 126.9780);
		double score = scorer.score(candidate, prev, CourseMode.GENERAL, noCtx(), Instant.now());
		// dist ~1km → score ≈ 1/1 = 1.0
		assertThat(score).isCloseTo(1.0, within(0.2));
	}

	@Test
	void general_distClamp_preventsInfinity() {
		AvailableCandidate prev = candidateWithCoords(37.5665, 126.9780);
		AvailableCandidate candidate = candidateWithCoords(37.5665, 126.9780);  // same point, dist=0
		double score = scorer.score(candidate, prev, CourseMode.GENERAL, noCtx(), Instant.now());
		// clamped to 1/0.001 = 1000
		assertThat(score).isCloseTo(1000.0, within(1.0));
	}

	@Test
	void trendy_daysSinceZero_isMax() {
		AvailableCandidate candidate = candidateWithCreatedAt(Instant.now());
		double score = scorer.score(candidate, null, CourseMode.TRENDY, noCtx(), Instant.now());
		// 1.0 + 0.5 * exp(0) = 1.5
		assertThat(score).isCloseTo(1.5, within(0.05));
	}

	@Test
	void trendy_daysSinceFar_approachesOne() {
		Instant veryOld = Instant.now().minus(365 * 3L, ChronoUnit.DAYS);
		AvailableCandidate candidate = candidateWithCreatedAt(veryOld);
		double score = scorer.score(candidate, null, CourseMode.TRENDY, noCtx(), Instant.now());
		// 1.0 + 0.5 * exp(-large) ≈ 1.0
		assertThat(score).isCloseTo(1.0, within(0.01));
	}

	@Test
	void trendy_weightRange_between1and1point5() {
		for (int days : new int[]{0, 7, 30, 90, 365}) {
			Instant savedAt = Instant.now().minus(days, ChronoUnit.DAYS);
			AvailableCandidate candidate = candidateWithCreatedAt(savedAt);
			double weight = scorer.score(candidate, null, CourseMode.TRENDY, noCtx(), Instant.now());
			assertThat(weight).isBetween(1.0, 1.5);
		}
	}

	@Test
	void popular_noLink_returnsOne() {
		AvailableCandidate candidate = candidateNoLink();
		double score = scorer.score(candidate, null, CourseMode.POPULAR, noCtx(), Instant.now());
		assertThat(score).isCloseTo(1.0, within(0.001));
	}

	@Test
	void popular_maxLikeCount_returnsOnePoint8() {
		NormalizationContext ctx = new NormalizationContext(Map.of(LinkSourceType.INSTAGRAM, 1000L));
		AvailableCandidate candidate = candidateWithLikeCount(1000L, LinkSourceType.INSTAGRAM);
		double score = scorer.score(candidate, null, CourseMode.POPULAR, ctx, Instant.now());
		// 1.0 + 0.8 * (1000/1000) = 1.8
		assertThat(score).isCloseTo(1.8, within(0.001));
	}

	@Test
	void popular_maxIsZero_returnsOne() {
		NormalizationContext ctx = new NormalizationContext(Map.of(LinkSourceType.INSTAGRAM, 0L));
		AvailableCandidate candidate = candidateWithLikeCount(0L, LinkSourceType.INSTAGRAM);
		double score = scorer.score(candidate, null, CourseMode.POPULAR, ctx, Instant.now());
		assertThat(score).isCloseTo(1.0, within(0.001));
	}

	@Test
	void popular_weightRange_between1and1point8() {
		NormalizationContext ctx = new NormalizationContext(Map.of(LinkSourceType.YOUTUBE, 500L));
		for (long likes : new long[]{0, 100, 250, 500}) {
			AvailableCandidate candidate = candidateWithLikeCount(likes, LinkSourceType.YOUTUBE);
			double weight = scorer.score(candidate, null, CourseMode.POPULAR, ctx, Instant.now());
			assertThat(weight).isBetween(1.0, 1.8);
		}
	}

	private static AvailableCandidate candidateWithCoords(double lat, double lng) {
		Place place = mock(Place.class);
		when(place.getLatitude()).thenReturn(BigDecimal.valueOf(lat));
		when(place.getLongitude()).thenReturn(BigDecimal.valueOf(lng));
		RoomPlace roomPlace = mock(RoomPlace.class);
		when(roomPlace.getPlace()).thenReturn(place);
		when(roomPlace.getCreatedAt()).thenReturn(Instant.now());
		when(roomPlace.getOriginRoomLink()).thenReturn(null);
		return new AvailableCandidate(roomPlace, null);
	}

	private static AvailableCandidate candidateWithCreatedAt(Instant createdAt) {
		Place place = mock(Place.class);
		when(place.getLatitude()).thenReturn(null);
		when(place.getLongitude()).thenReturn(null);
		RoomPlace roomPlace = mock(RoomPlace.class);
		when(roomPlace.getPlace()).thenReturn(place);
		when(roomPlace.getCreatedAt()).thenReturn(createdAt);
		when(roomPlace.getOriginRoomLink()).thenReturn(null);
		return new AvailableCandidate(roomPlace, null);
	}

	private static AvailableCandidate candidateNoLink() {
		Place place = mock(Place.class);
		when(place.getLatitude()).thenReturn(null);
		when(place.getLongitude()).thenReturn(null);
		RoomPlace roomPlace = mock(RoomPlace.class);
		when(roomPlace.getPlace()).thenReturn(place);
		when(roomPlace.getCreatedAt()).thenReturn(Instant.now());
		when(roomPlace.getOriginRoomLink()).thenReturn(null);
		return new AvailableCandidate(roomPlace, null);
	}

	private static AvailableCandidate candidateWithLikeCount(long likeCount, LinkSourceType sourceType) {
		Place place = mock(Place.class);
		when(place.getLatitude()).thenReturn(null);
		when(place.getLongitude()).thenReturn(null);
		Link link = mock(Link.class);
		when(link.getLikeCount()).thenReturn(likeCount);
		when(link.getLinkSourceType()).thenReturn(sourceType);
		RoomLink roomLink = mock(RoomLink.class);
		when(roomLink.getLink()).thenReturn(link);
		RoomPlace roomPlace = mock(RoomPlace.class);
		when(roomPlace.getPlace()).thenReturn(place);
		when(roomPlace.getCreatedAt()).thenReturn(Instant.now());
		when(roomPlace.getOriginRoomLink()).thenReturn(roomLink);
		return new AvailableCandidate(roomPlace, null);
	}

	private static NormalizationContext noCtx() {
		return new NormalizationContext(Map.of());
	}
}
