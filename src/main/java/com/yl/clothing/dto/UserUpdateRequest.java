package com.yl.clothing.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record UserUpdateRequest(
        @NotNull @Pattern(regexp = "[0-9]{11}", message = "手机号须为 11 位数字") String phone,
        @NotBlank @Size(max = 40) String nickname,
        @NotNull @Pattern(regexp = "UNKNOWN|MALE|FEMALE") String gender,
        @PastOrPresent(message = "生日不能晚于今天") LocalDate birthday) {
}
