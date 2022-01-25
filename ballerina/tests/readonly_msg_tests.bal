// Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

const READONLY_MSG = "nats-readonly-msg";
const READONLY_MSG_CALLER = "nats-readonly-msg-caller";

string receivedReadOnlyMessage = "";
string receivedReadOnlyMessageWithCaller = "";

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-streaming"]
}
function testConsumerServiceWithReadOnlyParams() returns error? {
    string message = "Testing Consumer Service With Readonly Message";
    Listener sub = check new(DEFAULT_URL);
    Client newClient = check new(DEFAULT_URL);
    check sub.attach(consumerServiceWithReadonlyMessage);
    check sub.'start();
    string id = check newClient->publishMessage({ content: message.toBytes(),
                                                       subject: READONLY_MSG});
    runtime:sleep(5);
    test:assertEquals(receivedReadOnlyMessage, message, msg = "Message received does not match.");
    check newClient.close();
    return;
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-streaming"]
}
function testConsumerServiceWithReadOnlyParams2() returns error? {
    string message = "Testing Consumer Service With Message and Caller";
    Listener sub = check new(DEFAULT_URL);
    Client newClient = check new(DEFAULT_URL);
    check sub.attach(consumerServiceWithReadonlyMessageAndCaller);
    check sub.'start();
    string id = check newClient->publishMessage({ content: message.toBytes(),
                                                       subject: READONLY_MSG_CALLER});
    runtime:sleep(5);
    test:assertEquals(receivedReadOnlyMessageWithCaller, message, msg = "Message received does not match.");
    check newClient.close();
    return;
}

Service consumerServiceWithReadonlyMessage =
@ServiceConfig {
    subject: READONLY_MSG
}
service object {
    remote function onMessage(readonly & Message msg) {
        string|error messageContent = 'string:fromBytes(msg.content);
        if messageContent is string {
            receivedReadOnlyMessage = messageContent;
            log:printInfo("Message Received: " + receivedReadOnlyMessage);
        }
    }
};

Service consumerServiceWithReadonlyMessageAndCaller =
@ServiceConfig {
    subject: READONLY_MSG_CALLER
}
service object {
    remote function onMessage(readonly & Message msg, Caller caller) returns error? {
        string|error messageContent = 'string:fromBytes(msg.content);
        if messageContent is string {
            receivedReadOnlyMessageWithCaller = messageContent;
            log:printInfo("Message Received: " + receivedReadOnlyMessageWithCaller);
        }
        check caller->ack();
        return;
    }
};
