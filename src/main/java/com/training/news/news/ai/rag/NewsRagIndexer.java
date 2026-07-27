package com.training.news.news.ai.rag;

import com.training.news.news.News;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NewsRagIndexer {
    private final VectorStore vectorStore;


    public NewsRagIndexer(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }


    @Async("aiTaskExecutor")
    public void indexNews(News news) {
        String stableKey = "news:" + news.getNewsId();
        byte[] idBytes = stableKey.getBytes(StandardCharsets.UTF_8);
        UUID id = UUID.nameUUIDFromBytes(idBytes);
        Map<String, Object> metadata = Map.of("newsId", news.getNewsId(), "title", news.getTitle(),
                "reportedBy", news.getReportedBy(), "documentType", "news");
        String content = """
                Title: %s\s
                \s
                Details:
                 %s
               \s""".formatted(news.getTitle(), news.getDetails());
        Document document = Document.builder()
                .id(id.toString())
                .text(content)
                .metadata(metadata)
                .build();
        vectorStore.add(List.of(document));
    }
}
