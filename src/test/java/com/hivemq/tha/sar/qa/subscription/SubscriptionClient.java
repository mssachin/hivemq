package com.hivemq.tha.sar.qa.subscription;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;

public class SubscriptionClient {

    private static final @NotNull String SERVER_URI_BASE = "http://localhost:";
    private static final @NotNull String SUBSCRIPTIONS_RESOURCE = "/subscriptions";
    private static final @NotNull String MATCH_RESOURCE = "/match";
    private static final @NotNull String SUBSCRIPTION_COUNT_KEY = "count";
    private static final @NotNull String TOPIC_KEY = "topic";
    private static final @NotNull String TOPIC_FILTER_KEY = "topicFilter";
    private static final @NotNull String CLIENT_ID_KEY = "clientId";
    private static final @NotNull String SUBSCRIBERS_KEY = "subscribers";
    private static final int HTTP_STATUS_CODE_OK = 200;

    private final @NotNull HttpClient client;
    private final @NotNull URI addSubscriptionUri;
    private final @NotNull URI getMatchingSubscribersUri;
    private final @NotNull URI getSubscriptionCountUri;
    private final @NotNull Duration requestTimeout;

    public SubscriptionClient(final int serverPort, final @NotNull Duration requestTimeout) throws URISyntaxException {
        assert requestTimeout.toMillis() >= 0 : "Request timeout must be >= 0: " + requestTimeout;
        this.client = HttpClient.newHttpClient();
        final String resourceUri = SERVER_URI_BASE + serverPort + SUBSCRIPTIONS_RESOURCE;
        this.addSubscriptionUri = new URI(resourceUri);
        this.getMatchingSubscribersUri = new URI(resourceUri + MATCH_RESOURCE);
        this.getSubscriptionCountUri = new URI(resourceUri);
        this.requestTimeout = requestTimeout;
    }

    public void addSubscription(final @NotNull String topicFilter, final @NotNull String clientId)
            throws IOException, InterruptedException {
        final JSONObject body = new JSONObject();
        body.put(TOPIC_FILTER_KEY, topicFilter);
        body.put(CLIENT_ID_KEY, clientId);
        sendPostRequest(addSubscriptionUri, body.toString(), response -> {});
    }

    public @NotNull ImmutableList<@NotNull String> getMatchingSubscribers(final @NotNull String topic)
            throws IOException, InterruptedException {
        final ImmutableList.Builder<String> subscribersBuilder = ImmutableList.builder();
        final JSONObject body = new JSONObject();
        body.put(TOPIC_KEY, topic);
        sendPostRequest(getMatchingSubscribersUri, body.toString(), response -> {
            assert response != null;
            for (final Object jsonSubscriber : response.getJSONArray(SUBSCRIBERS_KEY)) {
                assert jsonSubscriber instanceof String : jsonSubscriber.getClass().getSimpleName();
                subscribersBuilder.add((String) jsonSubscriber);
            }
        });
        return subscribersBuilder.build();
    }

    public long getSubscriptionCount() throws IOException, InterruptedException {
        final long[] count = new long[1];
        sendGetRequest(getSubscriptionCountUri, response -> count[0] = response.getLong(SUBSCRIPTION_COUNT_KEY));
        return count[0];
    }

    private void sendPostRequest(
            final @NotNull URI uri,
            final @NotNull String body,
            final @NotNull Consumer<@Nullable JSONObject> responseHandler)
            throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(requestTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        handleJsonResponse(response, responseHandler);
    }

    private void sendGetRequest(
            final @NotNull URI uri,
            final @NotNull Consumer<@NotNull JSONObject> responseHandler)
            throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(requestTimeout)
                .GET()
                .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        handleJsonResponse(response, responseHandler);
    }

    private static void handleJsonResponse(
            final @NotNull HttpResponse<String> response,
            final @NotNull Consumer<@Nullable JSONObject> responseHandler)
            throws IOException {
        if (response.statusCode() == HTTP_STATUS_CODE_OK) {
            final String body = response.body();
            responseHandler.accept(body.isEmpty() ? null : new JSONObject(body));
        } else {
            throw new IOException(response.statusCode() + ": " + response.body());
        }
    }
}
