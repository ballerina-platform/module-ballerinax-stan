// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

const CONSUMER_SERVICE_SUBJECT_NAME = "nats-streaming-consumer-service";
const ACK_SUBJECT_NAME = "nats-streaming-ack";
const DUMMY_SUBJECT_NAME = "nats-streaming-dummy";
const INVALID_SUBJECT_NAME = "nats-streaming-invalid";
const SERVICE_NO_CONFIG_NAME = "nats-streaming-service-no-config";
const QUEUE_SUBJECT_NAME = "nats-streaming-queue";
const DURABLE_SUBJECT_NAME = "nats-streaming-queue";

string receivedConsumerMessage = "";
string receivedAckMessage = "";
string noConfigServiceReceivedMessage = "";
string receivedQueueMessage = "";
string receivedDurableMessage = "";

boolean ackNegativeFlag = false;
boolean invalidServiceFlag = true;

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-streaming"]
}
function testConsumerService() returns error? {
    string message = "Testing Consumer Service";
    Listener sub = check new(DEFAULT_URL);
    Client newClient = check new(DEFAULT_URL);
    check sub.attach(consumerService);
    check sub.'start();
    string id = check newClient->publishMessage({ content: message.toBytes(),
                                                       subject: CONSUMER_SERVICE_SUBJECT_NAME});
    runtime:sleep(5);
    test:assertEquals(receivedConsumerMessage, message, msg = "Message received does not match.");
    check newClient.close();
}

@test:Config {
    dependsOn: [testConnectionWithMultipleServers],
    groups: ["nats-streaming"]
}
function testConsumerServiceWithMultipleServers() returns error? {
    string message = "Testing Multiple Server Consumer Service";
    Listener sub = check new([DEFAULT_URL, DEFAULT_URL]);
    Client newClient = check new([DEFAULT_URL, DEFAULT_URL]);
    check sub.attach(consumerService);
    check sub.'start();
    string id = check newClient->publishMessage({ content: message.toBytes(),
                                                       subject: CONSUMER_SERVICE_SUBJECT_NAME });
    runtime:sleep(5);
    test:assertEquals(receivedConsumerMessage, message, msg = "Message received does not match.");
    check newClient.close();
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-streaming"]
}
function testConsumerServiceWithAck() returns error? {
    string message = "Testing Consumer Service With Acknowledgement";
    Listener sub = check new(DEFAULT_URL);
    Client newClient = check new(DEFAULT_URL);
    check sub.attach(ackService);
    check sub.'start();
    string id = check newClient->publishMessage({ content: message.toBytes(), subject: ACK_SUBJECT_NAME});
    runtime:sleep(5);
    test:assertEquals(receivedAckMessage, message, msg = "Message received does not match.");
    check newClient.close();
    check sub.close();
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-streaming"]
}
function testConsumerServiceWithAckNegative() returns error? {
    string message = "Testing Consumer Service With Acknowledgement Negative";
    Listener sub = check new(DEFAULT_URL);
    Client newClient = check new(DEFAULT_URL);
    check sub.attach(ackNegativeService);
    check sub.'start();
    string id = check newClient->publishMessage({ content: message.toBytes(), subject: ACK_SUBJECT_NAME});
    runtime:sleep(5);
    test:assertTrue(ackNegativeFlag, msg = "Manual acknowledgement did not fail.");
    check newClient.close();
    check sub.close();
}

@test:Config {
    groups: ["nats-streaming"]
}
function testInvalidConsumerService() returns error? {
    string message = "Testing Invalid Consumer Service";
    Listener sub = check new(DEFAULT_URL);
    Client newClient = check new(DEFAULT_URL);
    check sub.attach(invalidService);
    check sub.'start();
    string id = check newClient->publishMessage({ content: message.toBytes(), subject: INVALID_SUBJECT_NAME});
    runtime:sleep(5);
    test:assertTrue(invalidServiceFlag, msg = "Message received does not match.");
    check newClient.close();
    check sub.close();
}


@test:Config {
   groups: ["nats-streaming"]
}
function testConsumerServiceWithQueue() returns error? {
    string message = "Testing Consumer Service With Queue";
    Listener sub = check new(DEFAULT_URL);
    Client newClient = check new(DEFAULT_URL);
    check sub.attach(queueService);
    check sub.'start();
    string id = check newClient->publishMessage({ content: message.toBytes(), subject: QUEUE_SUBJECT_NAME});
    runtime:sleep(5);
    test:assertEquals(receivedQueueMessage, message, msg = "Message received does not match.");
    check newClient.close();
    check sub.close();
}

@test:Config {
   groups: ["nats-streaming"]
}
function testConsumerServiceWithDurable() returns error? {
    string message = "Testing Consumer Service With Durable";
    Listener sub = check new(DEFAULT_URL);
    Client newClient = check new(DEFAULT_URL);
    check sub.attach(durableService);
    check sub.'start();
    string id = check newClient->publishMessage({ content: message.toBytes(), subject: DURABLE_SUBJECT_NAME });
    runtime:sleep(5);
    test:assertEquals(receivedDurableMessage, message, msg = "Message received does not match.");
    check newClient.close();
    check sub.close();
}

@test:Config {
    groups: ["nats-streaming"]
}
isolated function testConsumerWithToken() returns error? {
    Tokens myToken = { token: "MyToken" };
    Listener|error? sub = new("nats://localhost:4223", auth = myToken);
    if !(sub is Listener) {
        test:assertFail("Connecting to server with token failed.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
isolated function testConsumerWithCredentials() {
    Credentials myCredentials = {
        username: "ballerina",
        password: "ballerina123"
    };
    Listener|error? sub = new("nats://localhost:4224", auth = myCredentials);
    if !(sub is Listener) {
        test:assertFail("Connecting to server with credentials failed.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
isolated function testConsumerWithTokenNegative() {
    Tokens myToken = { token: "IncorrectToken" };
    Listener|error? sub = new("nats://localhost:4223", auth = myToken);
    if !(sub is error) {
        test:assertFail("Expected failure for connecting to server with invalid token.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
isolated function testConsumerWithCredentialsNegative() {
    Credentials myCredentials = {
        username: "ballerina",
        password: "IncorrectPassword"
    };
    Listener|error? sub = new("nats://localhost:4224", auth = myCredentials);
    if !(sub is error) {
        test:assertFail("Expected failure for connecting to server with invalid credentials.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-streaming"]
}
function testConsumerServiceDetach1() returns error? {
    Listener sub = check new(DEFAULT_URL);
    check sub.attach(dummyService);
    check sub.'start();
    error? detachResult = sub.detach(dummyService);
    if detachResult is error {
        test:assertFail("Detaching service failed.");
    }
    error? stopResult = sub.immediateStop();
    if stopResult is error {
        test:assertFail("Stopping listener failed.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-streaming"]
}
function testConsumerServiceDetach2() returns error? {
    Listener sub = check new(DEFAULT_URL);
    check sub.attach(dummyService);
    check sub.'start();
    error? detachResult = sub.detach(dummyService);
    if detachResult is error {
        test:assertFail("Detaching service failed.");
    }
    error? stopResult = sub.gracefulStop();
    if stopResult is error {
        test:assertFail("Stopping listener failed.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-streaming"]
}
function testConsumerServiceDetach3() returns error? {
    Listener sub = check new(DEFAULT_URL);
    check sub.'start();
    error? detachResult = sub.detach(dummyService);
    if detachResult is error {
        test:assertFail("Detaching service failed.");
    }
    error? stopResult = sub.gracefulStop();
    if stopResult is error {
        test:assertFail("Stopping listener failed.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-streaming"]
}
function testNoConfigConsumerService() returns error? {
    string message = "Testing No Subject Consumer Service";
    Listener sub = check new(DEFAULT_URL);
    Client newClient = check new(DEFAULT_URL);
    check sub.attach(noConfigService, SERVICE_NO_CONFIG_NAME);
    check sub.'start();
    string id = check newClient->publishMessage({ content: message.toBytes(), subject: SERVICE_NO_CONFIG_NAME });
    runtime:sleep(5);
    test:assertEquals(noConfigServiceReceivedMessage, message, msg = "Message received does not match.");

    error? attachResult = sub.attach(noConfigService);
    if !(attachResult is error){
        test:assertFail("Expected failure to attach did not fail.");
    }
    check newClient.close();
    check sub.close();
}

Service consumerService =
@ServiceConfig {
    subject: CONSUMER_SERVICE_SUBJECT_NAME
}
service object {
    remote function onMessage(Message msg) {
        string|error messageContent = 'string:fromBytes(msg.content);
        if messageContent is string {
            receivedConsumerMessage = messageContent;
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
    remote function onMessage(Message msg, Caller caller) returns error? {
        string|error messageContent = 'string:fromBytes(msg.content);
        if messageContent is string {
            receivedAckMessage = messageContent;
            log:printInfo("Message Received: " + receivedAckMessage);
        }
        check caller->ack();
    }
};

Service ackNegativeService =
@ServiceConfig {
    subject: ACK_SUBJECT_NAME
}
service object {
    remote function onMessage(Message msg, Caller caller) {
        string|error messageContent = 'string:fromBytes(msg.content);
        if messageContent is string {
            receivedAckMessage = messageContent;
            log:printInfo("Message Received: " + receivedAckMessage);
        }
        Error? ackResult = caller->ack();
        if ackResult is error {
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

Service queueService =
@ServiceConfig {
    subject: QUEUE_SUBJECT_NAME,
    queueName: "testQueue"
}
service object {
    remote function onMessage(Message msg, Caller caller) {
        string|error messageContent = 'string:fromBytes(msg.content);
        if messageContent is string {
            receivedQueueMessage = messageContent;
            log:printInfo("Message Received: " + receivedQueueMessage);
        }
    }
};

Service durableService =
@ServiceConfig {
    subject: DURABLE_SUBJECT_NAME,
    durableName: "testDurable"
}
service object {
    remote function onMessage(Message msg, Caller caller) {
        string|error messageContent = 'string:fromBytes(msg.content);
        if messageContent is string {
            receivedDurableMessage = messageContent;
            log:printInfo("Message Received: " + receivedDurableMessage);
        }
    }
};

Service noConfigService =
service object {
    remote function onMessage(Message msg, Caller caller) {
        string|error messageContent = 'string:fromBytes(msg.content);
            if messageContent is string {
                noConfigServiceReceivedMessage = messageContent;
                log:printInfo("Message Received: " + noConfigServiceReceivedMessage);
            }
    }
};

Service invalidService =
@ServiceConfig {
    subject: INVALID_SUBJECT_NAME
}
service object {
    remote function onMessage(Message msg, Caller caller, string invalidArgument) {
        invalidServiceFlag = false;
    }
};
