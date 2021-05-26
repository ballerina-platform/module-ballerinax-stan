// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/lang.'string;
import ballerina/lang.runtime as runtime;
import ballerina/log;
import ballerina/test;

Client? clientObj = ();
const SUBJECT_NAME = "nats-streaming";
const SERVICE_SUBJECT_NAME = "nats-streaming-service";
const ACK_SUBJECT_NAME = "nats-streaming-ack";
const DUMMY_SUBJECT_NAME = "nats-streaming-dummy";
const SERVICE_NO_CONFIG_NAME = "nats-streaming-service-no-config";
string receivedConsumerMessage = "";
string receivedAckMessage = "";
string noConfigServiceReceivedMessage = "";
boolean ackNegativeFlag = false;

@test:BeforeSuite
function setup() {
    Client newClient = checkpanic new(DEFAULT_URL);
    clientObj = newClient;
}

@test:Config {
    groups: ["nats-streaming"]
}
public function testConnection() {
    boolean flag = false;
    Client? con = clientObj;
    if (con is Client) {
        flag = true;
    }
    test:assertTrue(flag, msg = "NATS Connection creation failed.");
}

@test:Config {
    groups: ["nats-streaming"]
}
public isolated function testConnectionNegative() {
    Client|error? newClient = new("nats://localhost:5222");
    if (!(newClient is error)) {
        test:assertFail("Error expected for creating non-existent connection.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
public isolated function testConnectionWithMultipleServers() {
    boolean flag = false;
    Client? con = checkpanic new([DEFAULT_URL, DEFAULT_URL]);
    if (con is Client) {
        flag = true;
    }
    test:assertTrue(flag, msg = "NATS Connection creation failed.");
}

@test:Config {
    groups: ["nats-streaming"]
}
public isolated function testConnectionWithToken() {
    Tokens myToken = { token: "MyToken" };
    boolean flag = false;
    Client? con = checkpanic new("nats://localhost:4223", auth = myToken);
    if (con is Client) {
        flag = true;
    }
    test:assertTrue(flag, msg = "NATS Streaming connection creation with credentails failed.");
}

@test:Config {
    groups: ["nats-streaming"]
}
public isolated function testConnectionWithCredentials() {
    Credentials myCredentials = {
        username: "ballerina",
        password: "ballerina123"
    };
    boolean flag = false;
    Client? con = checkpanic new("nats://localhost:4224", auth = myCredentials);
    if (con is Client) {
        flag = true;
    }
    test:assertTrue(flag, msg = "NATS Streaming connection creation with credentails failed.");
}

@test:Config {
    dependsOn: [testConnection],
    groups: ["nats-streaming"]
}
public isolated function testConnectionClose() {
    Client con = checkpanic new(DEFAULT_URL);
    error? closeResult = con.close();
    if (closeResult is error) {
        test:assertFail("Error in closing connection.");
    }
}

@test:Config {
    dependsOn: [testConnection],
    groups: ["nats-streaming"]
}
public function testProducer() {
    Client? con = clientObj;
    if (con is Client) {
        string message = "Hello World";
        Error|string result = con->publishMessage({ content: message.toBytes(), subject: SUBJECT_NAME });
        test:assertTrue(result is string, msg = "Producing a message to the broker caused an error.");
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    dependsOn: [testConnectionWithMultipleServers],
    groups: ["nats-streaming"]
}
public isolated function testProducerWithMultipleServers() {
    Client? con = checkpanic new([DEFAULT_URL, DEFAULT_URL]);
    if (con is Client) {
        string message = "Hello World";
        Error|string result = con->publishMessage({ content: message.toBytes(),
                                                    subject: SERVICE_SUBJECT_NAME });
        test:assertTrue(result is string, msg = "Producing a message to the broker caused an error.");
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-streaming"]
}
public function testConsumerService() {
    string message = "Testing Consumer Service";
    Listener sub = checkpanic new(DEFAULT_URL);
    Client newClient = checkpanic new(DEFAULT_URL);
    checkpanic sub.attach(consumerService);
    checkpanic sub.'start();
    string id = checkpanic newClient->publishMessage({ content: message.toBytes(), subject: SERVICE_SUBJECT_NAME});
    runtime:sleep(15);
    test:assertEquals(receivedConsumerMessage, message, msg = "Message received does not match.");
    checkpanic newClient.close();
}

@test:Config {
    dependsOn: [testConnectionWithMultipleServers],
    groups: ["nats-streaming"]
}
public function testConsumerServiceWithMultipleServers() {
    string message = "Testing Multiple Server Consumer Service";
    Listener sub = checkpanic new([DEFAULT_URL, DEFAULT_URL]);
    Client newClient = checkpanic new([DEFAULT_URL, DEFAULT_URL]);
    checkpanic sub.attach(consumerService);
    checkpanic sub.'start();
    string id = checkpanic newClient->publishMessage({ content: message.toBytes(),
                                                       subject: SERVICE_SUBJECT_NAME });
    runtime:sleep(15);
    test:assertEquals(receivedConsumerMessage, message, msg = "Message received does not match.");
    checkpanic newClient.close();
}


@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-streaming"]
}
public function testConsumerServiceWithAck() {
    string message = "Testing Consumer Service With Acknowledgement";
    Listener sub = checkpanic new(DEFAULT_URL);
    Client newClient = checkpanic new(DEFAULT_URL);
    checkpanic sub.attach(ackService);
    checkpanic sub.'start();
    string id = checkpanic newClient->publishMessage({ content: message.toBytes(), subject: ACK_SUBJECT_NAME});
    runtime:sleep(15);
    test:assertEquals(receivedAckMessage, message, msg = "Message received does not match.");
    checkpanic newClient.close();
    checkpanic sub.close();
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-streaming"]
}
public function testConsumerServiceWithAckNegative() {
    string message = "Testing Consumer Service With Acknowledgement Negative";
    Listener sub = checkpanic new(DEFAULT_URL);
    Client newClient = checkpanic new(DEFAULT_URL);
    checkpanic sub.attach(ackNegativeService);
    checkpanic sub.'start();
    string id = checkpanic newClient->publishMessage({ content: message.toBytes(), subject: ACK_SUBJECT_NAME});
    runtime:sleep(15);
    test:assertTrue(ackNegativeFlag, msg = "Manual acknowledgement did not fail.");
    checkpanic newClient.close();
    checkpanic sub.close();
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-streaming"]
}
public function testConsumerServiceDetach1() {
    Listener sub = checkpanic new(DEFAULT_URL);
    checkpanic sub.attach(dummyService);
    checkpanic sub.'start();
    error? detachResult = sub.detach(dummyService);
    if (detachResult is error) {
        test:assertFail("Detaching service failed.");
    }
    error? stopResult = sub.immediateStop();
    if (stopResult is error) {
        test:assertFail("Stopping listener failed.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-streaming"]
}
public function testConsumerServiceDetach2() {
    Listener sub = checkpanic new(DEFAULT_URL);
    checkpanic sub.attach(dummyService);
    checkpanic sub.'start();
    error? detachResult = sub.detach(dummyService);
    if (detachResult is error) {
        test:assertFail("Detaching service failed.");
    }
    error? stopResult = sub.gracefulStop();
    if (stopResult is error) {
        test:assertFail("Stopping listener failed.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-streaming"]
}
public function testConsumerServiceAck() {
    string message = "Testing Consumer Service with Ack";
    Listener sub = checkpanic new(DEFAULT_URL);
    Client newClient = checkpanic new(DEFAULT_URL);
    checkpanic sub.attach(ackService);
    checkpanic sub.'start();
    string id = checkpanic newClient->publishMessage({ content: message.toBytes(), subject: ACK_SUBJECT_NAME});
    runtime:sleep(5);
    test:assertEquals(receivedAckMessage, message, msg = "Message received does not match.");
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-streaming"]
}
public function testNoConfigConsumerService() {
    string message = "Testing No Subject Consumer Service";
    Listener sub = checkpanic new(DEFAULT_URL);
    Client newClient = checkpanic new(DEFAULT_URL);
    checkpanic sub.attach(noConfigService, SERVICE_NO_CONFIG_NAME);
    checkpanic sub.'start();
    string id = checkpanic newClient->publishMessage({ content: message.toBytes(), subject: SERVICE_NO_CONFIG_NAME });
    runtime:sleep(15);
    test:assertEquals(noConfigServiceReceivedMessage, message, msg = "Message received does not match.");

    error? attachResult = sub.attach(noConfigService);
    if !(attachResult is error){
        test:assertFail("Expected failure to attach did not fail.");
    }
    checkpanic newClient.close();
    checkpanic sub.close();
}


Service consumerService =
@ServiceConfig {
    subject: SERVICE_SUBJECT_NAME
}
service object {
    remote function onMessage(Message msg) {
        string|error messageContent = 'string:fromBytes(msg.content);
        if (messageContent is string) {
            receivedConsumerMessage = <@untainted> messageContent;
            log:printInfo("Message Received: " + receivedConsumerMessage);
        }
    }
};

Service ackService =
@ServiceConfig {
    subject: ACK_SUBJECT_NAME,
    autoAck: false
}
service object {
    remote function onMessage(Message msg, Caller caller) {
        string|error messageContent = 'string:fromBytes(msg.content);
        if (messageContent is string) {
            receivedAckMessage = <@untainted> messageContent;
            log:printInfo("Message Received: " + receivedAckMessage);
        }
        checkpanic caller->ack();
    }
};

Service ackNegativeService =
@ServiceConfig {
    subject: ACK_SUBJECT_NAME
}
service object {
    remote function onMessage(Message msg, Caller caller) {
        string|error messageContent = 'string:fromBytes(msg.content);
        if (messageContent is string) {
            receivedAckMessage = <@untainted> messageContent;
            log:printInfo("Message Received: " + receivedAckMessage);
        }
        Error? ackResult = caller->ack();
        if (ackResult is error){
            ackNegativeFlag = true;
        }
    }
};

Service dummyService =
@ServiceConfig {
    subject: DUMMY_SUBJECT_NAME
}
service object {
    remote isolated function onMessage(Message msg, Caller caller) {
    }
};

Service noConfigService =
service object {
    remote function onMessage(Message msg, Caller caller) {
        string|error messageContent = 'string:fromBytes(msg.content);
            if (messageContent is string) {
                noConfigServiceReceivedMessage = <@untainted> messageContent;
                log:printInfo("Message Received: " + noConfigServiceReceivedMessage);
            }
    }
};