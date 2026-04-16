package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.external.processing.ProcessingClient;
import com.hufs.capstone.backend.external.processing.dto.CreateProcessingJobResponse;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkCommand;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkProcessingHistory;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.link.domain.repository.LinkProcessingHistoryRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LinkCommandServiceTest {

	@Mock
	private LinkRepository linkRepository;

	@Mock
	private RoomLinkRepository roomLinkRepository;

	@Mock
	private LinkProcessingHistoryRepository linkProcessingHistoryRepository;

	@Mock
	private ProcessingClient processingClient;

	@Mock
	private TransactionOperations transactionOperations;

	@InjectMocks
	private LinkCommandService linkCommandService;

	@BeforeEach
	void setUp() {
		org.mockito.Mockito.lenient()
				.when(transactionOperations.execute(org.mockito.ArgumentMatchers.<TransactionCallback<Object>>any()))
				.thenAnswer(invocation -> {
					TransactionCallback<Object> callback = invocation.getArgument(0);
					return callback.doInTransaction(new SimpleTransactionStatus());
				});
	}

	@Test
	void registerShouldCreateNewGlobalLinkAndRoomMapping() {
		when(linkRepository.findByNormalizedUrl("https://www.instagram.com/p/ABC123"))
				.thenReturn(Optional.empty());
		when(processingClient.createJob("https://www.instagram.com/p/ABC123", "room-1", "instagram"))
				.thenReturn(new CreateProcessingJobResponse("job-123"));
		when(linkRepository.saveAndFlush(any(Link.class))).thenAnswer(invocation -> {
			Link saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", 10L);
			return saved;
		});
		when(roomLinkRepository.saveAndFlush(any(RoomLink.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(linkProcessingHistoryRepository.saveAndFlush(any(LinkProcessingHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RegisterLinkResult result = linkCommandService.register(
				new RegisterLinkCommand("https://instagram.com/p/ABC123/?utm_source=test", "room-1", "instagram")
		);

		assertThat(result.linkId()).isEqualTo(10L);
		assertThat(result.processingJobId()).isEqualTo("job-123");
		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.REQUESTED);
	}

	@Test
	void registerShouldReuseExistingGlobalLinkWithoutCreatingNewJob() {
		Link existing = Link.register("https://www.instagram.com/p/ABC123", "https://www.instagram.com/p/ABC123", "job-existing");
		ReflectionTestUtils.setField(existing, "id", 11L);
		when(linkRepository.findByNormalizedUrl("https://www.instagram.com/p/ABC123"))
				.thenReturn(Optional.of(existing));
		when(roomLinkRepository.saveAndFlush(any(RoomLink.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(linkProcessingHistoryRepository.saveAndFlush(any(LinkProcessingHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RegisterLinkResult result = linkCommandService.register(
				new RegisterLinkCommand("https://www.instagram.com/p/ABC123?igsh=xx", "room-2", "instagram")
		);

		assertThat(result.linkId()).isEqualTo(11L);
		assertThat(result.processingJobId()).isEqualTo("job-existing");
		verify(processingClient, never()).createJob(any(), any(), any());
	}

	@Test
	void registerShouldThrowConflictWhenSameRoomAlreadyHasSameLink() {
		Link existing = Link.register("https://example.com/x", "https://example.com/x", "job-existing");
		ReflectionTestUtils.setField(existing, "id", 12L);

		when(linkRepository.findByNormalizedUrl("https://example.com/x"))
				.thenReturn(Optional.of(existing));
		when(roomLinkRepository.saveAndFlush(any(RoomLink.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate"));

		assertThatThrownBy(() -> linkCommandService.register(
				new RegisterLinkCommand("https://example.com/x", "room-1", null)
		))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E409_CONFLICT));
	}

	@Test
	void registerShouldFailWhenRoomIdIsMissing() {
		assertThatThrownBy(() -> linkCommandService.register(
				new RegisterLinkCommand("https://example.com/x", "   ", null)
		))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E400_ILLEGAL_ARGUMENT));
	}

	@Test
	void registerShouldRecoverWhenGlobalInsertRaceOccurs() {
		Link existing = Link.register("https://example.com/x", "https://example.com/x", "job-existing");
		ReflectionTestUtils.setField(existing, "id", 20L);

		when(linkRepository.findByNormalizedUrl("https://example.com/x"))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(existing));
		when(processingClient.createJob("https://example.com/x", "room-1", null))
				.thenReturn(new CreateProcessingJobResponse("job-new"));
		when(roomLinkRepository.saveAndFlush(any(RoomLink.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(linkProcessingHistoryRepository.saveAndFlush(any(LinkProcessingHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RegisterLinkResult result = linkCommandService.register(
				new RegisterLinkCommand("https://example.com/x", "room-1", null)
		);

		assertThat(result.linkId()).isEqualTo(20L);
		assertThat(result.processingJobId()).isEqualTo("job-existing");
	}
}
