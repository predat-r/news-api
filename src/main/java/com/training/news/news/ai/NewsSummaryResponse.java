package com.training.news.news.ai;

import java.util.Set;

public record NewsSummaryResponse(String summary, Set<String> keywords) {
}
