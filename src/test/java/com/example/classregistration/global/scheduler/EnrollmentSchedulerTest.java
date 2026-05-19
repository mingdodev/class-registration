package com.example.classregistration.global.scheduler;

import com.example.classregistration.domain.enrollment.service.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentSchedulerTest {

    @Mock
    private EnrollmentService enrollmentService;
    @InjectMocks
    private EnrollmentScheduler enrollmentScheduler;

    @Test
    void 만료된_PENDING_수강신청이_없으면_취소_처리가_실행되지_않는다() {
        given(enrollmentService.findExpiredPendingEnrollmentIds()).willReturn(List.of());

        enrollmentScheduler.cancelExpiredPendingEnrollments();

        then(enrollmentService).should().findExpiredPendingEnrollmentIds();
        then(enrollmentService).shouldHaveNoMoreInteractions();
    }

    @Test
    void 만료된_PENDING_수강신청이_있으면_각각_개별_취소가_실행된다() {
        given(enrollmentService.findExpiredPendingEnrollmentIds()).willReturn(List.of(1L, 2L, 3L));

        enrollmentScheduler.cancelExpiredPendingEnrollments();

        then(enrollmentService).should().cancelExpiredPendingEnrollment(1L);
        then(enrollmentService).should().cancelExpiredPendingEnrollment(2L);
        then(enrollmentService).should().cancelExpiredPendingEnrollment(3L);
    }

    @Test
    void 특정_수강신청_취소가_실패해도_나머지는_계속_처리된다() {
        given(enrollmentService.findExpiredPendingEnrollmentIds()).willReturn(List.of(1L, 2L, 3L));
        willThrow(new RuntimeException("취소 실패")).given(enrollmentService).cancelExpiredPendingEnrollment(2L);

        enrollmentScheduler.cancelExpiredPendingEnrollments();

        then(enrollmentService).should().cancelExpiredPendingEnrollment(1L);
        then(enrollmentService).should().cancelExpiredPendingEnrollment(2L);
        then(enrollmentService).should().cancelExpiredPendingEnrollment(3L);
    }
}
