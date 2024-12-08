// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.memory;

import jakarta.annotation.Nonnull;

public enum TrackingType {
    CREATION("Track Constructors");

    private final String myDescription;

    TrackingType(@SuppressWarnings("SameParameterValue") @Nonnull String description) {
        myDescription = description;
    }

    @Nonnull
    public String description() {
        return myDescription;
    }
}
