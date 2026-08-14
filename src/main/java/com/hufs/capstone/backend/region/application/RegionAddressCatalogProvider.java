package com.hufs.capstone.backend.region.application;

import com.hufs.capstone.backend.global.cache.CacheNames;
import com.hufs.capstone.backend.region.domain.entity.RegionSido;
import com.hufs.capstone.backend.region.domain.entity.RegionSigungu;
import com.hufs.capstone.backend.region.domain.repository.RegionSidoRepository;
import com.hufs.capstone.backend.region.domain.repository.RegionSigunguRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RegionAddressCatalogProvider {

	private final RegionSidoRepository regionSidoRepository;
	private final RegionSigunguRepository regionSigunguRepository;

	@Cacheable(cacheNames = CacheNames.REGION_ADDRESS_CATALOG, key = "'all'", sync = true)
	@Transactional(readOnly = true)
	public RegionAddressCatalog getCatalog() {
		List<RegionSido> sidos = regionSidoRepository.findAllByActiveTrueOrderByDisplayOrderAscCodeAsc();
		List<RegionSigungu> sigungus = regionSigunguRepository.findAllActiveWithSido();
		return RegionAddressCatalog.from(sidos, sigungus);
	}
}
