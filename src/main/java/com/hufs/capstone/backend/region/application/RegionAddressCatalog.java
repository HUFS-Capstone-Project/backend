package com.hufs.capstone.backend.region.application;

import com.hufs.capstone.backend.region.domain.entity.RegionSido;
import com.hufs.capstone.backend.region.domain.entity.RegionSigungu;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record RegionAddressCatalog(
		List<SidoMatcher> sidos,
		Map<String, List<SigunguMatcher>> sigungusBySidoCode
) {

	public RegionAddressCatalog {
		sidos = List.copyOf(sidos);
		Map<String, List<SigunguMatcher>> immutableSigungus = new LinkedHashMap<>();
		sigungusBySidoCode.forEach((code, sigungus) -> immutableSigungus.put(code, List.copyOf(sigungus)));
		sigungusBySidoCode = Map.copyOf(immutableSigungus);
	}

	public static RegionAddressCatalog from(List<RegionSido> sidos, List<RegionSigungu> sigungus) {
		List<SidoMatcher> sidoMatchers = sidos.stream()
				.map(SidoMatcher::from)
				.toList();
		Map<String, List<SigunguMatcher>> sigunguMatchers = new LinkedHashMap<>();
		for (RegionSigungu sigungu : sigungus) {
			sigunguMatchers.computeIfAbsent(sigungu.getSido().getCode(), ignored -> new ArrayList<>())
					.add(SigunguMatcher.from(sigungu));
		}
		return new RegionAddressCatalog(sidoMatchers, sigunguMatchers);
	}

	public ResolvedAddress resolve(String address) {
		String normalizedAddress = normalize(address);
		if (normalizedAddress == null || normalizedAddress.isBlank()) {
			return null;
		}
		for (SidoMatcher sido : sidos) {
			String rest = sido.removePrefix(normalizedAddress);
			if (rest == null) {
				continue;
			}
			SigunguMatcher sigungu = findSigungu(sido.code(), rest);
			return new ResolvedAddress(sido, sigungu);
		}
		return null;
	}

	private SigunguMatcher findSigungu(String sidoCode, String rest) {
		String normalizedRest = normalize(rest);
		if (normalizedRest == null || normalizedRest.isBlank()) {
			return null;
		}
		return sigungusBySidoCode.getOrDefault(sidoCode, List.of()).stream()
				.filter(sigungu -> normalizedRest.startsWith(sigungu.normalizedName()))
				.max(Comparator.comparingInt(sigungu -> sigungu.normalizedName().length()))
				.orElse(null);
	}

	static String normalize(String value) {
		if (value == null) {
			return null;
		}
		return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	public record ResolvedAddress(SidoMatcher sido, SigunguMatcher sigungu) {
	}

	public record SidoMatcher(String code, String name, List<String> normalizedAliases) {

		public SidoMatcher {
			normalizedAliases = List.copyOf(normalizedAliases);
		}

		private static SidoMatcher from(RegionSido sido) {
			return new SidoMatcher(
					sido.getCode(),
					sido.getName(),
					aliasesOf(sido).stream().map(RegionAddressCatalog::normalize).toList()
			);
		}

		private String removePrefix(String address) {
			for (String alias : normalizedAliases) {
				if (address.equals(alias)) {
					return "";
				}
				if (address.startsWith(alias + " ")) {
					return address.substring(alias.length()).trim();
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

	public record SigunguMatcher(String code, String name, String normalizedName) {

		private static SigunguMatcher from(RegionSigungu sigungu) {
			return new SigunguMatcher(sigungu.getCode(), sigungu.getName(), normalize(sigungu.getName()));
		}
	}
}
