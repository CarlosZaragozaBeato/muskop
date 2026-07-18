package com.zensyra.muskop.core.dto.muskop;

import com.fasterxml.jackson.databind.JsonNode;

public record ResourceDetailDTO(
        Integer id,
        String title,
        String type,
        String category,
        JsonNode content
) {}
