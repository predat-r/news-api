package com.training.news.news.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskNewsRequest(
        @NotBlank(message = "question is required")
        @Size(min = 3, max = 500, message = "question must contain between 3 and 500 characters")
        String question
) {
}
