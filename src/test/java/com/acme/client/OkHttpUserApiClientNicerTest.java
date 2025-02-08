package com.acme.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.kiwiproject.test.constants.KiwiTestConstants.JSON_HELPER;
import static org.kiwiproject.test.okhttp3.mockwebserver.RecordedRequestAssertions.assertThatRecordedRequest;

import com.acme.junit.extension.MockWebServerExtension;
import com.acme.model.User;
import com.google.common.net.HttpHeaders;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.UriBuilder;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kiwiproject.test.assertj.KiwiAssertJ;
import org.kiwiproject.test.okhttp3.mockwebserver.MockWebServers;
import org.kiwiproject.test.okhttp3.mockwebserver.RecordedRequests;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

@DisplayName("OkHttpUserApiClient (with kiwi-test)")
class OkHttpUserApiClientNicerTest {

    @RegisterExtension
    private final MockWebServerExtension serverExtension = new MockWebServerExtension();

    private UserApiClient apiClient;
    private Client client;
    private MockWebServer server;
    private URI baseUri;

    @BeforeEach
    void setUp() {
        client = ClientBuilder.newBuilder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .build();

        server = serverExtension.server();
        baseUri = MockWebServers.uri(server, "/");

        apiClient = new UserApiClient(client, baseUri);
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void shouldGetUserById() {
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

        var recordedRequest = RecordedRequests.takeRequiredRequest(server);

        assertThatRecordedRequest(recordedRequest)
                .isGET()
                .hasPath("/users/42")
                .hasNoBody();
    }

    @Test
    void shouldGetUserById_WhenNoUserFound() {
        server.enqueue(new MockResponse().setResponseCode(404));

        var userOptional = apiClient.getById(84);
        assertThat(userOptional).isEmpty();

        var recordedRequest = RecordedRequests.takeRequiredRequest(server);

        assertThatRecordedRequest(recordedRequest)
                .isGET()
                .hasPath("/users/84")
                .hasNoBody();
    }

    @Test
    void shouldListUsers() {
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

        var recordedRequest = RecordedRequests.takeRequiredRequest(server);

        assertThatRecordedRequest(recordedRequest)
                .isGET()
                .hasPath("/users")
                .hasNoBody();
    }

    @Test
    void shouldThrow_IllegalState_WhenListUsersDoesNotReturn_200() {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThatIllegalStateException()
                .isThrownBy(() -> apiClient.list())
                .withMessage("Received 500 response from /users");

        var recordedRequest = RecordedRequests.takeRequiredRequest(server);

        assertThatRecordedRequest(recordedRequest)
                .isGET()
                .hasPath("/users")
                .hasNoBody();
    }

    @Test
    void shouldCreateUser() {
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

        var recordedRequest = RecordedRequests.takeRequiredRequest(server);

        assertThatRecordedRequest(recordedRequest)
                .isPOST()
                .hasPath("/users")
                .hasBody(JSON_HELPER.toJson(newUser));
    }

    @Test
    void shouldThrow_IllegalState_WhenCreateUser_DoesNotReturn_201() {
        server.enqueue(new MockResponse().setResponseCode(422));

        var newUser = new User(null, null, null, null);
        assertThatIllegalStateException()
                .isThrownBy(() -> apiClient.create(newUser))
                .withMessage("Failed to create user (response code: 422)");

        var recordedRequest = RecordedRequests.takeRequiredRequest(server);

        assertThatRecordedRequest(recordedRequest)
                .isPOST()
                .hasPath("/users")
                .hasBody(JSON_HELPER.toJson(newUser));
    }

    @Test
    void shouldUpdateUser() {
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

        var recordedRequest = RecordedRequests.takeRequiredRequest(server);

        assertThatRecordedRequest(recordedRequest)
                .isPUT()
                .hasPath("/users/" + id)
                .hasBody(JSON_HELPER.toJson(existingUser));
    }
}
