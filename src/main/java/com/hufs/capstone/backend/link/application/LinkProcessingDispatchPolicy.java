package com.hufs.capstone.backend.link.application;

import jakarta.validation.constraints.Min;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Processing 디스패치 정책.
 *
 * <p>디스패치는 트랜잭션 커밋 이후 전용 비동기 executor에서 실행한다. 서버 종료나 executor 거절로 인메모리 이벤트가
 * 유실될 수 있으므로 stale 상태의 링크는 스케줄러가 다시 회수한다.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.link.dispatch")
public class LinkProcessingDispatchPolicy {

	@Min(1)
	private int maxAttempts = 3;

	private Duration retryBackoff = Duration.ofMillis(300);

	private boolean recoveryEnabled = true;

	private Duration staleThreshold = Duration.ofMinutes(1);

	@Min(1)
	private int recoveryBatchSize = 50;
}
