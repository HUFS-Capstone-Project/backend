package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.repository.PlaceCategoryRepository;
import com.hufs.capstone.backend.place.domain.repository.PlaceTagRepository;
import com.hufs.capstone.backend.region.domain.repository.RegionSigunguRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class DateCourseInputValidator {

	private final RegionSigunguRepository regionSigunguRepository;
	private final PlaceCategoryRepository placeCategoryRepository;
	private final PlaceTagRepository placeTagRepository;

	void validate(String sigunguCode, List<CategorySlotCommand> slots) {
		validateSigunguCode(sigunguCode);
		for (CategorySlotCommand slot : slots) {
			validateSlot(slot);
		}
	}

	private void validateSigunguCode(String sigunguCode) {
		regionSigunguRepository.findActiveByCode(sigunguCode)
				.orElseThrow(() -> new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT,
						"유효하지 않은 시군구 코드입니다: " + sigunguCode));
	}

	private void validateSlot(CategorySlotCommand slot) {
		PlaceCategory category = placeCategoryRepository.findByCode(slot.categoryCode())
				.filter(PlaceCategory::isActive)
				.orElseThrow(() -> new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT,
						"유효하지 않은 카테고리 코드입니다: " + slot.categoryCode()));

		if (!slot.isWildcard()) {
			placeTagRepository.findByCategoryAndCode(category, slot.tagCode())
					.filter(tag -> tag.isActive())
					.orElseThrow(() -> new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT,
							"유효하지 않은 태그 코드입니다: " + slot.tagCode()));
		}
	}
}