package com.training.news.news;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewsAuthorization {

    private final NewsRepository newsRepository;

    public boolean isOwner(Long newsId, String username) {
        return newsRepository.existsByNewsIdAndReportedBy(newsId, username);
    }
}