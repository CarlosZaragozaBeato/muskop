package com.zensyra.muskop.core.dto.http;


import com.zensyra.muskop.core.dto.muskop.GuitarTabDTO;

public record CreateResourceRequestDTO(
        String title,
        String type,
        Long userId,
        GuitarTabDTO tab
) {}