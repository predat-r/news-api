package com.training.news.news;

import java.util.Set;

public record NewsSummaryResponse(String summary, Set<String> keywords) {
}
