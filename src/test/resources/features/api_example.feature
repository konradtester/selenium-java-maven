Feature: API / REST requests

  Scenario: Fetch an existing resource
    When I send a "GET" request to "/posts/1"
    Then the response status code should be 200
    And the response field "id" should equal "1"
    And the response field "userId" should equal "1"

  Scenario: Create a resource from a JSON fixture and reuse its id
    Given I set the request header "Content-Type" to "application/json"
    When I send a "POST" request to "/posts" with body from file "create_post.json"
    Then the response status code should be 201
    And the response field "title" should equal "foo"
    And the response field "authorEmail" should equal "{{test_email}}"
    When I save the response field "id" as variable "new_post_id"
    And I send a "GET" request to "/posts/{{new_post_id}}"
    Then the response status code should be 404
