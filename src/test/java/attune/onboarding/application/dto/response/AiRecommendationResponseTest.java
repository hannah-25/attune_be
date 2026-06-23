package attune.onboarding.application.dto.response;

import attune.journal.domain.model.DailyGoalType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiRecommendationResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesTagIdentifierAsTagId() throws Exception {
        AiRecommendationResponse response = new AiRecommendationResponse(
                List.of(new AiRecommendationResponse.TagItem(
                        10L, "깜빡함", "INATTENTION", true)),
                List.of(new AiRecommendationResponse.GoalItem(
                        "할 일 3개 적기", DailyGoalType.WORK_STUDY))
        );

        JsonNode tag = objectMapper.readTree(objectMapper.writeValueAsString(response))
                .path("tags")
                .get(0);

        assertThat(tag.path("tagId").asLong()).isEqualTo(10L);
        assertThat(tag.has("id")).isFalse();
    }
}
