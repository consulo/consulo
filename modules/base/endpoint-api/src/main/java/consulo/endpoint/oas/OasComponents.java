// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import java.util.Map;

public final class OasComponents {
    private final Map<String, OasSchema> mySchemas;

    public OasComponents(Map<String, OasSchema> schemas) {
        mySchemas = schemas;
    }

    public Map<String, OasSchema> getSchemas() {
        return mySchemas;
    }
}
