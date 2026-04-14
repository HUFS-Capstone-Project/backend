package com.hufs.capstone.backend.external.processing;

import com.hufs.capstone.backend.external.processing.dto.CreateProcessingJobResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResultResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class ProcessingClientImpl implements ProcessingClient {

	private static final String JOBS_SEGMENT = "/api/v1/jobs";

	private final WebClient processingWebClient;

	public ProcessingClientImpl(
			@Qualifier(ProcessingWebClientConfig.PROCESSING_WEB_CLIENT) WebClient processingWebClient) {
		this.processingWebClient = processingWebClient;
	}

	@Override
	public CreateProcessingJobResponse createJob(String url, String roomId, String source) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("url", url);
		if (roomId != null) {
			body.put("room_id", roomId);
		}
		if (source != null) {
			body.put("source", source);
		}

		CreateProcessingJobResponse response = readBody(
				processingWebClient.post()
						.uri(JOBS_SEGMENT)
						.contentType(MediaType.APPLICATION_JSON)
						.bodyValue(body)
						.retrieve(),
				CreateProcessingJobResponse.class,
				"Processing job 생성에 실패했습니다.");
		if (response == null || response.jobId() == null || response.jobId().isBlank()) {
			throw new ProcessingClientException(
					"Processing job 생성 응답에 job 식별자가 없습니다.",
					HttpStatus.BAD_GATEWAY,
					"");
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
				"Processing job 조회에 실패했습니다.");
	}

	@Override
	public ProcessingJobResultResponse getJobResult(String jobId) {
		return readBody(
				processingWebClient.get()
						.uri(JOBS_SEGMENT + "/{jobId}/result", jobId)
						.retrieve(),
				ProcessingJobResultResponse.class,
				"Processing job 결과 조회에 실패했습니다.");
	}

	private static <T> T readBody(
			WebClient.ResponseSpec responseSpec,
			Class<T> bodyType,
			String errorMessage) {
		return responseSpec
				.onStatus(HttpStatusCode::isError, processingError(errorMessage))
				.bodyToMono(bodyType)
				.block();
	}

	private static Function<ClientResponse, Mono<? extends Throwable>> processingError(String message) {
		return response -> response.bodyToMono(String.class)
				.defaultIfEmpty("")
				.flatMap(body -> Mono.error(new ProcessingClientException(message, response.statusCode(), body)));
	}
}
