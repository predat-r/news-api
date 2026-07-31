package com.training.news.news.ai.rag;

import com.training.news.news.News;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NewsRagIndexer {

    private final VectorStore vectorStore;
    private final boolean indexingEnabled;

    public NewsRagIndexer(VectorStore vectorStore,
                          @Value("${app.rag.indexing-enabled:true}") boolean indexingEnabled) {
        this.vectorStore = vectorStore;
        this.indexingEnabled = indexingEnabled;
    }

    @Async("aiTaskExecutor")
    public void indexNews(News news) {
        if (!indexingEnabled) {
            return;
        }

        Map<String, Object> metadata = Map.of("newsId", news.getNewsId(), "title", news.getTitle(),
                "reportedBy", news.getReportedBy(), "documentType", "news");

        String content = "Title: %s%n%nDetails:%n%s%n".formatted(news.getTitle(), news.getDetails());


        UUID id = getStableId(news.getNewsId());

        Document document = Document.builder()
                .id(id.toString())
                .text(content)
                .metadata(metadata)
                .build();

        vectorStore.add(List.of(document));
    }

    @Async("aiTaskExecutor")
    public void deleteNewsIndex(Long newsId) {
        if (!indexingEnabled) {
            return;
        }

        vectorStore.delete(List.of(getStableId(newsId).toString()));
    }

    private UUID getStableId(Long newsId) {
        String stableKey = "news:" + newsId;
        byte[] idBytes = stableKey.getBytes(StandardCharsets.UTF_8);
        return UUID.nameUUIDFromBytes(idBytes);
    }
}