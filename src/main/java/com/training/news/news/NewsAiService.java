package com.training.news.news;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class NewAiService {

    private  final ChatClient chatClient;


    public NewAiService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }
}
