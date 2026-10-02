// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class OasHeader {
    private final String myName;
    private final boolean myRequired;
    private final OasSchema mySchema;

    public OasHeader(String name, boolean required, OasSchema schema) {
        myName = name;
        myRequired = required;
        mySchema = schema;
    }

    public String getName() {
        return myName;
    }

    public boolean getRequired() {
        return myRequired;
    }

    public OasSchema getSchema() {
        return mySchema;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OasHeader that)) {
            return false;
        }
        return myRequired == that.myRequired && myName.equals(that.myName) && mySchema.equals(that.mySchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myName, myRequired, mySchema);
    }

    @Override
    public String toString() {
        return "OasHeader(name=" + myName + ", required=" + myRequired + ", schema=" + mySchema + ")";
    }
}
