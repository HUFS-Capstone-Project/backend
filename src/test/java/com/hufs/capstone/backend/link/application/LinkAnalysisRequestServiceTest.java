package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.AnalyzeLinkCommand;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisRequestResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.LinkSource;
import com.hufs.capstone.backend.link.domain.ProcessingDispatchStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LinkAnalysisRequestServiceTest {

	private static final Long USER_ID = 100L;
	private static final String ROOM_PUBLIC_ID = "11111111-1111-1111-1111-111111111111";

	@Mock
	private LinkAnalysisRequestWriteService linkAnalysisRequestWriteService;

	@Mock
	private LinkRepository linkRepository;

	@InjectMocks
	private LinkAnalysisRequestService linkAnalysisRequestService;

	@Test
	void requestLinkAnalysisShouldReturnLatestLinkState() {
		LinkAnalysisRequestResult persisted = new LinkAnalysisRequestResult(
				10L,
				null,
				LinkAnalysisStatus.REQUESTED,
				true
		);
		when(linkAnalysisRequestWriteService.requestWithinWriteTransaction(any(), eq(ROOM_PUBLIC_ID), eq(USER_ID), eq("WEB")))
				.thenReturn(persisted);
		when(linkRepository.findById(10L)).thenReturn(Optional.of(link(10L, "job-123", LinkAnalysisStatus.REQUESTED)));

		LinkAnalysisRequestResult result = linkAnalysisRequestService.requestLinkAnalysis(
				USER_ID,
				ROOM_PUBLIC_ID,
				new AnalyzeLinkCommand("https://instagram.com/p/ABC123/?utm_source=test", LinkSource.WEB)
		);

		assertThat(result.linkId()).isEqualTo(10L);
		assertThat(result.processingJobId()).isEqualTo("job-123");
		assertThat(result.createdRequest()).isTrue();
	}

	@Test
	void requestLinkAnalysisShouldRetryWhenNormalizedUrlDuplicateRaceOccurs() {
		when(linkAnalysisRequestWriteService.requestWithinWriteTransaction(any(), eq(ROOM_PUBLIC_ID), eq(USER_ID), eq(null)))
				.thenThrow(new LinkAnalysisRequestWriteService.LinkDuplicateRaceException(
						"https://example.com/x",
						new RuntimeException("dup")
				))
				.thenReturn(new LinkAnalysisRequestResult(
						30L,
						"job-existing",
						LinkAnalysisStatus.REQUESTED,
						false
				));
		when(linkRepository.findById(30L)).thenReturn(Optional.of(link(30L, "job-existing", LinkAnalysisStatus.REQUESTED)));

		LinkAnalysisRequestResult result = linkAnalysisRequestService.requestLinkAnalysis(
				USER_ID,
				ROOM_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/x", null)
		);

		assertThat(result.linkId()).isEqualTo(30L);
		assertThat(result.createdRequest()).isFalse();
		verify(linkAnalysisRequestWriteService, times(2))
				.requestWithinWriteTransaction(any(), eq(ROOM_PUBLIC_ID), eq(USER_ID), eq(null));
	}

	@Test
	void requestLinkAnalysisShouldRetryWhenAnalysisRequestDuplicateRaceOccurs() {
		when(linkAnalysisRequestWriteService.requestWithinWriteTransaction(any(), eq(ROOM_PUBLIC_ID), eq(USER_ID), eq(null)))
				.thenThrow(new LinkAnalysisRequestWriteService.LinkAnalysisRequestDuplicateRaceException(
						ROOM_PUBLIC_ID,
						40L,
						new RuntimeException("dup")
				))
				.thenReturn(new LinkAnalysisRequestResult(
						40L,
						"job-existing",
						LinkAnalysisStatus.REQUESTED,
						false
				));
		when(linkRepository.findById(40L)).thenReturn(Optional.of(link(40L, "job-existing", LinkAnalysisStatus.REQUESTED)));

		LinkAnalysisRequestResult result = linkAnalysisRequestService.requestLinkAnalysis(
				USER_ID,
				ROOM_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/x", null)
		);

		assertThat(result.linkId()).isEqualTo(40L);
		assertThat(result.createdRequest()).isFalse();
		verify(linkAnalysisRequestWriteService, times(2))
				.requestWithinWriteTransaction(any(), eq(ROOM_PUBLIC_ID), eq(USER_ID), eq(null));
	}

	@Test
	void requestLinkAnalysisShouldStopWhenDuplicateRaceRetryIsExhausted() {
		when(linkAnalysisRequestWriteService.requestWithinWriteTransaction(any(), eq(ROOM_PUBLIC_ID), eq(USER_ID), eq(null)))
				.thenThrow(new LinkAnalysisRequestWriteService.LinkAnalysisRequestDuplicateRaceException(
						ROOM_PUBLIC_ID,
						50L,
						new RuntimeException("dup")
				));

		assertThatThrownBy(() -> linkAnalysisRequestService.requestLinkAnalysis(
				USER_ID,
				ROOM_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/x", null)
		))
				.isInstanceOf(LinkAnalysisRequestWriteService.LinkAnalysisRequestDuplicateRaceException.class);
		verify(linkAnalysisRequestWriteService, times(3))
				.requestWithinWriteTransaction(any(), eq(ROOM_PUBLIC_ID), eq(USER_ID), eq(null));
	}

	@Test
	void requestLinkAnalysisShouldFailWhenRoomIdIsBlank() {
		assertThatThrownBy(() -> linkAnalysisRequestService.requestLinkAnalysis(
				USER_ID,
				"  ",
				new AnalyzeLinkCommand("https://example.com/x", null)
		))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E400_ILLEGAL_ARGUMENT));
	}

	private static Link link(Long id, String processingJobId, LinkAnalysisStatus status) {
		Link link = Link.register("https://example.com/p/1", "https://example.com/p/1", processingJobId);
		ReflectionTestUtils.setField(link, "id", id);
		ReflectionTestUtils.setField(link, "dispatchStatus", ProcessingDispatchStatus.DISPATCHED);
		if (status == LinkAnalysisStatus.PROCESSING) {
			link.markProcessing();
		}
		if (status == LinkAnalysisStatus.FAILED) {
			link.markFailed();
		}
		if (status == LinkAnalysisStatus.SUCCEEDED) {
			link.markSucceeded("done");
		}
		return link;
	}
}
