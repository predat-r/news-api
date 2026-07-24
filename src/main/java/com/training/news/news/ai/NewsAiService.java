package com.training.news.news.ai;


import com.training.news.news.News;
import com.training.news.news.NewsService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class NewsAiService {

    private final ChatClient chatClient;
    private final NewsService newsService;
    private final MessageChatMemoryAdvisor chatMemoryAdvisor;
    private final NewsAiAsyncWorker newsAiAsyncWorker;

    public NewsAiService(ChatClient.Builder builder, NewsService newsService, ChatMemory chatMemory,
                         NewsAiAsyncWorker newsAiAsyncWorker) {
        this.chatClient = builder.build();
        this.newsService = newsService;
        this.chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();
        this.newsAiAsyncWorker = newsAiAsyncWorker;
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

    @PreAuthorize("""
            hasAnyRole('ADMIN', 'EDITOR', 'REPORTER')
            
            """)
    public CompletableFuture<AskNewsResponse> getAiGeneratedAnswer(Long newsId, String question,
                                                                   Authentication authentication) {

        News news = newsService.findNews(newsId);
        String chatId = newsId + ":" + authentication.getName();
        return newsAiAsyncWorker.answerQuestion(news.getTitle(), news.getDetails(), question, chatId);
    }


}
