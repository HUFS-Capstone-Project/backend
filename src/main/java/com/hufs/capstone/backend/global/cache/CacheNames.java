package com.hufs.capstone.backend.global.cache;

import java.util.List;

public final class CacheNames {

	public static final String PLACE_TAXONOMY_CATALOG = "placeTaxonomyCatalog";
	public static final String PLACE_TAXONOMY_RESPONSE = "placeTaxonomyResponse";
	public static final String REGION_SIDOS = "regionSidos";
	public static final String REGION_SIGUNGUS = "regionSigungus";
	public static final String REGION_ADDRESS_CATALOG = "regionAddressCatalog";
	public static final String LINK_ANALYSIS_RESULTS = "linkAnalysisResults";

	public static final List<String> REGION_CACHES = List.of(
			REGION_SIDOS,
			REGION_SIGUNGUS,
			REGION_ADDRESS_CATALOG
	);
	public static final List<String> PLACE_TAXONOMY_CACHES = List.of(
			PLACE_TAXONOMY_CATALOG,
			PLACE_TAXONOMY_RESPONSE
	);

	private CacheNames() {
	}
}
