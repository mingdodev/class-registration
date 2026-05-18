package com.example.classregistration.domain.waitlist;

import com.example.classregistration.domain.creator.model.Creator;
import com.example.classregistration.domain.creator.repository.CreatorRepository;
import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klass.repository.KlassRepository;
import com.example.classregistration.domain.klassmate.model.Klassmate;
import com.example.classregistration.domain.klassmate.repository.KlassmateRepository;
import com.example.classregistration.domain.waitlist.model.Waitlist;
import com.example.classregistration.domain.waitlist.repository.WaitlistRepository;
import com.example.classregistration.fixture.CreatorFixture;
import com.example.classregistration.fixture.KlassFixture;
import com.example.classregistration.fixture.KlassmateFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WaitlistApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired WaitlistRepository waitlistRepository;
    @Autowired KlassRepository klassRepository;
    @Autowired KlassmateRepository klassmateRepository;
    @Autowired CreatorRepository creatorRepository;

    Creator 강사;
    Klassmate 수강생;

    @BeforeEach
    void setUp() {
        강사 = creatorRepository.save(CreatorFixture.강사());
        수강생 = klassmateRepository.save(KlassmateFixture.수강생());
    }

    // ── POST /api/klasses/{klassId}/waitlist ──────────────────────────────────

    @Test
    void CLOSED_강의에_대기열_등록_요청_하면_201을_응답하고_대기열이_저장된다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.수강_기간이_종료되지_않은_마감된_강의(강사));
        long countBefore = waitlistRepository.count();

        mockMvc.perform(post("/api/klasses/{klassId}/waitlist", klass.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data", nullValue()));

        assertThat(waitlistRepository.count()).isEqualTo(countBefore + 1);
    }

    @Test
    void CLOSED_아닌_강의에_대기열_등록_요청_하면_409를_응답하고_대기열이_저장되지_않는다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.모집중_강의(강사));
        long countBefore = waitlistRepository.count();

        mockMvc.perform(post("/api/klasses/{klassId}/waitlist", klass.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isConflict());

        assertThat(waitlistRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void 수강_기간이_종료된_강의에_대기열_등록_요청_하면_409를_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.수강_기간이_종료된_마감된_강의(강사));

        mockMvc.perform(post("/api/klasses/{klassId}/waitlist", klass.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void 이미_대기열에_등록된_강의에_다시_대기열_등록_요청_하면_409를_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.수강_기간이_종료되지_않은_마감된_강의(강사));
        waitlistRepository.save(Waitlist.create(수강생, klass));

        mockMvc.perform(post("/api/klasses/{klassId}/waitlist", klass.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isConflict());
    }

    // ── GET /api/klasses/{klassId}/waitlist/me ────────────────────────────────

    @Test
    void 대기열에_등록된_강의의_등록_여부_확인_요청_하면_registered_true를_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.수강_기간이_종료되지_않은_마감된_강의(강사));
        waitlistRepository.save(Waitlist.create(수강생, klass));

        mockMvc.perform(get("/api/klasses/{klassId}/waitlist/me", klass.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registered").value(true))
                .andExpect(jsonPath("$.data.createdAt").isString());
    }

    @Test
    void 대기열에_등록되지_않은_강의의_등록_여부_확인_요청_하면_registered_false를_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.수강_기간이_종료되지_않은_마감된_강의(강사));

        mockMvc.perform(get("/api/klasses/{klassId}/waitlist/me", klass.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registered").value(false))
                .andExpect(jsonPath("$.data.createdAt", nullValue()));
    }

    // ── DELETE /api/klasses/{klassId}/waitlist ────────────────────────────────

    @Test
    void 대기열_등록_취소_요청_하면_200을_응답하고_대기열이_삭제된다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.수강_기간이_종료되지_않은_마감된_강의(강사));
        waitlistRepository.save(Waitlist.create(수강생, klass));

        mockMvc.perform(delete("/api/klasses/{klassId}/waitlist", klass.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", nullValue()));

        assertThat(waitlistRepository.existsByKlassmateIdAndKlassId(수강생.getId(), klass.getId())).isFalse();
    }

    @Test
    void 등록되지_않은_대기열_취소_요청_하면_404를_응답한다() throws Exception {
        Klass klass = klassRepository.save(KlassFixture.수강_기간이_종료되지_않은_마감된_강의(강사));

        mockMvc.perform(delete("/api/klasses/{klassId}/waitlist", klass.getId())
                        .header("X-Klassmate-Id", 수강생.getId()))
                .andExpect(status().isNotFound());
    }
}
