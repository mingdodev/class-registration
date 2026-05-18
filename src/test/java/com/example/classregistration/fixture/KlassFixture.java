package com.example.classregistration.fixture;

import com.example.classregistration.domain.creator.model.Creator;
import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klass.model.KlassStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

public class KlassFixture {

    public static Klass 초안_강의(Creator creator) {
        return Klass.create(creator, "테스트 강의", "강의 설명", 50000, 20, null, null);
    }

    public static Klass 모집중_강의(Creator creator) {
        Klass klass = 초안_강의(creator);
        ReflectionTestUtils.setField(klass, "status", KlassStatus.OPEN);
        return klass;
    }

    public static Klass 마감된_강의(Creator creator) {
        Klass klass = 초안_강의(creator);
        ReflectionTestUtils.setField(klass, "status", KlassStatus.CLOSED);
        ReflectionTestUtils.setField(klass, "remainingCapacity", 0);
        return klass;
    }

    public static Klass 수강_기간이_종료된_모집중_강의(Creator creator) {
        LocalDate endDate = LocalDate.now().minusDays(1);
        Klass klass = Klass.create(
                creator, "종료된 강의", "강의 설명", 50000, 20,
                endDate.minusDays(60), endDate
        );
        ReflectionTestUtils.setField(klass, "status", KlassStatus.OPEN);
        return klass;
    }

    public static Klass 수강_기간이_종료된_마감된_강의(Creator creator) {
        LocalDate endDate = LocalDate.now().minusDays(1);
        Klass klass = Klass.create(
                creator, "종료된 강의", "강의 설명", 50000, 20,
                endDate.minusDays(60), endDate
        );
        ReflectionTestUtils.setField(klass, "status", KlassStatus.CLOSED);
        ReflectionTestUtils.setField(klass, "remainingCapacity", 0);
        return klass;
    }

    public static Klass 시작일이_5일_후인_강의(Creator creator) {
        LocalDate startDate = LocalDate.now().plusDays(5);
        Klass klass = Klass.create(
                creator, "기간 강의", "강의 설명", 50000, 20,
                startDate, startDate.plusDays(30)
        );
        ReflectionTestUtils.setField(klass, "status", KlassStatus.OPEN);
        return klass;
    }

    public static Klass 시작일이_2일_후인_강의(Creator creator) {
        LocalDate startDate = LocalDate.now().plusDays(2);
        Klass klass = Klass.create(
                creator, "기간 강의", "강의 설명", 50000, 20,
                startDate, startDate.plusDays(30)
        );
        ReflectionTestUtils.setField(klass, "status", KlassStatus.OPEN);
        return klass;
    }
}
