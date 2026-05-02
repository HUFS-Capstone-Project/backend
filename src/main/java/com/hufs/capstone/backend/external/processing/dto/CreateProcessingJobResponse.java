package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateProcessingJobResponse(
		@JsonProperty("job_id")
		String jobId,
		String status,
		@JsonProperty("source_url")
		String sourceUrl,
		String source,
		@JsonProperty("created_at")
		OffsetDateTime createdAt
) {

	public CreateProcessingJobResponse(String jobId) {
		this(jobId, null, null, null, null);
	}
}
