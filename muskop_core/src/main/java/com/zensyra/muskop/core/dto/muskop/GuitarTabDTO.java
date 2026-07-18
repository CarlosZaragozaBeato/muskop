package com.zensyra.muskop.core.dto.muskop;

import java.util.List;

public record GuitarTabDTO(
        MetadataDTO metadata,
        List<MeasureDTO> measures
) { }

record MetadataDTO(
        String title,
        List<String> tuning,
        String timeSignature,
        Integer baseBpm
) {}

record MeasureDTO(
        Integer index,
        List<EventDTO> events
) {}

record EventDTO(
        Double beat,
        String duration,
        List<NoteDTO> notes
) {}

record NoteDTO(
        Integer string,
        Integer fret
) {}
