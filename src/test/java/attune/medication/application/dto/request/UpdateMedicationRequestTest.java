package attune.medication.application.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateMedicationRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new JsonNullableModule());

    @Test
    void explicitNullEndAtIsPresent() throws Exception {
        UpdateMedicationRequest request = objectMapper.readValue(
                "{\"endAt\":null,\"isActive\":true}",
                UpdateMedicationRequest.class
        );

        assertTrue(request.endAt().isPresent());
        assertNull(request.endAt().get());
    }

    @Test
    void omittedEndAtIsUndefined() throws Exception {
        UpdateMedicationRequest request = objectMapper.readValue(
                "{\"isActive\":true}",
                UpdateMedicationRequest.class
        );

        assertFalse(request.endAt().isPresent());
    }
}
