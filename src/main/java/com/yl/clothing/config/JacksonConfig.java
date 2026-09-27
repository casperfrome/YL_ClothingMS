package com.yl.clothing.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer apiSerialization() {
        return builder -> {
            builder.serializationInclusion(JsonInclude.Include.ALWAYS);
            // Keep the existing wire format; financial values are JSON strings, not numbers.
            builder.serializerByType(BigDecimal.class, new JsonSerializer<BigDecimal>() {
                @Override
                public void serialize(BigDecimal value, JsonGenerator generator, SerializerProvider provider) throws IOException {
                    generator.writeString(value.setScale(2).toPlainString());
                }
            });
            builder.serializerByType(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public void serialize(LocalDateTime value, JsonGenerator generator, SerializerProvider provider) throws IOException {
                    generator.writeString(value.atOffset(ZoneOffset.ofHours(8)).toString());
                }
            });
            builder.serializerByType(LocalDate.class, new JsonSerializer<LocalDate>() {
                @Override
                public void serialize(LocalDate value, JsonGenerator generator, SerializerProvider provider) throws IOException {
                    generator.writeString(value.toString());
                }
            });
        };
    }
}
