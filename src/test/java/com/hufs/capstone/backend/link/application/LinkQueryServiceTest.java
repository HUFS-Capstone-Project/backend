package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.external.processing.ProcessingClient;
import com.hufs.capstone.backend.external.processing.ProcessingClientException;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResultResponse;
import com.hufs.capstone.backend.link.application.dto.LinkStatusResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LinkQueryServiceTest {

	@Mock
	private LinkRepository linkRepository;

	@Mock
	private ProcessingClient processingClient;

	@Mock
	private LinkStatusWriteService linkStatusWriteService;

	@InjectMocks
	private LinkQueryService linkQueryService;

	@Test
	void getLinkStatusShouldReturnImmediatelyWhenTerminal() {
		Link terminal = newLink(1L, LinkAnalysisStatus.SUCCEEDED, "caption");
		when(linkRepository.findById(1L)).thenReturn(Optional.of(terminal));

		LinkStatusResult result = linkQueryService.getLinkStatus(1L);

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(result.captionRaw()).isEqualTo("caption");
		verify(processingClient, never()).getJob("job-1");
		verify(linkStatusWriteService, never()).applySyncSnapshot(anyLong(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void getLinkStatusShouldResolveSucceededWithCaptionAndApplySnapshot() {
		Link processing = newLink(2L, LinkAnalysisStatus.PROCESSING, null);
		when(linkRepository.findById(2L)).thenReturn(Optional.of(processing));
		when(processingClient.getJob("job-1"))
				.thenReturn(new ProcessingJobResponse("job-1", "succeeded", null, null, null, null, null));
		when(processingClient.getJobResult("job-1"))
				.thenReturn(new ProcessingJobResultResponse("video", " caption ", null, null, null, null));

		LinkStatusResult synced = new LinkStatusResult(
				2L,
				"https://example.com/p/1",
				"job-1",
				LinkAnalysisStatus.SUCCEEDED,
				"caption",
				Instant.now(),
				Instant.now()
		);
		when(linkStatusWriteService.applySyncSnapshot(2L, LinkAnalysisStatus.SUCCEEDED, "caption")).thenReturn(synced);

		LinkStatusResult result = linkQueryService.getLinkStatus(2L);

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(result.captionRaw()).isEqualTo("caption");
		verify(linkStatusWriteService).applySyncSnapshot(2L, LinkAnalysisStatus.SUCCEEDED, "caption");
	}

	@Test
	void getLinkStatusShouldKeepProcessingWhenResultNotReady() {
		Link processing = newLink(3L, LinkAnalysisStatus.PROCESSING, null);
		when(linkRepository.findById(3L)).thenReturn(Optional.of(processing));
		when(processingClient.getJob("job-1"))
				.thenReturn(new ProcessingJobResponse("job-1", "succeeded", null, null, null, null, null));
		when(processingClient.getJobResult("job-1"))
				.thenThrow(new ProcessingClientException("not-ready", HttpStatus.NOT_FOUND, ""));

		LinkStatusResult synced = new LinkStatusResult(
				3L,
				"https://example.com/p/1",
				"job-1",
				LinkAnalysisStatus.PROCESSING,
				null,
				Instant.now(),
				Instant.now()
		);
		when(linkStatusWriteService.applySyncSnapshot(3L, LinkAnalysisStatus.PROCESSING, null)).thenReturn(synced);

		LinkStatusResult result = linkQueryService.getLinkStatus(3L);

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.PROCESSING);
		verify(linkStatusWriteService).applySyncSnapshot(3L, LinkAnalysisStatus.PROCESSING, null);
	}

	@Test
	void getLinkStatusShouldUseShortTtlCacheForBurstRequests() {
		Link processing = newLink(8L, LinkAnalysisStatus.PROCESSING, null);
		when(linkRepository.findById(8L)).thenReturn(Optional.of(processing));
		when(processingClient.getJob("job-1"))
				.thenReturn(new ProcessingJobResponse("job-1", "processing", null, null, null, null, null));

		LinkStatusResult synced = new LinkStatusResult(
				8L,
				"https://example.com/p/1",
				"job-1",
				LinkAnalysisStatus.PROCESSING,
				null,
				Instant.now(),
				Instant.now()
		);
		when(linkStatusWriteService.applySyncSnapshot(8L, LinkAnalysisStatus.PROCESSING, null)).thenReturn(synced);

		LinkStatusResult first = linkQueryService.getLinkStatus(8L);
		LinkStatusResult second = linkQueryService.getLinkStatus(8L);

		assertThat(first.status()).isEqualTo(LinkAnalysisStatus.PROCESSING);
		assertThat(second.status()).isEqualTo(LinkAnalysisStatus.PROCESSING);
		verify(processingClient, times(1)).getJob("job-1");
		verify(linkStatusWriteService, times(1)).applySyncSnapshot(8L, LinkAnalysisStatus.PROCESSING, null);
	}

	@Test
	void getLinkStatusShouldCoalesceInFlightRequests() throws Exception {
		Link processing = newLink(9L, LinkAnalysisStatus.PROCESSING, null);
		when(linkRepository.findById(9L)).thenReturn(Optional.of(processing));

		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		when(processingClient.getJob("job-1")).thenAnswer(invocation -> {
			started.countDown();
			release.await(2, TimeUnit.SECONDS);
			return new ProcessingJobResponse("job-1", "processing", null, null, null, null, null);
		});

		LinkStatusResult synced = new LinkStatusResult(
				9L,
				"https://example.com/p/1",
				"job-1",
				LinkAnalysisStatus.PROCESSING,
				null,
				Instant.now(),
				Instant.now()
		);
		when(linkStatusWriteService.applySyncSnapshot(9L, LinkAnalysisStatus.PROCESSING, null)).thenReturn(synced);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<LinkStatusResult> f1 = executor.submit(() -> linkQueryService.getLinkStatus(9L));
			assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
			Future<LinkStatusResult> f2 = executor.submit(() -> linkQueryService.getLinkStatus(9L));
			release.countDown();

			assertThat(f1.get(2, TimeUnit.SECONDS).status()).isEqualTo(LinkAnalysisStatus.PROCESSING);
			assertThat(f2.get(2, TimeUnit.SECONDS).status()).isEqualTo(LinkAnalysisStatus.PROCESSING);
		} finally {
			executor.shutdownNow();
		}

		verify(processingClient, times(1)).getJob("job-1");
		verify(linkStatusWriteService, times(1)).applySyncSnapshot(9L, LinkAnalysisStatus.PROCESSING, null);
	}

	private static Link newLink(Long id, LinkAnalysisStatus status, String caption) {
		Link link = Link.register("https://example.com/p/1", "https://example.com/p/1", "job-1");
		ReflectionTestUtils.setField(link, "id", id);
		if (status == LinkAnalysisStatus.PROCESSING) {
			link.markProcessing();
		}
		if (status == LinkAnalysisStatus.FAILED) {
			link.markFailed();
		}
		if (status == LinkAnalysisStatus.SUCCEEDED) {
			link.markSucceeded(caption);
		}
		return link;
	}
}
