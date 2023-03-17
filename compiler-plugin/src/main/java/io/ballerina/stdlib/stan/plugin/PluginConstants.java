/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.stan.plugin;

/**
 * STAN compiler plugin constants.
 */
public class PluginConstants {
    // compiler plugin constants
    public static final String PACKAGE_PREFIX = "stan";
    public static final String ON_MESSAGE_FUNC = "onMessage";
    public static final String ON_ERROR_FUNC = "onError";
    public static final String PACKAGE_ORG = "ballerinax";

    // parameters
    public static final String MESSAGE = "Message";
    public static final String CONTENT_FIELD = "content";
    public static final String SUBJECT_FIELD = "subject";
    public static final String CALLER = "Caller";
    public static final String ERROR_PARAM = "Error";

    // return types error or nil
    public static final String ERROR = "error";
    public static final String STAN_ERROR = PACKAGE_PREFIX + ":" + ERROR_PARAM;
    public static final String NIL = "?";

    /**
     * Compilation errors.
     */
    enum CompilationErrors {
        NO_ON_MESSAGE("Service must have remote method onMessage.",
                "STAN_101"),
        INVALID_REMOTE_FUNCTION("Invalid remote method.", "STAN_102"),
        FUNCTION_SHOULD_BE_REMOTE("Method must have the remote qualifier.", "STAN_103"),
        MUST_HAVE_MESSAGE("Must have the method parameter stan:Message.", "STAN_104"),
        MUST_HAVE_MESSAGE_AND_ERROR("Must have the method parameters stan:Message and stan:Error.",
                "STAN_105"),
        INVALID_FUNCTION("Resource functions are not allowed.", "STAN_106"),
        INVALID_FUNCTION_PARAM_MESSAGE("Invalid method parameter. Only stan:Message is allowed.",
                "STAN_107"),
        INVALID_FUNCTION_PARAM_ERROR("Invalid method parameter. Only stan:Error is allowed.",
                "STAN_108"),
        INVALID_FUNCTION_PARAM_CALLER("Invalid method parameter. Only stan:Caller is allowed.",
                "STAN_109"),
        ONLY_PARAMS_ALLOWED("Invalid method parameter count. Only stan:Message and stan:Caller are allowed.",
                "STAN_110"),
        ONLY_PARAMS_ALLOWED_ON_ERROR("Invalid method parameter count. Only stan:Message and stan:Error are allowed.",
                "STAN_111"),
        INVALID_RETURN_TYPE_ERROR_OR_NIL("Invalid return type. Only error? or stan:Error? is allowed.",
                "STAN_112"),
        INVALID_MULTIPLE_LISTENERS("Multiple listener attachments. Only one nats:Listener is allowed.",
                "STAN_113"),
        INVALID_ANNOTATION_NUMBER("Only one service config annotation is allowed.",
                "STAN_114"),
        NO_ANNOTATION("No @nats:ServiceConfig{} annotation is found.",
                "STAN_115"),
        INVALID_ANNOTATION("Invalid service config annotation. Only @nats:ServiceConfig{} is allowed.",
                "STAN_116"),
        INVALID_SERVICE_ATTACH_POINT("Invalid service attach point. Only string literals are allowed.",
                "STAN_117"),
        DEPRECATION_WARNING("The ballerinax/stan module is being deprecated. NATS enabled applications requiring " +
                "persistence should use the JetStreamClient provided by ballerinax/nats module..", "STAN_118");

        private final String error;
        private final String errorCode;

        CompilationErrors(String error, String errorCode) {
            this.error = error;
            this.errorCode = errorCode;
        }

        String getError() {
            return error;
        }

        String getErrorCode() {
            return errorCode;
        }
    }

    private PluginConstants() {
    }
}
