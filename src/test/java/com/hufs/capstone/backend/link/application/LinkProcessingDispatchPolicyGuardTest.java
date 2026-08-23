package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hufs.capstone.backend.external.processing.ProcessingProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LinkProcessingDispatchPolicyGuardTest {

	private static final ProcessingProperties PROCESSING_PROPERTIES = new ProcessingProperties(
			"http://localhost:8000",
			"test-key",
			3_000,
			30_000
	);

	@Test
	void shouldAcceptStaleThresholdLongerThanMaximumDispatchDuration() {
		LinkProcessingDispatchPolicy policy = new LinkProcessingDispatchPolicy();
		policy.setStaleThreshold(Duration.ofMinutes(2));
		LinkProcessingDispatchPolicyGuard guard = new LinkProcessingDispatchPolicyGuard(
				policy,
				PROCESSING_PROPERTIES
		);

		assertThat(guard.maximumDispatchDuration()).isEqualTo(Duration.ofMillis(99_600));
		assertThatCode(guard::afterPropertiesSet).doesNotThrowAnyException();
	}

	@Test
	void shouldRejectStaleThresholdThatCanOverlapNormalDispatch() {
		LinkProcessingDispatchPolicy policy = new LinkProcessingDispatchPolicy();
		policy.setStaleThreshold(Duration.ofMinutes(1));
		LinkProcessingDispatchPolicyGuard guard = new LinkProcessingDispatchPolicyGuard(
				policy,
				PROCESSING_PROPERTIES
		);

		assertThatThrownBy(guard::afterPropertiesSet)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("stale-threshold")
				.hasMessageContaining("PT1M59.52S");
	}

	@Test
	void shouldSkipStaleThresholdValidationWhenRecoveryIsDisabled() {
		LinkProcessingDispatchPolicy policy = new LinkProcessingDispatchPolicy();
		policy.setRecoveryEnabled(false);
		policy.setStaleThreshold(Duration.ofSeconds(1));
		LinkProcessingDispatchPolicyGuard guard = new LinkProcessingDispatchPolicyGuard(
				policy,
				PROCESSING_PROPERTIES
		);

		assertThatCode(guard::afterPropertiesSet).doesNotThrowAnyException();
	}
}
