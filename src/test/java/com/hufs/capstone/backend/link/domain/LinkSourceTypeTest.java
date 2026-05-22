package com.hufs.capstone.backend.link.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.hufs.capstone.backend.link.domain.entity.Link;
import org.junit.jupiter.api.Test;

class LinkSourceTypeTest {

	@Test
	void resolvesInstagramHosts() {
		assertThat(LinkSourceTypeResolver.fromUrl("https://www.instagram.com/p/abc/"))
				.isEqualTo(LinkSourceType.INSTAGRAM);
		assertThat(LinkSourceTypeResolver.fromUrl("https://instagr.am/p/abc/"))
				.isEqualTo(LinkSourceType.INSTAGRAM);
	}

	@Test
	void resolvesNaverBlogHosts() {
		assertThat(LinkSourceTypeResolver.fromUrl("https://blog.naver.com/user/123"))
				.isEqualTo(LinkSourceType.NAVER_BLOG);
		assertThat(LinkSourceTypeResolver.fromUrl("https://m.blog.naver.com/user/123"))
				.isEqualTo(LinkSourceType.NAVER_BLOG);
		assertThat(LinkSourceTypeResolver.fromUrl("https://naver.me/abc"))
				.isEqualTo(LinkSourceType.NAVER_BLOG);
	}

	@Test
	void resolvesYoutubeHosts() {
		assertThat(LinkSourceTypeResolver.fromUrl("https://www.youtube.com/watch?v=abc"))
				.isEqualTo(LinkSourceType.YOUTUBE);
		assertThat(LinkSourceTypeResolver.fromUrl("https://youtu.be/abc"))
				.isEqualTo(LinkSourceType.YOUTUBE);
	}

	@Test
	void resolvesGenericAndBlankUrls() {
		assertThat(LinkSourceTypeResolver.fromUrl("https://example.com/post/1"))
				.isEqualTo(LinkSourceType.GENERIC_WEB);
		assertThat(LinkSourceTypeResolver.fromUrl(null)).isNull();
		assertThat(LinkSourceTypeResolver.fromUrl("not a url")).isNull();
	}

	@Test
	void resolvesProcessingSourceTypeValues() {
		assertThat(LinkSourceTypeResolver.fromProcessingSourceType("INSTAGRAM"))
				.isEqualTo(LinkSourceType.INSTAGRAM);
		assertThat(LinkSourceTypeResolver.fromProcessingSourceType(" naver_blog "))
				.isEqualTo(LinkSourceType.NAVER_BLOG);
		assertThat(LinkSourceTypeResolver.fromProcessingSourceType("UNKNOWN")).isNull();
		assertThat(LinkSourceTypeResolver.fromProcessingSourceType(null)).isNull();
	}

	@Test
	void keepsCurrentSourceTypeWhenProcessingSourceTypeIsMissingOrUnknown() {
		assertThat(LinkSourceTypeResolver.resolveProcessingResult(
				LinkSourceType.INSTAGRAM,
				LinkSourceTypeResolver.fromProcessingSourceType(null)
		)).isEqualTo(LinkSourceType.INSTAGRAM);
		assertThat(LinkSourceTypeResolver.resolveProcessingResult(
				LinkSourceType.YOUTUBE,
				LinkSourceTypeResolver.fromProcessingSourceType("UNKNOWN")
		)).isEqualTo(LinkSourceType.YOUTUBE);
	}

	@Test
	void linkStoresOriginalAndNormalizedUrlAndInitialSourceTypeWhenRegistered() {
		Link link = Link.registerPending(
				"https://m.instagram.com/reels/abc/?igsh=abc",
				"https://www.instagram.com/reel/abc/"
		);

		assertThat(link.getOriginalUrl()).isEqualTo("https://m.instagram.com/reels/abc/?igsh=abc");
		assertThat(link.getNormalizedUrl()).isEqualTo("https://www.instagram.com/reel/abc/");
		assertThat(link.getLinkSourceType()).isEqualTo(LinkSourceType.INSTAGRAM);
	}
}
