## Package Overview

This package provides the capability to connect with NATS Streaming server and performs the 
below functionalities.

- Point to point communication (Queues)
- Pub/Sub (Topics)

### Basic Usage

#### Setting up the connection

First step is setting up the connection with the NATS Streaming server. The following ways can be used to connect to a
NATS Streaming server.

1. Connect to a server using the default URL
```ballerina
stan:Client newClient = check new(stan:DEFAULT_URL);
```

2. Connect to a server using the URL
```ballerina
stan:Client newClient = check new("nats://localhost:4222");
```

3. Connect to a server with a custom configuration
```ballerina
stan:StreamingConfiguration config = {
  clusterId: "test-cluster",
  ackTimeout: 30,
  connectionTimeout: 5;
};
stan:Client newClient = check new("nats://localhost:4222",  config);
```

#### Publishing messages

Publishing messages is handled differently in the NATS Basic server and Streaming server. The 'ballerina/nats' package provides different 
APIs to publish messages to NATS Streaming server.

##### Publishing messages to the NATS Streaming server

Once connected, publishing is accomplished via one of the below two methods.

1. Publish with the subject and the message content.
```ballerina
string message = "hello world";
stan:Error? result = producer->publishMessage({ content: message, subject: "demo" });
```

#### Listening to incoming messages

##### Listening to messages from a Streaming server

```ballerina
// Initializes the NATS Streaming listener.
listener stan:Listener subscription = new(stan:DEFAULT_URL);

// Binds the consumer to listen to the messages published to the 'demo' subject.
@stan:ServiceConfig {
    subject: "demo"
}
service stan:Service on subscription {
    
    remote function onMessage(stan:Message message, stan:Caller caller) {
    }
}
```

>**Note:** The default thread pool size used in Ballerina is the number of processors available * 2. You can configure the thread pool size by using the `BALLERINA_MAX_POOL_SIZE` environment variable.

For information on the operations, which you can perform with this package, see the below **Functions**. 

For examples on the usage of the connector, see the following.
* [Basic Streaming Publisher and Subscriber Example](https://ballerina.io/learn/by-example/nats-streaming-client.html)
* [Durable Subscriptions Example](https://ballerina.io/learn/by-example/nats-streaming-durable-subscriptions.html)
* [Queue Groups Example](https://ballerina.io/learn/by-example/nats-streaming-queue-group.html)
* [Historical Message Replay Example](https://ballerina.io/learn/by-example/nats-streaming-start-position.html)
