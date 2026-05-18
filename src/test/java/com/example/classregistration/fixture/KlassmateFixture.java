package com.example.classregistration.fixture;

import com.example.classregistration.domain.klassmate.model.Klassmate;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

public class KlassmateFixture {

    public static Klassmate 수강생() {
        Klassmate klassmate = BeanUtils.instantiateClass(Klassmate.class);
        ReflectionTestUtils.setField(klassmate, "name", "테스트수강생");
        ReflectionTestUtils.setField(klassmate, "email", "klassmate@test.com");
        ReflectionTestUtils.setField(klassmate, "phoneNumber", "010-1234-5678");
        return klassmate;
    }

    public static Klassmate 수강생(String email) {
        Klassmate klassmate = BeanUtils.instantiateClass(Klassmate.class);
        ReflectionTestUtils.setField(klassmate, "name", "테스트수강생");
        ReflectionTestUtils.setField(klassmate, "email", email);
        ReflectionTestUtils.setField(klassmate, "phoneNumber", "010-1234-5678");
        return klassmate;
    }
}
