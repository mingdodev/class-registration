package com.example.classregistration.domain.waitlist;

import com.example.classregistration.domain.creator.model.Creator;
import com.example.classregistration.domain.enrollment.model.Enrollment;
import com.example.classregistration.domain.enrollment.repository.EnrollmentRepository;
import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klass.model.KlassStatus;
import com.example.classregistration.domain.klass.repository.KlassRepository;
import com.example.classregistration.domain.klassmate.model.Klassmate;
import com.example.classregistration.domain.waitlist.model.Waitlist;
import com.example.classregistration.domain.waitlist.repository.WaitlistRepository;
import com.example.classregistration.domain.waitlist.service.WaitlistProcessorService;
import com.example.classregistration.fixture.KlassFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class WaitlistProcessorServiceTest {

    @Mock WaitlistRepository waitlistRepository;
    @Mock KlassRepository klassRepository;
    @Mock EnrollmentRepository enrollmentRepository;
    @InjectMocks WaitlistProcessorService waitlistProcessorService;

    Creator creator;
    Klassmate klassmate;

    @BeforeEach
    void setUp() {
        creator = mock(Creator.class);
        klassmate = mock(Klassmate.class);
    }

    // ===== 대기자 있음 =====

    @Test
    void 대기자가_있고_정원이_남아_있으면_수강신청이_생성되고_대기열에서_제거된다() {
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator);
        Waitlist waitlist = Waitlist.create(klassmate, klass);
        given(waitlistRepository.findFirstWaiterByKlassId(1L)).willReturn(Optional.of(waitlist));
        given(klassRepository.decreaseRemainingCapacity(1L)).willReturn(1);
        given(klassRepository.getReferenceById(1L)).willReturn(klass);

        boolean result = waitlistProcessorService.process(1L);

        assertThat(result).isTrue();
        then(enrollmentRepository).should().save(any(Enrollment.class));
        then(waitlistRepository).should().delete(waitlist);
    }

    @Test
    void 대기자가_있지만_정원이_없으면_수강신청을_생성하지_않고_false를_반환한다() {
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator);
        Waitlist waitlist = Waitlist.create(klassmate, klass);
        given(waitlistRepository.findFirstWaiterByKlassId(1L)).willReturn(Optional.of(waitlist));
        given(klassRepository.decreaseRemainingCapacity(1L)).willReturn(0);

        boolean result = waitlistProcessorService.process(1L);

        assertThat(result).isFalse();
        then(enrollmentRepository).shouldHaveNoInteractions();
        then(waitlistRepository).should().findFirstWaiterByKlassId(1L);
        then(waitlistRepository).shouldHaveNoMoreInteractions();
    }

    // ===== 대기자 없음 (대기열 소진) =====

    @Test
    void 대기자가_없고_수강_기간이_남아_있으면_강의가_OPEN으로_전환된다() {
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator);
        given(waitlistRepository.findFirstWaiterByKlassId(1L)).willReturn(Optional.empty());
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        boolean result = waitlistProcessorService.process(1L);

        assertThat(result).isTrue();
        assertThat(klass.getStatus()).isEqualTo(KlassStatus.OPEN);
    }

    @Test
    void 대기자가_없고_수강_기간이_종료되었으면_강의_상태가_변경되지_않는다() {
        Klass klass = KlassFixture.수강_기간이_종료된_마감된_강의(creator);
        given(waitlistRepository.findFirstWaiterByKlassId(1L)).willReturn(Optional.empty());
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        boolean result = waitlistProcessorService.process(1L);

        assertThat(result).isTrue();
        assertThat(klass.getStatus()).isEqualTo(KlassStatus.CLOSED);
    }
}
