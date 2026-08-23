package com.hufs.capstone.backend.region.application.impl;

import com.hufs.capstone.backend.region.application.RegionAddressCatalog;
import com.hufs.capstone.backend.region.application.RegionAddressCatalogProvider;
import com.hufs.capstone.backend.region.application.RegionAddressResolver;
import com.hufs.capstone.backend.region.domain.vo.ResolvedRegion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RegionAddressResolverImpl implements RegionAddressResolver {

	private final RegionAddressCatalogProvider catalogProvider;

	@Override
	public ResolvedRegion resolve(String address, String roadAddress) {
		if (!StringUtils.hasText(address) && !StringUtils.hasText(roadAddress)) {
			return ResolvedRegion.unresolved();
		}
		RegionAddressCatalog catalog = catalogProvider.getCatalog();
		ResolvedRegion roadAddressRegion = resolveOne(catalog, roadAddress);
		if (roadAddressRegion.sidoCode() != null) {
			return roadAddressRegion;
		}
		return resolveOne(catalog, address);
	}

	private static ResolvedRegion resolveOne(RegionAddressCatalog catalog, String address) {
		RegionAddressCatalog.ResolvedAddress resolved = catalog.resolve(address);
		if (resolved == null) {
			return ResolvedRegion.unresolved();
		}
		RegionAddressCatalog.SigunguMatcher sigungu = resolved.sigungu();
		return new ResolvedRegion(
				resolved.sido().code(),
				resolved.sido().name(),
				sigungu == null ? null : sigungu.code(),
				sigungu == null ? null : sigungu.name()
		);
	}
}
