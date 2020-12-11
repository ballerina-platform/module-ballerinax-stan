## Module Overview

This module provides the capability to connect with NATS Streaming server and performs the 
below functionalities.

- Point to point communication (Queues)
- Pub/Sub (Topics)
- Request/Reply

### Basic Usage

#### Setting up the connection

First step is setting up the connection with the NATS Streaming server. The following ways can be used to connect to a
NATS Streaming server.

1. Connect to a server using the URL
```ballerina
stan:Client newClient = new("nats://localhost:4222");
```

2. Connect to one or more servers with a custom configuration
```ballerina
stan:Client newClient = new({"nats://serverone:4222", "nats://servertwo:4222"},  config);
```

#### Publishing messages

Publishing messages is handled differently in the NATS Basic server and Streaming server. The 'ballerina/nats' module provides different 
APIs to publish messages to NATS Streaming server.

##### Publishing messages to the NATS Streaming server

Once connected, publishing is accomplished via one of the below two methods.

1. Publish with the subject and the message content.
```ballerina
string message = "hello world";
stan:Error? result = producer->publish(subject, message.toBytes());
```

#### Listening to incoming messages

##### Listening to messages from a Streaming server

```ballerina
// Initializes the NATS Streaming listener.
listener stan:Listener subscription = new;

// Binds the consumer to listen to the messages published to the 'demo' subject.
@stan:ServiceConfig {
    subject: "demo"
}
service demo on subscription {

    resource function onMessage(stan:Message msg, stan:Caller caller) {
    }


}
```

>**Note:** The default thread pool size used in Ballerina is the number of processors available * 2. You can configure the thread pool size by using the `BALLERINA_MAX_POOL_SIZE` environment variable.

For information on the operations, which you can perform with this module, see the below **Functions**. 

For examples on the usage of the connector, see the following.
* [Basic Streaming Publisher and Subscriber Example](https://ballerina.io/swan-lake/learn/by-example/nats-streaming-client.html)
* [Streaming Publisher and Subscriber With Data Binding Example](https://ballerina.io/swan-lake/learn/by-example/nats-streaming-consumer-with-data-binding.html)
* [Durable Subscriptions Example](https://ballerina.io/swan-lake/learn/by-example/nats-streaming-durable-subscriptions.html)
* [Queue Groups Example](https://ballerina.io/swan-lake/learn/by-example/nats-streaming-queue-group.html)
* [Historical Message Replay Example](https://ballerina.io/swan-lake/learn/by-example/nats-streaming-start-position.html)
