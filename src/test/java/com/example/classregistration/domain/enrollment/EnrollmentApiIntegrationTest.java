package com.example.classregistration.domain.enrollment;

import com.example.classregistration.domain.creator.model.Creator;
import com.example.classregistration.domain.creator.repository.CreatorRepository;
import com.example.classregistration.domain.enrollment.model.Enrollment;
import com.example.classregistration.domain.enrollment.repository.EnrollmentRepository;
import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klass.repository.KlassRepository;
import com.example.classregistration.domain.klassmate.model.Klassmate;
import com.example.classregistration.domain.klassmate.repository.KlassmateRepository;
import com.example.classregistration.fixture.*;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EnrollmentApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired KlassRepository klassRepository;
    @Autowired KlassmateRepository klassmateRepository;
    @Autowired CreatorRepository creatorRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;

    Creator 강사;
    Klassmate 수강생;

    @BeforeEach
    void setUp() {
        강사 = creatorRepository.save(CreatorFixture.강사());
        수강생 = klassmateRepository.save(KlassmateFixture.수강생());
    }

    // ── POST /api/klasses/{klassId}/enrollments ───────────────────────────────

    @Test
    void 모집중_강의에_수강_신청_하면_201을_응답하고_수강신청이_저장된다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));
        long countBefore = enrollmentRepository.count();

        mockMvc.perform(post("/api/klasses/{klassId}/enrollments", klass.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.enrollmentId").isNumber());

        assertThat(enrollmentRepository.count()).isEqualTo(countBefore + 1);
    }

    @Test
    void OPEN_아닌_강의에_수강_신청_하면_409를_응답하고_수강신청이_저장되지_않는다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.초안_강의(강사));
        long countBefore = enrollmentRepository.count();

        mockMvc.perform(post("/api/klasses/{klassId}/enrollments", klass.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isConflict());

        assertThat(enrollmentRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void 정원이_가득_찬_강의에_수강_신청_하면_409를_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.수강_기간이_종료되지_않은_마감된_강의(강사));

        mockMvc.perform(post("/api/klasses/{klassId}/enrollments", klass.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void 수강_기간이_종료된_강의에_수강_신청_하면_409를_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.수강_기간이_종료된_모집중_강의(강사));

        mockMvc.perform(post("/api/klasses/{klassId}/enrollments", klass.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void 이미_수강_신청한_강의에_다시_수강_신청_하면_409를_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));
        enrollmentRepository.save(EnrollmentFixture.결제_대기중_수강신청(수강생, klass));

        mockMvc.perform(post("/api/klasses/{klassId}/enrollments", klass.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isConflict());
    }

    // ── POST /api/enrollments/{enrollmentId}/confirm ──────────────────────────

    @Test
    void 결제_대기중_수강신청_확정_요청_하면_200을_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));
        Enrollment enrollment = enrollmentRepository.save(EnrollmentFixture.결제_대기중_수강신청(수강생, klass));

        mockMvc.perform(post("/api/enrollments/{enrollmentId}/confirm", enrollment.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", nullValue()));
    }

    @Test
    void 이미_확정된_수강신청_확정_요청_하면_409를_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));
        Enrollment enrollment = enrollmentRepository.save(EnrollmentFixture.수강_확정된_수강신청(수강생, klass));

        mockMvc.perform(post("/api/enrollments/{enrollmentId}/confirm", enrollment.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void 결제_기한이_초과된_수강신청_확정_요청_하면_409를_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));
        Enrollment enrollment = enrollmentRepository.save(EnrollmentFixture.결제_대기중_수강신청(수강생, klass));
        jdbcTemplate.update("UPDATE enrollments SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusHours(25)), enrollment.getId());
        entityManager.clear(); // 1차 캐시 무효화 — DB에 반영된 createdAt을 재조회하도록

        mockMvc.perform(post("/api/enrollments/{enrollmentId}/confirm", enrollment.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isConflict());
    }

    // ── DELETE /api/enrollments/{enrollmentId} ────────────────────────────────

    @Test
    void PENDING_수강신청_취소_요청_하면_200을_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));
        Enrollment enrollment = enrollmentRepository.save(EnrollmentFixture.결제_대기중_수강신청(수강생, klass));

        mockMvc.perform(delete("/api/enrollments/{enrollmentId}", enrollment.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", nullValue()));
    }

    @Test
    void 강의_시작일_3일_전인_확정_수강신청_취소_요청_하면_200을_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.시작일이_5일_후인_강의(강사));
        Enrollment enrollment = enrollmentRepository.save(EnrollmentFixture.수강_확정된_수강신청(수강생, klass));

        mockMvc.perform(delete("/api/enrollments/{enrollmentId}", enrollment.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", nullValue()));
    }

    @Test
    void 강의_시작일_3일_이내인_확정_수강신청_취소_요청_하면_409를_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.시작일이_2일_후인_강의(강사));
        Enrollment enrollment = enrollmentRepository.save(EnrollmentFixture.수강_확정된_수강신청(수강생, klass));

        mockMvc.perform(delete("/api/enrollments/{enrollmentId}", enrollment.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void 이미_취소된_수강신청_취소_요청_하면_409를_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));
        Enrollment enrollment = enrollmentRepository.save(EnrollmentFixture.취소된_수강신청(수강생, klass));

        mockMvc.perform(delete("/api/enrollments/{enrollmentId}", enrollment.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isConflict());
    }

    // ── GET /api/klassmates/me/enrollments ────────────────────────────────────

    @Test
    void 내_수강신청_목록_요청_하면_200을_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));
        enrollmentRepository.save(EnrollmentFixture.결제_대기중_수강신청(수강생, klass));
        enrollmentRepository.save(EnrollmentFixture.취소된_수강신청(수강생,
                klassRepository.save(KlassFixture.모집중_강의(강사))));

        mockMvc.perform(get("/api/klassmates/me/enrollments")
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.nextCursor", nullValue()))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.content[0].id").isNumber())
                .andExpect(jsonPath("$.data.content[0].klass.id").isNumber())
                .andExpect(jsonPath("$.data.content[0].klass.title").isString())
                .andExpect(jsonPath("$.data.content[0].klass.startDate", nullValue()))
                .andExpect(jsonPath("$.data.content[0].klass.endDate", nullValue()))
                .andExpect(jsonPath("$.data.content[0].status").isString())
                .andExpect(jsonPath("$.data.content[0].cancelReason").exists())
                .andExpect(jsonPath("$.data.content[0].createdAt").isString())
                .andExpect(jsonPath("$.data.content[0].updatedAt").isString());
    }

    @Test
    void 내_수강신청_목록_status_필터_요청_하면_해당_상태만_반환한다() throws Exception {
        Klass klass1 = klassRepository.save(KlassFixture.모집중_강의(강사));
        Klass klass2 = klassRepository.save(KlassFixture.모집중_강의(강사));
        enrollmentRepository.save(EnrollmentFixture.결제_대기중_수강신청(수강생, klass1));
        enrollmentRepository.save(EnrollmentFixture.취소된_수강신청(수강생, klass2));

        mockMvc.perform(get("/api/klassmates/me/enrollments")
                        .header("X-Klassmate-Id", 수강생.getId())
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.nextCursor", nullValue()))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    // ── GET /api/klasses/{klassId}/enrollments/me ─────────────────────────────

    @Test
    void 수강_신청한_강의의_수강신청_여부_확인_요청_하면_enrolled_true를_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));
        enrollmentRepository.save(EnrollmentFixture.결제_대기중_수강신청(수강생, klass));

        mockMvc.perform(get("/api/klasses/{klassId}/enrollments/me", klass.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enrolled").value(true))
                .andExpect(jsonPath("$.data.enrollmentId").isNumber())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void 수강_신청하지_않은_강의의_수강신청_여부_확인_요청_하면_enrolled_false를_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));

        mockMvc.perform(get("/api/klasses/{klassId}/enrollments/me", klass.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enrolled").value(false))
                .andExpect(jsonPath("$.data.enrollmentId", nullValue()))
                .andExpect(jsonPath("$.data.status", nullValue()));
    }
}
