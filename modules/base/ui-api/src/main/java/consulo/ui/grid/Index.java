// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import org.jspecify.annotations.Nullable;

/**
 * @author gregsh
 */
public abstract class Index {
    public final int value;

    public Index(int value) {
        this.value = value;
    }

    public int asInteger() {
        return value;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            assert o instanceof Index : "Comparing Model indices with View indices!";
            return false;
        }

        Index index = (Index) o;
        return value == index.value;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
