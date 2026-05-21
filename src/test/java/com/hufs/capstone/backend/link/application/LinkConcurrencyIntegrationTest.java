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
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisRequestResult;
import com.hufs.capstone.backend.link.application.dto.LinkPlaceResult;
import com.hufs.capstone.backend.link.application.dto.RoomLinkCandidateOverrideResult;
import com.hufs.capstone.backend.link.application.dto.SaveRoomPlacesCommand;
import com.hufs.capstone.backend.link.application.event.LinkProcessingRequestedEvent;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.ProcessingDispatchStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkAnalysisRequest;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.link.domain.repository.LinkCandidateRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkAnalysisRequestRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkProcessingHistoryRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkCandidateOverrideRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceSaveResult;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceSourceType;
import com.hufs.capstone.backend.place.domain.repository.PlaceRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceSourceRepository;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import com.hufs.capstone.backend.room.domain.repository.RoomMemberRepository;
import com.hufs.capstone.backend.room.domain.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
	private LinkProcessingDispatchRecoveryService linkProcessingDispatchRecoveryService;

	@Autowired
	private LinkProcessingDispatchService linkProcessingDispatchService;

	@Autowired
	private RoomPlaceCommandService roomPlaceCommandService;

	@Autowired
	private RoomLinkCandidateOverrideService roomLinkCandidateOverrideService;

	@Autowired
	private LinkRepository linkRepository;

	@Autowired
	private LinkAnalysisRequestRepository linkAnalysisRequestRepository;

	@Autowired
	private LinkCandidateRepository linkCandidateRepository;

	@Autowired
	private RoomLinkCandidateOverrideRepository overrideRepository;

	@Autowired
	private RoomLinkRepository roomLinkRepository;

	@Autowired
	private RoomPlaceRepository roomPlaceRepository;

	@Autowired
	private RoomPlaceSourceRepository roomPlaceSourceRepository;

	@Autowired
	private PlaceRepository placeRepository;

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
		overrideRepository.deleteAll();
		roomPlaceSourceRepository.deleteAll();
		roomPlaceRepository.deleteAll();
		placeRepository.deleteAll();
		roomLinkRepository.deleteAll();
		linkCandidateRepository.deleteAll();
		linkAnalysisRequestRepository.deleteAll();
		linkProcessingHistoryRepository.deleteAll();
		roomMemberRepository.deleteAll();
		roomRepository.deleteAll();
		linkRepository.deleteAll();
		reset(processingClient);
		linkProcessingDispatchPolicy.setRetryBackoff(Duration.ZERO);
		linkProcessingDispatchPolicy.setRecoveryEnabled(false);
		linkProcessingDispatchPolicy.setStaleThreshold(Duration.ofMinutes(1));
		linkProcessingDispatchPolicy.setRecoveryBatchSize(50);

		roomA = createRoomWithMember(ROOM_A_PUBLIC_ID, "A Room", INVITE_A, MEMBER_USER_ID);
		roomB = createRoomWithMember(ROOM_B_PUBLIC_ID, "B Room", INVITE_B, MEMBER_USER_ID);
	}

	@AfterEach
	void tearDown() {
		overrideRepository.deleteAll();
		roomPlaceSourceRepository.deleteAll();
		roomPlaceRepository.deleteAll();
		placeRepository.deleteAll();
		roomLinkRepository.deleteAll();
		linkCandidateRepository.deleteAll();
		linkAnalysisRequestRepository.deleteAll();
		linkProcessingHistoryRepository.deleteAll();
		roomMemberRepository.deleteAll();
		roomRepository.deleteAll();
		linkRepository.deleteAll();
	}

	@Test
	void shouldCreateAnalysisRequestWithoutRoomLinkForNewUrl() throws Exception {
		when(processingClient.createJob("https://example.com/post/1", ROOM_A_PUBLIC_ID, null))
				.thenReturn(new CreateProcessingJobResponse("job-1"));

		LinkAnalysisRequestResult result = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/1", null)
		);

		assertThat(result.linkId()).isNotNull();
		assertThat(result.analysisRequestId()).isNotNull();
		assertThat(result.createdRequest()).isTrue();
		assertThat(linkRepository.count()).isEqualTo(1);
		assertThat(linkAnalysisRequestRepository.count()).isEqualTo(1);
		assertThat(roomLinkRepository.count()).isZero();
		assertThat(roomPlaceRepository.count()).isZero();
		awaitValue(
				() -> linkRepository.findById(result.linkId()).orElseThrow(),
				link -> link.getDispatchStatus() == ProcessingDispatchStatus.DISPATCHED
		);
		verify(processingClient, times(1)).createJob("https://example.com/post/1", ROOM_A_PUBLIC_ID, null);
	}

	@Test
	void shouldReuseExistingAnalysisRequestInSameRoom() throws Exception {
		when(processingClient.createJob("https://example.com/post/1", ROOM_A_PUBLIC_ID, null))
				.thenReturn(new CreateProcessingJobResponse("job-1"));

		LinkAnalysisRequestResult first = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/1", null)
		);
		awaitValue(
				() -> linkRepository.findById(first.linkId()).orElseThrow(),
				link -> link.getDispatchStatus() == ProcessingDispatchStatus.DISPATCHED
		);
		LinkAnalysisRequestResult second = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/1", null)
		);

		assertThat(first.linkId()).isEqualTo(second.linkId());
		assertThat(first.analysisRequestId()).isEqualTo(second.analysisRequestId());
		assertThat(first.createdRequest()).isTrue();
		assertThat(second.createdRequest()).isFalse();
		assertThat(linkRepository.count()).isEqualTo(1);
		assertThat(linkAnalysisRequestRepository.countByLinkId(first.linkId())).isEqualTo(1);
		assertThat(roomLinkRepository.count()).isZero();
		verify(processingClient, times(1)).createJob("https://example.com/post/1", ROOM_A_PUBLIC_ID, null);
	}

	@Test
	void shouldReuseSingleLinkAcrossDifferentRoomsWithoutNewJob() throws Exception {
		when(processingClient.createJob("https://example.com/post/1", ROOM_A_PUBLIC_ID, null))
				.thenReturn(new CreateProcessingJobResponse("job-1"));

		LinkAnalysisRequestResult first = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/1", null)
		);
		awaitValue(
				() -> linkRepository.findById(first.linkId()).orElseThrow(),
				link -> link.getDispatchStatus() == ProcessingDispatchStatus.DISPATCHED
		);
		LinkAnalysisRequestResult second = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_B_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/1", null)
		);

		assertThat(first.linkId()).isEqualTo(second.linkId());
		assertThat(first.analysisRequestId()).isNotEqualTo(second.analysisRequestId());
		assertThat(linkRepository.count()).isEqualTo(1);
		assertThat(linkAnalysisRequestRepository.countByLinkId(first.linkId())).isEqualTo(2);
		assertThat(roomLinkRepository.count()).isZero();
		verify(processingClient, times(1)).createJob("https://example.com/post/1", ROOM_A_PUBLIC_ID, null);
	}

	@Test
	void shouldAllowProcessingServerDedupeJobIdForDifferentSubmittedUrls() throws Exception {
		when(processingClient.createJob("https://example.com/post/1", ROOM_A_PUBLIC_ID, null))
				.thenReturn(new CreateProcessingJobResponse("job-deduped"));
		when(processingClient.createJob("https://example.com/post/1?utm_source=x", ROOM_A_PUBLIC_ID, null))
				.thenReturn(new CreateProcessingJobResponse("job-deduped"));

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

		Link firstLink = awaitValue(
				() -> linkRepository.findById(first.linkId()).orElseThrow(),
				link -> link.getDispatchStatus() == ProcessingDispatchStatus.DISPATCHED
		);
		Link secondLink = awaitValue(
				() -> linkRepository.findById(second.linkId()).orElseThrow(),
				link -> link.getDispatchStatus() == ProcessingDispatchStatus.DISPATCHED
		);

		assertThat(first.linkId()).isNotEqualTo(second.linkId());
		assertThat(firstLink.getProcessingJobId()).isEqualTo("job-deduped");
		assertThat(secondLink.getProcessingJobId()).isEqualTo("job-deduped");
		assertThat(linkRepository.count()).isEqualTo(2);
		verify(processingClient, times(1)).createJob("https://example.com/post/1", ROOM_A_PUBLIC_ID, null);
		verify(processingClient, times(1)).createJob("https://example.com/post/1?utm_source=x", ROOM_A_PUBLIC_ID, null);
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
		awaitValue(
				() -> linkRepository.findById(results.get(0).linkId()).orElseThrow(),
				link -> link.getDispatchStatus() == ProcessingDispatchStatus.DISPATCHED
		);
		verify(processingClient, times(1)).createJob("https://example.com/post/1", ROOM_A_PUBLIC_ID, null);
	}

	@Test
	void shouldReturnSucceededContentForRequestedRoom() {
		Link link = saveProcessingLink("https://example.com/post/2", "job-2", roomA);
		when(processingClient.getJob("job-2"))
				.thenReturn(new ProcessingJobResponse("job-2", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		when(processingClient.getJobResult("job-2"))
				.thenReturn(succeededResultWithContent("content ready"));

		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				analysisRequestIdFor(link, roomA)
		);

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(result.contentText()).isEqualTo("content ready");
		assertThat(result.linkStats().likeCount()).isEqualTo(15000L);
		assertThat(result.linkStats().commentCount()).isEqualTo(177L);
		assertThat(result.linkStats().postedAt()).isEqualTo("April 2, 2026");
	}

	@Test
	void shouldStoreCandidatePlacesAndRawJsonWhenProcessingSucceeded() {
		Link link = saveProcessingLink("https://example.com/post/place", "job-place", roomA);
		when(processingClient.getJob("job-place"))
				.thenReturn(new ProcessingJobResponse("job-place", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		when(processingClient.getJobResult("job-place"))
				.thenReturn(succeededResultWithPlace());

		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				analysisRequestIdFor(link, roomA)
		);
		Link reloaded = linkRepository.findById(link.getId()).orElseThrow();

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(result.candidatePlaces()).hasSize(1);
		assertThat(result.candidatePlaces().get(0).kakaoPlaceId()).isEqualTo("123456789");
		assertThat(result.candidatePlaces().get(0).selectable()).isTrue();
		assertThat(reloaded.getExtractionStoreName()).isEqualTo("Coffee Mansion");
		assertThat(reloaded.getExtractionAddress()).isEqualTo("Seoul Jongno-gu");
		assertThat(reloaded.getExtractedPlacesJson()).contains("Coffee Mansion");
		assertThat(reloaded.getProcessingResultJson()).contains("resolved_places");
	}

	@Test
	void shouldStoreSucceededResultWithoutCandidatePlacesWhenResolvedPlacesIsEmpty() {
		Link link = saveProcessingLink("https://example.com/post/no-place", "job-no-place", roomA);
		when(processingClient.getJob("job-no-place"))
				.thenReturn(new ProcessingJobResponse("job-no-place", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		when(processingClient.getJobResult("job-no-place"))
				.thenReturn(new ProcessingJobResultResponse(
						"job-no-place",
						"SUCCEEDED",
						"https://example.com/post/no-place",
						"https://example.com/post/no-place",
						"https://example.com/post/no-place",
						content("content without selected place"),
						linkStats(15000L, 177L, "April 2, 2026"),
						List.of(),
						null,
						null
				));

		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				analysisRequestIdFor(link, roomA)
		);
		Link reloaded = linkRepository.findById(link.getId()).orElseThrow();

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(result.contentText()).isEqualTo("content without selected place");
		assertThat(result.candidatePlaces()).isEmpty();
		assertThat(reloaded.getExtractedPlacesJson()).isEqualTo("[]");
		assertThat(reloaded.getProcessingResultJson()).contains("content without selected place");
	}

	@Test
	void shouldSaveMultipleCandidatePlacesFromSameLinkAndNoOpAlreadySaved() {
		Link link = saveProcessingLink("https://example.com/post/save-places", "job-save-places", roomA);
		when(processingClient.getJob("job-save-places"))
				.thenReturn(new ProcessingJobResponse("job-save-places", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		when(processingClient.getJobResult("job-save-places"))
				.thenReturn(succeededResultWithPlaces(
						place("123456789", "Coffee Mansion"),
						place("987654321", "Tea House")
				));

		Long analysisRequestId = analysisRequestIdFor(link, roomA);
		linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, analysisRequestId);

		RoomPlaceSaveResult saved = roomPlaceCommandService.saveRoomPlaces(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				analysisRequestId,
				new SaveRoomPlacesCommand(List.of("123456789", "987654321"))
		);
		RoomPlaceSaveResult repeated = roomPlaceCommandService.saveRoomPlaces(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				analysisRequestId,
				new SaveRoomPlacesCommand(List.of("123456789"))
		);

		assertThat(saved.places()).hasSize(2);
		assertThat(saved.places()).allMatch(RoomPlaceSaveResult.SavedRoomPlaceResult::created);
		assertThat(repeated.places()).hasSize(1);
		assertThat(repeated.places().get(0).created()).isFalse();
		assertThat(repeated.places().get(0).alreadyInRoom()).isTrue();
		assertThat(roomPlaceRepository.countByRoomId(roomA.getId())).isEqualTo(2);
		List<RoomPlace> savedRoomPlaces = roomPlaceRepository.findExistingByRoomIdAndKakaoPlaceIds(
				roomA.getId(),
				List.of("123456789", "987654321")
		);
		assertThat(savedRoomPlaces)
				.extracting(roomPlace -> roomPlace.getPlace().getServiceCategory().getCode())
				.containsOnly("CAFE");
		assertThat(savedRoomPlaces)
				.extracting(RoomPlace::getSourceType)
				.containsOnly(RoomPlaceSourceType.LINK_ANALYSIS);
		assertThat(savedRoomPlaces)
				.extracting(RoomPlace::getSourceRoomLinkId)
				.allMatch(sourceRoomLinkId -> sourceRoomLinkId != null);
		assertThat(roomLinkRepository.countByRoomIdAndLinkId(roomA.getId(), link.getId())).isEqualTo(1);
		assertThat(roomPlaceSourceRepository.countByRoomLinkId(
				roomLinkRepository.findByRoomAndLinkId(roomA, link.getId()).orElseThrow().getId()
		)).isEqualTo(2);
	}

	@Test
	void shouldMarkAlreadySavedCandidateInAnalysisResult() {
		Link link = saveProcessingLink("https://example.com/post/saved-status", "job-saved-status", roomA);
		when(processingClient.getJob("job-saved-status"))
				.thenReturn(new ProcessingJobResponse("job-saved-status", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		when(processingClient.getJobResult("job-saved-status"))
				.thenReturn(succeededResultWithPlaces(
						place("123456789", "Coffee Mansion"),
						place("987654321", "Tea House")
				));
		Long analysisRequestId = analysisRequestIdFor(link, roomA);
		linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, analysisRequestId);
		roomPlaceCommandService.saveRoomPlaces(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				analysisRequestId,
				new SaveRoomPlacesCommand(List.of("123456789"))
		);

		LinkAnalysisResult result =
				linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, analysisRequestId);

		LinkPlaceResult savedCandidate = result.candidatePlaces().stream()
				.filter(candidate -> "123456789".equals(candidate.kakaoPlaceId()))
				.findFirst()
				.orElseThrow();
		LinkPlaceResult unsavedCandidate = result.candidatePlaces().stream()
				.filter(candidate -> "987654321".equals(candidate.kakaoPlaceId()))
				.findFirst()
				.orElseThrow();
		assertThat(savedCandidate.alreadyInRoom()).isTrue();
		assertThat(savedCandidate.selectable()).isFalse();
		assertThat(savedCandidate.disabledReason()).isEqualTo(LinkPlaceResult.DisabledReason.ALREADY_IN_ROOM);
		assertThat(savedCandidate.roomPlaceId()).isNotNull();
		assertThat(unsavedCandidate.alreadyInRoom()).isFalse();
		assertThat(unsavedCandidate.selectable()).isTrue();
		assertThat(unsavedCandidate.disabledReason()).isNull();
	}

	@Test
	void shouldOverlayRoomScopedCandidateOverrideForRepeatedSameLinkAnalysis() throws Exception {
		when(processingClient.createJob("https://example.com/post/manual-corrected", ROOM_A_PUBLIC_ID, null))
				.thenReturn(new CreateProcessingJobResponse("job-manual-corrected"));
		LinkAnalysisRequestResult firstRequest = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/manual-corrected", null)
		);
		Link link = awaitValue(
				() -> linkRepository.findById(firstRequest.linkId()).orElseThrow(),
				item -> item.getDispatchStatus() == ProcessingDispatchStatus.DISPATCHED
		);
		when(processingClient.getJob("job-manual-corrected"))
				.thenReturn(new ProcessingJobResponse("job-manual-corrected", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		when(processingClient.getJobResult("job-manual-corrected"))
				.thenReturn(succeededResultWithPlaces(place("111111111", "A Twosome Place Myeongdong")));
		LinkAnalysisResult originalResult =
				linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, firstRequest.analysisRequestId());
		LinkPlaceResult originalCandidate = originalResult.candidatePlaces().get(0);
		RoomLinkCandidateOverrideResult override = roomLinkCandidateOverrideService.overrideCandidate(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				firstRequest.analysisRequestId(),
				originalCandidate.candidateId(),
				manualCafeSnapshot("222222222", "A Twosome Place HUFS")
		);

		LinkAnalysisRequestResult repeatedRequest = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/manual-corrected", null)
		);
		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				repeatedRequest.analysisRequestId()
		);

		assertThat(repeatedRequest.linkId()).isEqualTo(link.getId());
		assertThat(repeatedRequest.analysisRequestId()).isEqualTo(firstRequest.analysisRequestId());
		assertThat(repeatedRequest.createdRequest()).isFalse();
		LinkPlaceResult correctedCandidate = result.candidatePlaces().stream()
				.filter(candidate -> "222222222".equals(candidate.kakaoPlaceId()))
				.findFirst()
				.orElseThrow();
		assertThat(override.candidateId()).isEqualTo(originalCandidate.candidateId());
		assertThat(correctedCandidate.candidateId()).isEqualTo(originalCandidate.candidateId());
		assertThat(correctedCandidate.overrideId()).isEqualTo(override.overrideId());
		assertThat(correctedCandidate.placeName()).isEqualTo("A Twosome Place HUFS");
		assertThat(correctedCandidate.corrected()).isTrue();
		assertThat(correctedCandidate.alreadyInRoom()).isFalse();
		assertThat(correctedCandidate.selectable()).isTrue();
		assertThat(roomPlaceRepository.countByRoomId(roomA.getId())).isZero();

		roomPlaceCommandService.saveRoomPlaces(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				repeatedRequest.analysisRequestId(),
				new SaveRoomPlacesCommand(List.of("222222222"))
		);
		LinkAnalysisResult savedResult = linkAnalysisStatusService.getLinkAnalysisResult(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				repeatedRequest.analysisRequestId()
		);
		LinkPlaceResult savedCorrectedCandidate = savedResult.candidatePlaces().stream()
				.filter(candidate -> "222222222".equals(candidate.kakaoPlaceId()))
				.findFirst()
				.orElseThrow();
		assertThat(savedCorrectedCandidate.alreadyInRoom()).isTrue();
		assertThat(savedCorrectedCandidate.selectable()).isFalse();
		assertThat(roomPlaceRepository.countByRoomIdAndKakaoPlaceId(roomA.getId(), "222222222")).isEqualTo(1);

		LinkAnalysisRequestResult otherRoomRequest = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_B_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/manual-corrected", null)
		);
		LinkAnalysisResult otherRoomResult = linkAnalysisStatusService.getLinkAnalysisResult(
				MEMBER_USER_ID,
				ROOM_B_PUBLIC_ID,
				otherRoomRequest.analysisRequestId()
		);

		assertThat(otherRoomResult.candidatePlaces())
				.extracting(LinkPlaceResult::kakaoPlaceId)
				.contains("111111111")
				.doesNotContain("222222222");
	}

	@Test
	void shouldRejectSavingDuplicateRequestIdsAndInvalidCandidates() {
		Link link = saveProcessingLink("https://example.com/post/invalid-save", "job-invalid-save", roomA);
		when(processingClient.getJob("job-invalid-save"))
				.thenReturn(new ProcessingJobResponse("job-invalid-save", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		when(processingClient.getJobResult("job-invalid-save"))
				.thenReturn(succeededResultWithPlaces(place("123456789", "Coffee Mansion")));
		Long analysisRequestId = analysisRequestIdFor(link, roomA);
		linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, analysisRequestId);

		assertThatThrownBy(() -> roomPlaceCommandService.saveRoomPlaces(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				analysisRequestId,
				new SaveRoomPlacesCommand(List.of("123456789", "123456789"))
		))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E400_ILLEGAL_ARGUMENT));
		assertThatThrownBy(() -> roomPlaceCommandService.saveRoomPlaces(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				analysisRequestId,
				new SaveRoomPlacesCommand(List.of("missing"))
		))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E400_ILLEGAL_ARGUMENT));
		assertThat(roomPlaceRepository.countByRoomId(roomA.getId())).isZero();
	}

	@Test
	void shouldTreatSameKakaoPlaceFromDifferentLinkAsAlreadySavedInSameRoom() {
		Link firstLink = saveProcessingLink("https://example.com/post/first-place", "job-first-place", roomA);
		when(processingClient.getJob("job-first-place"))
				.thenReturn(new ProcessingJobResponse("job-first-place", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		when(processingClient.getJobResult("job-first-place"))
				.thenReturn(succeededResultWithPlaces(place("123456789", "Coffee Mansion")));
		Long firstAnalysisRequestId = analysisRequestIdFor(firstLink, roomA);
		linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, firstAnalysisRequestId);
		roomPlaceCommandService.saveRoomPlaces(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				firstAnalysisRequestId,
				new SaveRoomPlacesCommand(List.of("123456789"))
		);

		Link secondLink = saveProcessingLink("https://example.com/post/second-place", "job-second-place", roomA);
		when(processingClient.getJob("job-second-place"))
				.thenReturn(new ProcessingJobResponse("job-second-place", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		when(processingClient.getJobResult("job-second-place"))
				.thenReturn(succeededResultWithPlaces(place("123456789", "Coffee Mansion")));

		LinkAnalysisResult result =
				linkAnalysisStatusService.getLinkAnalysisResult(
						MEMBER_USER_ID,
						ROOM_A_PUBLIC_ID,
						analysisRequestIdFor(secondLink, roomA)
				);

		assertThat(result.candidatePlaces()).hasSize(1);
		assertThat(result.candidatePlaces().get(0).alreadyInRoom()).isTrue();
		assertThat(result.candidatePlaces().get(0).selectable()).isFalse();
		assertThat(roomPlaceRepository.countByRoomId(roomA.getId())).isEqualTo(1);
	}

	@Test
	void shouldAllowSameKakaoPlaceInDifferentRooms() {
		Link firstLink = saveProcessingLink("https://example.com/post/room-a-place", "job-room-a-place", roomA);
		when(processingClient.getJob("job-room-a-place"))
				.thenReturn(new ProcessingJobResponse("job-room-a-place", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		when(processingClient.getJobResult("job-room-a-place"))
				.thenReturn(succeededResultWithPlaces(place("123456789", "Coffee Mansion")));
		Long firstAnalysisRequestId = analysisRequestIdFor(firstLink, roomA);
		linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, firstAnalysisRequestId);
		roomPlaceCommandService.saveRoomPlaces(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				firstAnalysisRequestId,
				new SaveRoomPlacesCommand(List.of("123456789"))
		);
		Link secondLink = saveProcessingLink("https://example.com/post/room-b-place", "job-room-b-place", roomB);
		when(processingClient.getJob("job-room-b-place"))
				.thenReturn(new ProcessingJobResponse("job-room-b-place", "succeeded", null, ROOM_B_PUBLIC_ID, null, null, null));
		when(processingClient.getJobResult("job-room-b-place"))
				.thenReturn(succeededResultWithPlaces(place("123456789", "Coffee Mansion")));
		Long secondAnalysisRequestId = analysisRequestIdFor(secondLink, roomB);
		linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_B_PUBLIC_ID, secondAnalysisRequestId);

		roomPlaceCommandService.saveRoomPlaces(
				MEMBER_USER_ID,
				ROOM_B_PUBLIC_ID,
				secondAnalysisRequestId,
				new SaveRoomPlacesCommand(List.of("123456789"))
		);

		assertThat(roomPlaceRepository.countByRoomId(roomA.getId())).isEqualTo(1);
		assertThat(roomPlaceRepository.countByRoomId(roomB.getId())).isEqualTo(1);
	}

	@Test
	void shouldRejectSavingWhenLinkIsNotSharedInRoomOrUserIsNotMember() {
		Link link = saveProcessingLink("https://example.com/post/not-shared", "job-not-shared", roomA);

		assertThatThrownBy(() -> roomPlaceCommandService.saveRoomPlaces(
				MEMBER_USER_ID,
				ROOM_B_PUBLIC_ID,
				analysisRequestIdFor(link, roomA),
				new SaveRoomPlacesCommand(List.of("123456789"))
		))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E403_FORBIDDEN));
		assertThatThrownBy(() -> roomPlaceCommandService.saveRoomPlaces(
				OTHER_USER_ID,
				ROOM_A_PUBLIC_ID,
				analysisRequestIdFor(link, roomA),
				new SaveRoomPlacesCommand(List.of("123456789"))
		))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E403_FORBIDDEN));
	}

	@Test
	void shouldPreventDuplicateRoomPlaceUnderConcurrentSave() throws Exception {
		Link link = saveProcessingLink("https://example.com/post/concurrent-place", "job-concurrent-place", roomA);
		when(processingClient.getJob("job-concurrent-place"))
				.thenReturn(new ProcessingJobResponse("job-concurrent-place", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		when(processingClient.getJobResult("job-concurrent-place"))
				.thenReturn(succeededResultWithPlaces(place("123456789", "Coffee Mansion")));
		Long analysisRequestId = analysisRequestIdFor(link, roomA);
		linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, analysisRequestId);

		runConcurrently(() -> roomPlaceCommandService.saveRoomPlaces(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				analysisRequestId,
				new SaveRoomPlacesCommand(List.of("123456789"))
		), 2);

		assertThat(roomPlaceRepository.countByRoomIdAndKakaoPlaceId(roomA.getId(), "123456789")).isEqualTo(1);
	}

	@Test
	void shouldReturnMissingKakaoPlaceIdCandidateAsNotSelectable() {
		Link link = saveProcessingLink("https://example.com/post/missing-kakao", "job-missing-kakao", roomA);
		when(processingClient.getJob("job-missing-kakao"))
				.thenReturn(new ProcessingJobResponse("job-missing-kakao", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		when(processingClient.getJobResult("job-missing-kakao"))
				.thenReturn(succeededResultWithPlaces(place(null, "Unknown Cafe")));

		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				analysisRequestIdFor(link, roomA)
		);

		assertThat(result.candidatePlaces()).hasSize(1);
		assertThat(result.candidatePlaces().get(0).selectable()).isFalse();
		assertThat(result.candidatePlaces().get(0).disabledReason())
				.isEqualTo(LinkPlaceResult.DisabledReason.MISSING_KAKAO_PLACE_ID);
	}

	@Test
	void shouldReturnRequestedWithoutContentWhenJobIsNotReady() {
		Link link = linkRepository.saveAndFlush(Link.registerPending("https://example.com/post/3", "https://example.com/post/3"));
		linkAnalysisRequestRepository.saveAndFlush(LinkAnalysisRequest.create(link, roomA, MEMBER_USER_ID, null));
		roomLinkRepository.saveAndFlush(RoomLink.bind(roomA, link));

		Long analysisRequestId = analysisRequestIdFor(link, roomA);
		LinkAnalysisResult result =
				linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, analysisRequestId);

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.REQUESTED);
		assertThat(result.contentText()).isNull();
		verify(processingClient, never()).getJob(null);
	}

	@Test
	void shouldReturnProcessingWithoutContent() {
		Link link = saveProcessingLink("https://example.com/post/4", "job-4", roomA);
		when(processingClient.getJob("job-4"))
				.thenReturn(new ProcessingJobResponse("job-4", "running", null, ROOM_A_PUBLIC_ID, null, null, null));

		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				analysisRequestIdFor(link, roomA)
		);

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.PROCESSING);
		assertThat(result.contentText()).isNull();
	}

	@Test
	void shouldReturnFailedWithoutAutoRetry() {
		Link link = saveProcessingLink("https://example.com/post/5", "job-5", roomA);
		link.markFailed();
		ReflectionTestUtils.setField(link, "errorCode", "E001");
		ReflectionTestUtils.setField(link, "errorMessage", "failed");
		linkRepository.saveAndFlush(link);

		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				analysisRequestIdFor(link, roomA)
		);

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.FAILED);
		assertThat(result.errorCode()).isEqualTo("E001");
		assertThat(result.errorMessage()).isEqualTo("failed");
		verify(processingClient, never()).getJob("job-5");
	}

	@Test
	void shouldReturnDispatchFailedWhenDispatchRetriesExhausted() throws Exception {
		when(processingClient.createJob("https://example.com/post/10", ROOM_A_PUBLIC_ID, null))
				.thenThrow(new RuntimeException("processing server down"));

		LinkAnalysisRequestResult request = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/10", null)
		);
		awaitValue(
				() -> linkRepository.findById(request.linkId()).orElseThrow(),
				link -> link.getStatus() == LinkAnalysisStatus.DISPATCH_FAILED
		);
		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				request.analysisRequestId()
		);
		Link reloaded = linkRepository.findById(request.linkId()).orElseThrow();

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.DISPATCH_FAILED);
		assertThat(result.errorCode()).isEqualTo("PROCESSING_DISPATCH_FAILED");
		assertThat(reloaded.getDispatchStatus()).isEqualTo(ProcessingDispatchStatus.DISPATCH_FAILED);
		assertThat(linkProcessingHistoryRepository.countByLinkId(request.linkId())).isEqualTo(1);
		verify(processingClient, times(3)).createJob("https://example.com/post/10", ROOM_A_PUBLIC_ID, null);
	}

	@Test
	void shouldManuallyRetryDispatchFailedWhenSameUrlIsRequestedAgain() throws Exception {
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
		Link afterFailure = awaitValue(
				() -> linkRepository.findById(failed.linkId()).orElseThrow(),
				link -> link.getStatus() == LinkAnalysisStatus.DISPATCH_FAILED
		);
		assertThat(afterFailure.getStatus()).isEqualTo(LinkAnalysisStatus.DISPATCH_FAILED);
		assertThat(afterFailure.getDispatchStatus()).isEqualTo(ProcessingDispatchStatus.DISPATCH_FAILED);

		LinkAnalysisRequestResult retried = linkAnalysisRequestService.requestLinkAnalysis(
				MEMBER_USER_ID,
				ROOM_A_PUBLIC_ID,
				new AnalyzeLinkCommand("https://example.com/post/11", null)
		);
		Link afterRetry = awaitValue(
				() -> linkRepository.findById(retried.linkId()).orElseThrow(),
				link -> link.getDispatchStatus() == ProcessingDispatchStatus.DISPATCHED
		);

		assertThat(retried.linkId()).isEqualTo(failed.linkId());
		assertThat(retried.createdRequest()).isFalse();
		assertThat(retried.processingJobId()).isIn(null, "job-retry");
		assertThat(retried.status()).isEqualTo(LinkAnalysisStatus.REQUESTED);
		assertThat(afterRetry.getProcessingJobId()).isEqualTo("job-retry");
		assertThat(afterRetry.getDispatchStatus()).isEqualTo(ProcessingDispatchStatus.DISPATCHED);
		assertThat(afterRetry.getErrorCode()).isNull();
		verify(processingClient, times(4)).createJob("https://example.com/post/11", ROOM_A_PUBLIC_ID, null);
	}

	@Test
	void shouldRecoverStalePendingDispatchWithoutJobId() throws Exception {
		linkProcessingDispatchPolicy.setStaleThreshold(Duration.ZERO);
		when(processingClient.createJob("https://example.com/post/12", ROOM_A_PUBLIC_ID, null))
				.thenReturn(new CreateProcessingJobResponse("job-recovered"));
		Link link = linkRepository.saveAndFlush(
				Link.registerPending("https://example.com/post/12", "https://example.com/post/12")
		);
		linkAnalysisRequestRepository.saveAndFlush(LinkAnalysisRequest.create(link, roomA, MEMBER_USER_ID, null));

		List<LinkProcessingRequestedEvent> events =
				linkProcessingDispatchRecoveryService.findRecoverableEvents(Instant.now());
		assertThat(events).hasSize(1);

		linkProcessingDispatchService.dispatch(events.get(0));

		Link recovered = linkRepository.findById(link.getId()).orElseThrow();
		assertThat(recovered.getProcessingJobId()).isEqualTo("job-recovered");
		assertThat(recovered.getDispatchStatus()).isEqualTo(ProcessingDispatchStatus.DISPATCHED);
		verify(processingClient, times(1)).createJob("https://example.com/post/12", ROOM_A_PUBLIC_ID, null);
	}

	@Test
	void shouldCreateOnlyOneProcessingJobWhenDuplicateDispatchEventsRunConcurrently() throws Exception {
		AtomicInteger jobSeq = new AtomicInteger();
		when(processingClient.createJob("https://example.com/post/13", ROOM_A_PUBLIC_ID, null))
				.thenAnswer(invocation -> {
					Thread.sleep(100);
					return new CreateProcessingJobResponse("job-" + jobSeq.incrementAndGet());
				});
		Link link = linkRepository.saveAndFlush(
				Link.registerPending("https://example.com/post/13", "https://example.com/post/13")
		);
		linkAnalysisRequestRepository.saveAndFlush(LinkAnalysisRequest.create(link, roomA, MEMBER_USER_ID, null));
		LinkProcessingRequestedEvent event =
				new LinkProcessingRequestedEvent(
						link.getId(),
						link.getOriginalUrl(),
						link.getNormalizedUrl(),
						ROOM_A_PUBLIC_ID,
						null
				);

		runConcurrently(() -> {
			linkProcessingDispatchService.dispatch(event);
			return null;
		}, 2);

		Link dispatched = linkRepository.findById(link.getId()).orElseThrow();
		assertThat(dispatched.getDispatchStatus()).isEqualTo(ProcessingDispatchStatus.DISPATCHED);
		assertThat(dispatched.getProcessingJobId()).isEqualTo("job-1");
		verify(processingClient, times(1)).createJob("https://example.com/post/13", ROOM_A_PUBLIC_ID, null);
	}

	@Test
	void shouldRejectAnalysisQueryWhenUserHasNoRoomMembership() {
		Link link = saveProcessingLink("https://example.com/post/6", "job-6", roomA);

		assertThatThrownBy(() -> linkAnalysisStatusService.getLinkAnalysisResult(
				OTHER_USER_ID,
				ROOM_A_PUBLIC_ID,
				analysisRequestIdFor(link, roomA)
		))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E403_FORBIDDEN));
	}

	@Test
	void shouldRejectAnalysisQueryWhenRequestDoesNotExistInRoom() {
		Link link = saveProcessingLink("https://example.com/post/7", "job-7", roomA);

		assertThatThrownBy(() -> linkAnalysisStatusService.getLinkAnalysisResult(
				MEMBER_USER_ID,
				ROOM_B_PUBLIC_ID,
				analysisRequestIdFor(link, roomA)
		))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E403_FORBIDDEN));
	}

	@Test
	void shouldKeepSucceededAndContentWhenTwoConcurrentPollsDetectCompletion() throws Exception {
		Link link = saveProcessingLink("https://example.com/post/8", "job-8", roomA);
		when(processingClient.getJob("job-8"))
				.thenReturn(new ProcessingJobResponse("job-8", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		when(processingClient.getJobResult("job-8"))
				.thenReturn(succeededResultWithContent("content ready"));

		Long analysisRequestId = analysisRequestIdFor(link, roomA);
		runConcurrently(
				() -> linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, analysisRequestId),
				2
		);

		Link reloaded = linkRepository.findById(link.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(reloaded.getContentText()).isEqualTo("content ready");
	}

	@Test
	void shouldNotDowngradeToProcessingWhenConcurrentReadyAndNotReadyResponsesRace() throws Exception {
		Link link = saveProcessingLink("https://example.com/post/9", "job-9", roomA);
		when(processingClient.getJob("job-9"))
				.thenReturn(new ProcessingJobResponse("job-9", "succeeded", null, ROOM_A_PUBLIC_ID, null, null, null));
		AtomicInteger resultCalls = new AtomicInteger();
		when(processingClient.getJobResult("job-9")).thenAnswer(invocation -> {
			if (resultCalls.incrementAndGet() == 1) {
				return succeededResultWithContent("content final");
			}
			throw new ProcessingClientException("not-ready", HttpStatus.CONFLICT, "");
		});

		Long analysisRequestId = analysisRequestIdFor(link, roomA);
		runConcurrently(
				() -> linkAnalysisStatusService.getLinkAnalysisResult(MEMBER_USER_ID, ROOM_A_PUBLIC_ID, analysisRequestId),
				2
		);

		Link reloaded = linkRepository.findById(link.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(reloaded.getContentText()).isEqualTo("content final");
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
		roomLinkRepository.saveAndFlush(RoomLink.bind(room, savedLink));
		return savedLink;
	}

	private Long analysisRequestIdFor(Link link, Room room) {
		return linkAnalysisRequestRepository.findByRoomAndLinkId(room, link.getId())
				.orElseThrow()
				.getId();
	}

	private static ProcessingJobResultResponse succeededResultWithPlace() {
		return succeededResultWithPlaces(place("123456789", "Coffee Mansion"));
	}

	private static ProcessingJobResultResponse succeededResultWithContent(String contentText) {
		return new ProcessingJobResultResponse(
				null,
				null,
				null,
				null,
				null,
				content(contentText),
				linkStats(15000L, 177L, "April 2, 2026"),
				null,
				null,
				null
		);
	}

	private static ProcessingJobResultResponse succeededResultWithPlaces(
			ProcessingJobResultResponse.ResolvedPlaceResponse... places
	) {
		List<ProcessingJobResultResponse.ResolvedPlaceResponse> resolvedPlaces = List.of(places);
		return new ProcessingJobResultResponse(
				"job-place",
				"SUCCEEDED",
				"https://example.com/post/place",
				"https://example.com/post/place",
				"https://example.com/post/place",
				content("content ready"),
				linkStats(15000L, 177L, "April 2, 2026"),
				resolvedPlaces,
				null,
				null
		);
	}

	private static ProcessingJobResultResponse.ContentResponse content(String contentText) {
		return new ProcessingJobResultResponse.ContentResponse(
				"INSTAGRAM",
				contentText,
				null,
				null,
				null,
				List.of(),
				"INSTAGRAM_OG_META"
		);
	}

	private static ProcessingJobResultResponse.LinkStatsResponse linkStats(
			Long likeCount,
			Long commentCount,
			String postedAt
	) {
		return new ProcessingJobResultResponse.LinkStatsResponse(likeCount, commentCount, postedAt);
	}

	private static ProcessingJobResultResponse.ResolvedPlaceResponse place(String kakaoPlaceId, String placeName) {
		String effectiveKakaoPlaceId = kakaoPlaceId == null ? null : kakaoPlaceId;
		return new ProcessingJobResultResponse.ResolvedPlaceResponse(
				effectiveKakaoPlaceId,
				placeName,
				"Seoul Jongno-gu",
				"Seoul Road 1",
				new BigDecimal("126.972000000000"),
				new BigDecimal("37.570000000000"),
				"Food > Cafe",
				"CE7",
				effectiveKakaoPlaceId == null ? null : "https://place.map.kakao.com/" + effectiveKakaoPlaceId,
				"02-000-0000"
		);
	}

	private static PlaceSnapshot manualCafeSnapshot(String kakaoPlaceId, String placeName) {
		return PlaceSnapshot.kakao(
				kakaoPlaceId,
				placeName,
				"Food > Cafe",
				"CE7",
				"02-111-1111",
				"Seoul Dongdaemun-gu",
				"HUFS Road 1",
				new BigDecimal("127.058000000000"),
				new BigDecimal("37.596000000000"),
				"https://place.map.kakao.com/" + kakaoPlaceId
		);
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

	private static <T> T awaitValue(Supplier<T> supplier, Predicate<T> predicate) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		T value;
		do {
			value = supplier.get();
			if (predicate.test(value)) {
				return value;
			}
			Thread.sleep(50);
		} while (System.nanoTime() < deadline);
		throw new AssertionError("condition was not met within timeout");
	}
}
