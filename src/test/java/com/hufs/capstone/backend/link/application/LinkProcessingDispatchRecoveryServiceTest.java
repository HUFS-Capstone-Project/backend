package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.link.application.event.LinkProcessingRequestedEvent;
import com.hufs.capstone.backend.link.domain.entity.LinkProcessingDispatchAttempt;
import com.hufs.capstone.backend.link.domain.repository.LinkProcessingDispatchAttemptRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkProcessingDispatchRecoveryServiceTest {

	@Mock
	private LinkProcessingDispatchAttemptRepository dispatchAttemptRepository;

	@Mock
	private LinkProcessingDispatchPolicy dispatchPolicy;

	private LinkProcessingDispatchRecoveryService recoveryService;

	@BeforeEach
	void setUp() {
		recoveryService = new LinkProcessingDispatchRecoveryService(
				dispatchAttemptRepository,
				dispatchPolicy
		);
	}

	@Test
	void shouldBuildRecoveryEventsWithOnlyPersistedAttemptIds() {
		Instant now = Instant.parse("2026-08-23T00:00:00Z");
		when(dispatchPolicy.getStaleThreshold()).thenReturn(Duration.ofMinutes(2));
		LinkProcessingDispatchAttempt firstAttempt = dispatchAttempt(1L);
		LinkProcessingDispatchAttempt secondAttempt = dispatchAttempt(2L);
		when(dispatchAttemptRepository.findStaleTargets(
				any(), any(), any(), any(), any(), any()
		)).thenReturn(List.of(firstAttempt, secondAttempt));

		List<LinkProcessingRequestedEvent> events = recoveryService.findRecoverableEvents(now, 50);

		assertThat(events).containsExactly(
				new LinkProcessingRequestedEvent(1L),
				new LinkProcessingRequestedEvent(2L)
		);
		verify(dispatchAttemptRepository).findStaleTargets(
				any(), any(), any(), any(), any(), any()
		);
	}

	@Test
	void shouldNotQueryRecoveryTargetsWhenBatchSizeIsNotPositive() {
		List<LinkProcessingRequestedEvent> events = recoveryService.findRecoverableEvents(Instant.now(), 0);

		assertThat(events).isEmpty();
		verify(dispatchAttemptRepository, never()).findStaleTargets(
				any(), any(), any(), any(), any(), any()
		);
	}

	private static LinkProcessingDispatchAttempt dispatchAttempt(Long attemptId) {
		LinkProcessingDispatchAttempt attempt = mock(LinkProcessingDispatchAttempt.class);
		when(attempt.getId()).thenReturn(attemptId);
		return attempt;
	}
}
