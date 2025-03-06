package com.acme.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.kiwiproject.test.okhttp3.mockwebserver.RecordedRequestAssertions.assertThatRecordedRequest;
import static org.kiwiproject.test.okhttp3.mockwebserver.RecordedRequests.takeRequiredRequest;

import com.acme.junit.extension.MockWebServerExtension;
import com.google.common.net.HttpHeaders;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.TimeUnit;

/**
 * Test of the {@link MathApiClient} using the OkHttp {@link MockWebServer} and some of
 * the test utilities in <a href="https://github.com/kiwiproject/kiwi-test">kiwi-test</a>
 * including {@link MockWebServerExtension}.
 */
@DisplayName("OkHttpMathApiClient (with kiwi-test and JUnit extension)")
class OkHttpMathApiClientEvenNicerTest {

    @RegisterExtension
    private final MockWebServerExtension serverExtension = new MockWebServerExtension();

    private MathApiClient mathClient;
    private Client client;
    private MockWebServer server;

    @BeforeEach
    void setUp() {
        client = ClientBuilder.newBuilder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .build();

        server = serverExtension.server();
        var baseUri = serverExtension.uri();

        mathClient = new MathApiClient(client, baseUri);
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void shouldAdd() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
                .setBody("42"));

        assertThat(mathClient.add(40, 2)).isEqualTo(42);

        var recordedRequest = takeRequiredRequest(server);

        assertThatRecordedRequest(recordedRequest)
                .isGET()
                .hasPath("/math/add/40/2")
                .hasNoBody();
    }

    @Test
    void shouldThrowIllegalArgumentException_ForInvalidInput() {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
                .setBody("overflow"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> mathClient.add(Integer.MAX_VALUE, 1))
                .withMessage("Invalid arguments: overflow");

        var recordedRequest = takeRequiredRequest(server);

        assertThatRecordedRequest(recordedRequest)
                .isGET()
                .hasPath("/math/add/%s/1", Integer.MAX_VALUE);
    }

    @Test
    void shouldThrowIllegalStateException_ForServerError() {
        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
                .setBody("Server error: can't add right now"));

        assertThatIllegalStateException()
                .isThrownBy(() -> mathClient.add(2, 2))
                .withMessage("Unknown error: Server error: can't add right now");

        var recordedRequest = takeRequiredRequest(server);

        assertThatRecordedRequest(recordedRequest)
                .isGET()
                .hasPath("/math/add/2/2");
    }
}
