package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.external.processing.ProcessingClient;
import com.hufs.capstone.backend.external.processing.dto.CreateProcessingJobResponse;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkCommand;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.LinkSource;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkCommandServiceTest {

	private static final Long USER_ID = 100L;
	private static final String ROOM_PUBLIC_ID = "11111111-1111-1111-1111-111111111111";

	@Mock
	private LinkRepository linkRepository;

	@Mock
	private ProcessingClient processingClient;

	@Mock
	private RoomAccessService roomAccessService;

	@Mock
	private LinkRegistrationWriteService linkRegistrationWriteService;

	@InjectMocks
	private LinkCommandService linkCommandService;

	@BeforeEach
	void setUp() {
		Mockito.lenient()
				.when(roomAccessService.requireMemberRoom(any(), anyLong()))
				.thenReturn(room(ROOM_PUBLIC_ID));
	}

	@Test
	void registerShouldCreateProcessingJobWhenGlobalLinkDoesNotExist() {
		when(linkRepository.findByNormalizedUrl("https://www.instagram.com/p/ABC123")).thenReturn(Optional.empty());
		when(processingClient.createJob("https://www.instagram.com/p/ABC123", ROOM_PUBLIC_ID, "WEB"))
				.thenReturn(new CreateProcessingJobResponse("job-123"));
		RegisterLinkResult persisted = new RegisterLinkResult(10L, "job-123", LinkAnalysisStatus.REQUESTED);
		when(linkRegistrationWriteService.registerWithinWriteTransaction(
				any(), eq(ROOM_PUBLIC_ID), eq(USER_ID), eq("WEB"), eq("job-123")
		)).thenReturn(persisted);

		RegisterLinkResult result = linkCommandService.register(
				USER_ID,
				new RegisterLinkCommand("https://instagram.com/p/ABC123/?utm_source=test", ROOM_PUBLIC_ID, LinkSource.WEB)
		);

		assertThat(result.linkId()).isEqualTo(10L);
		assertThat(result.processingJobId()).isEqualTo("job-123");
		verify(processingClient).createJob("https://www.instagram.com/p/ABC123", ROOM_PUBLIC_ID, "WEB");
	}

	@Test
	void registerShouldReuseExistingGlobalLinkWithoutCreatingJob() {
		Link existing = Link.register("https://www.instagram.com/p/ABC123", "https://www.instagram.com/p/ABC123", "job-existing");
		when(linkRepository.findByNormalizedUrl("https://www.instagram.com/p/ABC123")).thenReturn(Optional.of(existing));
		RegisterLinkResult persisted = new RegisterLinkResult(11L, "job-existing", LinkAnalysisStatus.PROCESSING);
		when(linkRegistrationWriteService.registerWithinWriteTransaction(
				any(), eq(ROOM_PUBLIC_ID), eq(USER_ID), eq("APP"), eq(null)
		)).thenReturn(persisted);

		RegisterLinkResult result = linkCommandService.register(
				USER_ID,
				new RegisterLinkCommand("https://www.instagram.com/p/ABC123?igsh=xx", ROOM_PUBLIC_ID, LinkSource.APP)
		);

		assertThat(result.linkId()).isEqualTo(11L);
		assertThat(result.processingJobId()).isEqualTo("job-existing");
		verify(processingClient, never()).createJob(any(), any(), any());
	}

	@Test
	void registerShouldRetryWhenNormalizedUrlDuplicateRaceOccurs() {
		when(linkRepository.findByNormalizedUrl("https://example.com/x")).thenReturn(Optional.empty());
		when(processingClient.createJob("https://example.com/x", ROOM_PUBLIC_ID, null))
				.thenReturn(new CreateProcessingJobResponse("job-new"));

		when(linkRegistrationWriteService.registerWithinWriteTransaction(any(), eq(ROOM_PUBLIC_ID), eq(USER_ID), eq(null), eq("job-new")))
				.thenThrow(new LinkRegistrationWriteService.LinkDuplicateRaceException("https://example.com/x", new RuntimeException("dup")));
		when(linkRegistrationWriteService.registerWithinWriteTransaction(any(), eq(ROOM_PUBLIC_ID), eq(USER_ID), eq(null), eq(null)))
				.thenReturn(new RegisterLinkResult(20L, "job-existing", LinkAnalysisStatus.REQUESTED));

		RegisterLinkResult result = linkCommandService.register(
				USER_ID,
				new RegisterLinkCommand("https://example.com/x", ROOM_PUBLIC_ID, null)
		);

		assertThat(result.linkId()).isEqualTo(20L);
		assertThat(result.processingJobId()).isEqualTo("job-existing");
		verify(linkRegistrationWriteService, times(2)).registerWithinWriteTransaction(any(), eq(ROOM_PUBLIC_ID), eq(USER_ID), eq(null), any());
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

	private static Room room(String publicId) {
		return Room.create(publicId, "테스트 방", "INVITE123456", USER_ID);
	}
}
