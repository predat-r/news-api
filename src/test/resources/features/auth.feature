Feature: User login

  Scenario: Log in with valid credentials
    Given the user is on the login page
    When the user enters valid credentials
    And submits the login form
    Then a JWT access token should be returned