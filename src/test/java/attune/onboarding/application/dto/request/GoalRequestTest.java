package attune.onboarding.application.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoalRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void deserializesVisibleTagIds() throws Exception {
        GoalRequest request = objectMapper.readValue(requestJson("visibleTagIds"), GoalRequest.class);

        assertThat(request.visibleTagIds()).containsExactly(10L, 11L);
    }

    @Test
    void deserializesLegacyVisibleCatalogTagIds() throws Exception {
        GoalRequest request = objectMapper.readValue(
                requestJson("visibleCatalogTagIds"), GoalRequest.class);

        assertThat(request.visibleTagIds()).containsExactly(10L, 11L);
    }

    @Test
    void rejectsNullGoals() {
        GoalRequest request = new GoalRequest(null, null);

        assertThat(request.visibleTagIds()).isEmpty();
        assertThat(validator.validate(request))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("goals"));
    }

    private String requestJson(String tagIdsField) {
        return """
                {
                  "goals": [
                    {
                      "title": "할 일 3개 적기",
                      "type": "WORK_STUDY"
                    }
                  ],
                  "%s": [10, 11]
                }
                """.formatted(tagIdsField);
    }
}
