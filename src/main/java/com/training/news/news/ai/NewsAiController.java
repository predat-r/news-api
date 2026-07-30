package com.training.news.news.ai;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/news")
public class NewsAiController {

    private final NewsAiService newsAiService;

    @Value("${app.ai.generation-enabled:true}")
    private boolean generationEnabled;

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{newsId}/summary")
    public ResponseEntity<NewsSummaryResponse> summary(@PathVariable Long newsId) {
        if (!generationEnabled) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        return ResponseEntity.ok(
                newsAiService.getAiGeneratedSummary(newsId)
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/ask")
    public CompletableFuture<ResponseEntity<AskNewsResponse>> askQuestion(
            @Valid @RequestBody AskNewsRequest request,
            Authentication authentication
    ) {
        if (!generationEnabled) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
            );
        }

        return newsAiService
                .getAiGeneratedAnswer(request.question(), authentication)
                .thenApply(ResponseEntity::ok);
    }
}