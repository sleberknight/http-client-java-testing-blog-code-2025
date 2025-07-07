# Example Java HTTP Client Testing using MockWebServer

[![Build](https://github.com/sleberknight/http-client-java-testing-blog-code-2025/actions/workflows/build.yml/badge.svg)](https://github.com/sleberknight/http-client-java-testing-blog-code-2025/actions/workflows/build.yml)

This small project contains example code showing how to use the OkHttp MockWebServer in tests of HTTP client code.

It is the companion code to the following blogs:

* [Testing HTTP Client Code with MockWebServer](http://www.sleberknight.com/blog/sleberkn/entry/testing_http_client_code_with)
* [Making HTTP Client Tests Cleaner with MockWebServer and kiwi-test](http://www.sleberknight.com/blog/sleberkn/entry/making_http_client_tests_cleaner)
* [Adding a MockWebServer JUnit Jupiter Extension](http://www.sleberknight.com/blog/sleberkn/entry/adding_a_mockwebserver_junit_jupiter)

## Summary of Example Classes and Tests

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

## OkHttp Versions

Originally this used the stable 4.x version of OkHttp and the `mockwebserver` artifact, not `mockwebserver3`.
The reason was that `mockwebserver3` requires OkHttp 5.x, which had been in alpha since 2023 and was just released
in early July 2025.

The [OkHttp Change Log](https://square.github.io/okhttp/changelogs/changelog/) indicated that the 5.x releases were
stable as of the [5.0.0-alpha.7]([url](https://square.github.io/okhttp/changelogs/changelog/#version-500-alpha7)) release, saying:

> The alpha releases in the 5.0.0 series have production-quality code and an unstable API.
> We expect to make changes to the APIs introduced in 5.0.0-alpha.X.
> These releases are safe for production use and ‘alpha’ strictly signals that we’re still experimenting with some new APIs.

So, this repository has used OkHttp 4.x and the original `mockwebserver` since its inception.
But once OkHttp released OkHttp 5.0.0 in July 2025, this repository now uses 5.x but is still
using `mockwebserver` since there are some breaking API changes with `mockwebserver3`.

It is possible I may update to use `mockwebserver3`, or maybe just create a separate repository
so they can both co-exist.
