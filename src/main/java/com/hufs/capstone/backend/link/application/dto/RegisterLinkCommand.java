package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.link.domain.LinkSource;

public record RegisterLinkCommand(
		String url,
		String roomId,
		LinkSource source
) {
}

