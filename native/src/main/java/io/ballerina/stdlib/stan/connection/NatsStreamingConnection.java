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
package io.ballerina.stdlib.stan.connection;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.stan.BallerinaNatsStreamingConnectionFactory;
import io.ballerina.stdlib.stan.Constants;
import io.ballerina.stdlib.stan.Utils;
import io.ballerina.stdlib.stan.observability.NatsMetricsReporter;
import io.ballerina.stdlib.stan.observability.NatsObservabilityConstants;
import io.ballerina.stdlib.stan.observability.NatsTracingUtil;
import io.nats.streaming.StreamingConnection;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Remote function implementation for NATS Streaming Connection creation.
 */
public class NatsStreamingConnection {

    public static StreamingConnection createConnection(BObject streamingClientObject, Object url,
                                                       String clusterId, Object clientIdNillable,
                                                       BMap<BString, Object> streamingConfig) throws Exception {
        String clientId = clientIdNillable == null ? UUID.randomUUID().toString() :
                ((BString) clientIdNillable).getValue();
        BallerinaNatsStreamingConnectionFactory streamingConnectionFactory =
                new BallerinaNatsStreamingConnectionFactory(
                        url, clusterId, clientId, streamingConfig);
        StreamingConnection streamingConnection = streamingConnectionFactory.createConnection();
        streamingClientObject.addNativeData(Constants.NATS_STREAMING_CONNECTION, streamingConnection);
        NatsMetricsReporter.reportNewConnection(url);
        return streamingConnection;
    }

    public static Object closeConnection(Environment environment, BObject streamingClientObject) {
        StreamingConnection streamingConnection = (StreamingConnection) streamingClientObject
                .getNativeData(Constants.NATS_STREAMING_CONNECTION);
        NatsTracingUtil.traceResourceInvocation(environment,
                                                streamingConnection.getNatsConnection().getConnectedUrl());
        try {
            String url = streamingConnection.getNatsConnection().getConnectedUrl();
            streamingConnection.close();
            NatsMetricsReporter.reportConnectionClose(url);
            return null;
        } catch (IOException | TimeoutException | InterruptedException e) {
            NatsMetricsReporter.reportStreamingError(streamingConnection.getNatsConnection().getConnectedUrl(),
                                                    NatsObservabilityConstants.UNKNOWN,
                                                    NatsObservabilityConstants.CONTEXT_STREAMING_CONNNECTION,
                                                    NatsObservabilityConstants.ERROR_TYPE_CLOSE);
            return Utils.createNatsError(e.getMessage());
        }
    }

}
