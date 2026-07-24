package com.training.news.news;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class NewsAiService {

    private final ChatClient chatClient;
    private final NewsService newsService;


    public NewsAiService(ChatClient.Builder builder, NewsService newsService) {
        this.chatClient = builder.build();
        this.newsService = newsService;
    }

    public NewsSummaryResponse generateSummary(News news) {
        return chatClient.prompt()
                .system("You are a news editor and are ordered to generate a 400-500 character summary for following news piece")
                .user(user -> user.text("""
                                News title:{title}
                                
                                News details:
                                {details}
                                """)
                        .param("title", news.getTitle())
                        .param("details", news.getDetails()))
                .call()
                .entity(NewsSummaryResponse.class);
    }

    @PreAuthorize("""
            hasAnyRole('ADMIN', 'EDITOR')
            or (hasRole('REPORTER')
            and @newsAuthorization.isOwner(#newsId, authentication.name))
            """)
    public NewsSummaryResponse getAiGeneratedSummary(Long newsId) {
        News news = newsService.findNews(newsId);
        return generateSummary(news);

    }

}
