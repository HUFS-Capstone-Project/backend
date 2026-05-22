package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import com.hufs.capstone.backend.link.application.dto.ProcessingResultSnapshot;
import com.hufs.capstone.backend.link.application.event.LinkStatusSyncedEvent;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.LinkSourceType;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class LinkAnalysisStatusWriteServiceTest {

	@Mock
	private LinkRepository linkRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private LinkAnalysisResultAssembler linkAnalysisResultAssembler;

	@Mock
	private LinkPlaceCandidateSnapshotMapper placeCandidateSnapshotMapper;

	@Mock
	private LinkCandidateSyncService linkCandidateSyncService;

	@InjectMocks
	private LinkAnalysisStatusWriteService linkAnalysisStatusWriteService;

	@Test
	void applySyncSnapshotShouldReturnImmediatelyWhenTerminal() {
		Link terminal = link(1L, 1L);
		terminal.markSucceeded("done");
		LinkAnalysisResult mapped = result(1L, LinkAnalysisStatus.SUCCEEDED, "done", null, null);
		when(linkRepository.findById(1L)).thenReturn(Optional.of(terminal));
		when(linkAnalysisResultAssembler.from(terminal)).thenReturn(mapped);

		LinkAnalysisResult result = linkAnalysisStatusWriteService.applySyncSnapshot(
				1L,
				LinkAnalysisStatus.PROCESSING,
				null,
				null,
				null
		);

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		verify(linkRepository, never()).compareAndSetAnalysisResult(
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any()
		);
	}

	@Test
	void applySyncSnapshotShouldUpdateByCasAndSaveHistory() {
		Link processing = link(2L, 1L);
		processing.markProcessing();
		Link synced = link(2L, 2L);
		synced.markSucceeded("done");
		LinkAnalysisResult mapped = result(2L, LinkAnalysisStatus.SUCCEEDED, "done", null, null);

		when(linkRepository.findById(2L))
				.thenReturn(Optional.of(processing))
				.thenReturn(Optional.of(synced));
		when(linkAnalysisResultAssembler.from(synced)).thenReturn(mapped);
		stubCasUpdate(1);

		LinkAnalysisResult result = linkAnalysisStatusWriteService.applySyncSnapshot(
				2L,
				LinkAnalysisStatus.SUCCEEDED,
				resultSnapshot("done"),
				null,
				null
		);

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(result.contentText()).isEqualTo("done");
		ArgumentCaptor<LinkStatusSyncedEvent> eventCaptor = ArgumentCaptor.forClass(LinkStatusSyncedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().linkId()).isEqualTo(2L);
	}

	@Test
	void applySyncSnapshotShouldReturnLatestStateWhenCasMissed() {
		Link processing = link(3L, 1L);
		processing.markProcessing();
		Link latest = link(3L, 2L);
		latest.markSucceeded("latest");
		LinkAnalysisResult mapped = result(3L, LinkAnalysisStatus.SUCCEEDED, "latest", null, null);

		when(linkRepository.findById(3L))
				.thenReturn(Optional.of(processing))
				.thenReturn(Optional.of(processing))
				.thenReturn(Optional.of(processing))
				.thenReturn(Optional.of(latest));
		when(linkAnalysisResultAssembler.from(latest)).thenReturn(mapped);
		stubCasUpdate(0);

		LinkAnalysisResult result = linkAnalysisStatusWriteService.applySyncSnapshot(
				3L,
				LinkAnalysisStatus.SUCCEEDED,
				resultSnapshot("done"),
				null,
				null
		);

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(result.contentText()).isEqualTo("latest");
	}

	@Test
	void applySyncSnapshotShouldStoreFailedErrorDetails() {
		Link processing = link(4L, 1L);
		processing.markProcessing();
		Link failed = link(4L, 2L);
		failed.markFailed();
		ReflectionTestUtils.setField(failed, "errorCode", "E001");
		ReflectionTestUtils.setField(failed, "errorMessage", "failed");
		LinkAnalysisResult mapped = result(4L, LinkAnalysisStatus.FAILED, null, "E001", "failed");

		when(linkRepository.findById(4L))
				.thenReturn(Optional.of(processing))
				.thenReturn(Optional.of(failed));
		when(linkAnalysisResultAssembler.from(failed)).thenReturn(mapped);
		stubCasUpdate(1);

		LinkAnalysisResult result = linkAnalysisStatusWriteService.applySyncSnapshot(
				4L,
				LinkAnalysisStatus.FAILED,
				null,
				"E001",
				"failed"
		);

		assertThat(result.status()).isEqualTo(LinkAnalysisStatus.FAILED);
		assertThat(result.errorCode()).isEqualTo("E001");
		assertThat(result.errorMessage()).isEqualTo("failed");
	}

	@Test
	void applySyncSnapshotShouldUpdateLinkSourceTypeFromProcessingResult() {
		Link processing = link(5L, 1L);
		processing.markProcessing();
		Link synced = link(5L, 2L);
		synced.markSucceeded("done");
		when(linkRepository.findById(5L))
				.thenReturn(Optional.of(processing))
				.thenReturn(Optional.of(synced));
		when(linkAnalysisResultAssembler.from(synced))
				.thenReturn(result(5L, LinkAnalysisStatus.SUCCEEDED, "done", null, null));
		stubCasUpdate(1);

		linkAnalysisStatusWriteService.applySyncSnapshot(
				5L,
				LinkAnalysisStatus.SUCCEEDED,
				resultSnapshot("done", LinkSourceType.INSTAGRAM),
				null,
				null
		);

		assertThat(capturedLinkSourceType()).isEqualTo(LinkSourceType.INSTAGRAM);
	}

	@Test
	void applySyncSnapshotShouldKeepCurrentLinkSourceTypeWhenProcessingResultIsNull() {
		Link processing = link(6L, 1L, "https://www.instagram.com/p/abc/");
		processing.markProcessing();
		Link synced = link(6L, 2L, "https://www.instagram.com/p/abc/");
		synced.markSucceeded("done");
		when(linkRepository.findById(6L))
				.thenReturn(Optional.of(processing))
				.thenReturn(Optional.of(synced));
		when(linkAnalysisResultAssembler.from(synced))
				.thenReturn(result(6L, LinkAnalysisStatus.SUCCEEDED, "done", null, null));
		stubCasUpdate(1);

		linkAnalysisStatusWriteService.applySyncSnapshot(
				6L,
				LinkAnalysisStatus.SUCCEEDED,
				resultSnapshot("done", null),
				null,
				null
		);

		assertThat(capturedLinkSourceType()).isEqualTo(LinkSourceType.INSTAGRAM);
	}

	private static Link link(Long id, Long version) {
		return link(id, version, "https://example.com/p/" + id);
	}

	private static Link link(Long id, Long version, String url) {
		Link link = Link.register(url, url, "job-" + id);
		ReflectionTestUtils.setField(link, "id", id);
		ReflectionTestUtils.setField(link, "version", version);
		return link;
	}

	private void stubCasUpdate(int updatedCount) {
		when(linkRepository.compareAndSetAnalysisResult(
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(Instant.class)
		)).thenReturn(updatedCount);
	}

	private static ProcessingResultSnapshot resultSnapshot(String contentText) {
		return resultSnapshot(contentText, null);
	}

	private static ProcessingResultSnapshot resultSnapshot(String contentText, LinkSourceType linkSourceType) {
		return new ProcessingResultSnapshot(
				null,
				linkSourceType,
				contentText,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);
	}

	private static LinkAnalysisResult result(
			Long linkId,
			LinkAnalysisStatus status,
			String contentText,
			String errorCode,
			String errorMessage
	) {
		return new LinkAnalysisResult(linkId, status, contentText, errorCode, errorMessage);
	}

	private LinkSourceType capturedLinkSourceType() {
		ArgumentCaptor<LinkSourceType> linkSourceTypeCaptor = ArgumentCaptor.forClass(LinkSourceType.class);
		verify(linkRepository).compareAndSetAnalysisResult(
				any(),
				any(),
				any(),
				any(),
				linkSourceTypeCaptor.capture(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(Instant.class)
		);
		return linkSourceTypeCaptor.getValue();
	}
}
