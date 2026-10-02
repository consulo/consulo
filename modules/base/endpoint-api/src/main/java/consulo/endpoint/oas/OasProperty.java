// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class OasProperty {
    private final String myName;
    private final OasSchema mySchema;

    public OasProperty(String name, OasSchema schema) {
        myName = name;
        mySchema = schema;
    }

    public String getName() {
        return myName;
    }

    public OasSchema getSchema() {
        return mySchema;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OasProperty that)) {
            return false;
        }
        return myName.equals(that.myName) && mySchema.equals(that.mySchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myName, mySchema);
    }

    @Override
    public String toString() {
        return "OasProperty(name=" + myName + ", schema=" + mySchema + ")";
    }
}
