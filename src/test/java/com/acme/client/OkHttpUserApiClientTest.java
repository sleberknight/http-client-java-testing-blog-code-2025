package com.acme.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.kiwiproject.test.constants.KiwiTestConstants.JSON_HELPER;

import com.acme.model.User;
import com.google.common.net.HttpHeaders;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.UriBuilder;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kiwiproject.test.assertj.KiwiAssertJ;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

@DisplayName("OkHttpUserApiClient")
class OkHttpUserApiClientTest {

    private UserApiClient apiClient;
    private Client client;
    private MockWebServer server;
    private URI baseUri;

    @BeforeEach
    void setUp() throws URISyntaxException {
        client = ClientBuilder.newBuilder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .build();

        server = new MockWebServer();
        baseUri = server.url("/").url().toURI();

        apiClient = new UserApiClient(client, baseUri);
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
        server.close();
    }

    @Test
    void shouldGetUserById() throws InterruptedException {
        var responseEntity = new User(42L, "j_smith", "[password hidden]", "Jane Smith");

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .setBody(JSON_HELPER.toJson(responseEntity)));

        var userOptional = apiClient.getById(42);
        var user = KiwiAssertJ.assertPresentAndGet(userOptional);

        assertAll(
                () -> assertThat(user.id()).isEqualTo(42L),
                () -> assertThat(user.username()).isEqualTo("j_smith")
        );

        var recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();

        assertAll(
                () -> assertThat(recordedRequest.getMethod()).isEqualTo("GET"),
                () -> assertThat(recordedRequest.getPath()).isEqualTo("/users/42"),
                () -> assertThat(recordedRequest.getBodySize()).isZero()
        );
    }

    @Test
    void shouldGetUserById_WhenNoUserFound() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(404));

        var userOptional = apiClient.getById(84);
        assertThat(userOptional).isEmpty();

        var recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();

        assertAll(
                () -> assertThat(recordedRequest.getMethod()).isEqualTo("GET"),
                () -> assertThat(recordedRequest.getPath()).isEqualTo("/users/84"),
                () -> assertThat(recordedRequest.getBodySize()).isZero()
        );
    }

    @Test
    void shouldListUsers() throws InterruptedException {
        var responseEntity = List.of(
                User.newWithRedactedPassword(1L, "a_jones", "Alice Jones"),
                User.newWithRedactedPassword(2L, "bob_hart", "Bob Hart"),
                User.newWithRedactedPassword(3L, "carlos_d", "Carlos Diaz"),
                User.newWithRedactedPassword(4L, "d_smith", "Diane Smith")
        );

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .setBody(JSON_HELPER.toJson(responseEntity)));

        var users = apiClient.list();

        assertThat(users).extracting(User::name).containsExactly(
                "Alice Jones",
                "Bob Hart",
                "Carlos Diaz",
                "Diane Smith"
        );

        var recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();

        assertAll(
                () -> assertThat(recordedRequest.getMethod()).isEqualTo("GET"),
                () -> assertThat(recordedRequest.getPath()).isEqualTo("/users"),
                () -> assertThat(recordedRequest.getBodySize()).isZero()
        );
    }

    @Test
    void shouldThrow_IllegalState_WhenListUsersDoesNotReturn_200() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThatIllegalStateException()
                .isThrownBy(() -> apiClient.list())
                .withMessage("Received 500 response from /users");

        var recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();

        assertAll(
                () -> assertThat(recordedRequest.getMethod()).isEqualTo("GET"),
                () -> assertThat(recordedRequest.getPath()).isEqualTo("/users"),
                () -> assertThat(recordedRequest.getBodySize()).isZero()
        );
    }

    @Test
    void shouldCreateUser() throws InterruptedException {
        var id = RandomGenerator.getDefault().nextLong(1, 501);
        var responseEntity = User.newWithRedactedPassword(id, "s_white", "Shaun White");

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .setHeader(HttpHeaders.LOCATION, UriBuilder.fromUri(baseUri).path("/users/{id}").build(id))
                .setBody(JSON_HELPER.toJson(responseEntity)));

        var newUser = new User(null, "s_white", "snowboarding", "Shaun White");
        var createdUser = apiClient.create(newUser);

        assertAll(
                () -> assertThat(createdUser.id()).isEqualTo(id),
                () -> assertThat(createdUser.username()).isEqualTo("s_white"),
                () -> assertThat(createdUser.password()).isEqualTo(User.REDACTED_PASSWORD)
        );

        var recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();

        assertAll(
                () -> assertThat(recordedRequest.getMethod()).isEqualTo("POST"),
                () -> assertThat(recordedRequest.getPath()).isEqualTo("/users"),
                () -> assertThat(readBodyAsUser(recordedRequest)).isEqualTo(newUser)
        );
    }

    @Test
    void shouldThrow_IllegalState_WhenCreateUser_DoesNotReturn_201() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(422));

        var newUser = new User(null, null, null, null);
        assertThatIllegalStateException()
                .isThrownBy(() -> apiClient.create(newUser))
                .withMessage("Failed to create user (response code: 422)");

        var recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();

        assertAll(
                () -> assertThat(recordedRequest.getMethod()).isEqualTo("POST"),
                () -> assertThat(recordedRequest.getPath()).isEqualTo("/users"),
                () -> assertThat(readBodyAsUser(recordedRequest)).isEqualTo(newUser)
        );
    }

    @Test
    void shouldUpdateUser() throws InterruptedException {
        var id = RandomGenerator.getDefault().nextLong(1, 501);
        var responseEntity = User.newWithRedactedPassword(id, "j_jones", "Jeremy Jones");

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .setBody(JSON_HELPER.toJson(responseEntity)));

        var existingUser = new User(id, "j_jones", "snowboarding", "Jeremy Jones");

        var updatedUser = apiClient.update(existingUser);

        assertAll(
                () -> assertThat(updatedUser.id()).isEqualTo(id),
                () -> assertThat(updatedUser.username()).isEqualTo("j_jones"),
                () -> assertThat(updatedUser.password()).isEqualTo(User.REDACTED_PASSWORD)
        );

        var recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();

        assertAll(
                () -> assertThat(recordedRequest.getMethod()).isEqualTo("PUT"),
                () -> assertThat(recordedRequest.getPath()).isEqualTo("/users/" + id),
                () -> assertThat(readBodyAsUser(recordedRequest)).isEqualTo(existingUser)
        );
    }

    private static User readBodyAsUser(RecordedRequest recordedRequest) {
        var json = recordedRequest.getBody().readUtf8();
        return JSON_HELPER.toObject(json, User.class);
    }
}
