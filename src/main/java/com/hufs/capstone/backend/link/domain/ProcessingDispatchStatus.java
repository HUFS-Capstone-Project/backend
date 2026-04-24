package com.hufs.capstone.backend.link.domain;

public enum ProcessingDispatchStatus {

	PENDING,
	DISPATCHING,
	DISPATCHED,
	DISPATCH_FAILED;

	public boolean canPoll() {
		return this == DISPATCHED;
	}
}
