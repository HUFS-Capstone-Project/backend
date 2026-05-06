package com.hufs.capstone.backend.region.application.impl;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
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
				.orElseThrow(() -> invalidRegion("Invalid sidoCode."));
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
			throw invalidRegion("sidoCode is required when sigunguCode is provided.");
		}
		RegionSido sido = regionSidoRepository.findByCodeAndActiveTrue(normalizedSidoCode)
				.orElseThrow(() -> invalidRegion("Invalid sidoCode."));
		if (normalizedSigunguCode == null) {
			return new RegionFilter(sido.getCode(), null);
		}
		RegionSigungu sigungu = regionSigunguRepository.findActiveByCode(normalizedSigunguCode)
				.orElseThrow(() -> invalidRegion("Invalid sigunguCode."));
		if (!sido.getCode().equals(sigungu.getSido().getCode())) {
			throw invalidRegion("sidoCode and sigunguCode do not match.");
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

	private static BusinessException invalidRegion(String message) {
		return new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, message);
	}
}
