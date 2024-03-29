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

import ballerina/test;

@test:Config {
    groups: ["nats-streaming"]
}
isolated function testConnectionWithToken() {
    Tokens myToken = { token: "MyToken" };
    Client|Error con = new("nats://localhost:4223", auth = myToken);
    if con is Error {
        test:assertFail("NATS Streaming connection creation with token failed.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
isolated function testConnectionWithCredentials() {
    Credentials myCredentials = {
        username: "ballerina",
        password: "ballerina123"
    };
    Client|Error con = new("nats://localhost:4224", auth = myCredentials);
    if !(con is Client) {
        test:assertFail("NATS Streaming connection creation with credentails failed.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
isolated function testConnectionWithTokenNegative() {
    Tokens myToken = { token: "IncorrectToken" };
    Client|Error con = new("nats://localhost:4223", auth = myToken);
    if !(con is Error) {
        test:assertFail("Expected failure for connecting to server without token.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
isolated function testConnectionWithCredentialsNegative() {
    Credentials myCredentials = {
        username: "ballerina",
        password: "IncorrectPassword"
    };
    Client|Error con = new("nats://localhost:4224", auth = myCredentials);
    if !(con is Error) {
        test:assertFail("Expected failure for connecting to server with invalid credentials.");
    }
}
