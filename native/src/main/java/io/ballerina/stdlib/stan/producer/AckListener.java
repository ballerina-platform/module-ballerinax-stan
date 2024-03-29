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
package io.ballerina.stdlib.stan.producer;

import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.stdlib.stan.Utils;
import io.ballerina.stdlib.stan.observability.NatsMetricsReporter;
import io.ballerina.stdlib.stan.observability.NatsObservabilityConstants;
import io.nats.streaming.AckHandler;

/**
 * {@link AckHandler} implementation to listen to message acknowledgements from NATS streaming server.
 */
public class AckListener implements AckHandler {
    private final Future balFuture;
    private final String subject;
    private final NatsMetricsReporter natsMetricsReporter;

    AckListener(Future balFuture, String subject, NatsMetricsReporter natsMetricsReporter) {
        this.balFuture = balFuture;
        this.subject = subject;
        this.natsMetricsReporter = natsMetricsReporter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAck(String nuid, Exception ex) {
        if (ex == null) {
            natsMetricsReporter.reportAcknowledgement(subject);
            balFuture.complete(StringUtils.fromString(nuid));
        } else {
            natsMetricsReporter.reportProducerError(subject, NatsObservabilityConstants.ERROR_TYPE_ACKNOWLEDGEMENT);
            BError error = Utils.createNatsError("NUID: " + nuid + "; " + ex.getMessage());
            balFuture.complete(error);
        }
    }
}
