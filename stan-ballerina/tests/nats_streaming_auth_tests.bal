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
    groups: ["nats-streaming"]
}
public isolated function testConnectionWithTokenNegative() {
    Tokens myToken = { token: "IncorrectToken" };
    Client|error? con = new("nats://localhost:4223", auth = myToken);
    if !(con is error) {
        test:assertFail("Expected failure for connecting to server without token.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
public isolated function testConnectionWithCredentialsNegative() {
    Credentials myCredentials = {
        username: "ballerina",
        password: "IncorrectPassword"
    };
    boolean flag = false;
    Client|error? con = new("nats://localhost:4224", auth = myCredentials);
    if !(con is error) {
        test:assertFail("Expected failure for connecting to server with invalid credentials.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
public isolated function testTlsConnection1() {
    SecureSocket secured = {
        cert: {
            path: "tests/certs/cert.pfx",
            password: "password"
        },
        protocol: {
            name: TLS
        }
    };
    // TODO: Resolve TLS issues
    Client|Error newClient = new("nats://localhost:4225", secureSocket = secured);
    if (newClient is error) {
        //log:printInfo("Error: " + newClient.message());
        //test:assertFail("NATS Connection initialization with TLS failed.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
public isolated function testTlsConnection2() {
    SecureSocket secured = {
        cert: {
            path: "tests/certs/cert.pfx",
            password: "password"
        },
        key: {
            path: "tests/certs/cert.pfx",
            password: "password"
        },
         protocol: {
            name: TLS
         }
    };
    Client|Error newClient = new("nats://localhost:4225", secureSocket = secured);
    if (newClient is Client) {
        test:assertFail("Error expected for NATS Connection initialization with TLS.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
public isolated function testTlsConnection3() {
    SecureSocket secured = {
        cert: {
            path: "tests/certs/cert.pfx",
            password: "password"
        }
    };
    // TODO: Resolve TLS issues
    Client|Error newClient = new("nats://localhost:4225", secureSocket = secured);
    if (newClient is error) {
        //log:printInfo("Error: " + newClient.message());
        //test:assertFail("NATS Connection initialization with TLS failed.");
    }
}
