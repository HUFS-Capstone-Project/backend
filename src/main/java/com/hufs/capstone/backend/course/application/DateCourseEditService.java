package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.application.dto.DateCoursePlaceResult;
import com.hufs.capstone.backend.course.application.dto.DateCourseResult;
import com.hufs.capstone.backend.course.domain.DateCourseNamePolicy;
import com.hufs.capstone.backend.course.domain.entity.DateCourse;
import com.hufs.capstone.backend.course.domain.entity.DateCoursePlace;
import com.hufs.capstone.backend.course.domain.repository.DateCoursePlaceRepository;
import com.hufs.capstone.backend.course.domain.repository.DateCourseRepository;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.global.exception.FieldValidationException;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.user.domain.entity.User;
import com.hufs.capstone.backend.user.domain.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DateCourseEditService {

	private final RoomAccessService roomAccessService;
	private final DateCourseRepository dateCourseRepository;
	private final DateCoursePlaceRepository dateCoursePlaceRepository;
	private final RoomPlaceRepository roomPlaceRepository;
	private final DateCourseDuplicatePolicy duplicatePolicy;
	private final UserRepository userRepository;

	@Transactional
	public DateCourseResult update(
			String roomPublicId,
			String dateCourseId,
			String courseName,
			List<Long> roomPlaceIds,
			Long userId
	) {
		// 1. 방 멤버 검증
		Room room = roomAccessService.requireMemberRoom(roomPublicId, userId);

		// 2. 코스 존재 확인 (soft delete 제외)
		DateCourse course = dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(
				dateCourseId, room.getId())
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "데이트 코스를 찾을 수 없습니다."));

		// 3. 저장된 코스만 편집 대상 (미저장 후보는 사용자 가시 대상 아님)
		if (course.getSavedByUserId() == null) {
			throw new BusinessException(ErrorCode.E404_NOT_FOUND, "데이트 코스를 찾을 수 없습니다.");
		}

		// 4. 권한 검증 — 생성자 또는 저장자만 수정 가능
		boolean isOwner = userId.equals(course.getCreatedByUserId())
				|| userId.equals(course.getSavedByUserId());
		if (!isOwner) {
			throw new BusinessException(ErrorCode.E403_FORBIDDEN, "데이트 코스를 수정할 권한이 없습니다.");
		}

		// 5. 코스 이름 정규화/검증
		String normalizedName = DateCourseNamePolicy.normalizeAndValidate(courseName);

		// 6. roomPlaceIds 검증
		if (roomPlaceIds == null || roomPlaceIds.isEmpty()) {
			throw new FieldValidationException("roomPlaceIds", "장소는 최소 1개 이상이어야 합니다.");
		}

		// 중복 id 검증 — 같은 장소를 두 번 포함 불가 (DB 유니크 제약과 일치)
		long distinctCount = roomPlaceIds.stream().distinct().count();
		if (distinctCount != roomPlaceIds.size()) {
			throw new FieldValidationException("roomPlaceIds", "중복된 장소 ID가 포함되어 있습니다.");
		}

		// 방에 속한 장소인지 배치 검증 — 결과 수와 요청 수가 다르면 방에 없는 장소 포함
		List<RoomPlace> foundRoomPlaces = roomPlaceRepository.findAllByIdInAndRoomId(roomPlaceIds, room.getId());
		if (foundRoomPlaces.size() != roomPlaceIds.size()) {
			throw new FieldValidationException("roomPlaceIds", "이 방에 저장된 장소만 추가할 수 있습니다.");
		}

		// 요청 순서대로 정렬
		Map<Long, RoomPlace> roomPlaceById = foundRoomPlaces.stream()
				.collect(Collectors.toMap(RoomPlace::getId, rp -> rp));
		List<RoomPlace> orderedPlaces = roomPlaceIds.stream()
				.map(roomPlaceById::get)
				.toList();

		// 7. 중복 코스 검사 (자기 자신 제외)
		if (duplicatePolicy.existsSavedCourseWithSameRoomPlacesExcluding(
				room.getId(), course.getId(), orderedPlaces)) {
			throw new BusinessException(ErrorCode.E409_DUPLICATE_DATE_COURSE,
					"동일한 데이트 코스가 이미 저장되어 있습니다.");
		}

		// 8. 변경 적용
		course.rename(normalizedName);
		course.clearSkippedSlots();

		// 장소 전체 교체 (유니크 제약 충돌 방지: 삭제 후 재삽입)
		dateCoursePlaceRepository.deleteByDateCourseId(course.getId());

		List<DateCoursePlace> newPlaces = new ArrayList<>();
		for (int i = 0; i < orderedPlaces.size(); i++) {
			newPlaces.add(DateCoursePlace.create(course, orderedPlaces.get(i), i));
		}
		List<DateCoursePlace> savedPlaces = dateCoursePlaceRepository.saveAll(newPlaces);

		// 9. 결과 반환
		User saver = userRepository.findByIdAndDeletedAtIsNull(course.getSavedByUserId()).orElse(null);
		return toCourseResult(course, savedPlaces, saver);
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
				List.of(),
				saver != null ? saver.getId() : null,
				saver != null ? saver.getNickname() : null,
				saver != null ? saver.getProfileImageUrl() : null,
				course.getSavedAt()
		);
	}
}
