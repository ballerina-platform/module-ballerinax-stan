## Overview

This module provides the capability to send and receive messages by connecting to the NATS streaming server.

NATS Streaming is a data streaming system powered by NATS. It embeds, extends, and interoperates seamlessly with the core NATS platform. In addition to the features of the core NATS platform, NATS Streaming provides advanced functionality such as persistence, message replay, durable subscriptions, etc.

### Basic Usage

#### Setting up the connection

First you need to set up the connection with the NATS Streaming server. The following ways can be used to connect to a
NATS Streaming server by initializing the stan:Client or stan:Listener.

1. Connect to a server using the default URL:
```ballerina
stan:Client stanClient = check new(stan:DEFAULT_URL);
```

2. Connect to a server using the URL:
```ballerina
stan:Client stanClient = check new("nats://localhost:4222");
```

3. Connect to a server with custom configurations:
```ballerina
stan:StreamingConfiguration config = {
  clusterId: "test-cluster",
  ackTimeout: 30,
  connectionTimeout: 5;
};
stan:Client stanClient = check new("nats://localhost:4222",  config);
```

#### Publishing Messages

Once connected, use the `publishMessage` function to publish messages to a given subject as shown below.

```ballerina
string message = "hello world";
check producer->publishMessage({ content: message, subject: "demo" });
```

#### Listening to incoming messages

```ballerina
// Binds the consumer to listen to the messages published to the 'demo' subject.
@stan:ServiceConfig {
    subject: "demo"
}
service stan:Service on subscription {
    
    remote function onMessage(stan:Message message) {
    }
}
```
### Advanced Usage

#### Setting up TLS

The Ballerina NATS streaming module allows the use of TLS in communication. This setting expects a secure socket to be
set in the connection configuration as shown below.

##### Configuring TLS in the `stan:Listener`
```ballerina
stan:SecureSocket secured = {
    cert: {
        path: "<path>/truststore.p12",
        password: "password"
    },
    key: {
        path: "<path>/keystore.p12",
        password: "password"
    }
};
stan:Listener stanListener = check new("nats://serverone:4222", secureSocket = secured);
```

##### Configuring TLS in the `stan:Client`
```ballerina
stan:SecureSocket secured = {
    cert: {
        path: "<path>/truststore.p12",
        password: "password"
    },
    key: {
        path: "<path>/keystore.p12",
        password: "password"
    }
};
stan:Client stanClient = check new("nats://serverone:4222", secureSocket = secured);
```

#### Acknowledging Messages

NATS Streaming offers At-Least-Once delivery semantics, meaning that once a message has been delivered to an eligible subscriber, if an acknowledgement is not received within the configured timeout interval, NATS Streaming will attempt redelivery of the message.
If you need to acknowledge the incoming messages manually, make sure to set the `autoAck` status of the service config to false as shown below.

```ballerina
// Set `autoAck` to false.
@stan:ServiceConfig {
    subject: "demo",
    autoAck: false
}
service stan:Service on subscription {
    
    remote function onMessage(stan:Message message, stan:Caller caller) {
        // Manually acknowledge the message received.
        stan:Error? ackResult = caller->ack();
    }
}
```

#### Durable Subscriptions

Durable subscriptions allow clients to assign a durable name to a subscription when it is created. Doing this causes the NATS Streaming server to track the last acknowledged message for that clientID + durable name, so that only messages since the last acknowledged message will be delivered to the client. These subscriptions will survive a server restart.

```ballerina

// Set the client ID for the listener.
listener stan:Listener lis = new(stan:DEFAULT_URL, clientId = "c0");

// Set the durable name.
@stan:ServiceConfig {
    subject: "demo",
    durableName: "sample-name"
}
service stan:Service on lis {
    remote function onMessage(stan:Message message) {
    }
}
```
