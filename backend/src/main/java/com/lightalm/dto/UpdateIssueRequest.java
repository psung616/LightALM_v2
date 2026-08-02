package com.lightalm.dto;

import com.lightalm.domain.IssueType;
import com.lightalm.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateIssueRequest {

    @NotBlank(message = "title은 필수입니다.")
    @Size(max = 255)
    private String title;

    private String description;

    @NotNull(message = "type은 필수입니다.")
    private IssueType type;

    private Priority priority;

    private Long assigneeId;

    private LocalDate dueDate;
}
