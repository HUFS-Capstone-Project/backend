package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LinkAnalysisStatusServiceTest {

	private static final Long USER_ID = 100L;
	private static final String ROOM_PUBLIC_ID = "11111111-1111-1111-1111-111111111111";

	@Mock
	private LinkAnalysisAuthorizationService linkAnalysisAuthorizationService;

	@Mock
	private LinkAnalysisCacheCoordinator linkAnalysisCacheCoordinator;

	@Mock
	private LinkRepository linkRepository;

	@Mock
	private LinkSyncOrchestrator linkSyncOrchestrator;

	@Mock
	private LinkAnalysisStatusResolver linkAnalysisStatusResolver;

	@Mock
	private LinkAnalysisStatusWriteService linkAnalysisStatusWriteService;

	@InjectMocks
	private LinkAnalysisStatusService linkAnalysisStatusService;

	@Test
	void getLinkAnalysisResultShouldSyncAndWriteOnCacheMissWhenResolverRequiresWrite() {
		Link snapshot = link(10L, "job-1", LinkAnalysisStatus.PROCESSING);
		LinkSyncOrchestrator.ProcessingSyncSnapshot syncSnapshot =
				new LinkSyncOrchestrator.ProcessingSyncSnapshot(LinkAnalysisStatus.SUCCEEDED, "caption", null, null);
		LinkAnalysisStatusResolver.Resolution resolution = LinkAnalysisStatusResolver.Resolution.write(
				LinkAnalysisStatus.SUCCEEDED,
				"caption",
				null,
				null
		);
		LinkAnalysisResult synced = new LinkAnalysisResult(
				10L,
				LinkAnalysisStatus.SUCCEEDED,
				"caption",
				null,
				null
		);

		when(linkRepository.findById(10L)).thenReturn(Optional.of(snapshot));
		when(linkSyncOrchestrator.resolve(snapshot)).thenReturn(syncSnapshot);
		when(linkAnalysisStatusResolver.resolve(snapshot, syncSnapshot)).thenReturn(resolution);
		when(linkAnalysisStatusWriteService.applySyncSnapshot(10L, LinkAnalysisStatus.SUCCEEDED, "caption", null, null))
				.thenReturn(synced);
		when(linkAnalysisCacheCoordinator.getOrLoad(eq(10L), any())).thenAnswer(invocation -> {
			@SuppressWarnings("unchecked")
			Supplier<LinkAnalysisResult> loader = invocation.getArgument(1, Supplier.class);
			return loader.get();
		});

		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(USER_ID, ROOM_PUBLIC_ID, 10L);

		assertThat(result).isEqualTo(synced);
		verify(linkAnalysisAuthorizationService).assertReadable(USER_ID, ROOM_PUBLIC_ID, 10L);
		verify(linkSyncOrchestrator).resolve(snapshot);
		verify(linkAnalysisStatusWriteService).applySyncSnapshot(10L, LinkAnalysisStatus.SUCCEEDED, "caption", null, null);
	}

	@Test
	void getLinkAnalysisResultShouldReturnSnapshotWithoutExternalCallWhenTerminal() {
		Link terminal = link(11L, "job-2", LinkAnalysisStatus.SUCCEEDED);
		terminal.markSucceeded("caption done");
		LinkAnalysisStatusResolver.Resolution resolution = LinkAnalysisStatusResolver.Resolution.noWrite();
		when(linkRepository.findById(11L)).thenReturn(Optional.of(terminal));
		when(linkAnalysisStatusResolver.resolve(terminal, null)).thenReturn(resolution);
		when(linkAnalysisCacheCoordinator.getOrLoad(eq(11L), any())).thenAnswer(invocation -> {
			@SuppressWarnings("unchecked")
			Supplier<LinkAnalysisResult> loader = invocation.getArgument(1, Supplier.class);
			return loader.get();
		});

		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(USER_ID, ROOM_PUBLIC_ID, 11L);

		assertThat(result.linkId()).isEqualTo(11L);
		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(result.captionRaw()).isEqualTo("caption done");
		verify(linkAnalysisAuthorizationService).assertReadable(USER_ID, ROOM_PUBLIC_ID, 11L);
		verify(linkSyncOrchestrator, never()).resolve(any());
		verify(linkAnalysisStatusWriteService, never()).applySyncSnapshot(any(), any(), any(), any(), any());
	}

	@Test
	void getLinkAnalysisResultShouldReturnCachedValueWithoutLoadingAgain() {
		LinkAnalysisResult cached = new LinkAnalysisResult(
				12L,
				LinkAnalysisStatus.PROCESSING,
				null,
				null,
				null
		);
		when(linkAnalysisCacheCoordinator.getOrLoad(eq(12L), any())).thenReturn(cached);

		LinkAnalysisResult result = linkAnalysisStatusService.getLinkAnalysisResult(USER_ID, ROOM_PUBLIC_ID, 12L);

		assertThat(result).isEqualTo(cached);
		verify(linkAnalysisAuthorizationService).assertReadable(USER_ID, ROOM_PUBLIC_ID, 12L);
		verify(linkRepository, never()).findById(any());
		verify(linkSyncOrchestrator, never()).resolve(any());
		verify(linkAnalysisStatusWriteService, never()).applySyncSnapshot(any(), any(), any(), any(), any());
	}

	private static Link link(Long id, String processingJobId, LinkAnalysisStatus status) {
		Link link = Link.register("https://example.com/p/" + id, "https://example.com/p/" + id, processingJobId);
		ReflectionTestUtils.setField(link, "id", id);
		if (status == LinkAnalysisStatus.PROCESSING) {
			link.markProcessing();
		}
		if (status == LinkAnalysisStatus.FAILED) {
			link.markFailed();
		}
		if (status == LinkAnalysisStatus.SUCCEEDED) {
			link.markSucceeded("caption");
		}
		return link;
	}
}
