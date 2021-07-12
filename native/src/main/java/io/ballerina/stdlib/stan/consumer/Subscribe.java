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

import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.stan.Constants;
import io.ballerina.stdlib.stan.Utils;
import io.ballerina.stdlib.stan.observability.NatsMetricsReporter;
import io.ballerina.stdlib.stan.observability.NatsObservabilityConstants;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.Subscription;
import io.nats.streaming.SubscriptionOptions;

import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import static io.ballerina.runtime.api.constants.RuntimeConstants.ORG_NAME_SEPARATOR;
import static io.ballerina.runtime.api.constants.RuntimeConstants.VERSION_SEPARATOR;

/**
 * Remote function implementation for subscribing to a NATS subject.
 */
public class Subscribe {
    private static final PrintStream console;
    private static final String STREAMING_SUBSCRIPTION_CONFIG = "ServiceConfig";
    private static final BString QUEUE_NAME_ANNOTATION_FIELD = StringUtils.fromString("queueGroup");
    private static final BString DURABLE_NAME_ANNOTATION_FIELD = StringUtils.fromString("durableName");
    private static final BString MAX_IN_FLIGHT_ANNOTATION_FIELD = StringUtils.fromString("maxInFlight");
    private static final BString ACK_WAIT_ANNOTATION_FIELD = StringUtils.fromString("ackWait");
    private static final BString SUBSCRIPTION_TIMEOUT_ANNOTATION_FIELD = StringUtils.fromString(
            "subscriptionTimeout");
    private static final BString MANUAL_ACK_ANNOTATION_FIELD = StringUtils.fromString("autoAck");
    private static final BString START_POSITION_ANNOTATION_FIELD = StringUtils.fromString("startPosition");

    public static void streamingSubscribe(BObject streamingListener) {

        NatsMetricsReporter natsMetricsReporter =
                (NatsMetricsReporter) streamingListener.getNativeData(Constants.NATS_METRIC_UTIL);
        ConcurrentHashMap<BObject, StreamingListener> serviceListenerMap =
                (ConcurrentHashMap<BObject, StreamingListener>) streamingListener
                        .getNativeData(Constants.STREAMING_DISPATCHER_LIST);
        ConcurrentHashMap<BObject, Subscription> subscriptionsMap =
                (ConcurrentHashMap<BObject, Subscription>) streamingListener
                        .getNativeData(Constants.STREAMING_SUBSCRIPTION_LIST);
        Iterator serviceListeners = serviceListenerMap.entrySet().iterator();
        StreamingConnection streamingConnection =
                (StreamingConnection) streamingListener.getNativeData(Constants.NATS_STREAMING_CONNECTION);
        while (serviceListeners.hasNext()) {
            Map.Entry pair = (Map.Entry) serviceListeners.next();
            Subscription sub =
                    createSubscription((BObject) pair.getKey(), (StreamingListener) pair.getValue(),
                                       streamingConnection, natsMetricsReporter);
            subscriptionsMap.put((BObject) pair.getKey(), sub);
            serviceListeners.remove(); // avoids a ConcurrentModificationException
        }
    }

    private static Subscription createSubscription(BObject service, StreamingListener messageHandler,
                                                   io.nats.streaming.StreamingConnection streamingConnection,
                                                   NatsMetricsReporter natsMetricsReporter) {
        BMap<BString, Object> annotation = (BMap<BString, Object>) service.getType()
                .getAnnotation(StringUtils.fromString(Utils.getModule().getOrg() + ORG_NAME_SEPARATOR +
                                                              Utils.getModule().getName() + VERSION_SEPARATOR +
                                                              Utils.getModule().getVersion() +
                                                              ":" + STREAMING_SUBSCRIPTION_CONFIG));
        String subject = messageHandler.getSubject();
        assertNull(subject, "`Subject` annotation field is mandatory");
        String queueName = null;
        Subscription subscription;
        try {
            if (annotation != null) {
                if (annotation.containsKey(QUEUE_NAME_ANNOTATION_FIELD)) {
                    queueName = annotation.getStringValue(QUEUE_NAME_ANNOTATION_FIELD).getValue();
                }
                SubscriptionOptions subscriptionOptions = buildSubscriptionOptions(annotation);
                subscription = streamingConnection.subscribe(subject, queueName, messageHandler, subscriptionOptions);
            } else {
               subscription = streamingConnection.subscribe(subject, messageHandler);
            }
            String consoleOutput = "subject " + subject + (queueName != null ? " & queue " + queueName : "");
            console.println(Constants.NATS_CLIENT_SUBSCRIBED + consoleOutput);
            NatsMetricsReporter.reportSubscription(streamingConnection.getNatsConnection().getConnectedUrl(), subject);
            return subscription;
        } catch (IOException | InterruptedException e) {
            natsMetricsReporter.reportConsumerError(subject, NatsObservabilityConstants.ERROR_TYPE_SUBSCRIPTION);
            throw Utils.createNatsError(e.getMessage());
        } catch (TimeoutException e) {
            natsMetricsReporter.reportConsumerError(subject, NatsObservabilityConstants.ERROR_TYPE_SUBSCRIPTION);
            throw Utils.createNatsError("Error while creating the subscription");
        }
    }

    private static SubscriptionOptions buildSubscriptionOptions(BMap<BString, Object> annotation) {
        SubscriptionOptions.Builder builder = new SubscriptionOptions.Builder();
        String durableName = null;
        int maxInFlight = 1024;
        int ackWait = 30;
        int subscriptionTimeout = 2;
        if (annotation.containsKey(DURABLE_NAME_ANNOTATION_FIELD)) {
            durableName = annotation.getStringValue(DURABLE_NAME_ANNOTATION_FIELD).getValue();
        }
        if (annotation.containsKey(MAX_IN_FLIGHT_ANNOTATION_FIELD)) {
            maxInFlight = annotation.getIntValue(MAX_IN_FLIGHT_ANNOTATION_FIELD).intValue();
        }
        if (annotation.containsKey(ACK_WAIT_ANNOTATION_FIELD)) {
            ackWait = (int) ((BDecimal) annotation.get(ACK_WAIT_ANNOTATION_FIELD)).intValue();
        }
        if (annotation.containsKey(SUBSCRIPTION_TIMEOUT_ANNOTATION_FIELD)) {
            subscriptionTimeout = (int) ((BDecimal) annotation.get(SUBSCRIPTION_TIMEOUT_ANNOTATION_FIELD)).intValue();
        }
        boolean manualAck = !annotation.getBooleanValue(MANUAL_ACK_ANNOTATION_FIELD);

        Object startPosition = annotation.get(START_POSITION_ANNOTATION_FIELD);

        setStartPositionInBuilder(builder, startPosition);
        builder.durableName(durableName).maxInFlight(maxInFlight).ackWait(Duration.ofSeconds(ackWait))
                .subscriptionTimeout(Duration.ofSeconds(subscriptionTimeout));
        if (manualAck) {
            builder.manualAcks();
        }
        return builder.build();
    }

    private static void setStartPositionInBuilder(SubscriptionOptions.Builder builder, Object startPosition) {
        Type type = TypeUtils.getType(startPosition);
        int startPositionType = type.getTag();
        switch (startPositionType) {
            case TypeTags.STRING_TAG:
                BallerinaStartPosition startPositionValue = BallerinaStartPosition.valueOf(startPosition.toString());
                if (startPositionValue.equals(BallerinaStartPosition.LAST_RECEIVED)) {
                    builder.startWithLastReceived();
                } else if (startPositionValue.equals(BallerinaStartPosition.FIRST)) {
                    builder.deliverAllAvailable();
                }
                // The else scenario is when the Start Position is "NEW_ONLY". There is no option to set this
                // to the builder since this is the default.
                break;
            case TypeTags.TUPLE_TAG:
                BArray tupleValue = (BArray) startPosition;
                String startPositionKind = tupleValue.getRefValue(0).toString();
                long timeOrSequenceNo = (Long) tupleValue.getRefValue(1);
                if (startPositionKind.equals(BallerinaStartPosition.TIME_DELTA_START.name())) {
                    builder.startAtTimeDelta(Duration.ofSeconds(timeOrSequenceNo));
                } else {
                    builder.startAtSequence(timeOrSequenceNo);
                }
                break;
            default:
                throw new AssertionError("Invalid type for start position value " + startPositionType);
        }
    }

    private static void assertNull(Object nullableObject, String errorMessage) {
        if (nullableObject == null) {
            throw Utils.createNatsError(errorMessage);
        }
    }

    /**
     * Enum representing the constant values of the Ballerina level StartPosition type.
     */
    private enum BallerinaStartPosition {
        NEW_ONLY, LAST_RECEIVED, FIRST, TIME_DELTA_START, SEQUENCE_NUMBER;
    }

    static {
        console = System.out;
    }
}
