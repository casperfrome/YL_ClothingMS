package com.yl.clothing.dto;

import jakarta.validation.constraints.*;

public record CategoryRequest(
        @NotBlank @Size(max = 60) String name) {
}
