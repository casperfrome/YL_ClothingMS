package com.yl.clothing.dto;

import jakarta.validation.constraints.*;

public record ColorPatchRequest(
        @NotBlank @Size(max = 40) String name,
        @NotNull @Pattern(regexp = "#[0-9a-fA-F]{6}") String hex1,
        @Pattern(regexp = "#[0-9a-fA-F]{6}") String hex2) {
}
