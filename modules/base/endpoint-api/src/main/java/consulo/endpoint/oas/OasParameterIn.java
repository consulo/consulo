// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

public enum OasParameterIn {
    PATH("path"),
    QUERY("query"),
    HEADER("header"),
    COOKIE("cookie");

    private final String myPlaceName;

    OasParameterIn(String placeName) {
        myPlaceName = placeName;
    }

    public String getPlaceName() {
        return myPlaceName;
    }
}
