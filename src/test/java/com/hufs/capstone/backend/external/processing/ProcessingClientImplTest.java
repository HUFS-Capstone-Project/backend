package com.hufs.capstone.backend.external.processing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobCreateErrorResponse;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursJobCreateRequest;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursJobCreateResponse;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursJobStatus;
import com.hufs.capstone.backend.external.processing.dto.CreateProcessingJobResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResultResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResponse;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ProcessingClientImplTest {

	private static final String INTERNAL_API_KEY = "test-secret-key";

	private HttpServer server;
	private ExecutorService serverExecutor;

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
			server = null;
		}
		if (serverExecutor != null) {
			serverExecutor.shutdownNow();
			try {
				serverExecutor.awaitTermination(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			serverExecutor = null;
		}
	}

	@Test
	void createJobShouldSendApiKeyAndRequestBody() throws Exception {
		AtomicReference<String> observedPath = new AtomicReference<>();
		AtomicReference<String> observedApiKey = new AtomicReference<>();
		AtomicReference<String> observedBody = new AtomicReference<>();
		startServer(exchange -> {
			observedPath.set(exchange.getRequestURI().getPath());
			observedApiKey.set(exchange.getRequestHeaders().getFirst(ProcessingApiHeaders.INTERNAL_API_KEY));
			observedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			writeJson(exchange, HttpStatus.CREATED.value(), """
					{
					  "job_id":"job-1",
					  "status":"QUEUED",
					  "original_url":"https://example.com/p/1",
					  "canonical_url":"https://example.com/p/1",
					  "crawl_url":"https://example.com/p/1"
					}
					""");
		});

		CreateProcessingJobResponse response = client(3000).createJob(
				"https://example.com/p/1",
				"11111111-1111-1111-1111-111111111111",
				"WEB"
		);

		assertThat(response.jobId()).isEqualTo("job-1");
		assertThat(response.originalUrl()).isEqualTo("https://example.com/p/1");
		assertThat(response.canonicalUrl()).isEqualTo("https://example.com/p/1");
		assertThat(response.crawlUrl()).isEqualTo("https://example.com/p/1");
		assertThat(observedPath.get()).isEqualTo("/api/v1/jobs");
		assertThat(observedApiKey.get()).isEqualTo(INTERNAL_API_KEY);
		assertThat(observedBody.get()).contains("\"original_url\":\"https://example.com/p/1\"");
		assertThat(observedBody.get()).contains("\"room_id\":\"11111111-1111-1111-1111-111111111111\"");
		assertThat(observedBody.get()).doesNotContain("\"canonical_url\"");
		assertThat(observedBody.get()).doesNotContain("\"url\"");
		assertThat(observedBody.get()).doesNotContain("\"source\"");
	}

	@Test
	void getJobAndResultShouldUseDocumentedPaths() throws Exception {
		AtomicReference<String> jobPath = new AtomicReference<>();
		AtomicReference<String> resultPath = new AtomicReference<>();
		startServer(exchange -> {
			String path = exchange.getRequestURI().getPath();
			if (path.endsWith("/result")) {
				resultPath.set(path);
				writeJson(exchange, HttpStatus.OK.value(), """
						{"job_id":"job-1","status":"SUCCEEDED","content":{"content_text":"done"},"resolved_places":[]}
						""");
				return;
			}
			jobPath.set(path);
			writeJson(exchange, HttpStatus.OK.value(), """
					{"job_id":"job-1","status":"PROCESSING","room_id":"room-1"}
					""");
		});

		ProcessingJobResponse job = client(3000).getJob("job-1");
		ProcessingJobResultResponse result = client(3000).getJobResult("job-1");

		assertThat(job.status()).isEqualTo("PROCESSING");
		assertThat(result.content().contentText()).isEqualTo("done");
		assertThat(jobPath.get()).isEqualTo("/api/v1/jobs/job-1");
		assertThat(resultPath.get()).isEqualTo("/api/v1/jobs/job-1/result");
	}

	@Test
	void resultResponseShouldReadContentLinkStatsAndResolvedPlaces() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

		ProcessingJobResultResponse result = objectMapper.readValue("""
				{
				  "job_id":"job-1",
				  "status":"SUCCEEDED",
				  "original_url":"https://www.instagram.com/reels/abc123/?utm_source=test",
				  "canonical_url":"https://www.instagram.com/reel/abc123/",
				  "crawl_url":"https://www.instagram.com/reel/abc123/",
				  "content":{
				    "source_type":"INSTAGRAM",
				    "content_text":"done",
				    "title":null,
				    "description":"desc",
				    "thumbnail_url":null,
				    "links":[],
				    "extraction_method":"INSTAGRAM_OG_META"
				  },
				  "link_stats":{
				    "like_count":123,
				    "comment_count":45,
				    "posted_at":"April 2, 2026"
				  },
				  "resolved_places":[
				    {
				      "kakao_place_id":"123",
				      "place_name":"Coffee Mansion",
				      "address":"서울 중구 세종대로 1",
				      "road_address":"서울 중구 세종대로 2",
				      "longitude":127.060138952594,
				      "latitude":37.5959759766929,
				      "category_name":"음식점 > 카페 > 커피전문점",
				      "category_group_code":"CE7",
				      "place_url":"https://place.map.kakao.com/123",
				      "phone":"02-0000-0000"
				    }
				  ],
				  "error_code":null,
				  "error_message":null,
				  "retryable":false
				}
				""", ProcessingJobResultResponse.class);

		assertThat(result.originalUrl()).isEqualTo("https://www.instagram.com/reels/abc123/?utm_source=test");
		assertThat(result.canonicalUrl()).isEqualTo("https://www.instagram.com/reel/abc123/");
		assertThat(result.crawlUrl()).isEqualTo("https://www.instagram.com/reel/abc123/");
		assertThat(result.content().contentText()).isEqualTo("done");
		assertThat(result.linkStats().likeCount()).isEqualTo(123);
		assertThat(result.linkStats().commentCount()).isEqualTo(45);
		assertThat(result.linkStats().postedAt()).isEqualTo("April 2, 2026");
		assertThat(result.resolvedPlaces()).hasSize(1);
		assertThat(result.resolvedPlaces().get(0).kakaoPlaceId()).isEqualTo("123");
		assertThat(result.resolvedPlaces().get(0).longitude()).isEqualByComparingTo("127.060138952594");
		assertThat(result.resolvedPlaces().get(0).latitude()).isEqualByComparingTo("37.5959759766929");
		assertThat(result.resolvedPlaces().get(0).categoryGroupCode()).isEqualTo("CE7");
		assertThat(result.retryable()).isFalse();
	}

	@Test
	void createJobErrorResponseShouldReadFastApiDetailWrapper() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();

		ProcessingJobCreateErrorResponse response = objectMapper.readValue("""
				{
				  "detail": {
				    "code": "INSTAGRAM_RATE_LIMITED",
				    "message": "Instagram cooldown active.",
				    "retryable": true,
				    "cooldown_seconds": 120
				  }
				}
				""", ProcessingJobCreateErrorResponse.class);

		assertThat(response.detail().code()).isEqualTo(ProcessingErrorCodes.INSTAGRAM_RATE_LIMITED);
		assertThat(response.detail().retryable()).isTrue();
		assertThat(response.detail().cooldownSeconds()).isEqualTo(120);
	}

	@Test
	void businessHoursDtosShouldUseSnakeCaseContract() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

		String requestJson = objectMapper.writeValueAsString(new BusinessHoursJobCreateRequest(
				"123",
				"https://place.map.kakao.com/123",
				"Coffee Mansion"
		));
		BusinessHoursJobCreateResponse response = objectMapper.readValue("""
				{
				  "cache_hit":false,
				  "job":{"job_id":"job-1","status":"SUCCESS","error_code":null,"error_message":null},
				  "place":{
				    "kakao_place_id":"123",
				    "place_name":"Coffee Mansion",
				    "place_url":"https://place.map.kakao.com/123",
				    "business_hours_status":"SUCCESS",
				    "business_hours":{"daily_hours":[]},
				    "business_hours_fetched_at":"2026-05-07T10:00:03Z",
				    "business_hours_expires_at":"2026-05-21T10:00:03Z",
				    "error_code":null,
				    "error_message":null
				  }
				}
				""", BusinessHoursJobCreateResponse.class);

		assertThat(requestJson).contains("\"kakao_place_id\":\"123\"");
		assertThat(requestJson).contains("\"place_url\":\"https://place.map.kakao.com/123\"");
		assertThat(requestJson).contains("\"place_name\":\"Coffee Mansion\"");
		assertThat(response.cacheHit()).isFalse();
		assertThat(response.job().jobId()).isEqualTo("job-1");
		assertThat(response.job().status()).isEqualTo(BusinessHoursJobStatus.SUCCEEDED);
		assertThat(response.place().businessHoursStatus()).isEqualTo(BusinessHoursStatus.SUCCEEDED);
		assertThat(response.place().businessHours().path("daily_hours").isArray()).isTrue();
	}

	@Test
	void shouldClassifyClientAndServerErrorsWithoutLeakingApiKey() throws Exception {
		startServer(exchange -> writeJson(exchange, HttpStatus.UNPROCESSABLE_ENTITY.value(), """
				{"detail":{"code":"INVALID_URL","message":"Invalid URL."}}
				"""));

		assertThatThrownBy(() -> client(3000).createJob("bad", "room-1", null))
				.isInstanceOfSatisfying(ProcessingClientException.class, exception -> {
					assertThat(exception.getErrorType()).isEqualTo(ProcessingClientErrorType.CLIENT_ERROR);
					assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
					assertThat(exception.getProcessingErrorCode()).isEqualTo("INVALID_URL");
					assertThat(exception.getMessage()).doesNotContain(INTERNAL_API_KEY);
				});
	}

	@Test
	void shouldClassifyResultNotReadyConflict() throws Exception {
		startServer(exchange -> writeJson(exchange, HttpStatus.CONFLICT.value(), """
				{"detail":{"code":"RESULT_NOT_READY","message":"Job is currently PROCESSING."}}
				"""));

		assertThatThrownBy(() -> client(3000).getJobResult("job-1"))
				.isInstanceOfSatisfying(ProcessingClientException.class, exception -> {
					assertThat(exception.getErrorType()).isEqualTo(ProcessingClientErrorType.CLIENT_ERROR);
					assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(exception.getProcessingErrorCode()).isEqualTo("RESULT_NOT_READY");
				});
	}

	@Test
	void createJobShouldThrowInstagramRateLimitedExceptionOnRateLimitDetailWrapper() throws Exception {
		startServer(exchange -> writeJson(exchange, HttpStatus.TOO_MANY_REQUESTS.value(), """
				{
				  "detail": {
				    "code": "INSTAGRAM_RATE_LIMITED",
				    "message": "Instagram cooldown active.",
				    "retryable": true,
				    "cooldown_seconds": 90
				  }
				}
				"""));

		assertThatThrownBy(() -> client(3000).createJob("https://instagram.com/p/abc", "room-1", null))
				.isInstanceOfSatisfying(InstagramRateLimitedException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
					assertThat(exception.getProcessingErrorCode()).isEqualTo(ProcessingErrorCodes.INSTAGRAM_RATE_LIMITED);
					assertThat(exception.isRetryable()).isTrue();
					assertThat(exception.getCooldownSeconds()).isEqualTo(90);
				});
	}

	@Test
	void shouldClassifyServerError() throws Exception {
		startServer(exchange -> writeJson(exchange, HttpStatus.BAD_GATEWAY.value(), "{}"));

		assertThatThrownBy(() -> client(3000).getJob("job-1"))
				.isInstanceOfSatisfying(ProcessingClientException.class, exception ->
						assertThat(exception.getErrorType()).isEqualTo(ProcessingClientErrorType.SERVER_ERROR));
	}

	@Test
	void shouldClassifyMalformedResponse() throws Exception {
		startServer(exchange -> writeJson(exchange, HttpStatus.OK.value(), "{"));

		assertThatThrownBy(() -> client(3000).getJob("job-1"))
				.isInstanceOfSatisfying(ProcessingClientException.class, exception ->
						assertThat(exception.getErrorType()).isEqualTo(ProcessingClientErrorType.MALFORMED_RESPONSE));
	}

	@Test
	void shouldClassifyTimeout() throws Exception {
		// 타임아웃 후에는 응답을 보내지 않음: 지연된 write가 다음 테스트 클라이언트에서 SocketException 등을 유발할 수 있음
		startServer(exchange -> {
			try {
				new CountDownLatch(1).await();
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		});

		assertThatThrownBy(() -> client(50).getJob("job-1"))
				.isInstanceOfSatisfying(ProcessingClientException.class, exception ->
						assertThat(exception.getErrorType()).isEqualTo(ProcessingClientErrorType.TIMEOUT));
	}

	private ProcessingClientImpl client(int readTimeoutMs) {
		ProcessingProperties properties = new ProcessingProperties(
				"http://127.0.0.1:" + server.getAddress().getPort(),
				INTERNAL_API_KEY,
				1000,
				readTimeoutMs
		);
		return new ProcessingClientImpl(
				new ProcessingWebClientConfig().processingWebClient(properties),
				new ObjectMapper()
		);
	}

	private void startServer(ExchangeHandler handler) throws IOException {
		serverExecutor = Executors.newCachedThreadPool();
		server = createHttpServer();
		server.createContext("/", exchange -> handler.handle(exchange));
		server.setExecutor(serverExecutor);
		server.start();
	}

	private static HttpServer createHttpServer() throws IOException {
		SocketException lastException = null;
		for (int attempt = 0; attempt < 3; attempt++) {
			try {
				return HttpServer.create(new InetSocketAddress(0), 0);
			} catch (SocketException ex) {
				lastException = ex;
			}
		}
		throw new IOException("Failed to bind test HTTP server.", lastException);
	}

	private static void writeJson(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream responseBody = exchange.getResponseBody()) {
			responseBody.write(bytes);
		}
	}

	@FunctionalInterface
	private interface ExchangeHandler {

		void handle(HttpExchange exchange) throws IOException;
	}
}
