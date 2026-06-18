package attune.common.config;

import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    private final JsonMapper objectMapper = JsonMapper.builder()
            .addModule(new JacksonConfig().jsonNullableModule())
            .build();

    @Test
    void jsonNullablePreservesGenericLocalDateType() throws Exception {
        TestRequest request = objectMapper.readValue(
                "{\"endAt\":\"2026-06-08\"}",
                TestRequest.class
        );

        assertThat(request.endAt().isPresent()).isTrue();
        assertThat(request.endAt().get()).isEqualTo(LocalDate.of(2026, 6, 8));
    }

    @Test
    void jsonNullableExplicitNullIsPresent() throws Exception {
        TestRequest request = objectMapper.readValue(
                "{\"endAt\":null}",
                TestRequest.class
        );

        assertThat(request.endAt().isPresent()).isTrue();
        assertThat(request.endAt().get()).isNull();
    }

    @Test
    void jsonNullableOmittedValueIsUndefined() throws Exception {
        TestRequest request = objectMapper.readValue(
                "{}",
                TestRequest.class
        );

        assertThat(request.endAt().isPresent()).isFalse();
    }

    record TestRequest(JsonNullable<LocalDate> endAt) {
        TestRequest {
            if (endAt == null) {
                endAt = JsonNullable.undefined();
            }
        }
    }
}
