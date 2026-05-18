package com.example.classregistration.fixture;

import com.example.classregistration.domain.enrollment.model.CancelReason;
import com.example.classregistration.domain.enrollment.model.Enrollment;
import com.example.classregistration.domain.enrollment.model.EnrollmentStatus;
import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klassmate.model.Klassmate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

public class EnrollmentFixture {

    public static Enrollment 결제_대기중_수강신청(Klassmate klassmate, Klass klass) {
        Enrollment enrollment = Enrollment.create(klassmate, klass);
        ReflectionTestUtils.setField(enrollment, "createdAt", LocalDateTime.now());
        return enrollment;
    }

    public static Enrollment 결제_기한이_만료된_수강신청(Klassmate klassmate, Klass klass) {
        Enrollment enrollment = Enrollment.create(klassmate, klass);
        ReflectionTestUtils.setField(enrollment, "createdAt", LocalDateTime.now().minusHours(25));
        return enrollment;
    }

    public static Enrollment 수강_확정된_수강신청(Klassmate klassmate, Klass klass) {
        Enrollment enrollment = Enrollment.create(klassmate, klass);
        ReflectionTestUtils.setField(enrollment, "status", EnrollmentStatus.CONFIRMED);
        ReflectionTestUtils.setField(enrollment, "createdAt", LocalDateTime.now());
        return enrollment;
    }

    public static Enrollment 취소된_수강신청(Klassmate klassmate, Klass klass) {
        Enrollment enrollment = Enrollment.create(klassmate, klass);
        ReflectionTestUtils.setField(enrollment, "status", EnrollmentStatus.CANCELLED);
        ReflectionTestUtils.setField(enrollment, "cancelReason", CancelReason.USER_REQUESTED);
        ReflectionTestUtils.setField(enrollment, "createdAt", LocalDateTime.now());
        return enrollment;
    }
}
