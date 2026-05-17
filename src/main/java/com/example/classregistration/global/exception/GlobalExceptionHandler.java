package com.example.classregistration.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_TYPE_BASE = "https://api.class-registration.com/errors/";

    // 비즈니스 예외: info level, 에러 코드 내 사유 출력
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.info("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ProblemDetail problem = buildProblemDetail(errorCode.getStatus(), errorCode.getMessage(), errorCode.name(), request);
        return ResponseEntity.status(errorCode.getStatus()).body(problem);
    }

    // 파라미터 검증 예외: info level
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.info("MethodArgumentNotValidException: {}", detail);
        ProblemDetail problem = buildProblemDetail(HttpStatus.BAD_REQUEST, detail, "INVALID_INPUT", request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // 파라미터 문법 예외: info level
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.info("HttpMessageNotReadableException: {}", e.getMessage());
        ProblemDetail problem = buildProblemDetail(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다.", "INVALID_REQUEST_BODY", request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // 매개변수 타입 예외: info level
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String detail = String.format("'%s' 파라미터의 값 '%s'이 올바르지 않습니다.", e.getName(), e.getValue());
        log.info("MethodArgumentTypeMismatchException: {}", detail);
        ProblemDetail problem = buildProblemDetail(HttpStatus.BAD_REQUEST, detail, "INVALID_PARAMETER_TYPE", request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // 확인되지 않은 예외: error level, 이 경우는 스택 트레이스를 출력
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception e, HttpServletRequest request) {
        log.error("Unexpected exception", e);
        ProblemDetail problem = buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR", request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String detail, String code, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(ERROR_TYPE_BASE + code.toLowerCase().replace('_', '-')));
        problem.setDetail(detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }
}
