package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.application.dto.AvailableCandidate;
import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.course.domain.repository.DateCourseCandidateRepository;
import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.repository.PlaceBusinessHoursRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AvailablePoolBuilder {

	private final DateCourseCandidateRepository candidateRepository;
	private final PlaceBusinessHoursRepository placeBusinessHoursRepository;
	private final BusinessHoursAtTimeChecker businessHoursAtTimeChecker;

	AvailablePool build(Long roomId, List<CategorySlotCommand> slots, Instant startDateTime, String sigunguCode) {
		Instant now = Instant.now();
		List<RoomPlace> roomPlaces = candidateRepository.findCandidates(roomId, slots, now, sigunguCode);
		if (roomPlaces.isEmpty()) {
			return new AvailablePool(List.of());
		}

		List<String> kakaoPlaceIds = roomPlaces.stream()
				.map(RoomPlace::getKakaoPlaceId)
				.distinct()
				.toList();

		Map<String, PlaceBusinessHours> businessHoursByKakaoId =
				placeBusinessHoursRepository.findByKakaoPlaceIdIn(kakaoPlaceIds).stream()
						.collect(Collectors.toMap(PlaceBusinessHours::getKakaoPlaceId, Function.identity()));

		List<AvailableCandidate> candidates = roomPlaces.stream()
				.filter(rp -> {
					PlaceBusinessHours pbh = businessHoursByKakaoId.get(rp.getKakaoPlaceId());
					if (pbh == null || pbh.getBusinessHoursJson() == null) {
						return false;
					}
					return businessHoursAtTimeChecker.isOpenAt(pbh.getBusinessHoursJson(), startDateTime);
				})
				.map(rp -> {
					PlaceBusinessHours pbh = businessHoursByKakaoId.get(rp.getKakaoPlaceId());
					return new AvailableCandidate(rp, pbh.getBusinessHoursJson());
				})
				.toList();

		return new AvailablePool(candidates);
	}
}
