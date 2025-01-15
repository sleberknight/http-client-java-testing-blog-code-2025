This small project contains example code showing how to use the OkHttp MockWebServer in tests of HTTP client code.

Note this is using the stable 4.x version of OkHttp and the `mockwebserver` artifact, not `mockwebserver3`.

Here are descriptions of each class:

* `User` is a simple model record class.
* `UserClient` is an HTTP client that uses Jersey to make HTTP requests.
* `DropwizardUserApiClientTest` is an example test that uses the Dropwizard test support, specifically
  `DropwizardClientExtension`.
* `OkHttpUserApiClientTest` is an example test that uses OkHttp's `MockWebServer`.
* `OkHttpUserApiClientNicerTest` is another example test that uses `MockWebServer`, but also uses some additional
  utilities
  in kiwi-test to reduce boilerplate.
* `MockWebServerExtension` is a JUnit extension that starts a `MockWebServer` before each test, and stops it after each
  test.
