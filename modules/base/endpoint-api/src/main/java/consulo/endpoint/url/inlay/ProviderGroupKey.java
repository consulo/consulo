// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.inlay;

import org.jspecify.annotations.Nullable;

public final class ProviderGroupKey {
    public static final ProviderGroupKey DEFAULT_KEY = new ProviderGroupKey("default.group.key");

    private final String myId;

    public ProviderGroupKey(String id) {
        myId = id;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProviderGroupKey that)) {
            return false;
        }
        return myId.equals(that.myId);
    }

    @Override
    public int hashCode() {
        return myId.hashCode();
    }

    @Override
    public String toString() {
        return "ProviderGroupKey(id=" + myId + ")";
    }
}
