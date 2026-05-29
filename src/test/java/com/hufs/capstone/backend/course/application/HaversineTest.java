package com.hufs.capstone.backend.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class HaversineTest {

	@Test
	void samePointReturnsZero() {
		double km = Haversine.km(
				BigDecimal.valueOf(37.5665),
				BigDecimal.valueOf(126.9780),
				BigDecimal.valueOf(37.5665),
				BigDecimal.valueOf(126.9780)
		);
		assertThat(km).isCloseTo(0.0, within(0.001));
	}

	@Test
	void approxOneKmBetweenNearbyPoints() {
		// Seoul City Hall vs ~1km north
		double km = Haversine.km(
				BigDecimal.valueOf(37.5665),
				BigDecimal.valueOf(126.9780),
				BigDecimal.valueOf(37.5755),
				BigDecimal.valueOf(126.9780)
		);
		assertThat(km).isCloseTo(1.0, within(0.1));
	}

	@Test
	void approxHundredKmBetweenSeoulAndSuwon() {
		// Seoul to Suwon (~45 km by Haversine)
		double km = Haversine.km(
				BigDecimal.valueOf(37.5665),
				BigDecimal.valueOf(126.9780),
				BigDecimal.valueOf(37.2636),
				BigDecimal.valueOf(127.0286)
		);
		assertThat(km).isBetween(30.0, 50.0);
	}

	@Test
	void antipodeIsApproxHalfEarthCircumference() {
		// Seoul antipode is roughly in South Atlantic
		double km = Haversine.km(
				BigDecimal.valueOf(37.5665),
				BigDecimal.valueOf(126.9780),
				BigDecimal.valueOf(-37.5665),
				BigDecimal.valueOf(-53.0220)
		);
		assertThat(km).isBetween(19000.0, 20200.0);
	}
}
