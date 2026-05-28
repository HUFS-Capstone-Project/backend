package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.application.dto.DateCourseBatchResult;
import com.hufs.capstone.backend.course.application.dto.DateCoursePlaceResult;
import com.hufs.capstone.backend.course.application.dto.DateCourseResult;
import com.hufs.capstone.backend.course.domain.entity.DateCourse;
import com.hufs.capstone.backend.course.domain.entity.DateCoursePlace;
import com.hufs.capstone.backend.course.domain.repository.DateCoursePlaceRepository;
import com.hufs.capstone.backend.course.domain.repository.DateCourseRepository;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DateCourseQueryService {

	private final RoomAccessService roomAccessService;
	private final DateCourseRepository dateCourseRepository;
	private final DateCoursePlaceRepository dateCourseParaRepository;

	@Transactional(readOnly = true)
	public List<DateCourseBatchResult> listBatches(String roomPublicId, Long userId) {
		Room room = roomAccessService.requireMemberRoom(roomPublicId, userId);

		List<DateCourse> courses = dateCourseRepository.findByRoomIdOrderByCreatedAtDesc(room.getId());
		if (courses.isEmpty()) {
			return List.of();
		}

		List<Long> courseIds = courses.stream().map(DateCourse::getId).toList();
		List<DateCoursePlace> allPlaces = dateCourseParaRepository.findWithRoomPlacesByCourseIdIn(courseIds);

		Map<Long, List<DateCoursePlace>> placesByCourseId = allPlaces.stream()
				.collect(Collectors.groupingBy(dcp -> dcp.getDateCourse().getId()));

		Map<String, List<DateCourse>> coursesByBatch = new LinkedHashMap<>();
		for (DateCourse course : courses) {
			coursesByBatch.computeIfAbsent(course.getGenerationBatchId(), k -> new ArrayList<>()).add(course);
		}

		return coursesByBatch.entrySet().stream()
				.map(entry -> {
					List<DateCourse> batchCourses = entry.getValue();
					DateCourse first = batchCourses.get(0);
					List<DateCourseResult> courseResults = batchCourses.stream()
							.map(c -> toCourseResult(c, placesByCourseId.getOrDefault(c.getId(), List.of())))
							.toList();
					return new DateCourseBatchResult(
							entry.getKey(),
							first.getCreatedAt(),
							first.getPlannedDateTime(),
							courseResults
					);
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public DateCourseResult getCourse(String roomPublicId, String coursePublicId, Long userId) {
		Room room = roomAccessService.requireMemberRoom(roomPublicId, userId);

		DateCourse course = dateCourseRepository.findByPublicIdAndRoomId(coursePublicId, room.getId())
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "코스를 찾을 수 없습니다."));

		List<DateCoursePlace> places = dateCourseParaRepository.findWithRoomPlacesByCourseIdIn(List.of(course.getId()));
		return toCourseResult(course, places);
	}

	private static DateCourseResult toCourseResult(DateCourse course, List<DateCoursePlace> places) {
		List<DateCoursePlaceResult> placeResults = places.stream()
				.map(dcp -> toPlaceResult(dcp.getRoomPlace(), dcp.getSequenceOrder()))
				.toList();

		List<Integer> skippedSlotIndices = parseSkipped(course.getSkippedSlotIndicesJson());

		return new DateCourseResult(
				course.getPublicId(),
				course.getCourseMode(),
				course.getGenerationBatchId(),
				course.getPlannedDateTime(),
				course.getCreatedAt(),
				placeResults,
				skippedSlotIndices
		);
	}

	private static DateCoursePlaceResult toPlaceResult(RoomPlace roomPlace, int sequenceOrder) {
		Place place = roomPlace.getPlace();
		return new DateCoursePlaceResult(
				roomPlace.getId(),
				place.getId(),
				place.getKakaoPlaceId(),
				place.getName(),
				place.getAddress(),
				place.getRoadAddress(),
				place.getLatitude(),
				place.getLongitude(),
				place.getServiceCategory().getCode(),
				place.getServiceCategory().getName(),
				place.getServiceTag().getCode(),
				place.getServiceTag().getName(),
				sequenceOrder
		);
	}

	private static List<Integer> parseSkipped(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			List<Integer> result = new ArrayList<>();
			String trimmed = json.trim();
			if (trimmed.equals("[]")) {
				return List.of();
			}
			String inner = trimmed.substring(1, trimmed.length() - 1);
			for (String part : inner.split(",")) {
				String num = part.trim();
				if (!num.isEmpty()) {
					result.add(Integer.parseInt(num));
				}
			}
			return result;
		} catch (Exception e) {
			return List.of();
		}
	}
}
