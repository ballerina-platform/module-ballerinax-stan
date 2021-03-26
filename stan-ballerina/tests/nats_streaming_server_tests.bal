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
string receivedConsumerMessage = "";

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
    runtime:sleep(5);
    test:assertEquals(receivedConsumerMessage, message, msg = "Message received does not match.");
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
