package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.place.application.dto.ResolvedPlaceCategory;
import com.hufs.capstone.backend.place.application.dto.ResolvedPlaceTaxonomy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceTaxonomyReadService {

	private final PlaceTaxonomyResolver placeTaxonomyResolver;

	@Transactional(readOnly = true)
	public ResolvedPlaceTaxonomy resolveTaxonomy(String kakaoCategoryGroupCode, String kakaoCategoryName) {
		return placeTaxonomyResolver.resolve(kakaoCategoryGroupCode, kakaoCategoryName);
	}

	@Transactional(readOnly = true)
	public ResolvedPlaceCategory resolveCategory(String kakaoCategoryGroupCode, String kakaoCategoryName) {
		return placeTaxonomyResolver.resolveCategory(kakaoCategoryGroupCode, kakaoCategoryName);
	}
}
