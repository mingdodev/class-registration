package com.example.classregistration.fixture;

import com.example.classregistration.domain.klass.dto.CreateKlassRequest;
import com.example.classregistration.domain.klass.dto.UpdateKlassRequest;

public class KlassRequestFixture {

    public static CreateKlassRequest 유효한_강의_생성_요청() {
        return new CreateKlassRequest("테스트 강의", "강의 설명", 50000, 20, null, null);
    }

    public static CreateKlassRequest 강의명이_20자_초과인_생성_요청() {
        return new CreateKlassRequest("이강의이름은스무자를초과하는아주긴강의이름입니다", "설명", 50000, 20, null, null);
    }

    public static CreateKlassRequest 수강_정원이_0인_생성_요청() {
        return new CreateKlassRequest("테스트 강의", "설명", 50000, 0, null, null);
    }

    public static UpdateKlassRequest 제목_수정_요청(String title) {
        return new UpdateKlassRequest(title, null, null, null, null, null);
    }

    public static UpdateKlassRequest 강의명이_20자_초과인_수정_요청() {
        return new UpdateKlassRequest("이강의이름은스무자를초과하는아주긴강의이름입니다", null, null, null, null, null);
    }

    public static UpdateKlassRequest 가격_수정_요청(int price) {
        return new UpdateKlassRequest(null, null, price, null, null, null);
    }

    public static UpdateKlassRequest 정원_감소_수정_요청(int maxCapacity) {
        return new UpdateKlassRequest(null, null, null, maxCapacity, null, null);
    }

    public static UpdateKlassRequest 정원_증가_수정_요청(int maxCapacity) {
        return new UpdateKlassRequest(null, null, null, maxCapacity, null, null);
    }

    public static UpdateKlassRequest 수강_기간_수정_요청() {
        return new UpdateKlassRequest(null, null, null, null,
                java.time.LocalDate.now().plusDays(10), java.time.LocalDate.now().plusDays(40));
    }
}
