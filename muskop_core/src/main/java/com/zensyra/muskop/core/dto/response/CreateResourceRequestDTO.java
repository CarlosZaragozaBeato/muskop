package com.zensyra.muskop.core.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

public record CreateResourceRequestDTO(
        String title,
        String type,
        String category,
        Integer userId,
        JsonNode content
) {}
