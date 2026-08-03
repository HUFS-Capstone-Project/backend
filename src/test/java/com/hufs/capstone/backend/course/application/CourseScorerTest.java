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
	void generalNoPrevReturnsOne() {
		AvailableCandidate candidate = candidateWithCoords(37.5, 127.0);
		double score = scorer.score(candidate, null, CourseMode.GENERAL, noCtx(), Instant.now());
		assertThat(score).isCloseTo(1.0, within(0.001));
	}

	@Test
	void generalWithPrevReturnsInverseDistance() {
		AvailableCandidate prev = candidateWithCoords(37.5665, 126.9780);
		// ~1 km away
		AvailableCandidate candidate = candidateWithCoords(37.5755, 126.9780);
		double score = scorer.score(candidate, prev, CourseMode.GENERAL, noCtx(), Instant.now());
		// dist ~1km → distScore=0.5 → score = 0.6*0.5 + 0.4*1.0 = 0.7
		assertThat(score).isCloseTo(0.7, within(0.1));
	}

	@Test
	void generalDistClampPreventsInfinity() {
		AvailableCandidate prev = candidateWithCoords(37.5665, 126.9780);
		AvailableCandidate candidate = candidateWithCoords(37.5665, 126.9780);  // same point, dist=0
		double score = scorer.score(candidate, prev, CourseMode.GENERAL, noCtx(), Instant.now());
		// dist=0 → distScore = 1/(1+0) = 1.0 → score = 0.6*1.0 + 0.4*1.0 = 1.0 (no infinity)
		assertThat(score).isCloseTo(1.0, within(0.001));
	}

	@Test
	void trendyDaysSinceZeroIsMax() {
		AvailableCandidate candidate = candidateWithCreatedAt(Instant.now());
		double score = scorer.score(candidate, null, CourseMode.TRENDY, noCtx(), Instant.now());
		// modeWeight = 1.0 + 0.5*exp(0) = 1.5 → score = 0.6*1.0 + 0.4*1.5 = 1.2
		assertThat(score).isCloseTo(1.2, within(0.05));
	}

	@Test
	void trendyDaysSinceFarApproachesOne() {
		Instant veryOld = Instant.now().minus(365 * 3L, ChronoUnit.DAYS);
		AvailableCandidate candidate = candidateWithCreatedAt(veryOld);
		double score = scorer.score(candidate, null, CourseMode.TRENDY, noCtx(), Instant.now());
		// 1.0 + 0.5 * exp(-large) ≈ 1.0
		assertThat(score).isCloseTo(1.0, within(0.01));
	}

	@Test
	void trendyScoreRangeBetween1and1point2() {
		for (int days : new int[]{0, 7, 30, 90, 365}) {
			Instant savedAt = Instant.now().minus(days, ChronoUnit.DAYS);
			AvailableCandidate candidate = candidateWithCreatedAt(savedAt);
			double score = scorer.score(candidate, null, CourseMode.TRENDY, noCtx(), Instant.now());
			assertThat(score).isBetween(1.0, 1.21);
		}
	}

	@Test
	void popularNoLinkReturnsOne() {
		AvailableCandidate candidate = candidateNoLink();
		double score = scorer.score(candidate, null, CourseMode.POPULAR, noCtx(), Instant.now());
		assertThat(score).isCloseTo(1.0, within(0.001));
	}

	@Test
	void popularMaxLikeCountReturnsOnePoint8() {
		NormalizationContext ctx = new NormalizationContext(Map.of(LinkSourceType.INSTAGRAM, 1000L));
		AvailableCandidate candidate = candidateWithLikeCount(1000L, LinkSourceType.INSTAGRAM);
		double score = scorer.score(candidate, null, CourseMode.POPULAR, ctx, Instant.now());
		// modeWeight = 1.0 + 0.8*(1000/1000) = 1.8 → score = 0.6*1.0 + 0.4*1.8 = 1.32
		assertThat(score).isCloseTo(1.32, within(0.001));
	}

	@Test
	void popularMaxIsZeroReturnsOne() {
		NormalizationContext ctx = new NormalizationContext(Map.of(LinkSourceType.INSTAGRAM, 0L));
		AvailableCandidate candidate = candidateWithLikeCount(0L, LinkSourceType.INSTAGRAM);
		double score = scorer.score(candidate, null, CourseMode.POPULAR, ctx, Instant.now());
		assertThat(score).isCloseTo(1.0, within(0.001));
	}

	@Test
	void popularScoreRangeBetween1and1point33() {
		NormalizationContext ctx = new NormalizationContext(Map.of(LinkSourceType.YOUTUBE, 500L));
		for (long likes : new long[]{0, 100, 250, 500}) {
			AvailableCandidate candidate = candidateWithLikeCount(likes, LinkSourceType.YOUTUBE);
			double score = scorer.score(candidate, null, CourseMode.POPULAR, ctx, Instant.now());
			assertThat(score).isBetween(1.0, 1.33);
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
		return new AvailableCandidate(
				roomPlace, null, null,
				BigDecimal.valueOf(lat), BigDecimal.valueOf(lng),
				Instant.now(), null, null, false, null
		);
	}

	private static AvailableCandidate candidateWithCreatedAt(Instant createdAt) {
		Place place = mock(Place.class);
		when(place.getLatitude()).thenReturn(null);
		when(place.getLongitude()).thenReturn(null);
		RoomPlace roomPlace = mock(RoomPlace.class);
		when(roomPlace.getPlace()).thenReturn(place);
		when(roomPlace.getCreatedAt()).thenReturn(createdAt);
		when(roomPlace.getOriginRoomLink()).thenReturn(null);
		return new AvailableCandidate(
				roomPlace, null, null, null, null,
				createdAt, null, null, false, null
		);
	}

	private static AvailableCandidate candidateNoLink() {
		Place place = mock(Place.class);
		when(place.getLatitude()).thenReturn(null);
		when(place.getLongitude()).thenReturn(null);
		RoomPlace roomPlace = mock(RoomPlace.class);
		when(roomPlace.getPlace()).thenReturn(place);
		when(roomPlace.getCreatedAt()).thenReturn(Instant.now());
		when(roomPlace.getOriginRoomLink()).thenReturn(null);
		return new AvailableCandidate(
				roomPlace, null, null, null, null,
				Instant.now(), null, null, false, null
		);
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
		return new AvailableCandidate(
				roomPlace, null, null, null, null,
				Instant.now(), sourceType, likeCount, true, null
		);
	}

	private static NormalizationContext noCtx() {
		return new NormalizationContext(Map.of());
	}
}
