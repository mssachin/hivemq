# SAR Team Take-home Assignment: QA Software Engineer

## Task

Add and extend the test cases of **SubscriptionServerSystemTest** to find as many issues as possible.

## The Subscription Server

You should be able to rely on the SubscriptionServerExtension and the SubscriptionClient to conveniently interact with the subscription server via code in **SubscriptionServerSystemTest**.

However, if you find the need to interact with the server manually, please refer to the following:

### Running the Server

```
java -jar src/test/resources/subscription-server.jar <port> <thread count> <request timeout in seconds>
```

For example:

```
$ java -jar src/test/resources/subscription-server.jar 8080 2 10
[main] INFO org.eclipse.jetty.server.Server - jetty-11.0.13; built: 2022-12-07T20:47:15.149Z; git: a04bd1ccf844cf9bebc12129335d7493111cbff6; jvm 11.0.16.1+1-LTS
[main] INFO org.eclipse.jetty.server.AbstractConnector - Started ServerConnector@76a3e297{HTTP/1.1, (http/1.1)}{0.0.0.0:8080}
[main] INFO org.eclipse.jetty.server.Server - Started Server@62fdb4a6{STARTING}[11.0.13,sto=0] @240ms
Server is running on: http://localhost:8080
```

### Example Requests

```
$ curl -X POST -d '{ "clientId": "client1", "topicFilter": "a/b/c" }' localhost:8080/subscriptions
$ curl -X POST -d '{ "clientId": "client2", "topicFilter": "a/+" }' localhost:8080/subscriptions
$ curl -X POST -d '{ "clientId": "client3", "topicFilter": "a/#" }' localhost:8080/subscriptions
$ curl localhost:8080/subscriptions
{"count":3}
$ curl -X POST -d '{ "topic": "a/b/c" }' localhost:8080/subscriptions/match
{"subscribers":["client1","client2","client3"]}
$ curl -X POST -d '{ "topic": "a/b/d" }' localhost:8080/subscriptions/match
{"subscribers":["client2","client3"]}
```
