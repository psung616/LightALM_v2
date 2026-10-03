package com.lightalm.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * ADR-009: projectKey 검증 제약 완화(^[A-Z][A-Z0-9_]{2,19}$) 검증.
 */
class CreateProjectRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"LALM", "PROJECT_2", "MY_APP_V2", "A2Z_TEAM_BACKEND_01", "ABC", "ABCDEFGHIJKLMNOPQRST"})
    void validProjectKeys_passValidation(String projectKey) {
        CreateProjectRequest request = newRequest(projectKey);

        Set<ConstraintViolation<CreateProjectRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1ABC",                  // 숫자로 시작
        "_ABC",                  // 언더바로 시작
        "AB",                    // 2자 (하한 미만)
        "ABCDEFGHIJKLMNOPQRSTU", // 21자 (상한 초과)
        "abc",                   // 소문자
        "AB-CD",                 // 허용되지 않는 특수문자
    })
    void invalidProjectKeys_failValidation(String projectKey) {
        CreateProjectRequest request = newRequest(projectKey);

        Set<ConstraintViolation<CreateProjectRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    private static CreateProjectRequest newRequest(String projectKey) {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setProjectKey(projectKey);
        request.setName("Test Project");
        return request;
    }
}
