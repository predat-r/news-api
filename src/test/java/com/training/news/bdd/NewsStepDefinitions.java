package com.training.news.bdd;

import com.training.news.news.News;
import com.training.news.news.NewsMapper;
import com.training.news.news.NewsRepository;
import com.training.news.news.NewsRequest;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NewsStepDefinitions {

    private static final String NEWS_API = "/api/v1/news";

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;
    private final MockMvc mockMvc;

    private ResultActions response;

    public NewsStepDefinitions(
            NewsRepository newsRepository,
            NewsMapper newsMapper,
            MockMvc mockMvc
    ) {
        this.newsRepository = newsRepository;
        this.newsMapper = newsMapper;
        this.mockMvc = mockMvc;
    }

    @Given("news records exist")
    public void newsRecordsExist() {
        newsRepository.deleteAll();

        NewsRequest newsRequest = new NewsRequest(
                "Cucumbers are great!",
                "Cucumbers are the greatest vegetable known to man. "
                        + "Wait, are they vegetables or fruits?"
        );

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
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").isNotEmpty())
                .andExpect(jsonPath("$.content[0].title")
                        .value("Cucumbers are great!"));
    }
}