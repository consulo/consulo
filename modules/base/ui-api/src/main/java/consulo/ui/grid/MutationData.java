// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import org.jspecify.annotations.Nullable;

public class MutationData {
    private final @Nullable Object myValue;
    private final @Nullable Object myMetadata;

    public MutationData(@Nullable Object value) {
        myValue = value;
        myMetadata = null;
    }

    public MutationData(@Nullable Object value, @Nullable Object metadata) {
        myValue = value;
        myMetadata = metadata;
    }

    public @Nullable Object getValue() {
        return myValue;
    }

    public @Nullable Object getMetadata() {
        return myMetadata;
    }
}
