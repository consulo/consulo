// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.inlay;

import org.jspecify.annotations.Nullable;

public final class ProviderGroupInfo {
    public static final ProviderGroupInfo DEFAULT_PROVIDER_GROUP_INFO = new ProviderGroupInfo(ProviderGroupKey.DEFAULT_KEY, -1);

    private final ProviderGroupKey myKey;
    private final int myPriority;

    public ProviderGroupInfo(ProviderGroupKey key, int priority) {
        myKey = key;
        myPriority = priority;
    }

    public ProviderGroupKey getKey() {
        return myKey;
    }

    public int getPriority() {
        return myPriority;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProviderGroupInfo that)) {
            return false;
        }
        return myPriority == that.myPriority && myKey.equals(that.myKey);
    }

    @Override
    public int hashCode() {
        return 31 * myKey.hashCode() + myPriority;
    }

    @Override
    public String toString() {
        return "ProviderGroupInfo(key=" + myKey + ", priority=" + myPriority + ")";
    }
}
