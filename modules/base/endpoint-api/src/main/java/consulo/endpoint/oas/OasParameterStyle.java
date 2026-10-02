// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

public enum OasParameterStyle {
    DEFAULT(""),
    MATRIX("matrix"),
    LABEL("label"),
    FORM("form"),
    SPACE_DELIMITED("spaceDelimited"),
    PIPE_DELIMITED("pipeDelimited"),
    DEEP_OBJECT("deepObject"),
    SIMPLE("simple");

    private final String myValue;

    OasParameterStyle(String value) {
        myValue = value;
    }

    public String getValue() {
        return myValue;
    }
}
