// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.concurrent;

import jakarta.annotation.Nullable;

public interface Obsolescent {
    /**
     * @return {@code true} if result of computation won't be used so computation may be interrupted
     */
    boolean isObsolete();

    static boolean isObsolete(@Nullable Object o) {
        return o instanceof Obsolescent obs && obs.isObsolete();
    }
}
