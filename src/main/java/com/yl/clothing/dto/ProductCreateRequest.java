package com.yl.clothing.dto;

import jakarta.validation.constraints.*;

public record ProductCreateRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 100) String name,
        @NotNull @Positive Long categoryId,
        @Size(max = 10000) String description) {
}
