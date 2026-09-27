package com.yl.clothing.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ColorCreateRequest(
        @NotBlank @Size(max = 40) String name,
        @NotNull @Pattern(regexp = "#[0-9a-fA-F]{6}") String hex1,
        @Pattern(regexp = "#[0-9a-fA-F]{6}") String hex2,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal initialPrice) {
}
