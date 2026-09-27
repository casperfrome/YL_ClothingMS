package com.yl.clothing.vo;

import java.time.LocalDateTime;
import java.time.LocalDate;

public record UserVO(
        Long id,
        String phone,
        String nickname,
        String gender,
        LocalDate birthday,
        LocalDateTime createdAt,
        Long channelCount,
        Long orderCount) {
}
