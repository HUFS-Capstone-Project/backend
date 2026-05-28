package com.hufs.capstone.backend.course.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.course.application.dto.CourseSelectionResult;
import com.hufs.capstone.backend.course.application.dto.DateCourseBatchResult;
import com.hufs.capstone.backend.course.application.dto.DateCourseGenerationCommand;
import com.hufs.capstone.backend.course.application.dto.DateCourseGenerationResult;
import com.hufs.capstone.backend.course.application.dto.DateCoursePlaceResult;
import com.hufs.capstone.backend.course.application.dto.DateCourseResult;
import com.hufs.capstone.backend.course.domain.entity.DateCourse;
import com.hufs.capstone.backend.course.domain.entity.DateCoursePlace;
import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import com.hufs.capstone.backend.course.domain.repository.DateCoursePlaceRepository;
import com.hufs.capstone.backend.course.domain.repository.DateCourseRepository;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DateCourseGenerationService {

	private final RoomAccessService roomAccessService;
	private final AvailablePoolBuilder poolBuilder;
	private final CourseSelector courseSelector;
	private final DateCourseRepository dateCourseRepository;
	private final DateCoursePlaceRepository dateCourseParaRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public DateCourseGenerationResult generate(DateCourseGenerationCommand command, Long userId) {
		Room room = roomAccessService.requireMemberRoom(command.roomPublicId(), userId);

		AvailablePool pool = poolBuilder.build(room.getId(), command.categorySequence(), command.plannedDateTime(), command.sigunguCode());
		if (pool.isEmpty()) {
			throw new BusinessException(ErrorCode.E404_NOT_FOUND, "코스 생성 가능한 장소가 없습니다.");
		}

		String batchId = UUID.randomUUID().toString();
		String categorySequenceJson = serializeSlots(command.categorySequence());

		Set<Long> globallyUsedIds = courseSelector.newGloballyUsedIds();
		List<DateCourseResult> results = new ArrayList<>();

		for (CourseMode mode : List.of(CourseMode.GENERAL, CourseMode.TRENDY, CourseMode.POPULAR)) {
			CourseSelectionResult selection = courseSelector.select(
					mode, command.categorySequence(), pool, globallyUsedIds, command.plannedDateTime());

			String skippedJson = serializeSkipped(selection.skippedSlotIndices());
			DateCourse dateCourse = dateCourseRepository.save(DateCourse.create(
					UUID.randomUUID().toString(),
					room,
					userId,
					mode,
					command.plannedDateTime(),
					batchId,
					command.sigunguCode(),
					categorySequenceJson,
					skippedJson
			));

			List<DateCoursePlace> places = new ArrayList<>();
			for (int i = 0; i < selection.pickedPlaces().size(); i++) {
				places.add(DateCoursePlace.create(dateCourse, selection.pickedPlaces().get(i), i));
			}
			dateCourseParaRepository.saveAll(places);

			results.add(toResult(dateCourse, selection.pickedPlaces(), selection.skippedSlotIndices()));
		}

		return new DateCourseGenerationResult(batchId, results);
	}

	private static DateCourseResult toResult(DateCourse dateCourse, List<RoomPlace> pickedPlaces, List<Integer> skipped) {
		List<DateCoursePlaceResult> placeResults = new ArrayList<>();
		for (int i = 0; i < pickedPlaces.size(); i++) {
			placeResults.add(toPlaceResult(pickedPlaces.get(i), i));
		}
		return new DateCourseResult(
				dateCourse.getPublicId(),
				dateCourse.getCourseMode(),
				dateCourse.getGenerationBatchId(),
				dateCourse.getPlannedDateTime(),
				dateCourse.getCreatedAt(),
				placeResults,
				skipped
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

	private String serializeSlots(List<CategorySlotCommand> slots) {
		try {
			return objectMapper.writeValueAsString(slots);
		} catch (JsonProcessingException e) {
			return "[]";
		}
	}

	private String serializeSkipped(List<Integer> skipped) {
		try {
			return objectMapper.writeValueAsString(skipped);
		} catch (JsonProcessingException e) {
			return "[]";
		}
	}
}
