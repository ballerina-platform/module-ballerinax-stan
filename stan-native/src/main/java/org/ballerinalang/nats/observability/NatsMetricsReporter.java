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

package org.ballerinalang.nats.observability;

import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.observability.ObserveUtils;
import io.ballerina.runtime.observability.metrics.DefaultMetricRegistry;
import io.ballerina.runtime.observability.metrics.MetricId;
import io.ballerina.runtime.observability.metrics.MetricRegistry;
import io.nats.streaming.StreamingConnection;

/**
 * Providing metrics functionality to NATS.
 *
 * @since 1.1.0
 */
public class NatsMetricsReporter {

    private static final MetricRegistry metricRegistry = DefaultMetricRegistry.getInstance();
    private StreamingConnection connection;

    public NatsMetricsReporter(StreamingConnection connection) {
        this.connection = connection;
    }

    /**
     * Reports a new consumer connection.
     *
     * @param url URL of the NATS server that the client is connecting to.
     */
    public static void reportNewConnection(Object url) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        if (TypeUtils.getType(url).getTag() == TypeTags.ARRAY_TAG) {
            // if string[]
            String[] serverUrls = ((BArray) url).getStringArray();
            for (String serverUrl: serverUrls) {
                incrementGauge(new NatsObserverContext(serverUrl), NatsObservabilityConstants.METRIC_CONNECTIONS[0],
                        NatsObservabilityConstants.METRIC_CONNECTIONS[1]);
            }
        } else {
            // if string
            String serverUrl = ((BString) url).getValue();
            incrementGauge(new NatsObserverContext(serverUrl), NatsObservabilityConstants.METRIC_CONNECTIONS[0],
                    NatsObservabilityConstants.METRIC_CONNECTIONS[1]);
        }
    }

    /**
     * Reports a disconnection.
     *
     * @param url URL of the NATS server that the client is disconnecting from.
     */
    public static void reportConnectionClose(String url) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        decrementGauge(new NatsObserverContext(url), NatsObservabilityConstants.METRIC_CONNECTIONS[0],
                       NatsObservabilityConstants.METRIC_CONNECTIONS[1]);
    }

    /**
     * Reports a new producer connection.
     */
    public void reportNewProducer() {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        incrementGauge(
                new NatsObserverContext(NatsObservabilityConstants.CONTEXT_PRODUCER,
                                        connection.getNatsConnection().getConnectedUrl()),
                NatsObservabilityConstants.METRIC_PUBLISHERS[0],
                NatsObservabilityConstants.METRIC_PUBLISHERS[1]);

    }

    /**
     * Reports a producer disconnection.
     */
    public void reportProducerClose() {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        decrementGauge(
                new NatsObserverContext(NatsObservabilityConstants.CONTEXT_PRODUCER,
                                        connection.getNatsConnection().getConnectedUrl()),
                NatsObservabilityConstants.METRIC_PUBLISHERS[0],
                NatsObservabilityConstants.METRIC_PUBLISHERS[1]);

    }

    /**
     * Reports a message being published by a NATS producer.
     *
     * @param subject Subject the message is published to.
     * @param size    Size in bytes of the message.
     */
    public void reportPublish(String subject, int size) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        reportPublish(new NatsObserverContext(NatsObservabilityConstants.CONTEXT_PRODUCER,
                                              connection.getNatsConnection().getConnectedUrl(), subject), size);
    }


    /**
     * Reports a message being successfully received and handled.
     *
     * @param subject Subject the message is received to.
     */
    public static void reportDelivery(String url, String subject) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        incrementCounter(
                new NatsObserverContext(NatsObservabilityConstants.CONTEXT_PRODUCER,
                                        url, subject),
                NatsObservabilityConstants.METRIC_DELIVERED[0],
                NatsObservabilityConstants.METRIC_DELIVERED[1]);

    }

    /**
     * Reports a consumer subscribing to a subject.
     *
     * @param url     URL of the NATS server that the consumer is connected to.
     * @param subject Subject that the consumer subscribes to.
     */
    public static void reportSubscription(String url, String subject) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        NatsObserverContext observerContext = new NatsObserverContext(
                NatsObservabilityConstants.CONTEXT_PRODUCER, url, subject);
        incrementGauge(observerContext, NatsObservabilityConstants.METRIC_SUBSCRIPTION[0],
                       NatsObservabilityConstants.METRIC_SUBSCRIPTION[1]);
    }

    /**
     * Reports a consumer unsubscribing from a subject.
     *
     * @param url     URL of the NATS server that the consumer is connected to.
     * @param subject Subject that the consumer unsubscribes from.
     */
    public static void reportStreamingUnsubscription(String url, String subject) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        decrementGauge(
                new NatsObserverContext(NatsObservabilityConstants.CONTEXT_PRODUCER, url, subject),
                NatsObservabilityConstants.METRIC_SUBSCRIPTION[0],
                NatsObservabilityConstants.METRIC_SUBSCRIPTION[1]);
    }

    /**
     * Reports an acknowledgement.
     *
     * @param subject Subject that the consumer subscribes to.
     */
    public void reportAcknowledgement(String subject) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        incrementCounter(new NatsObserverContext(NatsObservabilityConstants.CONTEXT_PRODUCER,
                                                 connection.getNatsConnection().getConnectedUrl(), subject),
                         NatsObservabilityConstants.METRIC_ACK[0],
                         NatsObservabilityConstants.METRIC_ACK[1]);
    }

    /**
     * Reports a consumer consuming a message.
     *
     * @param subject Subject that the consumer receives the message from.
     * @param size    Size of the message in bytes.
     */
    public static void reportConsume(String url, String subject, int size) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        reportConsume(new NatsObserverContext(url, subject), size);
    }

    /**
     * Reports an error generated by a producer.
     *
     * @param subject   Subject that the producer is subscribed to.
     * @param errorType type of the error.
     */
    public void reportProducerError(String subject, String errorType) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        reportError(subject, NatsObservabilityConstants.CONTEXT_PRODUCER, errorType);
    }

    /**
     * Reports an error generated by a consumer.
     *
     * @param subject   Subject that the consumer is subscribed to.
     * @param errorType type of the error.
     */
    public void reportConsumerError(String subject, String errorType) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        this.reportError(subject, NatsObservabilityConstants.CONTEXT_CONSUMER, errorType);
    }

    private static void reportPublish(NatsObserverContext observerContext, int size) {
        incrementCounter(observerContext, NatsObservabilityConstants.METRIC_PUBLISHED[0],
                         NatsObservabilityConstants.METRIC_PUBLISHED[1]);
        incrementCounter(observerContext, NatsObservabilityConstants.METRIC_PUBLISHED_SIZE[0],
                         NatsObservabilityConstants.METRIC_PUBLISHED_SIZE[1], size);
    }

    private static void reportConsume(NatsObserverContext observerContext, int size) {
        incrementCounter(observerContext, NatsObservabilityConstants.METRIC_CONSUMED[0],
                         NatsObservabilityConstants.METRIC_CONSUMED[1]);
        incrementCounter(observerContext, NatsObservabilityConstants.METRIC_CONSUMED_SIZE[0],
                         NatsObservabilityConstants.METRIC_CONSUMED_SIZE[1], size);
    }

    public static void reportError(String context, String errorType) {
        NatsObserverContext observerContext = new NatsObserverContext(context);
        observerContext.addTag(NatsObservabilityConstants.TAG_ERROR_TYPE, errorType);
        incrementCounter(observerContext, NatsObservabilityConstants.METRIC_ERRORS[0],
                         NatsObservabilityConstants.METRIC_ERRORS[1]);
    }

    public void reportError(String subject, String context, String errorType) {
        NatsObserverContext observerContext =
                new NatsObserverContext(context, connection.getNatsConnection().getConnectedUrl(), subject);
        observerContext.addTag(NatsObservabilityConstants.TAG_ERROR_TYPE, errorType);
        incrementCounter(observerContext, NatsObservabilityConstants.METRIC_ERRORS[0],
                         NatsObservabilityConstants.METRIC_ERRORS[1]);
    }

    public static void reportStreamingError(String url, String subject, String context, String errorType) {
        NatsObserverContext observerContext = new NatsObserverContext(context, url, subject);
        observerContext.addTag(NatsObservabilityConstants.TAG_ERROR_TYPE, errorType);
        incrementCounter(observerContext, NatsObservabilityConstants.METRIC_ERRORS[0],
                         NatsObservabilityConstants.METRIC_ERRORS[1]);
    }

    private static void incrementCounter(NatsObserverContext observerContext, String name, String desc) {
        incrementCounter(observerContext, name, desc, 1);
    }

    private static void incrementCounter(NatsObserverContext observerContext, String name, String desc, int amount) {
        if (metricRegistry == null) {
            return;
        }
        metricRegistry.counter(new MetricId(
                NatsObservabilityConstants.CONNECTOR_NAME + "_" + name, desc, observerContext.getAllTags()))
                .increment(amount);
    }

    private static void incrementGauge(NatsObserverContext observerContext, String name, String desc) {
        if (metricRegistry == null) {
            return;
        }
        metricRegistry.gauge(new MetricId(
                NatsObservabilityConstants.CONNECTOR_NAME + "_" + name, desc, observerContext.getAllTags()))
                .increment();
    }

    private static void decrementGauge(NatsObserverContext observerContext, String name, String desc) {
        if (metricRegistry == null) {
            return;
        }
        metricRegistry.gauge(new MetricId(
                NatsObservabilityConstants.CONNECTOR_NAME + "_" + name, desc, observerContext.getAllTags()))
                .decrement();
    }
}
