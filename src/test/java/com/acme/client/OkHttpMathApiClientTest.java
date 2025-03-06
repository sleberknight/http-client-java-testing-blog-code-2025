package com.acme.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.google.common.net.HttpHeaders;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Test of the {@link MathApiClient} using the OkHttp {@link MockWebServer}.
 */
class OkHttpMathApiClientTest {

    private MathApiClient mathClient;
    private Client client;
    private MockWebServer server;

    @BeforeEach
    void setUp() {
        client = ClientBuilder.newBuilder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .build();

        server = new MockWebServer();
        var baseUri = server.url("/").uri();

        mathClient = new MathApiClient(client, baseUri);
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
        server.close();
    }

    @Test
    void shouldAdd() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
                .setBody("42"));

        assertThat(mathClient.add(40, 2)).isEqualTo(42);

        var recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();

        assertAll(
                () -> assertThat(recordedRequest.getMethod()).isEqualTo("GET"),
                () -> assertThat(recordedRequest.getPath()).isEqualTo("/math/add/40/2"),
                () -> assertThat(recordedRequest.getBodySize()).isZero()
        );
    }

    @Test
    void shouldThrowIllegalArgumentException_ForInvalidInput() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
                .setBody("overflow"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> mathClient.add(Integer.MAX_VALUE, 1))
                .withMessage("Invalid arguments: overflow");

        var recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();

        assertAll(
                () -> assertThat(recordedRequest.getMethod()).isEqualTo("GET"),
                () -> assertThat(recordedRequest.getPath()).isEqualTo("/math/add/%d/1", Integer.MAX_VALUE)
        );
    }

    @Test
    void shouldThrowIllegalStateException_ForServerError() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
                .setBody("Server error: can't add right now"));

        assertThatIllegalStateException()
                .isThrownBy(() -> mathClient.add(2, 2))
                .withMessage("Unknown error: Server error: can't add right now");

        var recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();

        assertAll(
                () -> assertThat(recordedRequest.getMethod()).isEqualTo("GET"),
                () -> assertThat(recordedRequest.getPath()).isEqualTo("/math/add/2/2")
        );
    }
}
