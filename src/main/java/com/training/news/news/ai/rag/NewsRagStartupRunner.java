package com.training.news.news.ai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.JsonMetadataGenerator;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty("app.rag.import-on-startup")
public class NewsRagStartupRunner implements ApplicationRunner {

    private final Resource resource;
    private final VectorStore vectorStore;

    public NewsRagStartupRunner(@Value("classpath:/data/news_data.json") Resource resource,
                                VectorStore vectorStore) {
        this.resource = resource;
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        JsonMetadataGenerator jsonMetadataGenerator = jsonItem -> Map.of("author", jsonItem.get("author"),
                "url", jsonItem.get("url"));
        JsonReader jsonReader = new JsonReader(resource, jsonMetadataGenerator, "title", "details");
        List<Document> newsDocuments = jsonReader.get();
        List<Document> newsDocumentsWithStableIds = newsDocuments.stream().map(this::withStableId).toList();
        vectorStore.add(newsDocumentsWithStableIds);

    }

    private Document withStableId(Document document) {
        String url = document.getMetadata()
                .get("url")
                .toString();
        byte[] idBytes = url.getBytes(StandardCharsets.UTF_8);
        UUID stableId = UUID.nameUUIDFromBytes(idBytes);
        return Document.builder()
                .id(stableId.toString())
                .metadata(document.getMetadata())
                .text(document.getText()).build();
    }
}
