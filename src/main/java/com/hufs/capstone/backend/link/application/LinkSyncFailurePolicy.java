package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.external.processing.ProcessingClientException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class LinkSyncFailurePolicy {

	private static final int RESULT_NOT_READY_STATUS = HttpStatus.CONFLICT.value();
	private static final String RESULT_NOT_READY_CODE = "RESULT_NOT_READY";

	public boolean isResultNotReady(ProcessingClientException exception) {
		return exception.hasStatus(RESULT_NOT_READY_STATUS)
				&& (exception.getProcessingErrorCode() == null
				|| RESULT_NOT_READY_CODE.equals(exception.getProcessingErrorCode()));
	}
}
