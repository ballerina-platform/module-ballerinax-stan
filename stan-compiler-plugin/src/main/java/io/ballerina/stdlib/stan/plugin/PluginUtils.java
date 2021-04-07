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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;

import java.util.Optional;

/**
 * Util class for the compiler plugin.
 */
public class PluginUtils {

    public static Diagnostic getDiagnostic(PluginConstants.CompilationErrors error, DiagnosticSeverity severity,
                                           Location location) {
        String errorMessage = error.getError();
        String diagnosticCode = error.getErrorCode();
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(diagnosticCode, errorMessage,
                severity);
        return DiagnosticFactory.createDiagnostic(diagnosticInfo, location);
    }

    public static MethodSymbol getMethodSymbol(SyntaxNodeAnalysisContext context,
                                               FunctionDefinitionNode functionDefinitionNode) {
        MethodSymbol methodSymbol = null;
        SemanticModel semanticModel = context.semanticModel();
        Optional<Symbol> symbol = semanticModel.symbol(functionDefinitionNode);
        if (symbol.isPresent()) {
            methodSymbol = (MethodSymbol) symbol.get();
        }
        return methodSymbol;
    }

    public static boolean isRemoteFunction(SyntaxNodeAnalysisContext context,
                                           FunctionDefinitionNode functionDefinitionNode) {
        MethodSymbol methodSymbol = getMethodSymbol(context, functionDefinitionNode);
        return methodSymbol.qualifiers().contains(Qualifier.valueOf(PluginConstants.REMOTE_QUALIFIER));
    }
}
