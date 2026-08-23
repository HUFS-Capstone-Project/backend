package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.external.processing.ProcessingProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class LinkProcessingDispatchPolicyGuard implements InitializingBean {

	private final LinkProcessingDispatchPolicy dispatchPolicy;
	private final ProcessingProperties processingProperties;

	@Override
	public void afterPropertiesSet() {
		Duration maximumDispatchDuration = maximumDispatchDuration();
		if (!dispatchPolicy.isRecoveryEnabled()) {
			return;
		}

		Duration staleThreshold = dispatchPolicy.getStaleThreshold();
		if (staleThreshold == null || staleThreshold.isNegative() || staleThreshold.isZero()) {
			throw new IllegalArgumentException("app.link.dispatch.stale-threshold must be positive");
		}

		Duration minimumStaleThreshold = maximumDispatchDuration.plus(maximumDispatchDuration.dividedBy(5));
		if (staleThreshold.compareTo(minimumStaleThreshold) < 0) {
			throw new IllegalArgumentException(
					"app.link.dispatch.stale-threshold must include 20% headroom over the maximum dispatch duration: "
							+ "stale-threshold=" + staleThreshold + ", minimum-stale-threshold=" + minimumStaleThreshold
			);
		}
	}

	Duration maximumDispatchDuration() {
		int maxAttempts = dispatchPolicy.getMaxAttempts();
		if (maxAttempts < 1) {
			throw new IllegalArgumentException("app.link.dispatch.max-attempts must be at least 1");
		}

		Duration retryBackoff = dispatchPolicy.getRetryBackoff();
		if (retryBackoff == null || retryBackoff.isNegative()) {
			throw new IllegalArgumentException("app.link.dispatch.retry-backoff must not be negative");
		}

		long oneAttemptMillis = Math.addExact(
				(long) processingProperties.connectTimeoutMs(),
				(long) processingProperties.readTimeoutMs()
		);
		long attemptsMillis = Math.multiplyExact(oneAttemptMillis, (long) maxAttempts);
		long backoffMillis = Math.multiplyExact(retryBackoff.toMillis(), (long) maxAttempts - 1L);
		return Duration.ofMillis(Math.addExact(attemptsMillis, backoffMillis));
	}
}
