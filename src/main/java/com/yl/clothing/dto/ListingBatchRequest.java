package com.yl.clothing.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record ListingBatchRequest(
        @NotEmpty @Size(max = 500) List<@NotNull @Positive Long> ids,
        @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal price,
        Boolean onSale) {
}
