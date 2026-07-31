Feature: News management

  Scenario: Retrieve all news
    Given news records exist
    When the user requests all news
    Then the news should be returned