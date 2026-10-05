package com.lightalm.formlayout.dto;

import java.util.List;

public record FormLayoutSectionResponse(
        Long id,
        String title,
        Integer displayOrder,
        List<FormLayoutFieldResponse> fields
) {
}
