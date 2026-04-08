// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.util;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.disposer.Disposable;
import jakarta.annotation.Nonnull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @deprecated Please migrate to coroutines
 */
@Deprecated
public final class ProgressIndicatorsProvider implements Disposable {
    private final Set<ProgressIndicator> indicators = ConcurrentHashMap.newKeySet();

    public @Nonnull ProgressIndicator acquireIndicator() {
        ProgressIndicator indicator = new EmptyProgressIndicator();
        indicators.add(indicator);
        return indicator;
    }

    public boolean releaseIndicator(@Nonnull ProgressIndicator indicator) {
        return indicators.remove(indicator);
    }

    @Override
    public void dispose() {
        indicators.forEach(ProgressIndicator::cancel);
    }
}
