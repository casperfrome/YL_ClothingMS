package com.yl.clothing.dto;

import jakarta.validation.constraints.*;

public record SimulatorToggleRequest(
        @NotNull Boolean enabled) {
}
