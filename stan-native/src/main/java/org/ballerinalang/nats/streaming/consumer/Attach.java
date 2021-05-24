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

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;

import java.util.concurrent.ConcurrentHashMap;

import static io.ballerina.runtime.api.constants.RuntimeConstants.ORG_NAME_SEPARATOR;
import static io.ballerina.runtime.api.constants.RuntimeConstants.VERSION_SEPARATOR;
import static org.ballerinalang.nats.Constants.STREAMING_DISPATCHER_LIST;

/**
 * Create a listener and attach service.
 *
 * @since 1.0.0
 */
public class Attach {
    private static final String STREAMING_SUBSCRIPTION_CONFIG = "ServiceConfig";
    private static final BString SUBJECT_ANNOTATION_FIELD = StringUtils.fromString("subject");


    public static Object attach(Environment environment, BObject streamingListener, BObject service,
                                       Object serviceName) {
        String subject;
        Object streamingConnectionUrl = streamingListener.getNativeData(Constants.URL.getValue());
        @SuppressWarnings("unchecked")
        BMap<BString, Object> annotation = (BMap<BString, Object>) service.getType()
                .getAnnotation(StringUtils.fromString(Utils.getModule().getOrg() + ORG_NAME_SEPARATOR +
                                                              Utils.getModule().getName() + VERSION_SEPARATOR +
                                                              Utils.getModule().getVersion() +
                                                              ":" + STREAMING_SUBSCRIPTION_CONFIG));
        if (annotation != null) {
            subject = annotation.getStringValue(SUBJECT_ANNOTATION_FIELD).getValue();
        } else if (TypeUtils.getType(serviceName).getTag() == TypeTags.STRING_TAG) {
            // Else get the service name as the subject
            subject = ((BString) serviceName).getValue();
        } else {
            return Utils.createNatsError("Subject name cannot be found");
        }
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<BObject, StreamingListener> serviceListenerMap =
                (ConcurrentHashMap<BObject, StreamingListener>) streamingListener
                        .getNativeData(STREAMING_DISPATCHER_LIST);
        boolean manualAck = !getAckMode(service);
        serviceListenerMap.put(service, new StreamingListener(service, manualAck, environment.getRuntime(),
                                                              streamingConnectionUrl, subject));
        return null;
    }

    private static boolean getAckMode(BObject service) {
        @SuppressWarnings("unchecked")
        BMap<BString, Object> serviceConfig = (BMap<BString, Object>) service.getType()
                .getAnnotation(StringUtils.fromString(Utils.getModule().getOrg() + ORG_NAME_SEPARATOR +
                                                              Utils.getModule().getName() + VERSION_SEPARATOR +
                                                              Utils.getModule().getVersion() +
                                                              ":" + Constants.NATS_STREAMING_SUBSCRIPTION_ANNOTATION));
        return serviceConfig.getBooleanValue(Constants.NATS_STREAMING_MANUAL_ACK);
    }
}
