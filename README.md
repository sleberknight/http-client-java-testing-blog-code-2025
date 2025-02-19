# Example Java HTTP Client Testing using MockWebServer

[![Build](https://github.com/sleberknight/http-client-java-testing-blog-code-2025/actions/workflows/build.yml/badge.svg)](https://github.com/sleberknight/http-client-java-testing-blog-code-2025/actions/workflows/build.yml)

This small project contains example code showing how to use the OkHttp MockWebServer in tests of HTTP client code.

Note this is using the stable 4.x version of OkHttp and the `mockwebserver` artifact, not `mockwebserver3`
which requires OkHttp 5.x.

The [OkHttp Change Log](https://square.github.io/okhttp/changelogs/changelog/) indicates that the 5.x releases are
stable as of the [5.0.0-alpha.7]([url](https://square.github.io/okhttp/changelogs/changelog/#version-500-alpha7)) release, saying:

> The alpha releases in the 5.0.0 series have production-quality code and an unstable API.
> We expect to make changes to the APIs introduced in 5.0.0-alpha.X.
> These releases are safe for production use and ‘alpha’ strictly signals that we’re still experimenting with some new APIs.

But to be "safe" this repository uses OkHttp 4.x and the original `com.squareup.okhttp3:mockwebserver:4.12.0` Maven coordinates.

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
