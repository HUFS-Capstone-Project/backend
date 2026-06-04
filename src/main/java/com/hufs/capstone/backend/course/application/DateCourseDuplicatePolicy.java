package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.domain.entity.DateCoursePlace;
import com.hufs.capstone.backend.course.domain.repository.DateCoursePlaceRepository;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class DateCourseDuplicatePolicy {

	private final DateCoursePlaceRepository dateCoursePlaceRepository;

	boolean existsSavedCourseWithSamePlaces(Long roomId, List<RoomPlace> places) {
		return savedCourseSignatures(roomId).contains(roomPlaceSignature(places));
	}

	boolean existsSavedCourseWithSamePlacesExcluding(
			Long roomId,
			Long excludedCourseId,
			List<DateCoursePlace> places
	) {
		return savedCourseSignaturesExcluding(roomId, excludedCourseId)
				.contains(dateCoursePlaceSignature(places));
	}

	/**
	 * 코스 수정 시 — 최종 확정 RoomPlace 목록(순서 포함)이 자기 자신을 제외한
	 * 다른 저장 코스와 동일한지 검사한다.
	 */
	boolean existsSavedCourseWithSameRoomPlacesExcluding(
			Long roomId,
			Long excludedCourseId,
			List<RoomPlace> orderedPlaces
	) {
		return savedCourseSignaturesExcluding(roomId, excludedCourseId)
				.contains(roomPlaceSignature(orderedPlaces));
	}

	private Set<List<Long>> savedCourseSignatures(Long roomId) {
		return signatures(dateCoursePlaceRepository.findSavedPlacesByRoomId(roomId));
	}

	private Set<List<Long>> savedCourseSignaturesExcluding(Long roomId, Long excludedCourseId) {
		return signatures(dateCoursePlaceRepository.findSavedPlacesByRoomIdExcludingCourseId(roomId, excludedCourseId));
	}

	private static Set<List<Long>> signatures(List<DateCoursePlace> places) {
		Map<Long, List<DateCoursePlace>> placesByCourseId = places.stream()
				.collect(Collectors.groupingBy(
						place -> place.getDateCourse().getId(),
						LinkedHashMap::new,
						Collectors.toCollection(ArrayList::new)
				));

		return placesByCourseId.values().stream()
				.map(DateCourseDuplicatePolicy::dateCoursePlaceSignature)
				.collect(Collectors.toSet());
	}

	private static List<Long> roomPlaceSignature(List<RoomPlace> places) {
		return places.stream()
				.map(RoomPlace::getId)
				.toList();
	}

	private static List<Long> dateCoursePlaceSignature(List<DateCoursePlace> places) {
		return places.stream()
				.sorted(java.util.Comparator.comparingInt(DateCoursePlace::getSequenceOrder))
				.map(place -> place.getRoomPlace().getId())
				.toList();
	}
}
