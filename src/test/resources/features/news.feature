Feature: News management

  Scenario: Retrieve all news
    Given news records exist
    When the user requests all news
    Then the news should be returned

  Scenario: Create a news record
    Given valid news details are provided
    When the user creates the news
    Then the news should be saved

  Scenario: Update an existing news record
    Given an existing news record belongs to the reporter
    And updated news details are provided
    When the reporter updates the news
    Then the news should be updated

  Scenario: Delete an existing news record
    Given an existing news record belongs to the reporter
    When the reporter deletes the news
    Then the news should be deleted
