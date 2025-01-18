package com.acme.client;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.acme.model.User;
import io.dropwizard.testing.junit5.DropwizardClientExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kiwiproject.test.assertj.KiwiAssertJ;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.random.RandomGenerator;

@ExtendWith(DropwizardExtensionsSupport.class)
class DropwizardUserApiClientTest {

    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class MockUserResource {

        static final long USER_DOES_NOT_EXIST_ID = -42L;

        static final AtomicBoolean return500NextRequest = new AtomicBoolean();

        static void setReturn500NextRequest() {
            return500NextRequest.set(true);
        }

        @GET
        @Path("/{id}")
        public Response getById(@PathParam("id") long id) {
            // Strategy #1: return error responses based on input data having specific values
            if (id == USER_DOES_NOT_EXIST_ID) {
                return Response.status(404).build();
            }

            var user = User.newWithRedactedPassword(id, "j_smith", "Jane Smith");
            return Response.ok(user).build();
        }

        @GET
        public Response list() {
            // Strategy #2: allow calling tests to control when an error response is sent (not safe for parallel tests)
            if (return500NextRequest.getAndSet(false)) {
                return Response.serverError().build();
            }

            var users = List.of(
                    User.newWithRedactedPassword(1L, "a_jones", "Alice Jones"),
                    User.newWithRedactedPassword(2L, "bob_hart", "Bob Hart"),
                    User.newWithRedactedPassword(3L, "carlos_d", "Carlos Diaz"),
                    User.newWithRedactedPassword(4L, "d_smith", "Diane Smith")
            );
            return Response.ok(users).build();
        }

        @POST
        public Response create(@NotNull @Valid User user, @Context UriInfo uriInfo) {
            var id = RandomGenerator.getDefault().nextLong(1, 501);
            var createdUser = user.withRedactedPassword().withId(id);
            var location = uriInfo.getAbsolutePathBuilder().path("/users/{id}").build(id);
            return Response.created(location).entity(createdUser).build();
        }

        @PUT
        @Path("/{id}")
        public Response update(@PathParam("id") long id, @NotNull @Valid User user) {
            checkState(nonNull(user.id()) && user.id() == id, "path id and User.id do not match");
            var updatedUser = user.withRedactedPassword();
            return Response.ok(updatedUser).build();
        }
    }

    private static final DropwizardClientExtension CLIENT_EXTENSION =
            new DropwizardClientExtension(new MockUserResource());

    private UserApiClient apiClient;
    private Client client;

    @BeforeEach
    void setUp() {
        client = ClientBuilder.newBuilder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .build();

        var baseUri = CLIENT_EXTENSION.baseUri();
        apiClient = new UserApiClient(client, baseUri);
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void shouldGetUserById() {
        var id = RandomGenerator.getDefault().nextLong(1, 501);
        var userOptional = apiClient.getById(id);
        var user = KiwiAssertJ.assertPresentAndGet(userOptional);

        assertAll(
                () -> assertThat(user.id()).isEqualTo(id),
                () -> assertThat(user.username()).isEqualTo("j_smith")
        );
    }

    @Test
    void shouldGetUserById_WhenNoUserFound() {
        var id = MockUserResource.USER_DOES_NOT_EXIST_ID;
        var userOptional = apiClient.getById(id);

        assertThat(userOptional).isEmpty();
    }

    @Test
    void shouldListUsers() {
        var users = apiClient.list();

        assertThat(users).extracting(User::name).containsExactly(
                "Alice Jones",
                "Bob Hart",
                "Carlos Diaz",
                "Diane Smith"
        );
    }

    @Test
    void shouldThrow_IllegalState_WhenListUsersDoesNotReturn_200() {
        MockUserResource.setReturn500NextRequest();

        assertThatIllegalStateException()
                .isThrownBy(() -> apiClient.list())
                .withMessage("Received 500 response from /users");
    }

    @Test
    void shouldCreateUser() {
        var newUser = new User(null, "s_white", "snowboarding", "Shaun White");

        var createdUser = apiClient.create(newUser);

        assertAll(
                () -> assertThat(createdUser.id()).isPositive(),
                () -> assertThat(createdUser.username()).isEqualTo("s_white"),
                () -> assertThat(createdUser.password()).isEqualTo(User.REDACTED_PASSWORD)
        );
    }

    @Test
    void shouldThrow_IllegalState_WhenCreateUser_DoesNotReturn_201() {
        var newUser = new User(null, null, null, null);

        assertThatIllegalStateException()
                .isThrownBy(() -> apiClient.create(newUser))
                .withMessage("Failed to create user (response code: 422)");
    }

    @Test
    void shouldUpdateUser() {
        var existingUser = new User(123L, "j_jones", "snowboarding", "Jeremy Jones");

        var updatedUser = apiClient.update(existingUser);

        assertAll(
                () -> assertThat(updatedUser.id()).isEqualTo(existingUser.id()),
                () -> assertThat(updatedUser.username()).isEqualTo("j_jones"),
                () -> assertThat(updatedUser.password()).isEqualTo(User.REDACTED_PASSWORD)
        );
    }
}
