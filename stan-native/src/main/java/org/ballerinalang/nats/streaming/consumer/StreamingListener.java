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

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.observability.ObservabilityConstants;
import io.ballerina.runtime.observability.ObserveUtils;
import io.nats.streaming.Message;
import io.nats.streaming.MessageHandler;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;
import org.ballerinalang.nats.observability.NatsObservabilityConstants;
import org.ballerinalang.nats.observability.NatsObserverContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.ballerinalang.nats.Constants.NATS_STREAMING_MESSAGE_OBJ_NAME;
import static org.ballerinalang.nats.Constants.ON_ERROR_RESOURCE;
import static org.ballerinalang.nats.Constants.ON_MESSAGE_RESOURCE;
import static org.ballerinalang.nats.Utils.getAttachedFunctionType;

/**
 * {@link MessageHandler} implementation to listen to Messages of the subscribed subject from NATS streaming server.
 */
public class StreamingListener implements MessageHandler {
    private BObject service;
    private Runtime runtime;
    private String connectedUrl;
    private boolean manualAck;
    private String subject;

    public StreamingListener(BObject service, boolean manualAck, Runtime runtime,
                             String connectedUrl, String subject) {
        this.service = service;
        this.runtime = runtime;
        this.manualAck = manualAck;
        this.connectedUrl = connectedUrl;
        this.subject = subject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessage(Message msg) {
        BMap<BString, Object> msgRecord = ValueCreator.createRecordValue(
                Utils.getModule(), NATS_STREAMING_MESSAGE_OBJ_NAME);
        Object[] msgRecordValues = new Object[2];

        msgRecordValues[0] = ValueCreator.createArrayValue(msg.getData());
        msgRecordValues[1] = StringUtils.fromString(msg.getSubject());

        BMap<BString, Object> populatedMsgRecord = ValueCreator.createRecordValue(msgRecord, msgRecordValues);

        BObject callerObj = ValueCreator.createObjectValue(Utils.getModule(), Constants.NATS_CALLER);
        callerObj.addNativeData(Constants.NATS_STREAMING_MSG, msg);
        callerObj.addNativeData(Constants.NATS_STREAMING_MANUAL_ACK.getValue(), manualAck);

        MethodType onMessageResource = getAttachedFunctionType(service, "onMessage");
        Type[] parameterTypes = onMessageResource.getParameterTypes();
        if (parameterTypes.length == 1) {
            Object[] args1 = new Object[2];
            args1[0] = populatedMsgRecord;
            args1[1] = true;
            dispatch(args1, msg.getSubject());
        } else if (parameterTypes.length == 2) {
            Object[] args2 = new Object[4];
            args2[0] = populatedMsgRecord;
            args2[1] = true;
            args2[2] = callerObj;
            args2[3] = true;
            dispatch(args2, msg.getSubject());
        } else {
            throw Utils.createNatsError("Invalid remote function signature");
        }
    }

    private void dispatch(Object[] args, String subject) {
        executeResource(subject, args);
    }

    static void dispatchError(BObject serviceObject, BMap<BString, Object> msgObj, BError e, Runtime runtime) {
        boolean onErrorResourcePresent = Arrays.stream(serviceObject.getType().getMethods())
                .anyMatch(resource -> resource.getName().equals(ON_ERROR_RESOURCE));
        if (onErrorResourcePresent) {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            // Strand meta data
            StrandMetadata metadata = new StrandMetadata(Utils.getModule().getOrg(),
                                                         Utils.getModule().getName(),
                                                         Utils.getModule().getVersion(), ON_ERROR_RESOURCE);
            runtime.invokeMethodAsync(serviceObject, ON_ERROR_RESOURCE, null, metadata,
                                      new DispatcherCallback(), msgObj, true, e, true);
            try {
                countDownLatch.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw Utils.createNatsError(Constants.THREAD_INTERRUPTED_ERROR);
            }
        }
    }

    private void executeResource(String subject, Object[] args) {
        StrandMetadata metadata = new StrandMetadata(Utils.getModule().getOrg(), Utils.getModule().getName(),
                                                     Utils.getModule().getVersion(), ON_MESSAGE_RESOURCE);
        if (ObserveUtils.isTracingEnabled()) {
            Map<String, Object> properties = new HashMap<>();
            NatsObserverContext observerContext = new NatsObserverContext(NatsObservabilityConstants.CONTEXT_CONSUMER,
                                                                          connectedUrl, subject);
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            runtime.invokeMethodAsync(service, ON_MESSAGE_RESOURCE,
                                      null, metadata, new DispatcherCallback(),
                                      properties, args);
        } else {
            runtime.invokeMethodAsync(service, ON_MESSAGE_RESOURCE,
                                      null, metadata, new DispatcherCallback(), args);
        }
    }

    public String getSubject() {
        return this.subject;
    }

    private static class DispatcherCallback implements Callback {

        public DispatcherCallback() {
        }

        @Override
        public void notifySuccess(Object obj) {
        }

        @Override
        public void notifyFailure(io.ballerina.runtime.api.values.BError error) {
            error.printStackTrace();
        }
    }
}
