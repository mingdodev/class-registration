package com.example.classregistration.domain.klass;

import com.example.classregistration.domain.creator.CreatorRepository;
import com.example.classregistration.domain.creator.model.Creator;
import com.example.classregistration.domain.enrollment.EnrollmentRepository;
import com.example.classregistration.domain.enrollment.model.Enrollment;
import com.example.classregistration.domain.klass.dto.CreateKlassRequest;
import com.example.classregistration.domain.klass.dto.UpdateKlassRequest;
import com.example.classregistration.domain.waitlist.WaitlistEventPublisher;
import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klass.model.KlassStatus;
import com.example.classregistration.domain.klassmate.model.Klassmate;
import com.example.classregistration.fixture.EnrollmentFixture;
import com.example.classregistration.fixture.KlassFixture;
import com.example.classregistration.fixture.KlassRequestFixture;
import com.example.classregistration.global.exception.BusinessException;
import com.example.classregistration.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class KlassServiceTest {

    @Mock
    private KlassRepository klassRepository;
    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private WaitlistEventPublisher waitlistEventPublisher;
    @InjectMocks
    private KlassService klassService;

    private Creator creator;

    @BeforeEach
    void setUp() {
        creator = mock(Creator.class);
    }

    // ===== createKlass =====

    @Test
    void 유효한_요청으로_강의_초안을_등록하면_강의가_저장된다() {
        CreateKlassRequest request = KlassRequestFixture.유효한_강의_생성_요청();
        given(creatorRepository.getReferenceById(1L)).willReturn(creator);
        given(klassRepository.save(any(Klass.class))).willAnswer(invocation -> {
            Klass klass = invocation.getArgument(0);
            ReflectionTestUtils.setField(klass, "id", 1L);
            return klass;
        });

        Long klassId = klassService.createKlass(1L, request);

        assertThat(klassId).isEqualTo(1L);
        then(klassRepository).should().save(any(Klass.class));
    }

    @Test
    void 강의명이_20자를_초과하면_예외가_발생한다() {
        CreateKlassRequest request = KlassRequestFixture.강의명이_20자_초과인_생성_요청();
        given(creatorRepository.getReferenceById(1L)).willReturn(creator);

        assertThatThrownBy(() -> klassService.createKlass(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_TITLE_TOO_LONG);
    }

    @Test
    void 수강_정원이_1_미만이면_예외가_발생한다() {
        CreateKlassRequest request = KlassRequestFixture.수강_정원이_0인_생성_요청();
        given(creatorRepository.getReferenceById(1L)).willReturn(creator);

        assertThatThrownBy(() -> klassService.createKlass(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_CAPACITY_INVALID);
    }

    // ===== openKlass =====
    // 상태 기반 다중 케이스: Klass Fixture가 creator mock을 내부에 포함하므로
    // static @MethodSource 사용 불가 → 상태별 @Test로 분리

    @Test
    void DRAFT_강의를_모집_시작하면_OPEN_상태가_된다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.초안_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        klassService.openKlass(1L, 1L);

        assertThat(klass.getStatus()).isEqualTo(KlassStatus.OPEN);
    }

    @Test
    void 존재하지_않는_강의를_모집_시작하면_예외가_발생한다() {
        given(klassRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> klassService.openKlass(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_NOT_FOUND);
    }

    @Test
    void 자신의_강의가_아닌_강의를_모집_시작하면_예외가_발생한다() {
        Creator otherCreator = mock(Creator.class);
        given(otherCreator.getId()).willReturn(99L);
        Klass klass = KlassFixture.초안_강의(otherCreator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> klassService.openKlass(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_ACCESS_DENIED);
    }

    @Test
    void OPEN_강의를_모집_시작하면_예외가_발생한다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.모집중_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> klassService.openKlass(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_NOT_DRAFT);
    }

    @Test
    void CLOSED_강의를_모집_시작하면_예외가_발생한다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> klassService.openKlass(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_NOT_DRAFT);
    }

    // ===== updateKlass =====

    @Test
    void DRAFT_강의의_제목을_수정하면_제목이_변경된다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.초안_강의(creator);
        UpdateKlassRequest request = KlassRequestFixture.제목_수정_요청("수정된 강의");
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        klassService.updateKlass(1L, 1L, request);

        assertThat(klass.getTitle()).isEqualTo("수정된 강의");
    }

    @Test
    void OPEN_강의의_제목을_수정하면_제목이_변경된다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.모집중_강의(creator);
        UpdateKlassRequest request = KlassRequestFixture.제목_수정_요청("수정된 강의");
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        klassService.updateKlass(1L, 1L, request);

        assertThat(klass.getTitle()).isEqualTo("수정된 강의");
    }

    @Test
    void 강의명이_20자를_초과하면_수정_시에도_예외가_발생한다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.초안_강의(creator);
        UpdateKlassRequest request = KlassRequestFixture.강의명이_20자_초과인_수정_요청();
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> klassService.updateKlass(1L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_TITLE_TOO_LONG);
    }

    @Test
    void 자신의_강의가_아닌_강의를_수정하면_예외가_발생한다() {
        Creator otherCreator = mock(Creator.class);
        given(otherCreator.getId()).willReturn(99L);
        Klass klass = KlassFixture.초안_강의(otherCreator);
        UpdateKlassRequest request = KlassRequestFixture.제목_수정_요청("수정된 강의");
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> klassService.updateKlass(1L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_ACCESS_DENIED);
    }

    @Test
    void 존재하지_않는_강의를_수정하면_예외가_발생한다() {
        UpdateKlassRequest request = KlassRequestFixture.제목_수정_요청("수정된 강의");
        given(klassRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> klassService.updateKlass(1L, 999L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_NOT_FOUND);
    }

    @Test
    void DRAFT_강의의_정원을_1명_미만으로_수정하면_예외가_발생한다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.초안_강의(creator);
        UpdateKlassRequest request = KlassRequestFixture.정원_감소_수정_요청(0);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> klassService.updateKlass(1L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_CAPACITY_INVALID);
    }

    @Test
    void CLOSED_강의_정원을_늘리면_증가한_수만큼_이벤트가_발행된다() {
        // 기본 maxCapacity=20, 25로 증가 → 이벤트 5회
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator);
        UpdateKlassRequest request = KlassRequestFixture.정원_증가_수정_요청(25);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        klassService.updateKlass(1L, 1L, request);

        then(waitlistEventPublisher).should(times(5)).publish(1L);
    }

    @Test
    void DRAFT_강의_정원을_늘려도_이벤트가_발행되지_않는다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.초안_강의(creator);
        UpdateKlassRequest request = KlassRequestFixture.정원_증가_수정_요청(25);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        klassService.updateKlass(1L, 1L, request);

        then(waitlistEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void OPEN_강의_정원을_늘려도_이벤트가_발행되지_않는다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.모집중_강의(creator);
        UpdateKlassRequest request = KlassRequestFixture.정원_증가_수정_요청(25);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        klassService.updateKlass(1L, 1L, request);

        then(waitlistEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 모집중인_강의의_가격을_수정하려_하면_예외가_발생한다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.모집중_강의(creator);
        UpdateKlassRequest request = KlassRequestFixture.가격_수정_요청(60000);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> klassService.updateKlass(1L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_PRICE_UPDATE_NOT_ALLOWED);
    }

    @Test
    void 마감된_강의의_가격을_수정하려_하면_예외가_발생한다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator);
        UpdateKlassRequest request = KlassRequestFixture.가격_수정_요청(60000);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> klassService.updateKlass(1L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_PRICE_UPDATE_NOT_ALLOWED);
    }

    @Test
    void 모집중인_강의의_정원을_줄이려_하면_예외가_발생한다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.모집중_강의(creator);
        UpdateKlassRequest request = KlassRequestFixture.정원_감소_수정_요청(5);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> klassService.updateKlass(1L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_CAPACITY_DECREASE_NOT_ALLOWED);
    }

    @Test
    void 마감된_강의의_정원을_줄이려_하면_예외가_발생한다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator);
        UpdateKlassRequest request = KlassRequestFixture.정원_감소_수정_요청(5);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> klassService.updateKlass(1L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_CAPACITY_DECREASE_NOT_ALLOWED);
    }

    // ===== deleteKlass =====

    @Test
    void 존재하지_않는_강의를_삭제하면_예외가_발생한다() {
        given(klassRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> klassService.deleteKlass(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_NOT_FOUND);
    }

    @Test
    void DRAFT_강의를_삭제하면_강의가_삭제된다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.초안_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        klassService.deleteKlass(1L, 1L);

        then(klassRepository).should().delete(klass);
    }

    @Test
    void OPEN_강의를_삭제하면_예외가_발생한다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.모집중_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> klassService.deleteKlass(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_NOT_DELETABLE);
    }

    @Test
    void CLOSED_강의를_삭제하면_예외가_발생한다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.수강_기간이_종료되지_않은_마감된_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> klassService.deleteKlass(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_NOT_DELETABLE);
    }

    @Test
    void 자신의_강의가_아닌_강의를_삭제하면_예외가_발생한다() {
        Creator otherCreator = mock(Creator.class);
        given(otherCreator.getId()).willReturn(99L);
        Klass klass = KlassFixture.초안_강의(otherCreator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> klassService.deleteKlass(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_ACCESS_DENIED);
    }

    // ===== getKlass =====

    @Test
    void 강의_상세를_조회한다() {
        Klass klass = KlassFixture.모집중_강의(creator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        Klass result = klassService.getKlass(1L);

        assertThat(result.getTitle()).isEqualTo(klass.getTitle());
        assertThat(result.getStatus()).isEqualTo(KlassStatus.OPEN);
    }

    @Test
    void 존재하지_않는_강의를_조회하면_예외가_발생한다() {
        given(klassRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> klassService.getKlass(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_NOT_FOUND);
    }

    // ===== getCreatorKlasses =====

    @Test
    void 강사가_자신의_강의_목록을_전체_조회한다() {
        Klass draft = KlassFixture.초안_강의(creator);
        Klass open = KlassFixture.모집중_강의(creator);
        given(klassRepository.findByCreatorId(1L)).willReturn(List.of(draft, open));

        List<Klass> result = klassService.getCreatorKlasses(1L, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void 강의_상태_필터로_강의_목록을_조회한다() {
        Klass openKlass = KlassFixture.모집중_강의(creator);
        given(klassRepository.findByCreatorIdAndStatus(1L, KlassStatus.OPEN)).willReturn(List.of(openKlass));

        List<Klass> result = klassService.getCreatorKlasses(1L, KlassStatus.OPEN);

        assertThat(result).hasSize(1);
    }

    // ===== getKlassmates =====

    @Test
    void 강의별_수강생_목록을_조회한다() {
        given(creator.getId()).willReturn(1L);
        Klass klass = KlassFixture.모집중_강의(creator);
        Klassmate klassmate = mock(Klassmate.class);
        Enrollment enrollment = EnrollmentFixture.수강_확정된_수강신청(klassmate, klass);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));
        given(enrollmentRepository.findRegisteredEnrollmentsByKlassId(1L)).willReturn(List.of(enrollment));

        List<Enrollment> result = klassService.getKlassmates(1L, 1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void 존재하지_않는_강의의_수강생_목록을_조회하면_예외가_발생한다() {
        given(klassRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> klassService.getKlassmates(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_NOT_FOUND);
    }

    @Test
    void 자신의_강의가_아닌_경우_수강생_목록을_조회하면_예외가_발생한다() {
        Creator otherCreator = mock(Creator.class);
        given(otherCreator.getId()).willReturn(99L);
        Klass klass = KlassFixture.모집중_강의(otherCreator);
        given(klassRepository.findById(1L)).willReturn(Optional.of(klass));

        assertThatThrownBy(() -> klassService.getKlassmates(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KLASS_ACCESS_DENIED);
    }
}
