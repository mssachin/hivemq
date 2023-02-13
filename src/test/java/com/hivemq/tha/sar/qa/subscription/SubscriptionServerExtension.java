package com.hivemq.tha.sar.qa.subscription;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

public class SubscriptionServerExtension implements Extension, BeforeEachCallback, AfterEachCallback {

    private static final @NotNull String SERVER_JAR_PATH =
            new File("src/test/resources/subscription-server.jar").getAbsolutePath();

    public static class Builder {

        private @Nullable String shellPath;
        private @Nullable String javaPath;
        private @Nullable Integer port;
        private @Nullable Integer threadCount;
        private @Nullable Duration requestTimeout;
        private @Nullable Duration waitAfterStart;

        private Builder() {
        }

        public @NotNull Builder setShellPath(final @NotNull String shellPath) {
            this.shellPath = shellPath;
            return this;
        }

        public @NotNull Builder setJavaPath(final @NotNull String javaPath) {
            this.javaPath = javaPath;
            return this;
        }

        public @NotNull Builder setPort(final int port) {
            this.port = port;
            return this;
        }

        public @NotNull Builder setThreadCount(final int threadCount) {
            this.threadCount = threadCount;
            return this;
        }

        public @NotNull Builder setRequestTimeout(final @NotNull Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public @NotNull Builder setWaitAfterStart(final @NotNull Duration waitAfterStart) {
            this.waitAfterStart = waitAfterStart;
            return this;
        }

        public @NotNull SubscriptionServerExtension build() {
            return new SubscriptionServerExtension(
                    Objects.requireNonNull(shellPath),
                    Objects.requireNonNull(javaPath),
                    Objects.requireNonNull(port),
                    Objects.requireNonNull(threadCount),
                    Objects.requireNonNull(requestTimeout),
                    Objects.requireNonNull(waitAfterStart));
        }
    }

    public static @NotNull Builder newBuilder() {
        return new Builder();
    }

    private final int port;
    private final @NotNull Duration requestTimeout;
    private final @NotNull Duration waitAfterStart;
    private final @NotNull ProcessBuilder processBuilder;

    private @Nullable Process process;

    private SubscriptionServerExtension(
            final @NotNull String shellPath,
            final @NotNull String javaPath,
            final int port,
            final int threadCount,
            final @NotNull Duration requestTimeout,
            final @NotNull Duration waitAfterStart) {
        this.port = port;
        this.requestTimeout = requestTimeout;
        this.waitAfterStart = waitAfterStart;
        final String command = String.join(
                " ",
                javaPath,
                "-jar",
                SERVER_JAR_PATH,
                String.valueOf(port),
                String.valueOf(threadCount),
                String.valueOf(requestTimeout.getSeconds()));
        this.processBuilder = new ProcessBuilder(shellPath, "-c", command);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                terminateProcessIfRunning();
            } catch (final InterruptedException e) {
                // Intentionally ignore, because we're shutting down anyway.
            }
        }));
    }

    public int getPort() {
        return port;
    }

    public @NotNull Duration getRequestTimeout() {
        return requestTimeout;
    }

    @Override
    public void beforeEach(final @NotNull ExtensionContext extensionContext) throws IOException, InterruptedException {
        process = processBuilder.start();
        Thread.sleep(waitAfterStart.toMillis());
    }

    @Override
    public void afterEach(final @NotNull ExtensionContext extensionContext) throws InterruptedException {
        terminateProcessIfRunning();
    }

    private void terminateProcessIfRunning() throws InterruptedException {
        if (process != null) {
            process.destroy();
            process.waitFor();
        }
    }
}
