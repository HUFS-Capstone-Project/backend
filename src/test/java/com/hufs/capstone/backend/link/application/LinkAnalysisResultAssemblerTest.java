package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import com.hufs.capstone.backend.link.application.dto.LinkPlaceResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.place.application.PlaceTaxonomyReadService;
import com.hufs.capstone.backend.place.application.dto.ResolvedPlaceCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LinkAnalysisResultAssemblerTest {

	@Mock
	private PlaceTaxonomyReadService placeTaxonomyReadService;

	private LinkAnalysisResultAssembler assembler;

	@BeforeEach
	void setUp() {
		assembler = new LinkAnalysisResultAssembler(
				new LinkPlaceCandidateSnapshotMapper(new ObjectMapper()),
				placeTaxonomyReadService
		);
	}

	@Test
	void shouldIncludeServiceCategoryForExtractedCandidatePlaces() {
		Link link = Link.register("https://example.com/p/1", "https://example.com/p/1", "job-1");
		ReflectionTestUtils.setField(link, "id", 1L);
		link.markSucceeded("content");
		ReflectionTestUtils.setField(link, "extractedPlacesJson", """
				[
				  {
				    "kakaoPlaceId": "123",
				    "placeName": "Red Button",
				    "categoryName": "\uAC00\uC815,\uC0DD\uD65C > \uC5EC\uAC00\uC2DC\uC124 > \uBCF4\uB4DC\uCE74\uD398 > \uB808\uB4DC\uBC84\uD2BC",
				    "categoryGroupCode": "CE7"
				  }
				]
				""");
		when(placeTaxonomyReadService.resolveCategory(
				"CE7",
				"\uAC00\uC815,\uC0DD\uD65C > \uC5EC\uAC00\uC2DC\uC124 > \uBCF4\uB4DC\uCE74\uD398 > \uB808\uB4DC\uBC84\uD2BC"
		)).thenReturn(new ResolvedPlaceCategory("ACTIVITY", "\uB180\uAC70\uB9AC"));

		LinkAnalysisResult result = assembler.from(link);

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(result.originalUrl()).isEqualTo("https://example.com/p/1");
		assertThat(result.candidatePlaces()).hasSize(1);
		LinkPlaceResult candidate = result.candidatePlaces().get(0);
		assertThat(candidate.categoryGroupCode()).isEqualTo("CE7");
		assertThat(candidate.serviceCategoryCode()).isEqualTo("ACTIVITY");
		assertThat(candidate.serviceCategoryName()).isEqualTo("\uB180\uAC70\uB9AC");
	}
}
