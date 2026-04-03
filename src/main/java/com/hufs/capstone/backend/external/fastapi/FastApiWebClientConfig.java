package com.hufs.capstone.backend.external.fastapi;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class FastApiWebClientConfig {

	public static final String FAST_API_WEB_CLIENT = "fastApiWebClient";

	@Bean
	@Qualifier(FAST_API_WEB_CLIENT)
	public WebClient fastApiWebClient(FastApiProperties props) {
		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.connectTimeoutMs())
				.responseTimeout(Duration.ofMillis(props.readTimeoutMs()));

		return WebClient.builder()
				.baseUrl(props.baseUrl())
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}
}
