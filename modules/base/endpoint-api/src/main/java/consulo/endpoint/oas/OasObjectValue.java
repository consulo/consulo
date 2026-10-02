// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import java.util.Collections;
import java.util.Map;

public final class OasObjectValue implements OasExampleValue {
    private final Map<String, OasExampleValue> myProperties;

    public OasObjectValue(Map<String, ? extends OasExampleValue> properties) {
        myProperties = Collections.unmodifiableMap(properties);
    }

    public Map<String, OasExampleValue> getProperties() {
        return myProperties;
    }
}
