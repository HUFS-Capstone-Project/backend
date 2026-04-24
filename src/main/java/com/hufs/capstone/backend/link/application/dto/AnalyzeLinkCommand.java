package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.link.domain.LinkSource;

public record AnalyzeLinkCommand(
		String url,
		LinkSource source
) {
}
