// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.endpoint.url.UrlPath;
import consulo.endpoint.url.reference.UrlPathContext;

import java.util.ArrayList;
import java.util.List;

public final class PathVariableUtil {
    private PathVariableUtil() {
    }

    public static Iterable<String> getPathVariablesFromContext(UrlPathContext urlPathContext) {
        UrlPathContext fullyEvaluated = urlPathContext.getFullyEvaluated();
        return () -> collectPathVariables(fullyEvaluated).iterator();
    }

    private static List<String> collectPathVariables(UrlPathContext fullyEvaluated) {
        List<String> result = new ArrayList<>();
        for (UrlPathContext context = fullyEvaluated; context != null; context = context.getParent()) {
            for (UrlPath urlPath : context.getSelfPaths()) {
                for (UrlPath.PathSegment pathSegment : urlPath.getSegments()) {
                    if (pathSegment instanceof UrlPath.PathSegment.Composite composite) {
                        for (UrlPath.PathSegment segment : composite.getSegments()) {
                            if (segment instanceof UrlPath.PathSegment.Variable variable) {
                                addVariableName(result, variable);
                            }
                        }
                    }
                    else if (pathSegment instanceof UrlPath.PathSegment.Variable variable) {
                        addVariableName(result, variable);
                    }
                }
            }
        }
        return result;
    }

    private static void addVariableName(List<String> result, UrlPath.PathSegment.Variable variable) {
        String variableName = variable.getVariableName();
        if (variableName != null) {
            result.add(variableName);
        }
    }
}
