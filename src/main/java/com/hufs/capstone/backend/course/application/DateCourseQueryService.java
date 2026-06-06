package com.hufs.capstone.backend.course.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.course.application.dto.DateCoursePageResult;
import com.hufs.capstone.backend.course.application.dto.DateCoursePlaceResult;
import com.hufs.capstone.backend.course.application.dto.DateCourseResult;
import com.hufs.capstone.backend.course.application.dto.MyDateCoursePageResult;
import com.hufs.capstone.backend.course.application.dto.MyDateCourseResult;
import com.hufs.capstone.backend.course.domain.entity.DateCourse;
import com.hufs.capstone.backend.course.domain.entity.DateCoursePlace;
import com.hufs.capstone.backend.course.domain.repository.DateCoursePlaceRepository;
import com.hufs.capstone.backend.course.domain.repository.DateCourseRepository;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.global.exception.FieldValidationException;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.region.application.dto.RegionOptionResult;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.user.domain.entity.User;
import com.hufs.capstone.backend.user.domain.repository.UserRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DateCourseQueryService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_LIMIT = 20;
	private static final int MAX_LIMIT = 100;

	private final RoomAccessService roomAccessService;
	private final DateCourseRepository dateCourseRepository;
	private final DateCoursePlaceRepository dateCoursePlaceRepository;
	private final RoomPlaceRepository roomPlaceRepository;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;

	@Transactional(readOnly = true)
	public List<RegionOptionResult> listCourseGenerationSidos(String roomPublicId, Long userId) {
		Room room = roomAccessService.requireMemberRoom(roomPublicId, userId);
		return toRegionOptions(roomPlaceRepository.findDistinctSidoOptionsByRoomId(room.getId()));
	}

	@Transactional(readOnly = true)
	public List<RegionOptionResult> listCourseGenerationSigungus(String roomPublicId, String sidoCode, Long userId) {
		Room room = roomAccessService.requireMemberRoom(roomPublicId, userId);
		String normalizedSidoCode = requireSidoSavedInRoom(room.getId(), sidoCode);
		return toRegionOptions(
				roomPlaceRepository.findDistinctSigunguOptionsByRoomIdAndSidoCode(room.getId(), normalizedSidoCode)
		);
	}

	private String requireSidoSavedInRoom(Long roomId, String sidoCode) {
		if (sidoCode == null || sidoCode.isBlank()) {
			throw new FieldValidationException("sidoCode", "시/도 코드는 필수입니다.", sidoCode);
		}
		String normalized = sidoCode.trim();
		if (!roomPlaceRepository.existsByRoomIdAndSidoCode(roomId, normalized)) {
			throw new FieldValidationException(
					"sidoCode", "이 방에 저장된 시/도가 아닙니다.", sidoCode);
		}
		return normalized;
	}

	private static List<RegionOptionResult> toRegionOptions(List<RoomPlaceRepository.RoomPlaceRegionOption> options) {
		return IntStream.range(0, options.size())
				.mapToObj(index -> new RegionOptionResult(
						options.get(index).getCode(),
						options.get(index).getName(),
						index + 1,
						false
				))
				.toList();
	}

	@Transactional(readOnly = true)
	public DateCoursePageResult listSavedCourses(String roomPublicId, Long userId, Integer page, Integer limit) {
		Room room = roomAccessService.requireMemberRoom(roomPublicId, userId);
		int normalizedPage = page == null ? DEFAULT_PAGE : page;
		int normalizedLimit = limit == null ? DEFAULT_LIMIT : limit;
		if (normalizedPage < 0) {
			throw new FieldValidationException("page", "page는 0 이상이어야 합니다.", normalizedPage);
		}
		if (normalizedLimit < 1 || normalizedLimit > MAX_LIMIT) {
			throw new FieldValidationException("limit", "limit는 1~100 사이여야 합니다.", normalizedLimit);
		}

		Page<DateCourse> coursePage = dateCourseRepository.findSavedByRoomIdOrderBySavedAtDesc(
				room.getId(), PageRequest.of(normalizedPage, normalizedLimit));

		List<DateCourse> courses = coursePage.getContent();
		if (courses.isEmpty()) {
			return new DateCoursePageResult(List.of(), normalizedPage, normalizedLimit,
					coursePage.getTotalElements(), coursePage.getTotalPages());
		}

		List<Long> courseIds = courses.stream().map(DateCourse::getId).toList();
		List<DateCoursePlace> allPlaces = dateCoursePlaceRepository.findWithRoomPlacesByCourseIdIn(courseIds);
		Map<Long, List<DateCoursePlace>> placesByCourseId = allPlaces.stream()
				.collect(Collectors.groupingBy(dcp -> dcp.getDateCourse().getId()));

		Map<Long, User> userById = fetchUsers(courses.stream()
				.map(DateCourse::getSavedByUserId).distinct().toList());

		List<DateCourseResult> items = courses.stream()
				.map(c -> toCourseResult(c, placesByCourseId.getOrDefault(c.getId(), List.of()),
						userById.get(c.getSavedByUserId())))
				.toList();

		return new DateCoursePageResult(items, normalizedPage, normalizedLimit,
				coursePage.getTotalElements(), coursePage.getTotalPages());
	}

	@Transactional(readOnly = true)
	public DateCourseResult getCourse(String roomPublicId, String dateCourseId, Long userId) {
		Room room = roomAccessService.requireMemberRoom(roomPublicId, userId);

		DateCourse course = dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(dateCourseId, room.getId())
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "코스를 찾을 수 없습니다."));

		List<DateCoursePlace> places = dateCoursePlaceRepository.findWithRoomPlacesByCourseIdIn(List.of(course.getId()));
		User saver = course.getSavedByUserId() != null
				? userRepository.findByIdAndDeletedAtIsNull(course.getSavedByUserId()).orElse(null)
				: null;
		return toCourseResult(course, places, saver);
	}

	@Transactional(readOnly = true)
	public MyDateCoursePageResult listMySavedCourses(Long userId, Integer page, Integer limit) {
		int normalizedPage = page == null ? DEFAULT_PAGE : page;
		int normalizedLimit = limit == null ? DEFAULT_LIMIT : limit;
		if (normalizedPage < 0) {
			throw new FieldValidationException("page", "page는 0 이상이어야 합니다.", normalizedPage);
		}
		if (normalizedLimit < 1 || normalizedLimit > MAX_LIMIT) {
			throw new FieldValidationException("limit", "limit는 1~100 사이여야 합니다.", normalizedLimit);
		}

		Page<DateCourse> coursePage = dateCourseRepository.findSavedByUserIdOrderBySavedAtDesc(
				userId, PageRequest.of(normalizedPage, normalizedLimit));

		List<DateCourse> courses = coursePage.getContent();
		if (courses.isEmpty()) {
			return new MyDateCoursePageResult(List.of(), normalizedPage, normalizedLimit,
					coursePage.getTotalElements(), coursePage.getTotalPages());
		}

		List<Long> courseIds = courses.stream().map(DateCourse::getId).toList();
		List<DateCoursePlace> allPlaces = dateCoursePlaceRepository.findWithRoomPlacesByCourseIdIn(courseIds);
		Map<Long, List<DateCoursePlace>> placesByCourseId = allPlaces.stream()
				.collect(Collectors.groupingBy(dcp -> dcp.getDateCourse().getId()));

		List<MyDateCourseResult> items = courses.stream()
				.map(c -> toMyResult(c, placesByCourseId.getOrDefault(c.getId(), List.of())))
				.toList();

		return new MyDateCoursePageResult(items, normalizedPage, normalizedLimit,
				coursePage.getTotalElements(), coursePage.getTotalPages());
	}

	private Map<Long, User> fetchUsers(Collection<Long> userIds) {
		List<Long> nonNullIds = userIds.stream().filter(id -> id != null).toList();
		if (nonNullIds.isEmpty()) {
			return Map.of();
		}
		return userRepository.findByIdInAndDeletedAtIsNull(nonNullIds).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));
	}

	private DateCourseResult toCourseResult(DateCourse course, List<DateCoursePlace> places, User saver) {
		List<DateCoursePlaceResult> placeResults = places.stream()
				.sorted(Comparator.comparingInt(DateCoursePlace::getSequenceOrder))
				.map(dcp -> DateCoursePlaceMapper.toPlaceResult(dcp.getRoomPlace(), dcp.getSequenceOrder()))
				.toList();

		return new DateCourseResult(
				course.getDateCourseId(),
				course.getCourseName(),
				course.getCourseMode(),
				course.getGenerationBatchId(),
				course.getStartDateTime(),
				course.getEndDateTime(),
				course.getCreatedAt(),
				placeResults,
				parseSkipped(course.getSkippedSlotIndicesJson()),
				saver != null ? saver.getId() : null,
				saver != null ? saver.getNickname() : null,
				saver != null ? saver.getProfileImageUrl() : null,
				course.getSavedAt()
		);
	}

	private MyDateCourseResult toMyResult(DateCourse course, List<DateCoursePlace> places) {
		List<DateCoursePlaceResult> placeResults = places.stream()
				.sorted(Comparator.comparingInt(DateCoursePlace::getSequenceOrder))
				.map(dcp -> DateCoursePlaceMapper.toPlaceResult(dcp.getRoomPlace(), dcp.getSequenceOrder()))
				.toList();

		Room room = course.getRoom();
		return new MyDateCourseResult(
				course.getDateCourseId(),
				course.getCourseName(),
				course.getCourseMode(),
				course.getGenerationBatchId(),
				course.getStartDateTime(),
				course.getEndDateTime(),
				course.getSavedAt(),
				room.getPublicId(),
				room.getName(),
				placeResults,
				parseSkipped(course.getSkippedSlotIndicesJson())
		);
	}

	private List<Integer> parseSkipped(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<List<Integer>>() {
			});
		} catch (Exception e) {
			log.warn("Failed to parse skippedSlotIndicesJson: {}", json, e);
			return List.of();
		}
	}
}
