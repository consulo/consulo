// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

public enum OasSchemaFormat {
    INT_32("int32"),
    INT_64("int64"),
    FLOAT("float"),
    DOUBLE("double"),
    BYTE("byte"),
    BINARY("binary"),
    DATE("date"),
    DATE_TIME("date-time"),
    PARTIAL_TIME("partial-time"),
    PASSWORD("password"),
    URI("uri"),
    URL("url"),
    UUID("uuid");

    private final String myFormatName;

    OasSchemaFormat(String formatName) {
        myFormatName = formatName;
    }

    public String getFormatName() {
        return myFormatName;
    }
}
