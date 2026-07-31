package com.training.news.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthStepDefinitions {

    private final ObjectMapper objectMapper;
    private final String username;
    private final String password;

    @LocalServerPort
    private int port;

    private WebDriver driver;

    public AuthStepDefinitions(ObjectMapper objectMapper, @Value("${test.auth.username}") String username,
                               @Value("${test.auth.password}") String password) {
        this.objectMapper = objectMapper;
        this.username = username;
        this.password = password;
    }

    @Given("the user is on the login page")
    public void userIsOnLoginPage() {
        FirefoxOptions options = new FirefoxOptions().setBinary(
                "/snap/firefox/current/usr/lib/firefox/firefox");

        options.addArguments("-headless");
        options.addPreference("devtools.jsonview.enabled", false);
        driver = new FirefoxDriver(options);
        driver.get("http://localhost:" + port + "/login");
    }


    @When("the user enters valid credentials")
    public void userEntersValidCredentials() {

        driver.findElement(By.id("username"))
                .sendKeys(username);

        driver.findElement(By.id("password"))
                .sendKeys(password);
    }

    @When("submits the login form")
    public void submitsLoginForm() {
        driver.findElement(By.cssSelector("button[type='submit']"))
                .click();
    }


    @Then("a JWT access token should be returned")
    public void jwtAccessTokenShouldBeReturned() throws Exception {
        String responseBody = driver.findElement(By.tagName("body"))
                .getText();


        JsonNode json = objectMapper.readTree(responseBody);

        assertThat(json.get("accessToken")
                .asString()).isNotBlank();
        assertThat(json.get("tokenType")
                .asString()).isEqualTo("Bearer");
        assertThat(json.get("expiresIn")
                .asInt()).isGreaterThan(0);
    }

    @After
    public void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }
}