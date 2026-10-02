// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import java.util.Map;

public final class OasRequestBody {
    private final Map<String, OasSchema> myContent;
    private final boolean myRequired;

    public OasRequestBody(Map<String, OasSchema> content, boolean required) {
        myContent = content;
        myRequired = required;
    }

    public Map<String, OasSchema> getContent() {
        return myContent;
    }

    public boolean getRequired() {
        return myRequired;
    }
}
