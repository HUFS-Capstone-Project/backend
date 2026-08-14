package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.global.cache.CacheNames;
import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTag;
import com.hufs.capstone.backend.place.domain.repository.PlaceCategoryRepository;
import com.hufs.capstone.backend.place.domain.repository.PlaceTagRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PlaceTaxonomyCatalogProvider {

	private final PlaceCategoryRepository placeCategoryRepository;
	private final PlaceTagRepository placeTagRepository;

	@Cacheable(cacheNames = CacheNames.PLACE_TAXONOMY_CATALOG, key = "'all'", sync = true)
	@Transactional(readOnly = true)
	public PlaceTaxonomyCatalog getCatalog() {
		List<PlaceCategory> categories = placeCategoryRepository.findActiveCategories();
		List<PlaceTag> tags = placeTagRepository.findActiveTaxonomyTags();
		return PlaceTaxonomyCatalog.from(categories, tags);
	}
}
