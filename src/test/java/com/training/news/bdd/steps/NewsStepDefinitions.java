package com.training.news.bdd.steps;

import com.training.news.news.News;
import com.training.news.news.NewsMapper;
import com.training.news.news.NewsRepository;
import com.training.news.news.NewsRequest;
import com.training.news.security.api_user.Role;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NewsStepDefinitions {

    private static final String NEWS_API = "/api/v1/news";

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    private NewsRequest newsRequest;
    private ResultActions response;
    private Long newsId;

    public NewsStepDefinitions(NewsRepository newsRepository, NewsMapper newsMapper, MockMvc mockMvc,
                               ObjectMapper objectMapper) {
        this.newsRepository = newsRepository;
        this.newsMapper = newsMapper;
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Given("news records exist")
    public void newsRecordsExist() {
        newsRepository.deleteAll();

        newsRequest = new NewsRequest("Cucumbers are great!",
                "Cucumbers are the greatest vegetable known to man. " + "Wait, are they vegetables or fruits?");

        News news = newsMapper.toEntity(newsRequest);
        news.setReportedBy("cucumber");

        newsRepository.save(news);
    }

    @When("the user requests all news")
    public void theUserRequestsAllNews() throws Exception {
        response = mockMvc.perform(get(NEWS_API));
    }

    @Then("the news should be returned")
    public void theNewsShouldBeReturned() throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").isNotEmpty())
                .andExpect(jsonPath("$.content[0].title").value("Cucumbers are great!"));
    }

    @Given("valid news details are provided")
    public void validNewsDetailsAreProvided() {
        newsRepository.deleteAll();

        newsRequest = new NewsRequest("Gherkins are great!",
                "Gherkins are the greatest vegetable known to man. " + "Wait, are they vegetables or fruits?");
    }

    @When("the user creates the news")
    public void userCreatesNews() throws Exception {
        response = mockMvc.perform(post(NEWS_API).with(authenticatedAs("gherkin_lover", Role.REPORTER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newsRequest)));
    }

    @Then("the news should be saved")
    public void theNewsShouldBeSaved() throws Exception {
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Gherkins are great!"))
                .andExpect(jsonPath("$.reportedBy").value("gherkin_lover"));

        boolean newsExists = newsRepository.findAll()
                .stream()
                .anyMatch(news -> "Gherkins are great!".equals(news.getTitle()));

        assertThat(newsExists).isTrue();
    }

    @Given("an existing news record belongs to the reporter")
    public void existingNewsRecordBelongsToReporter() {
        newsRepository.deleteAll();

        NewsRequest existingRequest = new NewsRequest("Original news title", "Original news details");

        News news = newsMapper.toEntity(existingRequest);
        news.setReportedBy("crud_reporter");

        News savedNews = newsRepository.save(news);
        newsId = savedNews.getNewsId();
    }

    @Given("updated news details are provided")
    public void updatedNewsDetailsAreProvided() {
        newsRequest = new NewsRequest("Updated news title", "Updated news details");
    }

    @When("the reporter updates the news")
    public void reporterUpdatesTheNews() throws Exception {
        response = mockMvc.perform(
                put(NEWS_API + "/{newsId}", newsId).with(authenticatedAs("crud_reporter", Role.REPORTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newsRequest)));
    }

    @Then("the news should be updated")
    public void newsShouldBeUpdated() throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.newsId").value(newsId))
                .andExpect(jsonPath("$.title").value("Updated news title"))
                .andExpect(jsonPath("$.details").value("Updated news details"))
                .andExpect(jsonPath("$.reportedBy").value("crud_reporter"));

        News updatedNews = newsRepository.findById(newsId)
                .orElseThrow();

        assertThat(updatedNews.getTitle()).isEqualTo("Updated news title");

        assertThat(updatedNews.getDetails()).isEqualTo("Updated news details");

        assertThat(updatedNews.getReportedBy()).isEqualTo("crud_reporter");

        assertThat(updatedNews.getUpdatedAt()).isNotNull();
    }

    @When("the reporter deletes the news")
    public void reporterDeletesTheNews() throws Exception {
        response = mockMvc.perform(
                delete(NEWS_API + "/{newsId}", newsId).with(authenticatedAs("crud_reporter", Role.REPORTER)));
    }

    @Then("the news should be deleted")
    public void newsShouldBeDeleted() throws Exception {
        response.andExpect(status().isNoContent());

        assertThat(newsRepository.existsById(newsId)).isFalse();
    }

    private RequestPostProcessor authenticatedAs(String username, Role role) {
        return jwt().jwt(token -> token.subject(username))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}