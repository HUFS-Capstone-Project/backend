package com.hufs.capstone.backend.global.exception;

import com.hufs.capstone.backend.external.processing.ProcessingClientException;
import com.hufs.capstone.backend.global.response.FieldErrorDetail;
import com.hufs.capstone.backend.global.response.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toFieldErrorDetail)
				.toList();
		ProblemDetail body = ProblemDetailFactory.create(
				ErrorCode.E400_VALIDATION, "요청 본문 검증에 실패했습니다.", details, requestUri(request));
		return ResponseEntity.status(body.getStatus()).body(body);
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ProblemDetail> handleBindException(BindException ex, HttpServletRequest request) {
		List<FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toFieldErrorDetail)
				.toList();
		ProblemDetail body = ProblemDetailFactory.create(
				ErrorCode.E400_BIND, "요청 바인딩에 실패했습니다.", details, requestUri(request));
		return ResponseEntity.status(body.getStatus()).body(body);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ProblemDetail> handleConstraintViolation(
			ConstraintViolationException ex, HttpServletRequest request) {
		List<FieldErrorDetail> details = ex.getConstraintViolations().stream()
				.map(this::toFieldErrorDetail)
				.toList();
		ProblemDetail body = ProblemDetailFactory.create(
				ErrorCode.E400_CONSTRAINT, "제약 조건 검증에 실패했습니다.", details, requestUri(request));
		return ResponseEntity.status(body.getStatus()).body(body);
	}

	private FieldErrorDetail toFieldErrorDetail(FieldError fe) {
		return new FieldErrorDetail(
				fe.getField(),
				fe.getDefaultMessage(),
				rejectedValueToString(fe.getRejectedValue()));
	}

	private FieldErrorDetail toFieldErrorDetail(ConstraintViolation<?> v) {
		String path = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "unknown";
		return new FieldErrorDetail(path, v.getMessage(), rejectedValueToString(v.getInvalidValue()));
	}

	private static String rejectedValueToString(Object value) {
		if (value == null) {
			return null;
		}
		return String.valueOf(value);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
		log.debug("IllegalArgument: {}", ex.getMessage());
		ProblemDetail body = ProblemDetailFactory.create(ErrorCode.E400_ILLEGAL_ARGUMENT, ex.getMessage(), null, requestUri(request));
		return ResponseEntity.status(body.getStatus()).body(body);
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex, HttpServletRequest request) {
		log.info("BusinessException [{}]: {}", ex.getErrorCode(), ex.getMessage());
		ProblemDetail body = ProblemDetailFactory.create(ex.getErrorCode(), ex.getMessage(), null, requestUri(request));
		return ResponseEntity.status(body.getStatus()).body(body);
	}

	@ExceptionHandler(ProcessingClientException.class)
	public ResponseEntity<ProblemDetail> handleProcessing(ProcessingClientException ex, HttpServletRequest request) {
		log.warn("Processing(FastAPI private) 연동 실패: status={}", ex.getStatus(), ex);
		ProblemDetail body = ProblemDetailFactory.create(ErrorCode.E502_EXTERNAL_API, ex.getMessage(), null, requestUri(request));
		return ResponseEntity.status(body.getStatus()).body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleAny(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception", ex);
		ProblemDetail body = ProblemDetailFactory.create(ErrorCode.E500_INTERNAL, requestUri(request));
		return ResponseEntity.status(body.getStatus()).body(body);
	}

	private static URI requestUri(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		return URI.create(request.getRequestURI());
	}
}
