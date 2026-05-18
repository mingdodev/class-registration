package com.example.classregistration.fixture;

import com.example.classregistration.domain.creator.model.Creator;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

public class CreatorFixture {

    public static Creator 강사() {
        Creator creator = BeanUtils.instantiateClass(Creator.class);
        ReflectionTestUtils.setField(creator, "name", "테스트강사");
        ReflectionTestUtils.setField(creator, "email", "creator@test.com");
        return creator;
    }

    public static Creator 강사(String email) {
        Creator creator = BeanUtils.instantiateClass(Creator.class);
        ReflectionTestUtils.setField(creator, "name", "테스트강사");
        ReflectionTestUtils.setField(creator, "email", email);
        return creator;
    }
}
