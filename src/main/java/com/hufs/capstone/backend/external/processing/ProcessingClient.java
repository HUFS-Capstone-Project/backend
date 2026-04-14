package com.hufs.capstone.backend.external.processing;

import com.hufs.capstone.backend.external.processing.dto.CreateProcessingJobResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResultResponse;

public interface ProcessingClient {

	CreateProcessingJobResponse createJob(String url, String roomId, String source);

	ProcessingJobResponse getJob(String jobId);

	ProcessingJobResultResponse getJobResult(String jobId);
}
