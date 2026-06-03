package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.global.exception.FieldValidationException;
import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.repository.PlaceCategoryRepository;
import com.hufs.capstone.backend.place.domain.repository.PlaceTagRepository;
import com.hufs.capstone.backend.region.domain.repository.RegionSigunguRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class DateCourseInputValidator {

	private static final int MAX_CATEGORY_SEQUENCE_SIZE = 5;

	private final RegionSigunguRepository regionSigunguRepository;
	private final PlaceCategoryRepository placeCategoryRepository;
	private final PlaceTagRepository placeTagRepository;

	void validate(String sigunguCode, Instant startDateTime, Instant endDateTime, List<CategorySlotCommand> slots) {
		validateSigunguCode(sigunguCode);
		validateDateTimeRange(startDateTime, endDateTime);
		validateSlots(slots);
		for (CategorySlotCommand slot : slots) {
			validateSlot(slot);
		}
	}

	private void validateSigunguCode(String sigunguCode) {
		if (sigunguCode == null || sigunguCode.isBlank()) {
			throw new FieldValidationException("sigunguCode", "시/군/구 코드는 필수입니다.");
		}
		regionSigunguRepository.findActiveByCode(sigunguCode)
				.orElseThrow(() -> new FieldValidationException(
						"sigunguCode", "유효하지 않은 시/군/구 코드입니다.", sigunguCode));
	}

	private void validateDateTimeRange(Instant startDateTime, Instant endDateTime) {
		if (startDateTime == null) {
			throw new FieldValidationException("startDateTime", "시작 일시는 필수입니다.");
		}
		if (endDateTime == null) {
			throw new FieldValidationException("endDateTime", "종료 일시는 필수입니다.");
		}
		if (!startDateTime.isBefore(endDateTime)) {
			throw new FieldValidationException("startDateTime", "시작 일시는 종료 일시보다 이전이어야 합니다.", startDateTime);
		}
	}

	private void validateSlots(List<CategorySlotCommand> slots) {
		if (slots == null || slots.isEmpty()) {
			throw new FieldValidationException("categorySequence", "카테고리 순서는 필수입니다.");
		}
		if (slots.size() > MAX_CATEGORY_SEQUENCE_SIZE) {
			throw new FieldValidationException("categorySequence", "카테고리 순서는 최대 5개까지 가능합니다.", slots.size());
		}
	}

	private void validateSlot(CategorySlotCommand slot) {
		if (slot == null || slot.categoryCode() == null || slot.categoryCode().isBlank()) {
			throw new FieldValidationException("categorySequence[].categoryCode", "카테고리 코드는 필수입니다.");
		}
		PlaceCategory category = placeCategoryRepository.findByCode(slot.categoryCode())
				.filter(PlaceCategory::isActive)
				.orElseThrow(() -> new FieldValidationException(
						"categorySequence[].categoryCode", "유효하지 않은 카테고리 코드입니다.", slot.categoryCode()));

		if (!slot.isWildcard()) {
			placeTagRepository.findByCategoryAndCode(category, slot.tagCode())
					.filter(tag -> tag.isActive())
					.orElseThrow(() -> new FieldValidationException(
							"categorySequence[].tagCode", "유효하지 않은 태그 코드입니다.", slot.tagCode()));
		}
	}
}
