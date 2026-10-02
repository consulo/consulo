// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import org.jspecify.annotations.Nullable;

public final class OasParameterInUtil {
    private OasParameterInUtil() {
    }

    public static @Nullable OasParameterIn valueOfParameter(String paramName) {
        for (OasParameterIn value : OasParameterIn.values()) {
            if (value.getPlaceName().equals(paramName)) {
                return value;
            }
        }
        return null;
    }
}
