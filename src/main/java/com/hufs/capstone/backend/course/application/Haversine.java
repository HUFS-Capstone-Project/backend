package com.hufs.capstone.backend.course.application;

import java.math.BigDecimal;

final class Haversine {

	private static final double EARTH_RADIUS_KM = 6371.0;

	private Haversine() {
	}

	static double km(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
		double lat1Rad = Math.toRadians(lat1.doubleValue());
		double lat2Rad = Math.toRadians(lat2.doubleValue());
		double deltaLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
		double deltaLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());

		double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
				+ Math.cos(lat1Rad) * Math.cos(lat2Rad)
				* Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_KM * c;
	}
}
