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

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.stan.plugin.PluginConstants.CompilationErrors;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.Objects;
import java.util.Optional;

/**
 * STAN remote function validator.
 */
public class StanFunctionValidator {

    private final SyntaxNodeAnalysisContext context;
    private final ServiceDeclarationNode serviceDeclarationNode;
    FunctionDefinitionNode onMessage;
    FunctionDefinitionNode onError;

    public StanFunctionValidator(SyntaxNodeAnalysisContext context, FunctionDefinitionNode onMessage,
                                 FunctionDefinitionNode onError) {
        this.context = context;
        this.serviceDeclarationNode = (ServiceDeclarationNode) context.node();
        this.onMessage = onMessage;
        this.onError = onError;
    }

    public void validate() {
        validateMandatoryFunction();
        if (Objects.nonNull(onMessage)) {
            validateOnMessage();
        }
        if (Objects.nonNull(onError)) {
            validateOnError();
        }
    }

    private void validateMandatoryFunction() {
        if (Objects.isNull(onMessage)) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(CompilationErrors.NO_ON_MESSAGE,
                    DiagnosticSeverity.ERROR, serviceDeclarationNode.location()));
        }
    }

    private void validateOnMessage() {
        if (!PluginUtils.isRemoteFunction(context, onMessage)) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    CompilationErrors.FUNCTION_SHOULD_BE_REMOTE,
                    DiagnosticSeverity.ERROR, onMessage.functionSignature().location()));
        }
        SeparatedNodeList<ParameterNode> parameters = onMessage.functionSignature().parameters();
        validateFunctionParameters(parameters, onMessage);
        validateReturnTypeErrorOrNil(onMessage);
    }

    private void validateOnError() {
        if (!PluginUtils.isRemoteFunction(context, onError)) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    CompilationErrors.FUNCTION_SHOULD_BE_REMOTE,
                    DiagnosticSeverity.ERROR, onError.functionSignature().location()));
        }
        SeparatedNodeList<ParameterNode> parameters = onError.functionSignature().parameters();
        validateOnErrorFunctionParameters(parameters, onError);
        validateReturnTypeErrorOrNil(onError);
    }

    private void validateFunctionParameters(SeparatedNodeList<ParameterNode> parameters,
                                            FunctionDefinitionNode functionDefinitionNode) {
        if (parameters.size() == 1) {
            validateFirstParam(functionDefinitionNode, parameters.get(0));
        } else if (parameters.size() == 2) {
            validateFirstParam(functionDefinitionNode, parameters.get(0));
            validateSecondParam(functionDefinitionNode, parameters.get(1));
        }
        if (parameters.size() < 1) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(CompilationErrors.MUST_HAVE_MESSAGE,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.functionSignature().location()));
        }
        if (parameters.size() > 2) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(CompilationErrors.ONLY_PARAMS_ALLOWED,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.functionSignature().location()));
        }
    }

    private void validateOnErrorFunctionParameters(SeparatedNodeList<ParameterNode> parameters,
                                                   FunctionDefinitionNode functionDefinitionNode) {
        if (parameters.size() > 1) {
            validateFirstParam(functionDefinitionNode, parameters.get(0));
            validateErrorParam(functionDefinitionNode, parameters.get(1));
        }
        if (parameters.size() < 2) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(CompilationErrors.MUST_HAVE_MESSAGE_AND_ERROR,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.functionSignature().location()));
        }
        if (parameters.size() > 2) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(CompilationErrors.ONLY_PARAMS_ALLOWED_ON_ERROR,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.functionSignature().location()));
        }
    }

    private void validateFirstParam(FunctionDefinitionNode functionDefinitionNode,
                                    ParameterNode parameterNode) {
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        Node parameterTypeNode = requiredParameterNode.typeName();
        if (!parameterTypeNode.kind().equals(SyntaxKind.QUALIFIED_NAME_REFERENCE)) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
        } else {
            Token modulePrefix = ((QualifiedNameReferenceNode) parameterTypeNode).modulePrefix();
            IdentifierToken identifierToken = ((QualifiedNameReferenceNode) parameterTypeNode).identifier();
            if (!modulePrefix.text().equals(PluginConstants.PACKAGE_PREFIX) ||
                    !identifierToken.text().equals(PluginConstants.MESSAGE)) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(
                        CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE,
                        DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
            }
        }
    }

    private void validateSecondParam(FunctionDefinitionNode functionDefinitionNode,
                                    ParameterNode parameterNode) {
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        Node parameterTypeNode = requiredParameterNode.typeName();
        if (!parameterTypeNode.kind().equals(SyntaxKind.QUALIFIED_NAME_REFERENCE)) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    CompilationErrors.INVALID_FUNCTION_PARAM_CALLER,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
        } else {
            Token modulePrefix = ((QualifiedNameReferenceNode) parameterTypeNode).modulePrefix();
            IdentifierToken identifierToken = ((QualifiedNameReferenceNode) parameterTypeNode).identifier();
            if (!modulePrefix.text().equals(PluginConstants.PACKAGE_PREFIX) ||
                    !identifierToken.text().equals(PluginConstants.CALLER)) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(
                        CompilationErrors.INVALID_FUNCTION_PARAM_CALLER,
                        DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
            }
        }
    }

    private void validateReturnTypeErrorOrNil(FunctionDefinitionNode functionDefinitionNode) {
        Optional<ReturnTypeDescriptorNode> returnTypes = functionDefinitionNode.functionSignature().returnTypeDesc();
        if (returnTypes.isPresent()) {
            ReturnTypeDescriptorNode returnTypeDescriptorNode = returnTypes.get();
            Node returnNodeType = returnTypeDescriptorNode.type();
            String returnType = returnNodeType.toString().split(" ")[0];
            if (!returnType.equals(PluginConstants.ERROR_OR_NIL) &&
                    !returnType.equals(PluginConstants.NIL_OR_ERROR) &&
                    !returnType.equals(PluginConstants.STAN_ERROR_OR_NIL) &&
                    !returnType.equals(PluginConstants.NIL_OR_STAN_ERROR)) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(
                        CompilationErrors.INVALID_RETURN_TYPE_ERROR_OR_NIL,
                        DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
            }
        }
    }

    private void validateErrorParam(FunctionDefinitionNode functionDefinitionNode, ParameterNode parameterNode) {
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        Node parameterTypeNode = requiredParameterNode.typeName();
        if (!parameterTypeNode.kind().equals(SyntaxKind.QUALIFIED_NAME_REFERENCE)) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    CompilationErrors.INVALID_FUNCTION_PARAM_ERROR,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
        } else {
            Token modulePrefix = ((QualifiedNameReferenceNode) parameterTypeNode).modulePrefix();
            IdentifierToken identifierToken = ((QualifiedNameReferenceNode) parameterTypeNode).identifier();
            if (!modulePrefix.text().equals(PluginConstants.PACKAGE_PREFIX)) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(
                        CompilationErrors.INVALID_FUNCTION_PARAM_ERROR,
                        DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
            }
            if (!identifierToken.text().equalsIgnoreCase(PluginConstants.ERROR)) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(
                        CompilationErrors.INVALID_FUNCTION_PARAM_ERROR,
                        DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
            }
        }
    }
}
