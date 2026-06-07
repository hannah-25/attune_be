package attune.common.config;

import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {

    @Bean
    public JacksonModule jsonNullableModule() {
        SimpleModule module = new SimpleModule("JsonNullableModule");
        module.addDeserializer(JsonNullable.class, new JsonNullableDeserializer(null));
        return module;
    }

    static class JsonNullableDeserializer extends StdDeserializer<JsonNullable<?>> {

        private final JavaType containedType;

        JsonNullableDeserializer(JavaType containedType) {
            super(JsonNullable.class);
            this.containedType = containedType;
        }

        @Override
        public JsonNullable<?> deserialize(JsonParser p, DeserializationContext ctxt) {
            Object value = (containedType != null)
                    ? ctxt.readValue(p, containedType)
                    : p.readValueAs(Object.class);
            return JsonNullable.of(value);
        }

        @Override
        public JsonNullable<?> getNullValue(DeserializationContext ctxt) {
            return JsonNullable.of(null);
        }

        @Override
        public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
            if (property != null) {
                JavaType propType = property.getType();
                if (propType != null && propType.hasGenericTypes()) {
                    return new JsonNullableDeserializer(propType.containedType(0));
                }
            }
            return this;
        }
    }
}
