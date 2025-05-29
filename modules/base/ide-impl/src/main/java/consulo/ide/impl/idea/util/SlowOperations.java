// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util;

import consulo.application.AccessToken;
import consulo.application.util.function.ThrowableComputable;
import consulo.util.lang.function.ThrowableRunnable;
import jakarta.annotation.Nonnull;

public final class SlowOperations {
    public static <T, E extends Throwable> T allowSlowOperations(@Nonnull ThrowableComputable<T, E> computable) throws E {
        return computable.compute();
    }

    public static <E extends Throwable> void allowSlowOperations(@Nonnull ThrowableRunnable<E> runnable) throws E {
        runnable.run();
    }

    public static AccessToken reportOnceIfViolatedFor(Object o) {
        return AccessToken.EMPTY_ACCESS_TOKEN;
    }
}
