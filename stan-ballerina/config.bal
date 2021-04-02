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

import ballerina/crypto;

# Default URL for NATS connections.
public const string DEFAULT_URL = "nats://localhost:4222";

# Configuration related to establishing a streaming connection.
#
# + clientId - A unique identifier of the client
# + clusterId - The unique identifier of the cluster configured in the NATS server
# + ackTimeout - Timeout (in seconds) to wait for an acknowledgement
#                           for the corresponding subscription
# + connectionTimeout - Timeout (in seconds) to wait for a connection
# + maxPubAcksInFlight - The maximum number of published ACKs that may be
#                           in flight at any point of time
# + discoverPrefix - Subject prefix used for server discovery
# + pingInterval - The interval (in seconds) between the attempts
#                           of pinging the server
# + auth - Configurations related to authentication
# + secureSocket - Configurations related to SSL/TLS
public type StreamingConfiguration record {|
  string clientId?;
  string clusterId = "test-cluster";
  decimal ackTimeout = 30;
  decimal connectionTimeout = 5;
  int maxPubAcksInFlight = 16384;
  string discoverPrefix = "_STAN.discover";
  decimal pingInterval = 120;
  Credentials|Tokens auth?;
  SecureSocket secureSocket?;
|};

# Configurations related to token based authentication.
#
# + token - The token for token-based authentication
public type Tokens record {|
  string token;
|};

# Configurations related to basic authentication.
#
# + username - The username for basic authentication
# + password - The password for basic authentication
public type Credentials record {|
  string username;
  string password;
|};

# Configurations related to facilitating a secure communication.
#
# + cert - Configurations associated with `crypto:TrustStore`
# + key - Configurations associated with `crypto:KeyStore`
# + protocol - SSL/TLS protocol related options
public type SecureSocket record {|
    crypto:TrustStore cert;
    crypto:KeyStore key?;
    record {|
        Protocol name;
    |} protocol?;
|};

# Represents protocol options.
public enum Protocol {
   SSL,
   TLS,
   DTLS
}
