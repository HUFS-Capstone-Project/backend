package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateProcessingJobResponse(
		@JsonProperty("job_id")
		String jobId,
		String status,
		@JsonProperty("original_url")
		String originalUrl,
		@JsonProperty("canonical_url")
		String canonicalUrl,
		@JsonProperty("crawl_url")
		String crawlUrl,
		String source,
		@JsonProperty("created_at")
		OffsetDateTime createdAt
) {

	public CreateProcessingJobResponse(String jobId) {
		this(jobId, null, null, null, null, null, null);
	}
}
