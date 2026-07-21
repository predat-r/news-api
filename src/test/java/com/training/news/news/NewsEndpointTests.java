package com.training.news.news;

import com.training.news.security.api_user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
                        .with(authenticatedAs("reporter1", Role.REPORTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsRequest("Create title", "Create details")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newsId").isNumber())
                .andExpect(jsonPath("$.title").value("Create title"))
                .andExpect(jsonPath("$.details").value("Create details"))
                .andExpect(jsonPath("$.reportedBy").value("reporter1"));
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
        long newsId = createdNews.get("newsId")
                .asLong();

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
        long newsId = createdNews.get("newsId")
                .asLong();

        mockMvc.perform(put(NEWS_API + "/{newsId}", newsId)
                        .with(authenticatedAs("Reporter D", Role.REPORTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsRequest("After title", "After details")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsId").value(newsId))
                .andExpect(jsonPath("$.title").value("After title"))
                .andExpect(jsonPath("$.details").value("After details"))
                .andExpect(jsonPath("$.reportedBy").value("Reporter D"))
                .andExpect(jsonPath("$.reportedAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void deleteNewsRemovesNews() throws Exception {
        JsonNode createdNews = createNews("Delete title", "Delete details", "Reporter F");
        long newsId = createdNews.get("newsId")
                .asLong();

        mockMvc.perform(delete(NEWS_API + "/{newsId}", newsId)
                .with(authenticatedAs("Reporter F",Role.REPORTER)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(NEWS_API + "/{newsId}", newsId))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidCreateRequestReturnsValidationErrors() throws Exception {
        mockMvc.perform(post(NEWS_API)
                        .with(authenticatedAs("reporter1", Role.REPORTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsRequest("", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.title").value("Title is required"))
                .andExpect(jsonPath("$.errors.details").value("Details are required"));

    }

    private JsonNode createNews(String title, String details, String reportedBy) throws Exception {
        MvcResult result = mockMvc.perform(post(NEWS_API)
                        .with(authenticatedAs(reportedBy, Role.REPORTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsRequest(title, details)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse()
                .getContentAsString());
    }

    private String newsRequest(String title, String details) {
        return """
                                {
                                  "title": "%s",
                                  "details": "%s"
                }
                """.formatted(title, details);


    }

    private RequestPostProcessor authenticatedAs(String username, Role role) {
        return jwt()
                .jwt(token -> token.subject(username))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
