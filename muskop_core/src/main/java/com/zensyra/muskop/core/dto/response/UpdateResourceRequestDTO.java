package com.zensyra.muskop.core.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

public record UpdateResourceRequestDTO(
        String title,
        String type,
        String category,
        JsonNode content
) {}
