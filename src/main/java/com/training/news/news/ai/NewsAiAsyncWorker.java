package com.training.news.news.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class NewsAiAsyncWorker {
    private final ChatClient chatClient;
    private final MessageChatMemoryAdvisor chatMemoryAdvisor;

    public NewsAiAsyncWorker(ChatClient.Builder builder,ChatMemory chatMemory) {
        this.chatClient = builder.build();
        this.chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();
    }

    @Async("aiTaskExecutor")
    public  CompletableFuture<AskNewsResponse> answerQuestion(String title, String details,String question, String chatId){
        AskNewsResponse askNewsResponse =  chatClient.prompt()
                .advisors(advisor -> advisor.advisors(chatMemoryAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, chatId))
                .system("You are a news editor and are ordered to answer the user query using only the provided news article")
                .user(user -> user.text("""
                                News title:{title}
                                
                                News details:
                                {details}
                                
                                User question:
                                {question}
                                """)
                        .param("title", title)
                        .param("details", details)
                        .param("question", question))
                .call()
                .entity(AskNewsResponse.class);

        return CompletableFuture.completedFuture(askNewsResponse);
    }
}
