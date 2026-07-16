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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
// TODO: Update these tests for authenticated users, CSRF tokens, role permissions,

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
                        .content(newsRequest("Create title", "Create details", "Reporter A")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newsId").isNumber())
                .andExpect(jsonPath("$.title").value("Create title"))
                .andExpect(jsonPath("$.details").value("Create details"))
                .andExpect(jsonPath("$.reportedBy").value("Reporter A"));
    }

    @Test
    void getNewsReturnsPaginatedNews() throws Exception {
        createNews("List title", "List details", "Reporter B");

        mockMvc.perform(get(NEWS_API)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").isNotEmpty())
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    void getNewsByIdReturnsNews() throws Exception {
        JsonNode createdNews = createNews("Get title", "Get details", "Reporter C");
        long newsId = createdNews.get("newsId").asLong();

        mockMvc.perform(get(NEWS_API + "/{newsId}", newsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsId").value(newsId))
                .andExpect(jsonPath("$.title").value("Get title"))
                .andExpect(jsonPath("$.details").value("Get details"))
                .andExpect(jsonPath("$.reportedBy").value("Reporter C"));
    }

    @Test
    void updateNewsReturnsUpdatedNews() throws Exception {
        JsonNode createdNews = createNews("Before title", "Before details", "Reporter D");
        long newsId = createdNews.get("newsId").asLong();

        mockMvc.perform(put(NEWS_API + "/{newsId}", newsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsRequest("After title", "After details", "Reporter E")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsId").value(newsId))
                .andExpect(jsonPath("$.title").value("After title"))
                .andExpect(jsonPath("$.details").value("After details"))
                .andExpect(jsonPath("$.reportedBy").value("Reporter E"))
                .andExpect(jsonPath("$.reportedAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void deleteNewsRemovesNews() throws Exception {
        JsonNode createdNews = createNews("Delete title", "Delete details", "Reporter F");
        long newsId = createdNews.get("newsId").asLong();

        mockMvc.perform(delete(NEWS_API + "/{newsId}", newsId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(NEWS_API + "/{newsId}", newsId))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidCreateRequestReturnsValidationErrors() throws Exception {
        mockMvc.perform(post(NEWS_API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsRequest("", "", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.title").value("Title is required"))
                .andExpect(jsonPath("$.errors.details").value("Details are required"))
                .andExpect(jsonPath("$.errors.reportedBy").value("Reporter is required"));
    }

    private JsonNode createNews(String title, String details, String reportedBy) throws Exception {
        MvcResult result = mockMvc.perform(post(NEWS_API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsRequest(title, details, reportedBy)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String newsRequest(String title, String details, String reportedBy) {
        return """
                {
                  "title": "%s",
                  "details": "%s",
                  "reportedBy": "%s"
                }
                """.formatted(title, details, reportedBy);
    }
}
