package com.yl.clothing.vo;


import java.util.List;

public record ColorDetailVO(
        Long id,
        Long productId,
        String name,
        String hex1,
        String hex2,
        List<ImageVO> images) {
}
