package com.training.news.news;

import com.training.news.exception.NewsNotFoundException;
import com.training.news.news.ai.rag.NewsRagIndexer;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Transactional
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;
    private final NewsRagIndexer newsRagIndexer;

    public NewsService(NewsRepository newsRepository, NewsMapper newsMapper, NewsRagIndexer newsRagIndexer) {
        this.newsRepository = newsRepository;
        this.newsMapper = newsMapper;
        this.newsRagIndexer = newsRagIndexer;
    }


    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','REPORTER')")

    public NewsResponse createNews(NewsRequest request) {
        String username = Objects.requireNonNull(SecurityContextHolder.getContext()
                        .getAuthentication())
                .getName();
        News news = newsMapper.toEntity(request);
        news.setReportedBy(username);
        News savedNews = newsRepository.save(news);
        newsRagIndexer.indexNews(savedNews);
        return newsMapper.toResponse(savedNews);
    }

    @Transactional(readOnly = true)
    public Page<NewsResponse> getNews(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportedAt"));
        return newsRepository.findAll(pageRequest)
                .map(newsMapper::toResponse);
    }


    @Cacheable(cacheNames = "newsById", key = "#newsId")
    @Transactional(readOnly = true)
    public NewsResponse getNewsById(Long newsId) {
        return newsMapper.toResponse(findNews(newsId));
    }

    @PreAuthorize("""
            hasAnyRole('ADMIN', 'EDITOR')
            or (hasRole('REPORTER')
            and @newsAuthorization.isOwner(#newsId, authentication.name))
            """)

    @CacheEvict(cacheNames = {"newsById","aiGeneratedSummary"}, key = "#newsId")
    public NewsResponse updateNews(Long newsId, NewsRequest request) {
        News existingNews = findNews(newsId);

        existingNews.setTitle(request.getTitle());
        existingNews.setDetails(request.getDetails());
        existingNews.setUpdatedAt(LocalDateTime.now());

        News updatedNews = newsRepository.save(existingNews);
        newsRagIndexer.indexNews(updatedNews);
        return newsMapper.toResponse(updatedNews);
    }

    @PreAuthorize("""
            hasAnyRole('ADMIN', 'EDITOR')
            or (hasRole('REPORTER')
            and @newsAuthorization.isOwner(#newsId, authentication.name))
            """)

    @CacheEvict(cacheNames = {"newsById","aiGeneratedSummary"}, key = "#newsId")
    public void deleteNews(Long newsId) {
        News news = findNews(newsId);
        newsRepository.delete(news);
        newsRagIndexer.deleteNewsIndex(newsId);
    }

    public News findNews(Long newsId) {
        return newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsNotFoundException(newsId));
    }
}
