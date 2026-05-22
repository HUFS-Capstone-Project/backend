package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LinkUrlNormalizerTest {

	@Test
	void shouldCanonicalizeInstagramReelsAndReelToSameUrl() {
		assertThat(LinkUrlNormalizer.normalize("https://instagram.com/reels/DVDm96wjwWC/").normalizedUrl())
				.isEqualTo("https://www.instagram.com/reel/DVDm96wjwWC/");
		assertThat(LinkUrlNormalizer.normalize("https://www.instagram.com/reel/DVDm96wjwWC/").normalizedUrl())
				.isEqualTo("https://www.instagram.com/reel/DVDm96wjwWC/");
	}

	@Test
	void shouldRemoveInstagramQueryAndFragment() {
		LinkUrlNormalizer.NormalizedUrl normalized = LinkUrlNormalizer.normalize(
				"  https://m.instagram.com/reels/DVDm96wjwWC/?igsh=abc#comments  "
		);

		assertThat(normalized.originalUrl()).isEqualTo("https://m.instagram.com/reels/DVDm96wjwWC/?igsh=abc#comments");
		assertThat(normalized.normalizedUrl()).isEqualTo("https://www.instagram.com/reel/DVDm96wjwWC/");
	}

	@Test
	void shouldCanonicalizeInstagramPostAndTvUrls() {
		assertThat(LinkUrlNormalizer.normalize("http://instagram.com/p/ABC123/?utm_source=x").normalizedUrl())
				.isEqualTo("https://www.instagram.com/p/ABC123/");
		assertThat(LinkUrlNormalizer.normalize("https://instagr.am/p/ABC123/").normalizedUrl())
				.isEqualTo("https://www.instagram.com/p/ABC123/");
		assertThat(LinkUrlNormalizer.normalize("https://www.instagram.com/tv/XYZ789/#x").normalizedUrl())
				.isEqualTo("https://www.instagram.com/tv/XYZ789/");
	}

	@Test
	void shouldCanonicalizeNaverBlogDesktopAndMobileToSameUrl() {
		assertThat(LinkUrlNormalizer.normalize("https://blog.naver.com/blogId/12345?x=1#frag").normalizedUrl())
				.isEqualTo("https://blog.naver.com/blogId/12345");
		assertThat(LinkUrlNormalizer.normalize("https://m.blog.naver.com/blogId/12345").normalizedUrl())
				.isEqualTo("https://blog.naver.com/blogId/12345");
	}

	@Test
	void shouldFallbackToGenericWhenNaverBlogLogNoIsNotNumeric() {
		assertThat(LinkUrlNormalizer.normalize("https://m.blog.naver.com/blogId/not-a-number?b=2&a=1#frag").normalizedUrl())
				.isEqualTo("https://m.blog.naver.com/blogId/not-a-number?a=1&b=2");
	}

	@Test
	void shouldCanonicalizeYoutubeVideoUrlsToSameWatchUrl() {
		assertThat(LinkUrlNormalizer.normalize("https://youtu.be/ZJMi3m8spJA?si=abc").normalizedUrl())
				.isEqualTo("https://www.youtube.com/watch?v=ZJMi3m8spJA");
		assertThat(LinkUrlNormalizer.normalize("https://youtube.com/shorts/ZJMi3m8spJA?feature=share").normalizedUrl())
				.isEqualTo("https://www.youtube.com/watch?v=ZJMi3m8spJA");
		assertThat(LinkUrlNormalizer.normalize("https://m.youtube.com/watch?v=ZJMi3m8spJA&list=abc").normalizedUrl())
				.isEqualTo("https://www.youtube.com/watch?v=ZJMi3m8spJA");
	}

	@Test
	void shouldFallbackToGenericWhenYoutubeUrlHasNoVideoId() {
		assertThat(LinkUrlNormalizer.normalize("https://www.youtube.com/playlist?list=abc").normalizedUrl())
				.isEqualTo("https://www.youtube.com/playlist?list=abc");
	}

	@Test
	void shouldKeepGenericQueryButDropFragmentAndNormalizeHostPathAndQueryOrder() {
		assertThat(LinkUrlNormalizer.normalize("HTTPS://Example.COM/post/1/?b=2&a=1#section").normalizedUrl())
				.isEqualTo("https://example.com/post/1?a=1&b=2");
	}
}
