package com.yl.clothing.dto;

import jakarta.validation.constraints.*;

public record ProductPatchRequest(
        @Pattern(regexp = ".*\\S.*", message = "不能全部为空白") @Size(max = 100) String name,
        @Pattern(regexp = ".*\\S.*", message = "不能全部为空白") @Size(max = 40) String code,
        @Positive Long categoryId,
        @Size(max = 10000) String description) {
}
