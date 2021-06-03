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

import ballerina/test;

const SUBJECT_NAME = "subject";

@test:Config {
    groups: ["nats-streaming"]
}
isolated function testConnection() {
    Client|Error con = new("nats://localhost:4222");
    if !(con is Client) {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
isolated function testConnectionNegative() {
    Client|Error newClient = new("nats://localhost:5222");
    if !(newClient is Error) {
        test:assertFail("Error expected for creating non-existent connection.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
isolated function testConnectionWithMultipleServers() {
    Client|Error con = new([DEFAULT_URL, DEFAULT_URL]);
    if !(con is Client) {
        test:assertFail("NATS Connection creation with multiple servers failed.");
    }
}

@test:Config {
    dependsOn: [testConnection],
    groups: ["nats-streaming"]
}
isolated function testConnectionClose() returns error? {
    Client con = check new(DEFAULT_URL);
    error? closeResult = con.close();
    if closeResult is error {
        test:assertFail("Error in closing connection.");
    }
}

@test:Config {
    dependsOn: [testConnection],
    groups: ["nats-streaming"]
}
isolated function testProducer() returns error? {
    Client con = check new(DEFAULT_URL);
    string message = "Hello World";
    Error|string result = con->publishMessage({ content: message.toBytes(), subject: SUBJECT_NAME });
    test:assertTrue(result is string, msg = "Producing a message to the broker caused an error.");
}

@test:Config {
    dependsOn: [testConnectionWithMultipleServers],
    groups: ["nats-streaming"]
}
isolated function testProducerWithMultipleServers() returns error? {
    Client con = check new([DEFAULT_URL, DEFAULT_URL]);
    string message = "Hello World";
    Error|string result = con->publishMessage({ content: message.toBytes(),
                                                subject: SUBJECT_NAME });
    test:assertTrue(result is string, msg = "Producing a message to the broker caused an error.");
}
