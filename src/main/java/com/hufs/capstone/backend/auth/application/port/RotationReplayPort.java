package com.hufs.capstone.backend.auth.application.port;

import com.hufs.capstone.backend.auth.application.dto.TokenPair;
import com.hufs.capstone.backend.auth.domain.vo.ClientContext;
import java.time.Duration;

public interface RotationReplayPort {

	void save(String oldTokenHash, ClientContext context, TokenPair tokenPair, Duration ttl);

	TokenPair findReplay(String oldTokenHash, ClientContext context);
}
