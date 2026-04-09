package com.hufs.capstone.backend.auth.domain.vo;

public enum AuthClientType {
	WEB,
	APP;

	public boolean isWeb() {
		return this == WEB;
	}
}

