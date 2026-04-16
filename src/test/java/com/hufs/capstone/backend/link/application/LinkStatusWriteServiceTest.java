package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.link.application.dto.LinkStatusResult;
import com.hufs.capstone.backend.link.application.event.LinkStatusSyncedEvent;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class LinkStatusWriteServiceTest {

	private static final Set<LinkAnalysisStatus> UPDATABLE_STATUSES =
			Set.of(LinkAnalysisStatus.REQUESTED, LinkAnalysisStatus.PROCESSING);

	@Mock
	private LinkRepository linkRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private LinkStatusWriteService linkStatusWriteService;

	@Test
	void applySyncSnapshotShouldReturnImmediatelyWhenTerminal() {
		Link terminal = link(1L, 3L, LinkAnalysisStatus.SUCCEEDED, "caption");
		when(linkRepository.findById(1L)).thenReturn(Optional.of(terminal));

		LinkStatusResult result = linkStatusWriteService.applySyncSnapshot(1L, LinkAnalysisStatus.PROCESSING, null);

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		verify(linkRepository, never()).compareAndSetStatus(
				anyLong(),
				anyLong(),
				org.mockito.ArgumentMatchers.<java.util.Collection<LinkAnalysisStatus>>any(),
				any(),
				any(Instant.class)
		);
		verify(linkRepository, never()).compareAndSetStatusAndCaption(
				anyLong(),
				anyLong(),
				org.mockito.ArgumentMatchers.<java.util.Collection<LinkAnalysisStatus>>any(),
				any(),
				any(),
				any(Instant.class)
		);
		verify(eventPublisher, never()).publishEvent(any(LinkStatusSyncedEvent.class));
	}

	@Test
	void applySyncSnapshotShouldUpdateByCasAndSaveHistory() {
		Link before = link(2L, 5L, LinkAnalysisStatus.PROCESSING, null);
		Link after = link(2L, 6L, LinkAnalysisStatus.SUCCEEDED, "done");
		when(linkRepository.findById(2L)).thenReturn(Optional.of(before), Optional.of(after));
		when(linkRepository.compareAndSetStatusAndCaption(
				eq(2L),
				eq(5L),
				argThat(UPDATABLE_STATUSES::equals),
				eq(LinkAnalysisStatus.SUCCEEDED),
				eq("done"),
				any(Instant.class)
		)).thenReturn(1);

		LinkStatusResult result = linkStatusWriteService.applySyncSnapshot(2L, LinkAnalysisStatus.SUCCEEDED, "done");

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(result.captionRaw()).isEqualTo("done");
		verify(eventPublisher).publishEvent(any(LinkStatusSyncedEvent.class));
	}

	@Test
	void applySyncSnapshotShouldReturnLatestStateWhenCasMissed() {
		Link before = link(3L, 1L, LinkAnalysisStatus.PROCESSING, null);
		Link latest = link(3L, 2L, LinkAnalysisStatus.SUCCEEDED, "other-writer");
		when(linkRepository.findById(3L)).thenReturn(Optional.of(before), Optional.of(latest));
		when(linkRepository.compareAndSetStatusAndCaption(
				eq(3L),
				eq(1L),
				argThat(UPDATABLE_STATUSES::equals),
				eq(LinkAnalysisStatus.SUCCEEDED),
				eq("done"),
				any(Instant.class)
		)).thenReturn(0);

		LinkStatusResult result = linkStatusWriteService.applySyncSnapshot(3L, LinkAnalysisStatus.SUCCEEDED, "done");

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(result.captionRaw()).isEqualTo("other-writer");
		verify(eventPublisher, never()).publishEvent(any(LinkStatusSyncedEvent.class));
	}

	private static Link link(Long id, Long version, LinkAnalysisStatus status, String caption) {
		Link link = Link.register("https://example.com/p/1", "https://example.com/p/1", "job-1");
		ReflectionTestUtils.setField(link, "id", id);
		ReflectionTestUtils.setField(link, "version", version);
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
