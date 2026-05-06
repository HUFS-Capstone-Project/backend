package com.hufs.capstone.backend.region.application.impl;

import com.hufs.capstone.backend.region.application.RegionAddressResolver;
import com.hufs.capstone.backend.region.application.dto.ResolvedRegion;
import com.hufs.capstone.backend.region.domain.entity.RegionSido;
import com.hufs.capstone.backend.region.domain.entity.RegionSigungu;
import com.hufs.capstone.backend.region.domain.repository.RegionSidoRepository;
import com.hufs.capstone.backend.region.domain.repository.RegionSigunguRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RegionAddressResolverImpl implements RegionAddressResolver {

	private final RegionSidoRepository regionSidoRepository;
	private final RegionSigunguRepository regionSigunguRepository;

	@Override
	@Transactional(readOnly = true)
	public ResolvedRegion resolve(String address, String roadAddress) {
		ResolvedRegion roadAddressRegion = resolveOne(roadAddress);
		if (roadAddressRegion.sidoCode() != null) {
			return roadAddressRegion;
		}
		return resolveOne(address);
	}

	private ResolvedRegion resolveOne(String address) {
		String normalizedAddress = normalizeAddress(address);
		if (!StringUtils.hasText(normalizedAddress)) {
			return ResolvedRegion.unresolved();
		}
		for (SidoMatcher sidoMatcher : activeSidoMatchers()) {
			String rest = sidoMatcher.removePrefix(normalizedAddress);
			if (rest == null) {
				continue;
			}
			RegionSigungu sigungu = resolveSigungu(sidoMatcher.sido(), rest);
			return new ResolvedRegion(
					sidoMatcher.sido().getCode(),
					sidoMatcher.sido().getName(),
					sigungu == null ? null : sigungu.getCode(),
					sigungu == null ? null : sigungu.getName()
			);
		}
		return ResolvedRegion.unresolved();
	}

	private RegionSigungu resolveSigungu(RegionSido sido, String rest) {
		String normalizedRest = normalizeAddress(rest);
		if (!StringUtils.hasText(normalizedRest)) {
			return null;
		}
		return activeSigungus().stream()
				.filter(sigungu -> sido.getCode().equals(sigungu.getSido().getCode()))
				.filter(sigungu -> normalizedRest.startsWith(normalizeAddress(sigungu.getName())))
				.max(Comparator.comparingInt(sigungu -> normalizeAddress(sigungu.getName()).length()))
				.orElse(null);
	}

	@Cacheable(cacheNames = "regionSidoMatchers", key = "'all'")
	public List<SidoMatcher> activeSidoMatchers() {
		List<SidoMatcher> matchers = new ArrayList<>();
		for (RegionSido sido : regionSidoRepository.findAllByActiveTrueOrderByDisplayOrderAscCodeAsc()) {
			matchers.add(SidoMatcher.from(sido));
		}
		return List.copyOf(matchers);
	}

	@Cacheable(cacheNames = "regionSigunguEntities", key = "'all'")
	public List<RegionSigungu> activeSigungus() {
		return List.copyOf(regionSigunguRepository.findAllActiveWithSido());
	}

	private static String normalizeAddress(String value) {
		if (value == null) {
			return null;
		}
		return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	public record SidoMatcher(
			RegionSido sido,
			List<String> aliases
	) {

		private static SidoMatcher from(RegionSido sido) {
			return new SidoMatcher(sido, aliasesOf(sido));
		}

		private String removePrefix(String address) {
			for (String alias : aliases) {
				String normalizedAlias = normalizeAddress(alias);
				if (address.equals(normalizedAlias)) {
					return "";
				}
				if (address.startsWith(normalizedAlias + " ")) {
					return address.substring(normalizedAlias.length()).trim();
				}
			}
			return null;
		}

		private static List<String> aliasesOf(RegionSido sido) {
			return switch (sido.getCode()) {
				case "11" -> List.of("서울특별시", "서울시", "서울");
				case "26" -> List.of("부산광역시", "부산시", "부산");
				case "27" -> List.of("대구광역시", "대구시", "대구");
				case "28" -> List.of("인천광역시", "인천시", "인천");
				case "29" -> List.of("광주광역시", "광주시", "광주");
				case "30" -> List.of("대전광역시", "대전시", "대전");
				case "31" -> List.of("울산광역시", "울산시", "울산");
				case "36" -> List.of("세종특별자치시", "세종시", "세종");
				case "41" -> List.of("경기도", "경기");
				case "51" -> List.of("강원특별자치도", "강원도", "강원");
				case "43" -> List.of("충청북도", "충북");
				case "44" -> List.of("충청남도", "충남");
				case "52" -> List.of("전북특별자치도", "전라북도", "전북");
				case "46" -> List.of("전라남도", "전남");
				case "47" -> List.of("경상북도", "경북");
				case "48" -> List.of("경상남도", "경남");
				case "50" -> List.of("제주특별자치도", "제주도", "제주");
				default -> List.of(sido.getName());
			};
		}
	}
}
