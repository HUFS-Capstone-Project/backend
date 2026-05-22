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
				    "categoryName": "가정,생활 > 여가시설 > 보드카페 > 레드버튼",
				    "categoryGroupCode": "CE7"
				  }
				]
				""");
		when(placeTaxonomyReadService.resolveCategory(
				"CE7",
				"가정,생활 > 여가시설 > 보드카페 > 레드버튼"
		)).thenReturn(new ResolvedPlaceCategory("ACTIVITY", "놀거리"));

		LinkAnalysisResult result = assembler.from(link);

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(result.originalUrl()).isEqualTo("https://example.com/p/1");
		assertThat(result.candidatePlaces()).hasSize(1);
		LinkPlaceResult candidate = result.candidatePlaces().get(0);
		assertThat(candidate.categoryGroupCode()).isEqualTo("CE7");
		assertThat(candidate.serviceCategoryCode()).isEqualTo("ACTIVITY");
		assertThat(candidate.serviceCategoryName()).isEqualTo("놀거리");
	}
}
