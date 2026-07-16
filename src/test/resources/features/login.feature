Feature: User Login Flow

  Scenario: Successful login
    Given I navigate to "/login"
    When I fill the field "Username Field" with "{{username}}"
    And I fill the field "Password Field" with "{{password}}"
    And I click the button "Login Button"
    Then I should see the text or element "You logged into a secure area!"

  Scenario: Login failure with incorrect credentials
    Given I navigate to "/login"
    When I fill the field "Username Field" with "invalid_user"
    And I fill the field "Password Field" with "wrong_password"
    And I click the button "Login Button"
    Then I should see the text or element "Your username is invalid!"
