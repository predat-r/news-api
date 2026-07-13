package com.training.news.news;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NewsEndpointTests {

    private static final String NEWS_API = "/api/v1/news";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createNewsReturnsCreatedNews() throws Exception {
        mockMvc.perform(post(NEWS_API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validNewsRequest("Endpoint create title", "Endpoint create details", "Reporter A")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newsId").isNumber())
                .andExpect(jsonPath("$.title").value("Endpoint create title"))
                .andExpect(jsonPath("$.details").value("Endpoint create details"))
                .andExpect(jsonPath("$.reportedBy").value("Reporter A"))
                .andExpect(jsonPath("$.reportedAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void getNewsReturnsPaginatedNews() throws Exception {
        createNews("Endpoint list title", "Endpoint list details", "Reporter B");

        mockMvc.perform(get(NEWS_API)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].newsId").isNumber())
                .andExpect(jsonPath("$.page.totalElements").isNumber())
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    void getNewsByIdReturnsNews() throws Exception {
        JsonNode createdNews = createNews("Endpoint get title", "Endpoint get details", "Reporter C");
        long newsId = createdNews.get("newsId").asLong();

        mockMvc.perform(get(NEWS_API + "/{newsId}", newsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsId").value(newsId))
                .andExpect(jsonPath("$.title").value("Endpoint get title"))
                .andExpect(jsonPath("$.details").value("Endpoint get details"))
                .andExpect(jsonPath("$.reportedBy").value("Reporter C"));
    }

    @Test
    void updateNewsUpdatesAllowedFieldsAndUpdatedAtOnly() throws Exception {
        JsonNode createdNews = createNews("Before update title", "Before update details", "Reporter D");
        long newsId = createdNews.get("newsId").asLong();
        LocalDateTime reportedAt = LocalDateTime.parse(createdNews.get("reportedAt").asString());
        LocalDateTime oldUpdatedAt = LocalDateTime.parse(createdNews.get("updatedAt").asString());

        MvcResult result = mockMvc.perform(put(NEWS_API + "/{newsId}", newsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validNewsRequest("After update title", "After update details", "Reporter E")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsId").value(newsId))
                .andExpect(jsonPath("$.title").value("After update title"))
                .andExpect(jsonPath("$.details").value("After update details"))
                .andExpect(jsonPath("$.reportedBy").value("Reporter E"))
                .andReturn();

        JsonNode updatedNews = objectMapper.readTree(result.getResponse().getContentAsString());
        LocalDateTime updatedReportedAt = LocalDateTime.parse(updatedNews.get("reportedAt").asString());
        LocalDateTime newUpdatedAt = LocalDateTime.parse(updatedNews.get("updatedAt").asString());
        assertSameTimestamp(reportedAt, updatedReportedAt);
        assertThat(newUpdatedAt).isAfterOrEqualTo(oldUpdatedAt);
    }

    @Test
    void updateNewsReturnsNotFoundWhenNewsDoesNotExist() throws Exception {
        long missingNewsId = 999999L;

        mockMvc.perform(put(NEWS_API + "/{newsId}", missingNewsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validNewsRequest("Missing update title", "Missing update details", "Reporter X")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("News not found with id: " + missingNewsId));
    }

    @Test
    void deleteNewsRemovesNews() throws Exception {
        JsonNode createdNews = createNews("Endpoint delete title", "Endpoint delete details", "Reporter F");
        long newsId = createdNews.get("newsId").asLong();

        mockMvc.perform(delete(NEWS_API + "/{newsId}", newsId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(NEWS_API + "/{newsId}", newsId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("News not found with id: " + newsId));
    }

    @Test
    void invalidCreateRequestReturnsValidationErrors() throws Exception {
        mockMvc.perform(post(NEWS_API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "details": "",
                                  "reportedBy": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.title").value("Title is required"))
                .andExpect(jsonPath("$.errors.details").value("Details are required"))
                .andExpect(jsonPath("$.errors.reportedBy").value("Reporter is required"));
    }

    @Test
    void invalidPaginationReturnsBadRequest() throws Exception {
        mockMvc.perform(get(NEWS_API)
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    private JsonNode createNews(String title, String details, String reportedBy) throws Exception {
        MvcResult result = mockMvc.perform(post(NEWS_API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validNewsRequest(title, details, reportedBy)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String validNewsRequest(String title, String details, String reportedBy) {
        return """
                {
                  "title": "%s",
                  "details": "%s",
                  "reportedBy": "%s"
                }
                """.formatted(title, details, reportedBy);
    }

    private void assertSameTimestamp(LocalDateTime expected, LocalDateTime actual) {
        assertThat(Duration.between(expected, actual).abs()).isLessThanOrEqualTo(Duration.ofMillis(1));
    }
}
