package com.lightalm.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReleaseNotesResponse {
    private Long releaseId;
    private String version;
    private String notes;
}
