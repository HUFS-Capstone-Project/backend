package com.hufs.capstone.backend.course.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.course.application.dto.CourseSelectionResult;
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
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.ArrayList;
import java.util.HashSet;
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
	private final DateCourseInputValidator inputValidator;
	private final AvailablePoolBuilder poolBuilder;
	private final CourseSelector courseSelector;
	private final DateCourseDuplicatePolicy duplicatePolicy;
	private final DateCourseRepository dateCourseRepository;
	private final DateCoursePlaceRepository dateCoursePlaceRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public DateCourseGenerationResult generate(DateCourseGenerationCommand command, Long userId) {
		inputValidator.validate(command.sigunguCode(), command.startDateTime(),
				command.endDateTime(), command.categorySequence());
		Room room = roomAccessService.requireMemberRoom(command.roomPublicId(), userId);

		AvailablePool pool = poolBuilder.build(room.getId(), command.categorySequence(),
				command.startDateTime(), command.sigunguCode());
		if (pool.isEmpty()) {
			throw new BusinessException(ErrorCode.E404_NOT_FOUND, "데이트 코스 생성에 사용할 수 있는 장소가 없습니다.");
		}

		String batchId = UUID.randomUUID().toString();
		String categorySequenceJson = serializeSlots(command.categorySequence());

		Set<Long> globallyUsedIds = courseSelector.newGloballyUsedIds();
		List<DateCourseResult> results = new ArrayList<>();

		for (CourseMode mode : List.of(CourseMode.GENERAL, CourseMode.TRENDY, CourseMode.POPULAR)) {
			Set<Long> candidateUsedIds = new HashSet<>(globallyUsedIds);
			CourseSelectionResult selection = courseSelector.select(
					mode, command.categorySequence(), pool, candidateUsedIds, command.startDateTime());

			if (selection.pickedPlaces().isEmpty()) {
				continue;
			}
			if (duplicatePolicy.existsSavedCourseWithSamePlaces(room.getId(), selection.pickedPlaces())) {
				continue;
			}
			globallyUsedIds = candidateUsedIds;

			String skippedJson = serializeSkipped(selection.skippedSlotIndices());
			DateCourse dateCourse = dateCourseRepository.save(DateCourse.create(
					UUID.randomUUID().toString(),
					room,
					userId,
					mode,
					command.startDateTime(),
					command.endDateTime(),
					batchId,
					command.sigunguCode(),
					categorySequenceJson,
					skippedJson
			));

			List<DateCoursePlace> places = new ArrayList<>();
			for (int i = 0; i < selection.pickedPlaces().size(); i++) {
				places.add(DateCoursePlace.create(dateCourse, selection.pickedPlaces().get(i), i));
			}
			dateCoursePlaceRepository.saveAll(places);

			results.add(toResult(dateCourse, selection.pickedPlaces(), selection.skippedSlotIndices()));
		}

		if (results.isEmpty()) {
			throw new BusinessException(ErrorCode.E404_NOT_FOUND, "생성할 수 있는 코스가 없습니다.");
		}

		return new DateCourseGenerationResult(batchId, results);
	}

	private static DateCourseResult toResult(DateCourse dateCourse, List<RoomPlace> pickedPlaces, List<Integer> skipped) {
		List<DateCoursePlaceResult> placeResults = new ArrayList<>();
		for (int i = 0; i < pickedPlaces.size(); i++) {
			placeResults.add(DateCoursePlaceMapper.toPlaceResult(pickedPlaces.get(i), i));
		}
		return new DateCourseResult(
				dateCourse.getDateCourseId(),
				dateCourse.getCourseName(),
				dateCourse.getCourseMode(),
				dateCourse.getGenerationBatchId(),
				dateCourse.getStartDateTime(),
				dateCourse.getEndDateTime(),
				dateCourse.getCreatedAt(),
				placeResults,
				skipped,
				null,
				null,
				null,
				null
		);
	}

	private String serializeSlots(List<CategorySlotCommand> slots) {
		try {
			return objectMapper.writeValueAsString(slots);
		} catch (JsonProcessingException e) {
			throw new BusinessException(ErrorCode.E500_INTERNAL, "카테고리 순서 직렬화에 실패했습니다.");
		}
	}

	private String serializeSkipped(List<Integer> skipped) {
		try {
			return objectMapper.writeValueAsString(skipped);
		} catch (JsonProcessingException e) {
			throw new BusinessException(ErrorCode.E500_INTERNAL, "건너뛴 슬롯 직렬화에 실패했습니다.");
		}
	}
}
