package com.hivemq.tha.sar.qa;

import com.hivemq.tha.sar.qa.subscription.SubscriptionClient;
import com.hivemq.tha.sar.qa.subscription.SubscriptionServerExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;

public class SubscriptionServerSystemTest {

    @RegisterExtension
    public static final @NotNull SubscriptionServerExtension SERVER = SubscriptionServerExtension.newBuilder()
            .setShellPath("/bin/bash")
            .setJavaPath("/usr/bin/java")
            .setPort(44399)
            .setThreadCount(2)
            .setRequestTimeout(Duration.ofSeconds(10))
            .setWaitAfterStart(Duration.ofSeconds(1))
            .build();

    private @NotNull SubscriptionClient client;

    @BeforeEach
    public void setUp() throws URISyntaxException {
        client = new SubscriptionClient(SERVER.getPort(), SERVER.getRequestTimeout());
    }

    @Test
    public void tc01_positiveAddSubscriptionAndVerifyCountAndMatchingSubscribers() throws IOException, InterruptedException {
        client.addSubscription("building/floor/firstfloor/105", "firstFloor");
        assertEquals(1, client.getSubscriptionCount());
        assertTrue(client.getMatchingSubscribers("building/floor/firstfloor/105").contains("firstFloor"));
    }

    @Test
    public void tc02_positiveSingleClientMultipleTopicsVerifyCountAndMatchingSubscriptions() throws IOException, InterruptedException {
        client.addSubscription("building/floor/firstfloor/105", "firstFloor");
        client.addSubscription("building/floor/firstfloor/106", "firstFloor");
        client.addSubscription("building/floor/firstfloor/107", "firstFloor");
        client.addSubscription("building/floor/firstfloor/108", "firstFloor");
        assertEquals(1, client.getSubscriptionCount());
        assertTrue(client.getMatchingSubscribers("building/floor/firstfloor/105").contains("firstFloor"));
        assertTrue(client.getMatchingSubscribers("building/floor/firstfloor/106").contains("firstFloor"));
        assertTrue(client.getMatchingSubscribers("building/floor/firstfloor/107").contains("firstFloor"));
        assertTrue(client.getMatchingSubscribers("building/floor/firstfloor/108").contains("firstFloor"));
    }

    @Test
    public void tc03_positiveMultipleClientsMultipleTopicsVerifyCountAndSubscriptions() throws IOException, InterruptedException {
        client.addSubscription("building/floor/firstfloor/105", "firstFloor");
        client.addSubscription("building/floor/firstfloor/106", "firstFloor");
        client.addSubscription("building/floor/secondfloor/201", "secondFloor");
        client.addSubscription("building/floor/thirdfloor/301", "thirdFloor");
        client.addSubscription("building/floor/firstfloor/106", "thirdFloor");
        assertEquals(3, client.getSubscriptionCount());
        List<String> subscriberList = new ArrayList<>();
        subscriberList.add("firstFloor");
        subscriberList.add("thirdFloor");
        assertTrue(client.getMatchingSubscribers("building/floor/firstfloor/105").contains("firstFloor"));
        assertEquals(subscriberList, client.getMatchingSubscribers("building/floor/firstfloor/106"));
        assertTrue(client.getMatchingSubscribers("building/floor/secondfloor/201").contains("secondFloor"));
        assertTrue(client.getMatchingSubscribers("building/floor/thirdFloor/301").contains("thirdFloor"));
    }

    @Test
    public void tc04_positiveWildcardSearchHashVerifyCountAndSubscriptions() throws IOException, InterruptedException {
        client.addSubscription("building/floor/firstfloor/105", "firstFloor");
        client.addSubscription("building/floor/firstfloor/106", "firstFloor");
        client.addSubscription("building/floor/secondfloor/201", "secondFloor");
        client.addSubscription("building/floor/thirdfloor/301", "thirdFloor");
        client.addSubscription("building/floor/firstfloor/106", "thirdFloor");
        assertEquals(3, client.getSubscriptionCount());
        List<String> subscriberList = new ArrayList<>();
        subscriberList.add("firstFloor");
        subscriberList.add("secondFloor");
        subscriberList.add("thirdFloor");
        assertEquals(subscriberList, client.getMatchingSubscribers("building/floor/#"));
    }

    @Test
    public void tc05_positiveWildcardSearchPlusVerifyCountAndSubscriptions() throws IOException, InterruptedException {
        client.addSubscription("building/floor/firstfloor/floors", "firstFloor");
        client.addSubscription("building/floor/secondfloor/floors", "secondFloor");
        client.addSubscription("building/floor/thirdfloor/floors", "thirdFloor");
        assertEquals(3, client.getSubscriptionCount());
        List<String> subscriberList = new ArrayList<>();
        subscriberList.add("firstFloor");
        subscriberList.add("secondFloor");
        subscriberList.add("thirdFloor");
        assertEquals(subscriberList, client.getMatchingSubscribers("building/floor/+/floors"));
    }

    @Test
    public void tc06_positiveWildcardSearchPlusAndHashVerifyCountAndSubscriptions() throws IOException, InterruptedException {
        client.addSubscription("building/floor/firstfloor/105", "firstFloor");
        client.addSubscription("building/floor/firstfloor/106", "firstFloor");
        client.addSubscription("building/floor/secondfloor/201", "secondFloor");
        client.addSubscription("building/floor/thirdfloor/301", "thirdFloor");
        client.addSubscription("building/floor/firstfloor/106", "thirdFloor");
        assertEquals(3, client.getSubscriptionCount());
        List<String> subscriberList = new ArrayList<>();
        subscriberList.add("firstFloor");
        subscriberList.add("secondFloor");
        subscriberList.add("thirdFloor");
        assertEquals(subscriberList, client.getMatchingSubscribers("building/floor/+/#"));
    }

    @Test //TODO need to confirm the appropriate outcome when topic is empty
    public void tc07_emptyTopicLegalClient() throws IOException, InterruptedException {
        client.addSubscription("", "client");
        assertEquals(1, client.getSubscriptionCount());
        List<String> subscriberList = new ArrayList<>();
        subscriberList.add("client");
        assertEquals(subscriberList, client.getMatchingSubscribers(""));
    }

    @Test //TODO need to confirm the appropriate outcome when client is empty
    public void tc08_legalTopicEmptyClient() throws IOException, InterruptedException {
        client.addSubscription("legalTopic", "");
        assertEquals(1, client.getSubscriptionCount());
        List<String> subscriberList = new ArrayList<>();
        subscriberList.add("");
        assertEquals(subscriberList, client.getMatchingSubscribers("legalTopic"));
    }

    @Test
    public void tc09_nullTopicLegalClient() throws IOException, InterruptedException {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            client.addSubscription(null, "client");

        });
        String expectedMessage = "SubscriptionClient.addSubscription must not be null";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void tc10_legalTopicNullClient() throws IOException, InterruptedException {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            client.addSubscription("legalTopic", null);

        });
        String expectedMessage = "SubscriptionClient.addSubscription must not be null";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void tc11_spacesInBetween() throws IOException, InterruptedException {
        client.addSubscription("space in topic", "legalClient");
        assertEquals(1, client.getSubscriptionCount());
        List<String> subscriberList = new ArrayList<>();
        subscriberList.add("legalClient");
        assertEquals(subscriberList, client.getMatchingSubscribers("space in topic"));
    }

    @Test
    public void tc12_leadingSpaceInTopic() throws IOException, InterruptedException {
        client.addSubscription(" leadingSpaceInTopic", "legalClient");
        assertEquals(1, client.getSubscriptionCount());
        List<String> subscriberList = new ArrayList<>();
        subscriberList.add("legalClient");
        assertEquals(subscriberList, client.getMatchingSubscribers("leadingSpaceInTopic"));
    }

    @Test
    public void tc13_trailingSpaceInTopic() throws IOException, InterruptedException {
        client.addSubscription("trailingSpaceInTopic ", "legalClient");
        assertEquals(1, client.getSubscriptionCount());
        List<String> subscriberList = new ArrayList<>();
        subscriberList.add("legalClient");
        assertEquals(subscriberList, client.getMatchingSubscribers("trailingSpaceInTopic"));
    }

    @Test
    public void tc14_rootSubscription() throws IOException, InterruptedException {
        client.addSubscription("topic1 ", "legalClient");
        client.addSubscription("topic2 ", "legalClient");
        client.addSubscription("topic3 ", "legalClient");
        client.addSubscription("topic4 ", "legalClient");
        assertEquals(1, client.getSubscriptionCount());
        List<String> subscriberList = new ArrayList<>();
        subscriberList.add("legalClient");
        assertEquals(subscriberList, client.getMatchingSubscribers("#"));
    }

    @ParameterizedTest
    @MethodSource("decimalToASCIIProblemChars")
    public void tc15_asciiProblemCharactersTests(char problemChar) throws IOException, InterruptedException {
        String topic = "prefixString" + String.valueOf(problemChar) + "suffixString";
        client.addSubscription(topic, "legalClient");
        assertEquals(1, client.getSubscriptionCount());
        List<String> subscriberList = new ArrayList<>();
        subscriberList.add("legalClient");
        assertEquals(subscriberList, client.getMatchingSubscribers(topic));
    }

//    @ParameterizedTest
//    @MethodSource("decimalToASCII")
    public void tc16_asciiProblemCharactersTestsClient(char problemChar) throws IOException, InterruptedException {
        String client1 = "prefixString" + String.valueOf(problemChar) + "suffixString";
        client.addSubscription("legalTopic", client1);
        assertEquals(1, client.getSubscriptionCount());
        List<String> subscriberList = new ArrayList<>();
        subscriberList.add(client1);
        assertEquals(subscriberList, client.getMatchingSubscribers("legalTopic"));
    }


    @Test
    public void tc17_sameTopicMultipleSubscriptions() throws IOException, InterruptedException {
        client.addSubscription("topic1", "legalClient");
        client.addSubscription("topic1", "legalClient");
        assertEquals(1, client.getSubscriptionCount());
        List<String> subscriberList = new ArrayList<>();
        subscriberList.add("legalClient");
        assertEquals(subscriberList, client.getMatchingSubscribers("topic1"));
    }

    @Test
    public void tc18_caseSensitiveSearch() throws IOException, InterruptedException {
        client.addSubscription("topics/CaseSensitive", "legalClient");
        client.addSubscription("topics/CASESENSITIVE", "legalClient1");
        assertEquals(2, client.getSubscriptionCount());
        List<String> subscriberList = new ArrayList<>();
        subscriberList.add("legalClient");
        assertEquals(subscriberList, client.getMatchingSubscribers("topics/CaseSensitive"));
    }

    //@Test
    public void tc19_positiveMultipleClientsMultipleTopicsVerifyCountAndSubscriptionsRootLevel() throws IOException, InterruptedException {
        client.addSubscription("building/floor/firstfloor/105", "firstFloor");
        client.addSubscription("building/floor/firstfloor/106", "firstFloor");
        client.addSubscription("building/floor/secondfloor/201", "secondFloor");
        client.addSubscription("building/floor/thirdfloor/301", "thirdFloor");
        client.addSubscription("building/floor/firstfloor/106", "thirdFloor");
        assertEquals(3, client.getSubscriptionCount());
        List<String> subscriberList = new ArrayList<>();
        subscriberList.add("firstFloor");
        subscriberList.add("secondFloor");
        subscriberList.add("thirdFloor");
        assertEquals(subscriberList, client.getMatchingSubscribers("building/#"));

    }



    private static Stream<Character> decimalToASCII() {
        return IntStream.range(0, 256).mapToObj(integer -> ((char) integer));
    }

    private static Stream<Arguments> decimalToASCIIProblemChars() {
        return Stream.of(
                Arguments.of(" "),
                Arguments.of("("),
                Arguments.of(")"),
                Arguments.of("*"),
                Arguments.of("?"),
                Arguments.of("["),
                Arguments.of("\\"),
                Arguments.of("^"),
                Arguments.of("{"),
                Arguments.of("|")
        );
    }


}

