package com.example.classregistration.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Klass
    KLASS_NOT_FOUND(HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다."),
    KLASS_TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "강의명이 20자를 초과합니다."),
    KLASS_CAPACITY_INVALID(HttpStatus.BAD_REQUEST, "수강 정원은 1명 이상이어야 합니다."),
    KLASS_NOT_DRAFT(HttpStatus.CONFLICT, "이미 공개된 강의는 모집을 다시 시작할 수 없습니다."),
    KLASS_NOT_OPEN(HttpStatus.CONFLICT, "현재 수강 신청을 받지 않는 강의입니다."),
    KLASS_FULL(HttpStatus.CONFLICT, "수강 정원이 가득 찼습니다."),
    KLASS_PERIOD_ENDED(HttpStatus.CONFLICT, "수강 기간이 종료된 강의입니다."),
    KLASS_ACCESS_DENIED(HttpStatus.FORBIDDEN, "자신의 강의가 아닙니다."),
    KLASS_NOT_DELETABLE(HttpStatus.CONFLICT, "공개된 강의는 삭제할 수 없습니다."),
    KLASS_CAPACITY_DECREASE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "모집 중이거나 마감된 강의의 수강 정원은 줄일 수 없습니다."),
    KLASS_PRICE_UPDATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "모집 중이거나 마감된 강의의 가격은 수정할 수 없습니다."),

    // Enrollment
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "수강 신청을 찾을 수 없습니다."),
    ENROLLMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 수강 신청한 강의입니다."),
    ENROLLMENT_NOT_PENDING(HttpStatus.CONFLICT, "결제 대기 중인 수강 신청이 아닙니다."),
    ENROLLMENT_PAYMENT_EXPIRED(HttpStatus.CONFLICT, "결제 가능 시간(24시간)이 지났습니다."),
    ENROLLMENT_CANCEL_NOT_ALLOWED(HttpStatus.CONFLICT, "취소 가능 기간(강의 시작일 3일 전)이 지났습니다."),
    ENROLLMENT_NOT_CONFIRMED(HttpStatus.CONFLICT, "수강이 확정된 신청만 취소할 수 있습니다."),

    // Waitlist
    WAITLIST_NOT_AVAILABLE(HttpStatus.CONFLICT, "정원이 마감된 강의에만 대기열 등록이 가능합니다."),
    WAITLIST_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 대기열에 등록된 강의입니다."),
    WAITLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "대기열에 등록되어 있지 않습니다.");

    private final HttpStatus status;
    private final String message;
}
