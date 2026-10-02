// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class OasTag {
    private final String myName;
    private final String myDescription;

    public OasTag(String name, String description) {
        myName = name;
        myDescription = description;
    }

    public String getName() {
        return myName;
    }

    public String getDescription() {
        return myDescription;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OasTag that)) {
            return false;
        }
        return myName.equals(that.myName) && myDescription.equals(that.myDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myName, myDescription);
    }

    @Override
    public String toString() {
        return "OasTag(name=" + myName + ", description=" + myDescription + ")";
    }
}
