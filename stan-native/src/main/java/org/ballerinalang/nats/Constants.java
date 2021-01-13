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

package org.ballerinalang.nats;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;


/**
 * Represents the constants which will be used for NATS.
 */
public class Constants {
    // Represents the NATS objects.
    public static final String NATS_CONNECTION = "nats_connection";
    public static final String NATS_CALLER = "Caller";

    // Represents the NATS streaming connection.
    public static final String NATS_STREAMING_CONNECTION = "nats_streaming_connection";

    public static final String NATS_METRIC_UTIL = "nats_metric_util";

    // Represents connected clients.
    public static final String CONNECTED_CLIENTS = "connected_clients";

    // Represent whether connection close already triggered.
    public static final String CLOSING = "closing";

    // Error code for i/o.
    static final String NATS_ERROR = "StanError";

    // Represents the object which holds the connection.
    public static final BString CONNECTION_OBJ = StringUtils.fromString("conn");

    // Represents the connection url
    public static final BString URL = StringUtils.fromString("url");

    // Represents the NATS Streaming message.
    public static final String NATS_STREAMING_MSG = "nats_streaming_message";

    public static final String NATS_STREAMING_SUBSCRIPTION_ANNOTATION = "ServiceConfig";
    public static final BString NATS_STREAMING_MANUAL_ACK = StringUtils.fromString("autoAck");

    public static final String NATS_STREAMING_MESSAGE_OBJ_NAME = "Message";

    public static final String STREAMING_DISPATCHER_LIST = "StreamingDispatcherList";
    public static final String STREAMING_SUBSCRIPTION_LIST = "StreamingSubscriptionsList";

    public static final String ON_MESSAGE_RESOURCE = "onMessage";

    public static final String NATS_CLIENT_SUBSCRIBED = "[ballerina/nats] Client subscribed for ";

    public static final BString CONNECTION_CONFIG_SECURE_SOCKET = StringUtils.fromString("secureSocket");
    public static final BString CONNECTION_KEYSTORE = StringUtils.fromString("keyStore");
    public static final BString CONNECTION_TRUSTORE = StringUtils.fromString("trustStore");
    public static final BString CONNECTION_PROTOCOL = StringUtils.fromString("protocol");
    public static final String KEY_STORE_TYPE = "PKCS12";
    public static final BString KEY_STORE_PASS = StringUtils.fromString("password");
    public static final BString KEY_STORE_PATH = StringUtils.fromString("path");

    public static final String ERROR_SETTING_UP_SECURED_CONNECTION = "error while setting up secured connection. ";

    public static final String ON_ERROR_RESOURCE = "onError";

    public static final String THREAD_INTERRUPTED_ERROR =
            "internal error occurred. The current thread got interrupted.";

    private Constants() {
    }
}
