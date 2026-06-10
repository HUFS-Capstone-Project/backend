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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

	/**
	 * 한 배치 안에서 "코스 전체가 동일"하게 나오는 것을 피하기 위한 최대 재추첨 횟수.
	 * 장소가 부족해 회피가 불가능하면 마지막 후보를 그대로 사용해 생성을 보장한다.
	 */
	private static final int MAX_REROLL = 5;

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

		Map<Long, Integer> usageCounts = new HashMap<>();
		Set<List<Long>> batchSignatures = new HashSet<>();
		List<DateCourseResult> results = new ArrayList<>();

		for (CourseMode mode : List.of(CourseMode.GENERAL, CourseMode.TRENDY, CourseMode.POPULAR)) {
			CourseSelectionResult selection = selectAvoidingDuplicates(
					mode, command, pool, usageCounts, batchSignatures, room.getId());
			if (selection == null) {
				continue;
			}

			List<Long> signature = placeIdSignature(selection.pickedPlaces());
			batchSignatures.add(signature);
			for (Long placeId : signature) {
				usageCounts.merge(placeId, 1, Integer::sum);
			}

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

	/**
	 * 한 모드의 코스를 선택하되, 같은 배치 안에서 "코스 전체가 동일"한 결과를 최대 {@link #MAX_REROLL}회
	 * 재추첨으로 피한다. 회피에 실패하면(장소 부족) 마지막 유효 후보를 사용해 생성을 보장한다.
	 * 이미 저장된 코스와 장소·순서가 완전히 같은 후보는 절대 채택하지 않는다.
	 *
	 * @return 채택된 선택 결과. 채택 가능한 후보가 없으면(전부 비었거나 전부 저장-중복) {@code null}.
	 */
	private CourseSelectionResult selectAvoidingDuplicates(
			CourseMode mode,
			DateCourseGenerationCommand command,
			AvailablePool pool,
			Map<Long, Integer> usageCounts,
			Set<List<Long>> batchSignatures,
			Long roomId
	) {
		CourseSelectionResult fallback = null;
		for (int attempt = 0; attempt < MAX_REROLL; attempt++) {
			CourseSelectionResult selection = courseSelector.select(
					mode, command.categorySequence(), pool, usageCounts, command.startDateTime());
			if (selection.pickedPlaces().isEmpty()) {
				continue;
			}
			if (duplicatePolicy.existsSavedCourseWithSamePlaces(roomId, selection.pickedPlaces())) {
				continue;
			}
			fallback = selection;
			if (!batchSignatures.contains(placeIdSignature(selection.pickedPlaces()))) {
				return selection;
			}
		}
		return fallback;
	}

	private static List<Long> placeIdSignature(List<RoomPlace> places) {
		return places.stream().map(RoomPlace::getId).toList();
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
