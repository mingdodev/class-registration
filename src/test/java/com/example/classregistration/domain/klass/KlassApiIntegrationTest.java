package com.example.classregistration.domain.klass;

import com.example.classregistration.domain.creator.model.Creator;
import com.example.classregistration.domain.creator.repository.CreatorRepository;
import com.example.classregistration.domain.klass.dto.CreateKlassRequest;
import com.example.classregistration.domain.klass.dto.UpdateKlassRequest;
import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klass.repository.KlassRepository;
import com.example.classregistration.fixture.CreatorFixture;
import com.example.classregistration.fixture.KlassFixture;
import com.example.classregistration.fixture.KlassRequestFixture;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class KlassApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired KlassRepository klassRepository;
    @Autowired CreatorRepository creatorRepository;

    Creator 강사;
    Creator 다른_강사;

    @BeforeEach
    void setUp() {
        강사 = creatorRepository.save(CreatorFixture.강사());
        다른_강사 = creatorRepository.save(CreatorFixture.강사("other@test.com"));
    }

    // ── GET /api/klasses ──────────────────────────────────────────────────────

    @Test
    void 전체_강의_목록_요청_하면_200을_응답하고_OPEN_CLOSED_강의만_반환한다() throws Exception {
        klassRepository.save(KlassFixture.초안_강의(강사));
        klassRepository.save(KlassFixture.모집중_강의(강사));
        klassRepository.save(KlassFixture.수강_기간이_종료되지_않은_마감된_강의(강사));

        mockMvc.perform(get("/api/klasses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor", nullValue()))
                .andExpect(jsonPath("$.data.content[0].id").isNumber())
                .andExpect(jsonPath("$.data.content[0].title").isString())
                .andExpect(jsonPath("$.data.content[0].price").isNumber())
                .andExpect(jsonPath("$.data.content[0].status").isString())
                .andExpect(jsonPath("$.data.content[0].remainingCapacity").isNumber())
                .andExpect(jsonPath("$.data.content[0].startDate", nullValue()))
                .andExpect(jsonPath("$.data.content[0].endDate", nullValue()));
    }

    @Test
    void status_필터로_강의_목록_요청_하면_해당_상태의_강의만_반환한다() throws Exception {
        klassRepository.save(KlassFixture.모집중_강의(강사));
        klassRepository.save(KlassFixture.수강_기간이_종료되지_않은_마감된_강의(강사));

        mockMvc.perform(get("/api/klasses").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor", nullValue()))
                .andExpect(jsonPath("$.data.content[0].status").value("OPEN"));
    }

    // ── GET /api/klasses/{klassId} ────────────────────────────────────────────

    @Test
    void 강의_상세_요청_하면_200을_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));

        mockMvc.perform(get("/api/klasses/{klassId}", klass.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(klass.getId()))
                .andExpect(jsonPath("$.data.title").isString())
                .andExpect(jsonPath("$.data.description").isString())
                .andExpect(jsonPath("$.data.creator.id").value(강사.getId()))
                .andExpect(jsonPath("$.data.creator.name").isString())
                .andExpect(jsonPath("$.data.price").isNumber())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.maxCapacity").isNumber())
                .andExpect(jsonPath("$.data.remainingCapacity").isNumber())
                .andExpect(jsonPath("$.data.enrolledCount").isNumber())
                .andExpect(jsonPath("$.data.startDate", nullValue()))
                .andExpect(jsonPath("$.data.endDate", nullValue()));
    }

    @Test
    void 없는_강의_상세_요청_하면_404를_응답한다() throws Exception {
        mockMvc.perform(get("/api/klasses/{klassId}", 999L))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/klasses ─────────────────────────────────────────────────────

    @Test
    void 유효한_강의_생성_요청_하면_201을_응답하고_강의가_저장된다() throws Exception {
        CreateKlassRequest request = KlassRequestFixture.유효한_강의_생성_요청();
        long countBefore = klassRepository.count();

        mockMvc.perform(post("/api/klasses")
                        .header("X-Creator-Id", 강사.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber());

        assertThat(klassRepository.count()).isEqualTo(countBefore + 1);
    }

    @Test
    void 강의명이_20자_초과인_생성_요청_하면_400을_응답하고_강의가_저장되지_않는다() throws Exception {
        CreateKlassRequest request = KlassRequestFixture.강의명이_20자_초과인_생성_요청();
        long countBefore = klassRepository.count();

        mockMvc.perform(post("/api/klasses")
                        .header("X-Creator-Id", 강사.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(klassRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void 수강_정원이_0인_생성_요청_하면_400을_응답하고_강의가_저장되지_않는다() throws Exception {
        CreateKlassRequest request = KlassRequestFixture.수강_정원이_0인_생성_요청();
        long countBefore = klassRepository.count();

        mockMvc.perform(post("/api/klasses")
                        .header("X-Creator-Id", 강사.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(klassRepository.count()).isEqualTo(countBefore);
    }

    // ── PATCH /api/klasses/{klassId}/open ─────────────────────────────────────

    @Test
    void 초안_강의_모집_시작_요청_하면_200을_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.초안_강의(강사));

        mockMvc.perform(patch("/api/klasses/{klassId}/open", klass.getId())
                        .header("X-Creator-Id", 강사.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", nullValue()));
    }

    @Test
    void 다른_강사의_강의를_모집_시작_요청_하면_403을_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.초안_강의(강사));

        mockMvc.perform(patch("/api/klasses/{klassId}/open", klass.getId())
                        .header("X-Creator-Id", 다른_강사.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void DRAFT_아닌_강의를_모집_시작_요청_하면_409를_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));

        mockMvc.perform(patch("/api/klasses/{klassId}/open", klass.getId())
                        .header("X-Creator-Id", 강사.getId()))
                .andExpect(status().isConflict());
    }

    // ── PATCH /api/klasses/{klassId} ──────────────────────────────────────────

    @Test
    void 강의_수정_요청_하면_200을_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.초안_강의(강사));
        UpdateKlassRequest request = KlassRequestFixture.제목_수정_요청("수정된 제목");

        mockMvc.perform(patch("/api/klasses/{klassId}", klass.getId())
                        .header("X-Creator-Id", 강사.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", nullValue()));
    }

    @Test
    void 다른_강사의_강의를_수정_요청_하면_403을_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.초안_강의(강사));
        UpdateKlassRequest request = KlassRequestFixture.제목_수정_요청("수정된 제목");

        mockMvc.perform(patch("/api/klasses/{klassId}", klass.getId())
                        .header("X-Creator-Id", 다른_강사.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void OPEN_상태에서_가격_수정_요청_하면_400을_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));
        UpdateKlassRequest request = KlassRequestFixture.가격_수정_요청(60000);

        mockMvc.perform(patch("/api/klasses/{klassId}", klass.getId())
                        .header("X-Creator-Id", 강사.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void OPEN_상태에서_정원_감소_요청_하면_400을_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));
        UpdateKlassRequest request = KlassRequestFixture.정원_감소_수정_요청(1);

        mockMvc.perform(patch("/api/klasses/{klassId}", klass.getId())
                        .header("X-Creator-Id", 강사.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /api/klasses/{klassId} ─────────────────────────────────────────

    @Test
    void 초안_강의_삭제_요청_하면_200을_응답하고_강의가_삭제된다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.초안_강의(강사));

        mockMvc.perform(delete("/api/klasses/{klassId}", klass.getId())
                        .header("X-Creator-Id", 강사.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", nullValue()));

        assertThat(klassRepository.findById(klass.getId())).isEmpty();
    }

    @Test
    void DRAFT_아닌_강의를_삭제_요청_하면_409를_응답하고_강의가_삭제되지_않는다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));

        mockMvc.perform(delete("/api/klasses/{klassId}", klass.getId())
                        .header("X-Creator-Id", 강사.getId()))
                .andExpect(status().isConflict());

        assertThat(klassRepository.findById(klass.getId())).isPresent();
    }

    @Test
    void 다른_강사의_강의를_삭제_요청_하면_403을_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.초안_강의(강사));

        mockMvc.perform(delete("/api/klasses/{klassId}", klass.getId())
                        .header("X-Creator-Id", 다른_강사.getId()))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/creators/me/klasses ──────────────────────────────────────────

    @Test
    void 내_강의_목록_요청_하면_200을_응답하고_내_강의만_반환한다() throws Exception {
        klassRepository.save(KlassFixture.초안_강의(강사));
        klassRepository.save(KlassFixture.모집중_강의(강사));
        klassRepository.save(KlassFixture.초안_강의(다른_강사));

        mockMvc.perform(get("/api/creators/me/klasses")
                        .header("X-Creator-Id", 강사.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.klasses.length()").value(2))
                .andExpect(jsonPath("$.data.klasses[0].id").isNumber())
                .andExpect(jsonPath("$.data.klasses[0].title").isString())
                .andExpect(jsonPath("$.data.klasses[0].price").isNumber())
                .andExpect(jsonPath("$.data.klasses[0].status").isString())
                .andExpect(jsonPath("$.data.klasses[0].remainingCapacity").isNumber())
                .andExpect(jsonPath("$.data.klasses[0].startDate", nullValue()))
                .andExpect(jsonPath("$.data.klasses[0].endDate", nullValue()));
    }

    @Test
    void 내_강의_목록_status_필터_요청_하면_해당_상태의_강의만_반환한다() throws Exception {
        klassRepository.save(KlassFixture.초안_강의(강사));
        klassRepository.save(KlassFixture.모집중_강의(강사));

        mockMvc.perform(get("/api/creators/me/klasses")
                        .header("X-Creator-Id", 강사.getId())
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.klasses.length()").value(1))
                .andExpect(jsonPath("$.data.klasses[0].status").value("DRAFT"));
    }

    // ── GET /api/klasses/{klassId}/klassmates ─────────────────────────────────

    @Test
    void 수강생_목록_요청_하면_200을_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));

        mockMvc.perform(get("/api/klasses/{klassId}/klassmates", klass.getId())
                        .header("X-Creator-Id", 강사.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.klassmates").isArray())
                .andExpect(jsonPath("$.data.klassmates.length()").value(0));
    }

    @Test
    void 다른_강사의_강의_수강생_목록_요청_하면_403을_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));

        mockMvc.perform(get("/api/klasses/{klassId}/klassmates", klass.getId())
                        .header("X-Creator-Id", 다른_강사.getId()))
                .andExpect(status().isForbidden());
    }
}
