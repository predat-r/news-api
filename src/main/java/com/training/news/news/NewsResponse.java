package com.training.news.news;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsResponse {

	private Long newsId;
	private String title;
	private String details;
	private String reportedBy;
	private LocalDateTime reportedAt;
	private LocalDateTime updatedAt;
}
