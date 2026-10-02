// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

public enum OasSchemaType {
    INTEGER("integer"),
    NUMBER("number"),
    STRING("string"),
    BOOLEAN("boolean"),
    OBJECT("object"),
    ARRAY("array");

    private final String myTypeName;

    OasSchemaType(String typeName) {
        myTypeName = typeName;
    }

    public String getTypeName() {
        return myTypeName;
    }
}
