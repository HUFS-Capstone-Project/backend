package com.hufs.capstone.backend.auth.application;

public interface ExpiringCodeStore<T> {

	String issue(T value);

	T consume(String code);
}



