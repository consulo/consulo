// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.icon;

import org.jspecify.annotations.Nullable;

public class CacheCustomizer {
    static final int DEFAULT_MAX_SIZE = 500;

    private @Nullable Integer myMaxSize = DEFAULT_MAX_SIZE;
    private @Nullable Integer myExpiresAfterMinutes = 5;

    public @Nullable Integer getMaxSize() {
        return myMaxSize;
    }

    public void setMaxSize(@Nullable Integer maxSize) {
        myMaxSize = maxSize;
    }

    public @Nullable Integer getExpiresAfterMinutes() {
        return myExpiresAfterMinutes;
    }

    public void setExpiresAfterMinutes(@Nullable Integer expiresAfterMinutes) {
        myExpiresAfterMinutes = expiresAfterMinutes;
    }
}
