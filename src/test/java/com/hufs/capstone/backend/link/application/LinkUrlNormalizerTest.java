package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LinkUrlNormalizerTest {

	@Test
	void shouldOnlyTrimAndValidateUrlWithoutCanonicalizing() {
		LinkUrlNormalizer.NormalizedUrl normalized = LinkUrlNormalizer.normalize(
				"  https://www.instagram.com/reels/DVDm96wjwWC/?igsh=abc  "
		);

		assertThat(normalized.originalUrl()).isEqualTo("https://www.instagram.com/reels/DVDm96wjwWC/?igsh=abc");
		assertThat(normalized.normalizedUrl()).isEqualTo("https://www.instagram.com/reels/DVDm96wjwWC/?igsh=abc");
	}
}
