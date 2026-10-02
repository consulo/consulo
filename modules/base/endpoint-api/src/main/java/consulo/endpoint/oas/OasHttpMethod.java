// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

public enum OasHttpMethod {
    GET("get"),
    PUT("put"),
    POST("post"),
    DELETE("delete"),
    OPTIONS("options"),
    HEAD("head"),
    PATCH("patch"),
    TRACE("trace");

    private final String myMethodName;

    OasHttpMethod(String methodName) {
        myMethodName = methodName;
    }

    public String getMethodName() {
        return myMethodName;
    }
}
