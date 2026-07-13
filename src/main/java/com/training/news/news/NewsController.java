package com.training.news.news;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {

	private final NewsService newsService;


	@PostMapping
	public ResponseEntity<NewsResponse> createNews(@Valid @RequestBody NewsRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(newsService.createNews(request));
	}

	@GetMapping
	public ResponseEntity<Page<NewsResponse>> getNews(
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "Page number cannot be negative") int page,
			@RequestParam(defaultValue = "10") @Min(value = 1, message = "Page size must be at least 1")
			@Max(value = 100, message = "Page size must not exceed 100") int size) {
		return ResponseEntity.ok(newsService.getNews(page, size));
	}

	@GetMapping("/{newsId}")
	public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long newsId) {
		return ResponseEntity.ok(newsService.getNewsById(newsId));
	}

	@PutMapping("/{newsId}")
	public ResponseEntity<NewsResponse> updateNews(@PathVariable Long newsId, @Valid @RequestBody NewsRequest request) {
		return ResponseEntity.ok(newsService.updateNews(newsId, request));
	}

	@DeleteMapping("/{newsId}")
	public ResponseEntity<Void> deleteNews(@PathVariable Long newsId) {
		newsService.deleteNews(newsId);
		return ResponseEntity.noContent().build();
	}
}
