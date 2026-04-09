package com.hufs.capstone.backend.auth.domain.vo;

import com.hufs.capstone.backend.auth.domain.enums.ClientPlatform;
import com.hufs.capstone.backend.auth.domain.enums.DeviceType;

public record ClientContext(
		DeviceType deviceType,
		ClientPlatform clientPlatform,
		String userAgent,
		String ipAddress
) {
}

