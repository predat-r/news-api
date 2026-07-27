package com.training.news.news.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class NewsAiAsyncWorker {
    private final ChatClient chatClient;
    private final MessageChatMemoryAdvisor chatMemoryAdvisor;
    private final StructuredOutputValidationAdvisor structuredOutputValidationAdvisor;

    private final QuestionAnswerAdvisor questionAnswerAdvisor;

    public NewsAiAsyncWorker(ChatClient.Builder builder, ChatMemory chatMemory, VectorStore vectorStore) {
        this.chatClient = builder.build();
        this.chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();
        this.structuredOutputValidationAdvisor = StructuredOutputValidationAdvisor.builder()
                .outputType(AskNewsResponse.class)
                .build();

        this.questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .build();
    }

    @Async("aiTaskExecutor")
    public CompletableFuture<AskNewsResponse> answerQuestion(String question, String chatId) {
        AskNewsResponse askNewsResponse = chatClient.prompt()
                .advisors(advisor -> advisor.advisors(chatMemoryAdvisor, questionAnswerAdvisor,
                                structuredOutputValidationAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, chatId))
                .system("You are a news assistant and are ordered to answer using only the news context retrieved from the knowledge base. If the retrieved context does not contain the answer, clearly say so.")
                .user(question)
                .call()
                .entity(AskNewsResponse.class);

        return CompletableFuture.completedFuture(askNewsResponse);
    }
}
