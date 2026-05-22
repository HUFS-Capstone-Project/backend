package com.hufs.capstone.backend.link.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LinkSourceTypeTest {

	@Test
	void resolvesInstagramHosts() {
		assertThat(LinkSourceType.fromUrl("https://www.instagram.com/p/abc/"))
				.isEqualTo(LinkSourceType.INSTAGRAM);
		assertThat(LinkSourceType.fromUrl("https://instagr.am/p/abc/"))
				.isEqualTo(LinkSourceType.INSTAGRAM);
	}

	@Test
	void resolvesNaverBlogHosts() {
		assertThat(LinkSourceType.fromUrl("https://blog.naver.com/user/123"))
				.isEqualTo(LinkSourceType.NAVER_BLOG);
		assertThat(LinkSourceType.fromUrl("https://m.blog.naver.com/user/123"))
				.isEqualTo(LinkSourceType.NAVER_BLOG);
		assertThat(LinkSourceType.fromUrl("https://naver.me/abc"))
				.isEqualTo(LinkSourceType.NAVER_BLOG);
	}

	@Test
	void resolvesYoutubeHosts() {
		assertThat(LinkSourceType.fromUrl("https://www.youtube.com/watch?v=abc"))
				.isEqualTo(LinkSourceType.YOUTUBE);
		assertThat(LinkSourceType.fromUrl("https://youtu.be/abc"))
				.isEqualTo(LinkSourceType.YOUTUBE);
	}

	@Test
	void resolvesGenericAndBlankUrls() {
		assertThat(LinkSourceType.fromUrl("https://example.com/post/1"))
				.isEqualTo(LinkSourceType.GENERIC_WEB);
		assertThat(LinkSourceType.fromUrl(null)).isNull();
		assertThat(LinkSourceType.fromUrl("not a url")).isNull();
	}
}
