package com.yl.clothing.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record ImageOrderRequest(
        @NotNull @Size(max = 100) List<@NotNull @Positive Long> ids) {
}
