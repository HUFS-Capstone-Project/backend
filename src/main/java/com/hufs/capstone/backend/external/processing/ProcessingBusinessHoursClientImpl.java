package com.hufs.capstone.backend.external.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursJobCreateRequest;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursJobCreateResponse;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursJobLookupResponse;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursPlaceResponse;
import io.netty.handler.timeout.ReadTimeoutException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@Component
public class ProcessingBusinessHoursClientImpl implements ProcessingBusinessHoursClient {

	private static final String BUSINESS_HOURS_JOBS_SEGMENT = "/business-hours/jobs";
	private static final String BUSINESS_HOURS_PLACES_SEGMENT = "/business-hours/places";

	private final WebClient processingWebClient;
	private final ObjectMapper objectMapper;

	public ProcessingBusinessHoursClientImpl(
			@Qualifier(ProcessingWebClientConfig.PROCESSING_WEB_CLIENT) WebClient processingWebClient,
			ObjectMapper objectMapper) {
		this.processingWebClient = processingWebClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public BusinessHoursJobCreateResponse createJob(BusinessHoursJobCreateRequest request) {
		return readBody(
				processingWebClient.post()
						.uri(BUSINESS_HOURS_JOBS_SEGMENT)
						.contentType(MediaType.APPLICATION_JSON)
						.bodyValue(request)
						.retrieve(),
				BusinessHoursJobCreateResponse.class,
				"Business hours job create request failed."
		);
	}

	@Override
	public BusinessHoursJobLookupResponse getJob(String jobId) {
		return readBody(
				processingWebClient.get()
						.uri(BUSINESS_HOURS_JOBS_SEGMENT + "/{jobId}", jobId)
						.retrieve(),
				BusinessHoursJobLookupResponse.class,
				"Business hours job lookup failed."
		);
	}

	@Override
	public Optional<BusinessHoursPlaceResponse> getPlace(String kakaoPlaceId) {
		try {
			return processingWebClient.get()
					.uri(BUSINESS_HOURS_PLACES_SEGMENT + "/{kakaoPlaceId}", kakaoPlaceId)
					.exchangeToMono(response -> {
						if (response.statusCode().value() == HttpStatus.NOT_FOUND.value()) {
							return Mono.just(Optional.<BusinessHoursPlaceResponse>empty());
						}
						if (response.statusCode().isError()) {
							return processingError("Business hours place lookup failed.")
									.apply(response)
									.flatMap(Mono::error);
						}
						return response.bodyToMono(BusinessHoursPlaceResponse.class).map(Optional::of);
					})
					.block();
		} catch (ProcessingClientException ex) {
			throw ex;
		} catch (WebClientRequestException ex) {
			throw transportException("Business hours place lookup failed.", ex);
		} catch (DecodingException ex) {
			throw malformedException("Business hours place lookup failed.", ex);
		}
	}

	private <T> T readBody(
			WebClient.ResponseSpec responseSpec,
			Class<T> bodyType,
			String errorMessage) {
		try {
			return responseSpec
					.onStatus(HttpStatusCode::isError, processingError(errorMessage))
					.bodyToMono(bodyType)
					.block();
		} catch (ProcessingClientException ex) {
			throw ex;
		} catch (WebClientRequestException ex) {
			throw transportException(errorMessage, ex);
		} catch (DecodingException ex) {
			throw malformedException(errorMessage, ex);
		}
	}

	private Function<ClientResponse, Mono<? extends Throwable>> processingError(String message) {
		return response -> response.bodyToMono(String.class)
				.defaultIfEmpty("")
				.flatMap(body -> Mono.error(httpException(message, response.statusCode(), body)));
	}

	private ProcessingClientException httpException(String message, HttpStatusCode status, String body) {
		return new ProcessingClientException(
				message,
				status,
				body,
				classifyHttp(status),
				extractProcessingErrorCode(body),
				null
		);
	}

	private static ProcessingClientErrorType classifyHttp(HttpStatusCode status) {
		if (status.is5xxServerError()) {
			return ProcessingClientErrorType.SERVER_ERROR;
		}
		return ProcessingClientErrorType.CLIENT_ERROR;
	}

	private static ProcessingClientException transportException(String message, WebClientRequestException ex) {
		return new ProcessingClientException(
				message,
				null,
				"",
				isTimeout(ex) ? ProcessingClientErrorType.TIMEOUT : ProcessingClientErrorType.NETWORK,
				null,
				ex
		);
	}

	private static ProcessingClientException malformedException(String message, DecodingException ex) {
		return new ProcessingClientException(
				message,
				HttpStatus.BAD_GATEWAY,
				"",
				ProcessingClientErrorType.MALFORMED_RESPONSE,
				null,
				ex
		);
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

	private String extractProcessingErrorCode(String body) {
		if (body == null || body.isBlank()) {
			return null;
		}
		try {
			JsonNode root = objectMapper.readTree(body);
			JsonNode code = root.path("detail").path("code");
			return code.isTextual() ? code.asText() : null;
		} catch (Exception ex) {
			return null;
		}
	}
}
