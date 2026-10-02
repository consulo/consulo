// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import java.util.Collections;
import java.util.List;

public final class OasArrayValue implements OasExampleValue {
    private final List<OasExampleValue> myItems;

    public OasArrayValue(List<? extends OasExampleValue> items) {
        myItems = Collections.unmodifiableList(items);
    }

    public List<OasExampleValue> getItems() {
        return myItems;
    }
}
