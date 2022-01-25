/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.stan;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;


/**
 * Represents the constants which will be used for NATS.
 */
public class Constants {
    // Represents the NATS objects.
    public static final String NATS_CALLER = "Caller";

    // Represents the NATS streaming connection.
    public static final String NATS_STREAMING_CONNECTION = "nats_streaming_connection";

    public static final String NATS_METRIC_UTIL = "nats_metric_util";

    // Error code for i/o.
    static final String NATS_ERROR = "Error";

    // Represents the NATS Streaming message.
    public static final String NATS_STREAMING_MSG = "nats_streaming_message";

    public static final String NATS_STREAMING_SUBSCRIPTION_ANNOTATION = "ServiceConfig";
    public static final BString NATS_STREAMING_MANUAL_ACK = StringUtils.fromString("autoAck");

    public static final String NATS_STREAMING_MESSAGE_OBJ_NAME = "Message";
    public static final String MESSAGE_CONTENT = "content";
    public static final String MESSAGE_SUBJECT = "subject";
    public static final String STREAMING_DISPATCHER_LIST = "StreamingDispatcherList";
    public static final String STREAMING_SUBSCRIPTION_LIST = "StreamingSubscriptionsList";

    public static final String ON_MESSAGE_RESOURCE = "onMessage";

    public static final BString CONNECTION_CONFIG_SECURE_SOCKET = StringUtils.fromString("secureSocket");
    public static final BString KEY = StringUtils.fromString("key");
    public static final BString CERT = StringUtils.fromString("cert");
    public static final BString PROTOCOL = StringUtils.fromString("protocol");
    public static final BString PROTOCOL_NAME = StringUtils.fromString("name");
    public static final String PKCS12 = "PKCS12";
    public static final BString KEY_STORE_PASS = StringUtils.fromString("password");
    public static final BString KEY_STORE_PATH = StringUtils.fromString("path");
    public static final BString CERT_FILE = StringUtils.fromString("certFile");
    public static final BString KEY_FILE = StringUtils.fromString("keyFile");
    public static final BString KEY_PASSWORD = StringUtils.fromString("keyPassword");
    public static final String NATIVE_DATA_PUBLIC_KEY_CERTIFICATE = "NATIVE_DATA_PUBLIC_KEY_CERTIFICATE";
    public static final String NATIVE_DATA_PRIVATE_KEY = "NATIVE_DATA_PRIVATE_KEY";

    // StreamingConfig fields
    public static final BString URL = StringUtils.fromString("url");
    public static final BString CLIENT_ID = StringUtils.fromString("clientId");
    public static final BString CLUSTER_ID = StringUtils.fromString("clusterId");

    private Constants() {
    }
}
