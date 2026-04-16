package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.external.processing.ProcessingClient;
import com.hufs.capstone.backend.external.processing.ProcessingClientException;
import com.hufs.capstone.backend.external.processing.dto.CreateProcessingJobResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResultResponse;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkCommand;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.repository.LinkProcessingHistoryRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
class LinkConcurrencyIntegrationTest {

	@Autowired
	private LinkCommandService linkCommandService;

	@Autowired
	private LinkQueryService linkQueryService;

	@Autowired
	private LinkRepository linkRepository;

	@Autowired
	private RoomLinkRepository roomLinkRepository;

	@Autowired
	private LinkProcessingHistoryRepository linkProcessingHistoryRepository;

	@MockitoBean
	private ProcessingClient processingClient;

	@BeforeEach
	void setUp() {
		roomLinkRepository.deleteAll();
		linkProcessingHistoryRepository.deleteAll();
		linkRepository.deleteAll();
		reset(processingClient);
	}

	@AfterEach
	void tearDown() {
		roomLinkRepository.deleteAll();
		linkProcessingHistoryRepository.deleteAll();
		linkRepository.deleteAll();
	}

	@Test
	void shouldSaveSingleUrlNormally() {
		when(processingClient.createJob("https://example.com/post/1", "room-a", null))
				.thenReturn(new CreateProcessingJobResponse("job-1"));

		RegisterLinkResult result = linkCommandService.register(
				new RegisterLinkCommand("https://example.com/post/1", "room-a", null)
		);

		assertThat(result.linkId()).isNotNull();
		assertThat(linkRepository.count()).isEqualTo(1);
		assertThat(roomLinkRepository.count()).isEqualTo(1);
		assertThat(linkProcessingHistoryRepository.count()).isEqualTo(1);
	}

	@Test
	void shouldNotCreateDuplicateGlobalContentForSameUrl() {
		when(processingClient.createJob("https://example.com/post/1", "room-a", null))
				.thenReturn(new CreateProcessingJobResponse("job-1"));

		RegisterLinkResult first = linkCommandService.register(
				new RegisterLinkCommand("https://example.com/post/1?utm_source=x", "room-a", null)
		);
		RegisterLinkResult second = linkCommandService.register(
				new RegisterLinkCommand("https://example.com/post/1/", "room-b", null)
		);

		assertThat(first.linkId()).isEqualTo(second.linkId());
		assertThat(linkRepository.count()).isEqualTo(1);
		assertThat(roomLinkRepository.countByLinkId(first.linkId())).isEqualTo(2);
		assertThat(linkProcessingHistoryRepository.countByLinkId(first.linkId())).isEqualTo(2);
		verify(processingClient, times(1)).createJob("https://example.com/post/1", "room-a", null);
	}

	@Test
	void shouldPreventDuplicateRoomMappingForSameUrl() {
		when(processingClient.createJob("https://example.com/post/1", "room-a", null))
				.thenReturn(new CreateProcessingJobResponse("job-1"));

		RegisterLinkResult first = linkCommandService.register(
				new RegisterLinkCommand("https://example.com/post/1", "room-a", null)
		);

		assertThatThrownBy(() -> linkCommandService.register(
				new RegisterLinkCommand("https://example.com/post/1?x=1", "room-a", null)
		))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E409_CONFLICT));

		assertThat(linkRepository.count()).isEqualTo(1);
		assertThat(roomLinkRepository.countByRoomIdAndLinkId("room-a", first.linkId())).isEqualTo(1);
	}

	@Test
	void shouldKeepSingleContentAndSingleRoomMappingUnderConcurrentDuplicateSave() throws Exception {
		AtomicInteger jobSeq = new AtomicInteger();
		when(processingClient.createJob(eq("https://example.com/post/1"), eq("room-a"), eq(null)))
				.thenAnswer(invocation -> new CreateProcessingJobResponse("job-" + jobSeq.incrementAndGet()));

		List<Object> results = runConcurrently(
				() -> {
					try {
						return linkCommandService.register(new RegisterLinkCommand("https://example.com/post/1", "room-a", null));
					} catch (BusinessException ex) {
						return ex;
					}
				},
				2
		);

		long successCount = results.stream().filter(RegisterLinkResult.class::isInstance).count();
		long conflictCount = results.stream()
				.filter(BusinessException.class::isInstance)
				.map(BusinessException.class::cast)
				.filter(ex -> ex.getErrorCode() == ErrorCode.E409_CONFLICT)
				.count();

		assertThat(successCount).isEqualTo(1);
		assertThat(conflictCount).isEqualTo(1);
		assertThat(linkRepository.count()).isEqualTo(1);
		Link saved = linkRepository.findAll().get(0);
		assertThat(roomLinkRepository.countByRoomIdAndLinkId("room-a", saved.getId())).isEqualTo(1);
	}

	@Test
	void shouldReuseSingleContentAcrossDifferentRooms() {
		when(processingClient.createJob("https://example.com/post/1", "room-a", null))
				.thenReturn(new CreateProcessingJobResponse("job-1"));

		RegisterLinkResult first = linkCommandService.register(
				new RegisterLinkCommand("https://example.com/post/1", "room-a", null)
		);
		RegisterLinkResult second = linkCommandService.register(
				new RegisterLinkCommand("https://example.com/post/1?ref=abc", "room-b", null)
		);

		assertThat(first.linkId()).isEqualTo(second.linkId());
		assertThat(linkRepository.count()).isEqualTo(1);
		assertThat(roomLinkRepository.countByLinkId(first.linkId())).isEqualTo(2);
	}

	@Test
	void shouldKeepSucceededAndCaptionWhenTwoConcurrentPollsDetectCompletion() throws Exception {
		Link link = saveProcessingLink("https://example.com/post/2", "job-2");
		when(processingClient.getJob("job-2"))
				.thenReturn(new ProcessingJobResponse("job-2", "succeeded", null, "room-a", null, null, null));
		when(processingClient.getJobResult("job-2"))
				.thenReturn(new ProcessingJobResultResponse("video", "caption ready", null, null, null, null));

		runConcurrently(() -> linkQueryService.getLinkStatus(link.getId()), 2);

		Link reloaded = linkRepository.findById(link.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(reloaded.getCaptionRaw()).isEqualTo("caption ready");
	}

	@Test
	void shouldNotDowngradeToProcessingWhenConcurrentReadyAndNotReadyResponsesRace() throws Exception {
		Link link = saveProcessingLink("https://example.com/post/3", "job-3");
		when(processingClient.getJob("job-3"))
				.thenReturn(new ProcessingJobResponse("job-3", "succeeded", null, "room-a", null, null, null));
		AtomicInteger resultCalls = new AtomicInteger();
		when(processingClient.getJobResult("job-3")).thenAnswer(invocation -> {
			if (resultCalls.incrementAndGet() == 1) {
				return new ProcessingJobResultResponse("video", "caption final", null, null, null, null);
			}
			throw new ProcessingClientException("not-ready", HttpStatus.NOT_FOUND, "");
		});

		runConcurrently(() -> linkQueryService.getLinkStatus(link.getId()), 2);

		Link reloaded = linkRepository.findById(link.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(reloaded.getCaptionRaw()).isEqualTo("caption final");
	}

	@Test
	void shouldKeepTerminalConsistencyUnderFailedSucceededRace() throws Exception {
		Link link = saveProcessingLink("https://example.com/post/4", "job-4");
		AtomicInteger jobCalls = new AtomicInteger();
		when(processingClient.getJob("job-4")).thenAnswer(invocation -> {
			if (jobCalls.incrementAndGet() == 1) {
				return new ProcessingJobResponse("job-4", "failed", null, "room-a", null, "E001", "failed");
			}
			return new ProcessingJobResponse("job-4", "succeeded", null, "room-a", null, null, null);
		});
		when(processingClient.getJobResult("job-4"))
				.thenReturn(new ProcessingJobResultResponse("video", "caption from race", null, null, null, null));

		runConcurrently(() -> linkQueryService.getLinkStatus(link.getId()), 2);

		Link reloaded = linkRepository.findById(link.getId()).orElseThrow();
		assertThat(reloaded.getStatus().isTerminal()).isTrue();
		if (reloaded.getStatus() == LinkAnalysisStatus.SUCCEEDED) {
			assertThat(reloaded.getCaptionRaw()).isEqualTo("caption from race");
		}
	}

	@Test
	void shouldNotCallProcessingAfterTerminalStatus() {
		Link link = saveProcessingLink("https://example.com/post/5", "job-5");
		link.markSucceeded("done caption");
		linkRepository.saveAndFlush(link);

		linkQueryService.getLinkStatus(link.getId());
		linkQueryService.getLinkStatus(link.getId());
		linkQueryService.getLinkStatus(link.getId());

		verify(processingClient, never()).getJob("job-5");
		verify(processingClient, never()).getJobResult("job-5");
	}

	private Link saveProcessingLink(String normalizedUrl, String jobId) {
		Link link = Link.register(normalizedUrl, normalizedUrl, jobId);
		link.markProcessing();
		return linkRepository.saveAndFlush(link);
	}

	private static <T> List<T> runConcurrently(Callable<T> task, int threadCount) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		try {
			CountDownLatch ready = new CountDownLatch(threadCount);
			CountDownLatch start = new CountDownLatch(1);
			List<Future<T>> futures = new ArrayList<>();
			for (int i = 0; i < threadCount; i++) {
				futures.add(executor.submit(() -> {
					ready.countDown();
					if (!start.await(3, TimeUnit.SECONDS)) {
						throw new IllegalStateException("start latch timeout");
					}
					return task.call();
				}));
			}
			if (!ready.await(3, TimeUnit.SECONDS)) {
				throw new IllegalStateException("ready latch timeout");
			}
			start.countDown();

			List<T> results = new ArrayList<>();
			for (Future<T> future : futures) {
				results.add(future.get(5, TimeUnit.SECONDS));
			}
			return results;
		} finally {
			executor.shutdownNow();
		}
	}
}
