package com.hufs.capstone.backend.external.processing;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class ProcessingWebClientConfig {

	public static final String PROCESSING_WEB_CLIENT = "processingWebClient";

	@Bean
	@Qualifier(PROCESSING_WEB_CLIENT)
	public WebClient processingWebClient(ProcessingProperties props) {
		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.connectTimeoutMs())
				.responseTimeout(Duration.ofMillis(props.readTimeoutMs()));

		ExchangeFilterFunction internalAuth = (request, next) -> next.exchange(
				ClientRequest.from(request)
						.header(ProcessingApiHeaders.INTERNAL_API_KEY, props.internalApiKey())
						.build()
		);

		return WebClient.builder()
				.baseUrl(props.baseUrl())
				.filter(internalAuth)
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}
}
