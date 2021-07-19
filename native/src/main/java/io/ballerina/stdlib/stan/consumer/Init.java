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
package io.ballerina.stdlib.stan.consumer;

import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.stan.Constants;
import io.ballerina.stdlib.stan.Utils;
import io.ballerina.stdlib.stan.connection.NatsStreamingConnection;
import io.ballerina.stdlib.stan.observability.NatsMetricsReporter;
import io.ballerina.stdlib.stan.observability.NatsObservabilityConstants;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.Subscription;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Initializes the listener.
 *
 * @since 1.0.0
 */
public class Init {

    public static Object streamingListenerInit(BObject streamingListener, Object url,
                                               BMap<BString, Object> streamingConfig) {
        StreamingConnection streamingConnection;
        BString clusterId = streamingConfig.getStringValue(Constants.CLUSTER_ID);
        Object clientId = streamingConfig.get(Constants.CLIENT_ID);
        try {
            streamingConnection = NatsStreamingConnection.createConnection(streamingListener, url,
                                                                           clusterId.getValue(), clientId,
                                                                           streamingConfig);
        } catch (Exception e) {
            NatsMetricsReporter.reportError(NatsObservabilityConstants.CONTEXT_STREAMING_CONNNECTION,
                                            NatsObservabilityConstants.ERROR_TYPE_CONNECTION);
            return Utils.createNatsError("Internal error while creating streaming connection " +
                                                 e.getMessage());
        }
        streamingListener.addNativeData(Constants.URL.getValue(), url);
        streamingListener.addNativeData(Constants.NATS_STREAMING_CONNECTION, streamingConnection);
        streamingListener.addNativeData(Constants.NATS_METRIC_UTIL, new NatsMetricsReporter(streamingConnection));
        ConcurrentHashMap<BObject, StreamingListener> serviceListenerMap = new ConcurrentHashMap<>();
        streamingListener.addNativeData(Constants.STREAMING_DISPATCHER_LIST, serviceListenerMap);
        ConcurrentHashMap<BObject, Subscription> subscriptionsMap = new ConcurrentHashMap<>();
        streamingListener.addNativeData(Constants.STREAMING_SUBSCRIPTION_LIST, subscriptionsMap);
        return null;
    }
}
