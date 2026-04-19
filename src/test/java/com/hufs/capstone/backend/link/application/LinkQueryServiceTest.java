package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.link.application.dto.LinkStatusResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LinkQueryServiceTest {

	private static final Long USER_ID = 100L;

	@Mock
	private LinkAuthorizationService linkAuthorizationService;

	@Mock
	private LinkCacheCoordinator linkCacheCoordinator;

	@Mock
	private LinkRepository linkRepository;

	@Mock
	private LinkSyncOrchestrator linkSyncOrchestrator;

	@Mock
	private LinkStatusResolver linkStatusResolver;

	@Mock
	private LinkStatusWriteService linkStatusWriteService;

	@InjectMocks
	private LinkQueryService linkQueryService;

	@Test
	void getLinkStatusShouldSyncAndWriteOnCacheMissWhenResolverRequiresWrite() {
		Link snapshot = link(10L, "job-1", LinkAnalysisStatus.PROCESSING);
		LinkSyncOrchestrator.ProcessingSyncSnapshot syncSnapshot =
				new LinkSyncOrchestrator.ProcessingSyncSnapshot(LinkAnalysisStatus.SUCCEEDED, "caption");
		LinkStatusResolver.Resolution resolution = LinkStatusResolver.Resolution.write(
				LinkAnalysisStatus.SUCCEEDED,
				"caption"
		);
		LinkStatusResult synced = new LinkStatusResult(
				10L,
				"https://example.com/p/10",
				"job-1",
				LinkAnalysisStatus.SUCCEEDED,
				"caption",
				Instant.now(),
				Instant.now()
		);

		when(linkRepository.findById(10L)).thenReturn(Optional.of(snapshot));
		when(linkSyncOrchestrator.resolve(snapshot)).thenReturn(syncSnapshot);
		when(linkStatusResolver.resolve(snapshot, syncSnapshot)).thenReturn(resolution);
		when(linkStatusWriteService.applySyncSnapshot(10L, LinkAnalysisStatus.SUCCEEDED, "caption")).thenReturn(synced);
		when(linkCacheCoordinator.getOrLoad(eq(10L), any())).thenAnswer(invocation -> {
			@SuppressWarnings("unchecked")
			Supplier<LinkStatusResult> loader = invocation.getArgument(1, Supplier.class);
			return loader.get();
		});

		LinkStatusResult result = linkQueryService.getLinkStatus(USER_ID, 10L);

		assertThat(result).isEqualTo(synced);
		verify(linkAuthorizationService).assertReadable(USER_ID, 10L);
		verify(linkSyncOrchestrator).resolve(snapshot);
		verify(linkStatusWriteService).applySyncSnapshot(10L, LinkAnalysisStatus.SUCCEEDED, "caption");
	}

	@Test
	void getLinkStatusShouldReturnSnapshotWithoutExternalCallWhenResolverDoesNotRequireWrite() {
		Link terminal = link(11L, "job-2", LinkAnalysisStatus.SUCCEEDED);
		terminal.markSucceeded("caption done");
		LinkStatusResolver.Resolution resolution = LinkStatusResolver.Resolution.noWrite();
		when(linkRepository.findById(11L)).thenReturn(Optional.of(terminal));
		when(linkStatusResolver.resolve(terminal, null)).thenReturn(resolution);
		when(linkCacheCoordinator.getOrLoad(eq(11L), any())).thenAnswer(invocation -> {
			@SuppressWarnings("unchecked")
			Supplier<LinkStatusResult> loader = invocation.getArgument(1, Supplier.class);
			return loader.get();
		});

		LinkStatusResult result = linkQueryService.getLinkStatus(USER_ID, 11L);

		assertThat(result.linkId()).isEqualTo(11L);
		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		verify(linkAuthorizationService).assertReadable(USER_ID, 11L);
		verify(linkSyncOrchestrator, never()).resolve(any());
		verify(linkStatusWriteService, never()).applySyncSnapshot(any(), any(), any());
	}

	@Test
	void getLinkStatusShouldReturnCachedValueWithoutLoadingAgain() {
		LinkStatusResult cached = new LinkStatusResult(
				12L,
				"https://example.com/p/12",
				"job-12",
				LinkAnalysisStatus.PROCESSING,
				null,
				Instant.now(),
				Instant.now()
		);
		when(linkCacheCoordinator.getOrLoad(eq(12L), any())).thenReturn(cached);

		LinkStatusResult result = linkQueryService.getLinkStatus(USER_ID, 12L);

		assertThat(result).isEqualTo(cached);
		verify(linkAuthorizationService).assertReadable(USER_ID, 12L);
		verify(linkRepository, never()).findById(any());
		verify(linkSyncOrchestrator, never()).resolve(any());
		verify(linkStatusWriteService, never()).applySyncSnapshot(any(), any(), any());
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
