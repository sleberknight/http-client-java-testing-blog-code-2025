package com.acme.client;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardClientExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

@ExtendWith(DropwizardExtensionsSupport.class)
class DropwizardMathApiClientTest {

    @Path("/math")
    public static class MathStubResource {
        @GET
        @Path("/add/{a}/{b}")
        @Produces(MediaType.TEXT_PLAIN)
        public Response add(@PathParam("a") int a, @PathParam("b") int b) {
            var answer = a + b;
            return Response.ok(answer).build();
        }
    }

    private static final DropwizardClientExtension CLIENT_EXTENSION =
            new DropwizardClientExtension(new MathStubResource());

    private MathApiClient mathClient;
    private Client client;

    @BeforeEach
    void setUp() {
        client = ClientBuilder.newBuilder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .build();
        var baseUri = CLIENT_EXTENSION.baseUri();
        mathClient = new MathApiClient(client, baseUri);
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void shouldAdd() {
        assertThat(mathClient.add(40, 2)).isEqualTo(42);
    }
}
