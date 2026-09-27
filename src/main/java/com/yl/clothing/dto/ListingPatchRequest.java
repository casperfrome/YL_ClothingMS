package com.yl.clothing.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ListingPatchRequest(
        @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal price,
        Boolean onSale) {
}
