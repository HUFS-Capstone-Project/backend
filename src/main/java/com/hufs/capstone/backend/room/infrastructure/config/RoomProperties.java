package com.hufs.capstone.backend.room.infrastructure.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.room")
public class RoomProperties {

	private JoinRateLimit joinRateLimit = new JoinRateLimit();

	@Getter
	@Setter
	public static class JoinRateLimit {

		private int maxAttempts = 5;
		private Duration window = Duration.ofMinutes(1);
	}
}

