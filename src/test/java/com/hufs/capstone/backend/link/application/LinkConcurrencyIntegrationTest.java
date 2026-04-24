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
import com.hufs.capstone.backend.link.application.dto.AnalyzeLinkCommand;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisRequestResult;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.ProcessingDispatchStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkAnalysisRequest;
import com.hufs.capstone.backend.link.domain.repository.LinkAnalysisRequestRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkProcessingHistoryRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import com.hufs.capstone.backend.room.domain.repository.RoomMemberRepository;
import com.hufs.capstone.backend.room.domain.repository.RoomRepository;
import java.time.Duration;
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
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
class LinkConcurrencyIntegrationTest {

	private static final Long MEMBER_USER_ID = 100L;
	private static final Long OTHER_USER_ID = 200L;
	private static final String ROOM_A_PUBLIC_ID = "11111111-1111-1111-1111-111111111111";
	private static final String ROOM_B_PUBLIC_ID = "22222222-2222-2222-2222-222222222222";
	private static final String INVITE_A = "INVITEAAA1111";
	private static final String INVITE_B = "INVITEBBB2222";

	@Autowired
	private LinkAnalysisRequestService linkAnalysisRequestService;

	@Autowired
	private LinkAnalysisStatusService linkAnalysisStatusService;

	@Autowired
	private LinkProcessingDispatchPolicy linkProcessingDispatchPolicy;

	@Autowired
	private LinkRepository linkRepository;

	@Autowired
	private LinkAnalysisRequestRepository linkAnalysisRequestRepository;

	@Autowired
	private RoomLinkRepository roomLinkRepository;

	@Autowired
	private LinkProcessingHistoryRepository linkProcessingHistoryRepository;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private RoomMemberRepository roomMemberRepository;

	@MockitoBean
	private ProcessingClient processingClient;

	private Room roomA;
	private Room roomB;

	@BeforeEach
	void setUp() {
		roomLinkRepository.deleteAll();
		linkAnalysisRequestRepository.deleteAll();
		linkProcessingHistoryRepository.deleteAll();
		roomMemberRepository.deleteAll();
		roomRepository.deleteAll();
		linkRepository.deleteAll();
		reset(processingClient);
		linkProcessingDispatchPolicy.setRetryBackoff(Duration.ZERO);

		roomA = createRoomWithMember(ROOM_A_PUBLIC_ID, "A Room", INVITE_A, MEMBER_USER_ID);
		roomB = createRoomWithMember(ROOM_B_PUBLIC_ID, "B Room", INVITE_B, MEMBER_USER_ID);
	}

	@AfterEach
	void tearDown() {
		roomLinkRepository.deleteAll();
		linkAnalysisRequestRepository.deleteAll();
		linkProcessingHistoryRepository.deleteAll();
		roomMemberRepository.deleteAll();
		roomRepository.deleteAll();
		linkRepository.deleteAll();
	}

	@Test
	void shouldCreateAnalysisRequestWithoutRoomLinkForNewUrl() {
		when(processingClient.createJob("https://example.com/post/1", ROOM_A_PUBLIC_ID, null))
				.thenReturn(new CreateProcessingJobResponse("job-1"));

		LinkAnalysisRequestResult result = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/1", null)
		);

		assertThat(result.linkId()).isNotNull();
		assertThat(result.createdRequest()).isTrue();
		assertThat(linkRepository.count()).isEqualTo(1);
		assertThat(linkAnalysisRequestRepository.count()).isEqualTo(1);
		assertThat(roomLinkRepository.count()).isZero();
		verify(processingClient, times(1)).createJob("https://example.com/post/1", ROOM_A_PUBLIC_ID, null);
	}

	@Test
	void shouldReuseExistingAnalysisRequestInSameRoom() {
		when(processingClient.createJob("https://example.com/post/1", ROOM_A_PUBLIC_ID, null))
				.thenReturn(new CreateProcessingJobResponse("job-1"));

		LinkAnalysisRequestResult first = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/1", null)
		);
		LinkAnalysisRequestResult second = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/1?utm_source=x", null)
		);

		assertThat(first.linkId()).isEqualTo(second.linkId());
		assertThat(first.createdRequest()).isTrue();
		assertThat(second.createdRequest()).isFalse();
		assertThat(linkRepository.count()).isEqualTo(1);
		assertThat(linkAnalysisRequestRepository.countByLinkId(first.linkId())).isEqualTo(1);
		assertThat(roomLinkRepository.count()).isZero();
		verify(processingClient, times(1)).createJob("https://example.com/post/1", ROOM_A_PUBLIC_ID, null);
	}

	@Test
	void shouldReuseSingleLinkAcrossDifferentRoomsWithoutNewJob() {
		when(processingClient.createJob("https://example.com/post/1", ROOM_A_PUBLIC_ID, null))
				.thenReturn(new CreateProcessingJobResponse("job-1"));

		LinkAnalysisRequestResult first = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/1", null)
		);
		LinkAnalysisRequestResult second = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_B_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/1?ref=abc", null)
		);

		assertThat(first.linkId()).isEqualTo(second.linkId());
		assertThat(linkRepository.count()).isEqualTo(1);
		assertThat(linkAnalysisRequestRepository.countByLinkId(first.linkId())).isEqualTo(2);
		assertThat(roomLinkRepository.count()).isZero();
		verify(processingClient, times(1)).createJob("https://example.com/post/1", ROOM_A_PUBLIC_ID, null);
	}

	@Test
	void shouldNotRecoverExistingRequestedLinkWithoutJobId() {
		Link existing = linkRepository.saveAndFlush(Link.registerPending("https://example.com/post/2", "https://example.com/post/2"));

		LinkAnalysisRequestResult result = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/2", null)
		);

		assertThat(result.linkId()).isEqualTo(existing.getId());
		assertThat(result.processingJobId()).isNull();
		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.REQUESTED);
		assertThat(linkAnalysisRequestRepository.count()).isEqualTo(1);
		verify(processingClient, never()).createJob("https://example.com/post/2", ROOM_A_PUBLIC_ID, null);
	}

	@Test
	void shouldRejectAnalysisRequestWhenUserIsNotRoomMember() {
		assertThatThrownBy(() -> linkAnalysisRequestService.requestLinkAnalysis(
				OTHER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/1", null)
		))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E403_FORBIDDEN));
	}

	@Test
	void shouldKeepSingleLinkAndSingleAnalysisRequestUnderConcurrentDuplicateAnalyze() throws Exception {
		AtomicInteger jobSeq = new AtomicInteger();
		when(processingClient.createJob(eq("https://example.com/post/1"), eq(ROOM_A_PUBLIC_ID), eq(null)))
				.thenAnswer(invocation -> new CreateProcessingJobResponse("job-" + jobSeq.incrementAndGet()));

		List<LinkAnalysisRequestResult> results = runConcurrently(
				() -> linkAnalysisRequestService.requestLinkAnalysis(
						MEMBER_USER_ID,
						ROOM_A_PUBLIC_ID,
						new AnalyzeLinkCommand("https://example.com/post/1", null)
				),
				2
		);

		assertThat(results).hasSize(2);
		assertThat(results).extracting(LinkAnalysisRequestResult::linkId).containsOnly(results.get(0).linkId());
		assertThat(results).extracting(LinkAnalysisRequestResult::createdRequest).contains(true, false);
		assertThat(linkRepository.count()).isEqualTo(1);
		assertThat(linkAnalysisRequestRepository.count()).isEqualTo(1);
		assertThat(roomLinkRepository.count()).isZero();
		verify(processingClient, times(1)).createJob("https://example.com/post/1", ROOM_A_PUBLIC_ID, null);
	}

	@Test
	void shouldReturnSucceededCaptionForRequestedRoom() {
		Link link = saveProcessingLink("https://example.com/post/2", "job-2", roomA);
		when(processingClient.getJob("job-2"))
				.thenReturn(new ProcessingJobResponse("job-2", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		when(processingClient.getJobResult("job-2"))
				.thenReturn(new ProcessingJobResultResponse("video", "caption ready", null, null, null, null));

		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, link.getId());

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(result.captionRaw()).isEqualTo("caption ready");
	}

	@Test
	void shouldReturnRequestedWithoutCaptionWhenJobIsNotReady() {
		Link link = linkRepository.saveAndFlush(Link.registerPending("https://example.com/post/3", "https://example.com/post/3"));
		linkAnalysisRequestRepository.saveAndFlush(LinkAnalysisRequest.create(link, roomA, MEMBER_USER_ID, null));

		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, link.getId());

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.REQUESTED);
		assertThat(result.captionRaw()).isNull();
		verify(processingClient, never()).getJob(null);
	}

	@Test
	void shouldReturnProcessingWithoutCaption() {
		Link link = saveProcessingLink("https://example.com/post/4", "job-4", roomA);
		when(processingClient.getJob("job-4"))
				.thenReturn(new ProcessingJobResponse("job-4", "running", null, ROOM_A_PUBLIC_ID, null, null, null));

		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, link.getId());

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.PROCESSING);
		assertThat(result.captionRaw()).isNull();
	}

	@Test
	void shouldReturnFailedWithoutAutoRetry() {
		Link link = saveProcessingLink("https://example.com/post/5", "job-5", roomA);
		link.markFailed();
		ReflectionTestUtils.setField(link, "errorCode", "E001");
		ReflectionTestUtils.setField(link, "errorMessage", "failed");
		linkRepository.saveAndFlush(link);

		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, link.getId());

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.FAILED);
		assertThat(result.errorCode()).isEqualTo("E001");
		assertThat(result.errorMessage()).isEqualTo("failed");
		verify(processingClient, never()).getJob("job-5");
	}

	@Test
	void shouldReturnDispatchFailedWhenDispatchRetriesExhausted() {
		when(processingClient.createJob("https://example.com/post/10", ROOM_A_PUBLIC_ID, null))
				.thenThrow(new RuntimeException("processing server down"));

		LinkAnalysisRequestResult request = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/10", null)
		);
		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				request.linkId()
		);
		Link reloaded = linkRepository.findById(request.linkId()).orElseThrow();

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.DISPATCH_FAILED);
		assertThat(result.errorCode()).isEqualTo("PROCESSING_DISPATCH_FAILED");
		assertThat(reloaded.getDispatchStatus()).isEqualTo(ProcessingDispatchStatus.DISPATCH_FAILED);
		verify(processingClient, times(3)).createJob("https://example.com/post/10", ROOM_A_PUBLIC_ID, null);
	}

	@Test
	void shouldManuallyRetryDispatchFailedWhenSameUrlIsRequestedAgain() {
		when(processingClient.createJob("https://example.com/post/11", ROOM_A_PUBLIC_ID, null))
				.thenThrow(new RuntimeException("processing server down"))
				.thenThrow(new RuntimeException("processing server down"))
				.thenThrow(new RuntimeException("processing server down"))
				.thenReturn(new CreateProcessingJobResponse("job-retry"));

		LinkAnalysisRequestResult failed = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/11", null)
		);
		Link afterFailure = linkRepository.findById(failed.linkId()).orElseThrow();
		assertThat(afterFailure.getStatus()).isEqualTo(LinkAnalysisStatus.DISPATCH_FAILED);
		assertThat(afterFailure.getDispatchStatus()).isEqualTo(ProcessingDispatchStatus.DISPATCH_FAILED);

		LinkAnalysisRequestResult retried = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/11?utm_source=retry", null)
		);
		Link afterRetry = linkRepository.findById(retried.linkId()).orElseThrow();

		assertThat(retried.linkId()).isEqualTo(failed.linkId());
		assertThat(retried.createdRequest()).isFalse();
		assertThat(retried.processingJobId()).isEqualTo("job-retry");
		assertThat(retried.status()).isEqualTo(LinkAnalysisStatus.REQUESTED);
		assertThat(afterRetry.getDispatchStatus()).isEqualTo(ProcessingDispatchStatus.DISPATCHED);
		assertThat(afterRetry.getErrorCode()).isNull();
		verify(processingClient, times(4)).createJob("https://example.com/post/11", ROOM_A_PUBLIC_ID, null);
	}

	@Test
	void shouldRejectAnalysisQueryWhenUserHasNoRoomMembership() {
		Link link = saveProcessingLink("https://example.com/post/6", "job-6", roomA);

		assertThatThrownBy(() -> linkAnalysisStatusService.getLinkAnalysisResult(OTHER_USER_ID, ROOM_A_PUBLIC_ID, link.getId()))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E403_FORBIDDEN));
	}

	@Test
	void shouldRejectAnalysisQueryWhenRequestDoesNotExistInRoom() {
		Link link = saveProcessingLink("https://example.com/post/7", "job-7", roomA);

		assertThatThrownBy(() -> linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_B_PUBLIC_ID, link.getId()))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E403_FORBIDDEN));
	}

	@Test
	void shouldKeepSucceededAndCaptionWhenTwoConcurrentPollsDetectCompletion() throws Exception {
		Link link = saveProcessingLink("https://example.com/post/8", "job-8", roomA);
		when(processingClient.getJob("job-8"))
				.thenReturn(new ProcessingJobResponse("job-8", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		when(processingClient.getJobResult("job-8"))
				.thenReturn(new ProcessingJobResultResponse("video", "caption ready", null, null, null, null));

		runConcurrently(() -> linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, link.getId()), 2);

		Link reloaded = linkRepository.findById(link.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(reloaded.getCaptionRaw()).isEqualTo("caption ready");
	}

	@Test
	void shouldNotDowngradeToProcessingWhenConcurrentReadyAndNotReadyResponsesRace() throws Exception {
		Link link = saveProcessingLink("https://example.com/post/9", "job-9", roomA);
		when(processingClient.getJob("job-9"))
				.thenReturn(new ProcessingJobResponse("job-9", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		AtomicInteger resultCalls = new AtomicInteger();
		when(processingClient.getJobResult("job-9")).thenAnswer(invocation -> {
			if (resultCalls.incrementAndGet() == 1) {
				return new ProcessingJobResultResponse("video", "caption final", null, null, null, null);
			}
			throw new ProcessingClientException("not-ready", HttpStatus.NOT_FOUND, "");
		});

		runConcurrently(() -> linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, link.getId()), 2);

		Link reloaded = linkRepository.findById(link.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(reloaded.getCaptionRaw()).isEqualTo("caption final");
	}

	private Room createRoomWithMember(String publicId, String name, String inviteCode, Long userId) {
		Room room = roomRepository.saveAndFlush(Room.create(publicId, name, inviteCode, userId));
		roomMemberRepository.saveAndFlush(RoomMember.join(room, userId));
		return room;
	}

	private Link saveProcessingLink(String normalizedUrl, String jobId, Room room) {
		Link link = Link.register(normalizedUrl, normalizedUrl, jobId);
		link.markProcessing();
		Link savedLink = linkRepository.saveAndFlush(link);
		linkAnalysisRequestRepository.saveAndFlush(LinkAnalysisRequest.create(savedLink, room, MEMBER_USER_ID, null));
		return savedLink;
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
