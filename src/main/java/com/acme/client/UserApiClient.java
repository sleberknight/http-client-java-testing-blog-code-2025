package com.acme.client;

import static org.kiwiproject.base.KiwiStrings.f;

import com.acme.model.User;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import org.kiwiproject.base.KiwiPreconditions;
import org.kiwiproject.jaxrs.KiwiResponses;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class UserApiClient {

    private static final GenericType<List<User>> USER_LIST_GENERIC_TYPE = new GenericType<>() {
    };

    private final Client client;
    private final URI baseUri;

    public UserApiClient(Client client, URI baseUri) {
        this.client = client;
        this.baseUri = baseUri;
    }

    /**
     * GET /users/{id}
     */
    public Optional<User> getById(long id) {
        var response = client.target(baseUri)
                .path("/users/{id}")
                .resolveTemplate("id", id)
                .request()
                .get();

        var user = KiwiResponses.apply(response,
                resp -> KiwiResponses.ok(resp) ? resp.readEntity(User.class) : null);

        return Optional.ofNullable(user);
    }

    /**
     * GET /users
     */
    public List<User> list() {
        var response = client.target(baseUri)
                .path("/users")
                .request()
                .get();

        return KiwiResponses.onSuccessWithResultOrFailureThrow(response,
                successResponse -> successResponse.readEntity(USER_LIST_GENERIC_TYPE),
                failResponse -> new IllegalStateException(
                        f("Received {} response from /users", failResponse.getStatus()))
        );
    }

    /**
     * POST /users
     */
    public User create(User newUser) {
        KiwiPreconditions.checkArgumentIsNull(newUser.id(), "new user must not have an id");

        var response = client.target(baseUri)
                .path("/users")
                .request()
                .post(Entity.json(newUser));

        var createdUser = KiwiResponses.apply(response,
                resp -> KiwiResponses.created(resp) ? resp.readEntity(User.class) : null);

        return Optional.ofNullable(createdUser)
                .map(User::withRedactedPassword)
                .orElseThrow(() -> new IllegalStateException(
                        f("Failed to create user (response code: {})", response.getStatus())));
    }

    /**
     * PUT /users/{id}
     */
    public User update(User existingUser) {
        var id = existingUser.id();
        KiwiPreconditions.checkArgumentNotNull(id, "existing user must have an id");

        var response = client.target(baseUri)
                .path("/users/{id}")
                .resolveTemplate("id", id)
                .request()
                .put(Entity.json(existingUser));

        var updatedUser = KiwiResponses.apply(response, resp ->
                KiwiResponses.ok(resp) ? resp.readEntity(User.class) : null);

        return Optional.ofNullable(updatedUser)
                .map(User::withRedactedPassword)
                .orElseThrow(() -> new IllegalStateException(
                        f("Failed to update user (response code: {})", response.getStatus())));
    }
}
