package com.hufs.capstone.backend.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.global.exception.FieldValidationException;
import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTag;
import com.hufs.capstone.backend.place.domain.repository.PlaceCategoryRepository;
import com.hufs.capstone.backend.place.domain.repository.PlaceTagRepository;
import com.hufs.capstone.backend.region.domain.entity.RegionSigungu;
import com.hufs.capstone.backend.region.domain.repository.RegionSigunguRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DateCourseInputValidatorTest {

	private final RegionSigunguRepository regionRepository = mock(RegionSigunguRepository.class);
	private final PlaceCategoryRepository categoryRepository = mock(PlaceCategoryRepository.class);
	private final PlaceTagRepository tagRepository = mock(PlaceTagRepository.class);
	private final DateCourseInputValidator validator =
			new DateCourseInputValidator(regionRepository, categoryRepository, tagRepository);

	@Test
	void startDateTimeMustBeBeforeEndDateTime() {
		when(regionRepository.findActiveByCode("11680")).thenReturn(Optional.of(mock(RegionSigungu.class)));

		assertThatThrownBy(() -> validator.validate(
				"11680",
				Instant.parse("2026-06-03T12:00:00Z"),
				Instant.parse("2026-06-03T12:00:00Z"),
				List.of(new CategorySlotCommand("FOOD", "KOREAN"))
		)).isInstanceOf(FieldValidationException.class)
				.satisfies(ex -> assertThat(((FieldValidationException) ex).getFieldErrors())
						.anySatisfy(error -> {
							assertThat(error.field()).isEqualTo("startDateTime");
							assertThat(error.message()).isEqualTo("시작 일시는 종료 일시보다 이전이어야 합니다.");
						}));
	}

	@Test
	void categoryTagCombinationMustExist() {
		PlaceCategory category = mock(PlaceCategory.class);
		when(category.isActive()).thenReturn(true);
		when(regionRepository.findActiveByCode("11680")).thenReturn(Optional.of(mock(RegionSigungu.class)));
		when(categoryRepository.findByCode("FOOD")).thenReturn(Optional.of(category));
		when(tagRepository.findByCategoryAndCode(category, "KOREAN")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> validator.validate(
				"11680",
				Instant.parse("2026-06-03T12:00:00Z"),
				Instant.parse("2026-06-03T14:00:00Z"),
				List.of(new CategorySlotCommand("FOOD", "KOREAN"))
		)).isInstanceOf(FieldValidationException.class)
				.satisfies(ex -> assertThat(((FieldValidationException) ex).getFieldErrors())
						.anySatisfy(error -> {
							assertThat(error.field()).isEqualTo("categorySequence[].tagCode");
							assertThat(error.message()).isEqualTo("유효하지 않은 태그 코드입니다.");
						}));
	}

	@Test
	void validWildcardSlotPassesWithoutTagLookup() {
		PlaceCategory category = mock(PlaceCategory.class);
		when(category.isActive()).thenReturn(true);
		when(regionRepository.findActiveByCode("11680")).thenReturn(Optional.of(mock(RegionSigungu.class)));
		when(categoryRepository.findByCode("CAFE")).thenReturn(Optional.of(category));

		validator.validate(
				"11680",
				Instant.parse("2026-06-03T12:00:00Z"),
				Instant.parse("2026-06-03T14:00:00Z"),
				List.of(new CategorySlotCommand("CAFE", null))
		);
	}

	@Test
	void validCategoryTagCombinationPasses() {
		PlaceCategory category = mock(PlaceCategory.class);
		PlaceTag tag = mock(PlaceTag.class);
		when(category.isActive()).thenReturn(true);
		when(tag.isActive()).thenReturn(true);
		when(regionRepository.findActiveByCode("11680")).thenReturn(Optional.of(mock(RegionSigungu.class)));
		when(categoryRepository.findByCode("FOOD")).thenReturn(Optional.of(category));
		when(tagRepository.findByCategoryAndCode(category, "KOREAN")).thenReturn(Optional.of(tag));

		validator.validate(
				"11680",
				Instant.parse("2026-06-03T12:00:00Z"),
				Instant.parse("2026-06-03T14:00:00Z"),
				List.of(new CategorySlotCommand("FOOD", "KOREAN"))
		);
	}
}
