package com.training.news.news;

import com.training.news.exception.NewsNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;

    public NewsService(NewsRepository newsRepository, NewsMapper newsMapper) {
        this.newsRepository = newsRepository;
        this.newsMapper = newsMapper;
    }

    public NewsResponse createNews(NewsRequest request) {
        News news = newsMapper.toEntity(request);
        return newsMapper.toResponse(newsRepository.save(news));
    }

    @Transactional(readOnly = true)
    public Page<NewsResponse> getNews(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportedAt"));
        return newsRepository.findAll(pageRequest).map(newsMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public NewsResponse getNewsById(Long newsId) {
        return newsMapper.toResponse(findNews(newsId));
    }

    public NewsResponse updateNews(Long newsId, NewsRequest request) {
        News existingNews = findNews(newsId);

        existingNews.setTitle(request.getTitle());
        existingNews.setDetails(request.getDetails());
        existingNews.setReportedBy(request.getReportedBy());
        existingNews.setUpdatedAt(LocalDateTime.now());

        News updatedNews = newsRepository.save(existingNews);

        return newsMapper.toResponse(updatedNews);
    }

    public void deleteNews(Long newsId) {
        News news = findNews(newsId);
        newsRepository.delete(news);
    }

    private News findNews(Long newsId) {
        return newsRepository.findById(newsId).orElseThrow(() -> new NewsNotFoundException(newsId));
    }
}
