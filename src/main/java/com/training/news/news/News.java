package com.training.news.news;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long newsId;

	@NotBlank(message = "Title is required")
	@Size(max = 150, message = "Title must not exceed 150 characters")
	private String title;

	@NotBlank(message = "Details are required")
	@Size(max = 5000, message = "Details must not exceed 5000 characters")
	private String details;

	@NotBlank(message = "Reporter is required")
	@Size(max = 100, message = "Reporter must not exceed 100 characters")
	private String reportedBy;

	@NotNull(message = "Reported time is required")
	@PastOrPresent(message = "Reported time cannot be in the future")
	private LocalDateTime reportedAt;

	@NotNull(message = "Updated time is required")
	@PastOrPresent(message = "Updated time cannot be in the future")
	private LocalDateTime updatedAt;
}
