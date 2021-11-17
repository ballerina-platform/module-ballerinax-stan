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

# Represents the NATS streaming listener to which a subscription service should be bound in order to
# receive messages of the corresponding subscription.
public isolated class Listener {

    # Initializes the NATS streaming Listener.
    # ```ballerina
    #  stan:Listener stanListener = check new(stan:DEFAULT_URL);
    # ```
    #
    # + url - The NATS Broker URL. For a clustered use case, provide the URLs as a string array
    # + streamingConfig - The configuration related to the NATS streaming connectivity
    public isolated function init(string|string[] url, *StreamingConfiguration streamingConfig) returns Error? {
        return streamingListenerInit(self, url, streamingConfig);
    }

    # Binds a service to the `stan:Listener`.
    # ```ballerina
    # check stanListener.attach(service, "serviceName");
    # ```
    #
    # + s - The type descriptor of the service
    # + name - The name of the service
    # + return - `()` or else a `stan:Error` upon failure to attach
    public isolated function attach(Service s, string|string[]? name = ()) returns error? =
    @java:Method {
        'class: "io.ballerina.stdlib.stan.consumer.Attach"
    } external;

    # Stops consuming messages and detaches the service from the `stan:Listener`.
    # ```ballerina
    # check stanListener.detach(service);
    # ```
    #
    # + s - The type descriptor of the service
    # + return - `()` or else a `stan:Error` upon failure to detach the service
    public isolated function detach(Service s) returns error? {
        streamingDetach(self, s);
        return;
    }

    # Starts the `stan:Listener`.
    # ```ballerina
    # check stanListener.'start();
    # ```
    #
    # + return - `()` or else a `stan:Error` upon failure to start the listener
    public isolated function 'start() returns error? {
         streamingSubscribe(self);
         return;
    }

    # Stops the `stan:Listener` gracefully.
    # ```ballerina
    # check stanListener.gracefulStop();
    # ```
    #
    # + return - `()` or else a `stan:Error` upon failure to stop the listener
    public isolated function gracefulStop() returns error? {
        return ();
    }

    # Stops the `stan:Listener` forcefully.
    # ```ballerina
    # check stanListener.immediateStop();
    # ```
    # + return - `()` or else a `stan:Error` upon failure to stop the listener
    public isolated function immediateStop() returns error? {
        return streamingListenerClose(self);
    }

    isolated function close() returns error? {
        return streamingListenerClose(self);
    }
}

isolated function streamingListenerInit(Listener lis, string|string[] urlString,
*StreamingConfiguration streamingConfig) returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.stan.consumer.Init"
} external;

isolated function streamingSubscribe(Listener streamingClient) =
@java:Method {
    'class: "io.ballerina.stdlib.stan.consumer.Subscribe"
} external;

isolated function streamingDetach(Listener lis, Service serviceType) =
@java:Method {
    'class: "io.ballerina.stdlib.stan.consumer.Detach"
} external;

isolated function streamingListenerClose(Listener lis) returns error? =
@java:Method {
    'class: "io.ballerina.stdlib.stan.consumer.Close"
} external;

# The STAN service type
public type Service distinct service object {
    // TBD when support for optional params in remote functions is available in lang
};
