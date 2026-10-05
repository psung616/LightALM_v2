package com.lightalm.customfield.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BulkSaveCustomFieldValuesRequest(
        @NotNull(message = "values는 필수입니다.") @Valid List<CustomFieldValueItem> values
) {
}
