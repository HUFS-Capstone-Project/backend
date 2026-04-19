package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.external.processing.ProcessingClientException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class LinkSyncFailurePolicy {

	private static final int RESULT_NOT_READY_STATUS = HttpStatus.NOT_FOUND.value();

	public boolean isResultNotReady(ProcessingClientException exception) {
		return exception.getStatus() != null && exception.getStatus().value() == RESULT_NOT_READY_STATUS;
	}
}
