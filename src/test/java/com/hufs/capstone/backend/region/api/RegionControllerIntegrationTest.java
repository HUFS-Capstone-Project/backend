package com.hufs.capstone.backend.region.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hufs.capstone.backend.global.cache.CacheNames;
import com.hufs.capstone.backend.region.domain.repository.RegionSidoRepository;
import com.hufs.capstone.backend.region.domain.repository.RegionSigunguRepository;
import com.hufs.capstone.backend.region.infrastructure.seed.RegionSeedDataInitializer;
import com.hufs.capstone.backend.region.infrastructure.seed.RegionSeedReadinessHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
	"management.endpoint.health.probes.enabled=true",
	"management.endpoint.health.group.readiness.include=readinessState,regionSeedReadiness"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
class RegionControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private RegionSeedDataInitializer seedDataInitializer;

	@Autowired
	private RegionSeedReadinessHealthIndicator readiness;

	@Autowired
	private RegionSidoRepository regionSidoRepository;

	@Autowired
	private RegionSigunguRepository regionSigunguRepository;

	@Test
	@WithMockUser
	void shouldReturnSidosWithVirtualAllOption() throws Exception {
		mockMvc.perform(get("/api/v1/regions/sidos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].code").value("ALL"))
				.andExpect(jsonPath("$.data[0].name").value("전체"))
				.andExpect(jsonPath("$.data[0].all").value(true))
				.andExpect(jsonPath("$.data[1].code").value("11"))
				.andExpect(jsonPath("$.data[1].name").value("서울특별시"));
	}

	@Test
	@WithMockUser
	void shouldReturnSigungusWithVirtualAllOption() throws Exception {
		mockMvc.perform(get("/api/v1/regions/sidos/11/sigungus"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].code").value("ALL"))
				.andExpect(jsonPath("$.data[0].name").value("전체"))
				.andExpect(jsonPath("$.data[0].all").value(true))
				.andExpect(jsonPath("$.data[1].code").value("11680"))
				.andExpect(jsonPath("$.data[1].name").value("강남구"));
	}

	@Test
	@WithMockUser
	void shouldRejectInvalidSidoCode() throws Exception {
		mockMvc.perform(get("/api/v1/regions/sidos/99/sigungus"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("E400_VALIDATION"))
				.andExpect(jsonPath("$.detail").value("입력값을 확인해 주세요."))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("sidoCode"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("유효하지 않은 시/도 코드입니다."));
	}

	@Test
	void shouldKeepLivenessUpWhileRegionSeedReadinessIsClosed() throws Exception {
		readiness.markSeeding();
		try {
			mockMvc.perform(get("/actuator/health/readiness"))
					.andExpect(status().isServiceUnavailable());
			mockMvc.perform(get("/actuator/health"))
					.andExpect(status().isServiceUnavailable());
			mockMvc.perform(get("/actuator/health/liveness"))
					.andExpect(status().isOk());
		} finally {
			readiness.markReady();
		}

		mockMvc.perform(get("/actuator/health/readiness"))
				.andExpect(status().isOk());
	}

	@Test
	void shouldClearEveryRegionCacheAfterSeedCompletes() throws Exception {
		for (String cacheName : CacheNames.REGION_CACHES) {
			requireCache(cacheName).put("stale", "value");
		}

		seedDataInitializer.run(new DefaultApplicationArguments(new String[0]));

		for (String cacheName : CacheNames.REGION_CACHES) {
			assertCacheEntryWasCleared(cacheName);
		}
		org.assertj.core.api.Assertions.assertThat(regionSidoRepository.count()).isEqualTo(17L);
		org.assertj.core.api.Assertions.assertThat(regionSigunguRepository.count()).isEqualTo(255L);
	}

	private Cache requireCache(String cacheName) {
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) {
			throw new IllegalStateException("Missing cache: " + cacheName);
		}
		return cache;
	}

	private void assertCacheEntryWasCleared(String cacheName) {
		org.assertj.core.api.Assertions.assertThat(requireCache(cacheName).get("stale")).isNull();
	}
}
