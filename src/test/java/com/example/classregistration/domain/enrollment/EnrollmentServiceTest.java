package com.example.classregistration.domain.enrollment;

import com.example.classregistration.domain.creator.model.Creator;
import com.example.classregistration.domain.enrollment.dto.CreateEnrollmentResponse;
import com.example.classregistration.domain.enrollment.model.CancelReason;
import com.example.classregistration.domain.enrollment.model.Enrollment;
import com.example.classregistration.domain.enrollment.model.EnrollmentStatus;
import com.example.classregistration.domain.enrollment.repository.EnrollmentRepository;
import com.example.classregistration.domain.enrollment.service.EnrollmentService;
import com.example.classregistration.domain.klass.repository.KlassRepository;
import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klassmate.repository.KlassmateRepository;
import com.example.classregistration.domain.klassmate.model.Klassmate;
import com.example.classregistration.domain.waitlist.publisher.WaitlistEventPublisher;
import com.example.classregistration.domain.enrollment.client.PaymentClient;
import com.example.classregistration.fixture.EnrollmentFixture;
import com.example.classregistration.fixture.KlassFixture;
import com.example.classregistration.global.exception.BusinessException;
import com.example.classregistration.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private KlassRepository klassRepository;
    @Mock
    private KlassmateRepository klassmateRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private WaitlistEventPublisher waitlistEventPublisher;
    @Mock
    private PaymentClient paymentClient;
    @InjectMocks
    private EnrollmentService enrollmentService;

    private Creator creator;
    private Klassmate klassmate;

    @BeforeEach
    void setUp() {
        creator = mock(Creator.class);
        klassmate = mock(Klassmate.class);
    }

    // ===== enroll =====

    @Test
    void OPEN_강의에_수강_신청하면_결제_대기_수강신청이_생성된다() {
        Klass klass = KlassFixture.모집중_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));
        given(enrollmentRepository.isAlreadyEnrolled(1L, 1L)).willReturn(false);
        given(klassRepository.decreaseRemainingCapacity(1L)).willReturn(1); // 자리 확보 성공
        given(klassmateRepository.getReferenceById(1L)).willReturn(klassmate);
        given(enrollmentRepository.save(any(Enrollment.class))).willAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            ReflectionTestUtils.setField(enrollment, "id", 1L);
            return enrollment;
        });

        CreateEnrollmentResponse response = enrollmentService.enroll(1L, 1L);

        assertThat(response.enrollmentId()).isEqualTo(1L);
        then(enrollmentRepository).should().save(any(Enrollment.class));
    }

    @Test
    void 존재하지_않는_강의에_수강_신청하면_예외가_발생한다() {
        given(klassRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_NOT_FOUND);
    }

    @Test
    void DRAFT_강의에_수강_신청하면_예외가_발생한다() {
        Klass klass = KlassFixture.초안_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_NOT_OPEN);
    }

    @Test
    void CLOSED_강의에_수강_신청하면_예외가_발생한다() {
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_NOT_OPEN);
    }

    @Test
    void 수강_기간이_종료된_강의에_수강_신청하면_예외가_발생한다() {
        // 수강 신청 요청 시점에서 이중 방어 - 스케줄러 주기 사이 만료된 강의 거부
        Klass klass = KlassFixture.수강_기간이_종료된_모집중_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_PERIOD_ENDED);
    }

    @Test
    void 정원이_가득_찬_강의에_수강_신청하면_예외가_발생한다() {
        // 원자적 업데이트가 0행을 반환하면 정원 초과로 거부
        Klass klass = KlassFixture.모집중_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));
        given(enrollmentRepository.isAlreadyEnrolled(1L, 1L)).willReturn(false);
        given(klassRepository.decreaseRemainingCapacity(1L)).willReturn(0); // 정원 가득 참

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_FULL);
    }

    @Test
    void 이미_수강신청한_강의에_재신청하면_예외가_발생한다() {
        Klass klass = KlassFixture.모집중_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));
        given(enrollmentRepository.isAlreadyEnrolled(1L, 1L)).willReturn(true);

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENROLLMENT_ALREADY_EXISTS);
    }

    @Test
    void 취소된_수강신청이_있는_강의에_재신청하면_수강신청이_생성된다() {
        Klass klass = KlassFixture.모집중_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));
        given(enrollmentRepository.isAlreadyEnrolled(1L, 1L)).willReturn(false);
        given(klassRepository.decreaseRemainingCapacity(1L)).willReturn(1);
        given(klassmateRepository.getReferenceById(1L)).willReturn(klassmate);
        given(enrollmentRepository.save(any(Enrollment.class))).willAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            ReflectionTestUtils.setField(enrollment, "id", 2L);
            return enrollment;
        });

        CreateEnrollmentResponse response = enrollmentService.enroll(1L, 1L);

        assertThat(response.enrollmentId()).isEqualTo(2L);
    }

    // ===== confirmEnrollment =====

    @Test
    void PENDING_수강신청을_결제_확정하면_CONFIRMED_상태가_된다() {
        Klass klass = KlassFixture.모집중_강의(creator);
        Enrollment enrollment = EnrollmentFixture.결제_대기중_수강신청(klassmate, klass);
        given(enrollmentRepository.findByIdAndKlassmateId(1L, 1L)).willReturn(Optional.of(enrollment));
        given(paymentClient.pay(anyLong(), anyInt())).willReturn(true);

        enrollmentService.confirmEnrollment(1L, 1L);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    }

    @Test
    void 결제_기한이_만료된_수강신청을_확정하면_예외가_발생한다() {
        // ADR-04: 결제 요청 시점에서 만료 여부 이중 방어
        Klass klass = KlassFixture.모집중_강의(creator);
        Enrollment enrollment = EnrollmentFixture.결제_기한이_만료된_수강신청(klassmate, klass);
        given(enrollmentRepository.findByIdAndKlassmateId(1L, 1L)).willReturn(Optional.of(enrollment));
        given(paymentClient.pay(anyLong(), anyInt())).willReturn(true);

        assertThatThrownBy(() -> enrollmentService.confirmEnrollment(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENROLLMENT_PAYMENT_EXPIRED);
    }

    @Test
    void 존재하지_않는_수강신청을_확정하면_예외가_발생한다() {
        given(enrollmentRepository.findByIdAndKlassmateId(999L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.confirmEnrollment(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENROLLMENT_NOT_FOUND);
    }

    @ParameterizedTest
    @MethodSource("nonPendingEnrollments")
    void PENDING이_아닌_수강신청을_확정하면_예외가_발생한다(Enrollment enrollment) {
        given(enrollmentRepository.findByIdAndKlassmateId(1L, 1L)).willReturn(Optional.of(enrollment));
        given(paymentClient.pay(anyLong(), anyInt())).willReturn(true);

        assertThatThrownBy(() -> enrollmentService.confirmEnrollment(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENROLLMENT_NOT_PENDING);
    }

    static Stream<Enrollment> nonPendingEnrollments() {
        Creator creator = mock(Creator.class);
        Klassmate klassmate = mock(Klassmate.class);
        Klass klass = KlassFixture.모집중_강의(creator);
        return Stream.of(
                EnrollmentFixture.수강_확정된_수강신청(klassmate, klass),
                EnrollmentFixture.취소된_수강신청(klassmate, klass)
        );
    }

    // ===== cancelEnrollment =====

    @Test
    void 시작일_3일_이전에_수강을_취소하면_CANCELLED_상태가_된다() {
        Klass klass = KlassFixture.시작일이_5일_후인_강의(creator); // OPEN, 마감일: 오늘 + 2일 → 취소 가능
        ReflectionTestUtils.setField(klass, "id", 1L);
        Enrollment enrollment = EnrollmentFixture.수강_확정된_수강신청(klassmate, klass);
        given(enrollmentRepository.findByIdAndKlassmateId(1L, 1L)).willReturn(Optional.of(enrollment));

        enrollmentService.cancelEnrollment(1L, 1L);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(enrollment.getCancelReason()).isEqualTo(CancelReason.USER_REQUESTED);
        then(klassRepository).should().increaseRemainingCapacity(1L);
        then(waitlistEventPublisher).shouldHaveNoInteractions(); // OPEN → 대기열 이벤트 불필요
    }

    @Test
    void 취소_가능_기간이_지난_수강신청을_취소하면_예외가_발생한다() {
        Klass klass = KlassFixture.시작일이_2일_후인_강의(creator); // 마감일: 오늘 - 1일 → 취소 불가
        Enrollment enrollment = EnrollmentFixture.수강_확정된_수강신청(klassmate, klass);
        given(enrollmentRepository.findByIdAndKlassmateId(1L, 1L)).willReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENROLLMENT_CANCEL_NOT_ALLOWED);
    }

    @Test
    void 수강_기간이_무제한인_강의의_수강을_취소하면_언제든_취소가_가능하다() {
        Klass klass = KlassFixture.모집중_강의(creator); // OPEN, startDate = null
        ReflectionTestUtils.setField(klass, "id", 1L);
        Enrollment enrollment = EnrollmentFixture.수강_확정된_수강신청(klassmate, klass);
        given(enrollmentRepository.findByIdAndKlassmateId(1L, 1L)).willReturn(Optional.of(enrollment));

        enrollmentService.cancelEnrollment(1L, 1L);

        assertThat(enrollment.getCancelReason()).isEqualTo(CancelReason.USER_REQUESTED);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        then(klassRepository).should().increaseRemainingCapacity(1L);
        then(waitlistEventPublisher).shouldHaveNoInteractions(); // OPEN → 대기열 이벤트 불필요
    }

    @Test
    void 존재하지_않는_수강신청을_취소하면_예외가_발생한다() {
        given(enrollmentRepository.findByIdAndKlassmateId(999L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENROLLMENT_NOT_FOUND);
    }

    @Test
    void PENDING_수강신청은_취소_가능_기간과_관계없이_취소할_수_있다() {
        Klass klass = KlassFixture.시작일이_2일_후인_강의(creator); // OPEN, CONFIRMED면 취소 불가
        ReflectionTestUtils.setField(klass, "id", 1L);
        Enrollment enrollment = EnrollmentFixture.결제_대기중_수강신청(klassmate, klass);
        given(enrollmentRepository.findByIdAndKlassmateId(1L, 1L)).willReturn(Optional.of(enrollment));

        enrollmentService.cancelEnrollment(1L, 1L);

        assertThat(enrollment.getCancelReason()).isEqualTo(CancelReason.USER_REQUESTED);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        then(klassRepository).should().increaseRemainingCapacity(1L);
        then(waitlistEventPublisher).shouldHaveNoInteractions(); // POPEN 강의 → 대기열 이벤트 불필요
    }

    @Test
    void CLOSED_강의_수강을_취소하면_정원이_복구되고_이벤트가_발행된다() {
        // ADR-04: CLOSED 상태에서만 대기열 이벤트 발행
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator); // CLOSED, startDate=null → 취소 가능
        ReflectionTestUtils.setField(klass, "id", 1L);
        Enrollment enrollment = EnrollmentFixture.수강_확정된_수강신청(klassmate, klass);
        given(enrollmentRepository.findByIdAndKlassmateId(1L, 1L)).willReturn(Optional.of(enrollment));

        enrollmentService.cancelEnrollment(1L, 1L);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        then(klassRepository).should().increaseRemainingCapacity(1L);
        then(waitlistEventPublisher).should().publish(1L);
    }

    @Test
    void 이벤트_발행에_실패해도_취소와_정원_복구는_완료된다() {
        // ADR-04: 대기열 처리 실패가 취소에 영향을 주어서는 안 됨
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator); // CLOSED여야 publish가 호출됨
        ReflectionTestUtils.setField(klass, "id", 1L);
        Enrollment enrollment = EnrollmentFixture.수강_확정된_수강신청(klassmate, klass);
        given(enrollmentRepository.findByIdAndKlassmateId(1L, 1L)).willReturn(Optional.of(enrollment));
        willThrow(new RuntimeException("이벤트 발행 실패")).given(waitlistEventPublisher).publish(1L);

        enrollmentService.cancelEnrollment(1L, 1L);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        then(klassRepository).should().increaseRemainingCapacity(1L);
    }

    @Test
    void 이미_취소된_수강신청을_취소하면_예외가_발생한다() {
        Klass klass = KlassFixture.시작일이_5일_후인_강의(creator);
        Enrollment enrollment = EnrollmentFixture.취소된_수강신청(klassmate, klass);
        given(enrollmentRepository.findByIdAndKlassmateId(1L, 1L)).willReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENROLLMENT_ALREADY_CANCELLED);
    }
}
