package com.hufs.capstone.backend.link.application;

import jakarta.validation.constraints.Min;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 참고:
 * - 트랜잭션 커밋 후(AFTER_COMMIT) 동기 디스패치
 * - 재시도 소진 시 DISPATCH_FAILED로 전환하고, 이후 사용자 수동 재시도만 허용
 * 위 정책은 임시 운영 타협안이며 이 클래스에서 중앙 관리한다.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.link.dispatch")
public class LinkProcessingDispatchPolicy {

	@Min(1)
	private int maxAttempts = 3;

	private Duration retryBackoff = Duration.ofMillis(300);
}
