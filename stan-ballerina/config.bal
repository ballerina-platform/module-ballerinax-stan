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
# + ackTimeoutInSeconds - Timeout (in seconds) to wait for an acknowledgement
#                           for the corresponding subscription
# + connectionTimeoutInSeconds - Timeout (in seconds) to wait for a connection
# + maxPubAcksInFlight - The maximum number of published ACKs that may be
#                           in flight at any point of time
# + discoverPrefix - Subject prefix used for server discovery
# + pingIntervalInMinutes - The interval (in minutes) between the attempts
#                           of pinging the server
# + auth - Configurations related to authentication
# + secureSocket - Configurations related to SSL/TLS
public type StreamingConfig record {|
  int ackTimeoutInSeconds = 30;
  int connectionTimeoutInSeconds = 5;
  int maxPubAcksInFlight = 16384;
  string discoverPrefix = "_STAN.discover";
  int pingIntervalInMinutes = 2;
  Credentials|Tokens auth?;
  SecureSocket? secureSocket = ();
|};

# Configurations related to token based authentication.
#
# + token - The token for token-based authentication
public type Tokens record {|
  string token?;
|};

# Configurations related to basic authentication.
#
# + username - The username for basic authentication
# + password - The password for basic authentication
public type Credentials record {|
  string username?;
  string password?;
|};

# Configurations related to facilitating a secure communication with a remote HTTP endpoint.
#
# + trustStore - Configurations associated with the TrustStore
# + keyStore - Configurations associated with the KeyStore
# + protocol - The standard name of the requested protocol
public type SecureSocket record {|
    crypto:TrustStore? trustStore = ();
    crypto:KeyStore? keyStore = ();
    string protocol = "TLS";
|};
