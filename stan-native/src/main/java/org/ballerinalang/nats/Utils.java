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

package org.ballerinalang.nats;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

/**
 * Utilities for producing and consuming via NATS sever.
 */
public class Utils {

    private static Module natsModule = null;

    private Utils() {
    }

    public static void setModule(Environment env) {
        natsModule = env.getCurrentModule();
    }

    public static Module getModule() {
        return natsModule;
    }

    public static BError createNatsError(String detailedErrorMessage) {
        return ErrorCreator.createDistinctError(Constants.NATS_ERROR, getModule(),
                                                StringUtils.fromString(detailedErrorMessage));
    }

    public static byte[] convertDataIntoByteArray(Object data) {
        return ((BArray) data).getBytes();
    }

    public static MethodType getAttachedFunctionType(BObject serviceObject, String functionName) {
        MethodType function = null;
        MethodType[] resourceFunctions = serviceObject.getType().getMethods();
        for (MethodType resourceFunction : resourceFunctions) {
            if (functionName.equals(resourceFunction.getName())) {
                function = resourceFunction;
                break;
            }
        }
        return function;
    }

    public static String getCommaSeparatedUrl(Object urlString) {
        if (TypeUtils.getType(urlString).getTag() == TypeTags.ARRAY_TAG) {
            // if string[]
            String[] serverUrls = ((BArray) urlString).getStringArray();
            return String.join(", ", serverUrls);
        } else {
            // if string
            return ((BString) urlString).getValue();
        }
    }
}
