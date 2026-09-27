package com.yl.clothing.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.yl.clothing.config.StrictBooleanDeserializer;
import jakarta.validation.constraints.NotNull;

public record SimulatorRetentionRequest(
        @NotNull @JsonDeserialize(using = StrictBooleanDeserializer.class) Boolean retainData) {
}
