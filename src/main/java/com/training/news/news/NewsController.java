package com.training.news.news;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<NewsResponse> createNews(@Valid @RequestBody NewsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(newsService.createNews(request));
    }

    @GetMapping
    public ResponseEntity<Page<NewsResponse>> getNews(@RequestParam(defaultValue = "0") @Min(value = 0, message = "Page number cannot be negative") int page, @RequestParam(defaultValue = "10") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 100, message = "Page size must not exceed 100") int size) {
        return ResponseEntity.ok(newsService.getNews(page, size));
    }

    @GetMapping("/{newsId}")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long newsId) {
        return ResponseEntity.ok(newsService.getNewsById(newsId));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{newsId}")
    public ResponseEntity<NewsResponse> updateNews(@PathVariable Long newsId, @Valid @RequestBody NewsRequest request) {
        return ResponseEntity.ok(newsService.updateNews(newsId, request));
    }

    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{newsId}")
    public ResponseEntity<Void> deleteNews(@PathVariable Long newsId) {
        newsService.deleteNews(newsId);
        return ResponseEntity.noContent().build();
    }
}
