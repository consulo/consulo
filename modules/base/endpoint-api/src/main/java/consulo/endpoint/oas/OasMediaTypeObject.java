// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import java.util.Map;

public final class OasMediaTypeObject {
    private final OasSchema mySchema;
    private final Map<String, OasExample> myExamples;

    public OasMediaTypeObject(OasSchema schema, Map<String, OasExample> examples) {
        mySchema = schema;
        myExamples = examples;
    }

    public OasSchema getSchema() {
        return mySchema;
    }

    public Map<String, OasExample> getExamples() {
        return myExamples;
    }
}
