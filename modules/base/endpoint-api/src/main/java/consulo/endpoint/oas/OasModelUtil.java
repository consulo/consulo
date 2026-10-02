// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import consulo.endpoint.url.PathSegmentRenderer;
import consulo.endpoint.url.UrlPath;
import org.jspecify.annotations.Nullable;

public final class OasModelUtil {
    public static final String OPEN_API_UNKNOWN_SEGMENT = "<unknown>";

    public static final PathSegmentRenderer OPEN_API_PRESENTATION = new PathSegmentRenderer() {
        @Override
        public String visitVariable(UrlPath.PathSegment.Variable variable) {
            StringBuilder builder = new StringBuilder();
            String variableName = variable.getVariableName();
            if (variableName == null || variableName.isEmpty()) {
                builder.append("{variable}");
            }
            else {
                builder.append("{");
                builder.append(variableName);
                builder.append("}");
            }
            return builder.toString();
        }

        @Override
        public String visitUndefined() {
            return OPEN_API_UNKNOWN_SEGMENT;
        }
    };

    private OasModelUtil() {
    }

    public static @Nullable OasHttpMethod getHttpMethodByName(String methodName) {
        for (OasHttpMethod method : OasHttpMethod.values()) {
            if (method.getMethodName().equalsIgnoreCase(methodName)) {
                return method;
            }
        }
        return null;
    }
}
