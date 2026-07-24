package com.training.news.news.ai;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AskNewsResponse> askQuestion(@PathVariable Long newsId,
                                                   @Valid @RequestBody AskNewsRequest askNewsRequest,
                                                   Authentication authentication) {
        AskNewsResponse askNewsResponse = newsAiService.getAiGeneratedAnswer(newsId,
                askNewsRequest.question(), authentication);
        return ResponseEntity.ok(askNewsResponse);
    }
}
