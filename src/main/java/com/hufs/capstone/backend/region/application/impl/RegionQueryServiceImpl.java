package com.hufs.capstone.backend.region.application.impl;

import com.hufs.capstone.backend.global.exception.FieldValidationException;
import com.hufs.capstone.backend.region.application.RegionQueryService;
import com.hufs.capstone.backend.region.application.dto.RegionFilter;
import com.hufs.capstone.backend.region.application.dto.RegionOptionResult;
import com.hufs.capstone.backend.region.domain.entity.RegionSido;
import com.hufs.capstone.backend.region.domain.entity.RegionSigungu;
import com.hufs.capstone.backend.region.domain.repository.RegionSidoRepository;
import com.hufs.capstone.backend.region.domain.repository.RegionSigunguRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegionQueryServiceImpl implements RegionQueryService {

	private static final String ALL_CODE = "ALL";
	private static final String ALL_NAME = "전체";
	private static final int ALL_DISPLAY_ORDER = 0;

	private final RegionSidoRepository regionSidoRepository;
	private final RegionSigunguRepository regionSigunguRepository;

	@Override
	@Transactional(readOnly = true)
	@Cacheable(cacheNames = "regionSidos", key = "'all'")
	public List<RegionOptionResult> getSidos() {
		List<RegionSido> sidos = regionSidoRepository.findAllByActiveTrueOrderByDisplayOrderAscCodeAsc();
		List<RegionOptionResult> results = new ArrayList<>(sidos.size() + 1);
		results.add(allOption());
		for (RegionSido sido : sidos) {
			results.add(new RegionOptionResult(sido.getCode(), sido.getName(), sido.getDisplayOrder(), false));
		}
		return List.copyOf(results);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(cacheNames = "regionSigungus", key = "#sidoCode")
	public List<RegionOptionResult> getSigungus(String sidoCode) {
		String normalizedSidoCode = normalize(sidoCode);
		RegionSido sido = regionSidoRepository.findByCodeAndActiveTrue(normalizedSidoCode)
				.orElseThrow(() -> invalidRegion("sidoCode", "유효하지 않은 시/도 코드입니다.", sidoCode));
		List<RegionSigungu> sigungus = regionSigunguRepository.findActiveBySidoCode(sido.getCode());
		List<RegionOptionResult> results = new ArrayList<>(sigungus.size() + 1);
		results.add(allOption());
		for (RegionSigungu sigungu : sigungus) {
			results.add(new RegionOptionResult(
					sigungu.getCode(),
					sigungu.getName(),
					sigungu.getDisplayOrder(),
					false
			));
		}
		return List.copyOf(results);
	}

	@Override
	@Transactional(readOnly = true)
	public RegionFilter validateFilter(String sidoCode, String sigunguCode) {
		String normalizedSidoCode = normalize(sidoCode);
		String normalizedSigunguCode = normalize(sigunguCode);
		if (normalizedSidoCode == null && normalizedSigunguCode == null) {
			return new RegionFilter(null, null);
		}
		if (normalizedSidoCode == null) {
			throw invalidRegion("sidoCode", "시/군/구 코드가 있으면 시/도 코드는 필수입니다.", sidoCode);
		}
		RegionSido sido = regionSidoRepository.findByCodeAndActiveTrue(normalizedSidoCode)
				.orElseThrow(() -> invalidRegion("sidoCode", "유효하지 않은 시/도 코드입니다.", sidoCode));
		if (normalizedSigunguCode == null) {
			return new RegionFilter(sido.getCode(), null);
		}
		RegionSigungu sigungu = regionSigunguRepository.findActiveByCode(normalizedSigunguCode)
				.orElseThrow(() -> invalidRegion("sigunguCode", "유효하지 않은 시/군/구 코드입니다.", sigunguCode));
		if (!sido.getCode().equals(sigungu.getSido().getCode())) {
			throw invalidRegion("sigunguCode", "시/도 코드와 시/군/구 코드가 일치하지 않습니다.", sigunguCode);
		}
		return new RegionFilter(sido.getCode(), sigungu.getCode());
	}

	private static RegionOptionResult allOption() {
		return new RegionOptionResult(ALL_CODE, ALL_NAME, ALL_DISPLAY_ORDER, true);
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty() || ALL_CODE.equalsIgnoreCase(trimmed)) {
			return null;
		}
		return trimmed;
	}

	private static FieldValidationException invalidRegion(String field, String message, Object rejectedValue) {
		return new FieldValidationException(field, message, rejectedValue);
	}
}
