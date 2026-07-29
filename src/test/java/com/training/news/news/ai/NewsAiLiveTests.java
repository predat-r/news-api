package com.training.news.news.ai;

import com.training.news.news.News;
import com.training.news.news.NewsRepository;
import com.training.news.security.api_user.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("live-ai")
class NewsAiLiveTests {

    private static final String NEWS_API = "/api/v1/news";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private VectorStore vectorStore;

    private Long savedNewsId;
    private String indexedDocumentId;

    @AfterEach
    void cleanUpFixtures() {
        if (savedNewsId != null) {
            newsRepository.deleteById(savedNewsId);
        }
        if (indexedDocumentId != null) {
            vectorStore.delete(List.of(indexedDocumentId));
        }
    }

    @Test
    void summaryReturnsGeneratedSummary() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        News news = newsRepository.saveAndFlush(News.builder()
                .title("City opens solar-powered public library")
                .details("""
                        The city opened a new public library powered entirely by rooftop solar panels.
                        The building includes study rooms, a children's section, and free computer access.
                        Officials expect the library to serve more than ten thousand residents each month.
                        """)
                .reportedBy("reporter1")
                .reportedAt(now)
                .updatedAt(now)
                .build());
        savedNewsId = news.getNewsId();

        mockMvc.perform(get(NEWS_API + "/{newsId}/summary", savedNewsId)
                        .with(authenticatedAs("editor1", Role.EDITOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").isNotEmpty())
                .andExpect(jsonPath("$.keywords").isArray())
                .andExpect(jsonPath("$.keywords").isNotEmpty());
    }

    @Test
    void askReturnsGeneratedAnswer() throws Exception {
        indexedDocumentId = UUID.randomUUID().toString();
        vectorStore.add(List.of(Document.builder()
                .id(indexedDocumentId)
                .text("""
                        Project Aurora status report:
                        The launch status color for Project Aurora is blue.
                        """)
                .metadata("documentType", "news")
                .build()));

        MvcResult result = mockMvc.perform(post(NEWS_API + "/ask")
                        .with(authenticatedAs("reporter1", Role.REPORTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "What color is Project Aurora's launch status?"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        result.getAsyncResult(60_000);

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isNotEmpty())
                .andExpect(jsonPath("$.answer").value(
                        org.hamcrest.Matchers.containsStringIgnoringCase("blue")));
    }

    private RequestPostProcessor authenticatedAs(String username, Role role) {
        return jwt()
                .jwt(token -> token.subject(username))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
