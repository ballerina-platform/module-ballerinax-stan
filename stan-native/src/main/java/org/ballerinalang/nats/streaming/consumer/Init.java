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
package org.ballerinalang.nats.streaming.consumer;

import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.Subscription;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;
import org.ballerinalang.nats.connection.NatsStreamingConnection;
import org.ballerinalang.nats.observability.NatsMetricsReporter;
import org.ballerinalang.nats.observability.NatsObservabilityConstants;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Initializes the listener.
 *
 * @since 1.0.0
 */
public class Init {

    public static Object streamingListenerInit(BObject streamingListener, BMap streamingConfig) {
        StreamingConnection streamingConnection;
        BString connectionObject = streamingConfig.getStringValue(Constants.URL);
        BString clusterId = streamingConfig.getStringValue(Constants.CLUSTER_ID);
        Object clientIdNillable = streamingConfig.get(Constants.CLIENT_ID);
        try {
            streamingConnection = NatsStreamingConnection.createConnection(streamingListener,
                                                                           connectionObject.getValue(),
                                                                           clusterId.getValue(), clientIdNillable,
                                                                           streamingConfig);
        } catch (IOException e) {
            NatsMetricsReporter.reportError(NatsObservabilityConstants.CONTEXT_STREAMING_CONNNECTION,
                                            NatsObservabilityConstants.ERROR_TYPE_CONNECTION);
            return Utils.createNatsError("internal error while creating streaming connection " +
                                                 e.getMessage());
        } catch (InterruptedException e) {
            NatsMetricsReporter.reportError(NatsObservabilityConstants.CONTEXT_STREAMING_CONNNECTION,
                                            NatsObservabilityConstants.ERROR_TYPE_CONNECTION);
            return Utils.createNatsError("internal error while creating streaming connection");
        } catch (CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException |
                KeyManagementException e) {
            NatsMetricsReporter.reportError(NatsObservabilityConstants.CONTEXT_STREAMING_CONNNECTION,
                                            NatsObservabilityConstants.ERROR_TYPE_CONNECTION);
            return Utils.createNatsError(Constants.ERROR_SETTING_UP_SECURED_CONNECTION + e.getMessage());
        }
        streamingListener.addNativeData(Constants.NATS_STREAMING_CONNECTION, streamingConnection);
        streamingListener.addNativeData(Constants.NATS_METRIC_UTIL, new NatsMetricsReporter(streamingConnection));
        ConcurrentHashMap<BObject, StreamingListener> serviceListenerMap = new ConcurrentHashMap<>();
        streamingListener.addNativeData(Constants.STREAMING_DISPATCHER_LIST, serviceListenerMap);
        ConcurrentHashMap<BObject, Subscription> subscriptionsMap = new ConcurrentHashMap<>();
        streamingListener.addNativeData(Constants.STREAMING_SUBSCRIPTION_LIST, subscriptionsMap);
        return null;
    }
}
