package com.hufs.capstone.backend.region.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import com.hufs.capstone.backend.region.domain.vo.ResolvedRegion;
import com.hufs.capstone.backend.region.application.impl.RegionAddressResolverImpl;
import com.hufs.capstone.backend.region.domain.entity.RegionSido;
import com.hufs.capstone.backend.region.domain.entity.RegionSigungu;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegionAddressResolverImplTest {

	@Mock
	private RegionAddressCatalogProvider catalogProvider;

	private RegionAddressResolverImpl resolver;

	@BeforeEach
	void setUp() {
		RegionSido seoul = RegionSido.create("11", "서울특별시", 1, true);
		RegionSido gyeonggi = RegionSido.create("41", "경기도", 2, true);
		RegionAddressCatalog catalog = RegionAddressCatalog.from(
				List.of(seoul, gyeonggi),
				List.of(
						RegionSigungu.create(seoul, "11110", "종로구", 1, true),
						RegionSigungu.create(gyeonggi, "41110", "수원시", 1, true),
						RegionSigungu.create(gyeonggi, "41111", "수원시 장안구", 2, true)
				)
		);
		lenient().when(catalogProvider.getCatalog()).thenReturn(catalog);
		resolver = new RegionAddressResolverImpl(catalogProvider);
	}

	@Test
	void prefersRoadAddressWhenItResolvesARegion() {
		ResolvedRegion result = resolver.resolve("경기도 수원시 장안구 영화동", "서울시 종로구 자하문로 1");

		assertThat(result.sidoCode()).isEqualTo("11");
		assertThat(result.sigunguCode()).isEqualTo("11110");
	}

	@Test
	void fallsBackToAddressOnlyWhenRoadAddressHasNoSido() {
		ResolvedRegion result = resolver.resolve("경기도 수원시 장안구 영화동", "알 수 없는 도로 1");

		assertThat(result.sidoCode()).isEqualTo("41");
		assertThat(result.sigunguCode()).isEqualTo("41111");
	}

	@Test
	void keepsPartialRoadAddressResultInsteadOfFallingBack() {
		ResolvedRegion result = resolver.resolve("경기도 수원시 영화동", "서울특별시 알 수 없는 구");

		assertThat(result.sidoCode()).isEqualTo("11");
		assertThat(result.sigunguCode()).isNull();
	}

	@Test
	void normalizesWhitespaceAndSupportsSidoAliases() {
		ResolvedRegion result = resolver.resolve(null, "  경기   수원시 장안구   영화동 ");

		assertThat(result.sidoName()).isEqualTo("경기도");
		assertThat(result.sigunguName()).isEqualTo("수원시 장안구");
	}

	@Test
	void returnsUnresolvedForBlankAddresses() {
		ResolvedRegion result = resolver.resolve(" ", null);

		assertThat(result).isEqualTo(ResolvedRegion.unresolved());
	}
}
