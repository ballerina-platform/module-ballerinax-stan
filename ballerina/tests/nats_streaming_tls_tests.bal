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
isolated function testTlsConnection() {
    SecureSocket secured = {
        cert: {
            path: "tests/certs/truststore.jks",
            password: "password"
        },
        key: {
            path: "tests/certs/keystore.jks",
            password: "password"
        }
    };
    Client|error newClient = new("nats://localhost:4225", clusterId = "my_cluster", secureSocket = secured);
    if (newClient is error) {
        test:assertFail("Error occurred in NATS Connection initialization with TLS.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
isolated function testTlsConnection2() {
    SecureSocket secured = {
        cert: {
            path: "tests/certs/truststore2.jks",
            password: "password"
        },
        key: {
            path: "tests/certs/keystore2.jks",
            password: "password"
        }
    };
    Client|error newClient = new("nats://localhost:4222", clusterId = "my_cluster", secureSocket = secured);
    if !(newClient is error) {
        test:assertFail("Error expected in NATS Connection initialization.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
isolated function testTlsConnection3() {
    SecureSocket secured = {
        cert: "tests/certs/server.crt",
        key: {
            certFile: "tests/certs/client.crt",
            keyFile: "tests/certs/client.key"
        }
    };
    Client|error newClient = new("nats://localhost:4225", clusterId = "my_cluster", secureSocket = secured);
    if newClient is error {
        test:assertFail("Error occurred in NATS Connection initialization with TLS.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
isolated function testTlsConnection4() {
    SecureSocket secured = {
        cert: {
            path: "tests/certs/truststore.jks",
            password: "password"
        },
        key: {
            certFile: "tests/certs/client.crt",
            keyFile: "tests/certs/client.key"
        }
    };
    Client|error newClient = new("nats://localhost:4225", clusterId = "my_cluster", secureSocket = secured);
    if newClient is error {
        test:assertFail("Error occurred in NATS Connection initialization with TLS.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
isolated function testTlsConnection6() {
    SecureSocket secured = {
        cert: "tests/certs/server1.crt",
        key: {
            certFile: "tests/certs/client.crt",
            keyFile: "tests/certs/client.key"
        }
    };
    Client|error newClient = new("nats://localhost:4225", clusterId = "my_cluster", secureSocket = secured);
    if !(newClient is error) {
        test:assertFail("Error occurred in NATS Connection initialization with TLS.");
    }
}

@test:Config {
    groups: ["nats-streaming"]
}
isolated function testTlsConnection7() {
    SecureSocket secured = {
        cert: "tests/certs/server.crt",
        key: {
            certFile: "tests/certs/client1.crt",
            keyFile: "tests/certs/client1.key"
        }
    };
    Client|error newClient = new("nats://localhost:4225", clusterId = "my_cluster", secureSocket = secured);
    if !(newClient is error) {
        test:assertFail("Error occurred in NATS Connection initialization with TLS.");
    }
}
