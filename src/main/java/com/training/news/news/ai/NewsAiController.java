package com.training.news.news.ai;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/news")
public class NewsAiController {
    private final NewsAiService newsAiService;

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{newsId}/summary")
    public ResponseEntity<NewsSummaryResponse> summary(@PathVariable Long newsId) {
        NewsSummaryResponse newsSummaryResponse = newsAiService.getAiGeneratedSummary(newsId);
        return ResponseEntity.ok(newsSummaryResponse);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{newsId}/ask")
    public CompletableFuture<ResponseEntity<AskNewsResponse>> askQuestion(@PathVariable Long newsId,
                                                                          @Valid @RequestBody AskNewsRequest askNewsRequest,
                                                                          Authentication authentication) {
        return newsAiService.getAiGeneratedAnswer(newsId,
                        askNewsRequest.question(), authentication)
                .thenApply(ResponseEntity::ok);
    }
}
