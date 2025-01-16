package com.acme.client;

import static org.kiwiproject.jaxrs.KiwiResponses.clientError;
import static org.kiwiproject.jaxrs.KiwiResponses.successful;

import jakarta.ws.rs.client.Client;

import java.net.URI;

public class MathApiClient {

    private final Client client;
    private final URI baseUri;

    public MathApiClient(Client client, URI baseUri) {
        this.client = client;
        this.baseUri = baseUri;
    }

    // Implementation with some error handling
    public int add(int a, int b) {
        var response = client.target(baseUri)
                .path("/math/add/{a}/{b}")
                .resolveTemplate("a", a)
                .resolveTemplate("b", b)
                .request()
                .get();

        if (successful(response)) {
            return response.readEntity(Integer.class);
        } else if (clientError(response)) {
            throw new IllegalArgumentException("Invalid arguments: " + response.readEntity(String.class));
        }

        throw new IllegalStateException("Unknown error: " + response.readEntity(String.class));
    }
}
