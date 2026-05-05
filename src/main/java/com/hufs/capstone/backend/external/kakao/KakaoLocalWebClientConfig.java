package com.hufs.capstone.backend.external.kakao;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class KakaoLocalWebClientConfig {

	public static final String KAKAO_LOCAL_WEB_CLIENT = "kakaoLocalWebClient";

	@Bean
	@Qualifier(KAKAO_LOCAL_WEB_CLIENT)
	public WebClient kakaoLocalWebClient(KakaoLocalProperties props) {
		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.connectTimeoutMs())
				.responseTimeout(Duration.ofMillis(props.readTimeoutMs()));
		return WebClient.builder()
				.baseUrl(trimTrailingSlash(props.baseUrl()))
				.defaultHeader("Authorization", "KakaoAK " + props.restApiKey())
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}

	private static String trimTrailingSlash(String baseUrl) {
		return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}
}
