package com.training.news.news;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NewsMapper {

	public News toEntity(NewsRequest request) {
		LocalDateTime now = LocalDateTime.now();
		return News.builder()
				.title(request.getTitle())
				.details(request.getDetails())
				.reportedAt(now)
				.updatedAt(now)
				.build();
	}

	public NewsResponse toResponse(News news) {
		return NewsResponse.builder()
				.newsId(news.getNewsId())
				.title(news.getTitle())
				.details(news.getDetails())
				.reportedBy(news.getReportedBy())
				.reportedAt(news.getReportedAt())
				.updatedAt(news.getUpdatedAt())
				.build();
	}
}
