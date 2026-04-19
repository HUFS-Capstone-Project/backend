package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkCommand;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.ProcessingDispatchStatus;
import com.hufs.capstone.backend.link.domain.LinkSource;
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
class LinkCommandServiceTest {

	private static final Long USER_ID = 100L;
	private static final String ROOM_PUBLIC_ID = "11111111-1111-1111-1111-111111111111";

	@Mock
	private LinkRegistrationWriteService linkRegistrationWriteService;

	@Mock
	private LinkRepository linkRepository;

	@InjectMocks
	private LinkCommandService linkCommandService;

	@Test
	void registerShouldReturnDirectResultWhenJobIdIsNotPending() {
		RegisterLinkResult persisted = new RegisterLinkResult(
				10L,
				"job-123",
				LinkAnalysisStatus.REQUESTED,
				ProcessingDispatchStatus.DISPATCHED
		);
		when(linkRegistrationWriteService.registerWithinWriteTransaction(any(), eq(ROOM_PUBLIC_ID), eq(USER_ID), eq("WEB")))
				.thenReturn(persisted);

		RegisterLinkResult result = linkCommandService.register(
				USER_ID,
				new RegisterLinkCommand("https://instagram.com/p/ABC123/?utm_source=test", ROOM_PUBLIC_ID, LinkSource.WEB)
		);

		assertThat(result.linkId()).isEqualTo(10L);
		assertThat(result.processingJobId()).isEqualTo("job-123");
		verify(linkRepository, never()).findById(any());
	}

	@Test
	void registerShouldRefreshResultWhenPendingJobIdWasAssigned() {
		RegisterLinkResult persisted = new RegisterLinkResult(
				20L,
				null,
				LinkAnalysisStatus.REQUESTED,
				ProcessingDispatchStatus.PENDING
		);
		when(linkRegistrationWriteService.registerWithinWriteTransaction(any(), eq(ROOM_PUBLIC_ID), eq(USER_ID), eq(null)))
				.thenReturn(persisted);
		when(linkRepository.findById(20L)).thenReturn(Optional.of(link(20L, "job-actual-1", LinkAnalysisStatus.REQUESTED)));

		RegisterLinkResult result = linkCommandService.register(
				USER_ID,
				new RegisterLinkCommand("https://example.com/x", ROOM_PUBLIC_ID, null)
		);

		assertThat(result.linkId()).isEqualTo(20L);
		assertThat(result.processingJobId()).isEqualTo("job-actual-1");
	}

	@Test
	void registerShouldRetryWhenNormalizedUrlDuplicateRaceOccurs() {
		when(linkRegistrationWriteService.registerWithinWriteTransaction(any(), eq(ROOM_PUBLIC_ID), eq(USER_ID), eq(null)))
				.thenThrow(new LinkRegistrationWriteService.LinkDuplicateRaceException("https://example.com/x", new RuntimeException("dup")))
				.thenReturn(new RegisterLinkResult(
						30L,
						"job-existing",
						LinkAnalysisStatus.REQUESTED,
						ProcessingDispatchStatus.DISPATCHED
				));

		RegisterLinkResult result = linkCommandService.register(
				USER_ID,
				new RegisterLinkCommand("https://example.com/x", ROOM_PUBLIC_ID, null)
		);

		assertThat(result.linkId()).isEqualTo(30L);
		verify(linkRegistrationWriteService, times(2))
				.registerWithinWriteTransaction(any(), eq(ROOM_PUBLIC_ID), eq(USER_ID), eq(null));
	}

	@Test
	void registerShouldFailWhenRoomIdIsBlank() {
		assertThatThrownBy(() -> linkCommandService.register(
				USER_ID,
				new RegisterLinkCommand("https://example.com/x", "  ", null)
		))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E400_ILLEGAL_ARGUMENT));
	}

	private static Link link(Long id, String processingJobId, LinkAnalysisStatus status) {
		Link link = Link.register("https://example.com/p/1", "https://example.com/p/1", processingJobId);
		ReflectionTestUtils.setField(link, "id", id);
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
