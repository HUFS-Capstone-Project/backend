package com.hufs.capstone.backend.external.kakao;

import com.hufs.capstone.backend.external.kakao.dto.KakaoKeywordSearchResponse;
import com.hufs.capstone.backend.external.kakao.dto.KakaoKeywordSearchResponse.Document;
import com.hufs.capstone.backend.place.application.dto.ExternalPlaceCandidateSearchQuery;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import io.netty.handler.timeout.ReadTimeoutException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@Component
public class KakaoLocalClientImpl implements KakaoLocalClient {

	private static final String KEYWORD_SEARCH_PATH = "/v2/local/search/keyword.json";

	private final WebClient kakaoLocalWebClient;

	public KakaoLocalClientImpl(
			@Qualifier(KakaoLocalWebClientConfig.KAKAO_LOCAL_WEB_CLIENT) WebClient kakaoLocalWebClient
	) {
		this.kakaoLocalWebClient = kakaoLocalWebClient;
	}

	@Override
	public List<PlaceSnapshot> searchByKeyword(ExternalPlaceCandidateSearchQuery query) {
		KakaoKeywordSearchResponse response;
		try {
			response = kakaoLocalWebClient.get()
					.uri(uriBuilder -> {
						uriBuilder.path(KEYWORD_SEARCH_PATH)
								.queryParam("query", query.kakaoQuery())
								.queryParam("size", query.limit());
						if (query.categoryGroupCode() != null) {
							uriBuilder.queryParam("category_group_code", query.categoryGroupCode());
						}
						return uriBuilder.build();
					})
					.retrieve()
					.onStatus(HttpStatusCode::isError, kakaoError())
					.bodyToMono(KakaoKeywordSearchResponse.class)
					.block();
		} catch (KakaoLocalClientException ex) {
			throw ex;
		} catch (WebClientRequestException ex) {
			throw transportException(ex);
		} catch (DecodingException ex) {
			throw new KakaoLocalClientException("Kakao Local response is malformed.", null, "", ex);
		}
		if (response == null || response.documents() == null) {
			return List.of();
		}
		return response.documents().stream()
				.map(this::toSnapshot)
				.toList();
	}

	private Function<ClientResponse, Mono<? extends Throwable>> kakaoError() {
		return response -> response.bodyToMono(String.class)
				.defaultIfEmpty("")
				.flatMap(body -> Mono.error(new KakaoLocalClientException(
						"Kakao Local API call failed.",
						response.statusCode(),
						body,
						null
				)));
	}

	private PlaceSnapshot toSnapshot(Document document) {
		return PlaceSnapshot.kakao(
				trimToNull(document.id()),
				trimToNull(document.placeName()),
				trimToNull(document.categoryName()),
				trimToNull(document.categoryGroupCode()),
				trimToNull(document.categoryGroupName()),
				trimToNull(document.phone()),
				trimToNull(document.addressName()),
				trimToNull(document.roadAddressName()),
				parseDecimal(document.x()),
				parseDecimal(document.y()),
				trimToNull(document.placeUrl()),
				null,
				null,
				null,
				null
		);
	}

	private static KakaoLocalClientException transportException(WebClientRequestException ex) {
		String message = isTimeout(ex) ? "Kakao Local API request timed out." : "Kakao Local API request failed.";
		return new KakaoLocalClientException(message, null, "", ex);
	}

	private static boolean isTimeout(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof SocketTimeoutException
					|| current instanceof TimeoutException
					|| current instanceof ReadTimeoutException) {
				return true;
			}
			if (current instanceof ConnectException) {
				return false;
			}
			current = current.getCause();
		}
		return false;
	}

	private static BigDecimal parseDecimal(String value) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			return null;
		}
		try {
			return new BigDecimal(trimmed);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
