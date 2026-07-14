package com.training.news.news;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsRequest {

	@NotBlank(message = "Title is required")
	@Size(max = 150, message = "Title must not exceed 150 characters")
	private String title;

	@NotBlank(message = "Details are required")
	@Size(max = 5000, message = "Details must not exceed 5000 characters")
	private String details;

}
