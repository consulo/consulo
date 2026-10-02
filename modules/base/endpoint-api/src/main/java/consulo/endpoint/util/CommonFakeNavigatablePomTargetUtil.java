// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.util;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public final class CommonFakeNavigatablePomTargetUtil {
    private static @Nullable Consumer<CommonFakeNavigatablePomTarget> ourMockedFindUsages = null;

    private CommonFakeNavigatablePomTargetUtil() {
    }

    static @Nullable Consumer<CommonFakeNavigatablePomTarget> getMockedFindUsages() {
        return ourMockedFindUsages;
    }

    @TestOnly
    public static void mockFindUsages(Disposable disposable, Consumer<CommonFakeNavigatablePomTarget> handler) {
        Consumer<CommonFakeNavigatablePomTarget> oldValue = ourMockedFindUsages;
        ourMockedFindUsages = handler;
        Disposer.register(disposable, () -> ourMockedFindUsages = oldValue);
    }
}
