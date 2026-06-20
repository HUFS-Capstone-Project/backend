package com.hufs.capstone.backend.global.exception;

import com.hufs.capstone.backend.external.kakao.KakaoLocalClientException;
import com.hufs.capstone.backend.external.processing.ProcessingClientException;
import com.hufs.capstone.backend.global.response.FieldErrorDetail;
import com.hufs.capstone.backend.global.response.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
				ErrorCode.E400_VALIDATION, ErrorCode.E400_VALIDATION.getDefaultMessage(), details, requestUri(request));
		return ResponseEntity.status(body.getStatus()).body(body);
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ProblemDetail> handleBindException(BindException ex, HttpServletRequest request) {
		List<FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toFieldErrorDetail)
				.toList();
		ProblemDetail body = ProblemDetailFactory.create(
				ErrorCode.E400_BIND, ErrorCode.E400_BIND.getDefaultMessage(), details, requestUri(request));
		return ResponseEntity.status(body.getStatus()).body(body);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ProblemDetail> handleConstraintViolation(
			ConstraintViolationException ex, HttpServletRequest request) {
		List<FieldErrorDetail> details = ex.getConstraintViolations().stream()
				.map(this::toFieldErrorDetail)
				.toList();
		ProblemDetail body = ProblemDetailFactory.create(
				ErrorCode.E400_CONSTRAINT, ErrorCode.E400_CONSTRAINT.getDefaultMessage(), details, requestUri(request));
		return ResponseEntity.status(body.getStatus()).body(body);
	}

	@ExceptionHandler(FieldValidationException.class)
	public ResponseEntity<ProblemDetail> handleFieldValidation(
			FieldValidationException ex, HttpServletRequest request) {
		ProblemDetail body = ProblemDetailFactory.create(
				ErrorCode.E400_VALIDATION, ex.getMessage(), ex.getFieldErrors(), requestUri(request));
		return ResponseEntity.status(body.getStatus()).body(body);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(
			MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		String field = ex.getName() != null ? ex.getName() : "unknown";
		FieldErrorDetail detail = FieldErrorDetail.of(field, "요청값 형식이 올바르지 않습니다.", ex.getValue());
		ProblemDetail body = ProblemDetailFactory.create(
				ErrorCode.E400_VALIDATION,
				ErrorCode.E400_VALIDATION.getDefaultMessage(),
				List.of(detail),
				requestUri(request));
		return ResponseEntity.status(body.getStatus()).body(body);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ProblemDetail> handleMissingServletRequestParameter(
			MissingServletRequestParameterException ex, HttpServletRequest request) {
		FieldErrorDetail detail = FieldErrorDetail.of(ex.getParameterName(), "필수 요청 파라미터입니다.");
		ProblemDetail body = ProblemDetailFactory.create(
				ErrorCode.E400_VALIDATION,
				ErrorCode.E400_VALIDATION.getDefaultMessage(),
				List.of(detail),
				requestUri(request));
		return ResponseEntity.status(body.getStatus()).body(body);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex, HttpServletRequest request) {
		log.debug("HttpMessageNotReadable: {}", ex.getMessage());
		ProblemDetail body = ProblemDetailFactory.create(
				ErrorCode.E400_ILLEGAL_ARGUMENT, "요청 본문 형식이 올바르지 않습니다.", null, requestUri(request));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
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
		if (ex.getErrorCode().getHttpStatus().is5xxServerError()) {
			log.error("BusinessException [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex);
		} else {
			log.info("BusinessException [{}]: {}", ex.getErrorCode(), ex.getMessage());
		}
		ProblemDetail body = ProblemDetailFactory.create(ex.getErrorCode(), ex.getMessage(), null, requestUri(request));
		return ResponseEntity.status(body.getStatus()).body(body);
	}

	@ExceptionHandler(ProcessingClientException.class)
	public ResponseEntity<ProblemDetail> handleProcessing(ProcessingClientException ex, HttpServletRequest request) {
		log.warn("Processing(FastAPI private) 연동 실패: status={}", ex.getStatus(), ex);
		ProblemDetail body = ProblemDetailFactory.create(ErrorCode.E502_EXTERNAL_API, null, null, requestUri(request));
		return ResponseEntity.status(body.getStatus()).body(body);
	}

	@ExceptionHandler(KakaoLocalClientException.class)
	public ResponseEntity<ProblemDetail> handleKakaoLocal(KakaoLocalClientException ex, HttpServletRequest request) {
		log.warn("Kakao Local API call failed: status={}", ex.getStatus(), ex);
		ProblemDetail body = ProblemDetailFactory.create(ErrorCode.E502_EXTERNAL_API, null, null, requestUri(request));
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
