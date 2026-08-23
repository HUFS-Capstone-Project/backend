package com.hufs.capstone.backend.external.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.external.processing.dto.CreateProcessingJobResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobCreateErrorResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResultResponse;
import io.netty.handler.timeout.ReadTimeoutException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;
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
public class ProcessingClientImpl implements ProcessingClient {

	private static final String JOBS_SEGMENT = "/jobs";

	private final WebClient processingWebClient;
	private final ObjectMapper objectMapper;

	public ProcessingClientImpl(
			@Qualifier(ProcessingWebClientConfig.PROCESSING_WEB_CLIENT) WebClient processingWebClient,
			ObjectMapper objectMapper) {
		this.processingWebClient = processingWebClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public CreateProcessingJobResponse createJob(String originalUrl, String roomId) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("original_url", originalUrl);
		if (roomId != null) {
			body.put("room_id", roomId);
		}

		CreateProcessingJobResponse response = readBody(
				processingWebClient.post()
						.uri(JOBS_SEGMENT)
						.contentType(MediaType.APPLICATION_JSON)
						.bodyValue(body)
						.retrieve(),
				CreateProcessingJobResponse.class,
				"Processing job 생성에 실패했습니다.",
				true
		);
		if (response == null || response.jobId() == null || response.jobId().isBlank()) {
			throw new ProcessingClientException(
					"Processing job 생성 응답에 job_id가 없습니다.",
					HttpStatus.BAD_GATEWAY,
					""
			);
		}
		return response;
	}

	@Override
	public ProcessingJobResponse getJob(String jobId) {
		return readBody(
				processingWebClient.get()
						.uri(JOBS_SEGMENT + "/{jobId}", jobId)
						.retrieve(),
				ProcessingJobResponse.class,
				"Processing job 조회에 실패했습니다."
		);
	}

	@Override
	public ProcessingJobResultResponse getJobResult(String jobId) {
		return readBody(
				processingWebClient.get()
						.uri(JOBS_SEGMENT + "/{jobId}/result", jobId)
						.retrieve(),
				ProcessingJobResultResponse.class,
				"Processing job 결과 조회에 실패했습니다.",
				false
		);
	}

	private <T> T readBody(
			WebClient.ResponseSpec responseSpec,
			Class<T> bodyType,
			String errorMessage) {
		return readBody(responseSpec, bodyType, errorMessage, false);
	}

	private <T> T readBody(
			WebClient.ResponseSpec responseSpec,
			Class<T> bodyType,
			String errorMessage,
			boolean translateInstagramRateLimit) {
		try {
			return responseSpec
					.onStatus(HttpStatusCode::isError, processingError(errorMessage, translateInstagramRateLimit))
					.bodyToMono(bodyType)
					.block();
		} catch (ProcessingClientException ex) {
			throw ex;
		} catch (WebClientRequestException ex) {
			throw transportException(errorMessage, ex);
		} catch (DecodingException ex) {
			throw new ProcessingClientException(
					errorMessage,
					HttpStatus.BAD_GATEWAY,
					"",
					ProcessingClientErrorType.MALFORMED_RESPONSE,
					null,
					ex
			);
		}
	}

	private Function<ClientResponse, Mono<? extends Throwable>> processingError(String message, boolean translateInstagramRateLimit) {
		return response -> response.bodyToMono(String.class)
				.defaultIfEmpty("")
				.flatMap(body -> Mono.error(httpException(message, response.statusCode(), body, translateInstagramRateLimit)));
	}

	private ProcessingClientException httpException(
			String message,
			HttpStatusCode status,
			String body,
			boolean translateInstagramRateLimit
	) {
		InstagramRateLimitedException instagramRateLimited =
				translateInstagramRateLimit ? instagramRateLimitedExceptionOrNull(message, status, body) : null;
		if (instagramRateLimited != null) {
			return instagramRateLimited;
		}
		return new ProcessingClientException(
				message,
				status,
				body,
				classifyHttp(status),
				extractProcessingErrorCode(body),
				null
		);
	}

	private InstagramRateLimitedException instagramRateLimitedExceptionOrNull(
			String fallbackMessage,
			HttpStatusCode status,
			String body
	) {
		if (status == null || status.value() != HttpStatus.TOO_MANY_REQUESTS.value()) {
			return null;
		}
		ProcessingJobCreateErrorResponse.Detail detail = parseCreateJobErrorDetail(body);
		if (detail == null || !ProcessingErrorCodes.INSTAGRAM_RATE_LIMITED.equals(detail.code())) {
			return null;
		}
		String message = detail.message() == null || detail.message().isBlank() ? fallbackMessage : detail.message();
		return new InstagramRateLimitedException(
				message,
				body,
				Boolean.TRUE.equals(detail.retryable()),
				detail.cooldownSeconds()
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

	private ProcessingJobCreateErrorResponse.Detail parseCreateJobErrorDetail(String body) {
		if (body == null || body.isBlank()) {
			return null;
		}
		try {
			ProcessingJobCreateErrorResponse response =
					objectMapper.readValue(body, ProcessingJobCreateErrorResponse.class);
			return response.detail();
		} catch (Exception ex) {
			return null;
		}
	}
}
