// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/jballerina.java;

# The client provides the capability to publish messages to the NATS streaming server.
public isolated client class Client {

    # Initializes the NATS streaming client.
    # ```ballerina
    # stan:Client stanClient = check new(stan:DEFAULT_URL);
    # ```
    #
    # + url - The NATS broker URL. For a clustered use case, provide the URLs as a string array
    # + streamingConfig - The configurations related to the NATS streaming connectivity
    public isolated function init(string|string[] url, *StreamingConfiguration connectionConfig) returns Error? {
        return streamingClientInit(self, url, connectionConfig);
    }

    # Publishes data to a given subject.
    # ```ballerina
    # string result = check stanClient->publishMessage(message);
    # ```
    #
    # + message - The message to be published
    # + return - The `string` value representing the NUID (NATS Unique Identifier) of the published message if the
    #            message gets successfully published and acknowledged by the NATS server,
    #            a `stan:Error` with NUID and `message` fields in case an error occurs in publishing, the timeout
    #            elapses while waiting for the acknowledgement, or else
    #            a `stan:Error` only with the `message` field in case an error occurs even before publishing
    #            is completed
    isolated remote function publishMessage(Message message) returns string|Error =
    @java:Method {
        'class: "io.ballerina.stdlib.stan.producer.Publish"
    } external;

    # Closes the NATS streaming client connection.
    # ```ballerina
    # check stanClient.close();
    # ```
    #
    # + return - `()` or else a `stan:Error` if unable to complete the close operation
    public isolated function close() returns error? =
    @java:Method {
        'class: "io.ballerina.stdlib.stan.producer.Close"
    } external;
}

isolated function streamingClientInit(Client streamingClient, string|string[] urlString,
*StreamingConfiguration streamingConfig) returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.stan.producer.Init"
} external;
