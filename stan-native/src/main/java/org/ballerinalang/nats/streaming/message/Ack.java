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
package org.ballerinalang.nats.streaming.message;

import io.ballerina.runtime.api.values.BObject;
import io.nats.streaming.Message;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;

import java.io.IOException;

/**
 * Remote function implementation for acknowledging a message from a NATS streaming server.
 */
public class Ack {

    public static Object ack(BObject caller) {
        Message streamingMessage = (Message) caller.getNativeData(Constants.NATS_STREAMING_MSG);
        boolean manualAck = (Boolean) caller.getNativeData(Constants.NATS_STREAMING_MANUAL_ACK.getValue());
        try {
            if (manualAck) {
                streamingMessage.ack();
            } else {
                return Utils.createNatsError("Invalid operation, " +
                        "manual acknowledgement is not supported in auto ACK mode.");
            }
            return null;
        } catch (IOException e) {
            return Utils.createNatsError(e.getMessage());
        }
    }
}
