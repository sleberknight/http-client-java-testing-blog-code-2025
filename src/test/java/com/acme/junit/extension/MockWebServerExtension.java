package com.acme.junit.extension;

import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;

import lombok.Getter;
import lombok.experimental.Accessors;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.io.KiwiIO;

import java.io.IOException;

/**
 * A JUnit Jupiter extension that starts a {@link MockWebServer}
 * before <em>each</em> test, and shuts it down after <em>each</em> test.
 * <p>
 * Optionally, you can specify your own {@code MockWebServer} if you
 * need specific features, such as when you need to make TLS requests.
 */
public class MockWebServerExtension implements BeforeEachCallback, AfterEachCallback {

    @Getter
    @Accessors(fluent = true)
    private MockWebServer server;

    public MockWebServerExtension() {
        this(new MockWebServer());
    }

    public MockWebServerExtension(MockWebServer server) {
        this.server = requireNotNull(server, "server must not be nul");
    }

    @Override
    public void beforeEach(ExtensionContext context) throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        KiwiIO.closeQuietly(server);
    }
}
