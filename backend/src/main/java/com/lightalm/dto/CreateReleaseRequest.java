package com.lightalm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReleaseRequest {

    @NotBlank(message = "version은 필수입니다.")
    @Size(max = 50)
    private String version;

    private String name;

    private LocalDate releaseDate;

    private String description;
}
