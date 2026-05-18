package com.example.classregistration.domain.waitlist;

import com.example.classregistration.domain.creator.model.Creator;
import com.example.classregistration.domain.klass.KlassRepository;
import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klass.model.KlassStatus;
import com.example.classregistration.domain.klassmate.KlassmateRepository;
import com.example.classregistration.domain.klassmate.model.Klassmate;
import com.example.classregistration.domain.waitlist.dto.MyWaitlistStatusResponse;
import com.example.classregistration.domain.waitlist.model.Waitlist;
import com.example.classregistration.fixture.KlassFixture;
import com.example.classregistration.global.exception.BusinessException;
import com.example.classregistration.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class WaitlistServiceTest {

    @Mock
    private KlassRepository klassRepository;
    @Mock
    private KlassmateRepository klassmateRepository;
    @Mock
    private WaitlistRepository waitlistRepository;
    @InjectMocks
    private WaitlistService waitlistService;

    private Creator creator;
    private Klassmate klassmate;

    @BeforeEach
    void setUp() {
        creator = mock(Creator.class);
        klassmate = mock(Klassmate.class);
    }

    // ===== joinWaitlist =====

    @Test
    void CLOSED_강의에_대기열_등록을_하면_대기열에_추가된다() {
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));
        given(waitlistRepository.existsByKlassmateIdAndKlassId(1L, 1L)).willReturn(false);
        given(klassmateRepository.getReferenceById(1L)).willReturn(klassmate);

        waitlistService.joinWaitlist(1L, 1L);

        then(waitlistRepository).should().save(any(Waitlist.class));
    }

    @Test
    void 존재하지_않는_강의에_대기열_등록하면_예외가_발생한다() {
        given(klassRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> waitlistService.joinWaitlist(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_NOT_FOUND);
    }

    @Test
    void CLOSED_상태가_아닌_강의에_대기열_등록하면_예외가_발생한다() {
        Klass klass = KlassFixture.모집중_강의(creator); // OPEN
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> waitlistService.joinWaitlist(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAITLIST_NOT_AVAILABLE);
    }

    @Test
    void 수강_기간이_종료된_강의에_대기열_등록하면_예외가_발생한다() {
        Klass klass = KlassFixture.수강_기간이_종료된_마감된_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> waitlistService.joinWaitlist(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAITLIST_NOT_AVAILABLE);
    }

    @Test
    void 이미_대기열에_등록된_경우_예외가_발생한다() {
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));
        given(waitlistRepository.existsByKlassmateIdAndKlassId(1L, 1L)).willReturn(true);

        assertThatThrownBy(() -> waitlistService.joinWaitlist(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAITLIST_ALREADY_EXISTS);
    }

    // ===== leaveWaitlist =====

    @Test
    void 존재하지_않는_강의의_대기열을_취소하면_예외가_발생한다() {
        given(klassRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> waitlistService.leaveWaitlist(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_NOT_FOUND);
    }

    @Test
    void 대기열_등록을_취소하면_대기열에서_삭제된다() {
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator);
        Waitlist waitlist = Waitlist.create(klassmate, klass);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));
        given(waitlistRepository.findByKlassmateIdAndKlassId(1L, 1L)).willReturn(Optional.of(waitlist));

        waitlistService.leaveWaitlist(1L, 1L);

        then(waitlistRepository).should().delete(waitlist);
    }

    @Test
    void 대기열에_등록되지_않은_경우_취소하면_예외가_발생한다() {
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));
        given(waitlistRepository.findByKlassmateIdAndKlassId(1L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> waitlistService.leaveWaitlist(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAITLIST_NOT_FOUND);
    }

    // ===== getMyWaitlistStatus =====

    @Test
    void 존재하지_않는_강의의_대기열_상태를_조회하면_예외가_발생한다() {
        given(klassRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> waitlistService.getMyWaitlistStatus(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_NOT_FOUND);
    }

    @Test
    void 대기열에_등록된_경우_등록_정보를_반환한다() {
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator);
        Waitlist waitlist = Waitlist.create(klassmate, klass);
        ReflectionTestUtils.setField(waitlist, "createdAt", LocalDateTime.now().minusHours(1));
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));
        given(waitlistRepository.findByKlassmateIdAndKlassId(1L, 1L)).willReturn(Optional.of(waitlist));

        MyWaitlistStatusResponse response = waitlistService.getMyWaitlistStatus(1L, 1L);

        assertThat(response.registered()).isTrue();
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void 대기열에_등록되지_않은_경우_미등록_정보를_반환한다() {
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));
        given(waitlistRepository.findByKlassmateIdAndKlassId(1L, 1L)).willReturn(Optional.empty());

        MyWaitlistStatusResponse response = waitlistService.getMyWaitlistStatus(1L, 1L);

        assertThat(response.registered()).isFalse();
        assertThat(response.createdAt()).isNull();
    }
}
