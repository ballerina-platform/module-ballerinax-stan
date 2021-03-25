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

package org.ballerinalang.nats.streaming.producer;

import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.nats.streaming.StreamingConnection;
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

/**
 * Initialize NATS producer using the connection.
 *
 * @since 1.1.0
 */
public class Init {

    public static Object streamingProducerInit(BObject streamingClientObject, BString url,
                                               BMap<BString, Object> streamingConfig) {
        BString clusterId = streamingConfig.getStringValue(Constants.CLUSTER_ID);
        Object clientIdNillable = streamingConfig.get(Constants.CLIENT_ID);
        StreamingConnection connection;
        try {
            connection = NatsStreamingConnection.createConnection(streamingClientObject, url.getValue(),
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
        streamingClientObject.addNativeData(Constants.NATS_STREAMING_CONNECTION, connection);
        NatsMetricsReporter natsMetricsReporter = new NatsMetricsReporter(connection);
        streamingClientObject.addNativeData(Constants.NATS_METRIC_UTIL, natsMetricsReporter);
        natsMetricsReporter.reportNewProducer();
        return null;
    }
}
