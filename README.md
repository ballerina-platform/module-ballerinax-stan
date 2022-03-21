Ballerina NATS Streaming Library
===================

[![Build](https://github.com/ballerina-platform/module-ballerinax-stan/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerinax-stan/actions/workflows/build-timestamped-master.yml)
[![Trivy](https://github.com/ballerina-platform/module-ballerinax-stan/actions/workflows/trivy-scan.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerinax-stan/actions/workflows/trivy-scan.yml)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerinax-stan.svg)](https://github.com/ballerina-platform/module-ballerinax-stan/commits/master)
[![codecov](https://codecov.io/gh/ballerina-platform/module-ballerinax-stan/branch/main/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerinax-stan)

This library provides the capability to send and receive messages by connecting to the NATS streaming server.

NATS Streaming is a data streaming system powered by NATS. It embeds, extends, and interoperates seamlessly with the core NATS platform. In addition to the features of the core NATS platform, NATS Streaming provides advanced functionality such as persistence, message replay, durable subscriptions, etc.

### Basic usage

#### Set up the connection

First, you need to set up the connection with the NATS Streaming server. The following ways can be used to connect to a
NATS Streaming server by initializing the `stan:Client` or `stan:Listener`.

1. Connect to a server using the default URL:
```ballerina
stan:Client stanClient = check new(stan:DEFAULT_URL);
```

2. Connect to a server using a specific URL:
```ballerina
stan:Client stanClient = check new("nats://localhost:4222");
```

3. Connect to a server with the custom configurations:
```ballerina
stan:StreamingConfiguration config = {
  clusterId: "test-cluster",
  ackTimeout: 30,
  connectionTimeout: 5;
};
stan:Client stanClient = check new("nats://localhost:4222",  config);
```

#### Publish messages

Once connected, use the `publishMessage` function to publish messages to a given subject as shown below.

```ballerina
string message = "hello world";
check producer->publishMessage({ content: message, subject: "demo" });
```

#### Listen to incoming messages

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
### Advanced usage

#### Set up TLS

The Ballerina NATS streaming library allows the use of TLS in communication. This setting expects a secure socket to be
set in the connection configuration as shown below.

##### Configure TLS in the `stan:Listener`
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

##### Configure TLS in the `stan:Client`
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

#### Acknowledge messages

NATS Streaming offers At-Least-Once delivery semantics meaning that once a message has been delivered to an eligible subscriber if an acknowledgment is not received within the configured timeout interval, NATS Streaming will attempt redelivery of the message.
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

#### Durable subscriptions

Durable subscriptions allow clients to assign a durable name to a subscription when it is created. Doing this causes the NATS Streaming server to track the last-acknowledged message for that `clientID` + `durable name` so that only messages since the last acknowledged message will be delivered to the client. These subscriptions will survive a server restart.

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

## Issues and projects 

Issues and Projects tabs are disabled for this repository as this is part of the Ballerina Standard Library. To report bugs, request new features, start new discussions, view project boards, etc. please visit Ballerina Standard Library [parent repository](https://github.com/ballerina-platform/ballerina-standard-library). 

This repository only contains the source code for the library.

## Build from the source

### Set up the prerequisites

* Download and install Java SE Development Kit (JDK) version 11 (from one of the following locations).

   * [Oracle](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)

   * [OpenJDK](https://adoptopenjdk.net/)

        > **Note:** Set the JAVA_HOME environment variable to the path name of the directory into which you installed JDK.
     
2. Download and install Docker as follows. (The stan library is tested with a docker-based integration test environment. 
The before suite initializes the docker container before executing the tests).
   
   * Installing Docker on Linux
   
        > **Note:** These commands retrieve content from the `get.docker.com` website in a quiet output-document mode and installs it.
   
          wget -qO- https://get.docker.com/ | sh
   
   * For instructions on installing Docker on Mac, go to <a target="_blank" href="https://docs.docker.com/docker-for-mac/">Get Started with Docker for Mac</a>.
  
   * For information on installing Docker on Windows, goo to <a target="_blank" href="https://docs.docker.com/docker-for-windows/">Get Started with Docker for Windows</a>.

### Build the source

Execute the commands below to build from source.

1. To build the library:
   ```    
   ./gradlew clean build
   ```

2. To run the tests:
   ```
   ./gradlew clean test
   ```
3. To build the library without the tests:
   ```
   ./gradlew clean build -x test
   ```
4. To debug package implementation:
   ```
   ./gradlew clean build -Pdebug=<port>
   ```
5. To debug the library with Ballerina language:
   ```
   ./gradlew clean build -PbalJavaDebug=<port>
   ```
6. Publish ZIP artifact to the local `.m2` repository:
   ```
   ./gradlew clean build publishToMavenLocal
   ```
7. Publish the generated artifacts to the local Ballerina central repository:
   ```
   ./gradlew clean build -PpublishToLocalCentral=true
   ```
8. Publish the generated artifacts to the Ballerina central repository:
   ```
   ./gradlew clean build -PpublishToCentral=true
   ```

## Contribute to Ballerina

As an open source project, Ballerina welcomes contributions from the community. 

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful links

* For more information go to the [`stan` library](https://lib.ballerina.io/ballerinax/stan/latest).
* For example demonstrations of the usage, go to [Ballerina By Examples](https://ballerina.io/learn/by-example/).
* Chat live with us via our [Slack channel](https://ballerina.io/community/slack/).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
